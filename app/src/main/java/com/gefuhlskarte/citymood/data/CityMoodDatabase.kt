package com.gefuhlskarte.citymood.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MoodEntry::class,
        TravelJournalEntry::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CityMoodDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao

    abstract fun travelJournalDao(): TravelJournalDao

    companion object {

        @Volatile
        private var INSTANCE: CityMoodDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS travel_journal (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        placeName TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        description TEXT NOT NULL,
                        photoUri TEXT,
                        facialEmotion TEXT,
                        textEmotion TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE travel_journal
                    ADD COLUMN pageColor TEXT NOT NULL
                    DEFAULT '#FFFFF8E7'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE travel_journal
                    ADD COLUMN textBackgroundColor TEXT NOT NULL
                    DEFAULT '#FFFFE8C2'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE travel_journal
                    ADD COLUMN fontStyle TEXT NOT NULL
                    DEFAULT 'SANS_SERIF'
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): CityMoodDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CityMoodDatabase::class.java,
                    "citymood_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}