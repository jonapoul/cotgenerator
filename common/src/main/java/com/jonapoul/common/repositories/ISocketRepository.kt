package com.jonapoul.common.repositories

import java.io.OutputStream
import java.net.MulticastSocket
import java.net.Socket

interface ISocketRepository {
    fun clearSockets()
    fun getUdpInputSocket(group: String, port: Int): MulticastSocket
    fun getTcpSocket(): Socket
    fun getSslSocket(): Socket
    fun getOutputStream(socket: Socket): OutputStream
}
