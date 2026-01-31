package com.example.goheung.presentation.fragment.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.base.Resource
import com.example.goheung.base.SingleLiveEvent
import com.example.goheung.data.firebase.AuthRepository
import com.example.goheung.data.firebase.UserRepository
import com.example.goheung.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableLiveData<UserModel>()
    val user: LiveData<UserModel> = _user

    private val _logoutEvent = SingleLiveEvent<Unit>()
    val logoutEvent: LiveData<Unit> = _logoutEvent

    fun loadProfile() {
        val currentUser = authRepository.getCurrentUser() ?: return
        userRepository.getUser(currentUser.uid)
            .onEach { result ->
                if (result is Resource.Success) {
                    _user.value = result.model
                }
            }
            .launchIn(viewModelScope)
    }

    fun logout() {
        authRepository.logout()
        _logoutEvent.call()
    }
}
