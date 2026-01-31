package com.example.goheung.data.firebase

import com.example.goheung.base.BaseRepository
import com.example.goheung.base.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : BaseRepository(), AuthRepository {

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override fun login(email: String, password: String): Flow<Resource<FirebaseUser>> {
        return callFirebase {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user!!
        }
    }

    override fun signUp(email: String, password: String): Flow<Resource<FirebaseUser>> {
        return callFirebase {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user!!
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
