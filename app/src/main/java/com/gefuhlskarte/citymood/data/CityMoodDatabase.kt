package com.gefuhlskarte.citymood.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MoodEntry::class],
    version = 1,
    exportSchema = false
)
abstract class CityMoodDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao

    companion object {
        @Volatile
        private var INSTANCE: CityMoodDatabase? = null

        fun getDatabase(context: Context): CityMoodDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CityMoodDatabase::class.java,
                    "citymood_database"
                ).build().also { database ->
                    INSTANCE = database
                }
            }
        }
    }
}
