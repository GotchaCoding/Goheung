package com.example.goheung.base

sealed class GoheungException {
    object NetworkException : GoheungException()
    class HttpException(val code: Int) : GoheungException()
    object AuthException : GoheungException()
    class FirebaseException(val message: String?) : GoheungException()
    object UnknownException : GoheungException()
}
