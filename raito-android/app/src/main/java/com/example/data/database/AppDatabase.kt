package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    ChapterEntity::class,
    TaskEntity::class,
    UserStatsEntity::class,
    ActivityDayEntity::class,
    CustomAvatarEntity::class
  ],
  version = 10,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun chapterDao(): ChapterDao
  abstract fun taskDao(): TaskDao
  abstract fun userStatsDao(): UserStatsDao
  abstract fun activityDayDao(): ActivityDayDao
  abstract fun customAvatarDao(): CustomAvatarDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "raito_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
