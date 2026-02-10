package com.example.goheung.data.model

/**
 * 사용자 프로필 (User + Presence + Attendance 결합)
 * View Layer에서만 사용, DB에는 저장하지 않음
 */
data class UserProfile(
    val user: User,
    val presence: Presence? = null,
    val attendance: Attendance? = null
) {
    /**
     * Presence 상태를 사용자에게 보여줄 텍스트로 변환
     */
    fun getPresenceText(): String {
        return when {
            presence?.online == true && presence.inChat -> "대화 중"
            presence?.online == true -> "활성"
            presence?.lastActive != null && presence.lastActive > 0 -> {
                val diffMinutes = (System.currentTimeMillis() - presence.lastActive) / 60000
                when {
                    diffMinutes < 1 -> "방금 전"
                    diffMinutes < 60 -> "${diffMinutes}분 전"
                    diffMinutes < 1440 -> "${diffMinutes / 60}시간 전"
                    else -> "${diffMinutes / 1440}일 전"
                }
            }
            else -> "오프라인"
        }
    }

    /**
     * 온라인 상태 여부
     */
    fun isOnline(): Boolean = presence?.online == true
}
