package com.goheung.app.util

/**
 * 일회성 이벤트를 위한 wrapper 클래스.
 * LiveData의 observer가 재등록될 때 이전 값이 다시 전달되는 것을 방지.
 */
open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    /**
     * 이벤트가 이미 처리되었으면 null 반환.
     * 처음 호출 시에만 content 반환.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}
