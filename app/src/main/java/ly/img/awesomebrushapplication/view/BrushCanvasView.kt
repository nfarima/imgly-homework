package ly.img.awesomebrushapplication.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import ly.img.awesomebrushapplication.drawing.DrawingStateManager
import ly.img.awesomebrushapplication.drawing.DrawingCache

class BrushCanvasView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ready get() = drawingStateManager != null

    var drawingStateManager: DrawingStateManager? = null
        set(value) {
            field = value
            field?.stateConsumer = { strokes ->
                cache = strokes.map { stroke -> stroke.drawingCache }
                invalidate()
            }
            invalidate()
        }

    private var cache = listOf<DrawingCache>()

    init {
        setWillNotDraw(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!ready) {
            return super.onTouchEvent(event)
        }
        val drawingStateManager = drawingStateManager ?: return super.onTouchEvent(event)
        event ?: return super.onTouchEvent(event)

        when (event.action) {
            ACTION_DOWN -> {
                drawingStateManager.addPoint(event.x, event.y)
                return true
            }
            ACTION_UP -> {
                drawingStateManager.endStroke(event.x, event.y)
                return true
            }
            ACTION_MOVE -> {
                drawingStateManager.addPoint(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun updatePaths() {
        // To get a very smooth path we do not simply want to draw lines between two consecutive points,
        // but rather draw a cubic Bezier curve between two consecutive points through two calculated control
        // points. The control points are calculated based on the previous point and the next point, which
        // means that you always have to draw one point in the past.
        //
        // Imagine the user is drawing on screen and as the user drags his finger around on the screen, you receive
        // multiple points. The last point that you receive is point P4. The point that you received prior to that 
        // is point P3 and so on. Now in order to get a smooth drawing, you'll want to draw a cubic Bezier curve between
        // P2 and P3 through control points that are calculated using P1 and P4.
        // 
        // This also means that in order to actually reach the last point that you've received (P4 in the above scenario),
        // you'll have to draw once more **after** the user's finger has already left the screen.
        //
        // If the user only taps on the screen instead of dragging their finger around, you should draw a point.

        // The algorithm below implements the described behavior from above. You only need to fetch the appropriate
        // points from your custom data structure.

        // Note: this should also be replaced by your custom data structure that stores points.
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        cache.forEach {
            canvas.drawPath(it.path, it.paint)
        }
        // TODO: If there is time left, try to implement a cache and draw only the last line instead of everything.
    }
}