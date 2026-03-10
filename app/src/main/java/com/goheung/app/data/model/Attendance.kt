package com.goheung.app.data.model

/**
 * 근무 상태 enum
 */
enum class AttendanceStatus(val displayName: String) {
    WORKING("근무중"),
    FIELD_WORK("외근중"),
    ANNUAL_LEAVE("연차"),
    HALF_DAY_LEAVE("반차")
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
