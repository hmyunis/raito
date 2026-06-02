package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.TaskEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaitoViewModel
import kotlinx.coroutines.delay

@Composable
fun FocusView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val timerSecondsLeft by viewModel.timerSecondsLeft.collectAsState()
  val timerDurationMinutes by viewModel.timerDurationMinutes.collectAsState()
  val isTimerRunning by viewModel.isTimerRunning.collectAsState()
  val activeTask by viewModel.activeTask.collectAsState()
  val tasks by viewModel.tasks.collectAsState()
  val chapters by viewModel.chapters.collectAsState()

  val showPenDipAnimation by viewModel.showPenDipAnimation.collectAsState()
  val showInkDropAnimation by viewModel.showInkDropAnimation.collectAsState()
  val configuration = LocalConfiguration.current

  val uncompletedTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
  
  var selectedChapterForFocus by remember { mutableStateOf<com.example.data.database.ChapterEntity?>(null) }
  var showChapterDropdown by remember { mutableStateOf(false) }
  var showTaskSelectorDropdown by remember { mutableStateOf(false) }

  var customHoursInput by remember { mutableStateOf("") }
  var customMinutesInput by remember { mutableStateOf("") }

  val incompleteTasksInSelectedChapter = remember(tasks, selectedChapterForFocus) {
    if (selectedChapterForFocus == null) {
      emptyList()
    } else {
      tasks.filter { it.chapterId == selectedChapterForFocus!!.id && !it.isCompleted }
    }
  }

  LaunchedEffect(activeTask, chapters) {
    if (activeTask != null && selectedChapterForFocus == null) {
      val defaultChapterID = activeTask!!.chapterId
      selectedChapterForFocus = chapters.find { it.id == defaultChapterID }
    }
  }

  // Format Helper: ss -> mm:ss
  val minutesPart = timerSecondsLeft / 60
  val secondsPart = timerSecondsLeft % 60
  val timeDisplayText = String.format("%02d:%02d", minutesPart, secondsPart)

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.TopCenter
  ) {
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.screenWidthDp > 600

    if (isLandscape) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Column: Timer Progress Ring & State Badge
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          Box(
            modifier = Modifier
              .size(200.dp)
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawCircle(
                color = InkGrayLight,
                radius = size.minDimension / 2.3f,
                style = Stroke(
                  width = 12f,
                  cap = StrokeCap.Round
                )
              )
            }

            val angleProgress = if (timerDurationMinutes > 0) {
              (timerSecondsLeft.toFloat() / (timerDurationMinutes * 60).toFloat()) * 360f
            } else {
              360f
            }
            val animatedAngleProgress by animateFloatAsState(
              targetValue = angleProgress,
              animationSpec = tween(500, easing = LinearEasing),
              label = "angle_progress"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
              val padding = 18f
              val diameter = size.minDimension - (padding * 2f)
              val topLeftOffset = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
              )
              drawArc(
                color = AnimePurple,
                startAngle = -90f,
                sweepAngle = animatedAngleProgress,
                useCenter = false,
                topLeft = topLeftOffset,
                size = Size(diameter, diameter),
                style = Stroke(
                  width = 18f,
                  cap = StrokeCap.Square
                )
              )
            }

            Box(
              modifier = Modifier
                .size(110.dp)
                .mangaShadow(offset = 3.dp)
                .background(MaterialTheme.colorScheme.background)
                .mangaBorder(width = 3.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = timeDisplayText,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
              )
            }

            if (showPenDipAnimation) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  AsyncImage(
                    model = viewModel.getActiveAvatarUrl("focus"),
                    contentDescription = "User avatar dip loading",
                    modifier = Modifier
                      .size(64.dp)
                      .rotate(-15f)
                      .mangaBorder()
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "DIPPING PEN...",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp
                  )
                }
              }
            }

            if (showInkDropAnimation) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .mangaBorder()
                  .background(InkBlack),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "SPLASH!",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                    color = Color.White
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "TASK INKED!",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnimeYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Box(
            modifier = Modifier
              .background(InkBlack)
              .padding(horizontal = 16.dp, vertical = 6.dp)
          ) {
            val focusStageLabel = if (isTimerRunning) "FOCUS ON WORK" else "REST / PREPARE SESSION"
            Text(
              text = focusStageLabel.uppercase(),
              color = Color.White,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp,
              fontSize = 10.sp
            )
          }

          val stats by viewModel.stats.collectAsState()
          val reward = viewModel.getXpReward("focus")
          Text(
            text = "YIELDS: +$reward PTS (${stats.difficulty.uppercase()} PACING)",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp)
          )

          Spacer(modifier = Modifier.height(12.dp))
        }

        // Right Column: Settings & Forms
        Column(
          modifier = Modifier
            .weight(1.2f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          // 3. Dropdown Selection Panel: Bucket first, then Tasks inside that bucket (disabled if no bucket selected)
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Dropdown 1: Pick Bucket Chapter
            Box(modifier = Modifier.fillMaxWidth()) {
              Card(
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .mangaBorder()
                  .mangaShadow(offset = 2.dp)
                  .clickable { showChapterDropdown = !showChapterDropdown },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  val accentColor = when (selectedChapterForFocus?.auraInk) {
                    "Red" -> AnimeRed
                    "Teal" -> AnimeTeal
                    "Purple" -> AnimePurple
                    "Pink" -> AnimePink
                    "Black" -> MaterialTheme.colorScheme.onBackground
                    else -> InkGrayLight
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxHeight()
                      .width(10.dp)
                      .background(accentColor)
                      .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface)
                  )

                  Row(
                    modifier = Modifier
                      .weight(1f)
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "SELECT BUCKET CHAPTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = selectedChapterForFocus?.name?.uppercase() ?: "CHOOSE A BUCKET...",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    Text(
                      text = if (showChapterDropdown) "▲" else "▼",
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    )
                  }
                }
              }

              DropdownMenu(
                expanded = showChapterDropdown,
                onDismissRequest = { showChapterDropdown = false },
                modifier = Modifier
                  .fillMaxWidth(0.9f)
                  .mangaBorder()
                  .background(MaterialTheme.colorScheme.surface)
              ) {
                if (chapters.isEmpty()) {
                  DropdownMenuItem(
                    text = { Text("Create a bucket first", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { showChapterDropdown = false }
                  )
                } else {
                  chapters.forEach { chapter ->
                    val chapterColor = when (chapter.auraInk) {
                      "Red" -> AnimeRed
                      "Teal" -> AnimeTeal
                      "Purple" -> AnimePurple
                      "Pink" -> AnimePink
                      "Black" -> MaterialTheme.colorScheme.onBackground
                      else -> InkGrayLight
                    }
                    DropdownMenuItem(
                      text = {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(width = 8.dp, height = 18.dp)
                              .background(chapterColor)
                              .border(0.5.dp, MaterialTheme.colorScheme.onSurface)
                          )
                          Text(chapter.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                      },
                      onClick = {
                        selectedChapterForFocus = chapter
                        showChapterDropdown = false
                      }
                    )
                  }
                }
              }
            }

            // Dropdown 2: Pick Incomplete Task inside the picked bucket (disabled if no bucket selected)
            val isTaskSelectionEnabled = selectedChapterForFocus != null
            Box(modifier = Modifier.fillMaxWidth()) {
              Card(
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .mangaBorder(
                    width = if (isTaskSelectionEnabled) 2.dp else 1.dp,
                    color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                  )
                  .mangaShadow(offset = if (isTaskSelectionEnabled) 2.dp else 0.dp)
                  .clickable(enabled = isTaskSelectionEnabled) {
                    showTaskSelectorDropdown = !showTaskSelectorDropdown
                  },
                colors = CardDefaults.cardColors(
                  containerColor = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = if (isTaskSelectionEnabled) "WORKING ON" else "WORKING ON (CHOOSE BUCKET FIRST)",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val taskLabelText = if (!isTaskSelectionEnabled) {
                      "SELECT BUCKET FIRST"
                    } else {
                      activeTask?.name ?: "NONE / REST BREAK"
                    }
                    Text(
                      text = taskLabelText,
                      style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                      color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Text(
                    text = if (showTaskSelectorDropdown) "▲" else "▼",
                    color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  )
                }
              }

              if (isTaskSelectionEnabled) {
                DropdownMenu(
                  expanded = showTaskSelectorDropdown,
                  onDismissRequest = { showTaskSelectorDropdown = false },
                  modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .mangaBorder()
                    .background(MaterialTheme.colorScheme.surface)
                ) {
                  DropdownMenuItem(
                    text = { Text("NONE / REST BREAK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                      viewModel.setActiveTask(TaskEntity(chapterId = 0, name = "Rest break module", isCompleted = false))
                      showTaskSelectorDropdown = false
                    }
                  )
                  incompleteTasksInSelectedChapter.forEach { task ->
                    DropdownMenuItem(
                      text = { Text(task.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                      onClick = {
                        viewModel.setActiveTask(task)
                        showTaskSelectorDropdown = false
                      }
                    )
                  }
                }
              }
            }
          }

          // 4. Timer Controls row (Reset, Play/Pause, Skip)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .clickable { viewModel.resetTimer() },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "⟲",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Button(
              onClick = {
                if (isTimerRunning) viewModel.pauseTimer() else viewModel.startTimer()
              },
              shape = RectangleShape,
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .mangaShadow(offset = 2.dp)
                .mangaBorder(),
              colors = ButtonDefaults.buttonColors(containerColor = InkBlack)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                val controlIcon = if (isTimerRunning) "⏸" else "▶"
                val controlLabel = if (isTimerRunning) "PAUSE" else "START"
                Text(
                  text = controlIcon,
                  color = Color.White,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = controlLabel,
                  color = Color.White,
                  style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Box(
              modifier = Modifier
                .size(44.dp)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .clickable {
                  viewModel.resetTimer()
                },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "⏭",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // 5. Durations Chips Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val durations = listOf(25, 30, 45, 60)
            durations.forEach { duration ->
              val isActive = timerDurationMinutes == duration
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
                  .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                  .mangaBorder(width = if (isActive) 2.dp else 1.dp)
                  .clickable { viewModel.setTimerDuration(duration) },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "$duration MIN",
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 10.sp
                )
              }
            }
          }

          // 6. Custom manual focus timer inputs
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder()
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "ENTER CUSTOM TIMER VALUE",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
              fontSize = 10.sp
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val canSetCustomTimer = ((customHoursInput.toIntOrNull() ?: 0) * 60 + (customMinutesInput.toIntOrNull() ?: 0)) > 0
              OutlinedTextField(
                value = customHoursInput,
                onValueChange = { input ->
                  if (input.all { it.isDigit() } && input.length <= 2) {
                    customHoursInput = input
                  }
                },
                label = { Text("HRS", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp) },
                placeholder = { Text("0") },
                modifier = Modifier.weight(1f),
                shape = RectangleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = MaterialTheme.colorScheme.onSurface,
                  unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
              )

              OutlinedTextField(
                value = customMinutesInput,
                onValueChange = { input ->
                  if (input.all { it.isDigit() } && input.length <= 2) {
                    customMinutesInput = input
                  }
                },
                label = { Text("MINS", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp) },
                placeholder = { Text("25") },
                modifier = Modifier.weight(1f),
                shape = RectangleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = MaterialTheme.colorScheme.onSurface,
                  unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
              )

              Button(
                onClick = {
                  val hours = customHoursInput.toIntOrNull() ?: 0
                  val minutes = customMinutesInput.toIntOrNull() ?: 0
                  val totalMinutes = hours * 60 + minutes
                  if (totalMinutes > 0) {
                    viewModel.setTimerDuration(totalMinutes)
                  }
                },
                enabled = canSetCustomTimer,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                  disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                  disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                  .height(44.dp)
                  .mangaShadow(offset = 2.dp)
                  .mangaBorder()
              ) {
                Text(
                  text = "SET",
                  fontWeight = FontWeight.ExtraBold,
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Timer Circular Progress Ring Area
        Box(
          modifier = Modifier
            .size(240.dp)
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          // Dotted Background Ring Canvas
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
              color = InkGrayLight,
              radius = size.minDimension / 2.3f,
              style = Stroke(
                width = 16f,
                cap = StrokeCap.Round
              )
            )
          }

          // Active Theme/Progress concentric circle arc
          val angleProgress = if (timerDurationMinutes > 0) {
            (timerSecondsLeft.toFloat() / (timerDurationMinutes * 60).toFloat()) * 360f
          } else {
            360f
          }
          val animatedAngleProgress by animateFloatAsState(
            targetValue = angleProgress,
            animationSpec = tween(500, easing = LinearEasing),
            label = "angle_progress_portrait"
          )

          Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 24f
            val diameter = size.minDimension - (padding * 2f)
            val topLeftOffset = Offset(
              (size.width - diameter) / 2f,
              (size.height - diameter) / 2f
            )
            drawArc(
              color = AnimePurple,
              startAngle = -90f,
              sweepAngle = animatedAngleProgress,
              useCenter = false,
              topLeft = topLeftOffset,
              size = Size(diameter, diameter),
              style = Stroke(
                width = 24f,
                cap = StrokeCap.Square
              )
            )
          }

          // Center solid square countdown panel
          Box(
            modifier = Modifier
              .size(130.dp)
              .mangaShadow(offset = 3.dp)
              .background(MaterialTheme.colorScheme.background)
              .mangaBorder(width = 3.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = timeDisplayText,
              style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.ExtraBold
            )
          }

          // Animation overlays
          // Interactive Pen Dip fountain drawing icon
          if (showPenDipAnimation) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                  model = viewModel.getActiveAvatarUrl("focus"),
                  contentDescription = "User avatar dip loading",
                  modifier = Modifier
                    .size(80.dp)
                    .rotate(-15f)
                    .mangaBorder()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "DIPPING PEN IN AURA INK...",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          // Timer completed Ink splatter ink drop splash card
          if (showInkDropAnimation) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .mangaBorder()
                .background(InkBlack),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "SPLASH!",
                  style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                  color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "TASK INKED IN FULL COLOR!",
                  style = MaterialTheme.typography.bodySmall,
                  color = AnimeYellow,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // 2. Active Session slant labeled tag
        Box(
          modifier = Modifier
            .background(InkBlack)
            .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
          val focusStageLabel = if (isTimerRunning) "FOCUS ON WORK" else "REST / PREPARE SESSION"
          Text(
            text = focusStageLabel.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
        }

        val stats by viewModel.stats.collectAsState()
        val reward = viewModel.getXpReward("focus")
        Text(
          text = "COMPLETING YIELDS: +$reward PTS (${stats.difficulty.uppercase()} PACING)",
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          modifier = Modifier.padding(top = 4.dp)
        )

        // 3. Dropdown Selection Panel: Bucket first, then Tasks inside that bucket (disabled if no bucket selected)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Dropdown 1: Pick Bucket Chapter
          Box(modifier = Modifier.fillMaxWidth()) {
            Card(
              shape = RectangleShape,
              modifier = Modifier
                .fillMaxWidth()
                .mangaBorder()
                .mangaShadow(offset = 2.dp)
                .clickable { showChapterDropdown = !showChapterDropdown },
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
              ) {
                val accentColor = when (selectedChapterForFocus?.auraInk) {
                  "Red" -> AnimeRed
                  "Teal" -> AnimeTeal
                  "Purple" -> AnimePurple
                  "Pink" -> AnimePink
                  "Black" -> MaterialTheme.colorScheme.onBackground
                  else -> InkGrayLight
                }
                Box(
                  modifier = Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(accentColor)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface)
                )

                Row(
                  modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "SELECT BUCKET CHAPTER",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = selectedChapterForFocus?.name?.uppercase() ?: "CHOOSE A BUCKET...",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Text(
                    text = if (showChapterDropdown) "▲" else "▼",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                }
              }
            }

            DropdownMenu(
              expanded = showChapterDropdown,
              onDismissRequest = { showChapterDropdown = false },
              modifier = Modifier
                .fillMaxWidth(0.9f)
                .mangaBorder()
                .background(MaterialTheme.colorScheme.surface)
            ) {
              if (chapters.isEmpty()) {
                DropdownMenuItem(
                  text = { Text("Create a bucket first", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                  onClick = { showChapterDropdown = false }
                )
              } else {
                chapters.forEach { chapter ->
                  val chapterColor = when (chapter.auraInk) {
                    "Red" -> AnimeRed
                    "Teal" -> AnimeTeal
                    "Purple" -> AnimePurple
                    "Pink" -> AnimePink
                    "Black" -> MaterialTheme.colorScheme.onBackground
                    else -> InkGrayLight
                  }
                  DropdownMenuItem(
                    text = {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(width = 8.dp, height = 18.dp)
                            .background(chapterColor)
                            .border(0.5.dp, MaterialTheme.colorScheme.onSurface)
                        )
                        Text(chapter.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                      }
                    },
                    onClick = {
                      selectedChapterForFocus = chapter
                      showChapterDropdown = false
                    }
                  )
                }
              }
            }
          }

          // Dropdown 2: Pick Incomplete Task inside the picked bucket (disabled if no bucket selected)
          val isTaskSelectionEnabled = selectedChapterForFocus != null
          Box(modifier = Modifier.fillMaxWidth()) {
            Card(
              shape = RectangleShape,
              modifier = Modifier
                .fillMaxWidth()
                .mangaBorder(
                  width = if (isTaskSelectionEnabled) 2.dp else 1.dp,
                  color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                )
                .mangaShadow(offset = if (isTaskSelectionEnabled) 2.dp else 0.dp)
                .clickable(enabled = isTaskSelectionEnabled) {
                  showTaskSelectorDropdown = !showTaskSelectorDropdown
                },
              colors = CardDefaults.cardColors(
                containerColor = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isTaskSelectionEnabled) "WORKING ON" else "WORKING ON (CHOOSE BUCKET FIRST)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  val taskLabelText = if (!isTaskSelectionEnabled) {
                    "SELECT BUCKET FIRST"
                  } else {
                    activeTask?.name ?: "NONE / REST BREAK"
                  }
                  Text(
                    text = taskLabelText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Text(
                  text = if (showTaskSelectorDropdown) "▲" else "▼",
                  color = if (isTaskSelectionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }

            if (isTaskSelectionEnabled) {
              DropdownMenu(
                expanded = showTaskSelectorDropdown,
                onDismissRequest = { showTaskSelectorDropdown = false },
                modifier = Modifier
                  .fillMaxWidth(0.9f)
                  .mangaBorder()
                  .background(MaterialTheme.colorScheme.surface)
              ) {
                DropdownMenuItem(
                  text = { Text("NONE / REST BREAK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                  onClick = {
                    viewModel.setActiveTask(TaskEntity(chapterId = 0, name = "Rest break module", isCompleted = false))
                    showTaskSelectorDropdown = false
                  }
                )
                incompleteTasksInSelectedChapter.forEach { task ->
                  DropdownMenuItem(
                    text = { Text(task.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                      viewModel.setActiveTask(task)
                      showTaskSelectorDropdown = false
                    }
                  )
                }
              }
            }
          }
        }

        // 4. Timer Controls row (Reset, Play/Pause, Skip)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Reset Button
          Box(
            modifier = Modifier
              .size(54.dp)
              .mangaShadow(offset = 2.dp)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder()
              .clickable { viewModel.resetTimer() },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "⟲",
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Start / Pause Key Button Container
          Button(
            onClick = {
              if (isTimerRunning) viewModel.pauseTimer() else viewModel.startTimer()
            },
            shape = RectangleShape,
            modifier = Modifier
              .weight(1f)
              .height(54.dp)
              .mangaShadow(offset = 2.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(containerColor = InkBlack)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val controlIcon = if (isTimerRunning) "⏸" else "▶"
              val controlLabel = if (isTimerRunning) "PAUSE" else "START"
              Text(
                text = controlIcon,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = controlLabel,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Skip/Complete Button
          Box(
            modifier = Modifier
              .size(54.dp)
              .mangaShadow(offset = 2.dp)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder()
              .clickable {
                viewModel.resetTimer()
              },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "⏭",
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // 5. Durations Chips Row (25, 30, 45, 60 minutes)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val durations = listOf(25, 30, 45, 60)
          durations.forEach { duration ->
            val isActive = timerDurationMinutes == duration
            Box(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                .mangaBorder(width = if (isActive) 2.dp else 1.dp)
                .clickable { viewModel.setTimerDuration(duration) },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$duration MIN",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }

        // 6. Custom manual focus timer inputs (Hours & Minutes)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .mangaBorder()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "ENTER CUSTOM TIMER VALUE",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val canSetCustomTimer = ((customHoursInput.toIntOrNull() ?: 0) * 60 + (customMinutesInput.toIntOrNull() ?: 0)) > 0
            OutlinedTextField(
              value = customHoursInput,
              onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 2) {
                  customHoursInput = input
                }
              },
              label = { Text("HRS", style = MaterialTheme.typography.labelSmall) },
              placeholder = { Text("0") },
              modifier = Modifier.weight(1f),
              shape = RectangleShape,
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
              )
            )

            OutlinedTextField(
              value = customMinutesInput,
              onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 2) {
                  customMinutesInput = input
                }
              },
              label = { Text("MINS", style = MaterialTheme.typography.labelSmall) },
              placeholder = { Text("25") },
              modifier = Modifier.weight(1f),
              shape = RectangleShape,
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
              )
            )

            Button(
              onClick = {
                val hours = customHoursInput.toIntOrNull() ?: 0
                val minutes = customMinutesInput.toIntOrNull() ?: 0
                val totalMinutes = hours * 60 + minutes
                if (totalMinutes > 0) {
                  viewModel.setTimerDuration(totalMinutes)
                }
              },
              enabled = canSetCustomTimer,
              shape = RectangleShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              modifier = Modifier
                .height(52.dp)
                .mangaShadow(offset = 2.dp)
                .mangaBorder()
            ) {
              Text(
                text = "SET",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
