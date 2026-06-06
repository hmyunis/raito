package com.example.util

import com.example.data.database.ChapterEntity
import com.example.data.database.TaskEntity
import kotlin.math.abs

enum class ChibiMood {
  Neutral,
  Happy,
  Focus,
  Sad,
  Completed
}

object ChibiSpeechBank {
  private val lastLineIndexByContext = mutableMapOf<String, Int>()

  fun moodFor(chapter: ChapterEntity, tasks: List<TaskEntity>): ChibiMood {
    if (chapter.isCompleted) return ChibiMood.Completed
    if (tasks.any { it.isOverdue && !it.isCompleted }) return ChibiMood.Sad
    if (tasks.isEmpty()) return ChibiMood.Neutral
    val completed = tasks.count { it.isCompleted }
    return when {
      completed == tasks.size -> ChibiMood.Completed
      completed == 0 -> ChibiMood.Focus
      completed * 2 >= tasks.size -> ChibiMood.Happy
      else -> ChibiMood.Focus
    }
  }

  fun expressionFor(companionId: String, mood: ChibiMood): String {
    val expressions = CompanionRegistry.getExpressions(companionId)
    return when (mood) {
      ChibiMood.Neutral -> expressions.neutral
      ChibiMood.Happy -> expressions.happy
      ChibiMood.Focus -> expressions.focus
      ChibiMood.Sad -> expressions.sad
      ChibiMood.Completed -> expressions.completed
    }
  }

  fun lineFor(companionId: String, mood: ChibiMood, chapter: ChapterEntity, tasks: List<TaskEntity>, variation: Long = 0L): String {
    val lines = linesFor(companionId, mood)
    val completed = tasks.count { it.isCompleted }
    val seed = "${chapter.id}-${chapter.name}-${mood.name}-${tasks.size}-$completed-${tasks.count { it.isOverdue }}-$variation".hashCode()
    val contextKey = "$companionId-${chapter.id}-${mood.name}"
    val candidateIndex = abs(seed).mod(lines.size)
    val previousIndex = lastLineIndexByContext[contextKey]
    val templateIndex = when {
      lines.size <= 1 -> 0
      previousIndex == null || previousIndex != candidateIndex -> candidateIndex
      else -> (candidateIndex + 1 + abs(seed / lines.size.coerceAtLeast(1)).mod(lines.size - 1)) % lines.size
    }
    lastLineIndexByContext[contextKey] = templateIndex
    val template = lines[templateIndex]
    return template
      .replace("{bucket}", chapter.name)
      .replace("{left}", (tasks.size - completed).coerceAtLeast(0).toString())
      .replace("{done}", completed.toString())
      .replace("{total}", tasks.size.toString())
  }

  private fun linesFor(companionId: String, mood: ChibiMood): List<String> {
    val key = when (companionId) {
      "Knight" -> "Knight"
      "Scholar", "Artist" -> "Scholar"
      "Ranger" -> "Ranger"
      "Dragon" -> "Dragon"
      else -> "Cyber"
    }
    return banks.getValue(key).getValue(mood)
  }

  private val cyberNeutral = listOf(
    "Systems online for {bucket}. Pick one clean action.",
    "I am scanning {bucket}. The next step is waiting.",
    "{left} task nodes remain. We can process them calmly.",
    "No panic detected. Just choose the next useful move.",
    "Idle mode ready. I will keep the signal clear.",
    "Your workspace is stable. Let us make one precise update.",
    "I have {total} entries mapped for {bucket}.",
    "Small inputs compound. Start with the lightest task.",
    "The board is readable. Your next command can be simple.",
    "I am here if you need a steady checkpoint."
  )

  private val cyberHappy = listOf(
    "Nice progress. {done} tasks cleared and the signal is brighter.",
    "{bucket} is gaining color. Keep the momentum measured.",
    "Good execution. The queue is shrinking.",
    "I logged that progress ping. Very clean.",
    "You are past the halfway mark. Maintain course.",
    "The system likes this trajectory.",
    "Task friction is dropping. Continue with the next small win.",
    "Progress confirmed. Your future self gets a cleaner board.",
    "That was a strong pass through the list.",
    "The bucket is responding well to steady effort."
  )

  private val cyberFocus = listOf(
    "Focus mode suggested. One task, one timer, no extra windows.",
    "{left} remaining. Select the task with the clearest first action.",
    "Let us reduce the queue by one precise cut.",
    "I recommend a short sprint for {bucket}.",
    "Noise filtered. Keep only the next action in view.",
    "Task density is manageable. Begin where resistance is lowest.",
    "Attention lock is ready. Choose a target.",
    "The fastest route is one completed item, not a perfect plan.",
    "We can turn this list into motion now.",
    "Start with the task you can finish without negotiation.",
    "Set the timer. Let the rest wait outside the room.",
    "Your task stack is not a wall. It is a sequence.",
    "I am holding the context. You only need the next move.",
    "A five minute start is enough to break the seal.",
    "Convert one loose task into a finished mark."
  )

  private val cyberSad = listOf(
    "Warning: overdue pressure detected. We should stabilize the board.",
    "Some tasks are flashing red. Pick the smallest overdue item first.",
    "No shame protocol active. We just recover one task.",
    "The queue is noisy, but still solvable.",
    "Let us clear the oldest pressure point.",
    "Overdue does not mean impossible. It means start smaller.",
    "I recommend a reset breath and one concrete action.",
    "The red markers are information, not a verdict.",
    "We can lower the pressure by finishing one visible task.",
    "Recovery mode: choose the task with the shortest path to done."
  )

  private val cyberCompleted = listOf(
    "{bucket} is fully cleared. Excellent execution.",
    "Completion confirmed. This bucket is in full color.",
    "All task nodes resolved. Archive the win.",
    "Clean finish. The system records this as a strong cycle.",
    "Nothing left in this bucket. That is a real result.",
    "Full clear achieved. Your attention paid off.",
    "The queue is empty. Enjoy the quiet signal.",
    "Bucket complete. You can move on without dragging residue.",
    "All checks passed. That was disciplined work.",
    "Victory state reached. The board is clean."
  )

  private fun personality(
    neutral: List<String>,
    happy: List<String>,
    focus: List<String>,
    sad: List<String>,
    completed: List<String>
  ) = mapOf(
    ChibiMood.Neutral to neutral,
    ChibiMood.Happy to happy,
    ChibiMood.Focus to focus,
    ChibiMood.Sad to sad,
    ChibiMood.Completed to completed
  )

  private val banks = mapOf(
    "Cyber" to personality(cyberNeutral, cyberHappy, cyberFocus, cyberSad, cyberCompleted),
    "Knight" to personality(
      neutral = listOf("The field is quiet. Name the quest, and I shall hold the line.", "A calm stance wins long campaigns.", "{bucket} awaits your banner."),
      happy = listOf("Well struck. {done} marks of honor are won.", "Your resolve is showing, brave one.", "The quest grows lighter with each cleared task."),
      focus = listOf("Raise your shield against distraction. One task at a time.", "Choose the nearest foe and advance.", "{left} challenges remain. We charge with discipline."),
      sad = listOf("The red flags rise, but courage begins in recovery.", "No retreat from overdue work. We take the smallest gate first.", "Steady. Even a tired knight can lift one task."),
      completed = listOf("Quest complete. Your banner flies over {bucket}.", "A full victory. Rest your blade.", "The chapter is cleared with honor.")
    ),
    "Scholar" to personality(
      neutral = listOf("Let us observe {bucket} with a fresh eye.", "A blank margin is a fine place to begin.", "The composition is waiting for its first line."),
      happy = listOf("Lovely progress. The piece is gaining form.", "{done} strokes are already placed with care.", "This bucket has a better rhythm now."),
      focus = listOf("Choose one detail and render it well.", "No need to finish the whole canvas at once.", "Work close. One deliberate mark changes the page."),
      sad = listOf("The palette is muddy, but we can restore contrast.", "Overdue work needs a gentle first stroke.", "Let us clean one corner of the canvas."),
      completed = listOf("Finished piece. {bucket} has its full color.", "The final stroke is down.", "Signed, sealed, and ready to admire.")
    ),
    "Ranger" to personality(
      neutral = listOf("I am watching the trail. {bucket} is quiet for now.", "Move lightly. Pick the next track.", "The route is visible if we stay low."),
      happy = listOf("Good hit. The trail is clearing.", "{done} targets down. Keep moving.", "You slipped past resistance cleanly."),
      focus = listOf("Mark one target. Ignore the rest.", "Stay hidden from distractions and take the next shot.", "{left} tracks remain. Follow the nearest one."),
      sad = listOf("Overdue signs on the trail. We move carefully.", "Pressure is close. Take the smallest safe route.", "Do not sprint. Scout, choose, finish."),
      completed = listOf("Trail cleared. {bucket} is safe.", "Clean extraction. No tasks left behind.", "Mission complete. Fade out with the win.")
    ),
    "Dragon" to personality(
      neutral = listOf("The hoard of tasks rests before us.", "Tiny flame, steady breath. Begin when ready.", "{bucket} can be forged one ember at a time."),
      happy = listOf("Excellent. The flame grows brighter.", "{done} treasures secured from the task hoard.", "Momentum is warm. Feed it one more task."),
      focus = listOf("Breathe fire at one target only.", "Guard your attention like treasure.", "{left} embers remain. Stoke the next one."),
      sad = listOf("Smoke in the cave. Clear one overdue ember first.", "The hoard feels heavy, but dragons move mountains slowly.", "Bank the panic. Keep the flame small and useful."),
      completed = listOf("The hoard is conquered. {bucket} shines.", "Full blaze achieved. Rest in the warm cave.", "Every ember is lit. This bucket is complete.")
    )
  )
}
