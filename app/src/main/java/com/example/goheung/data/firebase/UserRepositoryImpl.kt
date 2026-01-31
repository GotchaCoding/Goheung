package com.example.goheung.data.firebase

import com.example.goheung.base.BaseRepository
import com.example.goheung.base.Resource
import com.example.goheung.constants.Constants
import com.example.goheung.model.UserModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BaseRepository(), UserRepository {

    override fun createOrUpdateUser(user: UserModel): Flow<Resource<Unit>> {
        return callFirebase {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .set(user)
                .await()
        }
    }

    override fun getUser(uid: String): Flow<Resource<UserModel>> {
        return callFirebase {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .await()
            snapshot.toObject(UserModel::class.java)
                ?: throw Exception("User not found")
        }
    }

    override fun getAllUsers(): Flow<Resource<List<UserModel>>> {
        return callFirebase {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .get()
                .await()
            snapshot.toObjects(UserModel::class.java)
        }
    }
}
