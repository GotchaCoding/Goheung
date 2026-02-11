package com.example.goheung.presentation.user

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.goheung.R
import com.example.goheung.data.model.AttendanceStatus
import com.example.goheung.data.model.User
import com.example.goheung.data.model.UserProfile
import com.example.goheung.databinding.ItemUserProfileBinding

class UserProfileAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<UserProfile, UserProfileAdapter.ViewHolder>(UserProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserProfileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onUserClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemUserProfileBinding,
        private val onUserClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: UserProfile) {
            // 1. 기본 정보
            binding.textViewName.text = profile.user.displayName
            binding.textViewDepartment.text = profile.user.department

            // 2. 프로필 이미지 (Glide)
            Glide.with(binding.imageViewAvatar)
                .load(profile.user.profileImageUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imageViewAvatar)

            // 3. Presence 업데이트
            updatePresence(profile)

            // 4. Attendance 상태 표시
            displayAttendanceStatus(profile)

            // 5. 클릭 리스너
            binding.root.setOnClickListener { onUserClick(profile.user) }
        }

        private fun updatePresence(profile: UserProfile) {
            // Indicator 색상
            val color = when {
                profile.presence?.online == true && profile.presence.inChat ->
                    R.color.presenceInChat
                profile.presence?.online == true ->
                    R.color.presenceOnline
                else ->
                    R.color.presenceOffline
            }

            binding.viewPresenceIndicator.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, color)
                )

            // Presence 텍스트
            binding.textViewPresence.text = profile.getPresenceText()
        }

        private fun displayAttendanceStatus(profile: UserProfile) {
            val currentStatus = profile.attendance?.status?.let {
                try {
                    AttendanceStatus.valueOf(it)
                } catch (e: Exception) {
                    AttendanceStatus.WORKING
                }
            } ?: AttendanceStatus.WORKING

            binding.textViewAttendance.text = currentStatus.displayName
        }
    }

    private class UserProfileDiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(old: UserProfile, new: UserProfile): Boolean {
            return old.user.uid == new.user.uid
        }

        override fun areContentsTheSame(old: UserProfile, new: UserProfile): Boolean {
            return old == new
        }
    }
}
