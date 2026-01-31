package com.example.goheung.presentation.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.base.Resource
import com.example.goheung.data.firebase.AuthRepository
import com.example.goheung.data.firebase.UserRepository
import com.example.goheung.model.UserModel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authResult = MutableLiveData<Resource<FirebaseUser>>()
    val authResult: LiveData<Resource<FirebaseUser>> = _authResult

    fun login(email: String, password: String) {
        authRepository.login(email, password)
            .onEach { result ->
                _authResult.value = result
            }
            .launchIn(viewModelScope)
    }

    fun signUp(email: String, password: String, displayName: String) {
        authRepository.signUp(email, password)
            .onEach { result ->
                _authResult.value = result
                if (result is Resource.Success) {
                    val user = result.model
                    val userModel = UserModel(
                        uid = user.uid,
                        email = email,
                        displayName = displayName
                    )
                    userRepository.createOrUpdateUser(userModel)
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }
}
