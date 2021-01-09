package com.jonapoul.cotbeacon

import com.jonapoul.common.CotApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BeaconApplication : CotApplication() {
    override fun onCreate() {
        super.onCreate()

        /* Implementation details TBC */
    }

    companion object {
        var chatFragmentIsVisible = false
    }
}
