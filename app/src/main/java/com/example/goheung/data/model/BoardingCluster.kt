package com.example.goheung.data.model

/**
 * 탑승 클러스터 데이터 모델
 *
 * 50m 반경 내의 탑승 이벤트들을 그룹화한 클러스터
 * 5회 이상 탑승 시 자동 정류장으로 승격됨
 */
data class BoardingCluster(
    val id: String = "",
    val centroidLat: Double = 0.0,
    val centroidLng: Double = 0.0,
    val boardingCount: Int = 0,
    val isPromotedToBusStop: Boolean = false,
    val lastUpdated: Long = 0L
)
