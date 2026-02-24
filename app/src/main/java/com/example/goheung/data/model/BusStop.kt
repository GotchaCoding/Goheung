package com.example.goheung.data.model

/**
 * 버스 정류장 데이터 모델
 */
data class BusStop(
    val id: String = "",
    val name: String = "",        // "고흥터미널"
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val order: Int = 0,           // 노선 순서
    val isActive: Boolean = true,
    // 자동 감지 관련 필드
    val isAutoDetected: Boolean = false,  // 자동 감지된 정류장 여부
    val boardingCount: Int = 0,           // 탑승 횟수
    val sourceClusterId: String? = null,  // 원본 클러스터 ID
    val createdAt: Long = 0L              // 생성 시간
)
