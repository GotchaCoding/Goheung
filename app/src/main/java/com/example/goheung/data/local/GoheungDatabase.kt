package com.example.goheung.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for Goheung app
 */
@Database(
    entities = [
        BusStopEntity::class,
        BoardingEventEntity::class,
        BoardingClusterEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GoheungDatabase : RoomDatabase() {
    abstract fun busStopDao(): BusStopDao
    abstract fun boardingEventDao(): BoardingEventDao
    abstract fun boardingClusterDao(): BoardingClusterDao
}
