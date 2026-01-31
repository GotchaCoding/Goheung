package com.example.goheung.presentation.fragment.userlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.base.Resource
import com.example.goheung.base.SingleLiveEvent
import com.example.goheung.data.firebase.AuthRepository
import com.example.goheung.data.firebase.ChatRepository
import com.example.goheung.data.firebase.UserRepository
import com.example.goheung.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _users = MutableLiveData<Resource<List<UserModel>>>()
    val users: LiveData<Resource<List<UserModel>>> = _users

    private val _chatRoomCreated = SingleLiveEvent<Pair<String, String>>()
    val chatRoomCreated: LiveData<Pair<String, String>> = _chatRoomCreated

    private val currentUserId = authRepository.getCurrentUser()?.uid ?: ""

    fun loadUsers() {
        userRepository.getAllUsers()
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> _users.value = result
                    is Resource.Success -> {
                        // Filter out current user
                        val filteredUsers = result.model.filter { it.uid != currentUserId }
                        _users.value = Resource.Success(filteredUsers)
                    }
                    is Resource.Fail -> _users.value = result
                }
            }
            .launchIn(viewModelScope)
    }

    fun createOrGetChatRoom(otherUser: UserModel) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val participants = listOf(currentUserId, otherUser.uid).sorted()
        val participantNames = mapOf(
            currentUserId to (currentUser.displayName ?: ""),
            otherUser.uid to otherUser.displayName
        )

        chatRepository.createChatRoom(participants, participantNames)
            .onEach { result ->
                if (result is Resource.Success) {
                    _chatRoomCreated.value = Pair(result.model, otherUser.displayName)
                }
            }
            .launchIn(viewModelScope)
    }
}
