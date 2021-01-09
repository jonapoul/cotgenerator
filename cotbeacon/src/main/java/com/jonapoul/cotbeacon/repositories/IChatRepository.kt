package com.jonapoul.cotbeacon.repositories

import androidx.lifecycle.LiveData
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.rugovit.eventlivedata.EventLiveData

interface IChatRepository {
    fun getLatestChat(): LiveData<ChatCursorOnTarget>
    fun getChats(): LiveData<List<ChatCursorOnTarget>>
    fun postChat(chat: ChatCursorOnTarget)
    fun getErrors(): EventLiveData<String>
    fun postError(errorMessage: String)
}
