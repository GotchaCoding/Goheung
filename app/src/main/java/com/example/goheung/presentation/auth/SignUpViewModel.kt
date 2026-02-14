package com.example.goheung.presentation.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.fcm.FcmTokenManager
import com.example.goheung.data.model.User
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val _signUpState = MutableLiveData<SignUpState>()
    val signUpState: LiveData<SignUpState> = _signUpState

    fun signUp(displayName: String, email: String, password: String, department: String) {
        if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
            _signUpState.value = SignUpState.Error("이름, 이메일, 비밀번호는 필수 입력입니다")
            return
        }

        if (password.length < 6) {
            _signUpState.value = SignUpState.Error("비밀번호는 6자 이상이어야 합니다")
            return
        }

        _signUpState.value = SignUpState.Loading
        viewModelScope.launch {
            val authResult = authRepository.signUp(email, password, displayName)
            authResult.fold(
                onSuccess = { firebaseUser ->
                    val user = User(
                        uid = firebaseUser.uid,
                        email = email,
                        displayName = displayName,
                        department = department
                    )
                    val createResult = userRepository.createUser(user)
                    createResult.fold(
                        onSuccess = {
                            registerFcmToken()
                            _signUpState.value = SignUpState.Success
                        },
                        onFailure = { e ->
                            _signUpState.value = SignUpState.Error(
                                e.message ?: "사용자 프로필 생성에 실패했습니다"
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _signUpState.value = SignUpState.Error(e.message ?: "회원가입에 실패했습니다")
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
        _signUpState.value = SignUpState.Idle
    }

    sealed class SignUpState {
        data object Idle : SignUpState()
        data object Loading : SignUpState()
        data object Success : SignUpState()
        data class Error(val message: String) : SignUpState()
    }
}
