package com.jonapoul.cotgenerator.ui

import android.content.SharedPreferences
import androidx.preference.Preference
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.prefs.getBooleanFromPair
import com.jonapoul.common.ui.main.SettingsFragment
import com.jonapoul.common.utils.InputValidator
import com.jonapoul.cotgenerator.prefs.GeneratorPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GeneratorSettingsFragment @Inject constructor() : SettingsFragment() {

    override fun getPhoneInputKeys() = super.getPhoneInputKeys().apply {
        add(GeneratorPrefs.ICON_COUNT.key)
        add(GeneratorPrefs.MOVEMENT_SPEED.key)
        add(GeneratorPrefs.RADIAL_DISTRIBUTION.key)
        add(GeneratorPrefs.CENTRE_LATITUDE.key)
        add(GeneratorPrefs.CENTRE_LONGITUDE.key)
        add(GeneratorPrefs.CENTRE_ALTITUDE.key)
    }

    override fun getSuffixes() = super.getSuffixes().apply {
        put(GeneratorPrefs.CENTRE_LATITUDE.key, "degrees")
        put(GeneratorPrefs.CENTRE_LONGITUDE.key, "degrees")
        put(GeneratorPrefs.MOVEMENT_SPEED.key, "mph")
        put(GeneratorPrefs.RADIAL_DISTRIBUTION.key, "metres")
        put(GeneratorPrefs.CENTRE_ALTITUDE.key, "metres")
    }

    override fun getPrefValidationRationales() = super.getPrefValidationRationales().apply {
        put(GeneratorPrefs.ICON_COUNT.key, "Should be an integer from 1 to 9999")
        put(GeneratorPrefs.CENTRE_LATITUDE.key, "Should be a number between -90 and +90")
        put(GeneratorPrefs.CENTRE_LONGITUDE.key, "Should be a number between -180 and +180")
        put(GeneratorPrefs.RADIAL_DISTRIBUTION.key, "Should be a positive integer")
        put(GeneratorPrefs.MOVEMENT_SPEED.key, "Should be a positive number")
        put(GeneratorPrefs.CENTRE_ALTITUDE.key, "Should be an integer between 0 and 50,000")
    }

    override fun updatePreferences() {
        /* If any toggles are enabled, hide the accompanying custom setting boxes */
        toggleCallsignSettingVisibility()
        toggleColourPickerVisibility()
        toggleRoleSettingVisibility()
        toggleLatLonSettingsVisibility()
        toggleAltitudeSettingVisibility()

        /* Fetch presets from the database */
        super.updatePreferences()
    }

    private fun toggleCallsignSettingVisibility() {
        val randomCallsignEnabled = prefs.getBooleanFromPair(GeneratorPrefs.RANDOM_CALLSIGNS)
        setPrefVisibleIfCondition(CommonPrefs.CALLSIGN, !randomCallsignEnabled)
        setPrefVisibleIfCondition(GeneratorPrefs.INDEXED_CALLSIGNS, !randomCallsignEnabled)
    }

    private fun toggleColourPickerVisibility() {
        /* The Colour Picker option should only be visible if Random Colours is disabled  */
        val randomColoursEnabled = prefs.getBooleanFromPair(GeneratorPrefs.RANDOM_COLOUR)
        setPrefVisibleIfCondition(CommonPrefs.TEAM_COLOUR, !randomColoursEnabled)
    }

    private fun toggleRoleSettingVisibility() {
        val randomRoleEnabled = prefs.getBooleanFromPair(GeneratorPrefs.RANDOM_ROLE)
        setPrefVisibleIfCondition(CommonPrefs.ICON_ROLE, !randomRoleEnabled)
    }

    private fun toggleLatLonSettingsVisibility() {
        val followGps = prefs.getBooleanFromPair(GeneratorPrefs.FOLLOW_GPS_LOCATION)
        setPrefVisibleIfCondition(GeneratorPrefs.CENTRE_LATITUDE, !followGps)
        setPrefVisibleIfCondition(GeneratorPrefs.CENTRE_LONGITUDE, !followGps)
    }

    private fun toggleAltitudeSettingVisibility() {
        val showAltitudeSetting = prefs.getBooleanFromPair(GeneratorPrefs.STAY_AT_GROUND_LEVEL)
        setPrefVisibleIfCondition(GeneratorPrefs.CENTRE_ALTITUDE, !showAltitudeSetting)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        super.onSharedPreferenceChanged(prefs, key)
        when (key) {
            GeneratorPrefs.RANDOM_CALLSIGNS.key -> toggleCallsignSettingVisibility()
            GeneratorPrefs.RANDOM_COLOUR.key -> toggleColourPickerVisibility()
            GeneratorPrefs.RANDOM_ROLE.key -> toggleRoleSettingVisibility()
            GeneratorPrefs.FOLLOW_GPS_LOCATION.key -> toggleLatLonSettingsVisibility()
            GeneratorPrefs.STAY_AT_GROUND_LEVEL.key -> toggleAltitudeSettingVisibility()
        }
    }

    override fun onPreferenceChange(pref: Preference, newValue: Any): Boolean {
        /* Check the base preferences first, but back out if they fail */
        if (!super.onPreferenceChange(pref, newValue)) {
            return false
        }
        val input = newValue as String
        val inputValidator = InputValidator()
        return when (val key = pref.key) {
            GeneratorPrefs.ICON_COUNT.key -> errorIfInvalid(input, key, inputValidator.validateInt(input, min = 1, max = 9999))
            GeneratorPrefs.CENTRE_LATITUDE.key -> errorIfInvalid(input, key, inputValidator.validateDouble(input, min = -90.0, max = 90.0))
            GeneratorPrefs.CENTRE_LONGITUDE.key -> errorIfInvalid(input, key, inputValidator.validateDouble(input, min = -180.0, max = 180.0))
            GeneratorPrefs.RADIAL_DISTRIBUTION.key -> errorIfInvalid(input, key, inputValidator.validateInt(input, min = 1))
            GeneratorPrefs.MOVEMENT_SPEED.key -> errorIfInvalid(input, key, inputValidator.validateDouble(input, min = 0.0))
            GeneratorPrefs.CENTRE_ALTITUDE.key -> errorIfInvalid(input, key, inputValidator.validateInt(input, min = 0, max = 50000))
            else -> true
        }
    }
}
