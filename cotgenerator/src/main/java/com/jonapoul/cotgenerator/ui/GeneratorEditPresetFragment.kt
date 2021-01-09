package com.jonapoul.cotgenerator.ui

import androidx.navigation.fragment.navArgs
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.ui.editpreset.EditPresetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeneratorEditPresetFragment : EditPresetFragment() {
    override val args: GeneratorEditPresetFragmentArgs by navArgs()

    override fun getFragmentArgumentPreset(): OutputPreset? {
        return args.presetArgument
    }
}