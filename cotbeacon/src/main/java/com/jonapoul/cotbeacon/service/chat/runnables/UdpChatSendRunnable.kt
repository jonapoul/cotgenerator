package com.jonapoul.cotbeacon.service.chat.runnables

import android.content.SharedPreferences
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.cotbeacon.repositories.IChatRepository
import com.jonapoul.cotbeacon.service.chat.ChatConstants
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpChatSendRunnable(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        socketRepository: ISocketRepository,
        chatRepository: IChatRepository,
        chatMessage: ChatCursorOnTarget,
) : ChatSendRunnable(prefs, errorListener, socketRepository, chatRepository, chatMessage) {

    private lateinit var socket: DatagramSocket

    override fun run() {
        safeInitialise {
            socket = DatagramSocket()
        } ?: return

        postErrorIfThrowable {
            Timber.i("Sending chat message: ${chatMessage.message}")
            val ip = InetAddress.getByName(ChatConstants.UDP_ALL_CHAT_ADDRESS)
            val buf = chatMessage.toBytes(dataFormat)
            socket.send(DatagramPacket(buf, buf.size, ip, ChatConstants.UDP_PORT))
            chatRepository.postChat(chatMessage)
        }

        Timber.i("Finishing UdpChatSendRunnable")
    }
}
