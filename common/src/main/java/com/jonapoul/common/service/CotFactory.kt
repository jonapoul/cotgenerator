package com.jonapoul.common.service

import android.content.SharedPreferences
import com.jonapoul.common.cot.CursorOnTarget
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.repositories.IBatteryRepository
import com.jonapoul.common.repositories.IDeviceUidRepository

abstract class CotFactory(
        protected val prefs: SharedPreferences,
        protected val buildResources: IBuildResources,
        protected val deviceUidRepository: IDeviceUidRepository,
        protected val gpsRepository: IGpsRepository,
        protected val batteryRepository: IBatteryRepository
) {
    abstract fun generate(): List<CursorOnTarget>
    protected abstract fun initialise(): List<CursorOnTarget>
    protected abstract fun update(): List<CursorOnTarget>
    abstract fun clear()
}
