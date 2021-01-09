package com.jonapoul.cotbeacon.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.utils.MinimumVersions.IS_DEVICE_IDLE_MODE
import com.jonapoul.common.utils.VersionUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DozeReceiver : BroadcastReceiver() {
    @Inject
    lateinit var gpsRepository: IGpsRepository

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.i("onReceive")
        if (intent?.action == PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) {
            postDozeModeValue(context, gpsRepository)
        }
    }

    fun postDozeModeValue(context: Context, gpsRepository: IGpsRepository) {
        if (VersionUtils.isAtLeast(IS_DEVICE_IDLE_MODE)) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            Timber.i("isDeviceIdleMode = ${powerManager.isDeviceIdleMode}")
            gpsRepository.onIdleModeChanged(powerManager.isDeviceIdleMode)
        }
    }
}
