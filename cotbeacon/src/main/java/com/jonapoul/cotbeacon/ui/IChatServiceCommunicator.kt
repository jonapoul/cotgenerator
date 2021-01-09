package com.jonapoul.cotbeacon.ui

import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.ui.IServiceCommunicator

interface IChatServiceCommunicator : IServiceCommunicator {
    fun sendChat(chat: ChatCursorOnTarget)
}
