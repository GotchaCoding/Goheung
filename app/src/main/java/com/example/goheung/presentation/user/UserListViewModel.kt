package com.example.goheung.presentation.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.AttendanceStatus
import com.example.goheung.data.model.User
import com.example.goheung.data.model.UserProfile
import com.example.goheung.data.repository.AttendanceRepository
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.ChatRepository
import com.example.goheung.data.repository.PresenceRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val presenceRepository: PresenceRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    // UserProfile로 변경
    private val _userProfiles = MutableLiveData<List<UserProfile>>(emptyList())
    val userProfiles: LiveData<List<UserProfile>> = _userProfiles

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigateToChatDetail = MutableLiveData<Pair<String, String>?>()
    val navigateToChatDetail: LiveData<Pair<String, String>?> = _navigateToChatDetail

    init {
        loadUserProfiles()
        setupPresenceForCurrentUser()
    }

    private fun loadUserProfiles() {
        val currentUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true

            userRepository.getAllUsers(excludeUid = currentUid).collect { result ->
                result.onSuccess { users ->
                    // 각 사용자의 Presence와 Attendance를 결합
                    val profileFlows = users.map { user ->
                        combine(
                            presenceRepository.observePresence(user.uid),
                            attendanceRepository.observeAttendance(user.uid)
                        ) { presence, attendance ->
                            UserProfile(user, presence, attendance)
                        }
                    }

                    // 모든 Flow 결합
                    if (profileFlows.isEmpty()) {
                        _userProfiles.value = emptyList()
                        _loading.value = false
                    } else {
                        combine(profileFlows) { it.toList() }
                            .collect { profiles ->
                                _userProfiles.value = profiles
                                _loading.value = false
                            }
                    }
                }
                .onFailure {
                    _error.value = it.message
                    _loading.value = false
                }
            }
        }
    }

    private fun setupPresenceForCurrentUser() {
        val currentUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            presenceRepository.setOnline(currentUid)
            presenceRepository.setupDisconnectHandler(currentUid)
        }
    }

    fun onAttendanceChanged(uid: String, status: AttendanceStatus) {
        viewModelScope.launch {
            attendanceRepository.updateAttendance(uid, status)
                .onFailure { _error.value = it.message }
        }
    }

    fun onUserClicked(user: User) {
        val myUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            val result = chatRepository.findOrCreateDirectChat(
                myUid = myUid,
                friendUid = user.uid,
                chatName = user.displayName
            )
            _loading.value = false

            result.onSuccess { chatRoomId ->
                _navigateToChatDetail.value = Pair(chatRoomId, user.displayName)
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun onNavigationComplete() {
        _navigateToChatDetail.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
