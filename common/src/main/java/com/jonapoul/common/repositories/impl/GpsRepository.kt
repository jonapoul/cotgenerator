package com.jonapoul.common.repositories.impl

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.utils.MinimumVersions.HEADING_ACCURACY
import com.jonapoul.common.utils.VersionUtils
import timber.log.Timber
import javax.inject.Inject

class GpsRepository @Inject constructor() : IGpsRepository {
    private val lock = Any()
    private val lastLocation = MutableLiveData<Location?>().also { it.value = null }

    private var lastUpdateTimeMs = 0L
    private var idleMode = false

    override fun onIdleModeChanged(idleModeActive: Boolean) {
        idleMode = idleModeActive
    }

    override fun idleModeActive(): Boolean {
        return idleMode
    }

    override fun setLocation(location: Location) {
        synchronized(lock) {
            Timber.d("Updating GPS to %f %f", location.latitude, location.longitude)
            lastUpdateTimeMs = System.currentTimeMillis()
            lastLocation.value = location
        }
    }

    override fun getLocation(): LiveData<Location?> {
        return lastLocation
    }

    override fun getLastUpdateTime(): Long {
        return lastUpdateTimeMs
    }

    override fun latitude() = lastLocation.value?.latitude ?: ZERO

    override fun longitude() = lastLocation.value?.longitude ?: ZERO

    override fun altitude() = lastLocation.value?.altitude ?: ZERO

    override fun bearing(): Double {
        return if (lastLocation.value?.hasBearing() == true) {
            lastLocation.value?.bearing?.toDouble() ?: ZERO
        } else {
            ZERO
        }
    }

    override fun speed(): Double {
        return if (lastLocation.value?.hasSpeed() == true) {
            lastLocation.value?.speed?.toDouble() ?: ZERO
        } else {
            ZERO
        }
    }

    override fun circularError90(): Double {
        return if (lastLocation.value?.hasAccuracy() == true) {
            lastLocation.value?.accuracy?.toDouble() ?: UNKNOWN
        } else {
            UNKNOWN
        }
    }

    override fun linearError90(): Double {
        return if (VersionUtils.isAtLeast(HEADING_ACCURACY) && lastLocation.value?.hasVerticalAccuracy() == true) {
            lastLocation.value?.verticalAccuracyMeters?.toDouble() ?: UNKNOWN
        } else {
            UNKNOWN
        }
    }

    override fun hasGpsFix(): Boolean {
        return lastLocation.value != null
    }

    companion object {
        private const val UNKNOWN: Double = 99999999.0
        private const val ZERO: Double = 0.0
    }
}
