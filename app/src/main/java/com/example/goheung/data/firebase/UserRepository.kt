package com.example.goheung.data.firebase

import com.example.goheung.base.Resource
import com.example.goheung.model.UserModel
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun createOrUpdateUser(user: UserModel): Flow<Resource<Unit>>
    fun getUser(uid: String): Flow<Resource<UserModel>>
    fun getAllUsers(): Flow<Resource<List<UserModel>>>
}
