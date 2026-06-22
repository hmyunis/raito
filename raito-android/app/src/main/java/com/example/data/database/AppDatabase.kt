package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    ChapterEntity::class,
    TaskEntity::class,
    UserStatsEntity::class,
    ActivityDayEntity::class,
    CustomAvatarEntity::class,
    AppliedTelegramOperationEntity::class
  ],
  version = 12,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun chapterDao(): ChapterDao
  abstract fun taskDao(): TaskDao
  abstract fun userStatsDao(): UserStatsDao
  abstract fun activityDayDao(): ActivityDayDao
  abstract fun customAvatarDao(): CustomAvatarDao
  abstract fun appliedTelegramOperationDao(): AppliedTelegramOperationDao

  companion object {
    private val MIGRATION_10_11 = object : Migration(10, 11) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          ALTER TABLE chapters
          ADD COLUMN telegramSyncEnabled INTEGER NOT NULL DEFAULT 0
          """.trimIndent()
        )
      }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS applied_telegram_operations (
            operationId TEXT NOT NULL PRIMARY KEY,
            appliedAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "raito_database"
        )
          .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
