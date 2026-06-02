package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.*
import kotlinx.coroutines.flow.StateFlow
import java.util.Random

private data class ConfettiParticle(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  val color: Color,
  val size: Float,
  var rotation: Float,
  val rotationSpeed: Float,
  val isCircle: Boolean
)

@Composable
fun ConfettiOverlay(
  triggerFlow: StateFlow<Long>,
  modifier: Modifier = Modifier
) {
  val triggerTime by triggerFlow.collectAsState()
  var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
  val random = remember { Random() }

  // Vibrant anime/manga comic colors
  val colors = remember {
    listOf(AnimeRed, AnimeYellow, AnimePurple, AnimeTeal, AnimeGreen, AnimePink, AnimeOrange, IndigoAccent)
  }

  LaunchedEffect(triggerTime) {
    if (triggerTime > 0) {
      val count = 80
      val newList = List(count) {
        val sizeVal = 12f + random.nextFloat() * 25f
        ConfettiParticle(
          x = random.nextFloat() * 1200f,
          y = -40f - random.nextFloat() * 400f,
          vx = -3f + random.nextFloat() * 6f,
          vy = 5f + random.nextFloat() * 10f,
          color = colors[random.nextInt(colors.size)],
          size = sizeVal,
          rotation = random.nextFloat() * 360f,
          rotationSpeed = -6f + random.nextFloat() * 12f,
          isCircle = random.nextBoolean()
        )
      }
      particles = newList

      var lastTime = withFrameNanos { it }
      var active = true
      while (active) {
        withFrameNanos { frameTimeNanos ->
          val frameTime = frameTimeNanos / 1_000_000L
          val dt = ((frameTime - lastTime / 1_000_000L) / 16.6f).coerceIn(0.1f, 3.0f)
          lastTime = frameTimeNanos

          var allOffScreen = true
          val updatedList = particles.map { p ->
            val nextY = p.y + p.vy * dt
            if (nextY < 2600f) allOffScreen = false
            p.copy(
              x = p.x + p.vx * dt,
              y = nextY,
              rotation = (p.rotation + p.rotationSpeed * dt) % 360f
            )
          }
          particles = updatedList
          if (allOffScreen) {
            active = false
          }
        }
      }
      particles = emptyList()
    }
  }

  if (particles.isNotEmpty()) {
    Canvas(modifier = modifier.fillMaxSize()) {
      val canvasWidth = size.width
      
      particles.forEach { p ->
        // Adjust position dynamically to match actual Canvas width
        if (p.x < -50f || p.x > canvasWidth + 50f) {
          p.x = random.nextFloat() * canvasWidth
        }

        rotate(p.rotation, pivot = Offset(p.x + p.size / 2, p.y + p.size / 2)) {
          if (p.isCircle) {
            drawCircle(
              color = p.color,
              radius = p.size / 2,
              center = Offset(p.x, p.y)
            )
          } else {
            drawRect(
              color = p.color,
              topLeft = Offset(p.x, p.y),
              size = Size(p.size, p.size * 0.6f)
            )
          }
        }
      }
    }
  }
}
