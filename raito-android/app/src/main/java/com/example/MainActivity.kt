package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.ui.screens.*
import com.example.ui.theme.AnimePurple
import com.example.ui.theme.AnimeGreen
import com.example.ui.theme.AnimeYellow
import com.example.ui.theme.InkBlack
import com.example.ui.theme.RaitoTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RaitoViewModel
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Set up GIF-supporting Coil ImageLoader globally
    val imageLoader = ImageLoader.Builder(this)
      .components {
        if (Build.VERSION.SDK_INT >= 28) {
          add(ImageDecoderDecoder.Factory())
        } else {
          add(GifDecoder.Factory())
        }
      }
      .build()
    Coil.setImageLoader(imageLoader)

    enableEdgeToEdge(
      statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK),
      navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
    )
    setContent {
      val viewModel: RaitoViewModel = viewModel()
      val stats by viewModel.stats.collectAsState()
      val activeScreen by viewModel.activeScreen.collectAsState()
      val showResetProgressDialog by viewModel.showResetProgressDialog.collectAsState()
      val appUpdateUiState by viewModel.appUpdateUiState.collectAsState()

      RaitoTheme(
        themeMode = stats.themeMode,
        scale = stats.typographyScale
      ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val context = androidx.compose.ui.platform.LocalContext.current

        LaunchedEffect(Unit) {
          viewModel.uiEvents.collect { event ->
            when (event) {
              is UiEvent.ShowUndoSnackbar -> {
                val canUndo = event.taskId != -1
                val result = snackbarHostState.showSnackbar(
                  message = event.message,
                  actionLabel = if (canUndo) "UNDO" else null,
                  duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed && canUndo) {
                  viewModel.undoTaskCompletion(event.taskId)
                }
              }
            }
          }
        }

        LaunchedEffect(stats.notificationsMasterEnabled, stats.dailyReminders) {
          if (
            stats.notificationsMasterEnabled &&
            stats.dailyReminders &&
            NotificationHelper.canPostNotifications(context)
          ) {
            NotificationHelper.scheduleDailyReminder(context)
          } else {
            NotificationHelper.cancelDailyReminder(context)
          }
        }

        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
          Surface(
            modifier = Modifier
              .fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background
          ) {
            ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
              ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onBackground,
                drawerShape = RectangleShape,
                modifier = Modifier
                  .width(280.dp)
                  .fillMaxHeight()
                  .mangaBorder(width = 3.dp, color = MaterialTheme.colorScheme.onBackground)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 24.dp)
                ) {
                  // Drawer Header
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(54.dp)
                        .mangaBorder(width = 2.5.dp, color = MaterialTheme.colorScheme.onBackground)
                        .background(MaterialTheme.colorScheme.surface),
                      contentAlignment = Alignment.Center
                    ) {
                      androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_app_icon_padded),
                        contentDescription = "Raito Studio Launcher Icon",
                        modifier = Modifier
                          .fillMaxSize()
                          .padding(2.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                      )
                    }

                    Column {
                      Text(
                        text = "RAITO STUDIO",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "Ink your progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.61f),
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))
                  // Heavy Divider
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(3.dp)
                      .background(MaterialTheme.colorScheme.onBackground)
                  )
                  Spacer(modifier = Modifier.height(16.dp))

                  // Navigation Items
                  val navItems = listOf(
                    Triple(AppScreen.HOME, "STUDIO HOME", Icons.Default.Home),
                    Triple(AppScreen.BUCKETS, "CHAPTER BUCKETS", Icons.Default.Layers),
                    Triple(AppScreen.FOCUS, "FOCUS ROOM", Icons.Default.Timer),
                    Triple(AppScreen.PROGRESS, "STATS & PROGRESS", Icons.Default.Assessment),
                    Triple(AppScreen.SHOP, "COMPANION SHOP", Icons.Default.ShoppingCart),
                    Triple(AppScreen.SETTINGS, "SYSTEM SETTINGS", Icons.Default.Settings)
                  )

                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    navItems.forEach { (screen, label, icon) ->
                      val isSelected = activeScreen == screen
                      val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                      val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                      
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(bg)
                          .then(if (isSelected) Modifier.mangaBorder(width = 1.5.dp, color = MaterialTheme.colorScheme.onBackground) else Modifier)
                          .clickable {
                            viewModel.navigateTo(screen)
                            scope.launch { drawerState.close() }
                          }
                          .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = icon,
                          contentDescription = label,
                          tint = textCol,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                          text = label,
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.ExtraBold,
                          color = textCol
                        )
                      }
                    }
                  }

                  // Drawer Bottom Segment: Night Mode Toggle & Stats Info
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    // Separator line
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                    )

                    // Night Mode Toggle custom switch row
                    val isNight = stats.themeMode == "Dark"
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .mangaBorder()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                          viewModel.updateSettingTheme(if (isNight) "Light" else "Dark")
                        }
                        .padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = if (isNight) Icons.Default.Star else Icons.Default.Refresh,
                          contentDescription = "Theme mode icon",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "NIGHT MODE",
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                      }

                      // Brutalist switch indicator
                      Box(
                        modifier = Modifier
                          .size(width = 44.dp, height = 22.dp)
                          .mangaBorder(width = 1.dp, color = MaterialTheme.colorScheme.onSurface)
                          .background(if (isNight) AnimeGreen else MaterialTheme.colorScheme.surfaceVariant)
                          .padding(2.dp),
                        contentAlignment = if (isNight) Alignment.CenterEnd else Alignment.CenterStart
                      ) {
                        Box(
                          modifier = Modifier
                            .fillMaxHeight()
                            .width(16.dp)
                            .mangaBorder(width = 0.8.dp, color = MaterialTheme.colorScheme.onSurface)
                            .background(if (isNight) InkBlack else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        )
                      }
                    }

                    // Points Indicator capsule
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(AnimeYellow.copy(alpha = 0.15f))
                        .mangaBorder(color = AnimeYellow)
                        .padding(10.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                      ) {
                        Icon(
                          imageVector = Icons.Default.Star,
                          contentDescription = "Points indicator icon",
                          tint = AnimeYellow,
                          modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "SAVED: ${stats.points} PTS",
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onBackground
                        )
                      }
                    }

                    // Author & website block
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                      Text(
                        text = "VERSION ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
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
                          text = "Crafted by ",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                          fontSize = 10.sp
                        )
                        Text(
                          text = "Hamdi",
                          style = MaterialTheme.typography.bodySmall,
                          color = AnimePurple,
                          fontWeight = FontWeight.Black,
                          fontSize = 11.sp,
                          textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                      }
                    }
                  }
                }
              }
            }
          ) {
            Box(modifier = Modifier.fillMaxSize()) {
              Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                  // Hide topbar on Milestone Celebrating view to allow full-bleed impact
                  if (activeScreen != AppScreen.MILESTONE) {
                    val customAvatars by viewModel.customAvatars.collectAsState()
                    RaitoTopBar(
                      activeScreen = activeScreen,
                      points = stats.points,
                      companionUrl = viewModel.getActiveAvatarUrl("neutral"),
                      onMenuClick = {
                        scope.launch {
                          drawerState.open()
                        }
                      },
                      onAddChapterClick = { viewModel.navigateTo(AppScreen.NEW_CHAPTER) },
                      onNavigateHome = { viewModel.navigateTo(AppScreen.HOME) },
                      onPointsClick = { viewModel.navigateTo(AppScreen.SHOP) }
                    )
                  }
                },
                bottomBar = {
                  // Transactional screens suppress BottomNavBar
                  val hideNavigationBar = activeScreen == AppScreen.NEW_CHAPTER || activeScreen == AppScreen.MILESTONE || activeScreen == AppScreen.NEW_TASK
                  if (!hideNavigationBar) {
                    RaitoBottomNavBar(
                      activeScreen = activeScreen,
                      onTabSelected = { viewModel.navigateTo(it) }
                    )
                  }
                },
                floatingActionButton = {
                  if (activeScreen == AppScreen.HOME) {
                    val infiniteTransition = rememberInfiniteTransition(label = "fab_float_transition")
                    val fabTranslationY by infiniteTransition.animateFloat(
                      initialValue = 0f,
                      targetValue = -5f,
                      animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "fab_translation_y"
                    )

                    FloatingActionButton(
                      onClick = { viewModel.navigateTo(AppScreen.NEW_TASK) },
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                      shape = RectangleShape,
                      modifier = Modifier
                        .padding(bottom = 16.dp, end = 8.dp)
                        .mangaShadow(offset = 3.dp)
                        .mangaBorder()
                        .graphicsLayer { translationY = fabTranslationY }
                    ) {
                      Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick add task"
                      )
                    }
                  }
                }
              ) { innerPadding ->
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                ) {
                  when (activeScreen) {
                    AppScreen.HOME -> HomeView(viewModel = viewModel, onNavigateToShop = { viewModel.navigateTo(AppScreen.SHOP) })
                    AppScreen.BUCKETS -> BucketsView(viewModel = viewModel)
                    AppScreen.FOCUS -> FocusView(viewModel = viewModel)
                    AppScreen.PROGRESS -> ProgressView(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsView(viewModel = viewModel)
                    AppScreen.NEW_CHAPTER -> NewChapterView(viewModel = viewModel)
                    AppScreen.MILESTONE -> MilestoneView(viewModel = viewModel)
                    AppScreen.NEW_TASK -> NewTaskView(viewModel = viewModel)
                    AppScreen.SHOP -> ShopView(viewModel = viewModel)
                  }
                }
              }

              // Global modal warning resetting overlays
              if (showResetProgressDialog) {
                ResetProgressDialog(
                  onConfirm = { viewModel.confirmResetChapterProgress() },
                  onDismiss = { viewModel.showResetProgressDialog.value = false }
                )
              }

              if (appUpdateUiState.isVisible && appUpdateUiState.info != null) {
                AppUpdateDialog(
                  state = appUpdateUiState,
                  onDownloadClick = { viewModel.startAppUpdateDownload() },
                  onDismiss = { viewModel.dismissAppUpdatePrompt() },
                  onInstallClick = { viewModel.retryAppUpdateInstall() },
                  onOpenInstallSettings = { viewModel.openAppUpdateInstallSettings() },
                  onOpenInBrowser = { viewModel.openAppUpdateInBrowser() }
                )
              }

              // Visual celebration particle feedback
              ConfettiOverlay(triggerFlow = viewModel.confettiTrigger)
            }
          }
          }
        }
      }
    }
  }
}

@Composable
fun RaitoTopBar(
  activeScreen: AppScreen,
  points: Int,
  companionUrl: String,
  onMenuClick: () -> Unit,
  onAddChapterClick: () -> Unit,
  onNavigateHome: () -> Unit,
  onPointsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Menu Drawer Left Trigger
      Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = "Main menu drawer",
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
          .size(24.dp)
          .clickable { onMenuClick() }
      )

      RaitoAppBarLogo(
        modifier = Modifier
          .weight(1f),
        onClick = onNavigateHome
      )

      // Context-aware Action Badge on the right
      when (activeScreen) {
        AppScreen.NEW_CHAPTER, AppScreen.NEW_TASK -> {
          // Empty or close button
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close form",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
              .size(24.dp)
              .clickable { onNavigateHome() }
          )
        }
        AppScreen.FOCUS -> {
          // Empty or points only for focus
          val scale = remember { Animatable(1f) }
          var prevPoints by remember { mutableStateOf(points) }
          LaunchedEffect(points) {
            if (points > prevPoints) {
              scale.animateTo(1.35f, animationSpec = tween(300, easing = FastOutSlowInEasing))
              scale.animateTo(0.85f, animationSpec = tween(300, easing = EaseInOutSine))
              scale.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessLow))
            }
            prevPoints = points
          }

          Box(
            modifier = Modifier
              .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
              }
              .mangaShadow(offset = 2.dp)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder()
              .clickable { onPointsClick() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Points indicator",
                tint = AnimeYellow,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "$points PTS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
              )
            }
          }
        }
        else -> {
          // Action standard PTS badge or ADD Chapter
          val scale = remember { Animatable(1f) }
          var prevPoints by remember { mutableStateOf(points) }
          LaunchedEffect(points) {
            if (points > prevPoints) {
              scale.animateTo(1.35f, animationSpec = tween(300, easing = FastOutSlowInEasing))
              scale.animateTo(0.85f, animationSpec = tween(300, easing = EaseInOutSine))
              scale.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessLow))
            }
            prevPoints = points
          }

          Box(
            modifier = Modifier
              .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
              }
              .mangaShadow(offset = 2.dp)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder()
              .clickable { onPointsClick() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Points indicator",
                tint = AnimeYellow,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "$points PTS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }

    // A single, heavy bottom boundary line
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(3.dp)
        .background(MaterialTheme.colorScheme.onBackground)
    )
  }
}

@Composable
private fun RaitoAppBarLogo(
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val logoFont = FontFamily(Font(com.example.R.font.bangers_regular))
  val textStyle = MaterialTheme.typography.displayLarge.copy(
    fontFamily = logoFont,
    fontSize = 30.sp,
    letterSpacing = 4.sp,
    fontWeight = FontWeight.Black,
    textAlign = TextAlign.Center
  )
  val fillColor = Color.White
  val borderColor = InkBlack
  val shadowColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)

  Box(
    modifier = modifier
      .height(44.dp)
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "RAITO",
      style = textStyle,
      color = shadowColor,
      modifier = Modifier.offset(x = 4.dp, y = 4.dp)
    )

    val borderOffsets = listOf(
      (-2).dp to 0.dp,
      2.dp to 0.dp,
      0.dp to (-2).dp,
      0.dp to 2.dp,
      (-1).dp to (-1).dp,
      1.dp to (-1).dp,
      (-1).dp to 1.dp,
      1.dp to 1.dp
    )
    borderOffsets.forEach { (x, y) ->
      Text(
        text = "RAITO",
        style = textStyle,
        color = borderColor,
        modifier = Modifier.offset(x = x, y = y)
      )
    }

    Text(
      text = "RAITO",
      style = textStyle,
      color = fillColor
    )
  }
}

@Composable
fun RaitoBottomNavBar(
  activeScreen: AppScreen,
  onTabSelected: (AppScreen) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Single solid line at the top of the entire navigation bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(3.dp)
        .background(MaterialTheme.colorScheme.onBackground)
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. Home / Studio
      BottomTabCell(
        label = "Home",
        icon = Icons.Default.Home,
        isSelected = activeScreen == AppScreen.HOME,
        onClick = { onTabSelected(AppScreen.HOME) },
        modifier = Modifier.weight(1f)
      )

      // 2. Buckets
      BottomTabCell(
        label = "Buckets",
        icon = Icons.Default.Layers,
        isSelected = activeScreen == AppScreen.BUCKETS,
        onClick = { onTabSelected(AppScreen.BUCKETS) },
        modifier = Modifier.weight(1f)
      )

      // 3. Focus Timer
      BottomTabCell(
        label = "Focus",
        icon = Icons.Default.Timer,
        isSelected = activeScreen == AppScreen.FOCUS,
        onClick = { onTabSelected(AppScreen.FOCUS) },
        modifier = Modifier.weight(1f)
      )

      // 4. Progress
      BottomTabCell(
        label = "Progress",
        icon = Icons.Default.Assessment,
        isSelected = activeScreen == AppScreen.PROGRESS,
        onClick = { onTabSelected(AppScreen.PROGRESS) },
        modifier = Modifier.weight(1f)
      )

      // 5. Settings
      BottomTabCell(
        label = "Settings",
        icon = Icons.Default.Settings,
        isSelected = activeScreen == AppScreen.SETTINGS,
        onClick = { onTabSelected(AppScreen.SETTINGS) },
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
fun BottomTabCell(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val cellBg = MaterialTheme.colorScheme.background
  val cellTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

  Box(
    modifier = modifier
      .fillMaxHeight()
      .background(cellBg)
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = cellTint,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
          fontStyle = FontStyle.Italic,
          fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
          fontSize = 9.sp
        ),
        color = cellTint
      )
    }
  }
}
