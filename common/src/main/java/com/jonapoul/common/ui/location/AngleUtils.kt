package com.jonapoul.common.ui.location

internal object AngleUtils {
    fun getDirection(angleDegrees: Double): String {
        return when {
            angleDegrees < 11.25 -> "N"
            angleDegrees < 33.75 -> "NNE"
            angleDegrees < 56.25 -> "NE"
            angleDegrees < 78.75 -> "ENE"
            angleDegrees < 101.25 -> "E"
            angleDegrees < 123.75 -> "SEE"
            angleDegrees < 146.25 -> "SE"
            angleDegrees < 168.75 -> "SSE"
            angleDegrees < 191.25 -> "S"
            angleDegrees < 213.75 -> "SSW"
            angleDegrees < 236.25 -> "SW"
            angleDegrees < 258.75 -> "WSW"
            angleDegrees < 281.25 -> "W"
            angleDegrees < 303.75 -> "WNW"
            angleDegrees < 326.25 -> "NW"
            angleDegrees < 348.75 -> "NNW"
            else -> "N"
        }
    }
}