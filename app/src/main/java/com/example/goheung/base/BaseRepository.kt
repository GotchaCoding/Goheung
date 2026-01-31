package com.example.goheung.base

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

open class BaseRepository {

    fun <Response : BaseResponse> callApi(
        responseFunction: suspend () -> Response
    ): Flow<Resource<Response>> {
        return flow {
            emit(Resource.Loading())
            emit(safeResult(responseFunction))
        }
    }

    fun <T> callFirebase(
        firebaseFunction: suspend () -> T
    ): Flow<Resource<T>> {
        return flow {
            emit(Resource.Loading())
            try {
                val result = firebaseFunction.invoke()
                emit(Resource.Success(result))
            } catch (e: com.google.firebase.auth.FirebaseAuthException) {
                Log.e("GoheungRepo", "FirebaseAuthException: ${e.message}", e)
                emit(Resource.Fail(GoheungException.AuthException))
            } catch (e: com.google.firebase.FirebaseException) {
                Log.e("GoheungRepo", "FirebaseException: ${e.message}", e)
                emit(Resource.Fail(GoheungException.FirebaseException(e.message)))
            } catch (e: Exception) {
                Log.e("GoheungRepo", "Exception: ${e.message}", e)
                emit(Resource.Fail(GoheungException.UnknownException))
            }
        }
    }

    private suspend fun <Response : BaseResponse> safeResult(
        responseFunction: suspend () -> Response,
    ): Resource<Response> {
        return try {
            Resource.Success(responseFunction.invoke())
        } catch (e: HttpException) {
            Log.e("GoheungRepo", "HttpException")
            Resource.Fail(GoheungException.HttpException(code = e.code()))
        } catch (e: IOException) {
            Log.e("GoheungRepo", "IOException")
            Resource.Fail(GoheungException.NetworkException)
        } catch (e: Exception) {
            Log.e("GoheungRepo", "Exception: ${e.message}", e)
            Resource.Fail(GoheungException.UnknownException)
        }
    }
}
