package com.jonapoul.cotbeacon.service.chat.runnables

import android.content.SharedPreferences
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.common.utils.DataFormat
import com.jonapoul.cotbeacon.repositories.IChatRepository
import timber.log.Timber
import java.io.OutputStream
import java.net.Socket

class TcpChatSendRunnable(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        socketRepository: ISocketRepository,
        chatRepository: IChatRepository,
        chatMessage: ChatCursorOnTarget,
) : ChatSendRunnable(prefs, errorListener, socketRepository, chatRepository, chatMessage) {

    private lateinit var socket: Socket
    private var outputStream: OutputStream? = null

    override fun run() {
        safeInitialise {
            socket = socketRepository.getTcpSocket()
            outputStream = socketRepository.getOutputStream(socket)
        } ?: return

        postErrorIfThrowable {
            Timber.i("Sending chat message: ${chatMessage.message} to port %d from %d", socket.port, socket.localPort)
            outputStream?.let {
                it.write(chatMessage.toBytes(DataFormat.XML))
                it.flush()
                chatRepository.postChat(chatMessage)
            }
        }

        Timber.i("Finishing TcpChatSendRunnable")
    }
}
