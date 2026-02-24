package com.example.goheung.domain.boarding

/**
 * 탑승 감지 상태 머신 상태
 *
 * 상태 전이:
 * IDLE → NEAR_BUS (버스 50m 이내 & 정지)
 * NEAR_BUS → BOARDING (속도: 0→이동)
 * BOARDING → BOARDED (3초 지속 이동)
 * NEAR_BUS/BOARDING → IDLE (버스 이탈)
 */
enum class BoardingState {
    /**
     * 대기 상태 - 초기 상태
     */
    IDLE,

    /**
     * 버스 근접 상태 - 버스 50m 이내 & 정지 상태
     */
    NEAR_BUS,

    /**
     * 탑승 중 상태 - 정지 → 이동 속도 변화 감지
     */
    BOARDING,

    /**
     * 탑승 완료 상태 - 3초 이상 이동 지속
     */
    BOARDED
}
