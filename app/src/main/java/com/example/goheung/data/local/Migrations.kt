package com.example.goheung.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migrations
 */
object Migrations {

    /**
     * Migration from version 1 to version 2
     * - Add auto-detection fields to bus_stops table
     * - Create boarding_events table
     * - Create boarding_clusters table
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. bus_stops 테이블에 자동 감지 필드 추가
            db.execSQL("ALTER TABLE bus_stops ADD COLUMN isAutoDetected INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE bus_stops ADD COLUMN boardingCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE bus_stops ADD COLUMN sourceClusterId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE bus_stops ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")

            // 2. boarding_events 테이블 생성
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS boarding_events (
                    id TEXT NOT NULL PRIMARY KEY,
                    passengerUid TEXT NOT NULL,
                    driverUid TEXT NOT NULL,
                    lat REAL NOT NULL,
                    lng REAL NOT NULL,
                    timestamp INTEGER NOT NULL,
                    clusterId TEXT
                )
            """.trimIndent())

            // 3. boarding_clusters 테이블 생성
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS boarding_clusters (
                    id TEXT NOT NULL PRIMARY KEY,
                    centroidLat REAL NOT NULL,
                    centroidLng REAL NOT NULL,
                    boardingCount INTEGER NOT NULL,
                    isPromotedToBusStop INTEGER NOT NULL,
                    lastUpdated INTEGER NOT NULL
                )
            """.trimIndent())

            // 4. 인덱스 생성 (성능 최적화)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_boarding_events_clusterId ON boarding_events(clusterId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_boarding_events_timestamp ON boarding_events(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_boarding_clusters_isPromotedToBusStop ON boarding_clusters(isPromotedToBusStop)")
        }
    }
}
