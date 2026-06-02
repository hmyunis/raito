package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.ActivityDayEntity
import com.example.data.database.ChapterEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RaitoViewModel
import com.example.util.CompanionRegistry
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val stats by viewModel.stats.collectAsState()
  val chapters by viewModel.chapters.collectAsState()
  val activityDays by viewModel.activityDays.collectAsState()

  val completedChapters = remember(chapters) { chapters.filter { it.isCompleted } }

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
        horizontalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // Left Column (Stats & Studio Unlocks banner)
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          // 1. Stats Cards Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Daily Ink Streak
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = "Daily Ink Streak",
                  tint = AnimeRed,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "DAILY INK",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${stats.dailyStreak} Days",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Raito Points
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Raito Points Balance",
                  tint = AnimeYellow,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "RAITO PTS",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${stats.points}",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Cleared Tasks
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "✓", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AnimeGreen)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "CLEARED",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${stats.clearedCount}",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // 4. Custom Unlocks Visual banner
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .mangaShadow(offset = 3.dp)
              .background(AnimeYellow)
              .mangaBorder(width = 3.dp)
              .screentonePattern(dotColor = InkBlack.copy(0.08f))
              .padding(16.dp)
          ) {
            Text(
              text = "STUDIO UNLOCKS",
              style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "New companions & outfit skins are available!",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { viewModel.navigateTo(AppScreen.SHOP) },
              shape = RectangleShape,
              modifier = Modifier
                .mangaShadow(offset = 2.dp)
                .mangaBorder(),
              colors = ButtonDefaults.buttonColors(containerColor = InkBlack)
            ) {
              Text(
                text = "OPEN SHOP",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
          
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Right Column (Activity grid & Completed chapters)
        Column(
          modifier = Modifier
            .weight(1.2f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          // Heatmap section
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "ACTIVITY",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(InkBlack)
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .mangaShadow(offset = 3.dp)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder()
              .padding(12.dp)
              .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.CenterStart
          ) {
            ActivityHeatmapGrid(activityDays)
          }

          // Completed Chapter Chibi Gallery list
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "COMPLETED CHAPTERS",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(InkBlack)
            )
          }

        if (completedChapters.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Default.EmojiEvents,
              title = "No Completed Buckets",
              message = "Complete every task in a bucket to add it to your progress gallery."
            )
          } else {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              completedChapters.forEach { chapter ->
                CompletedChapterCard(chapter)
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
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. Stats Cards Row
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Daily Ink Streak
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = "Daily Ink Streak",
                  tint = AnimeRed,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "DAILY INK",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${stats.dailyStreak} Days",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Raito Points
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Raito Points Balance",
                  tint = AnimeYellow,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "RAITO PTS",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${stats.points}",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Cleared Tasks
            Box(
              modifier = Modifier
                .weight(1f)
                .mangaShadow(offset = 2.dp)
                .background(MaterialTheme.colorScheme.surface)
                .mangaBorder()
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "✓", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AnimeGreen)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "CLEARED",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${stats.clearedCount}",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // 2. Heatmap section
        item {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "ACTIVITY",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(InkBlack)
            )
          }
        }

        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .mangaShadow(offset = 3.dp)
              .background(MaterialTheme.colorScheme.surface)
              .mangaBorder()
              .padding(16.dp)
              .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.CenterStart
          ) {
            ActivityHeatmapGrid(activityDays)
          }
        }

        // 3. Completed Chapter Chibi Gallery list
        item {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "COMPLETED CHAPTERS",
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(InkBlack)
            )
          }
        }

        if (completedChapters.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Default.EmojiEvents,
              title = "No Completed Buckets",
              message = "Complete every task in a bucket to add it to your progress gallery."
            )
          }
        } else {
          item {
            LazyRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              items(completedChapters) { chapter ->
                CompletedChapterCard(chapter)
              }
            }
          }
        }

        // 4. Custom Unlocks Visual banner
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .mangaShadow(offset = 3.dp)
              .background(AnimeYellow)
              .mangaBorder(width = 3.dp)
              .screentonePattern(dotColor = InkBlack.copy(0.08f))
              .padding(16.dp)
          ) {
            Text(
              text = "STUDIO UNLOCKS",
              style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "New companions & outfit skins are available!",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { viewModel.navigateTo(AppScreen.SHOP) },
              shape = RectangleShape,
              modifier = Modifier
                .mangaShadow(offset = 2.dp)
                .mangaBorder(),
              colors = ButtonDefaults.buttonColors(containerColor = InkBlack)
            ) {
              Text(
                text = "OPEN SHOP",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
      }
    }
  }
}

@Composable
fun ActivityHeatmapGrid(
  loggedDays: List<ActivityDayEntity>
) {
  // Let's draw a mock grid representing past weeks
  // Days of week (7 rows) x 18 columns (weeks)
  val columnsCount = 18
  val rowsCount = 7

  val cal = Calendar.getInstance()

  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    for (col in 0 until columnsCount) {
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        for (row in 0 until rowsCount) {
          // calculate days index backward
          val offset = (columnsCount - 1 - col) * 7 + (rowsCount - 1 - row)
          cal.time = Date()
          cal.add(Calendar.DAY_OF_YEAR, -offset)
          val dayStr = DateUtils.formatYyyyMMdd(cal.time)

          val log = loggedDays.firstOrNull { it.date == dayStr }
          val intensity = log?.intensity ?: 0

          val cellColor = when (intensity) {
            1 -> InkGrayWash
            2 -> InkGrayLight
            3 -> InkGrayDark
            4 -> InkBlack
            else -> Color(0xFFF3F1EA) // Empty grey
          }

          Box(
            modifier = Modifier
              .size(14.dp)
              .background(cellColor)
              .border(0.5.dp, Color.White, RectangleShape)
          )
        }
      }
    }
  }
}

@Composable
fun CompletedChapterCard(
  chapter: ChapterEntity,
  modifier: Modifier = Modifier
) {
  val completedChibiUrl = CompanionRegistry.getExpressions(chapter.companionId).completed

  Box(
    modifier = modifier
      .width(130.dp)
      .mangaShadow(offset = 3.dp)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder()
  ) {
    Column {
      // Color illustration box representation
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .mangaBorder(width = 1.dp)
          .screentonePattern(dotColor = InkBlack.copy(0.1f)),
        contentAlignment = Alignment.Center
      ) {
        AsyncImage(
          model = completedChibiUrl,
          contentDescription = "Milestone full color Chibi",
          modifier = Modifier.fillMaxSize(0.85f)
        )
      }

      // Title ink block
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(InkBlack)
          .padding(8.dp)
      ) {
        Column {
          Text(
            text = chapter.name.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = chapter.discipline.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AnimeYellow,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 8.sp
          )
        }
      }
    }
  }
}
