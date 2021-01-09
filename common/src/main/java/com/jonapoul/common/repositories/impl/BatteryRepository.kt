package com.jonapoul.common.repositories.impl

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.jonapoul.common.repositories.IBatteryRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt

class BatteryRepository @Inject constructor(private val context: Context) : IBatteryRepository {
    private val lock = Any()
    private lateinit var batteryIntent: Intent
    private var isInitialised = false

    override fun getPercentage(): Int {
        synchronized(lock) {
            if (!isInitialised) {
                initialise(context)
            }
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return (level * 100 / scale.toFloat()).roundToInt()
        }
    }

    private fun initialise(context: Context) {
        Timber.d("initialise")
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryIntent = context.registerReceiver(null, intentFilter)!!
        isInitialised = true
    }
}
