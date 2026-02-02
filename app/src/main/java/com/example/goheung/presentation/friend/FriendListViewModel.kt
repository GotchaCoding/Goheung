package com.example.goheung.presentation.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.Friend
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendListViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _friends = MutableLiveData<List<Friend>>(emptyList())
    val friends: LiveData<List<Friend>> = _friends

    private val _pendingRequestCount = MutableLiveData(0)
    val pendingRequestCount: LiveData<Int> = _pendingRequestCount

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadFriends()
        loadPendingRequestCount()
    }

    private fun loadFriends() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            friendRepository.getFriends(uid).collect { result ->
                _loading.value = false
                result.onSuccess { _friends.value = it }
                    .onFailure { _error.value = it.message }
            }
        }
    }

    private fun loadPendingRequestCount() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            friendRepository.getPendingRequests(uid).collect { result ->
                result.onSuccess { _pendingRequestCount.value = it.size }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
