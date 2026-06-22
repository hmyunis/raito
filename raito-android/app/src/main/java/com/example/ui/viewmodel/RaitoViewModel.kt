package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.BuildConfig
import com.example.data.database.*
import com.example.data.repository.RaitoRepository
import com.example.data.network.*
import com.example.ui.screens.NotificationHelper
import com.example.widget.TaskWidgetProvider
import com.example.update.AppUpdateManager
import com.example.update.DownloadProgressState
import com.example.update.VersionComparator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.util.CompanionRegistry
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
  HOME, BUCKETS, FOCUS, PROGRESS, SETTINGS, NEW_CHAPTER, MILESTONE, NEW_TASK, SHOP
}

sealed interface UiEvent {
  data class ShowUndoSnackbar(val message: String, val taskId: Int) : UiEvent
}

@OptIn(FlowPreview::class)
class RaitoViewModel(application: Application) : AndroidViewModel(application) {
  private companion object {
    const val KEY_BUCKET_LAYOUT_MODE = "bucket_layout_mode"
    const val KEY_TIME_FORMAT_MODE = "time_format_mode"
    const val DEFAULT_BACKEND_URL = "https://raito.hamdi.dev.et"
  }

  private val appPreferences = application.getSharedPreferences("raito_app_preferences", Context.MODE_PRIVATE)
  private val appUpdateManager = AppUpdateManager(application.applicationContext)

  private val database = AppDatabase.getDatabase(application)
  val repository = RaitoRepository(
    database = database,
    chapterDao = database.chapterDao(),
    taskDao = database.taskDao(),
    userStatsDao = database.userStatsDao(),
    activityDayDao = database.activityDayDao(),
    customAvatarDao = database.customAvatarDao(),
    appliedTelegramOperationDao = database.appliedTelegramOperationDao()
  )

  // Single-use event triggers
  private val _uiEvents = MutableSharedFlow<UiEvent>()
  val uiEvents = _uiEvents.asSharedFlow()

  // Navigation state
  private val _activeScreen = MutableStateFlow(AppScreen.HOME)
  val activeScreen: StateFlow<AppScreen> = _activeScreen.asStateFlow()

  // Selected companion for milestone display
  private val _milestoneCompanionId = MutableStateFlow("Cyber")
  val milestoneCompanionId: StateFlow<String> = _milestoneCompanionId.asStateFlow()

  // Chapter Creation/Edit State
  val editingChapterId = MutableStateFlow<Int?>(null)
  val chapterNameInput = MutableStateFlow("")
  val selectedDiscipline = MutableStateFlow("Study") // Study, Work, Personal, Fitness, Project, Custom
  val selectedCompanionId = MutableStateFlow("Knight") // Knight, Cyber, Scholar
  val selectedAuraInk = MutableStateFlow("Red") // Red, Teal, Purple, Pink, Black
  val selectedDeadline = MutableStateFlow("") // mm/dd/yyyy
  val selectedTelegramSyncEnabled = MutableStateFlow(false)
  val customAuraColor = MutableStateFlow<Color?>(null)

  val chapterSearchQuery = MutableStateFlow("")

  // Task Creation State
  val taskNameInput = MutableStateFlow("")
  val taskChapterIdInput = MutableStateFlow<Int?>(null)
  val taskTimeRemainingInput = MutableStateFlow("Today") // "Today", "Tomorrow", "Soon", "Later"
  val taskIsOverdueInput = MutableStateFlow(false)
  val taskDescriptionInput = MutableStateFlow("")
  val taskDueDatetimeInput = MutableStateFlow("")

  // dialog control states
  val showResetProgressDialog = MutableStateFlow(false)
  val chapterIdToReset = MutableStateFlow<Int?>(null)

  // Focus Timer States
  private val _timerDurationMinutes = MutableStateFlow(25)
  val timerDurationMinutes: StateFlow<Int> = _timerDurationMinutes.asStateFlow()

  private val _timerSecondsLeft = MutableStateFlow(25 * 60)
  val timerSecondsLeft: StateFlow<Int> = _timerSecondsLeft.asStateFlow()

  private val _isTimerRunning = MutableStateFlow(false)
  val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

  val showPenDipAnimation = MutableStateFlow(false)
  val showInkDropAnimation = MutableStateFlow(false)

  private val _confettiTrigger = MutableStateFlow(0L)
  val confettiTrigger: StateFlow<Long> = _confettiTrigger.asStateFlow()

  fun triggerConfetti() {
    _confettiTrigger.value = System.currentTimeMillis()
  }

  // Current active task the user is focusing on
  private val _activeTask = MutableStateFlow<TaskEntity?>(null)
  val activeTask: StateFlow<TaskEntity?> = _activeTask.asStateFlow()

  // Selected Bucket/Chapter filter for detail view under BUCKETS screen
  val selectedChapterId = MutableStateFlow<Int?>(null)

  private val _bucketLayoutMode = MutableStateFlow(
    appPreferences.getString(KEY_BUCKET_LAYOUT_MODE, "Grid").takeIf { it == "Grid" || it == "List" } ?: "Grid"
  )
  val bucketLayoutMode: StateFlow<String> = _bucketLayoutMode.asStateFlow()

  fun updateBucketLayoutMode(mode: String) {
    if (mode != "Grid" && mode != "List") return
    _bucketLayoutMode.value = mode
    appPreferences.edit().putString(KEY_BUCKET_LAYOUT_MODE, mode).apply()
  }

  private val _timeFormatMode = MutableStateFlow(
    appPreferences.getString(KEY_TIME_FORMAT_MODE, "24").takeIf { it == "12" || it == "24" } ?: "24"
  )
  val timeFormatMode: StateFlow<String> = _timeFormatMode.asStateFlow()

  fun updateTimeFormatMode(mode: String) {
    if (mode != "12" && mode != "24") return
    _timeFormatMode.value = mode
    appPreferences.edit().putString(KEY_TIME_FORMAT_MODE, mode).apply()
  }

  // DB Flows
  val chapters: StateFlow<List<ChapterEntity>> = repository.allChapters
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val stats: StateFlow<UserStatsEntity> = repository.userStats
    .map { it ?: UserStatsEntity() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatsEntity())

  val activityDays: StateFlow<List<ActivityDayEntity>> = repository.allActivityDays
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val customAvatars: StateFlow<List<CustomAvatarEntity>> = repository.allCustomAvatars
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Server Integration States
  sealed interface ServerConnectionState {
    object Disconnected : ServerConnectionState
    object Connecting : ServerConnectionState
    data class Connected(val deviceName: String, val lastSeen: String?) : ServerConnectionState
    data class Error(val message: String) : ServerConnectionState
  }

  sealed interface PendingPanelsState {
    object Idle : PendingPanelsState
    object Loading : PendingPanelsState
    data class Success(val panels: List<RemotePanelDto>) : PendingPanelsState
    data class Error(val message: String) : PendingPanelsState
  }

  sealed interface PairingCodeState {
    object Idle : PairingCodeState
    object Loading : PairingCodeState
    data class Success(val code: String, val expiresInMinutes: Int) : PairingCodeState
    data class Error(val message: String) : PairingCodeState
  }

  data class AppUpdateInfo(
    val latestVersion: String,
    val minSupportedVersion: String?,
    val downloadUrl: String,
    val title: String,
    val releaseNotes: List<String>,
    val publishedAt: String?
  )

  sealed interface AppUpdateDownloadState {
    object Idle : AppUpdateDownloadState
    object Preparing : AppUpdateDownloadState
    object Queued : AppUpdateDownloadState
    data class InProgress(
      val percent: Int?,
      val downloadedBytes: Long,
      val totalBytes: Long?
    ) : AppUpdateDownloadState
    object ReadyToInstall : AppUpdateDownloadState
    object Installing : AppUpdateDownloadState
    object InstallPermissionRequired : AppUpdateDownloadState
    data class Failed(val message: String) : AppUpdateDownloadState
  }

  data class AppUpdateUiState(
    val isVisible: Boolean = false,
    val isMandatory: Boolean = false,
    val info: AppUpdateInfo? = null,
    val downloadState: AppUpdateDownloadState = AppUpdateDownloadState.Idle
  )

  private val _serverConnectionState = MutableStateFlow<ServerConnectionState>(ServerConnectionState.Disconnected)
  val serverConnectionState: StateFlow<ServerConnectionState> = _serverConnectionState.asStateFlow()

  private val _pendingPanelsState = MutableStateFlow<PendingPanelsState>(PendingPanelsState.Idle)
  val pendingPanelsState: StateFlow<PendingPanelsState> = _pendingPanelsState.asStateFlow()

  private val _pairingCodeState = MutableStateFlow<PairingCodeState>(PairingCodeState.Idle)
  val pairingCodeState: StateFlow<PairingCodeState> = _pairingCodeState.asStateFlow()
  private val _appUpdateUiState = MutableStateFlow(AppUpdateUiState())
  val appUpdateUiState: StateFlow<AppUpdateUiState> = _appUpdateUiState.asStateFlow()
  private val _isHomeDataReady = MutableStateFlow(false)
  val isHomeDataReady: StateFlow<Boolean> = _isHomeDataReady.asStateFlow()

  private var timerJob: Job? = null
  private var lastSyncedBucketSnapshotSignature: String? = null
  private var appUpdateMonitorJob: Job? = null

  private data class TelegramBucketSyncConfig(
    val backendBaseUrl: String,
    val backendDeviceToken: String
  )

  init {
    viewModelScope.launch {
      // populate defaults if DB is fresh
      repository.populateDefaults()
      
      // select first overdue/uncompleted task as standard active task
      tasks.collectLatest { list ->
        if (_activeTask.value == null && list.isNotEmpty()) {
          val activeOne = list.find { !it.isCompleted && it.isOverdue } ?: list.find { !it.isCompleted }
          _activeTask.value = activeOne
        }
      }
    }

    // Auto-connect on start if a token is present
    viewModelScope.launch {
      stats.collectLatest { s ->
        if (s.backendDeviceToken.isNotBlank() && _serverConnectionState.value is ServerConnectionState.Disconnected) {
          silentCheckConnection(s.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL }, s.backendDeviceToken)
        }
      }
    }

    viewModelScope.launch {
      stats
        .map { it.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL } }
        .distinctUntilChanged()
        .collectLatest { backendBaseUrl ->
          checkForAppUpdate(backendBaseUrl)
        }
    }

    // Background Auto-sync loop
    viewModelScope.launch {
      while (true) {
        delay(40000) // timer pool check every 40 seconds
        val s = stats.value
        if (s.backendDeviceToken.isNotBlank()) {
          try {
            val api = RaitoApiService.create(s.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL })
            syncPendingTelegramTaskOperations(api, s)
            if (!s.autoSyncEnabled) {
              continue
            }
            val resp = api.getPendingPanels("Bearer ${s.backendDeviceToken}")
            if (resp.ok && !resp.panels.isNullOrEmpty()) {
              val currentChapters = chapters.value
              if (currentChapters.isNotEmpty()) {
                val targetChapter = currentChapters.first()
                val importedCount = resp.panels.size
                
                for (panel in resp.panels) {
                  repository.insertTask(
                    TaskEntity(
                      chapterId = targetChapter.id,
                      name = panel.content,
                      timeRemaining = "Today",
                      isCompleted = false,
                      createdAt = System.currentTimeMillis()
                    )
                  )
                }
                notifyTaskWidgetChanged()
                
                api.markImported(
                  "Bearer ${s.backendDeviceToken}",
                  MarkImportedRequest(
                    panel_ids = resp.panels.map { it.remote_panel_id },
                    client_sync_id = "autosync-${System.currentTimeMillis()}",
                    app_version = appVersionName()
                  )
                )

                fetchPendingPanels()

                if (s.notificationsMasterEnabled && s.notifyOnSync) {
                  NotificationHelper.showSystemNotification(
                    getApplication<Application>(),
                    "Raito Cloud Sync Success",
                    "Imported $importedCount task(s) from @theraitobot into '${targetChapter.name}'."
                  )
                }
              }
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    }

    viewModelScope.launch {
      combine(
        chapters,
        tasks,
        stats.map { TelegramBucketSyncConfig(it.backendBaseUrl, it.backendDeviceToken) }
      ) { chaptersList, tasksList, config ->
        Triple(chaptersList, tasksList, config)
      }
        .debounce(1500)
        .collectLatest { (chaptersList, tasksList, config) ->
          syncTelegramBucketSnapshot(chaptersList, tasksList, config)
        }
    }

    viewModelScope.launch {
      combine(
        repository.allChapters.take(1),
        repository.allTasks.take(1),
        repository.userStats.take(1),
        repository.allCustomAvatars.take(1)
      ) { _, _, _, _ -> true }
        .collect {
          _isHomeDataReady.value = true
        }
    }
  }

  private fun silentCheckConnection(url: String, token: String) {
    viewModelScope.launch {
      try {
        val api = RaitoApiService.create(url.ifBlank { DEFAULT_BACKEND_URL })
        val resp = api.checkMe("Bearer $token")
        if (resp.ok && resp.user != null) {
          _serverConnectionState.value = ServerConnectionState.Connected(
            deviceName = resp.user.device_label ?: resp.user.display_name ?: "Android Device",
            lastSeen = resp.user.last_seen_at
          )
          syncTelegramTaskOperationsOnce()
        } else {
          _serverConnectionState.value = ServerConnectionState.Error(resp.error?.message ?: "Check failed.")
        }
      } catch (e: Exception) {
        _serverConnectionState.value = ServerConnectionState.Error(e.localizedMessage ?: "Network error.")
      }
    }
  }

  fun testConnection(url: String, token: String, onComplete: (Boolean) -> Unit = {}) {
    _serverConnectionState.value = ServerConnectionState.Connecting
    viewModelScope.launch {
      try {
        val resolvedUrl = url.ifBlank { DEFAULT_BACKEND_URL }
        val api = RaitoApiService.create(resolvedUrl)
        val resp = api.checkMe("Bearer $token")
        if (resp.ok && resp.user != null) {
          repository.updateStats(
            stats.value.copy(
              backendBaseUrl = resolvedUrl,
              backendDeviceToken = token,
              telegramDeviceName = resp.user.device_label ?: resp.user.display_name ?: "Android Device"
            )
          )
          _serverConnectionState.value = ServerConnectionState.Connected(
            deviceName = resp.user.device_label ?: resp.user.display_name ?: "Android Device",
            lastSeen = resp.user.last_seen_at
          )
          syncTelegramTaskOperationsOnce()
          _uiEvents.emit(UiEvent.ShowUndoSnackbar("Successfully connected to Raito Cloud backend!", -1))
          onComplete(true)
        } else {
          val errMsg = resp.error?.message ?: "Verification failed."
          _serverConnectionState.value = ServerConnectionState.Error(errMsg)
          onComplete(false)
        }
      } catch (e: Exception) {
        val errMsg = e.localizedMessage ?: "Failed to reach host."
        _serverConnectionState.value = ServerConnectionState.Error(errMsg)
        onComplete(false)
      }
    }
  }

  fun registerDeviceOnServer(url: String, displayName: String, deviceLabel: String) {
    _serverConnectionState.value = ServerConnectionState.Connecting
    viewModelScope.launch {
      try {
        val resolvedUrl = url.ifBlank { DEFAULT_BACKEND_URL }
        val api = RaitoApiService.create(resolvedUrl)
        val resp = api.registerDevice(RegisterDeviceRequest(display_name = displayName, device_label = deviceLabel))
        if (resp.ok && resp.device_token != null) {
          repository.updateStats(
            stats.value.copy(
              backendBaseUrl = resolvedUrl,
              backendDeviceToken = resp.device_token,
              telegramDeviceName = resp.user?.device_label ?: resp.user?.display_name ?: "Android Device"
            )
          )
          _serverConnectionState.value = ServerConnectionState.Connected(
            deviceName = resp.user?.device_label ?: resp.user?.display_name ?: "Android Device",
            lastSeen = "Just registered"
          )
          syncTelegramTaskOperationsOnce()
          _uiEvents.emit(UiEvent.ShowUndoSnackbar("Registered new companion device on server!", -1))
        } else {
          val errMsg = resp.error?.message ?: "Registration failed."
          _serverConnectionState.value = ServerConnectionState.Error(errMsg)
        }
      } catch (e: Exception) {
        val errMsg = e.localizedMessage ?: "Failed to reach registration endpoint."
        _serverConnectionState.value = ServerConnectionState.Error(errMsg)
      }
    }
  }

  fun disconnectServer() {
    viewModelScope.launch {
      repository.updateStats(
        stats.value.copy(
          backendDeviceToken = "",
          telegramDeviceName = ""
        )
      )
      _serverConnectionState.value = ServerConnectionState.Disconnected
      _pendingPanelsState.value = PendingPanelsState.Idle
      _pairingCodeState.value = PairingCodeState.Idle
      _uiEvents.emit(UiEvent.ShowUndoSnackbar("Disconnected from Raito Cloud.", -1))
    }
  }

  fun generatePairingCode() {
    val token = stats.value.backendDeviceToken
    val url = stats.value.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL }
    if (token.isBlank()) {
      _pairingCodeState.value = PairingCodeState.Error("Device is not connected to any server.")
      return
    }

    _pairingCodeState.value = PairingCodeState.Loading
    viewModelScope.launch {
      try {
        val api = RaitoApiService.create(url)
        val resp = api.createPairingCode("Bearer $token")
        if (resp.ok && resp.pairing != null) {
          _pairingCodeState.value = PairingCodeState.Success(
            code = resp.pairing.code,
            expiresInMinutes = resp.pairing.expires_in_minutes
          )
        } else {
          _pairingCodeState.value = PairingCodeState.Error(resp.error?.message ?: "Failed to build code.")
        }
      } catch (e: Exception) {
        _pairingCodeState.value = PairingCodeState.Error(e.localizedMessage ?: "Network error.")
      }
    }
  }

  fun fetchPendingPanels() {
    val token = stats.value.backendDeviceToken
    val url = stats.value.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL }
    if (token.isBlank()) {
      _pendingPanelsState.value = PendingPanelsState.Idle
      return
    }

    _pendingPanelsState.value = PendingPanelsState.Loading
    viewModelScope.launch {
      try {
        val api = RaitoApiService.create(url)
        val resp = api.getPendingPanels("Bearer $token")
        if (resp.ok && resp.panels != null) {
          _pendingPanelsState.value = PendingPanelsState.Success(resp.panels)
        } else {
          _pendingPanelsState.value = PendingPanelsState.Error(resp.error?.message ?: "Failed to fetch panels.")
        }
      } catch (e: Exception) {
        _pendingPanelsState.value = PendingPanelsState.Error(e.localizedMessage ?: "Failed to fetch panels.")
      }
    }
  }

  fun importPanelAsTask(panel: RemotePanelDto, targetChapterId: Int) {
    val token = stats.value.backendDeviceToken
    val url = stats.value.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL }
    viewModelScope.launch {
      try {
        // 1. Create task locally
        repository.insertTask(
          TaskEntity(
            chapterId = targetChapterId,
            name = panel.content,
            timeRemaining = "Today",
            isCompleted = false,
            createdAt = System.currentTimeMillis()
          )
        )
        notifyTaskWidgetChanged()

        // 2. Mark imported on server
        if (token.isNotBlank()) {
          val api = RaitoApiService.create(url)
          api.markImported(
            "Bearer $token",
            MarkImportedRequest(
              panel_ids = listOf(panel.remote_panel_id),
              client_sync_id = "import-${System.currentTimeMillis()}",
              app_version = appVersionName()
            )
          )
          // 3. Refresh pending panels list
          fetchPendingPanels()
        }
        _uiEvents.emit(UiEvent.ShowUndoSnackbar("Successfully imported task!", -1))
      } catch (e: Exception) {
        _uiEvents.emit(UiEvent.ShowUndoSnackbar("Imported locally, but server match failed: ${e.localizedMessage}", -1))
        fetchPendingPanels() // retry refresh
      }
    }
  }

  fun discardPanel(panel: RemotePanelDto) {
    val token = stats.value.backendDeviceToken
    val url = stats.value.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL }
    if (token.isBlank()) return

    viewModelScope.launch {
      try {
        val api = RaitoApiService.create(url)
        val resp = api.discardPanels("Bearer $token", DiscardPanelsRequest(panel_ids = listOf(panel.remote_panel_id)))
        if (resp.ok) {
          _uiEvents.emit(UiEvent.ShowUndoSnackbar("Discarded Telegram panel.", -1))
          fetchPendingPanels()
        } else {
          _uiEvents.emit(UiEvent.ShowUndoSnackbar("Failed to discard: ${resp.error?.message}", -1))
        }
      } catch (e: Exception) {
        _uiEvents.emit(UiEvent.ShowUndoSnackbar("Network failure card clean.", -1))
      }
    }
  }

  private suspend fun syncPendingTelegramTaskOperations(
    api: RaitoApiService,
    currentStats: UserStatsEntity
  ) {
    val response = api.getPendingTelegramTaskOperations(
      bearerToken = "Bearer ${currentStats.backendDeviceToken}",
      limit = 100
    )

    if (!response.ok || response.operations.isEmpty()) {
      return
    }

    val acknowledgements = mutableListOf<TelegramTaskOperationAckDto>()

    for (operation in response.operations) {
      if (repository.hasAppliedTelegramOperation(operation.operation_id)) {
        acknowledgements += TelegramTaskOperationAckDto(
          operation_id = operation.operation_id,
          status = "applied"
        )
        continue
      }

      val ack = applyTelegramTaskOperation(operation)
      acknowledgements += ack
      if (ack.status == "applied") {
        repository.markTelegramOperationApplied(operation.operation_id)
      }
    }

    if (acknowledgements.isNotEmpty()) {
      api.acknowledgeTelegramTaskOperations(
        bearerToken = "Bearer ${currentStats.backendDeviceToken}",
        request = TelegramTaskOperationsAckRequest(operations = acknowledgements)
      )
    }
  }

  private fun syncTelegramTaskOperationsOnce() {
    val currentStats = stats.value
    if (currentStats.backendDeviceToken.isBlank()) return

    viewModelScope.launch {
      try {
        val api = RaitoApiService.create(currentStats.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL })
        syncPendingTelegramTaskOperations(api, currentStats)
      } catch (_: Exception) {
      }
    }
  }

  private suspend fun applyTelegramTaskOperation(
    operation: TelegramTaskOperationDto
  ): TelegramTaskOperationAckDto {
    return try {
      when (operation.operation_type) {
        "create_task" -> {
          val targetBucketId = operation.target_bucket_client_id
          val chapter = repository.getChapterById(targetBucketId)
          val taskName = operation.task_name?.trim().orEmpty()

          if (chapter == null) {
            TelegramTaskOperationAckDto(
              operation_id = operation.operation_id,
              status = "failed",
              error_message = "Target bucket no longer exists on this device."
            )
          } else if (taskName.isBlank()) {
            TelegramTaskOperationAckDto(
              operation_id = operation.operation_id,
              status = "ignored",
              error_message = "Task name was empty."
            )
          } else {
            val createdTaskId = repository.insertTask(
              TaskEntity(
                chapterId = chapter.id,
                name = taskName,
                timeRemaining = "Today",
                isCompleted = false,
                createdAt = System.currentTimeMillis()
              )
            ).toInt()
            notifyTaskWidgetChanged()

            TelegramTaskOperationAckDto(
              operation_id = operation.operation_id,
              status = "applied",
              client_created_task_id = createdTaskId
            )
          }
        }

        "set_task_completion" -> {
          val taskId = operation.target_task_client_id
          val desiredCompletion = operation.desired_completion
          if (taskId == null || desiredCompletion == null) {
            TelegramTaskOperationAckDto(
              operation_id = operation.operation_id,
              status = "ignored",
              error_message = "Task completion payload was incomplete."
            )
          } else {
            val task = repository.getTaskById(taskId)
            if (task == null) {
              TelegramTaskOperationAckDto(
                operation_id = operation.operation_id,
                status = "failed",
                error_message = "Target task no longer exists on this device."
              )
            } else {
              setTaskCompletionState(taskId, desiredCompletion)
              TelegramTaskOperationAckDto(
                operation_id = operation.operation_id,
                status = "applied"
              )
            }
          }
        }

        else -> TelegramTaskOperationAckDto(
          operation_id = operation.operation_id,
          status = "ignored",
          error_message = "Unsupported operation type: ${operation.operation_type}"
        )
      }
    } catch (exception: Exception) {
      TelegramTaskOperationAckDto(
        operation_id = operation.operation_id,
        status = "failed",
        error_message = exception.localizedMessage ?: "Failed to apply operation."
      )
    }
  }

  fun navigateTo(screen: AppScreen) {
    _activeScreen.value = screen
  }

  fun triggerMilestone(companionId: String) {
    _milestoneCompanionId.value = companionId
    _activeScreen.value = AppScreen.MILESTONE
  }

  // Chapter actions
  fun startEditingChapter(chapter: ChapterEntity) {
    editingChapterId.value = chapter.id
    chapterNameInput.value = chapter.name
    selectedDiscipline.value = chapter.discipline
    selectedCompanionId.value = chapter.companionId
    selectedDeadline.value = chapter.deadline ?: ""
    selectedTelegramSyncEnabled.value = chapter.telegramSyncEnabled
    
    // Check if auraInk is a hex color
    if (chapter.auraInk.startsWith("#")) {
      selectedAuraInk.value = "Custom"
      try {
        customAuraColor.value = Color(android.graphics.Color.parseColor(chapter.auraInk))
      } catch (e: Exception) {
        customAuraColor.value = null
      }
    } else {
      selectedAuraInk.value = chapter.auraInk
      customAuraColor.value = null
    }
    
    _activeScreen.value = AppScreen.NEW_CHAPTER
  }

  fun saveChapter() {
    val name = chapterNameInput.value.trim()
    if (name.isEmpty()) return
    val auraInkToSave = if (selectedAuraInk.value == "Custom" && customAuraColor.value != null) {
      val c = customAuraColor.value!!
      String.format("#%02X%02X%02X%02X", (c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    } else {
      selectedAuraInk.value
    }

    viewModelScope.launch {
      val editId = editingChapterId.value
      if (editId != null) {
        val existing = chapters.value.find { it.id == editId }
        if (existing != null) {
          repository.updateChapter(
            existing.copy(
              name = name,
              discipline = selectedDiscipline.value,
              companionId = selectedCompanionId.value,
              auraInk = auraInkToSave,
              deadline = selectedDeadline.value.ifEmpty { null },
              telegramSyncEnabled = selectedTelegramSyncEnabled.value
            )
          )
        }
      } else {
        repository.insertChapter(
          ChapterEntity(
            name = name,
            discipline = selectedDiscipline.value,
            companionId = selectedCompanionId.value,
            auraInk = auraInkToSave,
            deadline = selectedDeadline.value.ifEmpty { null },
            telegramSyncEnabled = selectedTelegramSyncEnabled.value
          )
        )
      }

      notifyTaskWidgetChanged()

      clearChapterForm()
      _activeScreen.value = AppScreen.HOME
    }
  }

  fun clearChapterForm() {
    editingChapterId.value = null
    chapterNameInput.value = ""
    selectedDeadline.value = ""
    selectedTelegramSyncEnabled.value = false
    selectedDiscipline.value = "Study"
    selectedCompanionId.value = "Knight"
    selectedAuraInk.value = "Red"
    customAuraColor.value = null
  }

  fun toggleTaskCompletion(task: TaskEntity) {
    viewModelScope.launch {
      val completionResult = setTaskCompletionState(task.id, shouldComplete = !task.isCompleted)
      if (completionResult.completedTaskName != null) {
        _uiEvents.emit(UiEvent.ShowUndoSnackbar("Completed task: ${completionResult.completedTaskName}", task.id))
      }
      completionResult.milestoneCompanionId?.let(::triggerMilestone)
    }
  }

  fun undoTaskCompletion(taskId: Int) {
    viewModelScope.launch {
      setTaskCompletionState(taskId, shouldComplete = false)
    }
  }

  private data class TaskCompletionResult(
    val completedTaskName: String? = null,
    val milestoneCompanionId: String? = null
  )

  private suspend fun setTaskCompletionState(taskId: Int, shouldComplete: Boolean): TaskCompletionResult {
    val result = repository.setTaskCompletionState(taskId, shouldComplete)
    notifyTaskWidgetChanged()
    return TaskCompletionResult(
      completedTaskName = result.completedTaskName,
      milestoneCompanionId = result.milestoneCompanionId
    )
  }

  fun deleteTask(taskId: Int) {
    viewModelScope.launch {
      repository.deleteTask(taskId)
      notifyTaskWidgetChanged()
    }
  }

  fun deleteChapter(chapterId: Int) {
    viewModelScope.launch {
      repository.deleteChapter(chapterId)
      if (selectedChapterId.value == chapterId) {
        selectedChapterId.value = null
      }
      if (editingChapterId.value == chapterId) {
        clearChapterForm()
      }
      notifyTaskWidgetChanged()
      _activeScreen.value = AppScreen.BUCKETS
    }
  }

  fun updateChapterTelegramSync(chapterId: Int, enabled: Boolean) {
    viewModelScope.launch {
      val chapter = repository.getChapterById(chapterId) ?: return@launch
      if (chapter.telegramSyncEnabled == enabled) return@launch
      repository.updateChapter(chapter.copy(telegramSyncEnabled = enabled))
      notifyTaskWidgetChanged()
    }
  }

  fun toggleTaskPin(task: TaskEntity) {
    viewModelScope.launch {
      repository.updateTask(task.copy(isPinned = !task.isPinned))
      notifyTaskWidgetChanged()
    }
  }

  fun addTaskToChapter(chapterId: Int, name: String, description: String? = null, dueDatetime: String? = null) {
    val trimmedName = name.trim()
    if (trimmedName.isEmpty()) return
    viewModelScope.launch {
      repository.insertTask(
        TaskEntity(
          chapterId = chapterId,
          name = trimmedName,
          timeRemaining = "Soon",
          description = description,
          dueDatetime = dueDatetime
        )
      )
      notifyTaskWidgetChanged()
    }
  }

  fun createNewTask() {
    val name = taskNameInput.value.trim()
    val chId = taskChapterIdInput.value ?: return
    if (name.isEmpty()) return
    viewModelScope.launch {
      repository.insertTask(
        TaskEntity(
          chapterId = chId,
          name = name,
          timeRemaining = taskTimeRemainingInput.value,
          isCompleted = false,
          isOverdue = taskIsOverdueInput.value,
          description = taskDescriptionInput.value.trim().ifEmpty { null },
          dueDatetime = taskDueDatetimeInput.value.trim().ifEmpty { null }
        )
      )
      notifyTaskWidgetChanged()
      
      // clear state
      taskNameInput.value = ""
      taskTimeRemainingInput.value = "Today"
      taskIsOverdueInput.value = false
      taskDescriptionInput.value = ""
      taskDueDatetimeInput.value = ""
      
      _activeScreen.value = AppScreen.HOME
    }
  }

  fun setActiveTask(task: TaskEntity) {
    _activeTask.value = task
  }

  fun purchaseCompanion(companionId: String, cost: Int) {
    viewModelScope.launch {
      val success = repository.spendPoints(cost)
      if (success) {
        val currentStats = stats.value
        val list = currentStats.unlockedCompanions.split(",").toMutableSet()
        list.add(companionId)
        repository.updateStats(
          currentStats.copy(
            unlockedCompanions = list.joinToString(",")
          )
        )
        triggerConfetti()
      }
    }
  }

  fun equipCompanion(companionId: String) {
    viewModelScope.launch {
      val currentStats = stats.value
      if (companionId.startsWith("custom_") || currentStats.unlockedCompanions.split(",").contains(companionId)) {
        repository.updateStats(currentStats.copy(activeCompanionId = companionId))
      }
    }
  }

  fun setTimerDuration(minutes: Int) {
    pauseTimer()
    _timerDurationMinutes.value = minutes
    _timerSecondsLeft.value = minutes * 60
  }

  fun startTimer() {
    if (_isTimerRunning.value) return

    viewModelScope.launch {
      // 1. Show fountain pen splash animation for 1 second!
      showPenDipAnimation.value = true
      delay(1200)
      showPenDipAnimation.value = false

      // 2. Start timer countdown thread
      _isTimerRunning.value = true
      timerJob = viewModelScope.launch {
        while (_timerSecondsLeft.value > 0) {
          delay(1000)
          _timerSecondsLeft.value = _timerSecondsLeft.value - 1
        }
        completeTimer()
      }
    }
  }

  fun pauseTimer() {
    _isTimerRunning.value = false
    timerJob?.cancel()
    timerJob = null
  }

  fun resetTimer() {
    pauseTimer()
    _timerSecondsLeft.value = _timerDurationMinutes.value * 60
  }

  private fun completeTimer() {
    pauseTimer()
    viewModelScope.launch {
      // 1. Splash ink splash overlay
      showInkDropAnimation.value = true
      delay(1000)
      showInkDropAnimation.value = false

      // 2. Award Points
      val rewardedPoints = getXpReward("focus")
      repository.addPoints(rewardedPoints)
      repository.incrementClearedTasks()

      // 3. Complete the active task if preset
      _activeTask.value?.let { currentTask ->
        toggleTaskCompletion(currentTask)
      }

      // 4. Send Focus Complete native notification
      val s = stats.value
      if (s.notificationsMasterEnabled && s.notifyOnFocus) {
        NotificationHelper.showSystemNotification(
          getApplication<Application>(),
          "Raito Focus Session Complete",
          "Splendid work! You added +$rewardedPoints PTS with companion."
        )
      }
    }
  }

  fun requestResetChapterProgress(chapterId: Int) {
    chapterIdToReset.value = chapterId
    showResetProgressDialog.value = true
  }

  fun confirmResetChapterProgress() {
    val chapterId = chapterIdToReset.value ?: return
    viewModelScope.launch {
      database.withTransaction {
        val chapterTasks = repository.getTasksForChapterSnapshot(chapterId)
        val completedTasks = chapterTasks.filter { it.isCompleted }
        completedTasks.forEach { task ->
          repository.updateTask(task.copy(isCompleted = false))
        }

        val chapter = repository.getChapterById(chapterId)
        if (chapter?.isCompleted == true) {
          repository.updateChapter(chapter.copy(isCompleted = false))
          repository.addPoints(-getXpReward("chapter"))
        }

        if (completedTasks.isNotEmpty()) {
          repository.addPoints(-(completedTasks.size * getXpReward("task")))
          repository.adjustClearedTasks(-completedTasks.size)
        }
      }

      showResetProgressDialog.value = false
      chapterIdToReset.value = null
      notifyTaskWidgetChanged()
    }
  }

  fun updateSettingTheme(mode: String) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(themeMode = mode))
    }
  }

  fun updateSettingTypography(scale: Float) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(typographyScale = scale))
    }
  }

  fun updateSettingReducedMotion(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(reducedMotion = enabled))
    }
  }

  fun updateSettingSilenceChibi(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(silenceChibiComments = enabled))
    }
  }

  fun updateSettingBucketColor(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(bucketColoring = enabled))
    }
  }

  fun updateSettingReminders(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(dailyReminders = enabled))
      val context = getApplication<Application>()
      if (enabled && stats.value.notificationsMasterEnabled) {
        NotificationHelper.scheduleDailyReminder(context)
      } else {
        NotificationHelper.cancelDailyReminder(context)
      }
    }
  }

  fun claimWelcomingGift() {
    viewModelScope.launch {
      var claimed = false
      database.withTransaction {
        val currentStats = repository.getCurrentStats()
        if (!currentStats.isWelcomingGiftClaimed) {
          repository.updateStats(
            currentStats.copy(
              points = currentStats.points + 500,
              isWelcomingGiftClaimed = true
            )
          )
          claimed = true
        }
      }
      if (claimed) {
        triggerConfetti()
        _uiEvents.emit(UiEvent.ShowUndoSnackbar("Welcoming Gift Claimed! +500 PTS added.", -1))
      }
    }
  }

  fun exportJsonData(): String {
    try {
      val root = org.json.JSONObject()
      root.put("version", 1)
      
      val s = stats.value
      val statsObj = org.json.JSONObject()
      statsObj.put("points", s.points)
      statsObj.put("dailyStreak", s.dailyStreak)
      statsObj.put("clearedCount", s.clearedCount)
      statsObj.put("themeMode", s.themeMode)
      statsObj.put("typographyScale", s.typographyScale.toDouble())
      statsObj.put("reducedMotion", s.reducedMotion)
      statsObj.put("silenceChibiComments", s.silenceChibiComments)
      statsObj.put("bucketColoring", s.bucketColoring)
      statsObj.put("dailyReminders", s.dailyReminders)
      statsObj.put("activeCompanionId", s.activeCompanionId)
      statsObj.put("unlockedCompanions", s.unlockedCompanions)
      statsObj.put("difficulty", s.difficulty)
      statsObj.put("lastStreakClaimedDate", s.lastStreakClaimedDate)
      statsObj.put("backendBaseUrl", s.backendBaseUrl)
      statsObj.put("backendDeviceToken", s.backendDeviceToken)
      statsObj.put("telegramDeviceName", s.telegramDeviceName)
      statsObj.put("autoSyncEnabled", s.autoSyncEnabled)
      statsObj.put("notificationsMasterEnabled", s.notificationsMasterEnabled)
      statsObj.put("notifyOnSync", s.notifyOnSync)
      statsObj.put("notifyOnFocus", s.notifyOnFocus)
      statsObj.put("isWelcomingGiftClaimed", s.isWelcomingGiftClaimed)
      root.put("stats", statsObj)

      val chaptersList = chapters.value
      val chArray = org.json.JSONArray()
      for (i in chaptersList.indices) {
        val ch = chaptersList[i]
        val chObj = org.json.JSONObject()
        chObj.put("export_index", i)
        chObj.put("id", ch.id)
        chObj.put("name", ch.name)
        chObj.put("discipline", ch.discipline)
        chObj.put("companionId", ch.companionId)
        chObj.put("auraInk", ch.auraInk)
        chObj.put("deadline", ch.deadline ?: org.json.JSONObject.NULL)
        chObj.put("telegramSyncEnabled", ch.telegramSyncEnabled)
        chObj.put("isCompleted", ch.isCompleted)
        chObj.put("timestamp", ch.timestamp)
        chArray.put(chObj)
      }
      root.put("chapters", chArray)

      val tasksList = tasks.value
      val tArray = org.json.JSONArray()
      for (t in tasksList) {
        val tObj = org.json.JSONObject()
        tObj.put("name", t.name)
        tObj.put("timeRemaining", t.timeRemaining ?: org.json.JSONObject.NULL)
        tObj.put("isCompleted", t.isCompleted)
        tObj.put("isOverdue", t.isOverdue)
        tObj.put("description", t.description ?: org.json.JSONObject.NULL)
        tObj.put("dueDatetime", t.dueDatetime ?: org.json.JSONObject.NULL)
        tObj.put("createdAt", t.createdAt ?: System.currentTimeMillis())
        tObj.put("isPinned", t.isPinned)
        
        val chIndex = chaptersList.indexOfFirst { it.id == t.chapterId }
        tObj.put("chapter_export_index", chIndex)
        tArray.put(tObj)
      }
      root.put("tasks", tArray)

      return root.toString(2)
    } catch (e: Exception) {
      e.printStackTrace()
      return ""
    }
  }

  fun importJsonBackup(jsonStr: String, onComplete: (Boolean) -> Unit) {
    viewModelScope.launch {
      try {
        val root = org.json.JSONObject(jsonStr)
        val statsObj = root.optJSONObject("stats") ?: return@launch onComplete(false)
        val s = UserStatsEntity(
          points = statsObj.optInt("points", 0),
          dailyStreak = statsObj.optInt("dailyStreak", 0),
          clearedCount = statsObj.optInt("clearedCount", 0),
          themeMode = statsObj.optString("themeMode", "Light"),
          typographyScale = statsObj.optDouble("typographyScale", 1.0).toFloat(),
          reducedMotion = statsObj.optBoolean("reducedMotion", false),
          silenceChibiComments = statsObj.optBoolean("silenceChibiComments", false),
          bucketColoring = statsObj.optBoolean("bucketColoring", true),
          dailyReminders = statsObj.optBoolean("dailyReminders", true),
          activeCompanionId = statsObj.optString("activeCompanionId", "Cyber"),
          unlockedCompanions = statsObj.optString("unlockedCompanions", "Cyber"),
          difficulty = statsObj.optString("difficulty", "Medium"),
          lastStreakClaimedDate = statsObj.optString("lastStreakClaimedDate", ""),
          backendBaseUrl = statsObj.optString("backendBaseUrl", DEFAULT_BACKEND_URL),
          backendDeviceToken = statsObj.optString("backendDeviceToken", ""),
          telegramDeviceName = statsObj.optString("telegramDeviceName", ""),
          autoSyncEnabled = statsObj.optBoolean("autoSyncEnabled", false),
          notificationsMasterEnabled = statsObj.optBoolean("notificationsMasterEnabled", true),
          notifyOnSync = statsObj.optBoolean("notifyOnSync", true),
          notifyOnFocus = statsObj.optBoolean("notifyOnFocus", true),
          isWelcomingGiftClaimed = statsObj.optBoolean("isWelcomingGiftClaimed", false)
        )

        repository.clearAllImportData()
        repository.updateStats(s)

        val chArray = root.optJSONArray("chapters") ?: org.json.JSONArray()
        val indexToNewIdMap = mutableMapOf<Int, Int>()
        for (i in 0 until chArray.length()) {
          val chObj = chArray.getJSONObject(i)
          val exportIndex = chObj.optInt("export_index", i)
          val chEntity = ChapterEntity(
            name = chObj.getString("name"),
            discipline = chObj.getString("discipline"),
            companionId = chObj.getString("companionId"),
            auraInk = chObj.getString("auraInk"),
            deadline = if (chObj.isNull("deadline")) null else chObj.getString("deadline"),
            telegramSyncEnabled = chObj.optBoolean("telegramSyncEnabled", false),
            isCompleted = chObj.optBoolean("isCompleted", false),
            timestamp = chObj.optLong("timestamp", System.currentTimeMillis())
          )
          val newId = repository.insertChapter(chEntity).toInt()
          indexToNewIdMap[exportIndex] = newId
        }

        val tArray = root.optJSONArray("tasks") ?: org.json.JSONArray()
        for (i in 0 until tArray.length()) {
          val tObj = tArray.getJSONObject(i)
          val chExportIndex = tObj.optInt("chapter_export_index", -1)
          val targetChapterId = indexToNewIdMap[chExportIndex] ?: continue
          
          val tEntity = TaskEntity(
            chapterId = targetChapterId,
            name = tObj.getString("name"),
            timeRemaining = if (tObj.isNull("timeRemaining")) null else tObj.getString("timeRemaining"),
            isCompleted = tObj.optBoolean("isCompleted", false),
            isOverdue = tObj.optBoolean("isOverdue", false),
            description = if (tObj.isNull("description")) null else tObj.getString("description"),
            dueDatetime = if (tObj.isNull("dueDatetime")) null else tObj.getString("dueDatetime"),
            createdAt = tObj.optLong("createdAt", System.currentTimeMillis()),
            isPinned = tObj.optBoolean("isPinned", false)
          )
          repository.insertTask(tEntity)
        }

        _activeTask.value = null
        notifyTaskWidgetChanged()
        onComplete(true)
      } catch (e: Exception) {
        e.printStackTrace()
        onComplete(false)
      }
    }
  }

  fun updateSettingAutoSync(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(autoSyncEnabled = enabled))
    }
  }

  fun updateSettingNotificationsMaster(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(notificationsMasterEnabled = enabled))
      val context = getApplication<Application>()
      if (enabled && stats.value.dailyReminders) {
        NotificationHelper.scheduleDailyReminder(context)
      } else if (!enabled) {
        NotificationHelper.cancelDailyReminder(context)
      }
    }
  }

  fun updateSettingNotifyOnSync(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(notifyOnSync = enabled))
    }
  }

  fun updateSettingNotifyOnFocus(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateStats(stats.value.copy(notifyOnFocus = enabled))
    }
  }

  fun createCustomAvatar(
    name: String,
    neutralUri: android.net.Uri?,
    happyUri: android.net.Uri?,
    focusUri: android.net.Uri?,
    sadUri: android.net.Uri?,
    completedUri: android.net.Uri?,
    cost: Int = 0
  ) {
    viewModelScope.launch {
      val context = getApplication<Application>().applicationContext
      val id = System.currentTimeMillis() // unique tag

      val nPath = neutralUri?.let { copyUriToInternal(context, it, "avatar_neutral_$id") }
      val hPath = happyUri?.let { copyUriToInternal(context, it, "avatar_happy_$id") }
      val fPath = focusUri?.let { copyUriToInternal(context, it, "avatar_focus_$id") }
      val sPath = sadUri?.let { copyUriToInternal(context, it, "avatar_sad_$id") }
      val cPath = completedUri?.let { copyUriToInternal(context, it, "avatar_completed_$id") }

      val entity = CustomAvatarEntity(
        name = name,
        neutralPath = nPath,
        happyPath = hPath,
        focusPath = fPath,
        sadPath = sPath,
        completedPath = cPath
      )
      val newId = repository.insertCustomAvatar(entity)
      if (cost > 0) {
        repository.addPoints(-cost)
      }
      // Automatically equip the newly created avatar
      equipCompanion("custom_$newId")
    }
  }

  fun addCustomAvatarDirect(
    name: String,
    neutralUrl: Any,
    happyUrl: Any,
    focusUrl: Any,
    sadUrl: Any,
    completedUrl: Any,
    cost: Int
  ) {
    viewModelScope.launch {
      val context = getApplication<Application>().applicationContext
      val id = System.currentTimeMillis()

      fun resolvePath(source: Any, suffix: String): String? {
        return if (source is android.net.Uri) {
          copyUriToInternal(context, source, "shop_${id}_$suffix")
        } else if (source is String && source.isNotBlank()) {
          source
        } else null
      }

      val entity = CustomAvatarEntity(
        name = name,
        neutralPath = resolvePath(neutralUrl, "neutral"),
        happyPath = resolvePath(happyUrl, "happy"),
        focusPath = resolvePath(focusUrl, "focus"),
        sadPath = resolvePath(sadUrl, "sad"),
        completedPath = resolvePath(completedUrl, "completed")
      )
      val newId = repository.insertCustomAvatar(entity)
      repository.addPoints(-cost)
      // Unlock/equip it directly
      equipCompanion("custom_$newId")
      _uiEvents.emit(UiEvent.ShowUndoSnackbar("Added and equipped custom item: $name!", -1))
    }
  }

  private fun copyUriToInternal(context: android.content.Context, uri: android.net.Uri, baseName: String): String? {
    try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val type = context.contentResolver.getType(uri) ?: ""
        val extension = if (type.contains("gif", ignoreCase = true)) "gif" else "png"
        val fileName = "$baseName.$extension"
        val file = java.io.File(context.filesDir, fileName)
        file.outputStream().use { outputStream ->
          inputStream.copyTo(outputStream)
        }
        return file.absolutePath
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  fun getAvatarUrl(companionId: String, expression: String = "neutral"): String {
    if (companionId.startsWith("custom_")) {
      val customId = companionId.removePrefix("custom_").toIntOrNull() ?: return CompanionRegistry.CYBER.neutral
      val custom = customAvatars.value.find { it.id == customId } ?: return CompanionRegistry.CYBER.neutral
      return when (expression) {
        "happy" -> custom.happyPath ?: custom.neutralPath ?: CompanionRegistry.CYBER.happy
        "focus" -> custom.focusPath ?: custom.neutralPath ?: CompanionRegistry.CYBER.focus
        "sad" -> custom.sadPath ?: custom.neutralPath ?: CompanionRegistry.CYBER.sad
        "completed" -> custom.completedPath ?: custom.happyPath ?: custom.neutralPath ?: CompanionRegistry.CYBER.completed
        else -> custom.neutralPath ?: CompanionRegistry.CYBER.neutral
      }
    } else {
      val expressions = CompanionRegistry.getExpressions(companionId)
      return when (expression) {
        "happy" -> expressions.happy
        "focus" -> expressions.focus
        "sad" -> expressions.sad
        "completed" -> expressions.completed
        else -> expressions.neutral
      }
    }
  }

  fun getActiveAvatarUrl(expression: String = "neutral"): String {
    return getAvatarUrl(stats.value.activeCompanionId, expression)
  }

  fun getCompanionName(companionId: String): String {
    if (companionId.startsWith("custom_")) {
      val customId = companionId.removePrefix("custom_").toIntOrNull() ?: return "Custom Avatar"
      val custom = customAvatars.value.find { it.id == customId }
      return custom?.name ?: "Custom Avatar"
    }
    return when (companionId) {
      "Knight" -> "Tiny Knight"
      "Scholar" -> "Studio Artist"
      "Ranger" -> "Shadow Ranger"
      "Dragon" -> "Dragon Keeper"
      else -> "Cyber Assistant"
    }
  }

  fun deleteCustomAvatar(avatar: CustomAvatarEntity) {
    viewModelScope.launch {
      if (stats.value.activeCompanionId == "custom_${avatar.id}") {
        repository.updateStats(stats.value.copy(activeCompanionId = "Cyber"))
      }
      repository.deleteCustomAvatar(avatar)
    }
  }

  fun resetAllDatabaseData() {
    viewModelScope.launch {
      repository.resetAllData()
      notifyTaskWidgetChanged()
    }
  }

  fun getXpReward(action: String): Int {
    val diff = stats.value.difficulty
    return when (action) {
      "task" -> when (diff) {
        "Easy" -> 15
        "Hard" -> 2
        else -> 5 // Medium
      }
      "focus" -> when (diff) {
        "Easy" -> 30
        "Hard" -> 5
        else -> 15 // Medium
      }
      "chapter" -> when (diff) {
        "Easy" -> 100
        "Hard" -> 15
        else -> 50 // Medium
      }
      "streak" -> when (diff) {
        "Easy" -> 25
        "Hard" -> 3
        else -> 10 // Medium
      }
      else -> 0
    }
  }

  fun getShopPriceMultiplier(): Float {
    return when (stats.value.difficulty) {
      "Easy" -> 0.5f
      "Hard" -> 2.0f
      else -> 1.0f
    }
  }

  fun changeDifficulty(newDiff: String) {
    viewModelScope.launch {
      val currentStats = stats.value
      repository.updateStats(currentStats.copy(difficulty = newDiff, points = 0))
      _uiEvents.emit(UiEvent.ShowUndoSnackbar("Difficulty set to $newDiff! XP reset to 0.", -1))
    }
  }

  fun canClaimDailyStreakBonus(): Boolean {
    val todayStr = DateUtils.formatYyyyMMdd()
    return stats.value.lastStreakClaimedDate != todayStr
  }

  fun claimDailyStreakBonus() {
    viewModelScope.launch {
      val todayStr = DateUtils.formatYyyyMMdd()
      val reward = getXpReward("streak")
      var claimed = false
      database.withTransaction {
        val currentStats = repository.getCurrentStats()
        if (currentStats.lastStreakClaimedDate != todayStr) {
          repository.updateStats(
            currentStats.copy(
              points = currentStats.points + reward,
              lastStreakClaimedDate = todayStr,
              dailyStreak = currentStats.dailyStreak + 1
            )
          )
          claimed = true
        }
      }
      if (claimed) {
        _uiEvents.emit(UiEvent.ShowUndoSnackbar("Streak check-in bonus! Earned +$reward PTS!", -1))
      }
    }
  }

  private fun notifyTaskWidgetChanged() {
    TaskWidgetProvider.updateAllWidgets(getApplication())
  }

  fun dismissAppUpdatePrompt() {
    val currentState = _appUpdateUiState.value
    if (!currentState.isMandatory) {
      _appUpdateUiState.value = currentState.copy(isVisible = false)
    }
  }

  fun startAppUpdateDownload() {
    val info = _appUpdateUiState.value.info ?: return

    appUpdateMonitorJob?.cancel()
    _appUpdateUiState.value = _appUpdateUiState.value.copy(
      isVisible = true,
      downloadState = AppUpdateDownloadState.Preparing
    )

    viewModelScope.launch {
      val downloadId = runCatching {
        appUpdateManager.startDownload(
          version = info.latestVersion,
          title = info.title,
          downloadUrl = info.downloadUrl
        )
      }.getOrElse { error ->
        _appUpdateUiState.value = _appUpdateUiState.value.copy(
          downloadState = AppUpdateDownloadState.Failed(
            error.localizedMessage ?: "Unable to start the update download."
          )
        )
        return@launch
      }

      monitorAppUpdateDownload(downloadId)
    }
  }

  fun retryAppUpdateInstall() {
    val downloadId = appUpdateManager.getTrackedDownloadId() ?: return
    if (!appUpdateManager.canRequestPackageInstalls()) {
      _appUpdateUiState.value = _appUpdateUiState.value.copy(
        downloadState = AppUpdateDownloadState.InstallPermissionRequired
      )
      return
    }

    _appUpdateUiState.value = _appUpdateUiState.value.copy(downloadState = AppUpdateDownloadState.Installing)
    val result = appUpdateManager.launchInstaller(downloadId)
    if (result.isFailure) {
      _appUpdateUiState.value = _appUpdateUiState.value.copy(
        downloadState = AppUpdateDownloadState.Failed(
          result.exceptionOrNull()?.localizedMessage ?: "Unable to launch the installer."
        )
      )
    }
  }

  fun openAppUpdateInstallSettings() {
    appUpdateManager.openUnknownSourcesSettings()
  }

  fun openAppUpdateInBrowser() {
    val downloadUrl = _appUpdateUiState.value.info?.downloadUrl ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    getApplication<Application>().startActivity(intent)
  }

  private fun checkForAppUpdate(backendBaseUrl: String) {
    appUpdateMonitorJob?.cancel()

    viewModelScope.launch {
      try {
        val response = RaitoApiService
          .create(backendBaseUrl)
          .getAndroidAppUpdate(version = appVersionName())

        val updateDto = response.update
        if (updateDto == null) {
          if (appUpdateManager.getTrackedDownloadId() == null) {
            _appUpdateUiState.value = AppUpdateUiState()
          }
          return@launch
        }

        val isNewerVersionAvailable = VersionComparator.compare(updateDto.latest_version, appVersionName()) > 0
        val isMandatory = updateDto.min_supported_version?.let {
          VersionComparator.compare(appVersionName(), it) < 0
        } ?: false

        if (!isNewerVersionAvailable && !isMandatory) {
          appUpdateManager.clearTracking()
          _appUpdateUiState.value = AppUpdateUiState()
          return@launch
        }

        if (!updateDto.prompt_enabled && !isMandatory) {
          _appUpdateUiState.value = AppUpdateUiState()
          return@launch
        }

        val info = updateDto.toAppUpdateInfo()
        _appUpdateUiState.value = AppUpdateUiState(
          isVisible = true,
          isMandatory = isMandatory,
          info = info,
          downloadState = AppUpdateDownloadState.Idle
        )

        appUpdateManager.getTrackedDownloadId()?.let { trackedId ->
          val tracked = appUpdateManager.getTrackedUpdate()
          if (tracked != null && tracked.version == info.latestVersion) {
            monitorAppUpdateDownload(trackedId)
          }
        }
      } catch (_: Exception) {
        restoreTrackedAppUpdateIfPossible()
      }
    }
  }

  private fun restoreTrackedAppUpdateIfPossible() {
    val trackedDownloadId = appUpdateManager.getTrackedDownloadId() ?: return
    val tracked = appUpdateManager.getTrackedUpdate() ?: return

    _appUpdateUiState.value = AppUpdateUiState(
      isVisible = true,
      isMandatory = false,
      info = AppUpdateInfo(
        latestVersion = tracked.version,
        minSupportedVersion = null,
        downloadUrl = tracked.downloadUrl,
        title = tracked.title,
        releaseNotes = emptyList(),
        publishedAt = null
      ),
      downloadState = AppUpdateDownloadState.Preparing
    )
    monitorAppUpdateDownload(trackedDownloadId)
  }

  private fun monitorAppUpdateDownload(downloadId: Long) {
    appUpdateMonitorJob?.cancel()
    appUpdateMonitorJob = viewModelScope.launch {
      while (true) {
        when (val progress = appUpdateManager.queryDownloadProgress(downloadId)) {
          DownloadProgressState.Idle -> {
            _appUpdateUiState.value = _appUpdateUiState.value.copy(
              downloadState = AppUpdateDownloadState.Idle
            )
            return@launch
          }
          DownloadProgressState.Enqueued -> {
            _appUpdateUiState.value = _appUpdateUiState.value.copy(
              isVisible = true,
              downloadState = AppUpdateDownloadState.Queued
            )
          }
          is DownloadProgressState.Running -> {
            _appUpdateUiState.value = _appUpdateUiState.value.copy(
              isVisible = true,
              downloadState = AppUpdateDownloadState.InProgress(
                percent = progress.percent,
                downloadedBytes = progress.downloadedBytes,
                totalBytes = progress.totalBytes
              )
            )
          }
          is DownloadProgressState.Failed -> {
            _appUpdateUiState.value = _appUpdateUiState.value.copy(
              isVisible = true,
              downloadState = AppUpdateDownloadState.Failed(progress.message)
            )
            return@launch
          }
          is DownloadProgressState.ReadyToInstall -> {
            if (!appUpdateManager.canRequestPackageInstalls()) {
              _appUpdateUiState.value = _appUpdateUiState.value.copy(
                isVisible = true,
                downloadState = AppUpdateDownloadState.InstallPermissionRequired
              )
              return@launch
            }

            _appUpdateUiState.value = _appUpdateUiState.value.copy(
              isVisible = true,
              downloadState = AppUpdateDownloadState.ReadyToInstall
            )
            return@launch
          }
        }

        delay(500)
      }
    }
  }

  private fun AndroidAppUpdateDto.toAppUpdateInfo(): AppUpdateInfo {
    return AppUpdateInfo(
      latestVersion = latest_version,
      minSupportedVersion = min_supported_version,
      downloadUrl = download_url,
      title = release_title?.takeIf { it.isNotBlank() } ?: "Raito $latest_version",
      releaseNotes = release_notes,
      publishedAt = published_at
    )
  }

  private fun appVersionName(): String = BuildConfig.VERSION_NAME

  private suspend fun syncTelegramBucketSnapshot(
    chaptersList: List<ChapterEntity>,
    tasksList: List<TaskEntity>,
    config: TelegramBucketSyncConfig
  ) {
    if (config.backendDeviceToken.isBlank()) {
      lastSyncedBucketSnapshotSignature = null
      return
    }

    val syncedBuckets = chaptersList
      .filter { it.telegramSyncEnabled }
      .sortedByDescending { it.timestamp }
      .map { chapter ->
        SyncedBucketSnapshotDto(
          chapter_id = chapter.id,
          name = chapter.name,
          discipline = chapter.discipline,
          companion_id = chapter.companionId,
          aura_ink = chapter.auraInk,
          deadline = chapter.deadline,
          is_completed = chapter.isCompleted,
          timestamp = chapter.timestamp,
          tasks = tasksList
            .filter { it.chapterId == chapter.id }
            .sortedWith(
              compareByDescending<TaskEntity> { it.isPinned }
                .thenBy { it.isCompleted }
                .thenByDescending { it.createdAt ?: 0L }
            )
            .map { task ->
              SyncedBucketTaskSnapshotDto(
                task_id = task.id,
                name = task.name,
                time_remaining = task.timeRemaining,
                is_completed = task.isCompleted,
                is_overdue = task.isOverdue,
                description = task.description,
                due_datetime = task.dueDatetime,
                created_at = task.createdAt,
                is_pinned = task.isPinned
              )
            }
        )
      }

    val signature = syncedBuckets.toString() + "|" + config.backendBaseUrl + "|" + config.backendDeviceToken

    if (signature == lastSyncedBucketSnapshotSignature) {
      return
    }

    try {
      val request = SyncedBucketsSnapshotRequest(
        buckets = syncedBuckets,
        client_sync_id = "bucket-snapshot-${System.currentTimeMillis()}",
        app_version = appVersionName()
      )
      val api = RaitoApiService.create(config.backendBaseUrl.ifBlank { DEFAULT_BACKEND_URL })
      val response = api.syncSyncedBucketsSnapshot(
        bearerToken = "Bearer ${config.backendDeviceToken}",
        request = request
      )
      if (response.ok) {
        lastSyncedBucketSnapshotSignature = signature
      }
    } catch (_: Exception) {
      // Best-effort sync only; local data remains authoritative.
    }
  }
}
