package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        appWidgetIds.forEach { widgetId ->
          updateSingleWidget(context.applicationContext, appWidgetManager, widgetId)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    appWidgetIds.forEach { TaskWidgetState.clearWidget(context, it) }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)

    when (intent.action) {
      ACTION_PREV_BUCKET,
      ACTION_NEXT_BUCKET,
      ACTION_REFRESH,
      ACTION_TOGGLE_TASK,
      ACTION_FORCE_REFRESH -> {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
          try {
            handleAction(context.applicationContext, intent)
          } finally {
            pendingResult.finish()
          }
        }
      }
    }
  }

  private fun handleAction(context: Context, intent: Intent) {
    val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    val repository = TaskWidgetRepository(context)
    val appWidgetManager = AppWidgetManager.getInstance(context)

    when (intent.action) {
      ACTION_PREV_BUCKET,
      ACTION_NEXT_BUCKET -> {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
          val step = if (intent.action == ACTION_PREV_BUCKET) -1 else 1
          val chapters = runBlocking { repository.getChapters() }
          TaskWidgetState.cycleBucket(context, widgetId, chapters, step)
          updateSingleWidget(context, appWidgetManager, widgetId)
        }
      }

      ACTION_TOGGLE_TASK -> {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId > 0) {
          runBlocking { repository.toggleTask(taskId) }
          updateAllWidgets(context)
        }
      }

      ACTION_REFRESH -> {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
          updateSingleWidget(context, appWidgetManager, widgetId)
        }
      }

      ACTION_FORCE_REFRESH -> updateAllWidgets(context)
    }
  }

  companion object {
    const val ACTION_PREV_BUCKET = "com.example.widget.action.PREV_BUCKET"
    const val ACTION_NEXT_BUCKET = "com.example.widget.action.NEXT_BUCKET"
    const val ACTION_REFRESH = "com.example.widget.action.REFRESH"
    const val ACTION_TOGGLE_TASK = "com.example.widget.action.TOGGLE_TASK"
    const val ACTION_FORCE_REFRESH = "com.example.widget.action.FORCE_REFRESH"
    const val EXTRA_WIDGET_ID = "extra_widget_id"
    const val EXTRA_TASK_ID = "extra_task_id"

    fun updateAllWidgets(context: Context) {
      CoroutineScope(Dispatchers.IO).launch {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, TaskWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return@launch

        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_task_list)
        widgetIds.forEach { widgetId ->
          updateSingleWidget(context.applicationContext, appWidgetManager, widgetId)
        }
      }
    }

    private fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
      val repository = TaskWidgetRepository(context)
      val chapters = runBlocking { repository.getChapters() }
      val selectedBucket = TaskWidgetState.resolveSelectedBucket(context, widgetId, chapters)
      val tasks = selectedBucket?.let { runBlocking { repository.getTasksForBucket(it.id) } } ?: emptyList()
      val accentColor = TaskWidgetAccent.resolve(context, selectedBucket?.auraInk)

      val views = RemoteViews(context.packageName, R.layout.widget_task_overview).apply {
        val openCount = tasks.count { !it.isCompleted }
        val completedCount = tasks.size - openCount

        setTextViewText(R.id.widget_bucket_name, selectedBucket?.name ?: "No Buckets Yet")
        setTextViewText(
          R.id.widget_bucket_meta,
          if (selectedBucket == null) {
            "Create your first bucket in Raito to start using the widget."
          } else {
            "$openCount open • $completedCount done • ${tasks.size} total"
          }
        )
        setTextViewText(
          R.id.widget_bucket_badge,
          if (selectedBucket == null) "NO ACTIVE BUCKET" else selectedBucket.discipline.uppercase()
        )
        setInt(R.id.widget_accent_strip, "setBackgroundColor", accentColor)
        setTextColor(R.id.widget_bucket_badge, accentColor)
        setTextColor(R.id.widget_prev_bucket, accentColor)
        setTextColor(R.id.widget_next_bucket, accentColor)
        setTextColor(R.id.widget_refresh, accentColor)

        setViewVisibility(R.id.widget_empty_state, if (tasks.isEmpty()) View.VISIBLE else View.GONE)
        setTextViewText(
          R.id.widget_empty_title,
          if (selectedBucket == null) "No buckets yet" else "No tasks in ${selectedBucket.name}"
        )
        setTextViewText(
          R.id.widget_empty_message,
          if (selectedBucket == null) {
            "Open Raito and create a bucket to begin."
          } else {
            "Add tasks in the app or switch to another bucket."
          }
        )

        val serviceIntent = Intent(context, TaskWidgetRemoteViewsService::class.java).apply {
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
          data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        setRemoteAdapter(R.id.widget_task_list, serviceIntent)
        setEmptyView(R.id.widget_task_list, R.id.widget_empty_state)

        val toggleTemplateIntent = Intent(context, TaskWidgetProvider::class.java).apply {
          action = ACTION_TOGGLE_TASK
        }
        setPendingIntentTemplate(
          R.id.widget_task_list,
          PendingIntent.getBroadcast(
            context,
            widgetId,
            toggleTemplateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )
        )

        setOnClickPendingIntent(R.id.widget_prev_bucket, actionIntent(context, ACTION_PREV_BUCKET, widgetId))
        setOnClickPendingIntent(R.id.widget_next_bucket, actionIntent(context, ACTION_NEXT_BUCKET, widgetId))
        setOnClickPendingIntent(R.id.widget_refresh, actionIntent(context, ACTION_REFRESH, widgetId))

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
          context,
          widgetId + 5000,
          openAppIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setOnClickPendingIntent(R.id.widget_bucket_name, openAppPendingIntent)
        setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
      }

      appWidgetManager.updateAppWidget(widgetId, views)
      appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_task_list)
    }

    private fun actionIntent(context: Context, action: String, widgetId: Int): PendingIntent {
      val intent = Intent(context, TaskWidgetProvider::class.java).apply {
        this.action = action
        putExtra(EXTRA_WIDGET_ID, widgetId)
      }
      return PendingIntent.getBroadcast(
        context,
        "$action:$widgetId".hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    }
  }
}
