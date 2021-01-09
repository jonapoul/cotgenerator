package com.jonapoul.common.ui.listpresets

import com.jonapoul.common.presets.OutputPreset

internal interface IPresetClickListener {
    fun onClickEditItem(preset: OutputPreset)
    fun onClickDeleteItem(preset: OutputPreset)
}
