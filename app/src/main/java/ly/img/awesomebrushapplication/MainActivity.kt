package ly.img.awesomebrushapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity.BOTTOM
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import ly.img.awesomebrushapplication.databinding.ActivityMainBinding
import ly.img.awesomebrushapplication.drawing.DrawingStateManager
import java.io.FileDescriptor
import java.io.IOException
import java.io.OutputStream
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    private lateinit var views: ActivityMainBinding
    private var drawingStateManager: DrawingStateManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(views.root)
        /*

          == Layout ==

            We require a very simple layout here and you can use an XML layout or code to create it:
              * Load Image -> Load an image from the gallery and display it on screen.
              * Save Image -> Save the final image to the gallery.
              * Color -> Let the user select a color from a list of colors.
              * Size -> Let the user specify the radius of a stroke via a slider.gi
              * Clear all -> Let the user remove all strokes from the image to start over.

          ----------------------------------------------------------------------
         | HINT: The layout doesn't have to look good, but it should be usable. |
          ----------------------------------------------------------------------

          == Requirements ==
              * Your drawing must be applied to the original image, not the downscaled preview. That means that 
                your brush must work in image coordinates instead of view coordinates and the saved image must have 
                the same resolution as the originally loaded image.
              * You can ignore OutOfMemory issues. If you run into memory issues just use a smaller image for testing.

          == Things to consider ==
            These features would be nice to have. Structure your program in such a way, that it could be added afterwards 
            easily. If you have time left, feel free to implement it already.

              * The user can make mistakes, so a history (undo/redo) would be nice.
              * The image usually doesn't change while brushing, but can be replaced with a higher resolution variant. A 
                common scenario would be a small preview but a high-resolution final rendering. Keep this in mind when 
                creating your data structures.
         */

        setupInteraction()
    }

    private fun setupWidthSelection() {
        views.seekBar.isVisible = true
        views.seekBar.max = 100
        views.seekBar.progress = drawingStateManager?.currentWidth?.toInt() ?: 50
        views.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    drawingStateManager?.currentWidth = progress.toFloat()
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
    }

    private fun setupColorSelection() {
        views.colorSelectionContainer.removeAllViews()
        val selectionIndicator = View(this)
        selectionIndicator.setBackgroundColor(Color.RED)
        selectionIndicator.layoutParams =
            FrameLayout.LayoutParams(MATCH_PARENT, 10).apply { gravity = CENTER or BOTTOM }

        (0..8).map { Random.nextInt() }.forEach { color ->
            val colorView = FrameLayout(this)
            colorView.setBackgroundColor(color)
            views.colorSelectionContainer.addView(colorView)
            colorView.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 1f
                width = 0
                height = MATCH_PARENT
            }
            colorView.onSingleClick {
                (selectionIndicator.parent as? ViewGroup)?.removeView(selectionIndicator)
                colorView.addView(selectionIndicator)
                drawingStateManager?.currentColor = color
            }
            colorView.setOnLongClickListener {
                setupColorSelection()
                true
            }
        }
        views.colorSelectionContainer.children.toList().random().performClick()
    }

    private fun setupInteraction() {
        views.loadButton.onSingleClick {
            val intent = Intent(Intent.ACTION_PICK)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                intent.type = "image/*"
            } else {
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
            startActivityForResult(intent, GALLERY_INTENT_RESULT)
        }

        views.saveButton.onSingleClick {
            saveImageToGallery()
        }

        views.undoButton.onSingleClick { drawingStateManager?.undo() }
        views.redoButton.onSingleClick { drawingStateManager?.redo() }
        views.clearButton.onSingleClick { drawingStateManager?.clear() }
    }

    private var lastImageURI: Uri? = null

    private fun handleGalleryImage(uri: Uri) {
        lastImageURI = uri
        views.sourceImageView.setImageURI(uri)
        views.sourceImageView.post {
            startDrawing()
        }
    }

    private fun startDrawing() {
        val canvasWidth = views.sourceImageView.measuredWidth
        val canvasHeight = views.sourceImageView.measuredHeight
        views.brushCanvas.updateLayoutParams<FrameLayout.LayoutParams> {
            width = canvasWidth
            height = canvasHeight
        }
        drawingStateManager = DrawingStateManager(canvasWidth, canvasHeight)
        views.brushCanvas.drawingStateManager = drawingStateManager

        setupColorSelection()
        setupWidthSelection()
    }

    private fun saveImageToGallery() {
        val uri = lastImageURI ?: return
        val bitmap = uriToBitmap(uri) ?: return


        val drawing = drawingStateManager?.scaled(bitmap.width, bitmap.height) ?: return
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        drawing.contents.forEach {
            canvas.drawPath(it.drawingCache.path, it.drawingCache.paint)
        }

//        val matrixScaled =
//            drawingStateManager?.matrixScaledCache(bitmap.width, bitmap.height) ?: return
//        matrixScaled.forEach {
//            canvas.drawPath(it.path, it.paint)
//        }

        //quick and dirty, to avoid the permission request. may not work everywhere though
        MediaStore.Images.Media.insertImage(
            contentResolver,
            mutableBitmap,
            "",
            "Edited by AwesomeBrushApp"
        );


        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("The drawing has been saved to your gallery")
            .setPositiveButton("Ok", null)
            .show()
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor?.fileDescriptor ?: return null
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && resultCode == Activity.RESULT_OK && requestCode == GALLERY_INTENT_RESULT) {
            val uri = data.data
            if (uri != null) {
                handleGalleryImage(uri)
            }
        }

    }

    companion object {
        const val GALLERY_INTENT_RESULT = 0
    }
}

inline fun View.onSingleClick(crossinline listener: () -> Unit) {
    setOnClickListener {
        listener.invoke()
        isEnabled = false
        postDelayed({ isEnabled = true }, 200)
    }
}