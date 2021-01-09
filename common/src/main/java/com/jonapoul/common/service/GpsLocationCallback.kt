package com.jonapoul.common.service

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.jonapoul.common.repositories.IGpsRepository
import javax.inject.Inject

internal class GpsLocationCallback @Inject constructor(private val gpsRepository: IGpsRepository) : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
        locationResult.lastLocation?.let { gpsRepository.setLocation(it) }
    }
}
