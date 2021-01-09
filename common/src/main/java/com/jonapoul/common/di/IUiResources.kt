package com.jonapoul.common.di

import androidx.navigation.NavDirections
import com.jonapoul.common.presets.OutputPreset

interface IUiResources {
    /* Layout IDs */
    val activityLayoutId: Int

    /* XML IDs */
    val settingsXmlId: Int

    /* Menu IDs */
    val mainMenuId: Int

    /* Resource IDs */
    val navHostFragmentId: Int
    val startStopButtonId: Int

    /* String IDs */
    val permissionRationaleId: Int

    /* Colour IDs */
    val accentColourId: Int

    /* Navigation Directions */
    val mainToLocationDirections: NavDirections
    val mainToAboutDirections: NavDirections
    val mainToListDirections: NavDirections
    fun listToEditDirections(preset: OutputPreset?): NavDirections
}
