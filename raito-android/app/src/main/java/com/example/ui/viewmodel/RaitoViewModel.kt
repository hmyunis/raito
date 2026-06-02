package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.RaitoRepository
import com.example.data.network.*
import com.example.ui.screens.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class RaitoViewModel(application: Application) : AndroidViewModel(application) {
  private companion object {
    const val KEY_BUCKET_LAYOUT_MODE = "bucket_layout_mode"
    const val KEY_TIME_FORMAT_MODE = "time_format_mode"
  }

  private val appPreferences = application.getSharedPreferences("raito_app_preferences", Context.MODE_PRIVATE)

  private val database = AppDatabase.getDatabase(application)
  val repository = RaitoRepository(
    database.chapterDao(),
    database.taskDao(),
    database.userStatsDao(),
    database.activityDayDao(),
    database.customAvatarDao()
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

  private val _serverConnectionState = MutableStateFlow<ServerConnectionState>(ServerConnectionState.Disconnected)
  val serverConnectionState: StateFlow<ServerConnectionState> = _serverConnectionState.asStateFlow()

  private val _pendingPanelsState = MutableStateFlow<PendingPanelsState>(PendingPanelsState.Idle)
  val pendingPanelsState: StateFlow<PendingPanelsState> = _pendingPanelsState.asStateFlow()

  private val _pairingCodeState = MutableStateFlow<PairingCodeState>(PairingCodeState.Idle)
  val pairingCodeState: StateFlow<PairingCodeState> = _pairingCodeState.asStateFlow()

  private var timerJob: Job? = null

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
          silentCheckConnection("https://raito.hamdi.dev.et", s.backendDeviceToken)
        }
      }
    }

    // Background Auto-sync loop
    viewModelScope.launch {
      while (true) {
        delay(40000) // timer pool check every 40 seconds
        val s = stats.value
        if (s.autoSyncEnabled && s.backendDeviceToken.isNotBlank()) {
          try {
            val api = RaitoApiService.create("https://raito.hamdi.dev.et")
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
                
                api.markImported(
                  "Bearer ${s.backendDeviceToken}",
                  MarkImportedRequest(
                    panel_ids = resp.panels.map { it.remote_panel_id },
                    client_sync_id = "autosync-${System.currentTimeMillis()}",
                    app_version = "1.0.0"
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
  }

  private fun silentCheckConnection(url: String, token: String) {
    viewModelScope.launch {
      try {
        val api = RaitoApiService.create("https://raito.hamdi.dev.et")
        val resp = api.checkMe("Bearer $token")
        if (resp.ok && resp.user != null) {
          _serverConnectionState.value = ServerConnectionState.Connected(
            deviceName = resp.user.device_label ?: resp.user.display_name ?: "Android Device",
            lastSeen = resp.user.last_seen_at
          )
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
        val api = RaitoApiService.create("https://raito.hamdi.dev.et")
        val resp = api.checkMe("Bearer $token")
        if (resp.ok && resp.user != null) {
          repository.updateStats(
            stats.value.copy(
              backendBaseUrl = "https://raito.hamdi.dev.et",
              backendDeviceToken = token,
              telegramDeviceName = resp.user.device_label ?: resp.user.display_name ?: "Android Device"
            )
          )
          _serverConnectionState.value = ServerConnectionState.Connected(
            deviceName = resp.user.device_label ?: resp.user.display_name ?: "Android Device",
            lastSeen = resp.user.last_seen_at
          )
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
        val api = RaitoApiService.create("https://raito.hamdi.dev.et")
        val resp = api.registerDevice(RegisterDeviceRequest(display_name = displayName, device_label = deviceLabel))
        if (resp.ok && resp.device_token != null) {
          repository.updateStats(
            stats.value.copy(
              backendBaseUrl = "https://raito.hamdi.dev.et",
              backendDeviceToken = resp.device_token,
              telegramDeviceName = resp.user?.device_label ?: resp.user?.display_name ?: "Android Device"
            )
          )
          _serverConnectionState.value = ServerConnectionState.Connected(
            deviceName = resp.user?.device_label ?: resp.user?.display_name ?: "Android Device",
            lastSeen = "Just registered"
          )
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
    val url = "https://raito.hamdi.dev.et"
    if (token.isBlank()) {
      _pairingCodeState.value = PairingCodeState.Error("Device is not connected to any server.")
      return
    }

    _pairingCodeState.value = PairingCodeState.Loading
    viewModelScope.launch {
      try {
        val api = RaitoApiService.create("https://raito.hamdi.dev.et")
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
    val url = "https://raito.hamdi.dev.et"
    if (token.isBlank()) {
      _pendingPanelsState.value = PendingPanelsState.Idle
      return
    }

    _pendingPanelsState.value = PendingPanelsState.Loading
    viewModelScope.launch {
      try {
        val api = RaitoApiService.create("https://raito.hamdi.dev.et")
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
    val url = "https://raito.hamdi.dev.et"
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

        // 2. Mark imported on server
        if (token.isNotBlank()) {
          val api = RaitoApiService.create("https://raito.hamdi.dev.et")
          api.markImported(
            "Bearer $token",
            MarkImportedRequest(
              panel_ids = listOf(panel.remote_panel_id),
              client_sync_id = "import-${System.currentTimeMillis()}",
              app_version = "1.0.0"
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
    val url = "https://raito.hamdi.dev.et"
    if (token.isBlank()) return

    viewModelScope.launch {
      try {
        val api = RaitoApiService.create("https://raito.hamdi.dev.et")
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
              deadline = selectedDeadline.value.ifEmpty { null }
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
            deadline = selectedDeadline.value.ifEmpty { null }
          )
        )
      }

      clearChapterForm()
      _activeScreen.value = AppScreen.HOME
    }
  }

  fun clearChapterForm() {
    editingChapterId.value = null
    chapterNameInput.value = ""
    selectedDeadline.value = ""
    selectedDiscipline.value = "Study"
    selectedCompanionId.value = "Knight"
    selectedAuraInk.value = "Red"
    customAuraColor.value = null
  }

  fun toggleTaskCompletion(task: TaskEntity) {
    viewModelScope.launch {
      val updated = task.copy(isCompleted = !task.isCompleted)
      repository.updateTask(updated)
      
      if (updated.isCompleted) {
        repository.addPoints(getXpReward("task"))
        repository.incrementClearedTasks()
        
        // Emit undo snackbar event
        _uiEvents.emit(UiEvent.ShowUndoSnackbar("Completed task: ${task.name}", task.id))
        
        // check if this completes the chapter entirely!
        val chapterId = task.chapterId
        val chapterTasks = tasks.value.filter { it.chapterId == chapterId }
        val allOtherDone = chapterTasks.filter { it.id != task.id }.all { it.isCompleted }
        if (allOtherDone) {
          // Chapter is newly finalized fully completed!
          val ch = chapters.value.find { it.id == chapterId }
          if (ch != null && !ch.isCompleted) {
            repository.updateChapter(ch.copy(isCompleted = true))
            repository.addPoints(getXpReward("chapter")) // completion bonus
            triggerMilestone(ch.companionId) // celebrate milestone!
          }
        }
      } else {
        // Uncompleted chapter
        val ch = chapters.value.find { it.id == task.chapterId }
        if (ch != null && ch.isCompleted) {
          repository.updateChapter(ch.copy(isCompleted = false))
        }
      }
    }
  }

  fun undoTaskCompletion(taskId: Int) {
    viewModelScope.launch {
      val task = tasks.value.find { it.id == taskId } ?: return@launch
      if (task.isCompleted) {
        val updated = task.copy(isCompleted = false)
        repository.updateTask(updated)
        repository.addPoints(-getXpReward("task"))
        repository.decrementClearedTasks()
        
        val ch = chapters.value.find { it.id == task.chapterId }
        if (ch != null && ch.isCompleted) {
          repository.updateChapter(ch.copy(isCompleted = false))
          repository.addPoints(-getXpReward("chapter"))
        }
      }
    }
  }

  fun deleteTask(taskId: Int) {
    viewModelScope.launch {
      repository.deleteTask(taskId)
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
      _activeScreen.value = AppScreen.BUCKETS
    }
  }

  fun toggleTaskPin(task: TaskEntity) {
    viewModelScope.launch {
      repository.updateTask(task.copy(isPinned = !task.isPinned))
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
      // Complete Tasks are cleared/returned to uncompleted status
      val list = tasks.value.filter { it.chapterId == chapterId }
      list.forEach { task ->
        repository.updateTask(task.copy(isCompleted = false))
      }
      
      // Bucket completed status cleared
      val ch = chapters.value.find { it.id == chapterId }
      ch?.let {
        repository.updateChapter(it.copy(isCompleted = false))
      }

      showResetProgressDialog.value = false
      chapterIdToReset.value = null
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
      val s = stats.value
      if (!s.isWelcomingGiftClaimed) {
        val updated = s.copy(
          points = s.points + 500,
          isWelcomingGiftClaimed = true
        )
        repository.updateStats(updated)
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
          backendBaseUrl = statsObj.optString("backendBaseUrl", "https://raito.hamdi.dev.et"),
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
    if (!canClaimDailyStreakBonus()) return
    viewModelScope.launch {
      val todayStr = DateUtils.formatYyyyMMdd()
      val reward = getXpReward("streak")
      repository.addPoints(reward)
      repository.updateStats(stats.value.copy(lastStreakClaimedDate = todayStr, dailyStreak = stats.value.dailyStreak + 1))
      _uiEvents.emit(UiEvent.ShowUndoSnackbar("Streak check-in bonus! Earned +$reward PTS!", -1))
    }
  }
}
