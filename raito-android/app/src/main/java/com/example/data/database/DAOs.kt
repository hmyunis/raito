package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
  @Query("SELECT * FROM chapters ORDER BY timestamp DESC")
  fun getAllChapters(): Flow<List<ChapterEntity>>

  @Query("SELECT * FROM chapters WHERE id = :id")
  suspend fun getChapterById(id: Int): ChapterEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChapter(chapter: ChapterEntity): Long

  @Update
  suspend fun updateChapter(chapter: ChapterEntity)

  @Query("DELETE FROM chapters WHERE id = :id")
  suspend fun deleteChapterById(id: Int)

  @Query("DELETE FROM chapters")
  suspend fun clearAll()
}

@Dao
interface TaskDao {
  @Query("SELECT * FROM tasks WHERE id = :id")
  suspend fun getTaskById(id: Int): TaskEntity?

  @Query("SELECT * FROM tasks WHERE chapterId = :chapterId ORDER BY createdAt DESC")
  fun getTasksForChapter(chapterId: Int): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks WHERE chapterId = :chapterId")
  suspend fun getTasksForChapterSnapshot(chapterId: Int): List<TaskEntity>

  @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
  fun getAllTasks(): Flow<List<TaskEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity): Long

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Query("DELETE FROM tasks WHERE id = :id")
  suspend fun deleteTaskById(id: Int)

  @Query("DELETE FROM tasks WHERE chapterId = :chapterId")
  suspend fun deleteTasksByChapterId(chapterId: Int)

  @Query("DELETE FROM tasks")
  suspend fun clearAll()
}

@Dao
interface UserStatsDao {
  @Query("SELECT * FROM user_stats WHERE id = 1")
  fun getUserStatsFlow(): Flow<UserStatsEntity?>

  @Query("SELECT * FROM user_stats WHERE id = 1")
  suspend fun getUserStats(): UserStatsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUserStats(stats: UserStatsEntity)

  @Update
  suspend fun updateUserStats(stats: UserStatsEntity)

  @Query("DELETE FROM user_stats")
  suspend fun clearAll()
}

@Dao
interface ActivityDayDao {
  @Query("SELECT * FROM activity_days")
  fun getAllDays(): Flow<List<ActivityDayEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDay(day: ActivityDayEntity)

  @Query("DELETE FROM activity_days")
  suspend fun clearAll()
}

@Dao
interface CustomAvatarDao {
  @Query("SELECT * FROM custom_avatars ORDER BY id DESC")
  fun getAllCustomAvatars(): Flow<List<CustomAvatarEntity>>

  @Query("SELECT * FROM custom_avatars WHERE id = :id")
  suspend fun getCustomAvatarById(id: Int): CustomAvatarEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomAvatar(avatar: CustomAvatarEntity): Long

  @Delete
  suspend fun deleteCustomAvatar(avatar: CustomAvatarEntity)

  @Query("DELETE FROM custom_avatars")
  suspend fun clearAll()
}
