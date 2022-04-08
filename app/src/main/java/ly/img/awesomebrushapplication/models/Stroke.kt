package ly.img.awesomebrushapplication.models

import android.graphics.Color
import ly.img.awesomebrushapplication.drawing.DrawingCache

data class Stroke(
    val color: Int = Color.BLACK,
    val width: Float = 1f,
    val points: MutableList<Point> = mutableListOf(),
    val drawingCache: DrawingCache
)