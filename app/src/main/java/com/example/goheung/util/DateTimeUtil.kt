package com.example.goheung.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtil {

    private val timeFormat = SimpleDateFormat("a h:mm", Locale.KOREA)
    private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    private val fullFormat = SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.KOREA)

    fun formatTime(millis: Long): String {
        if (millis == 0L) return ""
        return timeFormat.format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        if (millis == 0L) return ""
        return dateFormat.format(Date(millis))
    }

    fun formatFull(millis: Long): String {
        if (millis == 0L) return ""
        return fullFormat.format(Date(millis))
    }

    fun isToday(millis: Long): Boolean {
        if (millis == 0L) return false
        val today = dateFormat.format(Date())
        val target = dateFormat.format(Date(millis))
        return today == target
    }

    fun formatChatTime(millis: Long): String {
        if (millis == 0L) return ""
        return if (isToday(millis)) {
            formatTime(millis)
        } else {
            formatDate(millis)
        }
    }
}
