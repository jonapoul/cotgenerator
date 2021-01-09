package com.jonapoul.cotbeacon.service.emergency.runnables

import android.content.SharedPreferences
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.common.utils.DataFormat
import com.jonapoul.cotbeacon.cot.EmergencyType
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpEmergencyRunnable(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        socketRepository: ISocketRepository,
        gpsRepository: IGpsRepository,
        deviceUidRepository: IDeviceUidRepository,
        emergencyType: EmergencyType,
) : EmergencyRunnable(prefs, errorListener, socketRepository, gpsRepository, deviceUidRepository, emergencyType) {

    private lateinit var socket: DatagramSocket
    private lateinit var dataFormat: DataFormat
    private var ip: InetAddress? = null
    private var port: Int = 0

    override fun run() {
        super.run()
        safeInitialise {
            socket = DatagramSocket()
            dataFormat = DataFormat.fromPrefs(prefs)
            ip = InetAddress.getByName(prefs.getString(CommonPrefs.DEST_ADDRESS, ""))
            port = prefs.getString(CommonPrefs.DEST_PORT, "")!!.toInt()
        } ?: return

        postErrorIfThrowable {
            Timber.i("Sending emergency ${emergencyType.description} to port %d from %d", port, socket.localPort)
            val buf = emergency.toBytes(dataFormat)
            socket.send(DatagramPacket(buf, buf.size, ip, port))
        }

        Timber.i("Finishing UdpEmergencyRunnable")
    }
}
