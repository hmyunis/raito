package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.ChapterEntity
import com.example.data.database.TaskEntity
import com.example.util.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaitoViewModel
import com.example.util.DateUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BucketsView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val chapters by viewModel.chapters.collectAsState()
  val tasks by viewModel.tasks.collectAsState()
  val stats by viewModel.stats.collectAsState()
  val selectedChapterId by viewModel.selectedChapterId.collectAsState()
  val chapterSearchQuery by viewModel.chapterSearchQuery.collectAsState()
  val chaptersLayoutMode by viewModel.bucketLayoutMode.collectAsState()
  val timeFormatMode by viewModel.timeFormatMode.collectAsState()
  val use24HourTime = timeFormatMode == "24"
  
  // Filter chapters based on search query
  val filteredChapters = remember(chapters, chapterSearchQuery) {
    if (chapterSearchQuery.isBlank()) chapters else {
      chapters.filter { it.name.contains(chapterSearchQuery, ignoreCase = true) || it.discipline.contains(chapterSearchQuery, ignoreCase = true) }
    }
  }

  var taskToDeleteConfirm by remember { mutableStateOf<TaskEntity?>(null) }

  // Find active chapter
  val activeChapter = remember(chapters, selectedChapterId) {
    chapters.find { it.id == selectedChapterId }
  }

  val chapterTasks = remember(tasks, activeChapter) {
    if (activeChapter != null) {
      tasks.filter { it.chapterId == activeChapter.id }
    } else {
      emptyList()
    }
  }

  var taskSortMode by remember { mutableStateOf("Default") }

  val sortedTasks = remember(chapterTasks, taskSortMode) {
    when (taskSortMode) {
      "Uncompleted" -> chapterTasks.sortedWith(
        compareByDescending<TaskEntity> { it.isPinned }
          .thenBy { it.isCompleted }
          .thenByDescending { it.createdAt ?: 0L }
      )
      "Deadline" -> chapterTasks.sortedWith(
        compareByDescending<TaskEntity> { it.isPinned }
          .thenBy { it.isCompleted }
          .thenBy { it.dueDatetime.isNullOrBlank() }
          .thenBy { it.dueDatetime ?: "" }
          .thenByDescending { it.createdAt ?: 0L }
      )
      else -> chapterTasks.sortedWith(
        compareByDescending<TaskEntity> { it.isPinned }
          .thenByDescending { it.createdAt ?: 0L }
      )
    }
  }

  // 16-bit console retro animated transitions
  val infiniteTransition = rememberInfiniteTransition(label = "chapter_details_animations")
  val shadowOffsetAnim by infiniteTransition.animateFloat(
    initialValue = 3f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shadow_offset"
  )
  val spriteBobbing by infiniteTransition.animateFloat(
    initialValue = -5f,
    targetValue = 5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sprite_bob"
  )
  val spriteScale by infiniteTransition.animateFloat(
    initialValue = 0.97f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sprite_scale"
  )

  var newTaskName by rememberSaveable(selectedChapterId) { mutableStateOf("") }
  var newTaskDescription by rememberSaveable(selectedChapterId) { mutableStateOf("") }
  var newTaskDueDatetime by rememberSaveable(selectedChapterId) { mutableStateOf("") }
  var showDetailsForm by rememberSaveable(selectedChapterId) { mutableStateOf(false) }
  var isQuickTaskInputFocused by remember { mutableStateOf(false) }
  var isSearchFocused by remember { mutableStateOf(false) }
  val searchFocusScale by animateFloatAsState(if (isSearchFocused) 1.01f else 1f, label = "search_focus_scale")
  val configuration = LocalConfiguration.current

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .starryTwinkleBackground()
  ) {
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || maxWidth >= 600.dp

    if (isLandscape) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // Left Column (Chapters Tabs, Companion, Info Details, reset chapter progress)
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          BucketBrowserControls(
            chapterSearchQuery = chapterSearchQuery,
            onSearchQueryChange = { viewModel.chapterSearchQuery.value = it },
            isSearchFocused = isSearchFocused,
            onSearchFocusChanged = { isSearchFocused = it },
            searchFocusScale = searchFocusScale,
            chaptersLayoutMode = chaptersLayoutMode,
            onLayoutModeChange = { viewModel.updateBucketLayoutMode(it) },
            onCreateBucketClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.NEW_CHAPTER) },
            compactLayout = true
          )

          if (chapters.isEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              EmptyStateCard(
                icon = Icons.Default.FolderOpen,
                title = "No Buckets Yet",
                message = "Create a bucket to organize related tasks and watch your progress build."
              )
              Button(
                onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.NEW_CHAPTER) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = RectangleShape,
                modifier = Modifier
                  .mangaBorder(width = 2.dp)
                  .mangaShadow(offset = 4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = "Add Icon", tint = MaterialTheme.colorScheme.onPrimary)
                  Text("CREATE NEW BUCKET", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          } else {
            Spacer(modifier = Modifier.height(4.dp))

            // Display Chapters list or grid
            if (chaptersLayoutMode == "List") {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                filteredChapters.forEach { chapter ->
                  val subTasks = tasks.filter { it.chapterId == chapter.id }
                  val compCount = subTasks.count { it.isCompleted }
                  val totCount = subTasks.size
                  ChapterCard(
                    chapter = chapter,
                    useBucketColoring = stats.bucketColoring,
                    totalTasks = totCount,
                    completedTasks = compCount,
                    isSelected = activeChapter?.id == chapter.id,
                    onSelect = { viewModel.selectedChapterId.value = chapter.id },
                    onLongClick = { viewModel.startEditingChapter(chapter) },
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            } else {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val chunked = filteredChapters.chunked(2)
                chunked.forEach { rowChapters ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    rowChapters.forEach { chapter ->
                      val subTasks = tasks.filter { it.chapterId == chapter.id }
                      val compCount = subTasks.count { it.isCompleted }
                      val totCount = subTasks.size
                      ChapterCard(
                        chapter = chapter,
                        useBucketColoring = stats.bucketColoring,
                        totalTasks = totCount,
                        completedTasks = compCount,
                        isSelected = activeChapter?.id == chapter.id,
                        onSelect = { viewModel.selectedChapterId.value = chapter.id },
                        onLongClick = { viewModel.startEditingChapter(chapter) },
                        modifier = Modifier.weight(1f)
                      )
                    }
                    if (rowChapters.size < 2) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }

            if (activeChapter != null) {
              val chapter = activeChapter!!
              val chibiMood = remember(chapter, chapterTasks) {
                ChibiSpeechBank.moodFor(chapter, chapterTasks)
              }
              val speechRenderKey = remember(chapter.id, chibiMood, chapterTasks.size, chapterTasks.count { it.isCompleted }, chapterTasks.count { it.isOverdue }) {
                System.nanoTime()
              }
              val chibiLine = remember(chapter, chapterTasks, chibiMood, speechRenderKey) {
                ChibiSpeechBank.lineFor(chapter.companionId, chibiMood, chapter, chapterTasks, speechRenderKey)
              }
              val chibiExpression = remember(chibiMood) { chibiMood.expressionKey() }
              val compUrl = remember(chapter.companionId, chibiExpression) {
                viewModel.getAvatarUrl(chapter.companionId, chibiExpression)
              }

              // Compact illustrative Companion Manga frame
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                if (!stats.silenceChibiComments) {
                  ChibiSpeechBubble(
                    text = chibiLine,
                    mood = chibiMood,
                    restartKey = speechRenderKey,
                    animateTypewriter = !isQuickTaskInputFocused,
                    modifier = Modifier.fillMaxWidth()
                  )
                }
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .mangaShadow(offset = shadowOffsetAnim.dp, shadowColor = InkBlack)
                    .background(MaterialTheme.colorScheme.surface)
                    .mangaBorder(width = 3.dp)
                    .screentonePattern(dotColor = InkBlack.copy(alpha = 0.08f)),
                  contentAlignment = Alignment.Center
                ) {
                  AsyncImage(
                    model = compUrl,
                    contentDescription = "Active Companion",
                    modifier = Modifier
                      .fillMaxSize(0.7f)
                      .graphicsLayer {
                        translationY = spriteBobbing
                        scaleX = spriteScale
                        scaleY = spriteScale
                      }
                  )

                  Box(
                    modifier = Modifier
                      .align(Alignment.BottomCenter)
                      .background(MaterialTheme.colorScheme.primary)
                      .padding(horizontal = 12.dp, vertical = 4.dp)
                      .clip(RectangleShape)
                  ) {
                    val statusStage = chapterProgressStageLabel(chapter, chapterTasks)
                    Text(
                      text = statusStage.uppercase(),
                      color = MaterialTheme.colorScheme.onPrimary,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp
                    )
                  }
                }
              }

              // Chapter header details
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .mangaShadow(offset = 3.dp)
                  .background(MaterialTheme.colorScheme.surface)
                  .mangaBorder()
                  .padding(12.dp)
              ) {
                Text(
                  text = activeChapter!!.name.uppercase(),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Discipline: ${activeChapter!!.discipline} | Companion: ${activeChapter!!.companionId}".uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.61f),
                  fontSize = 8.5.sp
                )
                
                if (!activeChapter!!.deadline.isNullOrEmpty()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Deadline: ${DateUtils.formatDateTimeStringForDisplay(activeChapter!!.deadline, use24HourTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AnimeRed,
                    fontSize = 8.5.sp
                  )
                }
              }

              Card(
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .mangaBorder(color = AnimeTeal)
                  .mangaShadow(offset = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  MangaSettingSwitchRow(
                    title = "Telegram Bucket Sync",
                    description = "Expose this bucket and its tasks to your linked Telegram bot.",
                    checked = activeChapter!!.telegramSyncEnabled,
                    onCheckedChange = { enabled ->
                      viewModel.updateChapterTelegramSync(activeChapter!!.id, enabled)
                    }
                  )
                }
              }

              // Reset chapter card
              Card(
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .mangaBorder(color = AnimeRed)
                  .mangaShadow(offset = 2.dp)
                  .clickable { viewModel.requestResetChapterProgress(activeChapter!!.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset chapter progress",
                    tint = AnimeRed,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "RESET CHAPTER PROGRESS",
                    color = AnimeRed,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Right Column (Chapter Tasks List and Fast Entry form)
        Column(
          modifier = Modifier
            .weight(1.2f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          if (activeChapter != null) {
            Text(
              text = "CHAPTER TASKS",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onBackground
            )

            // Dynamic sort buttons
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "SORT BY:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                val modes = listOf("Default" to "DEFAULT (RECENT)", "Uncompleted" to "UNCOMPLETED TOP", "Deadline" to "DEADLINE")
                modes.forEach { (modeKey, modeLabel) ->
                  val isSelected = taskSortMode == modeKey
                  val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                  val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                  val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                  val borderWidth = if (isSelected) 2.dp else 1.dp
                  
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .mangaShadow(offset = if (isSelected) 1.5.dp else 0.dp)
                      .background(bg)
                      .mangaBorder(width = borderWidth, color = borderCol)
                      .clickable { taskSortMode = modeKey }
                      .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = modeLabel,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = textCol,
                      fontSize = 8.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }

            if (sortedTasks.isEmpty()) {
              Text(
                text = "This chapter has no tasks yet. Add one below!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 12.dp),
                fontSize = 11.sp
              )
            } else {
              sortedTasks.forEach { task ->
                TaskItemRow(
                  task = task,
                  use24HourTime = use24HourTime,
                  onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                  onDeleteClick = { taskToDeleteConfirm = task },
                  onPinClick = { viewModel.toggleTaskPin(task) }
                )
              }
            }

            // Quick task entry forms
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .mangaShadow(offset = 3.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "ADD TASK TO THE CHAPTER",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                val canAddTask = newTaskName.trim().isNotEmpty()
                OutlinedTextField(
                  value = newTaskName,
                  onValueChange = { newTaskName = it },
                  placeholder = { Text("Enter task title...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp) },
                  modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isQuickTaskInputFocused = it.isFocused },
                  shape = RectangleShape,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                  ),
                  textStyle = MaterialTheme.typography.bodySmall
                )

                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .background(if (canAddTask) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .mangaBorder()
                    .clickable(enabled = canAddTask) {
                      val name = newTaskName.trim()
                      if (name.isNotEmpty()) {
                        val desc = newTaskDescription.trim().ifEmpty { null }
                        val due = newTaskDueDatetime.trim().ifEmpty { null }
                        viewModel.addTaskToChapter(activeChapter.id, name, description = desc, dueDatetime = due)
                        newTaskName = ""
                        newTaskDescription = ""
                        newTaskDueDatetime = ""
                        showDetailsForm = false
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task",
                    tint = if (canAddTask) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showDetailsForm = !showDetailsForm }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = if (showDetailsForm) "HIDE ADDITIONAL DETAILS" else "ADD ADDITIONAL DETAILS (DESCRIPTION, DUE DATETIME)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 8.5.sp
                )
                Icon(
                  imageVector = if (showDetailsForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                  contentDescription = "Toggle details form",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
              }

              if (showDetailsForm) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedTextField(
                  value = newTaskDescription,
                  onValueChange = { newTaskDescription = it },
                  placeholder = { Text("Task description...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp) },
                    modifier = Modifier
                      .fillMaxWidth()
                      .onFocusChanged { isQuickTaskInputFocused = it.isFocused },
                    shape = RectangleShape,
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = MaterialTheme.colorScheme.onSurface,
                      unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                      focusedBorderColor = MaterialTheme.colorScheme.primary,
                      unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                      focusedContainerColor = MaterialTheme.colorScheme.surface,
                      unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                  )

                  TaskDueDateTimePicker(
                    value = newTaskDueDatetime,
                    onValueChange = { newTaskDueDatetime = it },
                    use24HourTime = use24HourTime,
                    compact = true
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
          .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

    if (activeChapter == null) {
      if (chapters.isEmpty()) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          EmptyStateCard(
            icon = Icons.Default.FolderOpen,
            title = "No Buckets Yet",
            message = "Create a bucket to organize related tasks and watch your progress build."
          )
          Button(
            onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.NEW_CHAPTER) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RectangleShape,
            modifier = Modifier
              .mangaBorder(width = 2.dp)
              .mangaShadow(offset = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Icon", tint = MaterialTheme.colorScheme.onPrimary)
              Text("CREATE NEW BUCKET", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    } else {
      item {
        BucketBrowserControls(
          chapterSearchQuery = chapterSearchQuery,
          onSearchQueryChange = { viewModel.chapterSearchQuery.value = it },
          isSearchFocused = isSearchFocused,
          onSearchFocusChanged = { isSearchFocused = it },
          searchFocusScale = searchFocusScale,
          chaptersLayoutMode = chaptersLayoutMode,
          onLayoutModeChange = { viewModel.updateBucketLayoutMode(it) },
          onCreateBucketClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.NEW_CHAPTER) },
          compactLayout = false
        )
      }

      item {
        Spacer(modifier = Modifier.height(4.dp))
      }

          item {
            if (chaptersLayoutMode == "List") {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                filteredChapters.forEach { chapter ->
                  val subTasks = tasks.filter { it.chapterId == chapter.id }
                  val compCount = subTasks.count { it.isCompleted }
                  val totCount = subTasks.size
                  ChapterCard(
                    chapter = chapter,
                    useBucketColoring = stats.bucketColoring,
                    totalTasks = totCount,
                    completedTasks = compCount,
                    isSelected = activeChapter?.id == chapter.id,
                    onSelect = { viewModel.selectedChapterId.value = chapter.id },
                    onLongClick = { viewModel.startEditingChapter(chapter) },
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            } else {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val chunked = filteredChapters.chunked(2)
                chunked.forEach { rowChapters ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    rowChapters.forEach { chapter ->
                      val subTasks = tasks.filter { it.chapterId == chapter.id }
                      val compCount = subTasks.count { it.isCompleted }
                      val totCount = subTasks.size
                      ChapterCard(
                        chapter = chapter,
                        useBucketColoring = stats.bucketColoring,
                        totalTasks = totCount,
                        completedTasks = compCount,
                        isSelected = activeChapter?.id == chapter.id,
                        onSelect = { viewModel.selectedChapterId.value = chapter.id },
                        onLongClick = { viewModel.startEditingChapter(chapter) },
                        modifier = Modifier.weight(1f)
                      )
                    }
                    if (rowChapters.size < 2) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (activeChapter != null) {
        item {
           // Back button
           Button(
             onClick = { viewModel.selectedChapterId.value = null },
             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
             shape = RectangleShape,
             modifier = Modifier
               .fillMaxWidth()
               .mangaBorder()
               .mangaShadow(offset = 2.dp)
           ) {
             Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
               Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
               Text("BACK TO BUCKETS", fontWeight = FontWeight.Black)
             }
           }
        }

        val chapter = activeChapter!!
        val chapterTasks = tasks.filter { it.chapterId == chapter.id }

      // Large illustrative Companion Manga frame
      item {
        val chibiMood = remember(chapter, chapterTasks) {
          ChibiSpeechBank.moodFor(chapter, chapterTasks)
        }
        val speechRenderKey = remember(chapter.id, chibiMood, chapterTasks.size, chapterTasks.count { it.isCompleted }, chapterTasks.count { it.isOverdue }) {
          System.nanoTime()
        }
        val chibiLine = remember(chapter, chapterTasks, chibiMood, speechRenderKey) {
          ChibiSpeechBank.lineFor(chapter.companionId, chibiMood, chapter, chapterTasks, speechRenderKey)
        }
        val chibiExpression = remember(chibiMood) { chibiMood.expressionKey() }
        val compUrl = remember(chapter.companionId, chibiExpression) {
          viewModel.getAvatarUrl(chapter.companionId, chibiExpression)
        }
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (!stats.silenceChibiComments) {
            ChibiSpeechBubble(
              text = chibiLine,
              mood = chibiMood,
              restartKey = speechRenderKey,
              animateTypewriter = !isQuickTaskInputFocused,
              modifier = Modifier.fillMaxWidth()
            )
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(1.2f)
              .mangaShadow(offset = shadowOffsetAnim.dp, shadowColor = InkBlack)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder(width = 3.dp)
              .screentonePattern(dotColor = InkBlack.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
          ) {
            // Large pixel companion silhouette representation (Animated retro-style talk sprite)
            AsyncImage(
              model = compUrl,
              contentDescription = "Active Companion",
              modifier = Modifier
                .fillMaxSize(0.7f)
                .graphicsLayer {
                  translationY = spriteBobbing
                  scaleX = spriteScale
                  scaleY = spriteScale
                }
            )

            // Indicator bar overlay
            Box(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RectangleShape)
            ) {
              val statusStage = chapterProgressStageLabel(chapter, chapterTasks)
              Text(
                text = statusStage.uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Chapter header details
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .mangaShadow(offset = 3.dp)
            .background(MaterialTheme.colorScheme.surface)
            .mangaBorder()
            .padding(16.dp)
        ) {
          Text(
            text = activeChapter!!.name.uppercase(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Discipline: ${activeChapter!!.discipline} | Companion: ${activeChapter!!.companionId}".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.61f)
          )
          
          if (!activeChapter!!.deadline.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Deadline: ${DateUtils.formatDateTimeStringForDisplay(activeChapter!!.deadline, use24HourTime)}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = AnimeRed
            )
          }
        }
      }

      // Tasks List Section
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "CHAPTER TASKS",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
          )

          // Visually striking sort & filter buttons
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "SORT BY:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              val modes = listOf("Default" to "DEFAULT (RECENT)", "Uncompleted" to "UNCOMPLETED TOP", "Deadline" to "DEADLINE")
              modes.forEach { (modeKey, modeLabel) ->
                val isSelected = taskSortMode == modeKey
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                val borderWidth = if (isSelected) 2.dp else 1.dp
                
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .mangaShadow(offset = if (isSelected) 1.5.dp else 0.dp)
                    .background(bg)
                    .mangaBorder(width = borderWidth, color = borderCol)
                    .clickable { taskSortMode = modeKey }
                    .padding(vertical = 8.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = textCol,
                    fontSize = 8.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

      val sortedTasks = when (taskSortMode) {
        "Uncompleted" -> chapterTasks.sortedWith(
          compareByDescending<TaskEntity> { it.isPinned }
            .thenBy { it.isCompleted }
            .thenByDescending { it.createdAt ?: 0L }
        )
        "Deadline" -> chapterTasks.sortedWith(
          compareByDescending<TaskEntity> { it.isPinned }
            .thenBy { it.isCompleted }
            .thenBy { it.dueDatetime.isNullOrBlank() }
            .thenBy { it.dueDatetime ?: "" }
            .thenByDescending { it.createdAt ?: 0L }
        )
        else -> chapterTasks.sortedWith(
          compareByDescending<TaskEntity> { it.isPinned }
            .thenByDescending { it.createdAt ?: 0L }
        )
      }

      if (sortedTasks.isEmpty()) {
        item {
          Text(
            text = "This chapter has no tasks yet. Add one below!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 12.dp)
          )
        }
      } else {
        items(sortedTasks) { task ->
          TaskItemRow(
            task = task,
            use24HourTime = use24HourTime,
            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
            onDeleteClick = { taskToDeleteConfirm = task },
            onPinClick = { viewModel.toggleTaskPin(task) }
          )
        }
      }

      // Text input to add tasks dynamically
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .mangaShadow(offset = 3.dp)
            .background(MaterialTheme.colorScheme.surface)
            .mangaBorder()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "ADD TASK TO THE CHAPTER",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val canAddTask = newTaskName.trim().isNotEmpty()
            OutlinedTextField(
              value = newTaskName,
              onValueChange = { newTaskName = it },
              placeholder = { Text("Enter task title...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
              modifier = Modifier
                .weight(1f)
                .onFocusChanged { isQuickTaskInputFocused = it.isFocused },
              shape = RectangleShape,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
              ),
              textStyle = MaterialTheme.typography.bodyMedium
            )

            Box(
              modifier = Modifier
                .size(54.dp)
                .background(if (canAddTask) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .mangaBorder()
                .clickable(enabled = canAddTask) {
                  val name = newTaskName.trim()
                  if (name.isNotEmpty()) {
                    val desc = newTaskDescription.trim().ifEmpty { null }
                    val due = newTaskDueDatetime.trim().ifEmpty { null }
                    viewModel.addTaskToChapter(activeChapter!!.id, name, description = desc, dueDatetime = due)
                    newTaskName = ""
                    newTaskDescription = ""
                    newTaskDueDatetime = ""
                    showDetailsForm = false
                  }
                },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add task",
                tint = if (canAddTask) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
              )
            }
          }

          // Optional details toggle
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showDetailsForm = !showDetailsForm }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = if (showDetailsForm) "HIDE ADDITIONAL DETAILS" else "ADD ADDITIONAL DETAILS (DESCRIPTION, DUE DATETIME)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary
            )
            Icon(
              imageVector = if (showDetailsForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = "Toggle details form",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
          }

          if (showDetailsForm) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = newTaskDescription,
                onValueChange = { newTaskDescription = it },
                placeholder = { Text("Task description...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isQuickTaskInputFocused = it.isFocused },
                shape = RectangleShape,
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = MaterialTheme.colorScheme.onSurface,
                  unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                textStyle = MaterialTheme.typography.bodySmall
              )

              TaskDueDateTimePicker(
                value = newTaskDueDatetime,
                onValueChange = { newTaskDueDatetime = it },
                use24HourTime = use24HourTime
              )
            }
          }
        }
      }

      // Action Reset Chapter Progress Button
      item {
        Card(
          shape = RectangleShape,
          modifier = Modifier
            .fillMaxWidth()
            .mangaBorder(color = AnimeTeal),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            MangaSettingSwitchRow(
              title = "Telegram Bucket Sync",
              description = "Expose this bucket and its tasks to your linked Telegram bot.",
              checked = activeChapter!!.telegramSyncEnabled,
              onCheckedChange = { enabled ->
                viewModel.updateChapterTelegramSync(activeChapter!!.id, enabled)
              }
            )
          }
        }
      }

      // Action Reset Chapter Progress Button
      item {
        Card(
          shape = RectangleShape,
          modifier = Modifier
            .fillMaxWidth()
            .mangaBorder()
            .clickable { viewModel.requestResetChapterProgress(activeChapter!!.id) },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Reset chapter progress",
              tint = AnimeRed,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "RESET CHAPTER PROGRESS",
              color = AnimeRed,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    } // closes if (activeChapter != null)

  item { Spacer(modifier = Modifier.height(24.dp)) }
} // closes LazyColumn
    } // closes isLandscape else block
  } // closes BoxWithConstraints

  if (taskToDeleteConfirm != null) {
    DeleteConfirmationDialog(
      title = "DELETE TASK?",
      message = "Are you sure you want to permanently delete \"${taskToDeleteConfirm!!.name.uppercase()}\"? This action cannot be undone.",
      onConfirm = {
        viewModel.deleteTask(taskToDeleteConfirm!!.id)
        taskToDeleteConfirm = null
      },
      onDismiss = {
        taskToDeleteConfirm = null
      }
    )
  }
}

@Composable
private fun BucketBrowserControls(
  chapterSearchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  isSearchFocused: Boolean,
  onSearchFocusChanged: (Boolean) -> Unit,
  searchFocusScale: Float,
  chaptersLayoutMode: String,
  onLayoutModeChange: (String) -> Unit,
  onCreateBucketClick: () -> Unit,
  compactLayout: Boolean,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onCreateBucketClick,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RectangleShape,
        modifier = Modifier
          .mangaBorder(width = 1.5.dp)
          .mangaShadow(offset = 2.dp),
        contentPadding = if (compactLayout) {
          PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        } else {
          PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Icon", modifier = Modifier.size(if (compactLayout) 14.dp else 16.dp))
          Text(
            "CREATE NEW BUCKET",
            fontWeight = FontWeight.Black,
            fontSize = if (compactLayout) 10.sp else 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.width(2.dp))

      LayoutToggleButton(
        isSelected = chaptersLayoutMode == "List",
        onClick = { onLayoutModeChange("List") },
        icon = {
          Icon(
            imageVector = Icons.Default.List,
            contentDescription = "List View",
            tint = if (chaptersLayoutMode == "List") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
          )
        }
      )

      LayoutToggleButton(
        isSelected = chaptersLayoutMode == "Grid",
        onClick = { onLayoutModeChange("Grid") },
        icon = {
          Column(
            modifier = Modifier.size(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            val squareTint = if (chaptersLayoutMode == "Grid") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Row(
              modifier = Modifier.fillMaxWidth().weight(1f),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Box(modifier = Modifier.size(7.dp).background(squareTint))
              Box(modifier = Modifier.size(7.dp).background(squareTint))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth().weight(1f),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Box(modifier = Modifier.size(7.dp).background(squareTint))
              Box(modifier = Modifier.size(7.dp).background(squareTint))
            }
          }
        }
      )
    }

    OutlinedTextField(
      value = chapterSearchQuery,
      onValueChange = onSearchQueryChange,
      placeholder = {
        Text(
          "SEARCH BUCKETS...",
          fontSize = if (compactLayout) 9.sp else 10.sp,
          fontWeight = FontWeight.Black
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = searchFocusScale
          scaleY = searchFocusScale
        }
        .onFocusChanged { onSearchFocusChanged(it.isFocused) }
        .mangaShadow(offset = if (isSearchFocused) 3.dp else 1.dp),
      shape = RectangleShape,
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
      ),
      textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
      trailingIcon = {
        if (chapterSearchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchQueryChange("") }) {
            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
          }
        }
      }
    )
  }
}

@Composable
private fun LayoutToggleButton(
  isSelected: Boolean,
  onClick: () -> Unit,
  icon: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(36.dp)
      .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
      .mangaBorder(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
      )
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    icon()
  }
}

private fun chapterProgressStageLabel(
  chapter: ChapterEntity,
  tasks: List<TaskEntity>
): String {
  if (chapter.isCompleted || (tasks.isNotEmpty() && tasks.all { it.isCompleted })) {
    return "Fully Inked & Colored"
  }

  if (tasks.isEmpty()) {
    return "Stage: Blank Canvas"
  }

  val completedCount = tasks.count { it.isCompleted }
  val progressPercent = completedCount * 100 / tasks.size
  return when {
    progressPercent >= 75 -> "Stage: Final Inks"
    progressPercent >= 40 -> "Stage: Base Colors"
    progressPercent > 0 -> "Stage: Line Art Progress"
    else -> "Stage: Rough Sketch"
  }
}

@Composable
private fun TaskDueDateTimePicker(
  value: String,
  onValueChange: (String) -> Unit,
  use24HourTime: Boolean,
  compact: Boolean = false
) {
  val context = LocalContext.current
  val parts = remember(value) { value.split(" ") }
  val selectedDateStr = remember(value) { parts.firstOrNull { it.contains("-") } }
  val selectedTimeStr = remember(value) { parts.firstOrNull { it.contains(":") } }

  fun updateDueDatetime(date: String?, time: String?) {
    onValueChange(
      when {
        date != null && time != null -> "$date $time"
        date != null -> date
        time != null -> time
        else -> ""
      }
    )
  }

  val displayDate = remember(selectedDateStr) {
    selectedDateStr?.let { DateUtils.formatIsoToMmmDdYyyy(it).uppercase() } ?: "NOT SET"
  }
  val displayTime = remember(selectedTimeStr, use24HourTime) {
    selectedTimeStr?.let { DateUtils.formatTimeForDisplay(it, use24HourTime) } ?: "NOT SET"
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "DUE DATE & TIME",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
      fontSize = if (compact) 8.5.sp else 10.sp
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      DueDateTimeSelectorBox(
        label = "DATE",
        value = displayDate,
        isSet = selectedDateStr != null,
        icon = Icons.Default.DateRange,
        compact = compact,
        modifier = Modifier.weight(1f),
        onClick = {
          val calendar = java.util.Calendar.getInstance()
          val dateParts = selectedDateStr?.split("-")
          if (dateParts != null && dateParts.size == 3) {
            runCatching {
              calendar.set(java.util.Calendar.YEAR, dateParts[0].toInt())
              calendar.set(java.util.Calendar.MONTH, dateParts[1].toInt() - 1)
              calendar.set(java.util.Calendar.DAY_OF_MONTH, dateParts[2].toInt())
            }
          }
          android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
              val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
              updateDueDatetime(formattedDate, selectedTimeStr)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
          ).show()
        }
      )

      DueDateTimeSelectorBox(
        label = "TIME",
        value = displayTime,
        isSet = selectedTimeStr != null,
        icon = Icons.Default.Timer,
        compact = compact,
        modifier = Modifier.weight(1f),
        onClick = {
          val calendar = java.util.Calendar.getInstance()
          val timeParts = selectedTimeStr?.split(":")
          if (timeParts != null && timeParts.size == 2) {
            runCatching {
              calendar.set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
              calendar.set(java.util.Calendar.MINUTE, timeParts[1].toInt())
            }
          }
          android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
              val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
              updateDueDatetime(selectedDateStr, formattedTime)
            },
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            use24HourTime
          ).show()
        }
      )
    }

    if (selectedDateStr != null || selectedTimeStr != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { updateDueDatetime(null, null) }
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Clear due date and time",
          tint = AnimeRed,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "CLEAR DUE DATE / TIME",
          style = MaterialTheme.typography.labelSmall,
          color = AnimeRed,
          fontWeight = FontWeight.Bold,
          fontSize = 8.5.sp
        )
      }
    }
  }
}

@Composable
private fun DueDateTimeSelectorBox(
  label: String,
  value: String,
  isSet: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  compact: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Column(
    modifier = modifier
      .mangaShadow(offset = 2.dp)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder(width = 1.5.dp)
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = if (compact) 8.dp else 10.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      imageVector = icon,
      contentDescription = "Pick $label",
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(if (compact) 18.dp else 20.dp)
    )
    Spacer(modifier = Modifier.height(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
      fontSize = if (compact) 8.sp else 9.sp
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Black,
      color = if (isSet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
      fontSize = if (compact) 9.sp else 10.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun TaskItemRow(
  task: TaskEntity,
  use24HourTime: Boolean,
  onCheckedChange: () -> Unit,
  onDeleteClick: () -> Unit,
  onPinClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  val formattedCreated = remember(task.createdAt, use24HourTime) {
    val time = task.createdAt ?: System.currentTimeMillis()
    DateUtils.formatMmmDdYyyyHHmm(java.util.Date(time), use24HourTime)
  }
  val formattedDue = remember(task.dueDatetime, use24HourTime) {
    DateUtils.formatDateTimeStringForDisplay(task.dueDatetime, use24HourTime)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = 2.dp)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder(width = 1.dp, color = MaterialTheme.colorScheme.onBackground)
      .clickable { isExpanded = !isExpanded }
      .padding(horizontal = 12.dp, vertical = 12.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Manga ink style customized checkbox
        val checkScale by animateFloatAsState(
          targetValue = if (task.isCompleted) 1f else 0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
          ),
          label = "checkmark_scale"
        )
        Box(
          modifier = Modifier
            .size(24.dp)
            .mangaBorder(width = 2.dp, color = MaterialTheme.colorScheme.onBackground)
            .background(if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable { onCheckedChange() },
          contentAlignment = Alignment.Center
        ) {
          if (checkScale > 0.01f) {
            Text(
              text = "✓",
              color = MaterialTheme.colorScheme.onPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              modifier = Modifier.graphicsLayer(scaleX = checkScale, scaleY = checkScale)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = task.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
          )
          
          if (!isExpanded && (!task.description.isNullOrEmpty() || !task.dueDatetime.isNullOrEmpty())) {
            Text(
              text = "Tap to view details • " + if (!task.dueDatetime.isNullOrEmpty()) "Due: $formattedDue" else "Has description",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
          imageVector = Icons.Default.PushPin,
          contentDescription = "Pin/Unpin task",
          tint = if (task.isPinned) AnimeYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
          modifier = Modifier
            .size(18.dp)
            .clickable { onPinClick() }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = "Toggle task details",
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete task",
          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
          modifier = Modifier
            .size(22.dp)
            .clickable { onDeleteClick() }
        )
      }

      if (isExpanded) {
        Spacer(modifier = Modifier.height(10.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Create Details Row
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Description
          if (!task.description.isNullOrEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                  shape = RectangleShape
                )
                .padding(10.dp)
            ) {
              Text(
                text = "DESCRIPTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.sp,
                letterSpacing = 1.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp
              )
            }
          }

          // Due Time and Created At Time
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Created At Capsule
            Box(
              modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .mangaBorder(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Column {
                Text(
                  text = "CREATED AT",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold,
                  fontSize = 8.sp
                )
                Text(
                  text = formattedCreated,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp
                )
              }
            }

            // Due Date & Time Capsule (if present)
            if (!task.dueDatetime.isNullOrEmpty()) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(AnimeRed.copy(alpha = 0.08f))
                  .mangaBorder(width = 1.dp, color = AnimeRed)
                  .padding(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Column {
                  Text(
                    text = "DUE DATETIME",
                    style = MaterialTheme.typography.labelSmall,
                    color = AnimeRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                  )
                  Text(
                    text = formattedDue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// Utility mapper
private fun mutableStateFlowOf(value: String): MutableState<String> {
  return mutableStateOf(value)
}

private fun ChibiMood.expressionKey(): String {
  return when (this) {
    ChibiMood.Neutral -> "neutral"
    ChibiMood.Happy -> "happy"
    ChibiMood.Focus -> "focus"
    ChibiMood.Sad -> "sad"
    ChibiMood.Completed -> "completed"
  }
}

@Composable
private fun ChibiSpeechBubble(
  text: String,
  mood: ChibiMood,
  restartKey: Long,
  animateTypewriter: Boolean,
  modifier: Modifier = Modifier
) {
  var displayedText by remember(text, restartKey) { mutableStateOf("") }
  val accent = when (mood) {
    ChibiMood.Neutral -> MaterialTheme.colorScheme.primary
    ChibiMood.Happy -> AnimeGreen
    ChibiMood.Focus -> AnimeTeal
    ChibiMood.Sad -> AnimeRed
    ChibiMood.Completed -> AnimeYellow
  }

  LaunchedEffect(text, restartKey, animateTypewriter) {
    if (!animateTypewriter) {
      displayedText = text
    } else {
      displayedText = ""
      delay(350)
      text.indices.forEach { index ->
        displayedText = text.take(index + 1)
        delay(8)
      }
    }
  }

  Box(
    modifier = modifier
      .padding(end = 4.dp, bottom = 2.dp)
      .mangaShadow(offset = 3.dp, shadowColor = MaterialTheme.colorScheme.onBackground)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder(width = 2.dp, color = MaterialTheme.colorScheme.onBackground)
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .padding(top = 3.dp)
          .size(10.dp)
          .background(accent)
      )
      Box(modifier = Modifier.weight(1f)) {
        Text(
          text = text,
          color = Color.Transparent,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 18.sp
        )
        Text(
          text = displayedText.ifEmpty { " " },
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 18.sp
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterCard(
  chapter: ChapterEntity,
  useBucketColoring: Boolean,
  totalTasks: Int,
  completedTasks: Int,
  isSelected: Boolean, // Note: no longer used for selection styling as we navigate to detail
  onSelect: () -> Unit,
  onLongClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val pct = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
  val defaultBadgeColor = MaterialTheme.colorScheme.primary
  val badgeColor = remember(chapter.auraInk, useBucketColoring, defaultBadgeColor) {
    resolveChapterAccentColor(chapter, useBucketColoring, defaultBadgeColor)
  }

  val compUrl = remember(chapter.companionId) {
    CompanionRegistry.getExpressions(chapter.companionId).neutral
  }

  Box(
    modifier = modifier
      .mangaShadow(offset = 4.dp, shadowColor = MaterialTheme.colorScheme.onBackground)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder(width = 2.dp, color = MaterialTheme.colorScheme.onBackground)
      .combinedClickable(
        onClick = { onSelect() },
        onLongClick = { onLongClick() }
      )
      .padding(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        // Top tag row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          // Discipline tag
          Box(
            modifier = Modifier
              .background(MaterialTheme.colorScheme.onBackground)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = chapter.discipline.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.background
            )
          }

          if (chapter.telegramSyncEnabled) {
            Box(
              modifier = Modifier
                .background(AnimeTeal)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "SYNC",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
              )
            }
          }

          // Overdue / Critical Tag
          if (!chapter.deadline.isNullOrEmpty()) {
            Box(
              modifier = Modifier
                .background(AnimeRed)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "CRITICAL",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = chapter.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Text(
          text = "$completedTasks / $totalTasks Tasks Cleared",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress stats
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "$pct% INKED",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = badgeColor,
            fontSize = 11.sp
          )

          val dueCount = totalTasks - completedTasks
          if (dueCount > 0 && !chapter.deadline.isNullOrEmpty()) {
            Text(
              text = "$dueCount Overdue",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = AnimeRed,
              fontSize = 10.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Chunky Progress Bar
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .mangaBorder(width = 1.5.dp, color = MaterialTheme.colorScheme.onBackground)
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(if (totalTasks == 0) 0f else pct / 100f)
              .background(badgeColor)
              .border(width = 1.5.dp, color = MaterialTheme.colorScheme.onBackground, shape = RectangleShape)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Companion Thumbnail Frame (Now on the right as per request)
      Box(
        modifier = Modifier
          .size(64.dp)
          .mangaBorder(width = 1.5.dp, color = MaterialTheme.colorScheme.onBackground)
          .background(MaterialTheme.colorScheme.background)
          .padding(4.dp),
        contentAlignment = Alignment.Center
      ) {
        AsyncImage(
          model = compUrl,
          contentDescription = "Companion Thumbnail",
          modifier = Modifier.fillMaxSize(),
          contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
      }
    }
  }
}
