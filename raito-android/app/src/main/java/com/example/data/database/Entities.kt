package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class ChapterEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  val discipline: String, // Study, Work, Personal, Fitness, Project, Custom
  val companionId: String, // Knight, Cyber, Scholar
  val auraInk: String, // Red, Teal, Purple, Pink, Black
  val deadline: String? = null,
  val telegramSyncEnabled: Boolean = false,
  val isCompleted: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val chapterId: Int,
  val name: String,
  val timeRemaining: String? = null,
  val isCompleted: Boolean = false,
  val isOverdue: Boolean = false,
  val description: String? = null,
  val dueDatetime: String? = null,
  val createdAt: Long? = System.currentTimeMillis(),
  val isPinned: Boolean = false
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
  @PrimaryKey val id: Int = 1,
  val points: Int = 0,
  val dailyStreak: Int = 0,
  val clearedCount: Int = 0,
  val themeMode: String = "Light", // Light, Dark
  val typographyScale: Float = 1.0f,
  val reducedMotion: Boolean = false,
  val silenceChibiComments: Boolean = false,
  val bucketColoring: Boolean = true,
  val dailyReminders: Boolean = true,
  val activeCompanionId: String = "Cyber", // Cyber, Knight, Artist
  val unlockedCompanions: String = "Cyber", // Comma-separated ids (e.g. "Cyber,Knight")
  val difficulty: String = "Medium", // Easy, Medium, Hard
  val lastStreakClaimedDate: String = "", // YYYY-MM-DD
  val backendBaseUrl: String = "https://raito.hamdi.dev.et",
  val backendDeviceToken: String = "",
  val telegramDeviceName: String = "",
  val autoSyncEnabled: Boolean = false,
  val notificationsMasterEnabled: Boolean = true,
  val notifyOnSync: Boolean = true,
  val notifyOnFocus: Boolean = true,
  val isWelcomingGiftClaimed: Boolean = false
)

@Entity(tableName = "activity_days")
data class ActivityDayEntity(
  @PrimaryKey val date: String, // YYYY-MM-DD
  val intensity: Int // 0 to 4
)

@Entity(tableName = "custom_avatars")
data class CustomAvatarEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  val neutralPath: String? = null,
  val happyPath: String? = null,
  val focusPath: String? = null,
  val sadPath: String? = null,
  val completedPath: String? = null
)
