package com.jonapoul.cotbeacon.service.chat.runnables

import android.content.SharedPreferences
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.cotbeacon.repositories.IChatRepository
import com.jonapoul.cotbeacon.service.chat.ChatConstants
import timber.log.Timber
import java.net.DatagramPacket
import java.net.MulticastSocket

class UdpChatListenRunnable(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        socketRepository: ISocketRepository,
        chatRepository: IChatRepository,
        deviceUidRepository: IDeviceUidRepository,
) : ChatListenRunnable(prefs, errorListener, socketRepository, chatRepository, deviceUidRepository) {

    private var socket: MulticastSocket? = null

    override fun run() {
        safeInitialise {
            socket = socketRepository.getUdpInputSocket(
                    group = ChatConstants.UDP_ALL_CHAT_ADDRESS,
                    port = ChatConstants.UDP_PORT
            )
        } ?: return

        while (true) {
            postErrorIfThrowable {
                Timber.i("Listening for chat...")
                val bytes = ByteArray(PACKET_BUFFER_SIZE)
                val packet = DatagramPacket(bytes, PACKET_BUFFER_SIZE)
                socket?.receive(packet)
                val receivedBytes = packet.data.copyOf(packet.length)
                val chat = ChatCursorOnTarget.fromBytes(receivedBytes)
                if (chat?.uid != deviceUid) {
                    /* Only process the packet if it's not a loopback message */
                    dealWithChat(chat)
                }
            } ?: break
        }
        Timber.i("Finishing UdpChatListenRunnable")
        close()
    }

    override fun close() {
        safeClose(socket)
    }
}
