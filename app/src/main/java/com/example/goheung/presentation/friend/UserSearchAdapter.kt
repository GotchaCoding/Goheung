package com.example.goheung.presentation.friend

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.goheung.R
import com.example.goheung.databinding.ItemUserSearchBinding

class UserSearchAdapter(
    private val onSendRequest: (UserSearchItem) -> Unit
) : ListAdapter<UserSearchItem, UserSearchAdapter.UserSearchViewHolder>(UserSearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSearchViewHolder {
        val binding = ItemUserSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserSearchViewHolder(binding, onSendRequest)
    }

    override fun onBindViewHolder(holder: UserSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserSearchViewHolder(
        private val binding: ItemUserSearchBinding,
        private val onSendRequest: (UserSearchItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserSearchItem) {
            binding.textViewName.text = item.user.displayName
            binding.textViewDepartment.text = item.user.department

            Glide.with(binding.imageViewAvatar)
                .load(item.user.profileImageUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imageViewAvatar)

            when {
                item.isFriend -> {
                    binding.buttonSendRequest.isVisible = false
                    binding.textViewStatus.isVisible = true
                    binding.textViewStatus.text = binding.root.context.getString(R.string.already_friend)
                }
                item.isRequestPending -> {
                    binding.buttonSendRequest.isVisible = false
                    binding.textViewStatus.isVisible = true
                    binding.textViewStatus.text = binding.root.context.getString(R.string.request_pending)
                }
                else -> {
                    binding.buttonSendRequest.isVisible = true
                    binding.textViewStatus.isVisible = false
                    binding.buttonSendRequest.setOnClickListener {
                        onSendRequest(item)
                    }
                }
            }
        }
    }

    private class UserSearchDiffCallback : DiffUtil.ItemCallback<UserSearchItem>() {
        override fun areItemsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean {
            return oldItem.user.uid == newItem.user.uid
        }

        override fun areContentsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean {
            return oldItem == newItem
        }
    }
}
