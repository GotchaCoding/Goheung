package com.example.goheung.presentation.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.fcm.FcmTokenManager
import com.example.goheung.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("이메일과 비밀번호를 입력해주세요")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = {
                    registerFcmToken()
                    _loginState.value = LoginState.Success
                },
                onFailure = { e ->
                    _loginState.value = LoginState.Error(e.message ?: "로그인에 실패했습니다")
                }
            )
        }
    }

    private fun registerFcmToken() {
        viewModelScope.launch {
            fcmTokenManager.registerToken()
                .onFailure { e ->
                    Log.w(TAG, "Failed to register FCM token", e)
                }
        }
    }

    fun clearError() {
        _loginState.value = LoginState.Idle
    }

    sealed class LoginState {
        data object Idle : LoginState()
        data object Loading : LoginState()
        data object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
