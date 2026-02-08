package com.example.goheung.presentation.chat

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goheung.data.model.ChatRoomType
import com.example.goheung.databinding.ItemChatRoomDmBinding
import com.example.goheung.databinding.ItemChatRoomGroupBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying chat rooms list
 * Supports different view types for DM and Group chats
 */
class ChatListAdapter(
    private val onChatRoomClick: (ChatListViewModel.ChatRoomWithParticipants) -> Unit,
    private val onFavoriteClick: (String) -> Unit,
    private val onChatRoomLongClick: (ChatListViewModel.ChatRoomWithParticipants) -> Boolean,
    private val currentUserId: String
) : ListAdapter<ChatListViewModel.ChatRoomWithParticipants, RecyclerView.ViewHolder>(ChatRoomDiffCallback()) {

    companion object {
        private const val TAG = "ChatListAdapter"
        private const val VIEW_TYPE_DM = 1
        private const val VIEW_TYPE_GROUP = 2
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.chatRoom.type == ChatRoomType.DM) {
            VIEW_TYPE_DM
        } else {
            VIEW_TYPE_GROUP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DM -> {
                val binding = ItemChatRoomDmBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DMViewHolder(binding, onChatRoomClick, onFavoriteClick, onChatRoomLongClick, currentUserId)
            }
            VIEW_TYPE_GROUP -> {
                val binding = ItemChatRoomGroupBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                GroupViewHolder(binding, onChatRoomClick, onFavoriteClick, onChatRoomLongClick, currentUserId)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d(TAG, "onBindViewHolder: position=$position, displayName='${item.displayName}', id=${item.chatRoom.id}")
        when (holder) {
            is DMViewHolder -> holder.bind(item)
            is GroupViewHolder -> holder.bind(item)
        }
    }

    override fun submitList(list: List<ChatListViewModel.ChatRoomWithParticipants>?) {
        Log.d(TAG, "submitList: Submitting ${list?.size ?: 0} items")
        list?.forEachIndexed { index, item ->
            Log.d(TAG, "  Item[$index]: ${item.displayName} (${item.chatRoom.id})")
        }
        super.submitList(list)
    }

    /**
     * ViewHolder for DM (Direct Message)
     */
    class DMViewHolder(
        private val binding: ItemChatRoomDmBinding,
        private val onChatRoomClick: (ChatListViewModel.ChatRoomWithParticipants) -> Unit,
        private val onFavoriteClick: (String) -> Unit,
        private val onChatRoomLongClick: (ChatListViewModel.ChatRoomWithParticipants) -> Boolean,
        private val currentUserId: String
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(item: ChatListViewModel.ChatRoomWithParticipants) {
            Log.d(TAG, "DM bind: displayName='${item.displayName}', lastMessage='${item.chatRoom.lastMessage}'")
            binding.apply {
                textViewChatRoomName.text = item.displayName
                textViewLastMessage.text = item.chatRoom.lastMessage.ifEmpty { "No messages yet" }

                item.chatRoom.lastMessageTimestamp?.let { timestamp ->
                    val formattedTime = timeFormat.format(timestamp)
                    textViewTimestamp.text = formattedTime
                } ?: run {
                    textViewTimestamp.text = ""
                }

                // 즐겨찾기 아이콘 설정
                val isFavorite = item.chatRoom.isFavoriteBy(currentUserId)
                imageViewFavorite.setImageResource(
                    if (isFavorite) android.R.drawable.star_big_on
                    else android.R.drawable.star_big_off
                )

                imageViewFavorite.setOnClickListener {
                    onFavoriteClick(item.chatRoom.id)
                }

                root.setOnClickListener {
                    onChatRoomClick(item)
                }

                root.setOnLongClickListener {
                    onChatRoomLongClick(item)
                }
            }
        }
    }

    /**
     * ViewHolder for Group Chat
     */
    class GroupViewHolder(
        private val binding: ItemChatRoomGroupBinding,
        private val onChatRoomClick: (ChatListViewModel.ChatRoomWithParticipants) -> Unit,
        private val onFavoriteClick: (String) -> Unit,
        private val onChatRoomLongClick: (ChatListViewModel.ChatRoomWithParticipants) -> Boolean,
        private val currentUserId: String
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(item: ChatListViewModel.ChatRoomWithParticipants) {
            Log.d(TAG, "Group bind: displayName='${item.displayName}', chatRoomName='${item.chatRoom.name}', participants=${item.participants.size}")
            binding.apply {
                // 그룹 채팅방은 이미 계산된 displayName 사용 (chatRoom.name이 우선)
                textViewChatRoomName.text = item.displayName

                // 참여자 정보 표시
                val participantsText = formatParticipants(item.participants)
                textViewParticipants.text = participantsText

                textViewLastMessage.text = item.chatRoom.lastMessage.ifEmpty { "No messages yet" }

                item.chatRoom.lastMessageTimestamp?.let { timestamp ->
                    val formattedTime = timeFormat.format(timestamp)
                    textViewTimestamp.text = formattedTime
                } ?: run {
                    textViewTimestamp.text = ""
                }

                // 즐겨찾기 아이콘 설정
                val isFavorite = item.chatRoom.isFavoriteBy(currentUserId)
                imageViewFavorite.setImageResource(
                    if (isFavorite) android.R.drawable.star_big_on
                    else android.R.drawable.star_big_off
                )

                imageViewFavorite.setOnClickListener {
                    onFavoriteClick(item.chatRoom.id)
                }

                root.setOnClickListener {
                    onChatRoomClick(item)
                }

                root.setOnLongClickListener {
                    onChatRoomLongClick(item)
                }
            }
        }

        private fun formatParticipants(participants: List<com.example.goheung.data.model.User>): String {
            return when {
                participants.isEmpty() -> "참여자 없음"
                participants.size <= 3 -> participants.joinToString(", ") { it.displayName }
                else -> {
                    val firstThree = participants.take(3).joinToString(", ") { it.displayName }
                    val remaining = participants.size - 3
                    "$firstThree 외 ${remaining}명"
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
