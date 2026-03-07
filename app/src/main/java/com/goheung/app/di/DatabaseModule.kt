package com.goheung.app.di

import android.content.Context
import androidx.room.Room
import com.goheung.app.data.local.BoardingClusterDao
import com.goheung.app.data.local.BoardingEventDao
import com.goheung.app.data.local.BusStopDao
import com.goheung.app.data.local.GoheungDatabase
import com.goheung.app.data.local.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGoheungDatabase(
        @ApplicationContext context: Context
    ): GoheungDatabase {
        return Room.databaseBuilder(
            context,
            GoheungDatabase::class.java,
            "goheung_database"
        )
            .addMigrations(Migrations.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideBusStopDao(database: GoheungDatabase): BusStopDao {
        return database.busStopDao()
    }

    @Provides
    @Singleton
    fun provideBoardingEventDao(database: GoheungDatabase): BoardingEventDao {
        return database.boardingEventDao()
    }

    @Provides
    @Singleton
    fun provideBoardingClusterDao(database: GoheungDatabase): BoardingClusterDao {
        return database.boardingClusterDao()
    }
}
