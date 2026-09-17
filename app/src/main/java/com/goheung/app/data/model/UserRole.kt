package com.goheung.app.data.model

/**
 * FTA 원산지확인서 업무상의 역할.
 *
 * v1.7까지는 셔틀버스 역할(DRIVER/PASSENGER)이었다. 기존 users 문서에 남은
 * 그 값들은 fromString의 else 분기로 NONE에 떨어지며, 이는 "다시 선택하세요"에
 * 해당하는 의도된 동작이다. 별도 마이그레이션은 필요 없다.
 */
enum class UserRole(val displayName: String) {
    SUPPLIER("공급자"),
    BUYER("공급받는자"),
    ADMIN("관리자"),
    NONE("미설정");

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.name == value } ?: NONE
        }
    }
}
