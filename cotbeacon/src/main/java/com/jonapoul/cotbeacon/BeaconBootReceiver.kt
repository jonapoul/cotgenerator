package com.jonapoul.cotbeacon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.prefs.getBooleanFromPair
import com.jonapoul.common.service.CotService
import com.jonapoul.common.utils.Notify
import com.jonapoul.common.utils.VersionUtils
import com.jonapoul.cotbeacon.prefs.BeaconPrefs
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BeaconBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var buildResources: IBuildResources

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("onReceive %s", intent?.action)
        try {
            val launchFromBootEnabled = prefs.getBooleanFromPair(BeaconPrefs.LAUNCH_FROM_BOOT)
            Timber.d("launchFromBootEnabled = %s", launchFromBootEnabled)
            if (launchFromBootEnabled && intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                val serviceIntent = Intent(context, buildResources.serviceClass).apply {
                    action = CotService.START_SERVICE
                }
                if (VersionUtils.isAtLeast(26)) {
                    Timber.d("Starting foreground service")
                    context.startForegroundService(serviceIntent)
                } else {
                    Timber.d("Starting regular service?")
                    context.startService(serviceIntent)
                }
                Notify.toast(context, "Started transmitting location via CoT Beacon!")
            }
        } catch (e: Exception) {
            Timber.e(e)
            Notify.toast(context, "Error when launching CoT Beacon on device boot!")
        }
    }
}
