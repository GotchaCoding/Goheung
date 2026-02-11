package com.example.goheung.presentation.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.AttendanceStatus
import com.example.goheung.data.model.User
import com.example.goheung.data.model.UserProfile
import com.example.goheung.data.repository.AttendanceRepository
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.ChatRepository
import com.example.goheung.data.repository.PresenceRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val presenceRepository: PresenceRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    // UserProfile로 변경
    private val _userProfiles = MutableLiveData<List<UserProfile>>(emptyList())
    val userProfiles: LiveData<List<UserProfile>> = _userProfiles

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigateToChatDetail = MutableLiveData<Pair<String, String>?>()
    val navigateToChatDetail: LiveData<Pair<String, String>?> = _navigateToChatDetail

    // 검색 기능을 위한 필드
    private var allUserProfiles: List<UserProfile> = emptyList()
    private val _searchQuery = MutableLiveData("")
    private var searchJob: Job? = null
    private val searchDelayMillis: Long = 1000

    init {
        loadUserProfiles()
        setupPresenceForCurrentUser()
    }

    private fun loadUserProfiles() {
        val currentUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true

            userRepository.getAllUsers(excludeUid = currentUid).collect { result ->
                result.onSuccess { users ->
                    // 각 사용자의 Presence와 Attendance를 결합
                    val profileFlows = users.map { user ->
                        combine(
                            presenceRepository.observePresence(user.uid),
                            attendanceRepository.observeAttendance(user.uid)
                        ) { presence, attendance ->
                            UserProfile(user, presence, attendance)
                        }
                    }

                    // 모든 Flow 결합
                    if (profileFlows.isEmpty()) {
                        allUserProfiles = emptyList()
                        _userProfiles.value = emptyList()
                        _loading.value = false
                    } else {
                        combine(profileFlows) { it.toList() }
                            .collect { profiles ->
                                allUserProfiles = profiles

                                // 현재 검색어가 있으면 필터링 적용
                                val currentQuery = _searchQuery.value ?: ""
                                if (currentQuery.isEmpty()) {
                                    _userProfiles.value = profiles
                                } else {
                                    performSearch(currentQuery)
                                }

                                _loading.value = false
                            }
                    }
                }
                .onFailure {
                    _error.value = it.message
                    _loading.value = false
                }
            }
        }
    }

    private fun setupPresenceForCurrentUser() {
        val currentUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            presenceRepository.setOnline(currentUid)
            presenceRepository.setupDisconnectHandler(currentUid)
        }
    }

    fun onUserClicked(user: User) {
        val myUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            val result = chatRepository.findOrCreateDirectChat(
                myUid = myUid,
                friendUid = user.uid,
                chatName = user.displayName
            )
            _loading.value = false

            result.onSuccess { chatRoomId ->
                _navigateToChatDetail.value = Pair(chatRoomId, user.displayName)
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun onNavigationComplete() {
        _navigateToChatDetail.value = null
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * TMDB 패턴을 적용한 Debounce 검색
     * Fragment에서 EditText의 텍스트 변경 시마다 호출
     */
    fun onSearchQueryChanged(query: String) {
        // 기존 검색 Job 취소
        searchJob?.cancel()

        // 새로운 검색 Job 시작
        searchJob = viewModelScope.launch {
            delay(searchDelayMillis)  // 1초 대기
            _searchQuery.value = query.trim()
            performSearch(query.trim())
        }
    }

    /**
     * 실제 검색 수행 (클라이언트 필터링)
     */
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // 검색어가 없으면 전체 목록 표시
            _userProfiles.value = allUserProfiles
            return
        }

        // displayName 또는 department에서 대소문자 구분 없이 검색
        val filteredProfiles = allUserProfiles.filter { profile ->
            profile.user.displayName.contains(query, ignoreCase = true) ||
            profile.user.department.contains(query, ignoreCase = true)
        }

        _userProfiles.value = filteredProfiles
    }

    /**
     * 검색어 초기화
     */
    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _userProfiles.value = allUserProfiles
    }
}
