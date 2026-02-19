package com.example.goheung.util

import kotlin.math.*

object LocationUtils {

    /**
     * Haversine 공식을 사용하여 두 좌표 간 거리 계산
     * @return 거리 (미터)
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0  // 지구 반경 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * 도착 시간 계산 및 포맷팅
     * @param distanceInMeters 거리 (미터)
     * @param speedKmH 평균 속도 (km/h), 기본값 40
     * @return 포맷팅된 도착 시간 문자열
     */
    fun formatArrivalTime(distanceInMeters: Double, speedKmH: Double = 40.0): String {
        val speedMps = speedKmH * 1000 / 3600  // km/h -> m/s
        val timeInSeconds = distanceInMeters / speedMps

        return when {
            timeInSeconds < 30 -> "곧 도착"
            timeInSeconds < 90 -> "약 1분 후 도착"
            else -> {
                val timeInMinutes = (timeInSeconds / 60).roundToInt()
                "약 ${timeInMinutes}분 후 도착"
            }
        }
    }

    /**
     * 거리 포맷팅
     * @param distanceInMeters 거리 (미터)
     * @return 포맷팅된 거리 문자열
     */
    fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()}m"
            else -> String.format("%.1fkm", distanceInMeters / 1000)
        }
    }
}
