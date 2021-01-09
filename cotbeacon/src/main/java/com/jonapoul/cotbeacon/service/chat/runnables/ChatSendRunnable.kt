package com.jonapoul.cotbeacon.service.chat.runnables

import android.content.SharedPreferences
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.common.utils.DataFormat
import com.jonapoul.cotbeacon.repositories.IChatRepository

abstract class ChatSendRunnable(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        socketRepository: ISocketRepository,
        chatRepository: IChatRepository,
        protected val chatMessage: ChatCursorOnTarget
) : ChatRunnable(prefs, errorListener, socketRepository, chatRepository) {

    protected var dataFormat = DataFormat.fromPrefs(prefs)
}
