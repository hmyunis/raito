package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import com.example.ui.viewmodel.RaitoViewModel
import com.example.data.database.CustomAvatarEntity
import com.example.ui.theme.*
import com.example.util.CompanionRegistry
import java.text.DecimalFormat

private data class AvatarExpressionPreview(
  val title: String,
  val expressions: List<Pair<String, String?>>
)

@Composable
fun ResetProgressDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(0.4f))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth(0.9f)
          .mangaShadow(offset = 6.dp)
          .background(BgPaperLight)
          .mangaBorder(width = 3.dp)
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Warning title header with icon
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning reset icon",
            tint = AnimeRed,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "RESET PROGRESS?",
            style = MaterialTheme.typography.displayMedium,
            color = InkBlack,
            fontWeight = FontWeight.ExtraBold
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(InkBlack)
        )

        // dialog body text
        Text(
          text = "Reset this chapter’s chibi progress? This will return the character to Line Art stage, but your completed tasks will stay unchanged.",
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = InkGrayDark,
          lineHeight = 18.sp
        )

        // Actions
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onConfirm,
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .mangaShadow(offset = 2.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(containerColor = AnimeRed)
          ) {
            Text(
              text = "RESET PROGRESS",
              color = Color.White,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Button(
            onClick = onDismiss,
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = InkBlack)
          ) {
            Text(
              text = "CANCEL",
              color = InkBlack,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun AppUpdateDialog(
  state: RaitoViewModel.AppUpdateUiState,
  onDownloadClick: () -> Unit,
  onDismiss: () -> Unit,
  onInstallClick: () -> Unit,
  onOpenInstallSettings: () -> Unit,
  onOpenInBrowser: () -> Unit
) {
  val info = state.info ?: return

  Dialog(
    onDismissRequest = {
      if (!state.isMandatory) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = !state.isMandatory,
      dismissOnClickOutside = !state.isMandatory
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(0.5f))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth(0.94f)
          .mangaShadow(offset = 6.dp)
          .background(BgPaperLight)
          .mangaBorder(width = 3.dp)
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = "App update",
            tint = AnimePurple,
            modifier = Modifier.size(24.dp)
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = if (state.isMandatory) "UPDATE REQUIRED" else "UPDATE AVAILABLE",
              style = MaterialTheme.typography.displayMedium,
              fontWeight = FontWeight.ExtraBold,
              color = InkBlack
            )
            Text(
              text = info.title.uppercase(),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = InkGrayDark
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(InkBlack)
        )

        Text(
          text = buildString {
            append("Version ${info.latestVersion} is ready to install.")
            if (state.isMandatory) {
              append(" This release is required before you can continue using the app.")
            } else {
              append(" You can update now or continue and be reminded next launch.")
            }
          },
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = InkGrayDark,
          lineHeight = 18.sp
        )

        if (info.releaseNotes.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "WHAT'S NEW",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.ExtraBold,
              color = InkBlack
            )
            info.releaseNotes.take(5).forEach { note ->
              Text(
                text = "• $note",
                style = MaterialTheme.typography.bodySmall,
                color = InkGrayDark,
                lineHeight = 18.sp
              )
            }
          }
        }

        when (val downloadState = state.downloadState) {
          RaitoViewModel.AppUpdateDownloadState.Idle -> Unit
          RaitoViewModel.AppUpdateDownloadState.Preparing -> {
            DownloadProgressPanel(progress = null, status = "Preparing secure download…")
          }
          RaitoViewModel.AppUpdateDownloadState.Queued -> {
            DownloadProgressPanel(progress = 0, status = "Queued for download…")
          }
          is RaitoViewModel.AppUpdateDownloadState.InProgress -> {
            val progressLabel = downloadState.percent?.let { "$it%" } ?: "Starting"
            val sizeLabel = formatByteProgress(downloadState.downloadedBytes, downloadState.totalBytes)
            DownloadProgressPanel(
              progress = downloadState.percent,
              status = "Downloading update $progressLabel",
              detail = sizeLabel
            )
          }
          RaitoViewModel.AppUpdateDownloadState.ReadyToInstall -> {
            DownloadProgressPanel(progress = 100, status = "Update package downloaded. Ready to install.")
          }
          RaitoViewModel.AppUpdateDownloadState.Installing -> {
            DownloadProgressPanel(progress = 100, status = "Opening installer…")
          }
          RaitoViewModel.AppUpdateDownloadState.InstallPermissionRequired -> {
            DownloadProgressPanel(
              progress = 100,
              status = "Allow app installs for Raito to finish the update."
            )
          }
          is RaitoViewModel.AppUpdateDownloadState.Failed -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(AnimeRed.copy(alpha = 0.12f))
                .mangaBorder(color = AnimeRed)
                .padding(12.dp)
            ) {
              Text(
                text = downloadState.message,
                style = MaterialTheme.typography.bodySmall,
                color = InkBlack,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          when (state.downloadState) {
            RaitoViewModel.AppUpdateDownloadState.InstallPermissionRequired -> {
              Button(
                onClick = onOpenInstallSettings,
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .mangaShadow(offset = 2.dp)
                  .mangaBorder(),
                colors = ButtonDefaults.buttonColors(containerColor = AnimePurple)
              ) {
                Text("ALLOW INSTALLS", color = Color.White, fontWeight = FontWeight.Bold)
              }
              Button(
                onClick = onInstallClick,
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .mangaBorder(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = InkBlack)
              ) {
                Text("TRY INSTALL AGAIN", fontWeight = FontWeight.Bold)
              }
            }
            RaitoViewModel.AppUpdateDownloadState.ReadyToInstall -> {
              Button(
                onClick = onInstallClick,
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .mangaShadow(offset = 2.dp)
                  .mangaBorder(),
                colors = ButtonDefaults.buttonColors(containerColor = AnimePurple)
              ) {
                Text("INSTALL UPDATE", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
            is RaitoViewModel.AppUpdateDownloadState.InProgress,
            RaitoViewModel.AppUpdateDownloadState.Preparing,
            RaitoViewModel.AppUpdateDownloadState.Queued,
            RaitoViewModel.AppUpdateDownloadState.Installing -> Unit
            else -> {
              Button(
                onClick = onDownloadClick,
                shape = RectangleShape,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .mangaShadow(offset = 2.dp)
                  .mangaBorder(),
                colors = ButtonDefaults.buttonColors(containerColor = AnimePurple)
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                  Text(
                    text = if (state.downloadState is RaitoViewModel.AppUpdateDownloadState.Failed) {
                      "RETRY DOWNLOAD"
                    } else {
                      "DOWNLOAD UPDATE"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Button(
            onClick = onOpenInBrowser,
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = InkBlack)
          ) {
            Text("OPEN DOWNLOAD IN BROWSER", fontWeight = FontWeight.Bold)
          }

          if (!state.isMandatory) {
            Button(
              onClick = onDismiss,
              shape = RectangleShape,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .mangaBorder(),
              colors = ButtonDefaults.buttonColors(containerColor = BgPaperLight, contentColor = InkBlack)
            ) {
              Text("LATER", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DownloadProgressPanel(
  progress: Int?,
  status: String,
  detail: String? = null
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White)
      .mangaBorder()
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = status,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.ExtraBold,
      color = InkBlack
    )

    if (progress != null) {
      LinearProgressIndicator(
        progress = { progress / 100f },
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp),
        color = AnimePurple,
        trackColor = InkGrayLight
      )
      Text(
        text = "$progress%",
        style = MaterialTheme.typography.bodySmall,
        color = InkGrayDark,
        fontWeight = FontWeight.Bold
      )
    } else {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp),
        color = AnimePurple,
        trackColor = InkGrayLight
      )
    }

    if (!detail.isNullOrBlank()) {
      Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = InkGrayDark
      )
    }
  }
}

private fun formatByteProgress(downloadedBytes: Long, totalBytes: Long?): String {
  val formatter = DecimalFormat("0.0")
  val downloadedMb = downloadedBytes / (1024f * 1024f)
  return if (totalBytes != null && totalBytes > 0) {
    val totalMb = totalBytes / (1024f * 1024f)
    "${formatter.format(downloadedMb)} MB / ${formatter.format(totalMb)} MB"
  } else {
    "${formatter.format(downloadedMb)} MB downloaded"
  }
}

@Composable
private fun AvatarExpressionsPreviewDialog(
  preview: AvatarExpressionPreview,
  onDismiss: () -> Unit
) {
  val dialogSurface = MaterialTheme.colorScheme.surface
  val dialogCard = MaterialTheme.colorScheme.surfaceVariant
  val dialogText = MaterialTheme.colorScheme.onSurface
  val dialogSubtle = MaterialTheme.colorScheme.onSurfaceVariant

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(0.5f))
        .clickable { onDismiss() }
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .mangaShadow(offset = 6.dp)
          .background(dialogSurface)
          .mangaBorder()
          .clickable(enabled = false, onClick = {})
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "${preview.title.uppercase()} PREVIEWS",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = dialogText
        )

        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(preview.expressions) { (label, uri) ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .mangaBorder()
                .background(dialogCard)
                .padding(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .aspectRatio(1f)
                  .mangaBorder(1.dp)
                  .background(dialogSurface),
                contentAlignment = Alignment.Center
              ) {
                if (uri != null) {
                  AsyncImage(
                    model = uri,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(0.82f)
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = label,
                    tint = dialogSubtle,
                    modifier = Modifier.size(34.dp)
                  )
                }
              }
              Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = dialogText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }

        Button(
          onClick = onDismiss,
          shape = RectangleShape,
          colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .mangaBorder()
        ) {
          Text("CLOSE PREVIEW", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun DeleteConfirmationDialog(
  title: String,
  message: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(0.4f))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth(0.9f)
          .mangaShadow(offset = 6.dp)
          .background(BgPaperLight)
          .mangaBorder(width = 3.dp)
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning delete icon",
            tint = AnimeRed,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = InkBlack,
            fontWeight = FontWeight.ExtraBold
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(InkBlack)
        )

        Text(
          text = message,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = InkGrayDark,
          lineHeight = 18.sp
        )

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onConfirm,
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .mangaShadow(offset = 2.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(containerColor = AnimeRed)
          ) {
            Text(
              text = "DELETE PERMANENTLY",
              color = Color.White,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Button(
            onClick = onDismiss,
            shape = RectangleShape,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .mangaBorder(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = InkBlack)
          ) {
            Text(
              text = "CANCEL",
              color = InkBlack,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun SwitchAvatarDialog(
  viewModel: RaitoViewModel,
  onDismiss: () -> Unit,
  onGoToShop: () -> Unit
) {
  val stats by viewModel.stats.collectAsState()
  val craftCost = (1000 * viewModel.getShopPriceMultiplier()).toInt()
  val canAfford = stats.points >= craftCost
  val customAvatars by viewModel.customAvatars.collectAsState()
  val dialogSurface = MaterialTheme.colorScheme.surface
  val dialogCard = MaterialTheme.colorScheme.surfaceVariant
  val dialogText = MaterialTheme.colorScheme.onSurface
  val dialogSubtle = MaterialTheme.colorScheme.onSurfaceVariant

  var showCreateForm by remember { mutableStateOf(false) }
  var avatarToDelete by remember { mutableStateOf<CustomAvatarEntity?>(null) }
  var expressionPreview by remember { mutableStateOf<AvatarExpressionPreview?>(null) }

  // State in the Creation Form
  var avatarName by remember { mutableStateOf("") }
  var neutralUri by remember { mutableStateOf<Uri?>(null) }
  var happyUri by remember { mutableStateOf<Uri?>(null) }
  var focusUri by remember { mutableStateOf<Uri?>(null) }
  var sadUri by remember { mutableStateOf<Uri?>(null) }
  var completedUri by remember { mutableStateOf<Uri?>(null) }

  var pendingExpression by remember { mutableStateOf<String?>(null) }

  val pickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      when (pendingExpression) {
        "neutral" -> neutralUri = uri
        "happy" -> happyUri = uri
        "focus" -> focusUri = uri
        "sad" -> sadUri = uri
        "completed" -> completedUri = uri
      }
    }
  }

  if (avatarToDelete != null) {
    DeleteConfirmationDialog(
      title = "DELETE CUSTOM AVATAR?",
      message = "Are you sure you want to permanently delete \"${avatarToDelete!!.name.uppercase()}\"? This action cannot be undone.",
      onConfirm = {
        viewModel.deleteCustomAvatar(avatarToDelete!!)
        avatarToDelete = null
      },
      onDismiss = { avatarToDelete = null }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(0.4f))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.85f)
          .mangaShadow(offset = 6.dp)
          .background(dialogSurface)
          .mangaBorder(width = 3.dp)
          .padding(16.dp)
      ) {
        // Title header
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
              imageVector = Icons.Default.Face,
              contentDescription = "Avatar switch icon",
              tint = AnimePurple,
              modifier = Modifier.size(28.dp)
            )
            Text(
              text = if (showCreateForm) "CREATE CHIBI" else "SWITCH AVATAR",
              style = MaterialTheme.typography.displayMedium,
              color = dialogText,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 20.sp
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Dismiss",
              tint = dialogText
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(3.dp)
            .background(dialogText)
        )

        // Switchable Content Body
        if (!showCreateForm) {
          // Standard List of Avatars (Presets + Customs)
          Column(
            modifier = Modifier
              .weight(1f)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Point balance indicator in dialog
            Box(
              modifier = Modifier
                .align(Alignment.End)
                .background(dialogCard)
                .mangaBorder()
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "${stats.points} PTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = dialogText
              )
            }

            // Unlocked Preset Companions Section
            Text(
              text = "PRESET COMPANIONS",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = dialogText
            )

            val presets = listOf(
              Triple("Cyber", "Cyber Assistant", CompanionRegistry.CYBER.neutral),
              Triple("Knight", "Tiny Knight", CompanionRegistry.KNIGHT.neutral),
              Triple("Scholar", "Studio Artist", CompanionRegistry.SCHOLAR.neutral),
              Triple("Ranger", "Shadow Ranger", CompanionRegistry.RANGER.neutral),
              Triple("Dragon", "Dragon Keeper", CompanionRegistry.DRAGON.neutral)
            )

            presets.forEach { (id, name, imgUrl) ->
              val isUnlocked = stats.unlockedCompanions.split(",").contains(id) || id == "Cyber" || (id == "Scholar" && stats.unlockedCompanions.split(",").contains("Artist"))
              val isEquipped = stats.activeCompanionId == id || (id == "Scholar" && stats.activeCompanionId == "Artist")
              val expressions = CompanionRegistry.getExpressions(id)
              
              val basePrice = when(id) {
                "Knight" -> 200
                "Scholar" -> 500
                else -> 0
              }
              val calculatedPrice = (basePrice * viewModel.getShopPriceMultiplier()).toInt()

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .mangaBorder()
                  .background(if (isEquipped) AnimeYellow.copy(alpha = 0.18f) else dialogCard)
                  .clickable {
                    if (isUnlocked) {
                      viewModel.equipCompanion(id)
                    } else {
                      onDismiss()
                      onGoToShop()
                    }
                  }
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(50.dp)
                    .mangaBorder(width = 1.dp)
                    .background(dialogSurface)
                    .clickable {
                      expressionPreview = AvatarExpressionPreview(
                        title = name,
                        expressions = listOf(
                          "NEUTRAL" to expressions.neutral,
                          "HAPPY" to expressions.happy,
                          "FOCUS" to expressions.focus,
                          "SAD" to expressions.sad,
                          "COMPLETED" to expressions.completed
                        )
                      )
                    },
                  contentAlignment = Alignment.Center
                ) {
                  AsyncImage(
                    model = imgUrl,
                    contentDescription = name,
                    modifier = Modifier.size(45.dp)
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = dialogText
                  )
                  Text(
                    text = if (isUnlocked) "UNLOCKED" else "LOCKED ($calculatedPrice PTS) - GO TO SHOP",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnlocked) AnimeGreen else AnimeRed,
                    fontWeight = FontWeight.Bold
                  )
                }

                if (isEquipped) {
                  Text(
                    text = "ACTIVE",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AnimePurple
                  )
                } else if (!isUnlocked) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Locked",
                    tint = dialogSubtle
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Uploaded Avatars Section
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "YOUR CUSTOM AVATARS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = dialogText
              )

              Text(
                text = "(${customAvatars.size})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = dialogSubtle
              )
            }

            if (customAvatars.isEmpty()) {
              EmptyStateCard(
                icon = Icons.Default.Face,
                title = "No Custom Avatars",
                message = "Create one with your own image or GIF set whenever you want a personal companion."
              )
            } else {
              customAvatars.forEach { avatar ->
                val isEquipped = stats.activeCompanionId == "custom_${avatar.id}"
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .mangaBorder()
                    .background(if (isEquipped) AnimeYellow.copy(alpha = 0.18f) else dialogCard)
                    .clickable { viewModel.equipCompanion("custom_${avatar.id}") }
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(50.dp)
                      .mangaBorder(width = 1.dp)
                      .background(dialogSurface)
                      .clickable {
                        expressionPreview = AvatarExpressionPreview(
                          title = avatar.name.removePrefix("[Chibi] "),
                          expressions = listOf(
                            "NEUTRAL" to avatar.neutralPath,
                            "HAPPY" to avatar.happyPath,
                            "FOCUS" to avatar.focusPath,
                            "SAD" to avatar.sadPath,
                            "COMPLETED" to avatar.completedPath
                          )
                        )
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    AsyncImage(
                      model = avatar.neutralPath ?: CompanionRegistry.CYBER.neutral,
                      contentDescription = avatar.name,
                      modifier = Modifier.size(45.dp)
                    )
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = avatar.name.uppercase(),
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.ExtraBold,
                      color = dialogText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "CUSTOM CHIBI AVATAR",
                      style = MaterialTheme.typography.bodySmall,
                      color = AnimePurple,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  if (isEquipped) {
                    Text(
                      text = "ACTIVE",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.ExtraBold,
                      color = AnimePurple
                    )
                  }

                  IconButton(onClick = { avatarToDelete = avatar }) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete avatar",
                      tint = AnimeRed
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Create Custom Avatar Toggle
          Button(
            onClick = { showCreateForm = true },
            enabled = canAfford,
            shape = RectangleShape,
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = AnimePurple,
              contentColor = Color.White,
              disabledContainerColor = InkGrayLight,
              disabledContentColor = InkGrayDark
            ),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .mangaShadow(offset = if (canAfford) 2.dp else 0.dp)
              .mangaBorder()
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (canAfford) Icons.Default.Add else Icons.Default.Warning,
                contentDescription = null
              )
              Text(
                text = if (canAfford) "CREATE CUSTOM AVATAR" else "NEED $craftCost PTS TO CREATE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        } else {
          CustomAvatarForm(
            avatarName = avatarName,
            onNameChange = { avatarName = it },
            neutralUri = neutralUri,
            happyUri = happyUri,
            focusUri = focusUri,
            sadUri = sadUri,
            completedUri = completedUri,
            onSelectImageClick = {
              pendingExpression = it
              pickerLauncher.launch("image/*")
            },
            onSave = {
              if (avatarName.trim().isNotEmpty() && neutralUri != null && canAfford) {
                viewModel.createCustomAvatar(
                  name = "[Chibi] " + avatarName,
                  neutralUri = neutralUri,
                  happyUri = happyUri,
                  focusUri = focusUri,
                  sadUri = sadUri,
                  completedUri = completedUri,
                  cost = craftCost
                )
                showCreateForm = false
              }
            },
            onDiscard = { showCreateForm = false },
            saveButtonText = "SUBMIT & CRAFT (-$craftCost PTS)",
            saveEnabledOverride = canAfford
          )
        }
      }
    }
  }

  if (expressionPreview != null) {
    AvatarExpressionsPreviewDialog(
      preview = expressionPreview!!,
      onDismiss = { expressionPreview = null }
    )
  }
}
