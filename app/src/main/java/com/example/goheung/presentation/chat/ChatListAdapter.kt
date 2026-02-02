package com.example.goheung.presentation.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.databinding.ItemChatRoomBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying chat rooms list
 */
class ChatListAdapter(
    private val onChatRoomClick: (ChatRoom) -> Unit
) : ListAdapter<ChatRoom, ChatListAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatRoomViewHolder(binding, onChatRoomClick)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatRoomViewHolder(
        private val binding: ItemChatRoomBinding,
        private val onChatRoomClick: (ChatRoom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(chatRoom: ChatRoom) {
            binding.apply {
                textViewChatRoomName.text = chatRoom.name
                textViewLastMessage.text = chatRoom.lastMessage.ifEmpty { "No messages yet" }

                chatRoom.lastMessageTimestamp?.let { timestamp ->
                    val formattedTime = timeFormat.format(timestamp)
                    textViewTimestamp.text = formattedTime
                } ?: run {
                    textViewTimestamp.text = ""
                }

                root.setOnClickListener {
                    onChatRoomClick(chatRoom)
                }
            }
        }
    }

    private class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem == newItem
        }
    }
}
