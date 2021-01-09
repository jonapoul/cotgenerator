package com.jonapoul.cotbeacon.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.cotbeacon.R

internal class ChatAdapter(context: Context) : RecyclerView.Adapter<ChatViewHolder>() {

    private val chats = ArrayList<ChatCursorOnTarget>()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = inflater.inflate(viewType, parent, false)
        return ChatViewHolder(view, chats)
    }

    override fun getItemViewType(position: Int): Int {
        val isIncoming = chats[position].isIncoming
        return LAYOUTS.getValue(isIncoming)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.initialise()
    }

    override fun getItemCount() = chats.size

    fun updateChats(newChats: List<ChatCursorOnTarget>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
    }

    private companion object {
        val LAYOUTS = mapOf(
                true to R.layout.chat_recycler_item_incoming,
                false to R.layout.chat_recycler_item_outgoing
        )
    }
}
