package com.example.goheung.domain.boarding

import android.util.Log
import com.example.goheung.data.model.BoardingEvent
import com.example.goheung.data.model.BusStop
import com.example.goheung.data.model.UserLocation
import com.example.goheung.data.model.UserRole
import com.example.goheung.util.LocationUtils
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 셔틀버스 탑승 감지 상태 머신
 *
 * 승차 점수 기반 판정:
 * - 거리 점수 (0.0 ~ 0.3)
 * - 속도 동기화 점수 (0.0 ~ 0.3)
 * - 방향 유사도 점수 (0.0 ~ 0.2)
 * - 정류장 가중치 (0.0 ~ 0.2)
 *
 * 승차 확정 조건: 점수 >= 0.7 && 5초 지속
 */
@Singleton
class BoardingDetector @Inject constructor() {

    companion object {
        private const val TAG = "BoardingDetector"

        // 거리 임계값
        const val PROXIMITY_THRESHOLD = 50.0          // 버스 근접 판단 (미터)
        const val EXTENDED_PROXIMITY_THRESHOLD = 70.0 // 정류장 근처 확장 거리

        // 속도 임계값
        const val STATIONARY_SPEED = 0.5f             // 정지 상태 (m/s, <2km/h)
        const val MOVING_SPEED = 2.0f                 // 이동 상태 (m/s, >7km/h)

        // 승차 확정 시간
        const val BOARDING_CONFIRM_TIME = 5000L       // 5초

        // 승차 점수 관련
        const val MIN_BOARDING_SCORE = 0.7f           // 최소 승차 점수
        const val BUS_STOP_PROXIMITY = 50.0           // 정류장 근접 거리 (미터)
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

    // 버스 정류장 목록
    private var busStops: List<BusStop> = emptyList()

    // 상태 변경 콜백
    var onStateChanged: ((BoardingState) -> Unit)? = null

    /**
     * 버스 정류장 목록 업데이트
     */
    fun updateBusStops(stops: List<BusStop>) {
        busStops = stops
        Log.d(TAG, "Bus stops updated: ${stops.size} stops")
    }

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

        // 정류장 근처 여부 확인
        val isNearBusStop = isNearAnyBusStop(myLocation.lat, myLocation.lng)
        val effectiveProximity = if (isNearBusStop) EXTENDED_PROXIMITY_THRESHOLD else PROXIMITY_THRESHOLD

        Log.d(TAG, "State: $_currentState, Distance: ${distanceToNearestBus.toInt()}m, Speed: $speed m/s, NearStop: $isNearBusStop")

        return when (_currentState) {
            BoardingState.IDLE -> {
                handleIdleState(distanceToNearestBus, speed, nearestDriver, effectiveProximity)
            }
            BoardingState.NEAR_BUS -> {
                handleNearBusState(myLocation, nearestDriver, distanceToNearestBus, speed, currentTime)
            }
            BoardingState.BOARDING -> {
                handleBoardingState(myLocation, nearestDriver, distanceToNearestBus, speed, currentTime)
            }
            BoardingState.BOARDED -> {
                // 이미 탑승 완료 상태 - 다음 세션까지 유지
                null
            }
        }
    }

    /**
     * IDLE 상태 처리
     * - 버스 근접 + 정지 → NEAR_BUS 전이
     */
    private fun handleIdleState(
        distanceToBus: Double,
        speed: Float,
        driver: UserLocation?,
        effectiveProximity: Double
    ): BoardingEvent? {
        if (distanceToBus <= effectiveProximity && speed <= STATIONARY_SPEED) {
            transitionTo(BoardingState.NEAR_BUS)
            detectedDriver = driver
            Log.d(TAG, "Detected near bus: ${driver?.displayName} (${distanceToBus.toInt()}m)")
        }
        return null
    }

    /**
     * NEAR_BUS 상태 처리
     * - 속도 증가 (정지 → 이동) + 점수 충족 → BOARDING 전이
     * - 버스 이탈 → IDLE 전이
     */
    private fun handleNearBusState(
        myLocation: UserLocation,
        driver: UserLocation?,
        distanceToBus: Double,
        speed: Float,
        currentTime: Long
    ): BoardingEvent? {
        // 버스 이탈 체크
        if (distanceToBus > EXTENDED_PROXIMITY_THRESHOLD * 2) {
            Log.d(TAG, "Bus departed - returning to IDLE")
            transitionTo(BoardingState.IDLE)
            detectedDriver = null
            return null
        }

        // 속도 변화 감지 (정지 → 이동)
        if (speed >= MOVING_SPEED && driver != null) {
            // 초기 점수 체크
            val initialScore = calculateBoardingScore(myLocation, driver, distanceToBus)
            if (initialScore >= MIN_BOARDING_SCORE * 0.8f) { // 진입 조건은 약간 완화
                transitionTo(BoardingState.BOARDING)
                boardingStartTime = currentTime
                detectedDriver = driver
                Log.d(TAG, "Started moving - entering BOARDING state (score: ${"%.2f".format(initialScore)})")
            }
        }

        return null
    }

    /**
     * BOARDING 상태 처리
     * - 5초 이상 점수 유지 → BOARDED 전이 및 이벤트 생성
     * - 점수 미달 또는 버스 이탈 → IDLE/NEAR_BUS 전이
     */
    private fun handleBoardingState(
        myLocation: UserLocation,
        driver: UserLocation?,
        distanceToBus: Double,
        speed: Float,
        currentTime: Long
    ): BoardingEvent? {
        val currentDriver = driver ?: detectedDriver
        if (currentDriver == null) {
            Log.d(TAG, "No driver detected - returning to IDLE")
            transitionTo(BoardingState.IDLE)
            boardingStartTime = 0L
            return null
        }

        // 승차 점수 계산
        val boardingScore = calculateBoardingScore(myLocation, currentDriver, distanceToBus)
        val scoreDetails = getScoreDetails(myLocation, currentDriver, distanceToBus)
        Log.d(TAG, "Boarding score: ${"%.2f".format(boardingScore)} ($scoreDetails)")

        // 속도 감소 시 NEAR_BUS로 복귀
        if (speed < STATIONARY_SPEED) {
            Log.d(TAG, "Stopped moving - returning to NEAR_BUS")
            transitionTo(BoardingState.NEAR_BUS)
            boardingStartTime = 0L
            return null
        }

        // 점수가 임계값 미달 시 NEAR_BUS로 복귀
        if (boardingScore < MIN_BOARDING_SCORE) {
            Log.d(TAG, "Boarding score too low: ${"%.2f".format(boardingScore)} - returning to NEAR_BUS")
            transitionTo(BoardingState.NEAR_BUS)
            boardingStartTime = 0L
            return null
        }

        // 버스 완전 이탈 시 IDLE로 복귀
        if (distanceToBus > EXTENDED_PROXIMITY_THRESHOLD * 3) {
            Log.d(TAG, "Bus departed during boarding - returning to IDLE")
            transitionTo(BoardingState.IDLE)
            detectedDriver = null
            boardingStartTime = 0L
            return null
        }

        // 5초 이상 지속 시 탑승 확정
        val elapsedTime = currentTime - boardingStartTime
        if (elapsedTime >= BOARDING_CONFIRM_TIME) {
            transitionTo(BoardingState.BOARDED)
            Log.d(TAG, "Boarding confirmed! Score: ${"%.2f".format(boardingScore)}")

            val nearBusStop = findNearestBusStop(myLocation.lat, myLocation.lng)
            return BoardingEvent(
                id = UUID.randomUUID().toString(),
                passengerUid = myLocation.uid,
                driverUid = currentDriver.uid,
                lat = myLocation.lat,
                lng = myLocation.lng,
                timestamp = currentTime,
                clusterId = nearBusStop?.sourceClusterId
            )
        }

        Log.d(TAG, "Boarding in progress: ${elapsedTime / 1000}s / ${BOARDING_CONFIRM_TIME / 1000}s")
        return null
    }

    // ========== 승차 점수 계산 ==========

    /**
     * 승차 점수 계산 (0.0 ~ 1.0)
     */
    private fun calculateBoardingScore(
        passenger: UserLocation,
        bus: UserLocation,
        distanceToBus: Double
    ): Float {
        var score = 0f

        // 1. 거리 점수 (0.0 ~ 0.3)
        score += calculateDistanceScore(distanceToBus)

        // 2. 속도 동기화 점수 (0.0 ~ 0.3)
        score += calculateSpeedSyncScore(passenger.speed, bus.speed)

        // 3. 방향 유사도 점수 (0.0 ~ 0.2)
        if (passenger.hasBearing && bus.hasBearing) {
            score += calculateBearingSimilarityScore(passenger.bearing, bus.bearing)
        } else {
            // 방향 정보 없으면 기본 점수 부여
            score += 0.1f
        }

        // 4. 정류장 가중치 (0.0 ~ 0.2)
        score += calculateBusStopBonus(passenger.lat, passenger.lng)

        return score.coerceIn(0f, 1f)
    }

    /**
     * 거리 점수 계산
     */
    private fun calculateDistanceScore(distance: Double): Float {
        return when {
            distance <= 20.0 -> 0.3f
            distance <= 50.0 -> 0.2f
            distance <= 70.0 -> 0.1f
            else -> 0f
        }
    }

    /**
     * 속도 동기화 점수 계산
     * 승객과 버스의 속도 차이가 적을수록 높은 점수
     */
    private fun calculateSpeedSyncScore(passengerSpeed: Float, busSpeed: Float): Float {
        val diff = abs(passengerSpeed - busSpeed)
        return when {
            diff <= 1.0f -> 0.3f
            diff <= 2.0f -> 0.2f
            diff <= 3.0f -> 0.1f
            else -> 0f
        }
    }

    /**
     * 방향 유사도 점수 계산
     * 승객과 버스의 이동 방향이 유사할수록 높은 점수
     */
    private fun calculateBearingSimilarityScore(passengerBearing: Float, busBearing: Float): Float {
        val diff = calculateBearingDifference(passengerBearing, busBearing)
        return when {
            diff <= 30f -> 0.2f
            diff <= 60f -> 0.1f
            else -> 0f
        }
    }

    /**
     * 방향 차이 계산 (0-180도)
     */
    private fun calculateBearingDifference(bearing1: Float, bearing2: Float): Float {
        val diff = abs(bearing1 - bearing2)
        return if (diff > 180f) 360f - diff else diff
    }

    /**
     * 정류장 가중치 계산
     * 정류장 근처에서 승차 시 가중치 부여
     */
    private fun calculateBusStopBonus(lat: Double, lng: Double): Float {
        return if (isNearAnyBusStop(lat, lng)) 0.2f else 0f
    }

    /**
     * 정류장 근처 여부 확인
     */
    private fun isNearAnyBusStop(lat: Double, lng: Double): Boolean {
        return busStops.any { stop ->
            LocationUtils.calculateDistance(lat, lng, stop.lat, stop.lng) <= BUS_STOP_PROXIMITY
        }
    }

    /**
     * 가장 가까운 정류장 찾기
     */
    private fun findNearestBusStop(lat: Double, lng: Double): BusStop? {
        return busStops
            .filter { stop ->
                LocationUtils.calculateDistance(lat, lng, stop.lat, stop.lng) <= BUS_STOP_PROXIMITY
            }
            .minByOrNull { stop ->
                LocationUtils.calculateDistance(lat, lng, stop.lat, stop.lng)
            }
    }

    /**
     * 점수 상세 정보 (디버깅용)
     */
    private fun getScoreDetails(
        passenger: UserLocation,
        bus: UserLocation,
        distanceToBus: Double
    ): String {
        val distScore = calculateDistanceScore(distanceToBus)
        val speedScore = calculateSpeedSyncScore(passenger.speed, bus.speed)
        val bearingScore = if (passenger.hasBearing && bus.hasBearing) {
            calculateBearingSimilarityScore(passenger.bearing, bus.bearing)
        } else 0.1f
        val busStopBonus = calculateBusStopBonus(passenger.lat, passenger.lng)

        return "dist=${"%.1f".format(distScore)}, speed=${"%.1f".format(speedScore)}, " +
               "bearing=${"%.1f".format(bearingScore)}, busStop=${"%.1f".format(busStopBonus)}"
    }

    // ========== 유틸리티 ==========

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
