package com.example.goheung.data.firebase

import com.example.goheung.base.Resource
import kotlinx.coroutines.flow.Flow

interface FcmRepository {
    fun saveToken(userId: String, token: String): Flow<Resource<Unit>>
    fun getToken(): Flow<Resource<String>>
}
