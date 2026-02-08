package com.example.goheung.presentation.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.ChatRoomType
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

    companion object {
        private const val TAG = "ChatListViewModel"
    }

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
        val myUid = authRepository.currentUser?.uid ?: run {
            Log.e(TAG, "loadChatRooms: currentUser is null")
            return
        }

        Log.d(TAG, "loadChatRooms: Loading chat rooms for uid=$myUid")
        viewModelScope.launch {
            _loading.value = true
            chatRepository.getChatRooms(myUid)
                .catch { e ->
                    Log.e(TAG, "loadChatRooms: Error in flow", e)
                    _loading.value = false
                    _error.value = e.message ?: "Failed to load chat rooms"
                }
                .collect { result ->
                    _loading.value = false
                    result.fold(
                        onSuccess = { rooms ->
                            Log.d(TAG, "loadChatRooms: Received ${rooms.size} chat rooms")
                            processChatRooms(rooms, myUid)
                            _error.value = null
                        },
                        onFailure = { e ->
                            Log.e(TAG, "loadChatRooms: Failed", e)
                            _error.value = e.message ?: "Failed to load chat rooms"
                        }
                    )
                }
        }
    }

    private suspend fun processChatRooms(rooms: List<ChatRoom>, myUid: String) {
        Log.d(TAG, "processChatRooms: Processing ${rooms.size} rooms")

        // hiddenBy로 필터링 (숨긴 채팅방 제외)
        val visibleRooms = rooms.filter { !it.isHiddenBy(myUid) }
        Log.d(TAG, "processChatRooms: Visible rooms after filtering: ${visibleRooms.size}")

        val allParticipantUids = visibleRooms.flatMap { it.participants }.distinct()
        Log.d(TAG, "processChatRooms: Found ${allParticipantUids.size} unique participants")

        val usersResult = userRepository.getUsers(allParticipantUids)

        usersResult.fold(
            onSuccess = { users ->
                Log.d(TAG, "processChatRooms: Loaded ${users.size} users")
                val userMap = users.associateBy { it.uid }
                val roomsWithParticipants = visibleRooms.map { room ->
                    val participants = room.participants.mapNotNull { userMap[it] }
                    val displayName = calculateDisplayName(room, participants, myUid)
                    Log.d(TAG, "processChatRooms: Room ${room.id} -> displayName='$displayName'")
                    ChatRoomWithParticipants(room, participants, displayName)
                }

                val directMessages = roomsWithParticipants
                    .filter { it.chatRoom.type == ChatRoomType.DM }
                    .sortedWith(
                        compareByDescending<ChatRoomWithParticipants> { it.chatRoom.isFavoriteBy(myUid) }
                            .thenByDescending { it.chatRoom.lastMessageTimestamp }
                    )
                val groupMessages = roomsWithParticipants
                    .filter { it.chatRoom.type == ChatRoomType.GROUP }
                    .sortedWith(
                        compareByDescending<ChatRoomWithParticipants> { it.chatRoom.isFavoriteBy(myUid) }
                            .thenByDescending { it.chatRoom.lastMessageTimestamp }
                    )

                Log.d(TAG, "processChatRooms: DMs=${directMessages.size}, Groups=${groupMessages.size}")
                _directChats.value = directMessages
                _groupChats.value = groupMessages
            },
            onFailure = { e ->
                Log.e(TAG, "processChatRooms: Failed to load users", e)
                _error.value = e.message ?: "Failed to load participants"
            }
        )
    }

    private fun calculateDisplayName(room: ChatRoom, participants: List<User>, myUid: String): String {
        Log.d(TAG, "calculateDisplayName: room.id=${room.id}, room.name='${room.name}', room.type=${room.type}, room.participants=${room.participants.size}")
        Log.d(TAG, "  Loaded participants=${participants.size}: ${participants.map { "${it.displayName}(${it.uid})" }}")
        Log.d(TAG, "  myUid=$myUid")

        val result = when (room.type) {
            ChatRoomType.DM -> {
                // DM: 상대방 이름 표시 (participants에서 나를 제외한 사람)
                val other = participants.firstOrNull { it.uid != myUid }
                Log.d(TAG, "  DM logic: other user = ${other?.displayName}(${other?.uid})")
                other?.displayName ?: room.name.ifEmpty { "Unknown User" }
            }
            ChatRoomType.GROUP -> {
                // 그룹: 방 이름 우선, 없으면 참여자 나열
                room.name.ifEmpty {
                    if (participants.isEmpty()) {
                        "Empty Group"
                    } else {
                        participants.joinToString(", ") { it.displayName }
                    }
                }
            }
        }
        Log.d(TAG, "  → result='$result'")
        return result
    }

    fun createChatRoom(name: String, description: String, userId: String) {
        viewModelScope.launch {
            _loading.value = true
            val chatRoom = ChatRoom(
                name = name,
                description = description,
                participants = listOf(userId),
                createdBy = userId,
                type = ChatRoomType.GROUP
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

    fun toggleFavorite(chatRoomId: String) {
        val myUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = chatRepository.toggleFavorite(chatRoomId, myUid)
            result.onFailure { e ->
                _error.value = e.message ?: "즐겨찾기 설정 실패"
            }
            // 즐겨찾기 변경 후 자동으로 리스트 업데이트됨 (실시간 리스너)
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * 채팅방 나가기
     * DM: participants에서 제거 (상대방에게는 여전히 보임, 메시지 오면 다시 나타남)
     * GROUP: participants에서 제거 + 시스템 메시지 추가
     */
    fun leaveChatRoom(chatRoomId: String, chatRoomType: ChatRoomType) {
        val myUid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val userResult = userRepository.getUser(myUid)
            userResult.fold(
                onSuccess = { user ->
                    val leaveResult = chatRepository.leaveChatRoom(chatRoomId, myUid, user.displayName)
                    leaveResult.onFailure { e ->
                        _error.value = e.message ?: "채팅방 나가기 실패"
                    }
                },
                onFailure = { e ->
                    _error.value = e.message ?: "사용자 정보 로드 실패"
                }
            )
        }
    }
}
