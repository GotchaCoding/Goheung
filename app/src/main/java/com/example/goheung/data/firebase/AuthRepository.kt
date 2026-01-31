package com.example.goheung.data.firebase

import com.example.goheung.base.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): FirebaseUser?
    fun login(email: String, password: String): Flow<Resource<FirebaseUser>>
    fun signUp(email: String, password: String): Flow<Resource<FirebaseUser>>
    fun logout()
}
