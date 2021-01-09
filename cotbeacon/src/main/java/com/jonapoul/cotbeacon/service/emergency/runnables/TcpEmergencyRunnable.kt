package com.jonapoul.cotbeacon.service.emergency.runnables

import android.content.SharedPreferences
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.common.utils.DataFormat
import com.jonapoul.cotbeacon.cot.EmergencyType
import timber.log.Timber
import java.io.OutputStream
import java.net.Socket

class TcpEmergencyRunnable(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        socketRepository: ISocketRepository,
        gpsRepository: IGpsRepository,
        deviceUidRepository: IDeviceUidRepository,
        emergencyType: EmergencyType,
) : EmergencyRunnable(prefs, errorListener, socketRepository, gpsRepository, deviceUidRepository, emergencyType) {

    private lateinit var socket: Socket
    private var outputStream: OutputStream? = null

    override fun run() {
        super.run()
        safeInitialise {
            socket = socketRepository.getTcpSocket()
            outputStream = socketRepository.getOutputStream(socket)
        } ?: return

        postErrorIfThrowable {
            Timber.i("Sending emergency ${emergencyType.description} to port %d from %d", socket.port, socket.localPort)
            outputStream?.let {
                it.write(emergency.toBytes(DataFormat.XML))
                it.flush()
            }
        }

        Timber.i("Finishing TcpEmergencyRunnable")
    }
}