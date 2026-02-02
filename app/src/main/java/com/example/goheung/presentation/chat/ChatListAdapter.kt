package com.example.goheung.presentation.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goheung.databinding.ItemChatRoomBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying chat rooms list
 */
class ChatListAdapter(
    private val onChatRoomClick: (ChatListViewModel.ChatRoomWithParticipants) -> Unit
) : ListAdapter<ChatListViewModel.ChatRoomWithParticipants, ChatListAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

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
        private val onChatRoomClick: (ChatListViewModel.ChatRoomWithParticipants) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(item: ChatListViewModel.ChatRoomWithParticipants) {
            binding.apply {
                textViewChatRoomName.text = item.displayName
                textViewLastMessage.text = item.chatRoom.lastMessage.ifEmpty { "No messages yet" }

                item.chatRoom.lastMessageTimestamp?.let { timestamp ->
                    val formattedTime = timeFormat.format(timestamp)
                    textViewTimestamp.text = formattedTime
                } ?: run {
                    textViewTimestamp.text = ""
                }

                root.setOnClickListener {
                    onChatRoomClick(item)
                }
            }
        }
    }

    private class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatListViewModel.ChatRoomWithParticipants>() {
        override fun areItemsTheSame(
            oldItem: ChatListViewModel.ChatRoomWithParticipants,
            newItem: ChatListViewModel.ChatRoomWithParticipants
        ): Boolean {
            return oldItem.chatRoom.id == newItem.chatRoom.id
        }

        override fun areContentsTheSame(
            oldItem: ChatListViewModel.ChatRoomWithParticipants,
            newItem: ChatListViewModel.ChatRoomWithParticipants
        ): Boolean {
            return oldItem == newItem
        }
    }
}
