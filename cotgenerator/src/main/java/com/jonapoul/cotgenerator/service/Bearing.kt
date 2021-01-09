package com.jonapoul.cotgenerator.service

import com.jonapoul.common.utils.Constants
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object Bearing {
    fun from(startPoint: Point): Builder {
        return Builder(startPoint)
    }

    class Builder(private val startPoint: Point) {
        fun to(endPoint: Point): Double {
            val y = sin(endPoint.lon - startPoint.lon) * cos(endPoint.lat)
            val x = cos(startPoint.lat) * sin(endPoint.lat) -
                    sin(startPoint.lat) * cos(endPoint.lat) * cos(endPoint.lon - startPoint.lon)
            return (atan2(y, x) * Constants.RAD_TO_DEG + 360.0) % 360.0
        }
    }
}
