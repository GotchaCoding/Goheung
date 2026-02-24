package com.example.goheung.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.goheung.data.model.BoardingEvent

/**
 * Room Entity for boarding events
 */
@Entity(
    tableName = "boarding_events",
    indices = [
        Index(value = ["clusterId"]),
        Index(value = ["timestamp"])
    ]
)
data class BoardingEventEntity(
    @PrimaryKey
    val id: String,
    val passengerUid: String,
    val driverUid: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
    val clusterId: String?
) {
    fun toBoardingEvent(): BoardingEvent = BoardingEvent(
        id = id,
        passengerUid = passengerUid,
        driverUid = driverUid,
        lat = lat,
        lng = lng,
        timestamp = timestamp,
        clusterId = clusterId
    )

    companion object {
        fun fromBoardingEvent(event: BoardingEvent): BoardingEventEntity = BoardingEventEntity(
            id = event.id,
            passengerUid = event.passengerUid,
            driverUid = event.driverUid,
            lat = event.lat,
            lng = event.lng,
            timestamp = event.timestamp,
            clusterId = event.clusterId
        )
    }
}
