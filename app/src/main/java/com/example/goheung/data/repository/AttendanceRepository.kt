package com.example.goheung.data.repository

import android.util.Log
import com.example.goheung.data.model.Attendance
import com.example.goheung.data.model.AttendanceStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    companion object {
        private const val TAG = "AttendanceRepository"
        private const val PRESENCE_PATH = "presence"
        private const val ATTENDANCE_PATH = "attendance"
    }

    /**
     * 근무 상태 업데이트
     */
    suspend fun updateAttendance(uid: String, status: AttendanceStatus): Result<Unit> {
        return try {
            val attendanceRef = database.getReference("$PRESENCE_PATH/$uid/$ATTENDANCE_PATH")
            val attendanceData = mapOf(
                "uid" to uid,
                "status" to status.name,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            attendanceRef.setValue(attendanceData).await()
            Log.d(TAG, "Attendance updated for $uid: ${status.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update attendance", e)
            Result.failure(e)
        }
    }

    /**
     * 근무 상태 실시간 감시
     */
    fun observeAttendance(uid: String): Flow<Attendance?> = callbackFlow {
        val attendanceRef = database.getReference("$PRESENCE_PATH/$uid/$ATTENDANCE_PATH")

        val listener = attendanceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val attendance = snapshot.getValue(Attendance::class.java)
                trySend(attendance)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to observe attendance for $uid", error.toException())
                close(error.toException())
            }
        })

        awaitClose {
            attendanceRef.removeEventListener(listener)
        }
    }

    /**
     * 특정 사용자의 근무 상태 가져오기 (일회성)
     */
    suspend fun getAttendance(uid: String): Result<Attendance?> {
        return try {
            val snapshot = database.getReference("$PRESENCE_PATH/$uid/$ATTENDANCE_PATH")
                .get()
                .await()
            val attendance = snapshot.getValue(Attendance::class.java)
            Result.success(attendance)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get attendance", e)
            Result.failure(e)
        }
    }
}
