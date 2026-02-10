package com.example.goheung.data.model

/**
 * 근무 상태 enum
 */
enum class AttendanceStatus(val displayName: String) {
    WORKING("근무중"),
    REMOTE("재택근무"),
    ON_LEAVE("휴가"),
    HALF_DAY("반차")
}

/**
 * 근무 상태 모델
 * Firebase Realtime Database의 presence/{uid}/attendance 경로에 저장
 */
data class Attendance(
    val uid: String = "",
    val status: String = AttendanceStatus.WORKING.name,
    val updatedAt: Long = System.currentTimeMillis()
)
