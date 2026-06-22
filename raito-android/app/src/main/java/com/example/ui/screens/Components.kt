package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChapterEntity
import com.example.ui.theme.*
import kotlin.random.Random

// Custom Brutalist-Noir Modifiers
fun Modifier.mangaBorder(
  width: Dp = 2.dp,
  color: Color? = null
): Modifier = this.composed {
  val actualColor = color ?: MaterialTheme.colorScheme.onBackground
  this.border(width, actualColor, RectangleShape)
}

fun Modifier.mangaShadow(
  offset: Dp = 4.dp,
  shadowColor: Color? = null,
  borderColor: Color? = null
): Modifier = this.composed {
  val actualShadowColor = shadowColor ?: MaterialTheme.colorScheme.onBackground
  this.drawBehind {
    val offsetPx = offset.toPx()
    // Draw the solid offset shadow card backdrop
    drawRect(
      color = actualShadowColor,
      topLeft = Offset(offsetPx, offsetPx),
      size = Size(size.width, size.height)
    )
  }
}

fun Modifier.screentonePattern(
  dotColor: Color? = null,
  spacingDp: Dp = 8.dp
): Modifier = this.composed {
  val actualDotColor = dotColor ?: MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
  this.drawBehind {
    val radius = 2f
    val step = spacingDp.toPx()
    if (step <= 0f) return@drawBehind
    var y = 0f
    while (y < size.height) {
      var x = 0f
      while (x < size.width) {
        drawCircle(
          color = actualDotColor,
          radius = radius,
          center = Offset(x, y)
        )
        x += step
      }
      y += step
    }
  }
}

// Linear manga speed lines for active focus modes
fun Modifier.speedLinesPattern(
  lineColor: Color? = null
): Modifier = this.composed {
  val actualLineColor = lineColor ?: MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
  this.drawBehind {
    val step = 16.dp.toPx()
    if (step <= 0f) return@drawBehind
    var x = 0f
    while (x < size.width) {
      drawLine(
        color = actualLineColor,
        start = Offset(x, 0f),
        end = Offset(x + 40f, size.height),
        strokeWidth = 2f
      )
      x += step
    }
  }
}

fun Modifier.starryTwinkleBackground(): Modifier = this.composed {
  val isDark = MaterialTheme.colorScheme.background == BackgroundDark
  
  if (!isDark) {
    return@composed this
  }
  
  val infiniteTransition = rememberInfiniteTransition(label = "twinkle")
  
  val alpha1 by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = keyframes {
        durationMillis = 2400
        0.2f at 0
        0.9f at 1200
        0.2f at 2400
      },
      repeatMode = RepeatMode.Restart
    ),
    label = "star_alpha_1"
  )

  val alpha2 by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 0.1f,
    animationSpec = infiniteRepeatable(
      animation = keyframes {
        durationMillis = 3000
        0.8f at 0
        0.1f at 1500
        0.8f at 3000
      },
      repeatMode = RepeatMode.Restart
    ),
    label = "star_alpha_2"
  )

  val alpha3 by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = keyframes {
        durationMillis = 1800
        0.4f at 0
        1.0f at 900
        0.4f at 1800
      },
      repeatMode = RepeatMode.Restart
    ),
    label = "star_alpha_3"
  )

  val stars = remember {
    List(35) {
      Triple(
        Random.nextFloat(), // fractional x
        Random.nextFloat(), // fractional y
        Random.nextInt(1, 4) // star group
      )
    }
  }

  this.drawBehind {
    // Draw twinkling starry dots
    stars.forEach { (fracX, fracY, group) ->
      val starAlpha = when (group) {
        1 -> alpha1
        2 -> alpha2
        else -> alpha3
      }
      val color = Color(0xFFF8FAFC).copy(alpha = starAlpha)
      val x = fracX * size.width
      val y = fracY * size.height
      val radius = if (group == 3) 4.5f else 2.5f
      drawCircle(
        color = color,
        radius = radius,
        center = Offset(x, y)
      )
    }
  }
}

fun resolveAuraInkColor(
  auraInk: String?,
  fallbackColor: Color
): Color {
  if (auraInk.isNullOrBlank()) return fallbackColor

  if (auraInk.startsWith("#")) {
    return try {
      Color(android.graphics.Color.parseColor(auraInk))
    } catch (_: Exception) {
      fallbackColor
    }
  }

  return when (auraInk.uppercase()) {
    "RED" -> AnimeRed
    "TEAL" -> AnimeTeal
    "PURPLE" -> AnimePurple
    "PINK" -> AnimePink
    "BLACK" -> fallbackColor
    "ORANGE" -> AnimeOrange
    "GREEN" -> AnimeGreen
    "YELLOW" -> AnimeYellow
    "INDIGO" -> IndigoAccent
    "BLUE" -> Color(0xFF2196F3)
    else -> fallbackColor
  }
}

fun resolveChapterAccentColor(
  chapter: ChapterEntity?,
  bucketColoringEnabled: Boolean,
  fallbackColor: Color
): Color {
  if (!bucketColoringEnabled || chapter == null) return fallbackColor
  return resolveAuraInkColor(chapter.auraInk, fallbackColor)
}

// Segmented Navigation Header tab button
@Composable
fun MangaTabButton(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .mangaBorder(width = 2.dp)
      .background(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.bodySmall,
      color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.Bold
    )
  }
}

// Custom Speech thought bubble shape representing pixel assistants dialogues
@Composable
fun SpeechBubble(
  text: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .mangaShadow(offset = 6.dp)
      .background(MaterialTheme.colorScheme.surface)
      .mangaBorder(width = 3.dp)
      .padding(16.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RectangleShape,
    modifier = modifier
      .fillMaxWidth()
      .mangaShadow(offset = 2.dp)
      .mangaBorder()
      .screentonePattern(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .mangaBorder(width = 1.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
        )
      }
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 15.sp
      )
    }
  }
}

@Composable
fun CustomAvatarForm(
  avatarName: String,
  onNameChange: (String) -> Unit,
  neutralUri: android.net.Uri?,
  happyUri: android.net.Uri?,
  focusUri: android.net.Uri?,
  sadUri: android.net.Uri?,
  completedUri: android.net.Uri?,
  onSelectImageClick: (String) -> Unit, // passes "neutral", "happy", etc
  onSave: () -> Unit,
  onDiscard: () -> Unit,
  saveButtonText: String = "SAVE & EQUIP",
  saveEnabledOverride: Boolean = true
) {
  val formSurface = MaterialTheme.colorScheme.surface
  val formCard = MaterialTheme.colorScheme.surfaceVariant
  val formText = MaterialTheme.colorScheme.onSurface
  val formSubtle = MaterialTheme.colorScheme.onSurfaceVariant

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(androidx.compose.foundation.rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "CHIBI IDENTITY DETAILS",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.ExtraBold,
      color = formText
    )

    OutlinedTextField(
      value = avatarName,
      onValueChange = onNameChange,
      label = { Text("CHIBI NAME") },
      singleLine = true,
      shape = RectangleShape,
      colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = formSubtle,
        unfocusedLabelColor = formSubtle,
        focusedTextColor = formText,
        unfocusedTextColor = formText,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = formSurface,
        unfocusedContainerColor = formSurface
      ),
      modifier = Modifier
        .fillMaxWidth()
        .mangaBorder()
    )

    Text(
      text = "UPLOAD 5 EXPRESSIONS (IMAGES OR GIFS)",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.ExtraBold,
      color = formText
    )

    val expressions = listOf(
      Triple("neutral", "Neutral Expression *", neutralUri),
      Triple("happy", "Happy Expression", happyUri),
      Triple("focus", "Active Focus Expression", focusUri),
      Triple("sad", "Sad/Overdue Expression", sadUri),
      Triple("completed", "Cleared/Completed Expression", completedUri)
    )

    expressions.forEach { (key, title, uri) ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .mangaBorder()
          .background(formCard)
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .mangaBorder(width = 1.dp)
            .background(formSurface),
          contentAlignment = Alignment.Center
        ) {
          if (uri != null) {
            AsyncImage(
              model = uri,
              contentDescription = title,
              modifier = Modifier.size(50.dp)
            )
          } else {
            Icon(
              imageVector = Icons.Default.Face,
              contentDescription = null,
              tint = formSubtle.copy(alpha = 0.7f),
              modifier = Modifier.size(32.dp)
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = formText
          )
          Text(
            text = if (uri != null) "FILE LOADED" else "TAP SELECT TO ADD ONE",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (uri != null) AnimeGreen else formSubtle
          )
        }

        Button(
          onClick = { onSelectImageClick(key) },
          shape = RectangleShape,
          colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (uri != null) AnimeGreen else AnimeYellow,
            contentColor = InkBlack
          ),
          modifier = Modifier
            .mangaBorder()
            .height(36.dp)
        ) {
          Text(
            text = if (uri != null) "CHANGE" else "SELECT",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Text(
      text = "* Base neutral expression is required as fallback representation.",
      style = MaterialTheme.typography.bodySmall,
      color = formSubtle,
      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onDiscard,
        shape = RectangleShape,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
          containerColor = formCard,
          contentColor = formText
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .mangaBorder()
      ) {
        Text(
          text = "DISCARD",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }

      val isSaveEnabled = avatarName.trim().isNotEmpty() && neutralUri != null && saveEnabledOverride
      Button(
        onClick = onSave,
        shape = RectangleShape,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
          containerColor = AnimeGreen,
          contentColor = InkBlack,
          disabledContainerColor = InkGrayLight,
          disabledContentColor = InkBlack
        ),
        enabled = isSaveEnabled,
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .mangaShadow(offset = if (isSaveEnabled) 2.dp else 0.dp)
          .mangaBorder()
      ) {
        Text(
          text = saveButtonText,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          fontSize = 12.sp
        )
      }
    }
  }
}
