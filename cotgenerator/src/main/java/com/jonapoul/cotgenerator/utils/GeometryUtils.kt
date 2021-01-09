package com.jonapoul.cotgenerator.utils

import com.jonapoul.common.utils.Constants
import com.jonapoul.cotgenerator.service.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeometryUtils {
    fun arcdistance(p1: Point, p2: Point): Double {
        val lat1 = p1.lat
        val lat2 = p2.lat
        val dlat = p2.lat - p1.lat
        val dlon = p2.lon - p1.lon
        /* I can feel myself getting sweaty just looking at this */
        val a = sin(dlat / 2.0) * sin(dlat / 2.0) + cos(lat1) * cos(lat2) * sin(dlon / 2.0) * sin(dlon / 2.0)
        return 2.0 * Constants.EARTH_RADIUS_METRES * atan2(sqrt(a), sqrt(1.0 - a))
    }
}
