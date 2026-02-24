package com.example.goheung.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for boarding clusters
 */
@Dao
interface BoardingClusterDao {

    @Query("SELECT * FROM boarding_clusters ORDER BY boardingCount DESC")
    fun getAllClusters(): Flow<List<BoardingClusterEntity>>

    @Query("SELECT * FROM boarding_clusters WHERE isPromotedToBusStop = 0 ORDER BY boardingCount DESC")
    fun getUnpromotedClusters(): Flow<List<BoardingClusterEntity>>

    @Query("SELECT * FROM boarding_clusters WHERE isPromotedToBusStop = 1 ORDER BY boardingCount DESC")
    fun getPromotedClusters(): Flow<List<BoardingClusterEntity>>

    @Query("SELECT * FROM boarding_clusters")
    suspend fun getAllClustersList(): List<BoardingClusterEntity>

    @Query("SELECT * FROM boarding_clusters WHERE id = :id")
    suspend fun getClusterById(id: String): BoardingClusterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cluster: BoardingClusterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clusters: List<BoardingClusterEntity>)

    @Update
    suspend fun update(cluster: BoardingClusterEntity)

    @Query("UPDATE boarding_clusters SET boardingCount = :count, centroidLat = :lat, centroidLng = :lng, lastUpdated = :timestamp WHERE id = :clusterId")
    suspend fun updateClusterStats(clusterId: String, count: Int, lat: Double, lng: Double, timestamp: Long)

    @Query("UPDATE boarding_clusters SET isPromotedToBusStop = 1 WHERE id = :clusterId")
    suspend fun markAsPromoted(clusterId: String)

    @Query("DELETE FROM boarding_clusters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM boarding_clusters")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM boarding_clusters")
    suspend fun getCount(): Int
}
