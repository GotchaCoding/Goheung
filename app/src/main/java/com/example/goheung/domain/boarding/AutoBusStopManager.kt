package com.example.goheung.domain.boarding

import android.util.Log
import com.example.goheung.data.local.BoardingClusterDao
import com.example.goheung.data.local.BoardingClusterEntity
import com.example.goheung.data.local.BoardingEventDao
import com.example.goheung.data.local.BusStopDao
import com.example.goheung.data.local.BusStopEntity
import com.example.goheung.data.model.BoardingCluster
import com.example.goheung.data.model.BoardingEvent
import com.example.goheung.data.model.BusStop
import com.example.goheung.util.LocationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자동 버스 정류장 관리자
 *
 * - 탑승 이벤트를 클러스터링하여 그룹화
 * - 5회 이상 탑승 클러스터를 자동 정류장으로 승격
 */
@Singleton
class AutoBusStopManager @Inject constructor(
    private val boardingEventDao: BoardingEventDao,
    private val boardingClusterDao: BoardingClusterDao,
    private val busStopDao: BusStopDao
) {
    companion object {
        private const val TAG = "AutoBusStopManager"

        const val CLUSTER_RADIUS = 50.0          // 같은 정류장 판단 반경 (미터)
        const val MIN_BOARDING_COUNT = 5         // 정류장 승격 최소 탑승 횟수
    }

    // 새 자동 정류장 생성 콜백
    var onAutoBusStopCreated: ((BusStop) -> Unit)? = null

    /**
     * 탑승 이벤트 처리 및 클러스터링
     *
     * @param event 새 탑승 이벤트
     * @return 생성된 자동 정류장 (있는 경우)
     */
    suspend fun processBoardingEvent(event: BoardingEvent): BusStop? {
        Log.d(TAG, "Processing boarding event: ${event.id} at (${event.lat}, ${event.lng})")

        // 1. 기존 클러스터 중 50m 이내인 것 찾기
        val existingCluster = findNearbyCluster(event.lat, event.lng)

        return if (existingCluster != null) {
            // 기존 클러스터에 추가
            updateExistingCluster(event, existingCluster)
        } else {
            // 새 클러스터 생성
            createNewCluster(event)
        }
    }

    /**
     * 근처 클러스터 찾기 (50m 이내)
     */
    private suspend fun findNearbyCluster(lat: Double, lng: Double): BoardingCluster? {
        val allClusters = boardingClusterDao.getAllClustersList()

        return allClusters
            .map { it.toBoardingCluster() }
            .filter { !it.isPromotedToBusStop }  // 아직 정류장으로 승격되지 않은 것만
            .minByOrNull { cluster ->
                LocationUtils.calculateDistance(lat, lng, cluster.centroidLat, cluster.centroidLng)
            }
            ?.takeIf { cluster ->
                LocationUtils.calculateDistance(lat, lng, cluster.centroidLat, cluster.centroidLng) <= CLUSTER_RADIUS
            }
    }

    /**
     * 기존 클러스터 업데이트
     */
    private suspend fun updateExistingCluster(
        event: BoardingEvent,
        cluster: BoardingCluster
    ): BusStop? {
        Log.d(TAG, "Adding event to existing cluster: ${cluster.id}")

        // 이벤트에 클러스터 ID 할당
        boardingEventDao.updateClusterId(event.id, cluster.id)

        // 새 centroid 계산 (가중 평균)
        val newCount = cluster.boardingCount + 1
        val newLat = (cluster.centroidLat * cluster.boardingCount + event.lat) / newCount
        val newLng = (cluster.centroidLng * cluster.boardingCount + event.lng) / newCount
        val currentTime = System.currentTimeMillis()

        // 클러스터 업데이트
        boardingClusterDao.updateClusterStats(
            clusterId = cluster.id,
            count = newCount,
            lat = newLat,
            lng = newLng,
            timestamp = currentTime
        )

        Log.d(TAG, "Cluster ${cluster.id} updated: count=$newCount, centroid=($newLat, $newLng)")

        // 5회 이상 탑승 시 자동 정류장으로 승격
        if (newCount >= MIN_BOARDING_COUNT && !cluster.isPromotedToBusStop) {
            return promoteClusterToBusStop(
                cluster.copy(
                    boardingCount = newCount,
                    centroidLat = newLat,
                    centroidLng = newLng
                )
            )
        }

        return null
    }

    /**
     * 새 클러스터 생성
     */
    private suspend fun createNewCluster(event: BoardingEvent): BusStop? {
        val clusterId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "Creating new cluster: $clusterId")

        // 새 클러스터 저장
        val newCluster = BoardingClusterEntity(
            id = clusterId,
            centroidLat = event.lat,
            centroidLng = event.lng,
            boardingCount = 1,
            isPromotedToBusStop = false,
            lastUpdated = currentTime
        )
        boardingClusterDao.insert(newCluster)

        // 이벤트에 클러스터 ID 할당
        boardingEventDao.updateClusterId(event.id, clusterId)

        return null  // 첫 번째 이벤트이므로 아직 정류장 승격 안 함
    }

    /**
     * 클러스터를 자동 버스 정류장으로 승격
     */
    private suspend fun promoteClusterToBusStop(cluster: BoardingCluster): BusStop {
        Log.d(TAG, "Promoting cluster ${cluster.id} to bus stop")

        // 클러스터를 승격됨으로 표시
        boardingClusterDao.markAsPromoted(cluster.id)

        // 자동 정류장 이름 생성
        val autoStopCount = busStopDao.getAutoDetectedCount() + 1
        val stopName = "자동정류장 #$autoStopCount"

        // BusStop 생성
        val currentTime = System.currentTimeMillis()
        val busStop = BusStop(
            id = UUID.randomUUID().toString(),
            name = stopName,
            lat = cluster.centroidLat,
            lng = cluster.centroidLng,
            order = Int.MAX_VALUE,  // 노선 순서 가장 마지막
            isActive = true,
            isAutoDetected = true,
            boardingCount = cluster.boardingCount,
            sourceClusterId = cluster.id,
            createdAt = currentTime
        )

        // DB에 저장
        busStopDao.insert(BusStopEntity.fromBusStop(busStop))

        Log.d(TAG, "Auto bus stop created: $stopName at (${cluster.centroidLat}, ${cluster.centroidLng})")

        // 콜백 호출
        onAutoBusStopCreated?.invoke(busStop)

        return busStop
    }

    /**
     * 모든 클러스터 조회 (Flow)
     */
    fun observeAllClusters(): Flow<List<BoardingCluster>> {
        return boardingClusterDao.getAllClusters().map { entities ->
            entities.map { it.toBoardingCluster() }
        }
    }

    /**
     * 승격되지 않은 클러스터 조회 (Flow)
     */
    fun observeUnpromotedClusters(): Flow<List<BoardingCluster>> {
        return boardingClusterDao.getUnpromotedClusters().map { entities ->
            entities.map { it.toBoardingCluster() }
        }
    }

    /**
     * 미처리 이벤트 클러스터링 (앱 시작 시 호출)
     */
    suspend fun processUnclusteredEvents(): List<BusStop> {
        val unclusteredEvents = boardingEventDao.getUnclusteredEventsList()
        val createdBusStops = mutableListOf<BusStop>()

        Log.d(TAG, "Processing ${unclusteredEvents.size} unclustered events")

        for (entity in unclusteredEvents) {
            val event = entity.toBoardingEvent()
            val busStop = processBoardingEvent(event)
            if (busStop != null) {
                createdBusStops.add(busStop)
            }
        }

        return createdBusStops
    }

    /**
     * 특정 클러스터의 탑승 횟수 조회
     */
    suspend fun getClusterBoardingCount(clusterId: String): Int {
        return boardingEventDao.getCountByCluster(clusterId)
    }
}
