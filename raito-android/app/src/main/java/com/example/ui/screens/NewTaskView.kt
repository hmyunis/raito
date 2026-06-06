package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaitoViewModel
import com.example.util.DateUtils

@Composable
fun NewTaskView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val taskNameInput by viewModel.taskNameInput.collectAsState()
  val taskChapterIdInput by viewModel.taskChapterIdInput.collectAsState()
  val taskTimeRemainingInput by viewModel.taskTimeRemainingInput.collectAsState()
  val taskIsOverdueInput by viewModel.taskIsOverdueInput.collectAsState()
  val taskDescriptionInput by viewModel.taskDescriptionInput.collectAsState()
  val taskDueDatetimeInput by viewModel.taskDueDatetimeInput.collectAsState()
  val timeFormatMode by viewModel.timeFormatMode.collectAsState()
  val chapters by viewModel.chapters.collectAsState()
  val stats by viewModel.stats.collectAsState()
  val canSubmitTask = taskNameInput.trim().isNotEmpty() && taskChapterIdInput != null
  val use24HourTime = timeFormatMode == "24"

  // Initialize selected chapter id if not set yet
  LaunchedEffect(chapters) {
    if (taskChapterIdInput == null && chapters.isNotEmpty()) {
      viewModel.taskChapterIdInput.value = chapters.first().id
    }
  }

  val deadlineTiers = remember {
    listOf("Today", "Tomorrow", "Soon", "Later")
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize()
  ) {
    val isLandscape = maxWidth > maxHeight || maxWidth > 600.dp
    val columnModifier = if (isLandscape) {
      Modifier
        .fillMaxHeight()
        .widthIn(max = 650.dp)
        .align(Alignment.TopCenter)
    } else {
      Modifier.fillMaxSize()
    }

    Column(
      modifier = columnModifier
        .starryTwinkleBackground()
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

    // Header
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "NEW TASK",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(4.dp))
      Box(
        modifier = Modifier
          .width(120.dp)
          .height(4.dp)
          .background(MaterialTheme.colorScheme.primary)
      )
    }

    // Input: Task Name Form
    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        value = taskNameInput,
        onValueChange = { viewModel.taskNameInput.value = it },
        label = { Text("Task Action Name", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        placeholder = { Text("Enter micro-action detail...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = MaterialTheme.colorScheme.onBackground,
          unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
          focusedLabelColor = MaterialTheme.colorScheme.primary,
          unfocusedLabelColor = MaterialTheme.colorScheme.onBackground,
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
      )
    }

    // Input: Task Description Form
    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        value = taskDescriptionInput,
        onValueChange = { viewModel.taskDescriptionInput.value = it },
        label = { Text("Description (Optional)", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        placeholder = { Text("Enter deeper notes, sub-actions, or references...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        minLines = 3,
        maxLines = 5,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = MaterialTheme.colorScheme.onBackground,
          unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
          focusedLabelColor = MaterialTheme.colorScheme.primary,
          unfocusedLabelColor = MaterialTheme.colorScheme.onBackground,
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
      )
    }

    // Input: Due Datetime Form with interactive Date & Time Pickers (both optional)
    val context = LocalContext.current
    
    // Parse current state to pre-populate local picker state
    val parts = remember(taskDueDatetimeInput) { taskDueDatetimeInput.split(" ") }
    val initialDate = remember(parts) { parts.firstOrNull { it.contains("-") } }
    val initialTime = remember(parts) { parts.firstOrNull { it.contains(":") } }

    var selectedDateStr by remember(taskDueDatetimeInput) { mutableStateOf(initialDate) }
    var selectedTimeStr by remember(taskDueDatetimeInput) { mutableStateOf(initialTime) }

    fun updateDueDatetime(date: String?, time: String?) {
      val result = when {
        date != null && time != null -> "$date $time"
        date != null -> date
        time != null -> time
        else -> ""
      }
      viewModel.taskDueDatetimeInput.value = result
    }

    val displayDate = remember(selectedDateStr) {
      if (selectedDateStr != null) {
        DateUtils.formatIsoToMmmDdYyyy(selectedDateStr!!).uppercase()
      } else {
        "NOT SET"
      }
    }

    val displayTime = remember(selectedTimeStr, use24HourTime) {
      if (selectedTimeStr != null) {
        DateUtils.formatTimeForDisplay(selectedTimeStr!!, use24HourTime)
      } else {
        "NOT SET"
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "DUE DATE & TIME (OPTIONAL)",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Date Selector Box
        Column(
          modifier = Modifier
            .weight(1f)
            .mangaShadow(offset = 2.dp)
            .background(MaterialTheme.colorScheme.surface)
            .mangaBorder(width = 1.5.dp)
            .clickable {
              val calendar = java.util.Calendar.getInstance()
              val dateParts = selectedDateStr?.split("-")
              if (dateParts != null && dateParts.size == 3) {
                try {
                  calendar.set(java.util.Calendar.YEAR, dateParts[0].toInt())
                  calendar.set(java.util.Calendar.MONTH, dateParts[1].toInt() - 1)
                  calendar.set(java.util.Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                } catch (_: Exception) {}
              }
              android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                  val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                  selectedDateStr = formattedDate
                  updateDueDatetime(formattedDate, selectedTimeStr)
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
              ).show()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Pick Deadline Date",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "DATE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = displayDate,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            color = if (selectedDateStr != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines = 1
          )
        }

        // Time Selector Box
        Column(
          modifier = Modifier
            .weight(1f)
            .mangaShadow(offset = 2.dp)
            .background(MaterialTheme.colorScheme.surface)
            .mangaBorder(width = 1.5.dp)
            .clickable {
              val timeCalendar = java.util.Calendar.getInstance()
              val timeParts = selectedTimeStr?.split(":")
              if (timeParts != null && timeParts.size == 2) {
                try {
                  timeCalendar.set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                  timeCalendar.set(java.util.Calendar.MINUTE, timeParts[1].toInt())
                } catch (_: Exception) {}
              }
              android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                  val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                  selectedTimeStr = formattedTime
                  updateDueDatetime(selectedDateStr, formattedTime)
                },
                timeCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                timeCalendar.get(java.util.Calendar.MINUTE),
                use24HourTime
              ).show()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = "Pick Deadline Time",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "TIME",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = displayTime,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            color = if (selectedTimeStr != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines = 1
          )
        }
      }

      // If any is set, show a custom clear button to make both optional nicely
      if (selectedDateStr != null || selectedTimeStr != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedDateStr = null
              selectedTimeStr = null
              updateDueDatetime(null, null)
            }
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear deadline data",
            tint = AnimeRed,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "CLEAR DUE DATE / TIME",
            style = MaterialTheme.typography.labelSmall,
            color = AnimeRed,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // Chapter (Bucket) Selector List
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "ASSIGN TO BUCKET CHAPTER",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      if (chapters.isEmpty()) {
        EmptyStateCard(
          icon = Icons.Default.FolderOpen,
          title = "Create a Bucket First",
          message = "Tasks need a bucket so they have a clear home."
        )
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          chapters.forEach { chapter ->
            val isSelected = taskChapterIdInput == chapter.id
            val borderHighlightColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            val borderHighlightWidth = if (isSelected) 3.dp else 1.5.dp
            val backgroundHighlightColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .mangaShadow(offset = if (isSelected) 2.dp else 0.dp)
                .background(backgroundHighlightColor)
                .mangaBorder(width = borderHighlightWidth, color = borderHighlightColor)
                .clickable { viewModel.taskChapterIdInput.value = chapter.id }
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Color aura block
                val auraBg = resolveChapterAccentColor(
                  chapter = chapter,
                  bucketColoringEnabled = stats.bucketColoring,
                  fallbackColor = MaterialTheme.colorScheme.primary
                )
                Box(
                  modifier = Modifier
                    .size(12.dp)
                    .background(auraBg)
                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, RectangleShape)
                )

                Text(
                  text = chapter.name.uppercase(),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold
                )
              }

              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected bucket",
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }

    // Deadline Tiers (Segmented/Horizontal row)
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "EXPECTED TIMELINE",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        deadlineTiers.forEach { tier ->
          val isSelected = taskTimeRemainingInput == tier
          val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
          val textTint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

          Box(
            modifier = Modifier
              .weight(1f)
              .mangaShadow(offset = if (isSelected) 2.dp else 0.dp)
              .background(bg)
              .mangaBorder(width = if (isSelected) 2.5.dp else 1.dp)
              .clickable { viewModel.taskTimeRemainingInput.value = tier }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = tier.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = textTint
            )
          }
        }
      }
    }

    // Task Urgent Switch
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .mangaShadow(offset = 0.dp)
        .background(MaterialTheme.colorScheme.surface)
        .mangaBorder()
        .clickable { viewModel.taskIsOverdueInput.value = !taskIsOverdueInput }
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Urgent status marker",
          tint = if (taskIsOverdueInput) AnimeRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          modifier = Modifier.size(20.dp)
        )
        Column {
          Text(
            text = "URGENT OVERDUE ACTION",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Marker flashes red in dashboard due lists.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 9.sp
          )
        }
      }

      // custom manga outline checkbox
      Box(
        modifier = Modifier
          .size(22.dp)
          .mangaBorder(width = 2.dp)
          .background(if (taskIsOverdueInput) MaterialTheme.colorScheme.primary else Color.White),
        contentAlignment = Alignment.Center
      ) {
        if (taskIsOverdueInput) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Checked status",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Points reward notification
    val stats by viewModel.stats.collectAsState()
    val reward = viewModel.getXpReward("task")
    val difficulty = stats.difficulty.uppercase()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .mangaShadow(offset = 2.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .mangaBorder()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Star,
        contentDescription = "Task Star Points Reward Icon",
        tint = AnimeYellow,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = "Completing this micro-action will earn you +$reward PTS on $difficulty difficulty!",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Form submission submit pen CTA button
    Button(
      onClick = { viewModel.createNewTask() },
      enabled = canSubmitTask,
      shape = RectangleShape,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .mangaShadow(offset = 3.dp)
        .mangaBorder(),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
      )
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Ink task pen icon",
          tint = if (canSubmitTask) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "INK NEW TASK",
          style = MaterialTheme.typography.titleMedium,
          color = if (canSubmitTask) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
