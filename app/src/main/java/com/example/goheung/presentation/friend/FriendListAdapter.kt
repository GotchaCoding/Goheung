package com.example.goheung.presentation.friend

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.goheung.R
import com.example.goheung.data.model.Friend
import com.example.goheung.databinding.ItemFriendBinding

class FriendListAdapter(
    private val onFriendClick: (Friend) -> Unit
) : ListAdapter<Friend, FriendListAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding, onFriendClick)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendViewHolder(
        private val binding: ItemFriendBinding,
        private val onFriendClick: (Friend) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: Friend) {
            binding.textViewName.text = friend.displayName

            Glide.with(binding.imageViewAvatar)
                .load(friend.profileImageUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imageViewAvatar)

            binding.root.setOnClickListener {
                onFriendClick(friend)
            }
        }
    }

    private class FriendDiffCallback : DiffUtil.ItemCallback<Friend>() {
        override fun areItemsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return oldItem == newItem
        }
    }
}
