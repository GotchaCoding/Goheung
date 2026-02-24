package com.example.goheung.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.goheung.data.model.BoardingCluster

/**
 * Room Entity for boarding clusters
 */
@Entity(
    tableName = "boarding_clusters",
    indices = [
        Index(value = ["isPromotedToBusStop"])
    ]
)
data class BoardingClusterEntity(
    @PrimaryKey
    val id: String,
    val centroidLat: Double,
    val centroidLng: Double,
    val boardingCount: Int,
    val isPromotedToBusStop: Boolean,
    val lastUpdated: Long
) {
    fun toBoardingCluster(): BoardingCluster = BoardingCluster(
        id = id,
        centroidLat = centroidLat,
        centroidLng = centroidLng,
        boardingCount = boardingCount,
        isPromotedToBusStop = isPromotedToBusStop,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromBoardingCluster(cluster: BoardingCluster): BoardingClusterEntity = BoardingClusterEntity(
            id = cluster.id,
            centroidLat = cluster.centroidLat,
            centroidLng = cluster.centroidLng,
            boardingCount = cluster.boardingCount,
            isPromotedToBusStop = cluster.isPromotedToBusStop,
            lastUpdated = cluster.lastUpdated
        )
    }
}
