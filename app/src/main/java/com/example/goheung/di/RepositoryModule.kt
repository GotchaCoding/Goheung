package com.example.goheung.di

import com.example.goheung.data.firebase.AuthRepository
import com.example.goheung.data.firebase.AuthRepositoryImpl
import com.example.goheung.data.firebase.ChatRepository
import com.example.goheung.data.firebase.ChatRepositoryImpl
import com.example.goheung.data.firebase.FcmRepository
import com.example.goheung.data.firebase.FcmRepositoryImpl
import com.example.goheung.data.firebase.UserRepository
import com.example.goheung.data.firebase.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    abstract fun bindFcmRepository(impl: FcmRepositoryImpl): FcmRepository
}
