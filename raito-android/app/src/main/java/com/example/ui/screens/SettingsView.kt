package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CompanionRegistry
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaitoViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Refresh

@Composable
fun SettingsView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val stats by viewModel.stats.collectAsState()
  val timeFormatMode by viewModel.timeFormatMode.collectAsState()
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  BoxWithConstraints(
    modifier = modifier.fillMaxSize()
  ) {
    val isLandscape = maxWidth > maxHeight || maxWidth > 600.dp
    val listModifier = if (isLandscape) {
      Modifier
        .fillMaxHeight()
        .widthIn(max = 650.dp)
        .align(Alignment.TopCenter)
    } else {
      Modifier.fillMaxSize()
    }

    LazyColumn(
      modifier = listModifier
        .starryTwinkleBackground()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

    // Settings Header (Canvas Level)
    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "SETTINGS",
          style = MaterialTheme.typography.displayMedium,
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
          modifier = Modifier
            .width(130.dp)
            .height(4.dp)
            .background(MaterialTheme.colorScheme.primary)
        )
      }
    }

    // Appearance Section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .mangaShadow(offset = 3.dp)
          .background(MaterialTheme.colorScheme.surface)
          .mangaBorder()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "DISPLAY",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // Theme control: Light vs Dark buttons
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(text = "Theme", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .mangaBorder()
          ) {
            val isLight = stats.themeMode == "Light"
            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (isLight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .clickable { viewModel.updateSettingTheme("Light") }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "LIGHT MODE",
                color = if (isLight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
              )
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (!isLight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .clickable { viewModel.updateSettingTheme("Dark") }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "NIGHT MODE",
                color = if (!isLight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Scale typography slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Time Format",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .mangaBorder()
          ) {
            val is24Hour = timeFormatMode == "24"
            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (!is24Hour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .clickable { viewModel.updateTimeFormatMode("12") }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "12 HOUR",
                color = if (!is24Hour) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
              )
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (is24Hour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .clickable { viewModel.updateTimeFormatMode("24") }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "24 HOUR",
                color = if (is24Hour) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Scale typography slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Text Size",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            val scaleLabel = when {
              stats.typographyScale > 1.1f -> "High Contrast"
              stats.typographyScale < 0.9f -> "Compact Small"
              else -> "Medium Default"
            }
            Box(
              modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .mangaBorder()
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = scaleLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
              )
            }
          }

          Slider(
            value = stats.typographyScale,
            onValueChange = { viewModel.updateSettingTypography(it) },
            valueRange = 0.8f..1.4f,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
          )
        }

        // Switch Reduced Motion
        MangaSettingSwitchRow(
          title = "Reduce Motion",
          description = "Use calmer animations throughout the app.",
          checked = stats.reducedMotion,
          onCheckedChange = { viewModel.updateSettingReducedMotion(it) }
        )
      }
    }

    // Companions section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .mangaShadow(offset = 3.dp)
          .background(MaterialTheme.colorScheme.surface)
          .mangaBorder()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Face,
            contentDescription = "Companions logo settings",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
          )
          Text(
          text = "COMPANIONS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
          )
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        MangaSettingSwitchRow(
          title = "Quiet Companion",
          description = "Reduce companion comments and extra reminders.",
          checked = stats.silenceChibiComments,
          onCheckedChange = { viewModel.updateSettingSilenceChibi(it) }
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        MangaSettingSwitchRow(
          title = "Use Bucket Colors",
          description = "Let cards, avatars, and progress details use each bucket's selected color.",
          checked = stats.bucketColoring,
          onCheckedChange = { viewModel.updateSettingBucketColor(it) }
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // Select Active Companion
        Text(
          text = "ACTIVE COMPANION",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.onSurface
        )

        val customAvatars by viewModel.customAvatars.collectAsState()
        val unlockedList = remember(stats.unlockedCompanions) {
          stats.unlockedCompanions.split(",").toSet()
        }

        val standardComps = listOf(
          Triple("Cyber", "Cyber Assistant", CompanionRegistry.CYBER.neutral),
          Triple("Knight", "Tiny Knight", CompanionRegistry.KNIGHT.neutral),
          Triple("Scholar", "Studio Artist", CompanionRegistry.SCHOLAR.neutral),
          Triple("Ranger", "Shadow Ranger", CompanionRegistry.RANGER.neutral),
          Triple("Dragon", "Dragon Keeper", CompanionRegistry.DRAGON.neutral)
        )

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          standardComps.forEach { (compId, compName, imgUrl) ->
            val isUnlocked = compId == "Cyber" || unlockedList.contains(compId)
            val isSelected = stats.activeCompanionId == compId
            
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                .mangaBorder(
                  width = if (isSelected) 2.dp else 0.5.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                .clickable(enabled = isUnlocked) {
                  viewModel.equipCompanion(compId)
                }
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black)
                    .mangaBorder(width = 1.dp)
                ) {
                  coil.compose.AsyncImage(
                    model = imgUrl,
                    contentDescription = compName,
                    modifier = Modifier.fillMaxSize()
                  )
                }
                
                Column {
                  Text(
                    text = compName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                  )
                  Text(
                    text = if (isUnlocked) "EQUIPPED" else "LOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (isUnlocked) AnimeGreen else AnimeRed
                  )
                }
              }
              
              if (!isUnlocked) {
                Button(
                  onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.SHOP) },
                  colors = ButtonDefaults.buttonColors(containerColor = AnimeYellow, contentColor = Color.Black),
                  shape = RectangleShape,
                  modifier = Modifier.mangaBorder(0.5.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("SHOP", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
              } else if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Currently active",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          customAvatars.forEach { custom ->
            val compId = "custom_${custom.id}"
            val isSelected = stats.activeCompanionId == compId
            
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                .mangaBorder(
                  width = if (isSelected) 2.dp else 0.5.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                .clickable {
                  viewModel.equipCompanion(compId)
                }
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black)
                    .mangaBorder(width = 1.dp)
                ) {
                  coil.compose.AsyncImage(
                    model = custom.neutralPath ?: CompanionRegistry.CYBER.neutral,
                    contentDescription = custom.name,
                    modifier = Modifier.fillMaxSize()
                  )
                }
                
                Column {
                  Text(
                    text = custom.name.removePrefix("[Chibi] ").removePrefix("[Outfit] ").removePrefix("[Animal] "),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "CUSTOM CHIBI",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = AnimeTeal
                  )
                }
              }
              
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Currently active",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }

    // Notifications section
    item {
      var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.canPostNotifications(context))
      }
      val notificationsEnabled = stats.notificationsMasterEnabled && hasNotificationPermission
      val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
      ) { isGranted ->
        hasNotificationPermission = isGranted
        viewModel.updateSettingNotificationsMaster(isGranted)
        if (!isGranted) {
          Toast.makeText(context, "Notification permission was not granted.", Toast.LENGTH_SHORT).show()
        }
      }

      LaunchedEffect(Unit) {
        hasNotificationPermission = NotificationHelper.canPostNotifications(context)
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .mangaShadow(offset = 3.dp)
          .background(MaterialTheme.colorScheme.surface)
          .mangaBorder()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notification logo settings",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "NOTIFICATIONS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
          )
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // Master notification toggle
        MangaSettingSwitchRow(
          title = "Allow Notifications",
          description = "Turn on reminders and activity alerts from Raito.",
          checked = notificationsEnabled,
          onCheckedChange = { checked ->
            if (checked) {
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (NotificationHelper.canPostNotifications(context)) {
                  hasNotificationPermission = true
                  viewModel.updateSettingNotificationsMaster(true)
                } else {
                  notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
              } else {
                hasNotificationPermission = true
                viewModel.updateSettingNotificationsMaster(true)
              }
            } else {
              viewModel.updateSettingNotificationsMaster(false)
            }
          }
        )

        // If enabled, roll out downward!
        androidx.compose.animation.AnimatedVisibility(
          visible = notificationsEnabled,
          enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
          exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 12.dp, top = 8.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )

            MangaSettingSwitchRow(
              title = "Telegram Import Alerts",
              description = "Notify me when messages from Telegram are ready to become tasks.",
              checked = stats.notifyOnSync,
              onCheckedChange = { viewModel.updateSettingNotifyOnSync(it) }
            )

            MangaSettingSwitchRow(
              title = "Focus Timer Alerts",
              description = "Let me know when a focus session is complete.",
              checked = stats.notifyOnFocus,
              onCheckedChange = { viewModel.updateSettingNotifyOnFocus(it) }
            )

            MangaSettingSwitchRow(
              title = "Daily Check-in Reminder",
              description = "Send a gentle daily reminder to come back and complete something.",
              checked = stats.dailyReminders,
              onCheckedChange = { viewModel.updateSettingReminders(it) }
            )
          }
        }
      }
    }

    // Gamification Difficulty section
    item {
      var showDifficultyInfo by remember { mutableStateOf(false) }
      var pendingDifficultyChange by remember { mutableStateOf<String?>(null) }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .mangaShadow(offset = 3.dp)
          .background(MaterialTheme.colorScheme.surface)
          .mangaBorder()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Difficulty level logo settings",
              tint = AnimeYellow,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "GAMEPLAY PACING",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.ExtraBold
            )
          }

          IconButton(
            onClick = { showDifficultyInfo = true },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Difficulty information rules",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        Text(
          text = "Choose your pacing. Easy gives massive point yields and discounts. Hard limits your gains and inflates costs - requiring true steel discipline.",
          style = MaterialTheme.typography.bodySmall,
          color = InkGrayDark,
          lineHeight = 14.sp
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .mangaBorder()
        ) {
          listOf("Easy", "Medium", "Hard").forEach { diffKey ->
            val isSelected = stats.difficulty == diffKey
            val btnColor = when (diffKey) {
              "Easy" -> AnimeGreen
              "Hard" -> AnimePurple
              else -> AnimeTeal
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (isSelected) btnColor else MaterialTheme.colorScheme.surface)
                .border(
                  width = if (isSelected) 0.dp else 0.5.dp,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                .clickable {
                  if (stats.difficulty != diffKey) {
                    pendingDifficultyChange = diffKey
                  }
                }
                .padding(vertical = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = diffKey.uppercase(),
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black
              )
            }
          }
        }

        // Selected Status Message
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .mangaBorder(width = 0.5.dp)
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(
                when (stats.difficulty) {
                  "Easy" -> AnimeGreen
                  "Hard" -> AnimePurple
                  else -> AnimeTeal
                }
              )
          )
          Text(
            text = "ACTIVE PACING: ${stats.difficulty.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 1. Info Dialog Explaining Rules
      if (showDifficultyInfo) {
        AlertDialog(
          onDismissRequest = { showDifficultyInfo = false },
          confirmButton = {
            TextButton(
              onClick = { showDifficultyInfo = false },
              modifier = Modifier.mangaBorder().background(Color.White).padding(horizontal = 12.dp)
            ) {
              Text("ACKNOWLEDGED", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          },
          shape = RectangleShape,
          title = {
            Text(
              "DIFFICULTY REWARD MATRIX",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = Color.Black
            )
          },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                "Earn rates & shop multipliers scale based on difficulty:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
              )

              TableMatrixRow(title = "Task Cleared:", easyVal = "15 PTS", medVal = "5 PTS", hardVal = "2 PTS")
              TableMatrixRow(title = "Focus session:", easyVal = "30 PTS", medVal = "15 PTS", hardVal = "5 PTS")
              TableMatrixRow(title = "Done Chapter:", easyVal = "100 PTS", medVal = "50 PTS", hardVal = "15 PTS")
              TableMatrixRow(title = "Streak Claim:", easyVal = "25 PTS", medVal = "10 PTS", hardVal = "3 PTS")
              TableMatrixRow(title = "Shop Cost:", easyVal = "0.5x", medVal = "1.0x", hardVal = "2.0x")
              
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                "Changing difficulty resets points to 0 to prevent exploitative scaling.",
                style = MaterialTheme.typography.bodySmall,
                color = AnimeRed,
                fontWeight = FontWeight.Bold
              )
            }
          },
          containerColor = Color.White,
          modifier = Modifier.mangaBorder(width = 3.dp)
        )
      }

      // 2. Clear points Confirmation Dialog
      if (pendingDifficultyChange != null) {
        val targetDiff = pendingDifficultyChange!!
        AlertDialog(
          onDismissRequest = { pendingDifficultyChange = null },
          confirmButton = {
            Button(
              onClick = {
                viewModel.changeDifficulty(targetDiff)
                pendingDifficultyChange = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = AnimeRed, contentColor = Color.White),
              shape = RectangleShape,
              modifier = Modifier.mangaBorder()
            ) {
              Text("YES, RESET XP & PACING", fontWeight = FontWeight.Bold)
            }
          },
          dismissButton = {
            TextButton(
              onClick = { pendingDifficultyChange = null },
              shape = RectangleShape,
              modifier = Modifier.mangaBorder().background(Color.White),
              colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
            ) {
              Text("CANCEL", fontWeight = FontWeight.Bold)
            }
          },
          shape = RectangleShape,
          title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Warning, "Warning reset icon", tint = AnimeRed, modifier = Modifier.size(24.dp))
              Text("RESET ALL XP POINTS?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
            }
          },
          text = {
            Text(
              "Changing difficulty to ${targetDiff.uppercase()} will permanently reset your XP Point Balance (currently ${stats.points} PTS) to 0.\n\nAre you sure you want to proceed with this reset?",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.Black
            )
          },
          containerColor = Color.White,
          modifier = Modifier.mangaBorder(width = 3.dp)
        )
      }
    }

    // Cloud & Telegram Sync Section
    item {
      RaitoCloudSyncPanel(viewModel = viewModel)
    }

    // Data & Export/Danger Actions
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .mangaShadow(offset = 3.dp)
          .background(MaterialTheme.colorScheme.surface)
          .mangaBorder()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "DATA & PRIVACY",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // Room Storage Status Panel
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(AnimeGreen.copy(alpha = 0.15f))
            .mangaBorder(color = AnimeGreen)
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "SAVED ON THIS DEVICE",
                style = MaterialTheme.typography.labelSmall,
                color = InkBlack,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Your buckets, tasks, streaks, and points are available offline.",
                style = MaterialTheme.typography.bodySmall,
                color = InkGrayDark,
                fontSize = 9.sp
              )
            }
            Box(
              modifier = Modifier
                .background(AnimeGreen)
                .mangaBorder(width = 1.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "OFFLINE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.sp
              )
            }
          }
        }

        // State Dialogs for Backup & Restore
        var showBackupConfirmDialog by remember { mutableStateOf(false) }
        var showRestoreConfirmDialog by remember { mutableStateOf(false) }
        var showResetConfirmDialog by remember { mutableStateOf(false) }

        val backupFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
          contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
          if (uri != null) {
            try {
              val jsonString = viewModel.exportJsonData()
              context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonString.toByteArray())
              }
              Toast.makeText(context, "BACKUP EXPORTED SUCCESSFULLY!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
              Toast.makeText(context, "Backup save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
          }
        }

        val restoreFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
          contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
          if (uri != null) {
            try {
              context.contentResolver.openInputStream(uri)?.use { input ->
                val jsonString = input.bufferedReader().use { it.readText() }
                viewModel.importJsonBackup(jsonString) { success ->
                  if (success) {
                    Toast.makeText(context, "Backup restored successfully.", Toast.LENGTH_LONG).show()
                  } else {
                    Toast.makeText(context, "Restore failed: the selected file is not valid.", Toast.LENGTH_LONG).show()
                  }
                }
              }
            } catch (e: Exception) {
              Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
          }
        }

        // CONFIRM DIALOGS
        if (showBackupConfirmDialog) {
          AlertDialog(
            onDismissRequest = { showBackupConfirmDialog = false },
            shape = RectangleShape,
            modifier = Modifier.mangaBorder(width = 3.dp),
            containerColor = MaterialTheme.colorScheme.background,
            title = {
              Text(
                text = "SAVE BACKUP",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
              )
            },
            text = {
              Text(
                text = "Save your buckets, tasks, streak, points, and settings to a backup file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
            },
            confirmButton = {
              Button(
                onClick = {
                  showBackupConfirmDialog = false
                  backupFileLauncher.launch("raito_backup_${System.currentTimeMillis()}.json")
                },
                shape = RectangleShape,
                modifier = Modifier.mangaBorder(1.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Text("EXPORT", fontWeight = FontWeight.Bold)
              }
            },
            dismissButton = {
              TextButton(onClick = { showBackupConfirmDialog = false }) {
                Text("CANCEL", fontWeight = FontWeight.Bold, color = InkGrayDark)
              }
            }
          )
        }

        if (showRestoreConfirmDialog) {
          AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            shape = RectangleShape,
            modifier = Modifier.mangaBorder(width = 3.dp),
            containerColor = MaterialTheme.colorScheme.background,
            title = {
              Text(
                text = "RESTORE BACKUP?",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                color = AnimeRed
              )
            },
            text = {
              Text(
                text = "Restoring a backup will replace your current buckets, tasks, points, and settings with the selected file. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
            },
            confirmButton = {
              Button(
                onClick = {
                  showRestoreConfirmDialog = false
                  restoreFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
                shape = RectangleShape,
                modifier = Modifier.mangaBorder(1.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnimeRed, contentColor = Color.White)
              ) {
                Text("WIPE & RESTORE", fontWeight = FontWeight.Bold)
              }
            },
            dismissButton = {
              TextButton(onClick = { showRestoreConfirmDialog = false }) {
                Text("CANCEL", fontWeight = FontWeight.Bold, color = InkGrayDark)
              }
            }
          )
        }

        if (showResetConfirmDialog) {
          AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            shape = RectangleShape,
            modifier = Modifier.mangaBorder(width = 3.dp),
            containerColor = MaterialTheme.colorScheme.background,
            title = {
              Text(
                text = "CONFIRM FACTORY RESET",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                color = AnimeRed
              )
            },
            text = {
              Text(
                text = "Are you sure you want to delete all chapters and tasks, restoring default manga companion presets?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
            },
            confirmButton = {
              Button(
                onClick = {
                  showResetConfirmDialog = false
                  viewModel.resetAllDatabaseData()
                  Toast.makeText(context, "All local data has been reset.", Toast.LENGTH_LONG).show()
                },
                shape = RectangleShape,
                modifier = Modifier.mangaBorder(1.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnimeRed, contentColor = Color.White)
              ) {
                Text("RESET EVERYTHING", fontWeight = FontWeight.Bold)
              }
            },
            dismissButton = {
              TextButton(onClick = { showResetConfirmDialog = false }) {
                Text("CANCEL", fontWeight = FontWeight.Bold, color = InkGrayDark)
              }
            }
          )
        }

        // Export/Save Backup File button
        Button(
          onClick = { showBackupConfirmDialog = true },
          shape = RectangleShape,
          modifier = Modifier
            .fillMaxWidth()
            .mangaBorder(),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SAVE BACKUP FILE",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "▶",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Restore Backup File button
        Button(
          onClick = { showRestoreConfirmDialog = true },
          shape = RectangleShape,
          modifier = Modifier
            .fillMaxWidth()
            .mangaBorder(),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "RESTORE FROM BACKUP",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "◀",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // RESET ALL DATA Button
        Button(
          onClick = { showResetConfirmDialog = true },
          shape = RectangleShape,
          modifier = Modifier
            .fillMaxWidth()
            .mangaBorder(color = AnimeRed),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = AnimeRed)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Reset stats delete icon",
              tint = AnimeRed,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "RESET ALL DATA",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = AnimeRed
            )
          }
        }
      }
    }

    item {
      val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(2.dp)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "RAITO ENGINE VERSION ${com.example.BuildConfig.VERSION_NAME}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.clickable {
            try {
              uriHandler.openUri("https://hamdi.dev.et")
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
        ) {
          Text(
            text = "Crafted with ⚔ by ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.61f),
            fontSize = 11.sp
          )
          Text(
            text = "Hamdi",
            style = MaterialTheme.typography.bodySmall,
            color = AnimePurple,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
          )
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}

@Composable
fun MangaSettingSwitchRow(
  title: String,
  description: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      if (description != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 12.sp
        )
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Switch custom brutalist box trigger
    val activeColor = AnimeGreen
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbActiveColor = InkBlack
    val thumbInactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Box(
      modifier = Modifier
        .size(width = 50.dp, height = 26.dp)
        .mangaBorder(width = 1.5.dp, color = MaterialTheme.colorScheme.onSurface)
        .background(if (checked) activeColor else inactiveColor)
        .clickable { onCheckedChange(!checked) }
        .padding(2.dp),
      contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(20.dp)
          .mangaBorder(width = 1.dp, color = MaterialTheme.colorScheme.onSurface)
          .background(if (checked) thumbActiveColor else thumbInactiveColor)
      )
    }
  }
}

@Composable
fun TableMatrixRow(
  title: String,
  easyVal: String,
  medVal: String,
  hardVal: String
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(text = title, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Black)
    Text(text = easyVal, modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall, color = AnimeGreen, fontWeight = FontWeight.Black)
    Text(text = medVal, modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall, color = AnimeTeal, fontWeight = FontWeight.Black)
    Text(text = hardVal, modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall, color = AnimePurple, fontWeight = FontWeight.Black)
  }
}

@Composable
fun RaitoCloudSyncPanel(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val stats by viewModel.stats.collectAsState()
  val serverState by viewModel.serverConnectionState.collectAsState()
  val pairingState by viewModel.pairingCodeState.collectAsState()
  val panelsState by viewModel.pendingPanelsState.collectAsState()
  val chapters by viewModel.chapters.collectAsState()
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val uriHandler = LocalUriHandler.current
  val scope = rememberCoroutineScope()

  var editableUrl by remember(stats.backendBaseUrl) { mutableStateOf(stats.backendBaseUrl) }
  var tokenInput by remember { mutableStateOf("") }
  var displayNameInput by remember { mutableStateOf("Android Companion") }
  var deviceLabelInput by remember { mutableStateOf("Raito Client Engine") }
  var expandedPanelId by remember { mutableStateOf<String?>(null) }

  // Auto scan pending panels on active connection
  LaunchedEffect(serverState) {
    if (serverState is RaitoViewModel.ServerConnectionState.Connected) {
      viewModel.fetchPendingPanels()
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = 3.dp)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Section Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Star, // Standard core star icon representation
          contentDescription = "Cloud connection icon",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "TELEGRAM SYNC",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
      }

      // Connectivity Badge indicator
      val (badgeText, badgeColor) = when (serverState) {
        is RaitoViewModel.ServerConnectionState.Connected -> "ONLINE" to AnimeGreen
        is RaitoViewModel.ServerConnectionState.Connecting -> "CONNECTING" to AnimeYellow
        is RaitoViewModel.ServerConnectionState.Error -> "ERROR" to AnimeRed
        else -> "OFFLINE" to InkGrayLight
      }

      Box(
        modifier = Modifier
          .background(badgeColor)
          .mangaBorder(1.dp)
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = badgeText,
          style = MaterialTheme.typography.labelSmall,
          color = Color.Black,
          fontWeight = FontWeight.Black,
          fontSize = 9.sp
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    )

    // Part 1: Actions based on current Server Connection State
    when (val state = serverState) {
      is RaitoViewModel.ServerConnectionState.Disconnected -> {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Text(
            text = "Use Telegram to capture quick task ideas, then bring them into your Raito buckets when you are ready.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
          )

          // Auto-registration button
          Button(
            onClick = {
              viewModel.registerDeviceOnServer(
                "https://raito.hamdi.dev.et",
                "Raito Companion",
                "Android Client"
              )
            },
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .mangaBorder(1.5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AnimePurple, contentColor = Color.White)
          ) {
            Text("SET UP TELEGRAM SYNC", fontWeight = FontWeight.Black)
          }

          Text(
            text = "Already linked on another device?",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = InkGrayDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
          )

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = tokenInput,
              onValueChange = { tokenInput = it },
              placeholder = { Text("Paste your device token") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder(1.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
              ),
              textStyle = MaterialTheme.typography.bodySmall
            )
            Button(
              onClick = {
                viewModel.testConnection("https://raito.hamdi.dev.et", tokenInput.trim())
              },
              enabled = tokenInput.isNotBlank(),
              shape = RectangleShape,
              modifier = Modifier
                .fillMaxWidth()
                .mangaBorder(1.5.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = AnimeTeal,
                contentColor = Color.Black,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            ) {
            Text("CONNECT WITH TOKEN", fontWeight = FontWeight.Black)
            }
          }
        }
      }

      is RaitoViewModel.ServerConnectionState.Connecting -> {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(AnimeYellow.copy(alpha = 0.1f))
            .mangaBorder(color = AnimeYellow)
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CircularProgressIndicator(color = AnimeYellow, modifier = Modifier.size(28.dp))
          Text(
            text = "CONNECTING TO RAITO SYNC...",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "This usually only takes a moment.",
            style = MaterialTheme.typography.bodySmall,
            color = InkGrayDark
          )
        }
      }

      is RaitoViewModel.ServerConnectionState.Connected -> {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          // Connected stats card
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(AnimeGreen.copy(alpha = 0.1f))
              .mangaBorder(color = AnimeGreen)
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "SYNC IS CONNECTED",
                  style = MaterialTheme.typography.labelSmall,
                  fontStyle = FontStyle.Italic,
                  fontWeight = FontWeight.Black,
                  color = AnimeGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                val tokenPreview = if (stats.backendDeviceToken.length > 8) stats.backendDeviceToken.takeLast(8) else stats.backendDeviceToken
                Text(
                  text = "Token ending in ...$tokenPreview",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Device name: ${stats.telegramDeviceName.ifBlank { "Raito Client" }}",
                  style = MaterialTheme.typography.bodySmall,
                  color = InkGrayDark
                )
              }
              
              // Disconnect button
              Button(
                onClick = { viewModel.disconnectServer() },
                shape = RectangleShape,
                modifier = Modifier.mangaBorder(0.5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnimeRed, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text("DISCONNECT", fontWeight = FontWeight.Bold, fontSize = 10.sp)
              }
            }
          }

          // Generate Bot Code (Linking) card
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .mangaBorder(width = 1.dp)
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "LINK TELEGRAM",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Generate a short code, then send it to the Raito Telegram bot to connect this device.",
              style = MaterialTheme.typography.bodySmall,
              color = InkGrayDark
            )
            Button(
              onClick = {
                try {
                  uriHandler.openUri("https://t.me/theraitobot")
                } catch (_: Exception) {
                  Toast.makeText(context, "Could not open Telegram link.", Toast.LENGTH_SHORT).show()
                }
              },
              shape = RectangleShape,
              modifier = Modifier
                .fillMaxWidth()
                .mangaBorder(1.dp),
              colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = Color.White)
            ) {
              Text("OPEN @THERAITOBOT", fontWeight = FontWeight.Bold)
            }

            when (val pairState = pairingState) {
              is RaitoViewModel.PairingCodeState.Idle -> {
                Button(
                  onClick = { viewModel.generatePairingCode() },
                  shape = RectangleShape,
                  modifier = Modifier
                    .fillMaxWidth()
                    .mangaBorder(1.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = AnimeTeal, contentColor = Color.Black)
                ) {
                  Text("GENERATE LINK CODE", fontWeight = FontWeight.Bold)
                }
              }

              is RaitoViewModel.PairingCodeState.Loading -> {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  CircularProgressIndicator(color = AnimeTeal, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Creating link code...", style = MaterialTheme.typography.bodySmall)
                }
              }

              is RaitoViewModel.PairingCodeState.Success -> {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .mangaBorder(1.dp)
                    .padding(12.dp)
                ) {
                  Text(
                    text = pairState.code,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                  )
                  Text(
                    text = "Expires in ${pairState.expiresInMinutes} minutes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AnimeRed,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Open the Raito bot in Telegram and send:",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                  )
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(MaterialTheme.colorScheme.surface)
                      .mangaBorder(0.5.dp)
                      .clickable {
                        clipboardManager.setText(AnnotatedString("/link ${pairState.code}"))
                        Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                      }
                      .padding(8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "/link ${pairState.code}   [TAP TO COPY]",
                      style = MaterialTheme.typography.labelSmall,
                      fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.secondary
                    )
                  }
                  TextButton(onClick = { viewModel.generatePairingCode() }) {
                    Text("Create a new code", color = AnimeTeal, fontWeight = FontWeight.Bold)
                  }
                }
              }

              is RaitoViewModel.PairingCodeState.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "Could not create a link code: ${pairState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnimeRed
                  )
                  Button(
                    onClick = { viewModel.generatePairingCode() },
                    colors = ButtonDefaults.buttonColors(containerColor = AnimeRed),
                    shape = RectangleShape
                  ) {
                    Text("TRY AGAIN", color = Color.White)
                  }
                }
              }
            }
          }

          // Part 3: Pending Remote Telegram Panels Section
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .mangaBorder(width = 1.dp)
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "TELEGRAM INBOX",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = AnimePink
              )

              IconButton(
                onClick = { viewModel.fetchPendingPanels() },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh panels list",
                  tint = AnimePink,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            Text(
              text = "Messages you send to Telegram appear here. Pick one and choose a bucket to turn it into a task.",
              style = MaterialTheme.typography.bodySmall,
              color = InkGrayDark
            )

            when (val panState = panelsState) {
              is RaitoViewModel.PendingPanelsState.Idle -> {
                // Auto fetches
              }

              is RaitoViewModel.PendingPanelsState.Loading -> {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  CircularProgressIndicator(color = AnimePink, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Checking for new Telegram messages...", style = MaterialTheme.typography.bodySmall)
                }
              }

              is RaitoViewModel.PendingPanelsState.Success -> {
                val list = panState.panels
                if (list.isEmpty()) {
                  EmptyStateCard(
                    icon = Icons.Default.Info,
                    title = "No Telegram Messages",
                    message = "Send a message to the Raito bot and it will appear here for import."
                  )
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    list.forEach { panel ->
                      val isExpanded = expandedPanelId == panel.remote_panel_id
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(MaterialTheme.colorScheme.background)
                          .mangaBorder(width = 1.dp)
                          .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Text(
                          text = panel.content,
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = "Received: ${panel.created_at.take(16).replace("T", " ")}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = InkGrayDark
                          )
                          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                              onClick = {
                                expandedPanelId = if (isExpanded) null else panel.remote_panel_id
                              },
                              shape = RectangleShape,
                              modifier = Modifier.height(26.dp).mangaBorder(0.5.dp),
                              colors = ButtonDefaults.buttonColors(
                                containerColor = if (isExpanded) AnimePurple else AnimeGreen,
                                contentColor = if (isExpanded) Color.White else Color.Black
                              ),
                              contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                              Text(
                                text = if (isExpanded) "CLOSE" else "CHOOSE BUCKET",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                              )
                            }
                            OutlinedButton(
                              onClick = { viewModel.discardPanel(panel) },
                              shape = RectangleShape,
                              modifier = Modifier.height(26.dp).mangaBorder(0.5.dp),
                              colors = ButtonDefaults.outlinedButtonColors(contentColor = AnimeRed),
                              contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                              Text(
                                "DISCARD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                              )
                            }
                          }
                        }

                        // Inline chapter selector for import destination
                        if (isExpanded) {
                          Column(
                            modifier = Modifier
                              .fillMaxWidth()
                              .mangaBorder()
                              .background(MaterialTheme.colorScheme.surface)
                              .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                          ) {
                            Text(
                              text = "Choose a bucket for this task:",
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = FontWeight.Black
                            )
                            if (chapters.isEmpty()) {
                              Text(
                                "Create a bucket first, then come back to import this message.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AnimeRed
                              )
                            } else {
                              chapters.forEach { chapter ->
                                Row(
                                  modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                      viewModel.importPanelAsTask(panel, chapter.id)
                                      expandedPanelId = null
                                    }
                                    .background(MaterialTheme.colorScheme.background)
                                    .mangaBorder(0.5.dp)
                                    .padding(8.dp),
                                  horizontalArrangement = Arrangement.SpaceBetween,
                                  verticalAlignment = Alignment.CenterVertically
                                ) {
                                  Text(
                                    text = chapter.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                  )
                                  Text(
                                    text = chapter.discipline.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.SemiBold
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
              }

              is RaitoViewModel.PendingPanelsState.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "Could not load Telegram messages: ${panState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnimeRed
                  )
                  Button(
                    onClick = { viewModel.fetchPendingPanels() },
                    colors = ButtonDefaults.buttonColors(containerColor = AnimeRed),
                    shape = RectangleShape
                  ) {
                    Text("TRY AGAIN", color = Color.White)
                  }
                }
              }
            }
          }

          // Logout Action
          Button(
            onClick = { viewModel.disconnectServer() },
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .mangaBorder(1.5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AnimeRed, contentColor = Color.White)
          ) {
            Text("DISCONNECT TELEGRAM SYNC", fontWeight = FontWeight.Black)
          }
        }
      }

      is RaitoViewModel.ServerConnectionState.Error -> {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(AnimeRed.copy(alpha = 0.08f))
            .mangaBorder(color = AnimeRed)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "SYNC CONNECTION ERROR",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = AnimeRed
          )
          Text(
            text = state.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 14.sp
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                if (stats.backendDeviceToken.isNotBlank()) {
                  viewModel.testConnection(stats.backendBaseUrl, stats.backendDeviceToken)
                } else {
                  viewModel.disconnectServer()
                }
              },
              shape = RectangleShape,
              modifier = Modifier
                .weight(1f)
                .mangaBorder(1.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AnimeRed, contentColor = Color.White)
            ) {
              Text("TRY AGAIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { viewModel.disconnectServer() },
              shape = RectangleShape,
              modifier = Modifier
                .weight(1f)
                .mangaBorder(1.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
              Text("START OVER", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}
