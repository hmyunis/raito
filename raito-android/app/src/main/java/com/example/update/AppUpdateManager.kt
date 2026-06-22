package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

data class TrackedUpdate(
  val version: String,
  val downloadUrl: String,
  val title: String
)

sealed interface DownloadProgressState {
  object Idle : DownloadProgressState
  object Enqueued : DownloadProgressState
  data class Running(
    val percent: Int?,
    val downloadedBytes: Long,
    val totalBytes: Long?
  ) : DownloadProgressState
  data class Failed(val message: String) : DownloadProgressState
  data class ReadyToInstall(val downloadId: Long) : DownloadProgressState
}

class AppUpdateManager(private val context: Context) {
  private companion object {
    const val PREFS_NAME = "raito_app_update"
    const val KEY_DOWNLOAD_ID = "download_id"
    const val KEY_VERSION = "version"
    const val KEY_URL = "url"
    const val KEY_TITLE = "title"
  }

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

  fun getTrackedUpdate(): TrackedUpdate? {
    val version = prefs.getString(KEY_VERSION, null)
    val url = prefs.getString(KEY_URL, null)
    val title = prefs.getString(KEY_TITLE, null)
    if (version.isNullOrBlank() || url.isNullOrBlank() || title.isNullOrBlank()) {
      return null
    }
    return TrackedUpdate(version = version, downloadUrl = url, title = title)
  }

  fun getTrackedDownloadId(): Long? {
    if (!prefs.contains(KEY_DOWNLOAD_ID)) return null
    return prefs.getLong(KEY_DOWNLOAD_ID, -1L).takeIf { it > 0L }
  }

  fun clearTracking() {
    prefs.edit().clear().apply()
  }

  fun startDownload(version: String, title: String, downloadUrl: String): Long {
    val currentTrackedId = getTrackedDownloadId()
    val currentTrackedVersion = prefs.getString(KEY_VERSION, null)
    val currentTrackedUrl = prefs.getString(KEY_URL, null)

    if (
      currentTrackedId != null &&
      currentTrackedVersion == version &&
      currentTrackedUrl == downloadUrl
    ) {
      return currentTrackedId
    }

    currentTrackedId?.let { existingId ->
      runCatching { downloadManager.remove(existingId) }
    }

    val request = DownloadManager.Request(Uri.parse(downloadUrl))
      .setTitle(title)
      .setDescription("Downloading update")
      .setMimeType("application/vnd.android.package-archive")
      .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
      .setAllowedOverMetered(true)
      .setAllowedOverRoaming(true)
      .setDestinationInExternalFilesDir(
        context,
        Environment.DIRECTORY_DOWNLOADS,
        "raito-$version.apk"
      )

    val downloadId = downloadManager.enqueue(request)

    prefs.edit()
      .putLong(KEY_DOWNLOAD_ID, downloadId)
      .putString(KEY_VERSION, version)
      .putString(KEY_URL, downloadUrl)
      .putString(KEY_TITLE, title)
      .apply()

    return downloadId
  }

  fun queryDownloadProgress(downloadId: Long): DownloadProgressState {
    val query = DownloadManager.Query().setFilterById(downloadId)
    downloadManager.query(query).use { cursor ->
      if (!cursor.moveToFirst()) {
        clearTracking()
        return DownloadProgressState.Idle
      }

      val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
      val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
      val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
      val totalBytesRaw = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
      val totalBytes = totalBytesRaw.takeIf { it > 0L }
      val percent = totalBytes?.let {
        ((bytesDownloaded.toDouble() / it.toDouble()) * 100.0).toInt().coerceIn(0, 100)
      }

      return when (status) {
        DownloadManager.STATUS_PENDING -> DownloadProgressState.Enqueued
        DownloadManager.STATUS_PAUSED,
        DownloadManager.STATUS_RUNNING -> DownloadProgressState.Running(
          percent = percent,
          downloadedBytes = bytesDownloaded,
          totalBytes = totalBytes
        )
        DownloadManager.STATUS_SUCCESSFUL -> DownloadProgressState.ReadyToInstall(downloadId)
        DownloadManager.STATUS_FAILED -> {
          clearTracking()
          DownloadProgressState.Failed(message = failureReason(reason))
        }
        else -> DownloadProgressState.Idle
      }
    }
  }

  fun canRequestPackageInstalls(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
  }

  fun openUnknownSourcesSettings() {
    val intent = Intent(
      Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
      Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  fun launchInstaller(downloadId: Long): Result<Unit> {
    return runCatching {
      val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
        ?: error("Downloaded package is unavailable.")

      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(installIntent)
    }
  }

  private fun failureReason(reason: Int): String {
    return when (reason) {
      DownloadManager.ERROR_CANNOT_RESUME -> "Download could not resume. Retry the update."
      DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage device unavailable."
      DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "The update package already exists."
      DownloadManager.ERROR_FILE_ERROR -> "The update package could not be written to storage."
      DownloadManager.ERROR_HTTP_DATA_ERROR -> "The server returned corrupted download data."
      DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough device storage for this update."
      DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "The download URL redirected too many times."
      DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "The server rejected the download request."
      DownloadManager.ERROR_UNKNOWN -> "Download failed unexpectedly."
      DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network connection."
      DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for a stronger connection."
      else -> "Download failed. Please try again."
    }
  }
}
