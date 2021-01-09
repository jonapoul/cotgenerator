package com.jonapoul.cotgenerator.service

import com.jonapoul.common.cot.CursorOnTarget
import com.jonapoul.common.utils.Constants
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Point(
        var lat: Double, // radians
        var lon: Double, // radians
) {
    fun add(offset: Offset): Point {
        val rOverR = offset.R / Constants.EARTH_RADIUS_METRES
        val theta = Math.toRadians(offset.theta) // travel bearing in radians
        val lat1 = lat // start latitude in radians
        val lon1 = lon // start longitude in radians
        val lat2 = asin(sin(lat1) * cos(rOverR) + cos(lat1) * sin(rOverR) * cos(theta))
        val lon2 = lon1 + atan2(sin(theta) * sin(rOverR) * cos(lat1), cos(rOverR) - sin(lat1) * sin(lat2))
        return Point(lat2, lon2)
    }

    companion object {
        fun fromCot(cot: CursorOnTarget): Point {
            return Point(
                    cot.lat * Constants.DEG_TO_RAD,
                    cot.lon * Constants.DEG_TO_RAD
            )
        }
    }
}
