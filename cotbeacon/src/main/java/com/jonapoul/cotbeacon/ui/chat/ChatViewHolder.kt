package com.jonapoul.cotbeacon.ui.chat

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.utils.ColourUtils
import com.jonapoul.cotbeacon.R

internal class ChatViewHolder(
        itemView: View,
        private val chats: List<ChatCursorOnTarget>
) : RecyclerView.ViewHolder(itemView) {

    private val timestamp: TextView = itemView.findViewById(R.id.chat_timestamp)
    private val callsign: TextView = itemView.findViewById(R.id.chat_callsign)
    private val message: TextView = itemView.findViewById(R.id.chat_message)

    fun initialise() {
        val chat = chats[adapterPosition]
        timestamp.text = chat.start.shortLocalTimestamp()
        callsign.text = chat.callsign
        message.text = chat.message
        setTextViewColour(timestamp)
        setTextViewColour(callsign)
    }

    private fun setTextViewColour(textView: TextView) {
        val colour = ColourUtils.lightenColour(
                colour = Color.parseColor("#${chats[adapterPosition].team.toHexString()}"),
                value = 0.2f
        )
        textView.setTextColor(colour)
    }
}
