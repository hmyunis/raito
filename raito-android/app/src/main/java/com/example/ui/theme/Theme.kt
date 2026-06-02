package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography as M3Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
  primary = InkBlack,
  onPrimary = Color.White,
  primaryContainer = InkBlack,
  onPrimaryContainer = InkGrayWash,
  background = BackgroundLight,
  onBackground = InkBlack,
  surface = BgPaperLight,
  onSurface = InkBlack,
  surfaceVariant = InkGrayWash,
  onSurfaceVariant = InkGrayDark,
  outline = InkGrayDark,
  secondary = AnimeRed,
  onSecondary = Color.White,
  error = AnimeRed,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = IndigoAccent,
  onPrimary = InkBlack,
  primaryContainer = IndigoLight,
  onPrimaryContainer = InkBlack,
  background = BackgroundDark,
  onBackground = Color.White,
  surface = BgPaperDark,
  onSurface = Color.White,
  surfaceVariant = IndigoDark,
  onSurfaceVariant = IndigoLight,
  outline = IndigoAccent,
  secondary = AnimeTeal,
  onSecondary = InkBlack,
  error = AnimeRed,
  onError = Color.White
)

@Composable
fun RaitoTheme(
  themeMode: String = "Light", // "Light" or "Dark" or "System"
  scale: Float = 1.0f,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    "Dark" -> true
    "Light" -> false
    else -> isSystemInDarkTheme()
  }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

  val scaledTypography = androidx.compose.runtime.remember(scale) {
    M3Typography(
      displayLarge = Typography.displayLarge.copy(
        fontSize = (Typography.displayLarge.fontSize.value * scale).sp,
        lineHeight = (Typography.displayLarge.lineHeight.value * scale).sp
      ),
      displayMedium = Typography.displayMedium.copy(
        fontSize = (Typography.displayMedium.fontSize.value * scale).sp,
        lineHeight = (Typography.displayMedium.lineHeight.value * scale).sp
      ),
      titleLarge = Typography.titleLarge.copy(
        fontSize = (Typography.titleLarge.fontSize.value * scale).sp,
        lineHeight = (Typography.titleLarge.lineHeight.value * scale).sp
      ),
      titleMedium = Typography.titleMedium.copy(
        fontSize = (Typography.titleMedium.fontSize.value * scale).sp,
        lineHeight = (Typography.titleMedium.lineHeight.value * scale).sp
      ),
      bodyLarge = Typography.bodyLarge.copy(
        fontSize = (Typography.bodyLarge.fontSize.value * scale).sp,
        lineHeight = (Typography.bodyLarge.lineHeight.value * scale).sp
      ),
      bodyMedium = Typography.bodyMedium.copy(
        fontSize = (Typography.bodyMedium.fontSize.value * scale).sp,
        lineHeight = (Typography.bodyMedium.lineHeight.value * scale).sp
      ),
      bodySmall = Typography.bodySmall.copy(
        fontSize = (Typography.bodySmall.fontSize.value * scale).sp,
        lineHeight = (Typography.bodySmall.lineHeight.value * scale).sp
      ),
      labelSmall = Typography.labelSmall.copy(
        fontSize = (Typography.labelSmall.fontSize.value * scale).sp,
        lineHeight = (Typography.labelSmall.lineHeight.value * scale).sp
      )
    )
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = scaledTypography,
    content = content
  )
}
