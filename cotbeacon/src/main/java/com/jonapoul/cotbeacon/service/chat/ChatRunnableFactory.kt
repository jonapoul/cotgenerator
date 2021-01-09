package com.jonapoul.cotbeacon.service.chat

import android.content.SharedPreferences
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.common.utils.Protocol
import com.jonapoul.common.utils.exhaustive
import com.jonapoul.cotbeacon.repositories.IChatRepository
import com.jonapoul.cotbeacon.service.chat.runnables.*

internal class ChatRunnableFactory(
        private val prefs: SharedPreferences,
        private val threadErrorListener: IThreadErrorListener,
        private val socketRepository: ISocketRepository,
        private val chatRepository: IChatRepository,
) {
    fun getListenRunnable(deviceUidRepository: IDeviceUidRepository): ChatListenRunnable {
        return when (Protocol.fromPrefs(prefs)) {
            Protocol.UDP -> UdpChatListenRunnable(prefs, threadErrorListener, socketRepository, chatRepository, deviceUidRepository)
            Protocol.TCP -> TcpChatListenRunnable(prefs, threadErrorListener, socketRepository, chatRepository, deviceUidRepository)
            Protocol.SSL -> SslChatListenRunnable(prefs, threadErrorListener, socketRepository, chatRepository, deviceUidRepository)
        }.exhaustive
    }

    fun getSendRunnable(chatMessage: ChatCursorOnTarget): ChatSendRunnable {
        return when (Protocol.fromPrefs(prefs)) {
            Protocol.UDP -> UdpChatSendRunnable(prefs, threadErrorListener, socketRepository, chatRepository, chatMessage)
            Protocol.TCP -> TcpChatSendRunnable(prefs, threadErrorListener, socketRepository, chatRepository, chatMessage)
            Protocol.SSL -> SslChatSendRunnable(prefs, threadErrorListener, socketRepository, chatRepository, chatMessage)
        }.exhaustive
    }
}
