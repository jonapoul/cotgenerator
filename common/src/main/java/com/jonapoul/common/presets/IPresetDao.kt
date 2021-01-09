package com.jonapoul.common.presets

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface IPresetDao {
    @Query("SELECT * FROM Presets WHERE Protocol LIKE :protocol")
    fun getByProtocol(protocol: String): LiveData<List<OutputPreset>>

    @Query("SELECT * FROM Presets WHERE Protocol LIKE :protocol AND Address LIKE :address AND Port LIKE :port LIMIT 1")
    fun getPreset(protocol: String?, address: String?, port: Int): OutputPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(preset: OutputPreset)

    @Delete
    fun delete(preset: OutputPreset)

    @Query("DELETE FROM Presets")
    fun deleteAll()
}
