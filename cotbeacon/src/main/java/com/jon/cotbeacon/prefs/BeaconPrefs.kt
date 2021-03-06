package com.jon.cotbeacon.prefs

import com.jon.common.prefs.PrefPair
import com.jon.cotbeacon.R

internal object BeaconPrefs {
    val ENABLE_CHAT = PrefPair.bool(R.string.key_enable_chat, R.bool.def_enable_chat)
    val LAUNCH_FROM_BOOT = PrefPair.bool(R.string.key_launch_from_boot, R.bool.def_launch_from_boot)
    val LAUNCH_FROM_OPEN = PrefPair.bool(R.string.key_launch_from_open, R.bool.def_launch_from_open)
}
