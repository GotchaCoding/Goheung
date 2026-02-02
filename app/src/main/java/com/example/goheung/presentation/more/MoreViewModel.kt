package com.example.goheung.presentation.more

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.User
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _profile = MutableLiveData<User>()
    val profile: LiveData<User> = _profile

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            userRepository.getUser(uid)
                .onSuccess { _profile.value = it }
            _loading.value = false
        }
    }

    fun logout() {
        authRepository.signOut()
    }
}
