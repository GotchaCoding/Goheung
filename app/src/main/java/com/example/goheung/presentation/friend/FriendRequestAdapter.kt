package com.example.goheung.presentation.friend

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.goheung.R
import com.example.goheung.data.model.FriendRequest
import com.example.goheung.databinding.ItemFriendRequestBinding

class FriendRequestAdapter(
    private val onAccept: (FriendRequest) -> Unit,
    private val onReject: (String) -> Unit
) : ListAdapter<FriendRequest, FriendRequestAdapter.RequestViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding, onAccept, onReject)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RequestViewHolder(
        private val binding: ItemFriendRequestBinding,
        private val onAccept: (FriendRequest) -> Unit,
        private val onReject: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: FriendRequest) {
            binding.textViewName.text = request.fromDisplayName

            Glide.with(binding.imageViewAvatar)
                .load(request.fromProfileImageUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imageViewAvatar)

            binding.buttonAccept.setOnClickListener {
                onAccept(request)
            }

            binding.buttonReject.setOnClickListener {
                onReject(request.id)
            }
        }
    }

    private class RequestDiffCallback : DiffUtil.ItemCallback<FriendRequest>() {
        override fun areItemsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean {
            return oldItem == newItem
        }
    }
}
