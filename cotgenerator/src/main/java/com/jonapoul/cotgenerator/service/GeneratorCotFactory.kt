package com.jonapoul.cotgenerator.service

import android.content.SharedPreferences
import com.jonapoul.common.CotApplication
import com.jonapoul.common.cot.CotRole
import com.jonapoul.common.cot.CotTeam
import com.jonapoul.common.cot.CursorOnTarget
import com.jonapoul.common.cot.UtcTimestamp
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.prefs.*
import com.jonapoul.common.repositories.IBatteryRepository
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.service.CotFactory
import com.jonapoul.common.utils.Constants
import com.jonapoul.cotgenerator.R
import com.jonapoul.cotgenerator.prefs.GeneratorPrefs
import com.jonapoul.cotgenerator.service.streams.DoubleRandomStream
import com.jonapoul.cotgenerator.service.streams.IRandomStream
import com.jonapoul.cotgenerator.service.streams.IntRandomStream
import com.jonapoul.cotgenerator.service.streams.RadialDistanceRandomStream
import com.jonapoul.cotgenerator.utils.GeometryUtils.arcdistance
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

internal class GeneratorCotFactory @Inject constructor(
        prefs: SharedPreferences,
        buildResources: IBuildResources,
        deviceUidRepository: IDeviceUidRepository,
        gpsRepository: IGpsRepository,
        batteryRepository: IBatteryRepository,
) : CotFactory(prefs, buildResources, deviceUidRepository, gpsRepository, batteryRepository) {

    private data class IconData(var cot: CursorOnTarget, var offset: Offset)

    private val random = Random(System.currentTimeMillis())

    private var useRandomCallsigns: Boolean = false
    private var useIndexedCallsigns: Boolean = false
    private var useRandomTeams: Boolean = false
    private var useRandomRoles: Boolean = false
    private var iconCount: Int = 1
    private var distributionRadius: Double = 0.0
    private var followGps: Boolean = false
    private var centreLat: Double = 0.0
    private var centreLon: Double = 0.0
    private var stayAtGroundLevel: Boolean = false
    private var centreAlt: Double = 0.0
    private var staleTimer: Long = 0
    private var movementSpeed: Double = 0.0
    private var travelDistance: Double = 0.0

    private lateinit var callsigns: List<String>
    private lateinit var distributionCentre: Point
    private var icons = mutableListOf<IconData>()

    override fun clear() {
        icons.clear()
    }

    override fun generate(): List<CursorOnTarget> {
        return try {
            if (icons.isEmpty()) initialise() else update()
        } catch (e: ConcurrentModificationException) {
            ArrayList()
        }
    }

    override fun initialise(): List<CursorOnTarget> {
        grabValuesFromPreferences()
        icons = ArrayList()
        updateDistributionCentre()
        val now = UtcTimestamp.now()
        val distanceItr = weightedRadialIterator()
        val courseItr = doubleIterator(0.0, 360.0)
        val altitudeItr = doubleIterator(centreAlt - distributionRadius, centreAlt + distributionRadius)
        for (i in 0 until iconCount) {
            val cot = CursorOnTarget(buildResources)
            cot.uid = "%s_%04d".format(deviceUidRepository.getUid(), i)
            cot.callsign = callsigns[i]
            cot.start = now
            cot.time = cot.start
            cot.setStaleDiff(staleTimer, TimeUnit.MINUTES)
            cot.team = CotTeam.fromPrefs(prefs, useRandomTeams)
            cot.role = CotRole.fromPrefs(prefs, useRandomRoles)
            cot.speed = movementSpeed
            cot.lat = distributionCentre.lat * Constants.RAD_TO_DEG
            cot.lon = distributionCentre.lon * Constants.RAD_TO_DEG
            cot.hae = initialiseAltitude(altitudeItr)
            cot.battery = batteryRepository.getPercentage()
            val initialOffset = generateInitialOffset(distanceItr, courseItr)
            setPositionFromOffset(cot, initialOffset)
            cot.course = initialOffset.theta
            icons.add(IconData(cot, initialOffset))
        }
        return getCotList()
    }

    override fun update(): List<CursorOnTarget> {
        updateDistributionCentre()
        val now = UtcTimestamp.now()
        val courseItr = doubleIterator(0.0, 360.0)
        icons.forEach {
            it.cot.start = now
            it.cot.time = now
            it.cot.setStaleDiff(staleTimer, TimeUnit.MINUTES)
            it.offset = generateBoundedOffset(courseItr, Point.fromCot(it.cot))
            val oldPoint = Point.fromCot(it.cot)
            setPositionFromOffset(it.cot, it.offset)
            val newPoint = Point.fromCot(it.cot)
            it.cot.course = Bearing.from(oldPoint).to(newPoint)
            it.cot.hae = updateAltitude(it.cot.hae)
            it.cot.battery = batteryRepository.getPercentage()
        }
        return getCotList()
    }

    private fun grabValuesFromPreferences() {
        useRandomCallsigns = prefs.getBooleanFromPair(GeneratorPrefs.RANDOM_CALLSIGNS)
        useIndexedCallsigns = prefs.getBooleanFromPair(GeneratorPrefs.INDEXED_CALLSIGNS)
        useRandomTeams = prefs.getBooleanFromPair(GeneratorPrefs.RANDOM_COLOUR)
        useRandomRoles = prefs.getBooleanFromPair(GeneratorPrefs.RANDOM_ROLE)
        iconCount = prefs.parseIntFromPair(GeneratorPrefs.ICON_COUNT)
        callsigns = getCallsigns()
        distributionRadius = prefs.parseDoubleFromPair(GeneratorPrefs.RADIAL_DISTRIBUTION)
        followGps = prefs.getBooleanFromPair(GeneratorPrefs.FOLLOW_GPS_LOCATION)
        centreLat = prefs.parseDoubleFromPair(GeneratorPrefs.CENTRE_LATITUDE)
        centreLon = prefs.parseDoubleFromPair(GeneratorPrefs.CENTRE_LONGITUDE)
        stayAtGroundLevel = prefs.getBooleanFromPair(GeneratorPrefs.STAY_AT_GROUND_LEVEL)
        centreAlt = if (stayAtGroundLevel) 0.0 else prefs.parseDoubleFromPair(GeneratorPrefs.CENTRE_ALTITUDE)
        staleTimer = prefs.getIntFromPair(CommonPrefs.STALE_TIMER).toLong()
        movementSpeed = prefs.parseDoubleFromPair(GeneratorPrefs.MOVEMENT_SPEED) * Constants.MPH_TO_METRES_PER_SECOND

        /* Stop any fuckery with distribution radii */
        movementSpeed = min(movementSpeed, distributionRadius / 2.0)
        travelDistance = movementSpeed * prefs.getIntFromPair(CommonPrefs.TRANSMISSION_PERIOD)
    }

    private fun getCallsigns(): List<String> {
        val callsigns: MutableList<String> = ArrayList()
        if (useRandomCallsigns) {
            /* Grab the list of all valid callsigns and shuffle it into a random order */
            val resources = CotApplication.context.resources
            val allCallsigns = mutableListOf(*resources.getStringArray(R.array.atakCallsigns))
            allCallsigns.shuffle()
            /* Extract some at random */
            for (i in 0 until iconCount) {
                callsigns.add(allCallsigns[i % allCallsigns.size]) // modulus, just in case iconCount > allCallsigns.size
            }
        } else {
            /* Use custom callsign as entered in the settings */
            val baseCallsign = prefs.getStringFromPair(CommonPrefs.CALLSIGN)
            for (i in 0 until iconCount) {
                val callsign = if (useIndexedCallsigns) "${baseCallsign}-${i}" else baseCallsign
                callsigns.add(callsign)
            }
        }
        return callsigns
    }

    private fun setPositionFromOffset(cot: CursorOnTarget, newOffset: Offset) {
        val (lat, lon) = Point.fromCot(cot).add(newOffset)
        cot.lat = lat * Constants.RAD_TO_DEG
        cot.lon = lon * Constants.RAD_TO_DEG
    }

    private fun updateDistributionCentre() {
        distributionCentre = Point(
                lat = centreLatitudeDegrees() * Constants.DEG_TO_RAD,
                lon = centreLongitudeDegrees() * Constants.DEG_TO_RAD
        )
    }

    private fun generateInitialOffset(distanceItr: IRandomStream<Double>, courseItr: IRandomStream<Double>): Offset {
        return Offset(
                R = distanceItr.next(),
                theta = courseItr.next()
        )
    }

    private fun generateBoundedOffset(
            courseItr: IRandomStream<Double>,
            startPoint: Point,
            attemptsRemaining: Int = MAX_OFFSET_GENERATION_ATTEMPTS,
    ): Offset {
        if (attemptsRemaining == 0) {
            /* The recursive algorithm has failed too many times, so to avoid a stack overflow we just pick
             * a random point along the radius of the centrepoint and generate an Offset that will take us there */
            val offsetToEdge = Offset(travelDistance, courseItr.next())
            val nextPoint = distributionCentre.add(offsetToEdge)
            return Offset.from(startPoint).to(nextPoint)
        } else {
            val offset = Offset(travelDistance, courseItr.next())
            val endPoint = startPoint.add(offset)
            return if (arcdistance(endPoint, distributionCentre) > distributionRadius) {
                /* Invalid offset, so try again */
                generateBoundedOffset(courseItr, startPoint, attemptsRemaining - 1)
            } else {
                offset
            }
        }
    }

    private fun doubleIterator(min: Double, max: Double): IRandomStream<Double> {
        return DoubleRandomStream(random, min, max)
    }

    private fun weightedRadialIterator(): IRandomStream<Double> {
        return RadialDistanceRandomStream(random, distributionRadius)
    }

    private fun centreLatitudeDegrees() = if (followGps) {
        gpsRepository.latitude()
    } else {
        centreLat
    }

    private fun centreLongitudeDegrees() = if (followGps) {
        gpsRepository.longitude()
    } else {
        centreLon
    }

    private fun getCotList(): List<CursorOnTarget> {
        return icons.map { it.cot }
    }

    private fun initialiseAltitude(altitudeIterator: IRandomStream<Double>): Double {
        return if (stayAtGroundLevel) {
            0.0
        } else {
            /* Can't have altitude below 0 */
            max(0.0, altitudeIterator.next())
        }
    }

    private fun updateAltitude(altitude: Double): Double {
        return if (stayAtGroundLevel) {
            0.0
        } else {
            /* Direction is either -1, 0 or +1; representing falling, staying steady or rising respectively */
            val direction = IntRandomStream(random, -1, 1).next()
            var newAltitude = altitude + direction * movementSpeed

            /* Not going below ground */
            if (newAltitude < 0.0) newAltitude = 0.0

            /* Clip within the bounds of the distribution radius*/
            if (newAltitude < centreAlt - distributionRadius) newAltitude = centreAlt - distributionRadius
            if (newAltitude > centreAlt + distributionRadius) newAltitude = centreAlt + distributionRadius
            newAltitude
        }
    }

    private companion object {
        const val MAX_OFFSET_GENERATION_ATTEMPTS = 10
    }
}
