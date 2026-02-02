package com.example.goheung.presentation.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.User
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.ChatRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    data class ChatRoomWithParticipants(
        val chatRoom: ChatRoom,
        val participants: List<User>,
        val displayName: String
    )

    private val _directChats = MutableLiveData<List<ChatRoomWithParticipants>>()
    val directChats: LiveData<List<ChatRoomWithParticipants>> = _directChats

    private val _groupChats = MutableLiveData<List<ChatRoomWithParticipants>>()
    val groupChats: LiveData<List<ChatRoomWithParticipants>> = _groupChats

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _createdChatRoom = MutableLiveData<Pair<String, String>?>()
    val createdChatRoom: LiveData<Pair<String, String>?> = _createdChatRoom

    init {
        loadChatRooms()
    }

    private fun loadChatRooms() {
        val myUid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _loading.value = true
            chatRepository.getChatRooms(myUid)
                .catch { e ->
                    _loading.value = false
                    _error.value = e.message ?: "Failed to load chat rooms"
                }
                .collect { result ->
                    _loading.value = false
                    result.fold(
                        onSuccess = { rooms ->
                            processChatRooms(rooms, myUid)
                            _error.value = null
                        },
                        onFailure = { e ->
                            _error.value = e.message ?: "Failed to load chat rooms"
                        }
                    )
                }
        }
    }

    private suspend fun processChatRooms(rooms: List<ChatRoom>, myUid: String) {
        val allParticipantUids = rooms.flatMap { it.participants }.distinct()
        val usersResult = userRepository.getUsers(allParticipantUids)

        usersResult.fold(
            onSuccess = { users ->
                val userMap = users.associateBy { it.uid }
                val roomsWithParticipants = rooms.map { room ->
                    val participants = room.participants.mapNotNull { userMap[it] }
                    val displayName = calculateDisplayName(room, participants, myUid)
                    ChatRoomWithParticipants(room, participants, displayName)
                }

                val directMessages = roomsWithParticipants.filter { it.chatRoom.participants.size == 2 }
                val groupMessages = roomsWithParticipants.filter { it.chatRoom.participants.size != 2 }

                _directChats.value = directMessages
                _groupChats.value = groupMessages
            },
            onFailure = { e ->
                _error.value = e.message ?: "Failed to load participants"
            }
        )
    }

    private fun calculateDisplayName(room: ChatRoom, participants: List<User>, myUid: String): String {
        return when (room.participants.size) {
            2 -> {
                // DM: 상대방 이름 표시
                participants.firstOrNull { it.uid != myUid }?.displayName ?: "Unknown"
            }
            1 -> {
                // 나만 있는 그룹: 방 이름 또는 내 이름
                room.name.ifEmpty {
                    participants.firstOrNull()?.displayName ?: "Group Chat"
                }
            }
            else -> {
                // 그룹: 방 이름 또는 참여자 나열
                room.name.ifEmpty {
                    participants.joinToString(", ") { it.displayName }
                }
            }
        }
    }

    fun createChatRoom(name: String, description: String, userId: String) {
        viewModelScope.launch {
            _loading.value = true
            val chatRoom = ChatRoom(
                name = name,
                description = description,
                participants = listOf(userId),
                createdBy = userId
            )

            val result = chatRepository.createChatRoom(chatRoom)
            _loading.value = false

            result.fold(
                onSuccess = { chatRoomId ->
                    _createdChatRoom.value = Pair(chatRoomId, name)
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to create chat room"
                }
            )
        }
    }

    fun onNavigatedToCreatedRoom() {
        _createdChatRoom.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
