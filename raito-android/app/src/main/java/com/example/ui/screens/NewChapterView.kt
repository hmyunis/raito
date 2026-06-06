package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.util.CompanionRegistry
import com.example.util.CompanionRegistry.CYBER
import com.example.util.CompanionRegistry.DRAGON
import com.example.util.CompanionRegistry.KNIGHT
import com.example.util.CompanionRegistry.RANGER
import com.example.util.CompanionRegistry.SCHOLAR
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaitoViewModel
import com.example.util.DateUtils

/**
 * CHIBI IMAGE SLOTS DOCUMENTATION:
 * 1. NEUTRAL: Default idle state shown in bucket cards and main hub.
 * 2. HAPPY: Reaction when the user completes a task or reaches a milestone.
 * 3. FOCUS: Active state shown during the deep work timer sessions.
 * 4. SAD: State shown when tasks are overdue or the user cancels a focus session.
 * 5. COMPLETED: Celebratory state shown once a bucket (Chapter) is 100% finished.
 */

data class DisciplineCategory(
  val name: String,
  val icon: ImageVector
)

@Composable
fun NewChapterView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val editingChapterId by viewModel.editingChapterId.collectAsState()
  val chapterNameInput by viewModel.chapterNameInput.collectAsState()
  val selectedDiscipline by viewModel.selectedDiscipline.collectAsState()
  val selectedCompanionId by viewModel.selectedCompanionId.collectAsState()
  val selectedAuraInk by viewModel.selectedAuraInk.collectAsState()
  val selectedDeadline by viewModel.selectedDeadline.collectAsState()
  val selectedTelegramSyncEnabled by viewModel.selectedTelegramSyncEnabled.collectAsState()
  val timeFormatMode by viewModel.timeFormatMode.collectAsState()
  val customAuraColor: Color? by viewModel.customAuraColor.collectAsState()
  val use24HourTime = timeFormatMode == "24"
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }
  val canSubmitChapter = chapterNameInput.trim().isNotEmpty()

  val disciplines = remember {
    listOf(
      DisciplineCategory("Study", Icons.Default.List),
      DisciplineCategory("Work", Icons.Default.Build),
      DisciplineCategory("Personal", Icons.Default.Favorite),
      DisciplineCategory("Fitness", Icons.Default.Refresh),
      DisciplineCategory("Project", Icons.Default.Star),
      DisciplineCategory("Custom", Icons.Default.Add)
    )
  }

  val companionsList = remember {
    listOf(
      Triple("Cyber", "Line Art Cyber", CYBER.neutral),
      Triple("Knight", "Line Art Knight", KNIGHT.neutral),
      Triple("Scholar", "Line Art Scholar", SCHOLAR.neutral),
      Triple("Ranger", "Line Art Ranger", RANGER.neutral),
      Triple("Dragon", "Line Art Dragon", DRAGON.neutral)
    )
  }

  val onSurfaceColor = MaterialTheme.colorScheme.onSurface
  val swatchesList = remember(onSurfaceColor) {
    listOf(
      Pair("Red", AnimeRed),
      Pair("Teal", AnimeTeal),
      Pair("Purple", AnimePurple),
      Pair("Pink", AnimePink),
      Pair("Orange", AnimeOrange),
      Pair("Green", AnimeGreen),
      Pair("Yellow", AnimeYellow),
      Pair("Indigo", IndigoAccent),
      Pair("Blue", Color(0xFF2196F3)),
      Pair("Black", onSurfaceColor)
    )
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
  ) {
    val isLandscape = maxWidth > maxHeight || maxWidth > 600.dp

    if (isLandscape) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
      ) {
        // Left Column: Core Fields & CTA
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column {
              Text(
                text = if (editingChapterId != null) "EDIT CHAPTER" else "NEW CHAPTER",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .width(150.dp)
                  .height(4.dp)
                  .background(MaterialTheme.colorScheme.onSurface)
              )
            }

            if (editingChapterId != null) {
              IconButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier
                  .mangaBorder(width = 1.5.dp, color = MaterialTheme.colorScheme.error)
                  .background(MaterialTheme.colorScheme.surface)
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete bucket",
                  tint = MaterialTheme.colorScheme.error
                )
              }
            }
          }

          // Input: Chapter Name Form
          Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = chapterNameInput,
              onValueChange = { viewModel.chapterNameInput.value = it },
              label = { Text("Chapter Name", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
              placeholder = { Text("Enter objective...", color = InkGrayLight) },
              modifier = Modifier.fillMaxWidth(),
              shape = RectangleShape,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
              )
            )
          }

          // Input: Deadline
          var showDatePicker by remember { mutableStateOf(false) }
          var showTimePicker by remember { mutableStateOf(false) }
          @OptIn(ExperimentalMaterial3Api::class)
          val datePickerState = rememberDatePickerState()
          @OptIn(ExperimentalMaterial3Api::class)
          val timePickerState = rememberTimePickerState(is24Hour = use24HourTime)

          if (showDatePicker) {
            @OptIn(ExperimentalMaterial3Api::class)
            DatePickerDialog(
              onDismissRequest = { showDatePicker = false },
              confirmButton = {
                TextButton(onClick = {
                  showDatePicker = false
                  val selectedMs = datePickerState.selectedDateMillis
                  if (selectedMs != null) {
                    val date = java.util.Date(selectedMs)
                    val formatted = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()).format(date)
                    viewModel.selectedDeadline.value = formatted
                    showTimePicker = true // chain the time picker!
                  }
                }) { Text("Next") }
              },
              dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
              }
            ) {
              DatePicker(state = datePickerState)
            }
          }

          if (showTimePicker) {
            @OptIn(ExperimentalMaterial3Api::class)
            AlertDialog(
              onDismissRequest = { showTimePicker = false },
              confirmButton = {
                TextButton(onClick = {
                  showTimePicker = false
                  val hour = timePickerState.hour.toString().padStart(2, '0')
                  val minute = timePickerState.minute.toString().padStart(2, '0')
                  val currentBase = viewModel.selectedDeadline.value
                  viewModel.selectedDeadline.value = "$currentBase $hour:$minute"
                }) { Text("Confirm") }
              },
              dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
              },
              text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  TimePicker(state = timePickerState)
                }
              }
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showDatePicker = true }
          ) {
            OutlinedTextField(
              value = DateUtils.formatDateTimeStringForDisplay(selectedDeadline, use24HourTime),
              onValueChange = {},
              enabled = false,
              label = { Text("Deadline (Optional)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
              placeholder = { Text("Click to select", color = MaterialTheme.colorScheme.onSurface) },
              modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
              singleLine = true,
              shape = RectangleShape,
              colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
              ),
              trailingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
              }
            )
          }

          MangaSettingSwitchRow(
            title = "Sync This Bucket To Telegram",
            description = "Publish this bucket and its tasks to your linked Telegram bot for browsing in DMs.",
            checked = selectedTelegramSyncEnabled,
            onCheckedChange = { viewModel.selectedTelegramSyncEnabled.value = it }
          )
          
          Spacer(modifier = Modifier.weight(1f))

          // Landscape points banner
          val stats by viewModel.stats.collectAsState()
          val reward = viewModel.getXpReward("chapter")
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .mangaShadow(offset = 2.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .mangaBorder()
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Chapter Reward Star",
              tint = AnimeYellow,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "Award: +$reward PTS (${stats.difficulty.uppercase()})",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Create Chapter Submit CTA Button
          Button(
            onClick = { viewModel.saveChapter() },
            enabled = canSubmitChapter,
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .mangaShadow(offset = 3.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.onSurface,
              contentColor = MaterialTheme.colorScheme.surface,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = if (editingChapterId != null) Icons.Default.Save else Icons.Default.Edit,
                contentDescription = "Submit icon",
                tint = if (canSubmitChapter) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = if (editingChapterId != null) "SAVE CHANGES" else "CREATE CHAPTER",
                style = MaterialTheme.typography.bodyMedium,
                color = if (canSubmitChapter) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
            }
          }
          
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Right Column: Settings configuration (Discipline, Companion, Swatches)
        Column(
          modifier = Modifier
            .weight(1.2f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          // Discipline Selector row
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "DISCIPLINE",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                disciplines.take(3).forEach { discipline ->
                  DisciplineButton(
                    category = discipline,
                    isSelected = selectedDiscipline == discipline.name,
                    onClick = { viewModel.selectedDiscipline.value = discipline.name }
                  )
                }
              }

              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                disciplines.drop(3).forEach { discipline ->
                  DisciplineButton(
                    category = discipline,
                    isSelected = selectedDiscipline == discipline.name,
                    onClick = { viewModel.selectedDiscipline.value = discipline.name }
                  )
                }
              }
            }
          }

          // Companion Selector Row
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
            ) {
              Text(
                text = "COMPANION",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "PENCIL LINE ART STAGE",
                style = MaterialTheme.typography.labelSmall,
                color = InkGrayDark,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
              )
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              companionsList.forEach { p ->
                val id = p.first
                val label = p.second
                val url = p.third
                val isSelected = selectedCompanionId == id
                Box(
                  modifier = Modifier
                    .size(80.dp)
                    .mangaShadow(offset = if (isSelected) 3.dp else 0.dp)
                    .background(if (isSelected) Color.White else BackgroundLight)
                    .mangaBorder(width = if (isSelected) 2.dp else 1.dp, color = MaterialTheme.colorScheme.onSurface)
                    .clickable { viewModel.selectedCompanionId.value = id },
                  contentAlignment = Alignment.Center
                ) {
                  AsyncImage(
                    model = url,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(if (isSelected) 0.82f else 0.6f)
                  )

                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = id.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }

          // Aura Ink Color Swatch Picker Row
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "AURA INK",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              swatchesList.forEach { swatch ->
                val isSelected = selectedAuraInk == swatch.first
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .mangaShadow(offset = if (isSelected) 2.dp else 0.dp)
                    .background(swatch.second)
                    .mangaBorder(width = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                    .clickable { 
                      viewModel.selectedAuraInk.value = swatch.first
                      viewModel.customAuraColor.value = null
                    },
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .background(Color.White)
                    )
                  }
                }
              }
              
              // Custom Color Button
              val isCustomSelected = selectedAuraInk == "Custom"
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .mangaShadow(offset = if (isCustomSelected) 2.dp else 0.dp)
                  .background(customAuraColor ?: Color.Gray)
                  .mangaBorder(
                    width = if (isCustomSelected) 2.dp else 1.dp, 
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  .clickable { 
                    viewModel.selectedAuraInk.value = "Custom"
                    if (viewModel.customAuraColor.value == null) {
                       viewModel.customAuraColor.value = Color.Red
                    }
                  },
                contentAlignment = Alignment.Center
              ) {
                if (isCustomSelected) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .background(Color.White)
                  )
                } else {
                   Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
              }
            }
            
            if (selectedAuraInk == "Custom") {
               // Simple color slider representation for custom color
               Column(modifier = Modifier.fillMaxWidth()) {
                  Text("R / G / B", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  val currentColor = customAuraColor ?: Color.Red
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(
                      value = currentColor.red,
                      onValueChange = { viewModel.customAuraColor.value = currentColor.copy(red = it) },
                      modifier = Modifier.weight(1f)
                    )
                    Slider(
                      value = currentColor.green,
                      onValueChange = { viewModel.customAuraColor.value = currentColor.copy(green = it) },
                      modifier = Modifier.weight(1f)
                    )
                    Slider(
                      value = currentColor.blue,
                      onValueChange = { viewModel.customAuraColor.value = currentColor.copy(blue = it) },
                      modifier = Modifier.weight(1f)
                    )
                  }
               }
            }
          }
          
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column {
            Text(
              text = if (editingChapterId != null) "EDIT CHAPTER" else "NEW CHAPTER",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .width(150.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.onSurface)
            )
          }

          if (editingChapterId != null) {
            IconButton(
              onClick = { showDeleteConfirmDialog = true },
              modifier = Modifier
                .mangaBorder(width = 1.5.dp, color = MaterialTheme.colorScheme.error)
                .background(MaterialTheme.colorScheme.surface)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete bucket",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }

        // Input: Chapter Name Form
        Box(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = chapterNameInput,
            onValueChange = { viewModel.chapterNameInput.value = it },
            label = { Text("Chapter Name", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
            placeholder = { Text("Enter objective...", color = InkGrayLight) },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
              focusedBorderColor = MaterialTheme.colorScheme.onSurface,
              unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
              focusedLabelColor = MaterialTheme.colorScheme.onSurface,
              unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
          )
        }

        // Discipline Selector row
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "DISCIPLINE",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Grid col 1
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              disciplines.take(3).forEach { discipline ->
                DisciplineButton(
                  category = discipline,
                  isSelected = selectedDiscipline == discipline.name,
                  onClick = { viewModel.selectedDiscipline.value = discipline.name }
                )
              }
            }

            // Grid col 2
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              disciplines.drop(3).forEach { discipline ->
                DisciplineButton(
                  category = discipline,
                  isSelected = selectedDiscipline == discipline.name,
                  onClick = { viewModel.selectedDiscipline.value = discipline.name }
                )
              }
            }
          }
        }

        // Companion Selector Row
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Text(
              text = "COMPANION",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "PENCIL LINE ART STAGE",
              style = MaterialTheme.typography.labelSmall,
              color = InkGrayDark,
              fontWeight = FontWeight.Bold,
              fontSize = 8.sp
            )
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            companionsList.forEach { p ->
              val id = p.first
              val label = p.second
              val url = p.third
              val isSelected = selectedCompanionId == id
              Box(
                modifier = Modifier
                  .size(90.dp)
                  .mangaShadow(offset = if (isSelected) 3.dp else 0.dp)
                  .background(if (isSelected) Color.White else BackgroundLight)
                  .mangaBorder(width = if (isSelected) 2.dp else 1.dp, color = MaterialTheme.colorScheme.onSurface)
                  .clickable { viewModel.selectedCompanionId.value = id },
                contentAlignment = Alignment.Center
              ) {
                AsyncImage(
                  model = url,
                  contentDescription = label,
                  modifier = Modifier.fillMaxSize(if (isSelected) 0.82f else 0.6f)
                )

                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .align(Alignment.BottomCenter)
                      .background(MaterialTheme.colorScheme.onSurface)
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = p.first.uppercase(),
                      color = Color.White,
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 7.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }

        // Aura Ink Color Swatch Picker Row
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "AURA INK",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          @OptIn(ExperimentalLayoutApi::class)
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            swatchesList.forEach { swatch ->
              val isSelected = selectedAuraInk == swatch.first
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .mangaShadow(offset = if (isSelected) 2.dp else 0.dp)
                  .background(swatch.second)
                  .mangaBorder(width = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                  .clickable { 
                    viewModel.selectedAuraInk.value = swatch.first
                    viewModel.customAuraColor.value = null
                  },
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .background(Color.White)
                  )
                }
              }
            }
            
            // Custom Color Button
            val isCustomSelected = selectedAuraInk == "Custom"
            Box(
              modifier = Modifier
                .size(40.dp)
                .mangaShadow(offset = if (isCustomSelected) 2.dp else 0.dp)
                .background(customAuraColor ?: Color.Gray)
                .mangaBorder(
                  width = if (isCustomSelected) 2.dp else 1.dp, 
                  color = MaterialTheme.colorScheme.onSurface
                )
                .clickable { 
                  viewModel.selectedAuraInk.value = "Custom"
                  if (viewModel.customAuraColor.value == null) {
                     viewModel.customAuraColor.value = Color.Red
                  }
                },
              contentAlignment = Alignment.Center
            ) {
              if (isCustomSelected) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .background(Color.White)
                )
              } else {
                 Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              }
            }
          }
          
          if (selectedAuraInk == "Custom") {
             // Simple color slider representation for custom color
             Column(modifier = Modifier.fillMaxWidth()) {
                Text("R / G / B", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                val currentColor = customAuraColor ?: Color.Red
                Slider(
                  value = currentColor.red,
                  onValueChange = { viewModel.customAuraColor.value = currentColor.copy(red = it) }
                )
                Slider(
                  value = currentColor.green,
                  onValueChange = { viewModel.customAuraColor.value = currentColor.copy(green = it) }
                )
                Slider(
                  value = currentColor.blue,
                  onValueChange = { viewModel.customAuraColor.value = currentColor.copy(blue = it) }
                )
             }
          }
        }

          // Input: Deadline
          var showDatePicker by remember { mutableStateOf(false) }
          var showTimePicker by remember { mutableStateOf(false) }
          @OptIn(ExperimentalMaterial3Api::class)
          val datePickerState = rememberDatePickerState()
          @OptIn(ExperimentalMaterial3Api::class)
          val timePickerState = rememberTimePickerState(is24Hour = use24HourTime)

          if (showDatePicker) {
            @OptIn(ExperimentalMaterial3Api::class)
            DatePickerDialog(
              onDismissRequest = { showDatePicker = false },
              confirmButton = {
                TextButton(onClick = {
                  showDatePicker = false
                  val selectedMs = datePickerState.selectedDateMillis
                  if (selectedMs != null) {
                    val date = java.util.Date(selectedMs)
                    val formatted = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()).format(date)
                    viewModel.selectedDeadline.value = formatted
                    showTimePicker = true // chain the time picker!
                  }
                }) { Text("Next") }
              },
              dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
              }
            ) {
              DatePicker(state = datePickerState)
            }
          }

          if (showTimePicker) {
            @OptIn(ExperimentalMaterial3Api::class)
            AlertDialog(
              onDismissRequest = { showTimePicker = false },
              confirmButton = {
                TextButton(onClick = {
                  showTimePicker = false
                  val hour = timePickerState.hour.toString().padStart(2, '0')
                  val minute = timePickerState.minute.toString().padStart(2, '0')
                  val currentBase = viewModel.selectedDeadline.value
                  viewModel.selectedDeadline.value = "$currentBase $hour:$minute"
                }) { Text("Confirm") }
              },
              dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
              },
              text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  TimePicker(state = timePickerState)
                }
              }
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showDatePicker = true }
          ) {
            OutlinedTextField(
              value = DateUtils.formatDateTimeStringForDisplay(selectedDeadline, use24HourTime),
              onValueChange = {},
              enabled = false,
              label = { Text("Deadline (Optional)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
              placeholder = { Text("Click to select Date & Time", color = InkGrayLight) },
              modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
              singleLine = true,
              shape = RectangleShape,
              colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                disabledPlaceholderColor = InkGrayLight,
                disabledContainerColor = MaterialTheme.colorScheme.surface
              )
            )
          }

        MangaSettingSwitchRow(
          title = "Sync This Bucket To Telegram",
          description = "Publish this bucket and its tasks to your linked Telegram bot for browsing in DMs.",
          checked = selectedTelegramSyncEnabled,
          onCheckedChange = { viewModel.selectedTelegramSyncEnabled.value = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Portrait points banner
        val stats by viewModel.stats.collectAsState()
        val reward = viewModel.getXpReward("chapter")
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .mangaShadow(offset = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .mangaBorder()
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Chapter Reward Star",
            tint = AnimeYellow,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "Completing this objective will award a bonus of +$reward PTS on ${stats.difficulty.uppercase()} difficulty!",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Create Chapter Submit CTA Button
        Button(
          onClick = { viewModel.saveChapter() },
          enabled = canSubmitChapter,
          shape = RectangleShape,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .mangaShadow(offset = 3.dp)
            .mangaBorder(),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (editingChapterId != null) Icons.Default.Save else Icons.Default.Edit,
              contentDescription = "Submit icon",
              tint = if (canSubmitChapter) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Text(
            text = if (editingChapterId != null) "SAVE CHANGES" else "CREATE CHAPTER",
            style = MaterialTheme.typography.titleMedium,
            color = if (canSubmitChapter) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  if (showDeleteConfirmDialog && editingChapterId != null) {
    DeleteConfirmationDialog(
      title = "DELETE BUCKET?",
      message = "Deleting this bucket will permanently remove the bucket and all tasks inside it. This action cannot be undone.",
      onConfirm = {
        val chapterId = editingChapterId
        if (chapterId != null) {
          viewModel.deleteChapter(chapterId)
        }
        showDeleteConfirmDialog = false
      },
      onDismiss = { showDeleteConfirmDialog = false }
    )
  }
}

@Composable
fun DisciplineButton(
  category: DisciplineCategory,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = if (isSelected) 3.dp else 0.dp)
      .background(if (isSelected) AnimeYellow else MaterialTheme.colorScheme.surface)
      .mangaBorder(color = MaterialTheme.colorScheme.onSurface)
      .clickable { onClick() }
      .padding(vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = category.icon,
        contentDescription = category.name,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = category.name.uppercase(),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
