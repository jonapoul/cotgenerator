package com.jonapoul.common.repositories

import androidx.lifecycle.LiveData
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.utils.Protocol

interface IPresetRepository {
    fun insertPreset(preset: OutputPreset)
    fun deletePreset(preset: OutputPreset)
    fun getPreset(protocol: Protocol, address: String, port: Int): OutputPreset?
    fun getCustomByProtocol(protocol: Protocol): LiveData<List<OutputPreset>>
    fun deleteDatabase()
    fun defaultsByProtocol(protocol: Protocol): List<OutputPreset>
}
