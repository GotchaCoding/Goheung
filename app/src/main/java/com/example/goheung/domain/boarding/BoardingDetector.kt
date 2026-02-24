package com.example.goheung.domain.boarding

import android.util.Log
import com.example.goheung.data.model.BoardingEvent
import com.example.goheung.data.model.UserLocation
import com.example.goheung.data.model.UserRole
import com.example.goheung.util.LocationUtils
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 탑승 감지 상태 머신
 *
 * 상태 전이:
 * IDLE → NEAR_BUS (버스 50m 이내 & 정지)
 * NEAR_BUS → BOARDING (속도: 0→이동)
 * BOARDING → BOARDED (3초 지속 이동)
 * NEAR_BUS/BOARDING → IDLE (버스 이탈)
 */
@Singleton
class BoardingDetector @Inject constructor() {

    companion object {
        private const val TAG = "BoardingDetector"

        // 임계값 상수
        const val PROXIMITY_THRESHOLD = 50.0     // 버스 근접 판단 (미터)
        const val STATIONARY_SPEED = 0.5f        // 정지 상태 (m/s, <2km/h)
        const val MOVING_SPEED = 2.0f            // 이동 상태 (m/s, >7km/h)
        const val BOARDING_CONFIRM_TIME = 3000L  // 탑승 확정 시간 (ms)
    }

    // 현재 상태
    private var _currentState: BoardingState = BoardingState.IDLE
    val currentState: BoardingState get() = _currentState

    // 탑승 시작 시간 (BOARDING 상태 진입 시각)
    private var boardingStartTime: Long = 0L

    // 감지 중인 버스 (driver) 정보
    private var detectedDriver: UserLocation? = null

    // 마지막 위치
    private var lastLocation: UserLocation? = null

    // 상태 변경 콜백
    var onStateChanged: ((BoardingState) -> Unit)? = null

    /**
     * 위치 업데이트 처리
     *
     * @param myLocation 승객의 현재 위치
     * @param allLocations 모든 사용자 위치 (버스 포함)
     * @return 탑승 완료 시 BoardingEvent, 그렇지 않으면 null
     */
    fun processLocationUpdate(
        myLocation: UserLocation?,
        allLocations: List<UserLocation>
    ): BoardingEvent? {
        if (myLocation == null) return null

        // 승객만 탑승 감지
        if (UserRole.fromString(myLocation.role) != UserRole.PASSENGER) {
            reset()
            return null
        }

        lastLocation = myLocation

        // 가장 가까운 버스 찾기
        val nearestDriver = findNearestDriver(myLocation, allLocations)
        val distanceToNearestBus = nearestDriver?.let {
            LocationUtils.calculateDistance(
                myLocation.lat, myLocation.lng,
                it.lat, it.lng
            )
        } ?: Double.MAX_VALUE

        val speed = myLocation.speed
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "State: $_currentState, Distance: ${distanceToNearestBus.toInt()}m, Speed: $speed m/s")

        return when (_currentState) {
            BoardingState.IDLE -> {
                handleIdleState(distanceToNearestBus, speed, nearestDriver)
            }
            BoardingState.NEAR_BUS -> {
                handleNearBusState(distanceToNearestBus, speed, currentTime)
            }
            BoardingState.BOARDING -> {
                handleBoardingState(myLocation, distanceToNearestBus, speed, currentTime)
            }
            BoardingState.BOARDED -> {
                // 이미 탑승 완료 상태 - 다음 세션까지 유지
                null
            }
        }
    }

    /**
     * IDLE 상태 처리
     * - 버스 50m 이내 + 정지 → NEAR_BUS 전이
     */
    private fun handleIdleState(
        distanceToBus: Double,
        speed: Float,
        driver: UserLocation?
    ): BoardingEvent? {
        if (distanceToBus <= PROXIMITY_THRESHOLD && speed <= STATIONARY_SPEED) {
            transitionTo(BoardingState.NEAR_BUS)
            detectedDriver = driver
            Log.d(TAG, "Detected near bus: ${driver?.displayName} (${distanceToBus.toInt()}m)")
        }
        return null
    }

    /**
     * NEAR_BUS 상태 처리
     * - 속도 증가 (정지 → 이동) → BOARDING 전이
     * - 버스 이탈 → IDLE 전이
     */
    private fun handleNearBusState(
        distanceToBus: Double,
        speed: Float,
        currentTime: Long
    ): BoardingEvent? {
        // 버스 이탈 체크
        if (distanceToBus > PROXIMITY_THRESHOLD * 2) {
            Log.d(TAG, "Bus departed - returning to IDLE")
            transitionTo(BoardingState.IDLE)
            detectedDriver = null
            return null
        }

        // 속도 변화 감지 (정지 → 이동)
        if (speed >= MOVING_SPEED) {
            transitionTo(BoardingState.BOARDING)
            boardingStartTime = currentTime
            Log.d(TAG, "Started moving - entering BOARDING state")
        }

        return null
    }

    /**
     * BOARDING 상태 처리
     * - 3초 이상 이동 지속 → BOARDED 전이 및 이벤트 생성
     * - 속도 감소 또는 버스 이탈 → IDLE/NEAR_BUS 전이
     */
    private fun handleBoardingState(
        myLocation: UserLocation,
        distanceToBus: Double,
        speed: Float,
        currentTime: Long
    ): BoardingEvent? {
        // 속도 감소 시 NEAR_BUS로 복귀
        if (speed < STATIONARY_SPEED) {
            Log.d(TAG, "Stopped moving - returning to NEAR_BUS")
            transitionTo(BoardingState.NEAR_BUS)
            boardingStartTime = 0L
            return null
        }

        // 버스 완전 이탈 시 IDLE로 복귀
        if (distanceToBus > PROXIMITY_THRESHOLD * 3) {
            Log.d(TAG, "Bus departed during boarding - returning to IDLE")
            transitionTo(BoardingState.IDLE)
            detectedDriver = null
            boardingStartTime = 0L
            return null
        }

        // 3초 이상 이동 지속 시 탑승 확정
        val elapsedTime = currentTime - boardingStartTime
        if (elapsedTime >= BOARDING_CONFIRM_TIME) {
            transitionTo(BoardingState.BOARDED)
            Log.d(TAG, "Boarding confirmed! Creating BoardingEvent")

            val driver = detectedDriver
            return BoardingEvent(
                id = UUID.randomUUID().toString(),
                passengerUid = myLocation.uid,
                driverUid = driver?.uid ?: "",
                lat = myLocation.lat,
                lng = myLocation.lng,
                timestamp = currentTime,
                clusterId = null
            )
        }

        return null
    }

    /**
     * 상태 전이
     */
    private fun transitionTo(newState: BoardingState) {
        val oldState = _currentState
        _currentState = newState
        Log.d(TAG, "State transition: $oldState → $newState")
        onStateChanged?.invoke(newState)
    }

    /**
     * 가장 가까운 운전자(버스) 찾기
     */
    private fun findNearestDriver(
        myLocation: UserLocation,
        allLocations: List<UserLocation>
    ): UserLocation? {
        return allLocations
            .filter {
                UserRole.fromString(it.role) == UserRole.DRIVER &&
                it.uid != myLocation.uid
            }
            .minByOrNull {
                LocationUtils.calculateDistance(
                    myLocation.lat, myLocation.lng,
                    it.lat, it.lng
                )
            }
    }

    /**
     * 상태 초기화
     */
    fun reset() {
        _currentState = BoardingState.IDLE
        boardingStartTime = 0L
        detectedDriver = null
        lastLocation = null
        Log.d(TAG, "State reset to IDLE")
    }

    /**
     * 탑승 완료 후 새 세션 시작
     */
    fun startNewSession() {
        reset()
        Log.d(TAG, "New boarding session started")
    }
}
