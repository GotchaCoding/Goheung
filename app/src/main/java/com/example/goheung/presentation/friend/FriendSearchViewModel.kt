package com.example.goheung.presentation.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.FriendRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendSearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<UserSearchItem>>(emptyList())
    val searchResults: LiveData<List<UserSearchItem>> = _searchResults

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        val myUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            userRepository.searchUsers(query)
                .onSuccess { users ->
                    val filteredUsers = users.filter { it.uid != myUid }
                    val enrichedItems = filteredUsers.map { user ->
                        val isFriend = friendRepository.isFriend(myUid, user.uid)
                        val isPending = friendRepository.hasPendingRequest(myUid, user.uid)
                        UserSearchItem(user, isFriend, isPending)
                    }
                    _searchResults.value = enrichedItems
                }
                .onFailure {
                    _message.value = it.message
                }
            _loading.value = false
        }
    }

    fun sendFriendRequest(userSearchItem: UserSearchItem) {
        val myUid = authRepository.currentUser?.uid ?: return
        val myName = authRepository.currentUser?.displayName ?: ""
        val myProfileImageUrl = authRepository.currentUser?.photoUrl?.toString()

        viewModelScope.launch {
            friendRepository.sendFriendRequest(
                fromUid = myUid,
                toUid = userSearchItem.user.uid,
                fromDisplayName = myName,
                fromProfileImageUrl = myProfileImageUrl
            ).onSuccess {
                _message.value = "친구 요청을 보냈습니다"
                // Refresh search to update button state
                search(userSearchItem.user.displayName)
            }.onFailure {
                _message.value = it.message
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
