package com.example.goheung.data.model

enum class UserRole(val displayName: String) {
    DRIVER("운전기사"),
    PASSENGER("승객"),
    NONE("미설정");

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value) {
                "DRIVER" -> DRIVER
                "PASSENGER" -> PASSENGER
                else -> NONE
            }
        }
    }
}
