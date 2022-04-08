package ly.img.awesomebrushapplication.drawing

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.os.FileUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import ly.img.awesomebrushapplication.models.Drawing
import ly.img.awesomebrushapplication.models.Point
import ly.img.awesomebrushapplication.models.Stroke
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

const val SMOOTH_VAL = 3
const val distanceThresholdPercent: Float = 0.01f

class DrawingStateManager(
    private val viewPortWidth: Int,
    private val viewPortHeight: Int,
) {
    var stateConsumer: (List<Stroke>) -> Unit = {}
    var currentColor = Color.BLACK
    var currentWidth = 50f

    private val distanceThreshold = viewPortWidth * distanceThresholdPercent

    private val defaultPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var drawing = Drawing(mutableListOf(), viewPortWidth, viewPortHeight)
    private val currentState get() = drawing.contents
    private val currentStroke get() = currentState.lastOrNull()

    private var ongoingStroke = false

    val redoState = mutableListOf<Stroke>()

    private val canUndo: Boolean get() = currentState.isNotEmpty()

    private val canRedo: Boolean get() = redoState.isNotEmpty()

    fun undo() {
        if (!canUndo) {
            return
        }
        redoState.add(currentState.removeLast())
        refresh()
    }

    fun redo() {
        if (!canRedo) {
            return
        }
        currentState.add(redoState.removeLast())
        refresh()
    }

    fun clear() {
        drawing.contents.clear()
        clearRedo()
        refresh()
    }

    private fun clearRedo() {
        redoState.clear()
    }

    private fun refresh() {
        stateConsumer(drawing.contents)
    }

    private fun shouldAddPoint(x: Float, y: Float): Boolean {
        val currentStroke = currentStroke ?: return true
        val lastPoint = currentStroke.points.lastOrNull() ?: return true
        return lastPoint
            .distanceTo(x, y) > distanceThreshold
    }

    fun addPoint(x: Float, y: Float, force: Boolean = false) {
        if (!ongoingStroke) {
            addNewStroke(Point(x, y))
            ongoingStroke = true
        }
        if (!force && !shouldAddPoint(x, y)) {
            return
        }
        val points = currentStroke?.points ?: return
        val path = currentStroke?.drawingCache?.path ?: return
        points.add(Point(x, y))
        rebuildPath(points, path)
        refresh()
    }

    private fun rebuildPath(allPoints: List<Point>, path: Path) {
        path.rewind()
        allPoints.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, point.y)
                if (allPoints.size == 1) {
                    path.lineTo(point.x, point.y)
                }
            } else {
                val lastPoint = allPoints[index - 1]
                val nextPoint = allPoints.getOrNull(index + 1)
                val beforeLastPoint = allPoints.getOrElse(index - 2) { lastPoint }

                val pointDx: Float
                val pointDy: Float
                if (nextPoint == null) {
                    pointDx = (point.x - lastPoint.x) / SMOOTH_VAL
                    pointDy = (point.y - lastPoint.y) / SMOOTH_VAL
                } else {
                    pointDx = (nextPoint.x - lastPoint.x) / SMOOTH_VAL
                    pointDy = (nextPoint.y - lastPoint.y) / SMOOTH_VAL
                }

                val lastPointDx = (point.x - beforeLastPoint.x) / SMOOTH_VAL
                val lastPointDy = (point.y - beforeLastPoint.y) / SMOOTH_VAL

                path.cubicTo(
                    lastPoint.x + lastPointDx,
                    lastPoint.y + lastPointDy,
                    point.x - pointDx,
                    point.y - pointDy,
                    point.x,
                    point.y
                )
            }
        }
    }

    fun endStroke(x: Float, y: Float) {
        addPoint(x, y, true)
        ongoingStroke = false
        refresh()
    }

    private fun addNewStroke(startPoint: Point) {
        clearRedo()
        drawing.contents.add(
            Stroke(
                currentColor,
                currentWidth,
                mutableListOf(startPoint),
                DrawingCache(
                    Path(),
                    Paint(defaultPaint).apply {
                        strokeWidth = currentWidth
                        color = currentColor
                    })
            )
        )
    }

    fun scaled(newWidth: Int, newHeight: Int): Drawing {
        val scale = newWidth.toFloat() / viewPortWidth

        val scaledStrokes = currentState.map { stroke ->
            val scaledPoints = stroke.points.map { point ->
                Point(point.x * scale, point.y * scale)
            }.toMutableList()

            val path = Path()
            rebuildPath(scaledPoints, path)

            Stroke(
                stroke.color,
                stroke.width * scale,
                scaledPoints,
                DrawingCache(path, stroke.drawingCache.paint.apply { strokeWidth *= scale })
            )
        }.toMutableList()
        return Drawing(scaledStrokes, newWidth, newHeight)
    }

    fun matrixScaledCache(newWidth: Int, newHeight: Int): List<DrawingCache> {
        val scale = newWidth.toFloat() / viewPortWidth
        return currentState.map {
            val path = it.drawingCache.path
            path.transform(Matrix().apply { setScale(scale, scale) })
            DrawingCache(path, it.drawingCache.paint.apply { strokeWidth *= scale })
        }
    }

    fun persistState() {
        TODO()
    }

    companion object {
        private val savedDrawingFileName = "drawing.aba"
        fun loadPersistedState(): DrawingStateManager {
            TODO()
        }

    }
}