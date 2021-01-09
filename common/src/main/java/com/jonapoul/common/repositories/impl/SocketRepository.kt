package com.jonapoul.common.repositories.impl

import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.SocketFactory
import timber.log.Timber
import java.io.OutputStream
import java.net.MulticastSocket
import java.net.Socket
import javax.inject.Inject
import javax.net.ssl.SSLSocket

class SocketRepository @Inject constructor(private val socketFactory: SocketFactory) : ISocketRepository {
    private val lock = Any()

    private var tcpSocket: Socket? = null
    private var sslSocket: SSLSocket? = null
    private var outputStream: OutputStream? = null

    override fun clearSockets() {
        tcpSocket = null
        sslSocket = null
        outputStream = null
    }

    override fun getUdpInputSocket(group: String, port: Int): MulticastSocket {
        synchronized(lock) {
            return socketFactory.getUdpInputSocket(group, port)
        }
    }

    override fun getTcpSocket(): Socket {
        synchronized(lock) {
            Timber.i("Getting TCP socket")
            tcpSocket?.let {
                if (socketIsValid(it)) {
                    Timber.i("Returning existing at port %d", it.localPort)
                    return it
                }
            }
            return socketFactory.getTcpSocket().also {
                tcpSocket = it
                Timber.i("Returning new at port %d", it.localPort)
            }
        }
    }

    override fun getSslSocket(): Socket {
        synchronized(lock) {
            Timber.i("Getting SSL socket")
            sslSocket?.let {
                if (socketIsValid(it)) {
                    Timber.i("Returning existing at port %d", it.localPort)
                    return it
                }
            }
            return socketFactory.getSslSocket().also {
                sslSocket = it
                Timber.i("Returning new at port %d", it.localPort)
            }
        }
    }

    override fun getOutputStream(socket: Socket): OutputStream {
        synchronized(lock) {
            outputStream?.let { return it }
            return socketFactory.getOutputStream(socket).also { outputStream = it }
        }
    }

    private fun socketIsValid(socket: Socket): Boolean {
        return try {
            /* Write a single byte to the socket to check whether it throws any exceptions */
            socket.outputStream.write(0)
            Timber.i("Socket is valid")
            true
        } catch (t: Throwable) {
            Timber.w("Socket is not valid")
            false
        }
    }
}
