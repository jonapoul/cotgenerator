package com.jonapoul.cotbeacon.service.emergency.runnables

import android.content.SharedPreferences
import androidx.annotation.CallSuper
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.prefs.getStringFromPair
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.cotbeacon.cot.EmergencyCursorOnTarget
import com.jonapoul.cotbeacon.cot.EmergencyType
import timber.log.Timber
import java.net.SocketException


abstract class EmergencyRunnable(
        protected val prefs: SharedPreferences,
        private val errorListener: IThreadErrorListener,
        protected val socketRepository: ISocketRepository,
        private val gpsRepository: IGpsRepository,
        private val deviceUidRepository: IDeviceUidRepository,
        protected val emergencyType: EmergencyType
) : Runnable {

    protected lateinit var emergency: EmergencyCursorOnTarget

    @CallSuper
    override fun run() {
        emergency = EmergencyCursorOnTarget(
                emergencyType = emergencyType,
                gpsRepository = gpsRepository,
                uid = deviceUidRepository.getUid(),
                callsign = prefs.getStringFromPair(CommonPrefs.CALLSIGN)
        )
    }

    protected open fun safeInitialise(initialisation: () -> Unit): Any? {
        return try {
            initialisation()
            Any()
        } catch (t: Throwable) {
            Timber.e(t)
            errorListener.onThreadError(t)
            null
        }
    }

    protected fun postErrorIfThrowable(function: () -> Unit): Any? {
        return try {
            function()
            true
        } catch (e: SocketException) {
            /* Thrown when a socket is closed externally whilst listening, i.e. when the user
             * tells the service to shutdown. No-op, just back out and finish the runnable */
            null
        } catch (t: Throwable) {
            Timber.e(t)
            errorListener.onThreadError(t)
            null
        }
    }
}