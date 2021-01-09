package com.jonapoul.cotbeacon.ui

import android.os.Bundle
import androidx.preference.EditTextPreference
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.ui.main.SettingsFragment
import com.jonapoul.cotbeacon.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BeaconSettingsFragment @Inject constructor() : SettingsFragment() {

    override fun onCreatePreferences(savedState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedState, rootKey)

        /* If callsign hasn't previously been configured, pull a random ATAK default callsign and
         * assign it to this beacon. */
        if (prefs.getString(CommonPrefs.CALLSIGN.key, null) == null) {
            val atakDefaultCallsigns = resources.getStringArray(R.array.atakCallsigns)
            val randomCallsign = atakDefaultCallsigns.random()
            findPreference<EditTextPreference>(CommonPrefs.CALLSIGN.key)?.text = randomCallsign
            prefs.edit()
                    .putString(CommonPrefs.CALLSIGN.key, randomCallsign)
                    .apply()
        }
    }
}
