package com.jonapoul.common.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup

/**
 * A simple base class to hold the logic for dealing with inputting preferences in landscape mode.
 * This is shared between all preference fragments.
 *
 * This is needed because when we edit an [EditTextPreference] in landscape mode, a different
 * layout pops up which a) looks bad, and b) is inconsistent with "number_input_preference_dialog",
 * which is applied to all numeric preferences.
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    /**
     * Once we've inflated the preference resource file, iterate through the preferences to tell
     * all [EditTextPreference]s that we do not want to display in fullscreen editing mode.
     *
     * THIS SUPER-METHOD NEEDS TO BE CALLED ***AFTER*** [setPreferencesFromResource]!
     */
    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        blockFullScreenEditTextRecursive(preferenceScreen)
    }

    /**
     * Grab all children of the given [PreferenceGroup], then either dive in deeper or block the
     * fullscreen access - depending on each child's type.
     */
    private fun blockFullScreenEditTextRecursive(preferenceGroup: PreferenceGroup) {
        for (i in 0 until preferenceGroup.preferenceCount) {
            val child = preferenceGroup.getPreference(i)
            if (child is PreferenceGroup) {
                /* This is another PreferenceGroup, so dive deeper to work its children too */
                blockFullScreenEditTextRecursive(child)
            } else if (child is EditTextPreference) {
                child.setOnBindEditTextListener {
                    /* Block it from going fullscreen */
                    it.imeOptions = it.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
                }
            }
        }
    }
}
