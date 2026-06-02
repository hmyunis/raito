package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RaitoViewModel
import com.example.util.CompanionRegistry

@Composable
fun MilestoneView(
  viewModel: RaitoViewModel,
  modifier: Modifier = Modifier
) {
  val completedChibiUrl by viewModel.milestoneCompanionId.collectAsState()
  val completedChibi = CompanionRegistry.getExpressions(completedChibiUrl).completed

  // Retro 16-bit console box animation transitions
  val infiniteTransition = rememberInfiniteTransition(label = "milestone_animations")
  val shadowOffsetAnim by infiniteTransition.animateFloat(
    initialValue = 3f,
    targetValue = 7f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shadow_offset_anim"
  )
  val spriteBobbing by infiniteTransition.animateFloat(
    initialValue = -6f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sprite_bobbing"
  )
  val spriteScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sprite_scale"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundLight)
      .screentonePattern(dotColor = InkBlack.copy(0.04f))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    // Elegant single-screen Hello World greeting card proportions (Manga-Noir format)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .mangaShadow(offset = 8.dp)
        .background(BgPaperLight)
        .mangaBorder(width = 3.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. Yellow Header Banner
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(AnimeYellow)
          .mangaBorder()
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Star",
          tint = InkBlack,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "FULL COLOR REACHED!",
          style = MaterialTheme.typography.displayMedium,
          color = InkBlack,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Star",
          tint = InkBlack,
          modifier = Modifier.size(18.dp)
        )
      }

      // 2. Central Image Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
          .mangaBorder()
          .background(BgPaperLight)
          .speedLinesPattern(),
        contentAlignment = Alignment.Center
      ) {
        // Chibi Vector Frame with 16-bit console dialogue shadow fluctuation
        Box(
          modifier = Modifier
            .size(180.dp)
            .mangaShadow(offset = shadowOffsetAnim.dp, shadowColor = InkBlack)
            .background(Color.White)
            .mangaBorder(width = 2.dp),
          contentAlignment = Alignment.Center
        ) {
          AsyncImage(
            model = completedChibi,
            contentDescription = "Victorious Chibi",
            modifier = Modifier
              .fillMaxSize(0.9f)
              .graphicsLayer {
                translationY = spriteBobbing
                scaleX = spriteScale
                scaleY = spriteScale
              }
          )
        }

        // Sparkling Deco Accents floating (using Vector Star icons instead of emoji sparkles "✨")
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Sparkling star",
          tint = AnimeYellow,
          modifier = Modifier
            .size(36.dp)
            .align(Alignment.TopStart)
            .padding(8.dp)
        )
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Sparkling star",
          tint = AnimeYellow,
          modifier = Modifier
            .size(36.dp)
            .align(Alignment.BottomEnd)
            .padding(8.dp)
        )
      }

      // 3. Text Descriptions Block
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "MILESTONE UNLOCKED",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black,
          color = InkBlack
        )

        Text(
          text = "Master Study chapter is now fully inked and colored!",
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = InkGrayDark,
          textAlign = TextAlign.Center
        )
      }

      // 4. CTA Button Continue Action
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp)
      ) {
        Button(
          onClick = { viewModel.navigateTo(AppScreen.HOME) },
          shape = RectangleShape,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .mangaShadow(offset = 4.dp, shadowColor = AnimeYellow)
            .mangaBorder(),
          colors = ButtonDefaults.buttonColors(containerColor = InkBlack)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "CONTINUE",
              style = MaterialTheme.typography.displayMedium,
              color = Color.White
            )
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Continue play arrow",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}
