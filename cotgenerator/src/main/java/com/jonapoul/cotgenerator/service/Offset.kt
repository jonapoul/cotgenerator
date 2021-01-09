package com.jonapoul.cotgenerator.service

import com.jonapoul.cotgenerator.utils.GeometryUtils.arcdistance

data class Offset(
        var R: Double,    /* travel distance in metres */
        var theta: Double, /* bearing in degrees */
) {
    companion object {
        fun from(startPoint: Point): Builder {
            return Builder(startPoint)
        }
    }

    class Builder(private val startPoint: Point) {
        fun to(endPoint: Point): Offset {
            return Offset(
                    R = arcdistance(startPoint, endPoint),
                    theta = Bearing.from(startPoint).to(endPoint)
            )
        }
    }
}