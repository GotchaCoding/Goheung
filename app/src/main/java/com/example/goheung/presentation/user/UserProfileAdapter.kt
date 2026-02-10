package com.example.goheung.presentation.user

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    private val onUserClick: (User) -> Unit,
    private val onAttendanceChanged: (String, AttendanceStatus) -> Unit
) : ListAdapter<UserProfile, UserProfileAdapter.ViewHolder>(UserProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserProfileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onUserClick, onAttendanceChanged)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemUserProfileBinding,
        private val onUserClick: (User) -> Unit,
        private val onAttendanceChanged: (String, AttendanceStatus) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isSpinnerInitialized = false

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

            // 4. Attendance Spinner 설정
            setupAttendanceSpinner(profile)

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

        private fun setupAttendanceSpinner(profile: UserProfile) {
            val context = binding.root.context
            val statuses = AttendanceStatus.values()

            val adapter = ArrayAdapter(
                context,
                R.layout.spinner_attendance_item,
                statuses.map { it.displayName }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerAttendance.adapter = adapter

            // 현재 상태 선택
            val currentStatus = profile.attendance?.status?.let {
                try {
                    AttendanceStatus.valueOf(it)
                } catch (e: Exception) {
                    AttendanceStatus.WORKING
                }
            } ?: AttendanceStatus.WORKING

            isSpinnerInitialized = false
            binding.spinnerAttendance.setSelection(statuses.indexOf(currentStatus))

            // 변경 리스너
            binding.spinnerAttendance.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (isSpinnerInitialized) {
                            onAttendanceChanged(profile.user.uid, statuses[position])
                        }
                        isSpinnerInitialized = true
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
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
