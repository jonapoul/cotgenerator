package com.jonapoul.common.service

import android.content.SharedPreferences
import com.jonapoul.common.cot.CursorOnTarget
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.utils.DataFormat
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException


internal open class TcpThread(prefs: SharedPreferences) : BaseThread(prefs) {
    init {
        dataFormat = DataFormat.XML // regardless of what the preference is set as
    }

    protected lateinit var socket: Socket
    protected lateinit var outputStream: OutputStream

    override fun shutdown() {
        super.shutdown()
        Timber.i("shutdown")
        try {
            socket.close()
        } catch (e: Exception) {
            /* ignore, we're shutting down anyway */
        }
        socketRepository.clearSockets()
    }

    override fun run() {
        Timber.i("run")
        try {
            super.run()
            initialiseDestAddress()
            openSockets()
            val bufferTimeMs = (periodMilliseconds() / cotIcons.size).toInt()
            while (isRunning()) {
                for (cot in cotIcons) {
                    if (!isRunning()) break
                    sendToDestination(cot)
                    bufferSleep(bufferTimeMs.toLong())
                }
                if (!isRunning()) break
                cotIcons = cotFactory.generate()
                outputStream.flush()
            }
        } catch (t: Throwable) {
            Timber.e(t)
            throw RuntimeException(t.message)
        } finally {
            shutdown()
        }
    }

    @Throws(IOException::class)
    protected open fun sendToDestination(cot: CursorOnTarget) {
        try {
            outputStream.write(cot.toBytes(dataFormat))
            Timber.i("Sent cot: %s to %d from %d", cot.callsign, socket.port, socket.localPort)
        } catch (e: NullPointerException) {
            /* Thrown when the thread is cancelled from another thread and we try to access the sockets */
            shutdown()
        }
    }

    @Throws(UnknownHostException::class)
    override fun initialiseDestAddress() {
        destIp = InetAddress.getByName(prefs.getString(CommonPrefs.DEST_ADDRESS, ""))
        destPort = prefs.getString(CommonPrefs.DEST_PORT, "")!!.toInt()
    }

    @Throws(Exception::class)
    override fun openSockets() {
        socket = socketRepository.getTcpSocket()
        outputStream = socketRepository.getOutputStream(socket)
    }
}
