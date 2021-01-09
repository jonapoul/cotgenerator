package com.jonapoul.cotbeacon.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.rugovit.eventlivedata.EventLiveData
import com.rugovit.eventlivedata.MutableEventLiveData
import javax.inject.Inject

class ChatRepository @Inject constructor() : IChatRepository {
    private val chats = ArrayList<ChatCursorOnTarget>()
    private val latestChatLiveData = MutableLiveData<ChatCursorOnTarget>()
    private val chatLiveData = MutableLiveData<List<ChatCursorOnTarget>>().also { it.value = chats }

    private val chatErrorEvent = MutableEventLiveData<String>()

    override fun getLatestChat(): LiveData<ChatCursorOnTarget> {
        return latestChatLiveData
    }

    override fun getChats(): LiveData<List<ChatCursorOnTarget>> {
        return chatLiveData
    }

    override fun postChat(chat: ChatCursorOnTarget) {
        chats.add(chat)
        chatLiveData.postValue(chats)
        latestChatLiveData.postValue(chat)
    }

    override fun getErrors(): EventLiveData<String> {
        return chatErrorEvent
    }

    override fun postError(errorMessage: String) {
        chatErrorEvent.postValue(errorMessage)
    }
}
