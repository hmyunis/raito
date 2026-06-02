package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

class RaitoRepository(
  private val chapterDao: ChapterDao,
  private val taskDao: TaskDao,
  private val userStatsDao: UserStatsDao,
  private val activityDayDao: ActivityDayDao,
  private val customAvatarDao: CustomAvatarDao
) {
  val allChapters: Flow<List<ChapterEntity>> = chapterDao.getAllChapters()
  val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
  val userStats: Flow<UserStatsEntity?> = userStatsDao.getUserStatsFlow()
  val allActivityDays: Flow<List<ActivityDayEntity>> = activityDayDao.getAllDays()
  val allCustomAvatars: Flow<List<CustomAvatarEntity>> = customAvatarDao.getAllCustomAvatars()

  suspend fun insertCustomAvatar(avatar: CustomAvatarEntity): Long {
    return customAvatarDao.insertCustomAvatar(avatar)
  }

  suspend fun deleteCustomAvatar(avatar: CustomAvatarEntity) {
    customAvatarDao.deleteCustomAvatar(avatar)
  }

  fun getTasksForChapter(chapterId: Int): Flow<List<TaskEntity>> = taskDao.getTasksForChapter(chapterId)

  suspend fun insertChapter(chapter: ChapterEntity): Long {
    return chapterDao.insertChapter(chapter)
  }

  suspend fun updateChapter(chapter: ChapterEntity) {
    chapterDao.updateChapter(chapter)
  }

  suspend fun deleteChapter(id: Int) {
    chapterDao.deleteChapterById(id)
    taskDao.deleteTasksByChapterId(id)
  }

  suspend fun insertTask(task: TaskEntity): Long {
    return taskDao.insertTask(task)
  }

  suspend fun updateTask(task: TaskEntity) {
    taskDao.updateTask(task)
  }

  suspend fun deleteTask(id: Int) {
    taskDao.deleteTaskById(id)
  }

  suspend fun updateStats(stats: UserStatsEntity) {
    userStatsDao.insertUserStats(stats)
  }

  suspend fun addPoints(amount: Int) {
    val current = userStatsDao.getUserStats() ?: UserStatsEntity()
    userStatsDao.insertUserStats(current.copy(points = current.points + amount))
  }

  suspend fun spendPoints(amount: Int): Boolean {
    val current = userStatsDao.getUserStats() ?: UserStatsEntity()
    if (current.points >= amount) {
      userStatsDao.insertUserStats(current.copy(points = current.points - amount))
      return true
    }
    return false
  }

  suspend fun incrementClearedTasks() {
    val current = userStatsDao.getUserStats() ?: UserStatsEntity()
    userStatsDao.insertUserStats(current.copy(clearedCount = current.clearedCount + 1))
    logActivityToday()
  }

  suspend fun decrementClearedTasks() {
    val current = userStatsDao.getUserStats() ?: UserStatsEntity()
    userStatsDao.insertUserStats(current.copy(clearedCount = (current.clearedCount - 1).coerceAtLeast(0)))
  }

  suspend fun logActivityToday() {
    val todayStr = DateUtils.formatYyyyMMdd()
    val currentDays = allActivityDays.firstOrNull() ?: emptyList()
    val existing = currentDays.find { it.date == todayStr }
    val newIntensity = if (existing != null) (existing.intensity + 1).coerceAtMost(4) else 1
    activityDayDao.insertDay(ActivityDayEntity(todayStr, newIntensity))
  }

  suspend fun resetAllData() {
    chapterDao.clearAll()
    taskDao.clearAll()
    userStatsDao.clearAll()
    activityDayDao.clearAll()
    customAvatarDao.clearAll()
    populateDefaults()
  }

  suspend fun clearAllImportData() {
    chapterDao.clearAll()
    taskDao.clearAll()
    userStatsDao.clearAll()
    activityDayDao.clearAll()
  }

  suspend fun populateDefaults() {
    val existing = userStatsDao.getUserStats()
    if (existing == null) {
      // 1. Initialize stats with pure clean zero-based values
      userStatsDao.insertUserStats(
        UserStatsEntity(
          points = 0,
          dailyStreak = 0,
          clearedCount = 0,
          themeMode = "Light",
          activeCompanionId = "Cyber",
          unlockedCompanions = "Cyber",
          difficulty = "Medium",
          lastStreakClaimedDate = "",
          isWelcomingGiftClaimed = false
        )
      )
    }
  }
}
