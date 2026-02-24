package com.example.goheung.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for boarding events
 */
@Dao
interface BoardingEventDao {

    @Query("SELECT * FROM boarding_events ORDER BY timestamp DESC")
    fun getAllBoardingEvents(): Flow<List<BoardingEventEntity>>

    @Query("SELECT * FROM boarding_events WHERE clusterId = :clusterId ORDER BY timestamp DESC")
    fun getBoardingEventsByCluster(clusterId: String): Flow<List<BoardingEventEntity>>

    @Query("SELECT * FROM boarding_events WHERE clusterId IS NULL ORDER BY timestamp DESC")
    fun getUnclusteredEvents(): Flow<List<BoardingEventEntity>>

    @Query("SELECT * FROM boarding_events WHERE clusterId IS NULL")
    suspend fun getUnclusteredEventsList(): List<BoardingEventEntity>

    @Query("SELECT * FROM boarding_events WHERE id = :id")
    suspend fun getBoardingEventById(id: String): BoardingEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BoardingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<BoardingEventEntity>)

    @Update
    suspend fun update(event: BoardingEventEntity)

    @Query("UPDATE boarding_events SET clusterId = :clusterId WHERE id = :eventId")
    suspend fun updateClusterId(eventId: String, clusterId: String)

    @Query("DELETE FROM boarding_events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM boarding_events")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM boarding_events")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM boarding_events WHERE clusterId = :clusterId")
    suspend fun getCountByCluster(clusterId: String): Int
}
