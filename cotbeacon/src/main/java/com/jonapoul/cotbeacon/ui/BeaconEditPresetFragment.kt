package com.jonapoul.cotbeacon.ui

import androidx.navigation.fragment.navArgs
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.ui.editpreset.EditPresetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BeaconEditPresetFragment : EditPresetFragment() {
    override val args: BeaconEditPresetFragmentArgs by navArgs()

    override fun getFragmentArgumentPreset(): OutputPreset? {
        return args.presetArgument
    }
}