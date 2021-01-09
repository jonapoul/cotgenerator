package com.jonapoul.common.utils

import android.content.SharedPreferences
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.sharedprefs.getStringFromPair

enum class DataFormat(private val string: String) {
    XML("XML"),
    PROTOBUF("Protobuf");

    override fun toString(): String = string

    companion object {
        fun fromPrefs(prefs: SharedPreferences): DataFormat {
            return fromString(
                prefs.getStringFromPair(CommonPrefs.DATA_FORMAT)
            )
        }

        fun fromString(formatString: String): DataFormat {
            return values().first { it.toString() == formatString }
        }
    }
}
