package com.example.goheung.presentation.fragment.chat.chatdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goheung.R
import com.example.goheung.constants.Constants
import com.example.goheung.databinding.ItemReceivedMessageBinding
import com.example.goheung.databinding.ItemSentMessageBinding
import com.example.goheung.model.ChatMessageModel
import com.example.goheung.util.DateTimeUtil

class ChatDetailAdapter : ListAdapter<ChatMessageModel, RecyclerView.ViewHolder>(DIFF_UTIL) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Constants.VIEW_TYPE_SENT_MESSAGE -> {
                val binding: ItemSentMessageBinding = DataBindingUtil.inflate(
                    inflater, R.layout.item_sent_message, parent, false
                )
                SentMessageViewHolder(binding)
            }
            Constants.VIEW_TYPE_RECEIVED_MESSAGE -> {
                val binding: ItemReceivedMessageBinding = DataBindingUtil.inflate(
                    inflater, R.layout.item_received_message, parent, false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw Exception("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    class SentMessageViewHolder(
        private val binding: ItemSentMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessageModel) {
            binding.item = message
            binding.tvMessage.text = message.text
            binding.tvTime.text = DateTimeUtil.formatTime(message.timestamp)
            binding.executePendingBindings()
        }
    }

    class ReceivedMessageViewHolder(
        private val binding: ItemReceivedMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessageModel) {
            binding.item = message
            binding.tvSenderName.text = message.senderName
            binding.tvMessage.text = message.text
            binding.tvTime.text = DateTimeUtil.formatTime(message.timestamp)
            binding.executePendingBindings()
        }
    }

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<ChatMessageModel>() {
            override fun areItemsTheSame(oldItem: ChatMessageModel, newItem: ChatMessageModel): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(oldItem: ChatMessageModel, newItem: ChatMessageModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
