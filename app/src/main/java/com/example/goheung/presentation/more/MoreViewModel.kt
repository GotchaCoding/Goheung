package com.example.goheung.presentation.more

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.fcm.FcmTokenManager
import com.example.goheung.data.model.Attendance
import com.example.goheung.data.model.AttendanceStatus
import com.example.goheung.data.model.User
import com.example.goheung.data.model.UserRole
import com.example.goheung.data.repository.AttendanceRepository
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "MoreViewModel"
    }

    private val _profile = MutableLiveData<User>()
    val profile: LiveData<User> = _profile

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _currentAttendance = MutableLiveData<AttendanceStatus>()
    val currentAttendance: LiveData<AttendanceStatus> = _currentAttendance

    private val _attendanceUpdateSuccess = MutableLiveData<Boolean?>()
    val attendanceUpdateSuccess: LiveData<Boolean?> = _attendanceUpdateSuccess

    private val _currentRole = MutableLiveData<UserRole>()
    val currentRole: LiveData<UserRole> = _currentRole

    private val _roleUpdateSuccess = MutableLiveData<Boolean?>()
    val roleUpdateSuccess: LiveData<Boolean?> = _roleUpdateSuccess

    init {
        loadProfile()
        observeCurrentAttendance()
    }

    private fun loadProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            userRepository.getUser(uid)
                .onSuccess { user ->
                    _profile.value = user
                    _currentRole.value = UserRole.fromString(user.role)
                }
            _loading.value = false
        }
    }

    private fun observeCurrentAttendance() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            attendanceRepository.observeAttendance(uid).collect { attendance ->
                val status = attendance?.status?.let {
                    try {
                        AttendanceStatus.valueOf(it)
                    } catch (e: Exception) {
                        AttendanceStatus.WORKING
                    }
                } ?: AttendanceStatus.WORKING
                _currentAttendance.value = status
            }
        }
    }

    fun updateAttendance(status: AttendanceStatus) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            attendanceRepository.updateAttendance(uid, status)
                .onSuccess {
                    _attendanceUpdateSuccess.value = true
                }
                .onFailure {
                    _attendanceUpdateSuccess.value = false
                }
            _loading.value = false
        }
    }

    fun updateRole(role: UserRole) {
        val uid = authRepository.currentUser?.uid ?: return
        Log.d(TAG, "updateRole called with role=${role.name}, uid=$uid")
        viewModelScope.launch {
            _loading.value = true
            _roleUpdateSuccess.value = null  // 초기화
            userRepository.updateUserRole(uid, role.name)
                .onSuccess {
                    Log.d(TAG, "Role updated successfully to ${role.name} in Firebase")
                    _currentRole.value = role
                    _roleUpdateSuccess.value = true
                    // 프로필도 함께 업데이트 (캐시된 값 갱신)
                    _profile.value = _profile.value?.copy(role = role.name)
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to update role in Firebase", e)
                    _roleUpdateSuccess.value = false
                }
            _loading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            fcmTokenManager.clearToken()
                .onFailure { e ->
                    Log.w(TAG, "Failed to clear FCM token", e)
                }
            authRepository.signOut()
        }
    }
}
