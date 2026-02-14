package com.example.goheung.data.model

data class UserLocation(
    val uid: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val role: String = "",  // "DRIVER" or "PASSENGER"
    val timestamp: Long = 0L,
    val displayName: String = ""
)
