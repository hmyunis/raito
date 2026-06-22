package com.example.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.database.TaskEntity
import kotlinx.coroutines.runBlocking

class TaskWidgetRemoteViewsService : RemoteViewsService() {
  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
    return TaskWidgetRemoteViewsFactory(applicationContext, intent)
  }
}

private class TaskWidgetRemoteViewsFactory(
  private val context: android.content.Context,
  intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
  private val widgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
  private val widgetRepository = TaskWidgetRepository(context)
  private var selectedBucketAccent: Int = ContextCompat.getColor(context, R.color.widget_teal)
  private var tasks: List<TaskEntity> = emptyList()

  override fun onCreate() = Unit

  override fun onDataSetChanged() {
    runBlocking {
      val chapters = widgetRepository.getChapters()
      val selectedBucket = TaskWidgetState.resolveSelectedBucket(context, widgetId, chapters)
      selectedBucketAccent = TaskWidgetAccent.resolve(context, selectedBucket?.auraInk)
      tasks = selectedBucket?.let { widgetRepository.getTasksForBucket(it.id) } ?: emptyList()
    }
  }

  override fun onDestroy() {
    tasks = emptyList()
  }

  override fun getCount(): Int = tasks.size

  override fun getViewAt(position: Int): RemoteViews {
    val task = tasks.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_task_list_item)
    val views = RemoteViews(context.packageName, R.layout.widget_task_list_item)
    val titleColor = if (task.isCompleted) {
      ContextCompat.getColor(context, R.color.widget_task_done)
    } else {
      ContextCompat.getColor(context, R.color.widget_ink)
    }

    views.setTextViewText(R.id.widget_task_title, task.name)
    views.setTextColor(R.id.widget_task_title, titleColor)
    views.setTextViewText(R.id.widget_task_meta, taskMeta(task))
    views.setTextViewText(R.id.widget_task_toggle, if (task.isCompleted) "UNDO" else "DONE")
    views.setTextColor(R.id.widget_task_toggle, if (task.isCompleted) titleColor else ContextCompat.getColor(context, R.color.widget_ink))
    views.setInt(R.id.widget_task_item_accent, "setBackgroundColor", selectedBucketAccent)

    val fillInIntent = Intent().apply {
      putExtra(TaskWidgetProvider.EXTRA_WIDGET_ID, widgetId)
      putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.id)
    }
    views.setOnClickFillInIntent(R.id.widget_task_toggle, fillInIntent)

    return views
  }

  override fun getLoadingView(): RemoteViews? = null

  override fun getViewTypeCount(): Int = 1

  override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id?.toLong() ?: position.toLong()

  override fun hasStableIds(): Boolean = true

  private fun taskMeta(task: TaskEntity): String {
    val parts = mutableListOf<String>()
    task.timeRemaining?.takeIf { it.isNotBlank() }?.let(parts::add)
    if (task.isPinned) parts += "Pinned"
    if (task.isOverdue) parts += "Overdue"
    if (task.description?.isNotBlank() == true) parts += "Notes"
    return when {
      task.isCompleted && parts.isEmpty() -> "Completed"
      task.isCompleted -> "Completed • ${parts.joinToString(" • ")}"
      parts.isEmpty() -> "Tap to update"
      else -> parts.joinToString(" • ")
    }
  }
}
