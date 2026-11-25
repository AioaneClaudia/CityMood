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
    version = 5,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite stochează valorile Boolean nullable ca INTEGER (0/1).
                db.execSQL(
                    "ALTER TABLE travel_journal ADD COLUMN containsFace INTEGER"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Metadate de context pentru fiecare stare înregistrată.
                // Toate coloanele sunt nullable, deci intrările vechi rămân valide.
                val newColumns = listOf(
                    "latitude REAL",
                    "longitude REAL",
                    "temperature REAL",
                    "apparentTemperature REAL",
                    "humidity INTEGER",
                    "precipitation REAL",
                    "cloudCover INTEGER",
                    "windSpeed REAL",
                    "weatherCode INTEGER",
                    "weatherDescription TEXT",
                    "isDay INTEGER",
                    "hourOfDay INTEGER",
                    "timeOfDay TEXT",
                    "dayOfWeek INTEGER",
                    "isWeekend INTEGER",
                    "season TEXT",
                    "socialContext TEXT"
                )
                newColumns.forEach { column ->
                    db.execSQL("ALTER TABLE mood_entries ADD COLUMN $column")
                }
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
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}