package com.example.widget

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.ChapterEntity
import com.example.data.database.TaskEntity
import com.example.data.repository.RaitoRepository

class TaskWidgetRepository(context: Context) {
  private val appContext = context.applicationContext
  private val database = AppDatabase.getDatabase(appContext)
  private val repository = RaitoRepository(
    database = database,
    chapterDao = database.chapterDao(),
    taskDao = database.taskDao(),
    userStatsDao = database.userStatsDao(),
    activityDayDao = database.activityDayDao(),
    customAvatarDao = database.customAvatarDao(),
    appliedTelegramOperationDao = database.appliedTelegramOperationDao()
  )

  suspend fun getChapters(): List<ChapterEntity> = repository.getAllChaptersSnapshot()

  suspend fun getTasksForBucket(bucketId: Int): List<TaskEntity> = repository.getTasksForChapterWidget(bucketId)

  suspend fun toggleTask(taskId: Int) {
    val task = repository.getTaskById(taskId) ?: return
    repository.setTaskCompletionState(taskId, !task.isCompleted)
  }
}
