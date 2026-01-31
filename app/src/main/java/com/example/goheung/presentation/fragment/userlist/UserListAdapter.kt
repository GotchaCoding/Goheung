package com.example.goheung.presentation.fragment.userlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goheung.databinding.ItemUserBinding
import com.example.goheung.model.UserModel

class UserListAdapter(
    private val onUserClick: (UserModel) -> Unit
) : ListAdapter<UserModel, UserListAdapter.ViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onUserClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemUserBinding,
        private val onUserClick: (UserModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            binding.tvName.text = user.displayName
            binding.tvEmail.text = user.email
            binding.root.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<UserModel>() {
            override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel) =
                oldItem.uid == newItem.uid

            override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel) =
                oldItem == newItem
        }
    }
}
