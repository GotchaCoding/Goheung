package com.example.goheung.presentation.more

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.Attendance
import com.example.goheung.data.model.AttendanceStatus
import com.example.goheung.data.model.User
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
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _profile = MutableLiveData<User>()
    val profile: LiveData<User> = _profile

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _currentAttendance = MutableLiveData<AttendanceStatus>()
    val currentAttendance: LiveData<AttendanceStatus> = _currentAttendance

    private val _attendanceUpdateSuccess = MutableLiveData<Boolean?>()
    val attendanceUpdateSuccess: LiveData<Boolean?> = _attendanceUpdateSuccess

    init {
        loadProfile()
        observeCurrentAttendance()
    }

    private fun loadProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            userRepository.getUser(uid)
                .onSuccess { _profile.value = it }
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

    fun logout() {
        authRepository.signOut()
    }
}
