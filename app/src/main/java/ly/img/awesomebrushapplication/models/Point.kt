package ly.img.awesomebrushapplication.models

import kotlin.math.hypot

class Point(val x: Float, val y: Float) {
    fun distanceTo(other: Point): Float {
        return hypot(x - other.x, y - other.y)
    }

    fun distanceTo(otherX: Float, otherY: Float): Float {
        return hypot(x - otherX, y - otherY)
    }
}