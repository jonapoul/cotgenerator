package com.jonapoul.common.presets

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [OutputPreset::class], version = 4)
@TypeConverters(ProtocolConverter::class)
abstract class PresetDatabase : RoomDatabase() {
    abstract fun presetDao(): IPresetDao

    companion object {
        const val FILENAME = "presets.db"
    }
}
