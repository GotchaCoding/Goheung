package com.example.goheung.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.goheung.data.model.BusStop

/**
 * Room Entity for bus stops
 */
@Entity(tableName = "bus_stops")
data class BusStopEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val order: Int,
    val isActive: Boolean,
    // 자동 감지 관련 필드
    val isAutoDetected: Boolean = false,
    val boardingCount: Int = 0,
    val sourceClusterId: String? = null,
    val createdAt: Long = 0L
) {
    fun toBusStop(): BusStop = BusStop(
        id = id,
        name = name,
        lat = lat,
        lng = lng,
        order = order,
        isActive = isActive,
        isAutoDetected = isAutoDetected,
        boardingCount = boardingCount,
        sourceClusterId = sourceClusterId,
        createdAt = createdAt
    )

    companion object {
        fun fromBusStop(busStop: BusStop): BusStopEntity = BusStopEntity(
            id = busStop.id,
            name = busStop.name,
            lat = busStop.lat,
            lng = busStop.lng,
            order = busStop.order,
            isActive = busStop.isActive,
            isAutoDetected = busStop.isAutoDetected,
            boardingCount = busStop.boardingCount,
            sourceClusterId = busStop.sourceClusterId,
            createdAt = busStop.createdAt
        )
    }
}
