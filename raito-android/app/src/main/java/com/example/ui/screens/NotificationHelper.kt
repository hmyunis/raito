package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object NotificationHelper {
  private const val CHANNEL_ID = "raito_sync_channel"
  private const val DAILY_REMINDER_REQUEST_CODE = 4001

  fun canPostNotifications(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  }

  fun showSystemNotification(context: Context, title: String, content: String) {
    try {
      if (!canPostNotifications(context)) return

      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Raito Notifications",
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = "Alerts for Raito Synced Tasks & Timers"
        }
        notificationManager.createNotificationChannel(channel)
      }

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info) // Stable system standard icon
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

      notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun scheduleDailyReminder(context: Context) {
    if (!canPostNotifications(context)) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = dailyReminderPendingIntent(context)
    val triggerTime = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 20)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }.timeInMillis

    alarmManager.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      triggerTime,
      AlarmManager.INTERVAL_DAY,
      pendingIntent
    )
  }

  fun cancelDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(dailyReminderPendingIntent(context))
  }

  private fun dailyReminderPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, DailyReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
      context,
      DAILY_REMINDER_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }
}

class DailyReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    NotificationHelper.showSystemNotification(
      context,
      "Raito Daily Check-in",
      "Review your buckets and complete a task to keep your streak alive."
    )
  }
}
