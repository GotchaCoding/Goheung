package com.example.goheung.base

sealed class Resource<out T> {
    class Success<T>(val model: T) : Resource<T>()
    class Fail<T>(val exception: GoheungException) : Resource<T>()
    class Loading<T> : Resource<T>()
}
