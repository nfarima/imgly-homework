package ly.img.awesomebrushapplication.models

import android.graphics.Color

class Drawing(
    val contents: MutableList<Stroke> = mutableListOf(),
    val width: Int,
    val height: Int
)