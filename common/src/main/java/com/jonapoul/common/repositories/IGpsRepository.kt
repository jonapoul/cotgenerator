package com.jonapoul.common.repositories

import android.location.Location
import androidx.lifecycle.LiveData

interface IGpsRepository {
    fun onIdleModeChanged(idleModeActive: Boolean)
    fun idleModeActive(): Boolean
    fun setLocation(location: Location)
    fun getLocation(): LiveData<Location?>
    fun getLastUpdateTime(): Long
    fun latitude(): Double
    fun longitude(): Double
    fun altitude(): Double
    fun bearing(): Double
    fun speed(): Double
    fun circularError90(): Double
    fun linearError90(): Double
    fun hasGpsFix(): Boolean
}
