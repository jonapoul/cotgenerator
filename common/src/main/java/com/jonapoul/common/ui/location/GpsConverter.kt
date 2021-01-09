package com.jonapoul.common.ui.location

import android.location.Location
import com.jonapoul.common.utils.MinimumVersions.HEADING_ACCURACY
import com.jonapoul.common.utils.VersionUtils
import org.opensextant.geodesy.Angle
import org.opensextant.geodesy.Latitude
import org.opensextant.geodesy.Longitude
import org.opensextant.geodesy.MGRS
import kotlin.math.abs

internal class GpsConverter {
    data class Converted(
            val latitude: String,
            val longitude: String,
            val mgrs: String,
            var positionalError: String = UNKNOWN,
            var altitudeWgs84: String = UNKNOWN,
            var speedMetresPerSec: String = UNKNOWN,
            var bearing: String = UNKNOWN,
    )

    fun convertCoordinates(location: Location?, format: CoordinateFormat): Converted {
        if (location == null) {
            return Converted(
                    latitude = UNKNOWN,
                    longitude = UNKNOWN,
                    mgrs = UNKNOWN
            )
        }
        val latitude = Latitude(location.latitude, Angle.DEGREES)
        val longitude = Longitude(location.longitude, Angle.DEGREES)
        return when (format) {
            CoordinateFormat.DD -> dd(latitude, longitude)
            CoordinateFormat.DM -> dm(latitude, longitude)
            CoordinateFormat.DMS -> dms(latitude, longitude)
            else -> dd(latitude, longitude)
        }.also {
            it.positionalError = formatPositionalError(location)
            it.altitudeWgs84 = formatAltitude(location)
            it.speedMetresPerSec = formatSpeed(location)
            it.bearing = formatBearing(location)
        }
    }

    fun coordinatesToCopyableString(location: Location?, coordinateFormat: CoordinateFormat): String {
        if (location == null) {
            return UNKNOWN
        }
        val coords = convertCoordinates(location, coordinateFormat)
        return when (coordinateFormat) {
            CoordinateFormat.MGRS -> coords.mgrs
            else -> "${coords.latitude.trim()}, ${coords.longitude.trim()}"
        }
    }

    private fun dd(latitude: Latitude, longitude: Longitude): Converted {
        return Converted(
                latitude = "%3.6f".format(latitude.inDegrees()),
                longitude = "%3.6f".format(longitude.inDegrees()),
                mgrs = latLonToMgrs(latitude, longitude),
        )
    }

    private fun dm(latitude: Latitude, longitude: Longitude): Converted {
        return Converted(
                latitude = degreesToDm(latitude.inDegrees()),
                longitude = degreesToDm(longitude.inDegrees()),
                mgrs = latLonToMgrs(latitude, longitude),
        )
    }

    private fun dms(latitude: Latitude, longitude: Longitude): Converted {
        return Converted(
                latitude = degreesToDms(latitude.inDegrees()),
                longitude = degreesToDms(longitude.inDegrees()),
                mgrs = latLonToMgrs(latitude, longitude),
        )
    }

    private fun degreesToDm(decimalDegrees: Double): String {
        val degrees = decimalDegrees.toInt()
        val minutes = abs(decimalDegrees - degrees) * 60
        return "%3d° %2.4f'".format(degrees, minutes)
    }

    private fun degreesToDms(decimalDegrees: Double): String {
        val degrees = decimalDegrees.toInt()
        val minutes = (abs(decimalDegrees - degrees) * 60).toInt()
        val seconds = abs(decimalDegrees - degrees) * 60 - minutes
        return "%3d° %2d' %2.2f\"".format(degrees, minutes, seconds)
    }

    private fun latLonToMgrs(latitude: Latitude, longitude: Longitude): String {
        /* This comes in the format '30UAB1234567890', so we want to space it out into the various sections
         * to make it more readable */
        val squished = MGRS(longitude, latitude).toString()
        val zone = squished.substring(0, 3)
        val square = squished.substring(3, 5)
        val easting = squished.substring(5, 10)
        val northing = squished.substring(10)
        return "$zone $square $easting $northing"
    }

    companion object {
        private const val NA = "N/A"
        private const val UNKNOWN = "???"

        private fun formatPositionalError(location: Location): String {
            return if (location.hasAccuracy()) {
                "± %.0f m".format(location.accuracy)
            } else {
                UNKNOWN
            }
        }

        private fun formatSpeed(location: Location): String {
            return if (location.hasSpeed()) {
                val accuracy = if (VersionUtils.isAtLeast(HEADING_ACCURACY) && location.hasSpeedAccuracy()) {
                    " ± %.1f".format(abs(location.speedAccuracyMetersPerSecond))
                } else ""
                "%.1f%s m/s".format(location.speed, accuracy)
            } else {
                UNKNOWN
            }
        }

        private fun formatAltitude(location: Location): String {
            return if (location.hasAltitude()) {
                val accuracy = if (VersionUtils.isAtLeast(HEADING_ACCURACY) && location.hasVerticalAccuracy()) {
                    " ± %.1f".format(abs(location.verticalAccuracyMeters))
                } else ""
                return "%.1f%sm".format(location.altitude, accuracy)
            } else {
                UNKNOWN
            }
        }

        private fun formatBearing(location: Location): String {
            if (location.hasSpeed() && abs(location.speed) < 0.1) {
                /* Not moving, so bearing has no meaning */
                return NA
            }
            return if (location.hasBearing()) {
                val accuracy = if (VersionUtils.isAtLeast(HEADING_ACCURACY) && location.hasBearingAccuracy()) {
                    " ± %.1f".format(abs(location.bearingAccuracyDegrees))
                } else ""
                return "%.1f%s° %s".format(location.bearing, accuracy, AngleUtils.getDirection(location.bearing.toDouble()))
            } else {
                UNKNOWN
            }
        }
    }
}
