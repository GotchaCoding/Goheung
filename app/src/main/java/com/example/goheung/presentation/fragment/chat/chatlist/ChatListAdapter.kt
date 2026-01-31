package com.example.goheung.presentation.fragment.chat.chatlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goheung.R
import com.example.goheung.databinding.ItemChatRoomBinding
import com.example.goheung.model.ChatRoomModel
import com.example.goheung.util.DateTimeUtil

class ChatListAdapter(
    private val currentUserId: String,
    private val onItemClick: (ChatRoomModel) -> Unit
) : ListAdapter<ChatRoomModel, ChatListAdapter.ChatRoomViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding: ItemChatRoomBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_chat_room,
            parent,
            false
        )
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatRoomViewHolder(
        private val binding: ItemChatRoomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoomModel) {
            binding.item = chatRoom

            // Show other participant's name
            val otherName = chatRoom.participantNames.entries
                .firstOrNull { it.key != currentUserId }?.value ?: ""
            binding.tvName.text = otherName
            binding.tvLastMessage.text = chatRoom.lastMessage
            binding.tvTime.text = DateTimeUtil.formatChatTime(chatRoom.lastMessageTimestamp)

            binding.root.setOnClickListener {
                onItemClick(chatRoom)
            }
            binding.executePendingBindings()
        }
    }

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<ChatRoomModel>() {
            override fun areItemsTheSame(oldItem: ChatRoomModel, newItem: ChatRoomModel): Boolean {
                return oldItem.chatRoomId == newItem.chatRoomId
            }

            override fun areContentsTheSame(oldItem: ChatRoomModel, newItem: ChatRoomModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
