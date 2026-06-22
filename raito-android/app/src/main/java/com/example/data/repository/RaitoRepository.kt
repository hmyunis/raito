package com.example.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.example.util.DateUtils

class RaitoRepository(
  private val database: RoomDatabase,
  private val chapterDao: ChapterDao,
  private val taskDao: TaskDao,
  private val userStatsDao: UserStatsDao,
  private val activityDayDao: ActivityDayDao,
  private val customAvatarDao: CustomAvatarDao,
  private val appliedTelegramOperationDao: AppliedTelegramOperationDao
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

  suspend fun hasAppliedTelegramOperation(operationId: String): Boolean {
    return appliedTelegramOperationDao.hasOperation(operationId)
  }

  suspend fun markTelegramOperationApplied(operationId: String) {
    appliedTelegramOperationDao.insertOperation(
      AppliedTelegramOperationEntity(operationId = operationId)
    )
  }

  suspend fun getChapterById(id: Int): ChapterEntity? = chapterDao.getChapterById(id)

  suspend fun getTaskById(id: Int): TaskEntity? = taskDao.getTaskById(id)

  suspend fun getTasksForChapterSnapshot(chapterId: Int): List<TaskEntity> = taskDao.getTasksForChapterSnapshot(chapterId)

  suspend fun getAllChaptersSnapshot(): List<ChapterEntity> = chapterDao.getAllChaptersSnapshot()

  suspend fun getTasksForChapterWidget(chapterId: Int): List<TaskEntity> = taskDao.getTasksForChapterWidget(chapterId)

  suspend fun getCurrentStats(): UserStatsEntity = userStatsDao.getUserStats() ?: UserStatsEntity()

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
    userStatsDao.insertUserStats(current.copy(points = (current.points + amount).coerceAtLeast(0)))
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

  suspend fun adjustClearedTasks(delta: Int) {
    when {
      delta > 0 -> repeat(delta) { incrementClearedTasks() }
      delta < 0 -> repeat(-delta) { decrementClearedTasks() }
    }
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
    appliedTelegramOperationDao.clearAll()
    populateDefaults()
  }

  suspend fun clearAllImportData() {
    chapterDao.clearAll()
    taskDao.clearAll()
    userStatsDao.clearAll()
    activityDayDao.clearAll()
    appliedTelegramOperationDao.clearAll()
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

  data class TaskCompletionMutationResult(
    val completedTaskName: String? = null,
    val milestoneCompanionId: String? = null
  )

  suspend fun setTaskCompletionState(taskId: Int, shouldComplete: Boolean): TaskCompletionMutationResult {
    var completedTaskName: String? = null
    var milestoneCompanionId: String? = null

    database.withTransaction {
      val currentTask = taskDao.getTaskById(taskId) ?: return@withTransaction
      if (currentTask.isCompleted == shouldComplete) return@withTransaction

      taskDao.updateTask(currentTask.copy(isCompleted = shouldComplete))
      val chapter = chapterDao.getChapterById(currentTask.chapterId)
      val stats = userStatsDao.getUserStats() ?: UserStatsEntity()
      val taskReward = xpReward(stats.difficulty, "task")
      val chapterReward = xpReward(stats.difficulty, "chapter")

      if (shouldComplete) {
        userStatsDao.insertUserStats(stats.copy(points = stats.points + taskReward))
        incrementClearedTasksTransactional()
        completedTaskName = currentTask.name

        val chapterTasks = taskDao.getTasksForChapterSnapshot(currentTask.chapterId)
        val chapterJustCompleted = chapterTasks.isNotEmpty() && chapterTasks.all { it.isCompleted }
        if (chapter != null && chapterJustCompleted && !chapter.isCompleted) {
          chapterDao.updateChapter(chapter.copy(isCompleted = true))
          val updatedStats = userStatsDao.getUserStats() ?: stats
          userStatsDao.insertUserStats(updatedStats.copy(points = updatedStats.points + chapterReward))
          milestoneCompanionId = chapter.companionId
        }
      } else {
        userStatsDao.insertUserStats(stats.copy(points = (stats.points - taskReward).coerceAtLeast(0)))
        decrementClearedTasksTransactional()

        if (chapter != null && chapter.isCompleted) {
          chapterDao.updateChapter(chapter.copy(isCompleted = false))
          val updatedStats = userStatsDao.getUserStats() ?: stats
          userStatsDao.insertUserStats(
            updatedStats.copy(points = (updatedStats.points - chapterReward).coerceAtLeast(0))
          )
        }
      }
    }

    return TaskCompletionMutationResult(
      completedTaskName = completedTaskName,
      milestoneCompanionId = milestoneCompanionId
    )
  }

  private suspend fun incrementClearedTasksTransactional() {
    val current = userStatsDao.getUserStats() ?: UserStatsEntity()
    userStatsDao.insertUserStats(current.copy(clearedCount = current.clearedCount + 1))

    val todayStr = DateUtils.formatYyyyMMdd()
    val existing = activityDayDao.getDayByDate(todayStr)
    val newIntensity = if (existing != null) {
      (existing.intensity + 1).coerceAtMost(4)
    } else {
      1
    }
    activityDayDao.insertDay(ActivityDayEntity(todayStr, newIntensity))
  }

  private suspend fun decrementClearedTasksTransactional() {
    val current = userStatsDao.getUserStats() ?: UserStatsEntity()
    userStatsDao.insertUserStats(current.copy(clearedCount = (current.clearedCount - 1).coerceAtLeast(0)))
  }

  private fun xpReward(difficulty: String, action: String): Int {
    return when (action) {
      "task" -> when (difficulty) {
        "Easy" -> 15
        "Hard" -> 2
        else -> 5
      }
      "chapter" -> when (difficulty) {
        "Easy" -> 100
        "Hard" -> 15
        else -> 50
      }
      "focus" -> when (difficulty) {
        "Easy" -> 30
        "Hard" -> 5
        else -> 15
      }
      "streak" -> when (difficulty) {
        "Easy" -> 25
        "Hard" -> 3
        else -> 10
      }
      else -> 0
    }
  }
}
