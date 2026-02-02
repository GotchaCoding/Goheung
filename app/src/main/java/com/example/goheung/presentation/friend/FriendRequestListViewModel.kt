package com.example.goheung.presentation.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.FriendRequest
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestListViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _requests = MutableLiveData<List<FriendRequest>>(emptyList())
    val requests: LiveData<List<FriendRequest>> = _requests

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        loadRequests()
    }

    private fun loadRequests() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            friendRepository.getPendingRequests(uid).collect { result ->
                _loading.value = false
                result.onSuccess { _requests.value = it }
                    .onFailure { _message.value = it.message }
            }
        }
    }

    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            friendRepository.acceptFriendRequest(request)
                .onSuccess {
                    _message.value = "친구 요청을 수락했습니다"
                }
                .onFailure {
                    _message.value = it.message
                }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            friendRepository.rejectFriendRequest(requestId)
                .onSuccess {
                    _message.value = "친구 요청을 거절했습니다"
                }
                .onFailure {
                    _message.value = it.message
                }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
