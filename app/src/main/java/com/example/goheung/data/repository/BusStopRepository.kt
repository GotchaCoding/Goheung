package com.example.goheung.data.repository

import android.content.Context
import android.util.Log
import com.example.goheung.data.local.BusStopDao
import com.example.goheung.data.local.BusStopEntity
import com.example.goheung.data.model.BusStop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing bus stop data
 */
@Singleton
class BusStopRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val busStopDao: BusStopDao
) {
    companion object {
        private const val TAG = "BusStopRepository"
        private const val BUS_STOPS_FILE = "bus_stops.json"
    }

    /**
     * 활성화된 버스 정류장 목록을 Flow로 제공
     */
    fun observeActiveBusStops(): Flow<List<BusStop>> {
        return busStopDao.getAllActiveBusStops().map { entities ->
            entities.map { it.toBusStop() }
        }
    }

    /**
     * 모든 버스 정류장 목록을 Flow로 제공
     */
    fun observeAllBusStops(): Flow<List<BusStop>> {
        return busStopDao.getAllBusStops().map { entities ->
            entities.map { it.toBusStop() }
        }
    }

    /**
     * 앱 시작 시 버스 정류장 초기화
     * 기존 하드코딩 데이터 삭제 - 자동 감지 정류장만 사용
     */
    suspend fun initializeBusStops() {
        val deletedCount = busStopDao.deleteAllNonAutoDetected()
        Log.d(TAG, "Deleted $deletedCount hardcoded bus stops. Using auto-detected stops only.")
    }

    /**
     * JSON 파일에서 정류장 데이터를 로드하여 DB에 저장
     */
    private suspend fun loadBusStopsFromJson() {
        try {
            val jsonString = context.assets.open(BUS_STOPS_FILE).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<BusStop>>() {}.type
            val busStops: List<BusStop> = Gson().fromJson(jsonString, type)

            val entities = busStops.map { BusStopEntity.fromBusStop(it) }
            busStopDao.insertAll(entities)
            Log.d(TAG, "Loaded ${busStops.size} bus stops from JSON")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bus stops from JSON", e)
        }
    }

    /**
     * 강제로 JSON에서 데이터 다시 로드 (업데이트 시 사용)
     */
    suspend fun refreshBusStops() {
        busStopDao.deleteAll()
        loadBusStopsFromJson()
    }

    /**
     * 특정 정류장 조회
     */
    suspend fun getBusStopById(id: String): BusStop? {
        return busStopDao.getBusStopById(id)?.toBusStop()
    }

    // ========== 자동 감지 정류장 관련 메서드 ==========

    /**
     * 자동 감지된 버스 정류장 목록을 Flow로 제공
     */
    fun observeAutoDetectedBusStops(): Flow<List<BusStop>> {
        return busStopDao.getAutoDetectedBusStops().map { entities ->
            entities.map { it.toBusStop() }
        }
    }

    /**
     * 자동 감지된 버스 정류장 개수 조회
     */
    suspend fun getAutoDetectedCount(): Int {
        return busStopDao.getAutoDetectedCount()
    }

    /**
     * 클러스터 ID로 버스 정류장 조회
     */
    suspend fun getBusStopByClusterId(clusterId: String): BusStop? {
        return busStopDao.getBusStopByClusterId(clusterId)?.toBusStop()
    }

    /**
     * 버스 정류장 탑승 횟수 업데이트
     */
    suspend fun updateBoardingCount(id: String, count: Int) {
        busStopDao.updateBoardingCount(id, count)
    }

    /**
     * 자동 감지된 버스 정류장 저장
     */
    suspend fun saveAutoBusStop(busStop: BusStop) {
        busStopDao.insert(BusStopEntity.fromBusStop(busStop))
        Log.d(TAG, "Auto bus stop saved: ${busStop.name}")
    }

    /**
     * 모든 자동 감지 정류장 삭제
     */
    suspend fun deleteAllAutoDetected() {
        busStopDao.deleteAllAutoDetected()
        Log.d(TAG, "All auto-detected bus stops deleted")
    }
}
