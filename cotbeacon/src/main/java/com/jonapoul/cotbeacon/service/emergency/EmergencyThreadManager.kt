package com.jonapoul.cotbeacon.service.emergency

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.common.service.ThreadManager
import com.jonapoul.common.utils.Protocol
import com.jonapoul.common.utils.exhaustive
import com.jonapoul.cotbeacon.cot.EmergencyType
import com.jonapoul.cotbeacon.service.emergency.runnables.SslEmergencyRunnable
import com.jonapoul.cotbeacon.service.emergency.runnables.TcpEmergencyRunnable
import com.jonapoul.cotbeacon.service.emergency.runnables.UdpEmergencyRunnable
import timber.log.Timber
import java.util.concurrent.Executors

class EmergencyThreadManager(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        private val deviceUidRepository: IDeviceUidRepository,
        private val socketRepository: ISocketRepository,
        private val gpsRepository: IGpsRepository
) : ThreadManager(prefs, errorListener), IThreadErrorListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()

    override fun start() {
        /* No-op */
    }

    override fun shutdown() {
        /* No-op */
    }

    override fun restart() {
        /* No-op */
    }

    override fun isRunning(): Boolean {
        /* These are one-and-done tasks, so we won't be sat still in a "running" state */
        return false
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        /* No-op */
    }

    override fun onThreadError(throwable: Throwable) {
        mainHandler.post {
            errorListener.onThreadError(throwable)
        }
    }

    fun sendEmergency(emergencyType: EmergencyType) {
        Timber.i("Sending emergency ${emergencyType.description}")
        synchronized(lock) {
            val runnable = when (Protocol.fromPrefs(prefs)) {
                Protocol.UDP -> UdpEmergencyRunnable(prefs, errorListener, socketRepository, gpsRepository, deviceUidRepository, emergencyType)
                Protocol.TCP -> TcpEmergencyRunnable(prefs, errorListener, socketRepository, gpsRepository, deviceUidRepository, emergencyType)
                Protocol.SSL -> SslEmergencyRunnable(prefs, errorListener, socketRepository, gpsRepository, deviceUidRepository, emergencyType)
            }.exhaustive
            executor.execute(runnable)
        }
    }

}