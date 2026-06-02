package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.ChapterEntity
import com.example.data.database.TaskEntity
import com.example.util.CompanionRegistry
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RaitoViewModel

// Chibi entries will be retrieved via CompanionRegistry
@Composable
fun HomeView(
  viewModel: RaitoViewModel,
  onNavigateToShop: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val chapters by viewModel.chapters.collectAsState()
  val tasks by viewModel.tasks.collectAsState()
  val stats by viewModel.stats.collectAsState()

  val uncompletedTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
  val dueSoonCount = uncompletedTasks.size
  var showAvatarSelector by remember { mutableStateOf(false) }
  val customAvatars by viewModel.customAvatars.collectAsState()

  val companionUrl = remember(stats.activeCompanionId, dueSoonCount, customAvatars) {
    if (dueSoonCount > 3) {
      viewModel.getActiveAvatarUrl("sad")
    } else if (dueSoonCount == 0 && uncompletedTasks.isEmpty()) {
       viewModel.getActiveAvatarUrl("happy")
    } else {
      viewModel.getActiveAvatarUrl("neutral")
    }
  }

  // 16-bit console box infinite transition animations
  val infiniteTransition = rememberInfiniteTransition(label = "concierge_animations")
  
  val runningShadowOffset by infiniteTransition.animateFloat(
    initialValue = 3f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shadow_offset_ping_pong"
  )

  val spriteBobbing by infiniteTransition.animateFloat(
    initialValue = -5f,
    targetValue = 5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sprite_bob"
  )

  val spriteScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sprite_scale"
  )

  val spriteWiggle by infiniteTransition.animateFloat(
    initialValue = -2f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sprite_wiggle"
  )

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .starryTwinkleBackground()
  ) {
    val isLandscape = maxWidth > maxHeight || maxWidth > 600.dp

    if (isLandscape) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // Left Column (Concierge + Due Soon)
        Column(
          modifier = Modifier
            .weight(1.1f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          // 1. Concierge Speech Bubble Section
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 115.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Concierge Thought Bubble
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = runningShadowOffset.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder(width = 3.dp)
                .padding(12.dp)
                .align(Alignment.CenterVertically)
                .graphicsLayer {
                  translationY = spriteBobbing * 0.4f
                }
            ) {
              Column {
                val speechText = if (dueSoonCount > 0) {
                  "$dueSoonCount tasks due soon.\nStart with the closest one!"
                } else {
                  "All clean! Your workspace is fully synchronized. Rest or start focus!"
                }
                Text(
                  text = speechText.uppercase(),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 11.sp,
                  lineHeight = 14.sp
                )

                if (viewModel.canClaimDailyStreakBonus()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  val reward = viewModel.getXpReward("streak")
                  Box(
                    modifier = Modifier
                      .background(AnimeYellow)
                      .mangaBorder(width = 1.dp)
                      .clickable { viewModel.claimDailyStreakBonus() }
                      .padding(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = "CLAIM DAILY STREAK: +$reward XP",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Black,
                      fontSize = 8.sp,
                      color = Color.Black
                    )
                  }
                }
              }
            }

            // Chibi Concierge Sprite
            Box(
              modifier = Modifier
                .size(90.dp)
                .mangaShadow(offset = (runningShadowOffset * 0.8f).dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder(width = 2.dp)
                .clickable { showAvatarSelector = true }
                .graphicsLayer {
                  translationY = spriteBobbing
                  scaleX = spriteScale
                  scaleY = spriteScale
                  rotationZ = spriteWiggle
                },
              contentAlignment = Alignment.Center
            ) {
              AsyncImage(
                model = companionUrl,
                contentDescription = "Chibi Companion Concierge",
                modifier = Modifier.fillMaxSize(0.9f)
              )
            }
          }

          // 2. Due Soon Header
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "DUE SOON",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.onBackground)
            )
          }

          if (uncompletedTasks.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Default.CheckCircle,
              title = "Nothing Due Soon",
              message = "You are caught up. Add a new task when you are ready to plan the next step."
            )
          } else {
            uncompletedTasks.take(2).forEach { task ->
              DueSoonTaskCard(
                task = task,
                chapterName = chapters.find { it.id == task.chapterId }?.name ?: "Unknown Chapter",
                onCompleteCompleted = { viewModel.toggleTaskCompletion(task) },
                onFocusRequested = {
                  viewModel.setActiveTask(task)
                  viewModel.navigateTo(AppScreen.FOCUS)
                }
              )
            }
          }
          
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Right Column (Buckets)
        Column(
          modifier = Modifier
            .weight(0.9f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          // 3. Your Buckets Section
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "YOUR BUCKETS",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.onBackground)
            )
          }

          if (chapters.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Default.FolderOpen,
              title = "No Buckets Yet",
              message = "Create a bucket to group related tasks and track progress over time."
            )
          } else {
            chapters.forEach { chapter ->
              val chapterTasks = tasks.filter { it.chapterId == chapter.id }
              val completedCount = chapterTasks.count { it.isCompleted }
              val totalCount = chapterTasks.size

              BucketChapterCard(
                chapter = chapter,
                completedTasks = completedCount,
                totalTasks = totalCount,
                onCardClick = {
                  viewModel.selectedChapterId.value = chapter.id
                  viewModel.navigateTo(AppScreen.BUCKETS)
                }
              )
            }
          }
          
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // Spacer for TopBar safely
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. P5 Concierge Speech Bubble Section (Animated 16-bit style dialog box)
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 115.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Concierge Thought Bubble with moving shadow
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = runningShadowOffset.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder(width = 3.dp)
                .padding(12.dp)
                .align(Alignment.CenterVertically)
                .graphicsLayer {
                  translationY = spriteBobbing * 0.4f
                }
            ) {
              Column {
                val speechText = if (dueSoonCount > 0) {
                  "$dueSoonCount tasks due soon.\nStart with the closest one!"
                } else {
                  "All clean! Your workspace is fully synchronized. Rest or start focus!"
                }
                Text(
                  text = speechText.uppercase(),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 11.sp,
                  lineHeight = 14.sp
                )

                if (viewModel.canClaimDailyStreakBonus()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  val reward = viewModel.getXpReward("streak")
                  Box(
                    modifier = Modifier
                      .background(AnimeYellow)
                      .mangaBorder(width = 1.dp)
                      .clickable { viewModel.claimDailyStreakBonus() }
                      .padding(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = "CLAIM DAILY STREAK: +$reward XP",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Black,
                      fontSize = 8.sp,
                      color = Color.Black
                    )
                  }
                }
              }
            }

            // Chibi Concierge Sprite with full moving sprite animation & retro shadow
            Box(
              modifier = Modifier
                .size(90.dp)
                .mangaShadow(offset = (runningShadowOffset * 0.8f).dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder(width = 2.dp)
                .clickable { showAvatarSelector = true }
                .graphicsLayer {
                  translationY = spriteBobbing
                  scaleX = spriteScale
                  scaleY = spriteScale
                  rotationZ = spriteWiggle
                },
              contentAlignment = Alignment.Center
            ) {
              AsyncImage(
                model = companionUrl,
                contentDescription = "Chibi Companion Concierge",
                modifier = Modifier.fillMaxSize(0.9f)
              )
            }
          }
        }

        // 2. Due Soon Urgent Checkbox List
        item {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "DUE SOON",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.onBackground)
            )
          }
        }

        if (uncompletedTasks.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Default.CheckCircle,
              title = "Nothing Due Soon",
              message = "You are caught up. Add a new task when you are ready to plan the next step."
            )
          }
        } else {
          // Show top 2 uncompleted tasks for due soon list
          items(uncompletedTasks.take(2)) { task ->
            DueSoonTaskCard(
              task = task,
              chapterName = chapters.find { it.id == task.chapterId }?.name ?: "Unknown Chapter",
              onCompleteCompleted = { viewModel.toggleTaskCompletion(task) },
              onFocusRequested = {
                viewModel.setActiveTask(task)
                viewModel.navigateTo(AppScreen.FOCUS)
              }
            )
          }
        }

        // 3. Your Buckets Section
        item {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "YOUR BUCKETS",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.onBackground)
            )
          }
        }

        if (chapters.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Default.FolderOpen,
              title = "No Buckets Yet",
              message = "Create a bucket to group related tasks and track progress over time."
            )
          }
        } else {
          items(chapters) { chapter ->
            val chapterTasks = tasks.filter { it.chapterId == chapter.id }
            val completedCount = chapterTasks.count { it.isCompleted }
            val totalCount = chapterTasks.size

            BucketChapterCard(
              chapter = chapter,
              completedTasks = completedCount,
              totalTasks = totalCount,
              onCardClick = {
                viewModel.selectedChapterId.value = chapter.id
                viewModel.navigateTo(AppScreen.BUCKETS)
              }
            )
          }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
      }
    }
  }

  if (showAvatarSelector) {
    SwitchAvatarDialog(
      viewModel = viewModel,
      onDismiss = { showAvatarSelector = false },
      onGoToShop = {
        showAvatarSelector = false
        onNavigateToShop()
      }
    )
  }
}

@Composable
fun DueSoonTaskCard(
  task: TaskEntity,
  chapterName: String,
  onCompleteCompleted: () -> Unit,
  onFocusRequested: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }
  // Overdue tracks flashing red warning, standard is default border
  val cardBorderColor = if (task.isOverdue) AnimeRed else MaterialTheme.colorScheme.onBackground
  val cardBorderWidth = if (task.isOverdue) 4.dp else 2.dp
  val formattedCreated = remember(task.createdAt) {
    val time = task.createdAt ?: System.currentTimeMillis()
    com.example.util.DateUtils.formatMmmDdYyyyHHmm(java.util.Date(time), true)
  }
  val formattedDue = remember(task.dueDatetime) {
    com.example.util.DateUtils.formatDateTimeStringForDisplay(task.dueDatetime, true)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = 3.dp)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder(width = cardBorderWidth, color = cardBorderColor)
      .clickable { isExpanded = !isExpanded }
  ) {
    if (task.isOverdue) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .speedLinesPattern()
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          // Chapter Label Tag
          Box(
            modifier = Modifier
              .background(if (task.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant)
              .mangaBorder(width = 1.dp, color = MaterialTheme.colorScheme.onBackground)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = chapterName.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = if (task.isOverdue) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold,
              fontSize = 8.sp
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Task Label Name
          Text(
            text = task.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (task.isOverdue) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Overdue Alert Warning",
            tint = AnimeRed,
            modifier = Modifier.size(22.dp)
          )
        } else {
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = "Toggle task details",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val remainingText = task.timeRemaining ?: "Soon"
        Text(
          text = remainingText.uppercase(),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = if (task.isOverdue) AnimeRed else MaterialTheme.colorScheme.onSurface
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .mangaBorder(width = 2.dp)
              .background(MaterialTheme.colorScheme.primary)
              .clickable { onFocusRequested() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Start focus session",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(18.dp)
            )
          }

          // Ink checkbox box
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
              .mangaBorder(width = 2.dp)
              .background(if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
              .clickable { onCompleteCompleted() },
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
        }
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

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (!task.description.isNullOrBlank()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RectangleShape)
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

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            TaskDetailCapsule(
              label = "CHAPTER",
              value = chapterName.uppercase(),
              modifier = Modifier.weight(1f)
            )
            TaskDetailCapsule(
              label = "CREATED",
              value = formattedCreated,
              modifier = Modifier.weight(1f)
            )
          }

          if (!task.dueDatetime.isNullOrBlank()) {
            TaskDetailCapsule(
              label = "DUE DATETIME",
              value = formattedDue,
              modifier = Modifier.fillMaxWidth(),
              accentColor = AnimeRed
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TaskDetailCapsule(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .mangaBorder(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
        fontWeight = FontWeight.Bold,
        fontSize = 8.sp
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun BucketChapterCard(
  chapter: ChapterEntity,
  completedTasks: Int,
  totalTasks: Int,
  onCardClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val companionUrl = remember(chapter.companionId) {
    CompanionRegistry.getExpressions(chapter.companionId).neutral
  }

  val barColor = when (chapter.auraInk) {
    "Red" -> AnimeRed
    "Teal" -> AnimeTeal
    "Purple" -> AnimePurple
    "Pink" -> AnimePink
    "Black" -> MaterialTheme.colorScheme.onBackground
    else -> AnimeTeal
  }

  val progressPercent = if (totalTasks > 0) completedCountFloat(completedTasks, totalTasks) else 0f

  Box(
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = 3.dp)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder(width = 2.dp)
      .screentonePattern()
      .clickable { onCardClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
    ) {
      // Solid Vertical Indicator bar based on Aura Ink Color!
      Box(
        modifier = Modifier
          .width(12.dp)
          .fillMaxHeight()
          .background(barColor)
          .border(width = 2.dp, color = MaterialTheme.colorScheme.onBackground, shape = RectangleShape)
      )

      Column(
        modifier = Modifier
          .weight(1f)
          .background(MaterialTheme.colorScheme.surface)
          .mangaBorder(width = 2.dp)
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column(modifier = Modifier.weight(1f)) {
            // Stage/Status Tag
            val stageLabel = if (chapter.isCompleted) "INKED" else "LINE ART"
            val stageBgColor = if (chapter.isCompleted) barColor else MaterialTheme.colorScheme.surfaceVariant
            Box(
              modifier = Modifier
                .background(stageBgColor)
                .mangaBorder(width = 1.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = stageLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (chapter.isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Chapter Name Title
            Text(
              text = chapter.name,
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Black
            )
          }

          // Companion Avatar Graphic preview inside the card
          Box(
            modifier = Modifier
              .size(54.dp)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder(width = 1.dp)
              .padding(4.dp),
            contentAlignment = Alignment.Center
          ) {
            AsyncImage(
              model = companionUrl,
              contentDescription = "Chapter companion",
              modifier = Modifier.fillMaxSize()
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "PROGRESS",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "$completedTasks/$totalTasks TASKS",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar container
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .mangaBorder(width = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(progressPercent)
              .background(barColor)
          )
        }
      }
    }
  }
}

private fun completedCountFloat(completed: Int, total: Int): Float {
  if (total == 0) return 0f
  return completed.toFloat() / total.toFloat()
}
