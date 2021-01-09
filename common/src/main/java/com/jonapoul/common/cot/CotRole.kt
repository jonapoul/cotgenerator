package com.jonapoul.common.cot

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.sharedprefs.getStringFromPair
import java.util.*

enum class CotRole(val string: String) {
    TEAM_MEMBER("Team Member"),
    TEAM_LEADER("Team Leader"),
    HQ("HQ"),
    SNIPER("Sniper"),
    MEDIC("Medic"),
    FORWARD_OBSERVER("Forward Observer"),
    RTO("RTO"),
    K9("K9");

    override fun toString() = string

    companion object {
        private val random = Random()

        @SuppressLint("DefaultLocale")
        fun fromString(roleString: String): CotRole {
            return values().firstOrNull { it.toString().toLowerCase() == roleString.toLowerCase() }
                    ?: throw IllegalArgumentException("Unknown CoT role: $roleString")
        }

        fun fromPrefs(prefs: SharedPreferences, isRandom: Boolean = false): CotRole {
            return if (isRandom) {
                values()[random.nextInt(values().size)]
            } else {
                fromString(roleString = prefs.getStringFromPair(CommonPrefs.ICON_ROLE))
            }
        }
    }
}
