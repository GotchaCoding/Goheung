package com.example.goheung.presentation.location

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.UserLocation
import com.example.goheung.data.repository.LocationRepository
import com.example.goheung.data.repository.UserRepository
import com.example.goheung.service.LocationService
import com.example.goheung.util.LocationUtils
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val locationService: LocationService,
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val TAG = "LocationViewModel"
        private const val LOCATION_UPDATE_INTERVAL = 1000L  // 1초
    }

    private val _allLocations = MutableLiveData<List<UserLocation>>()
    val allLocations: LiveData<List<UserLocation>> = _allLocations

    private val _arrivalTime = MutableLiveData<String?>()
    val arrivalTime: LiveData<String?> = _arrivalTime

    private val _myLocation = MutableLiveData<UserLocation?>()
    val myLocation: LiveData<UserLocation?> = _myLocation

    private val _locationPermissionGranted = MutableLiveData<Boolean>()
    val locationPermissionGranted: LiveData<Boolean> = _locationPermissionGranted

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _mySpeed = MutableLiveData<Float>()
    val mySpeed: LiveData<Float> = _mySpeed

    private val _myBearing = MutableLiveData<Float?>()
    val myBearing: LiveData<Float?> = _myBearing

    private var previousBearing: Float? = null

    init {
        observeLocations()
        setupDisconnectHandler()
    }

    /**
     * 모든 위치 실시간 구독
     */
    private fun observeLocations() {
        viewModelScope.launch {
            locationRepository.observeAllLocations()
                .catch { e ->
                    Log.e(TAG, "Failed to observe locations", e)
                    _errorMessage.value = "위치 정보를 불러올 수 없습니다"
                }
                .collect { locations ->
                    _allLocations.value = locations
                    updateMyLocation(locations)
                    calculateArrivalTime(locations)
                }
        }
    }

    /**
     * 내 위치 업데이트
     */
    private fun updateMyLocation(locations: List<UserLocation>) {
        val currentUid = auth.currentUser?.uid ?: return
        _myLocation.value = locations.find { it.uid == currentUid }
    }

    /**
     * 도착 시간 계산
     */
    private fun calculateArrivalTime(locations: List<UserLocation>) {
        val currentUid = auth.currentUser?.uid ?: return
        val myLocation = locations.find { it.uid == currentUid } ?: return

        when (myLocation.role) {
            "PASSENGER" -> {
                // 승객: 가장 가까운 운전기사까지의 시간
                val driver = locations
                    .filter { it.role == "DRIVER" }
                    .minByOrNull {
                        LocationUtils.calculateDistance(
                            myLocation.lat, myLocation.lng,
                            it.lat, it.lng
                        )
                    }

                if (driver != null) {
                    val distance = LocationUtils.calculateDistance(
                        driver.lat, driver.lng,
                        myLocation.lat, myLocation.lng
                    )
                    _arrivalTime.value = LocationUtils.formatArrivalTime(distance)
                } else {
                    _arrivalTime.value = null
                }
            }
            "DRIVER" -> {
                // 운전기사: 가장 가까운 승객까지의 시간
                val passenger = locations
                    .filter { it.role == "PASSENGER" }
                    .minByOrNull {
                        LocationUtils.calculateDistance(
                            myLocation.lat, myLocation.lng,
                            it.lat, it.lng
                        )
                    }

                if (passenger != null) {
                    val distance = LocationUtils.calculateDistance(
                        myLocation.lat, myLocation.lng,
                        passenger.lat, passenger.lng
                    )
                    _arrivalTime.value = "가장 가까운 승객: ${LocationUtils.formatArrivalTime(distance)}"
                } else {
                    _arrivalTime.value = null
                }
            }
            else -> {
                _arrivalTime.value = null
            }
        }
    }

    /**
     * 위치 업데이트 시작
     */
    fun startLocationUpdates() {
        if (!locationService.hasLocationPermission()) {
            _locationPermissionGranted.value = false
            _errorMessage.value = "위치 권한이 필요합니다"
            return
        }

        _locationPermissionGranted.value = true

        locationService.startLocationUpdates(LOCATION_UPDATE_INTERVAL) { location ->
            viewModelScope.launch {
                val user = auth.currentUser ?: return@launch
                val userProfile = userRepository.getUser(user.uid).getOrNull()

                // 속도/방향/정확도 추출
                val speed = if (location.hasSpeed()) location.speed else 0f
                val rawBearing = if (location.hasBearing()) location.bearing else 0f
                val bearing = if (location.hasBearing()) smoothBearing(rawBearing) else 0f
                val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
                val hasBearing = location.hasBearing()

                // LiveData 업데이트
                _mySpeed.value = speed
                _myBearing.value = if (hasBearing) bearing else null

                locationRepository.updateLocation(
                    uid = user.uid,
                    lat = location.latitude,
                    lng = location.longitude,
                    role = userProfile?.role ?: "PASSENGER",
                    displayName = userProfile?.displayName ?: "Unknown",
                    speed = speed,
                    bearing = bearing,
                    accuracy = accuracy,
                    hasBearing = hasBearing
                )

                Log.d(TAG, "Location updated: lat=${location.latitude}, lng=${location.longitude}, speed=$speed, bearing=$bearing")
            }
        }
    }

    /**
     * 위치 업데이트 중지
     */
    fun stopLocationUpdates() {
        locationService.stopLocationUpdates()
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            locationRepository.clearLocation(uid)
        }
    }

    /**
     * 연결 해제 핸들러 설정
     */
    private fun setupDisconnectHandler() {
        val uid = auth.currentUser?.uid ?: return
        locationRepository.setupDisconnectHandler(uid)
    }

    /**
     * 권한 허용됨
     */
    fun onPermissionGranted() {
        _locationPermissionGranted.value = true
        startLocationUpdates()
    }

    /**
     * 권한 거부됨
     */
    fun onPermissionDenied() {
        _locationPermissionGranted.value = false
        _errorMessage.value = "위치 권한이 거부되었습니다. 설정에서 권한을 허용해주세요."
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Bearing 스무딩 (떨림 방지)
     */
    private fun smoothBearing(newBearing: Float): Float {
        val prev = previousBearing ?: return newBearing.also { previousBearing = it }

        // 각도 차이 계산 (0-360도 순환 고려)
        var delta = newBearing - prev
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360

        // 가중 평균 (70% 새값, 30% 이전값)
        val smoothed = (prev + delta * 0.7f + 360) % 360
        previousBearing = smoothed
        return smoothed
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
