package com.jonapoul.common.ui.location

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.sharedprefs.getStringFromPair

enum class CoordinateFormat {
    DD, DM, DMS, MGRS;

    companion object {
        @SuppressLint("DefaultLocale")
        fun fromString(str: String): CoordinateFormat {
            return values().firstOrNull { str.equals(it.name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown coordinate format '$str'")
        }

        fun fromPrefs(prefs: SharedPreferences): CoordinateFormat {
            return fromString(
                prefs.getStringFromPair(CommonPrefs.LOCATION_COORDINATE_FORMAT)
            )
        }

        fun getNext(format: CoordinateFormat): CoordinateFormat {
            val index = values().indexOf(format)
            return if (index == values().size - 1) values()[0] else values()[index + 1]
        }
    }
}