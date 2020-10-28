package com.jon.cotbeacon

import androidx.navigation.NavDirections
import com.jon.common.di.ActivityResources
import com.jon.common.presets.OutputPreset
import com.jon.common.ui.listpresets.ListPresetsFragmentDirections
import com.jon.common.ui.main.MainFragmentDirections
import javax.inject.Inject

class BeaconActivityResources @Inject constructor() : ActivityResources {
    override val activityLayoutId = R.layout.beacon_activity
    override val settingsXmlId = R.xml.settings
    override val mainMenuId = R.menu.beacon_main_menu
    override val navHostFragmentId = R.id.nav_host_fragment
    override val startStopButtonId = R.id.start_stop_button
    override val permissionRationaleId = R.string.permission_rationale
    override val accentColourId = R.color.colorAccent

    override val mainToLocationDirections = MainFragmentDirections.actionMainToLocation()
    override val mainToAboutDirections = MainFragmentDirections.actionMainToAbout()
    override val mainToListDirections = MainFragmentDirections.actionMainToListPresets()

    override fun listToEditDirections(preset: OutputPreset?): NavDirections {
        return ListPresetsFragmentDirections.actionListPresetsToBeaconEditPreset(preset)
    }
}