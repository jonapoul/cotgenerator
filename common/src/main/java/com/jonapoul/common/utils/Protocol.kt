package com.jonapoul.common.utils

import android.content.SharedPreferences
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.sharedprefs.PrefPair
import com.jonapoul.sharedprefs.getStringFromPair

enum class Protocol(
    private val string: String,
    val presetPref: PrefPair<String>,
) {
    SSL(
        string = "SSL",
        presetPref = CommonPrefs.SSL_PRESETS
    ),
    TCP(
        string = "TCP",
        presetPref = CommonPrefs.TCP_PRESETS
    ),
    UDP(
        string = "UDP",
        presetPref = CommonPrefs.UDP_PRESETS
    );

    override fun toString(): String = string

    companion object {
        fun fromPrefs(prefs: SharedPreferences): Protocol {
            val protocolString = prefs.getStringFromPair(CommonPrefs.TRANSMISSION_PROTOCOL)
            return fromString(protocolString)
        }

        fun fromString(protocolString: String): Protocol {
            return values().firstOrNull { it.toString().equals(protocolString, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown protocol: $protocolString")
        }
    }
}
