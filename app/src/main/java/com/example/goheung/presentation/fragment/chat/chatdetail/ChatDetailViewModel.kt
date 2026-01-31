package com.example.goheung.presentation.fragment.chat.chatdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.constants.Constants
import com.example.goheung.data.firebase.AuthRepository
import com.example.goheung.data.firebase.ChatRepository
import com.example.goheung.model.ChatMessageModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessageModel>>()
    val messages: LiveData<List<ChatMessageModel>> = _messages

    fun getCurrentUserId(): String {
        return authRepository.getCurrentUser()?.uid ?: ""
    }

    fun loadMessages(chatRoomId: String) {
        chatRepository.getMessages(chatRoomId)
            .onEach { messages ->
                val messagesWithViewType = messages.map { msg ->
                    msg.copy(
                        viewType = if (msg.senderId == getCurrentUserId()) {
                            Constants.VIEW_TYPE_SENT_MESSAGE
                        } else {
                            Constants.VIEW_TYPE_RECEIVED_MESSAGE
                        }
                    )
                }
                _messages.value = messagesWithViewType
            }
            .catch { /* handle error */ }
            .launchIn(viewModelScope)
    }

    fun sendMessage(chatRoomId: String, text: String) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val message = ChatMessageModel(
            senderId = currentUser.uid,
            senderName = currentUser.displayName ?: "",
            text = text,
            timestamp = System.currentTimeMillis()
        )
        chatRepository.sendMessage(chatRoomId, message)
            .launchIn(viewModelScope)
    }
}
