package com.example.widget

import android.content.Context
import com.example.data.database.ChapterEntity

object TaskWidgetState {
  private const val PREFS_NAME = "raito_task_widget_state"

  fun getSelectedBucketId(context: Context, widgetId: Int): Int? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return if (prefs.contains("bucket_$widgetId")) {
      prefs.getInt("bucket_$widgetId", -1).takeIf { it > 0 }
    } else {
      null
    }
  }

  fun setSelectedBucketId(context: Context, widgetId: Int, bucketId: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putInt("bucket_$widgetId", bucketId)
      .apply()
  }

  fun clearWidget(context: Context, widgetId: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .remove("bucket_$widgetId")
      .apply()
  }

  fun resolveSelectedBucket(
    context: Context,
    widgetId: Int,
    chapters: List<ChapterEntity>
  ): ChapterEntity? {
    if (chapters.isEmpty()) {
      clearWidget(context, widgetId)
      return null
    }

    val storedId = getSelectedBucketId(context, widgetId)
    val resolved = chapters.firstOrNull { it.id == storedId } ?: chapters.first()
    setSelectedBucketId(context, widgetId, resolved.id)
    return resolved
  }

  fun cycleBucket(context: Context, widgetId: Int, chapters: List<ChapterEntity>, step: Int): ChapterEntity? {
    if (chapters.isEmpty()) {
      clearWidget(context, widgetId)
      return null
    }

    val currentId = getSelectedBucketId(context, widgetId)
    val currentIndex = chapters.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
    val nextIndex = (currentIndex + step).floorMod(chapters.size)
    val selected = chapters[nextIndex]
    setSelectedBucketId(context, widgetId, selected.id)
    return selected
  }

  private fun Int.floorMod(size: Int): Int {
    val remainder = this % size
    return if (remainder < 0) remainder + size else remainder
  }
}
