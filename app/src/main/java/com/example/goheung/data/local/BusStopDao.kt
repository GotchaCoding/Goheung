package com.example.goheung.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for bus stops
 */
@Dao
interface BusStopDao {

    @Query("SELECT * FROM bus_stops WHERE isActive = 1 ORDER BY `order` ASC")
    fun getAllActiveBusStops(): Flow<List<BusStopEntity>>

    @Query("SELECT * FROM bus_stops ORDER BY `order` ASC")
    fun getAllBusStops(): Flow<List<BusStopEntity>>

    @Query("SELECT * FROM bus_stops WHERE id = :id")
    suspend fun getBusStopById(id: String): BusStopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(busStops: List<BusStopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(busStop: BusStopEntity)

    @Query("DELETE FROM bus_stops")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bus_stops")
    suspend fun getCount(): Int

    // 자동 감지 정류장 관련 쿼리

    @Query("SELECT * FROM bus_stops WHERE isAutoDetected = 1 ORDER BY createdAt DESC")
    fun getAutoDetectedBusStops(): Flow<List<BusStopEntity>>

    @Query("SELECT COUNT(*) FROM bus_stops WHERE isAutoDetected = 1")
    suspend fun getAutoDetectedCount(): Int

    @Query("SELECT * FROM bus_stops WHERE sourceClusterId = :clusterId")
    suspend fun getBusStopByClusterId(clusterId: String): BusStopEntity?

    @Query("UPDATE bus_stops SET boardingCount = :count WHERE id = :id")
    suspend fun updateBoardingCount(id: String, count: Int)

    @Query("DELETE FROM bus_stops WHERE isAutoDetected = 1")
    suspend fun deleteAllAutoDetected()
}
