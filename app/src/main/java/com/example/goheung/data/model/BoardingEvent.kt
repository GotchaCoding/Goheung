package com.example.goheung.data.model

/**
 * 탑승 이벤트 데이터 모델
 *
 * 승객이 버스에 탑승할 때 발생하는 이벤트를 나타냄
 */
data class BoardingEvent(
    val id: String = "",
    val passengerUid: String = "",
    val driverUid: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Long = 0L,
    val clusterId: String? = null
)
