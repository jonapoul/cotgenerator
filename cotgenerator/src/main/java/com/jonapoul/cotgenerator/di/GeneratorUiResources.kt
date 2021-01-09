package com.jonapoul.cotgenerator.di

import androidx.navigation.NavDirections
import com.jonapoul.common.di.IUiResources
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.ui.listpresets.ListPresetsFragmentDirections
import com.jonapoul.common.ui.main.MainFragmentDirections
import com.jonapoul.cotgenerator.R
import javax.inject.Inject

class GeneratorUiResources @Inject constructor() : IUiResources {
    override val activityLayoutId = R.layout.generator_activity
    override val settingsXmlId = R.xml.settings
    override val mainMenuId = R.menu.generator_main_menu
    override val navHostFragmentId = R.id.nav_host_fragment
    override val startStopButtonId = R.id.start_stop_button
    override val permissionRationaleId = R.string.permission_rationale
    override val accentColourId = R.color.colorAccent

    override val mainToLocationDirections = MainFragmentDirections.actionMainToLocation()
    override val mainToAboutDirections = MainFragmentDirections.actionMainToAbout()
    override val mainToListDirections = MainFragmentDirections.actionMainToListPresets()

    override fun listToEditDirections(preset: OutputPreset?): NavDirections {
        return ListPresetsFragmentDirections.actionListPresetsToGeneratorEditPreset(preset)
    }
}
