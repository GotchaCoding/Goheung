package com.example.goheung.presentation.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.User
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.ChatRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigateToChatDetail = MutableLiveData<String?>()
    val navigateToChatDetail: LiveData<String?> = _navigateToChatDetail

    init {
        loadUsers()
    }

    private fun loadUsers() {
        val currentUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            userRepository.getAllUsers(excludeUid = currentUid).collect { result ->
                _loading.value = false
                result.onSuccess { _users.value = it }
                    .onFailure { _error.value = it.message }
            }
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
                _navigateToChatDetail.value = chatRoomId
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
