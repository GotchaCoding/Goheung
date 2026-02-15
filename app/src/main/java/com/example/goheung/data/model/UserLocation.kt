package com.example.goheung.data.model

data class UserLocation(
    val uid: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val role: String = "",  // "DRIVER" or "PASSENGER"
    val timestamp: Long = 0L,
    val displayName: String = "",
    val speed: Float = 0f,          // m/s
    val bearing: Float = 0f,        // 0-360도
    val accuracy: Float = 0f,       // meters
    val hasBearing: Boolean = false // 방향 정보 유효성
)
