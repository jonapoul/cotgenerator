package com.jonapoul.cotbeacon.service.chat.runnables

import android.content.SharedPreferences
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.cot.CotTeam
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.cotbeacon.repositories.IChatRepository
import timber.log.Timber
import java.io.Closeable
import kotlin.math.abs

abstract class ChatListenRunnable(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        socketRepository: ISocketRepository,
        chatRepository: IChatRepository,
        protected val deviceUidRepository: IDeviceUidRepository,
) : ChatRunnable(prefs, errorListener, socketRepository, chatRepository), Closeable {

    protected val deviceUid = deviceUidRepository.getUid()

    /* Only difference to the superclass is the close() call in the catch block */
    override fun safeInitialise(initialisation: () -> Unit): Any? {
        return try {
            initialisation()
            Any()
        } catch (t: Throwable) {
            Timber.e(t)
            close()
            chatRepository.postError(t.message ?: "Unknown exception")
            null
        }
    }

    protected fun safeClose(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (t: Throwable) {
            /* No-op */
        }
    }

    protected fun dealWithChat(chat: ChatCursorOnTarget?) {
        if (chat != null) {
            chat.team = getCotTeam(chat.uid)
            chatRepository.postChat(chat)
            Timber.i("Received valid chat from ${chat.callsign}: ${chat.message}")
        }
    }

    private fun getCotTeam(uid: String?): CotTeam {
        return if (uid == null) {
            CotTeam.WHITE
        } else {
            val index = abs(uid.hashCode()) % CotTeam.values().size
            CotTeam.values()[index]
        }
    }

    protected companion object {
        const val PACKET_BUFFER_SIZE = 2048 // bytes
    }
}