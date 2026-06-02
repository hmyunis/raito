package com.example.util

import com.example.BuildConfig

data class ChibiExpressions(
    val neutral: String,
    val happy: String,
    val focus: String,
    val sad: String,
    val completed: String
)

object CompanionRegistry {
    private fun drawable(name: String): String = "android.resource://${BuildConfig.APPLICATION_ID}/drawable/$name"

    val CYBER = ChibiExpressions(
        neutral = drawable("cyber_assistant_neutral"),
        happy = drawable("cyber_assistant_happy"),
        focus = drawable("cyber_assistant_focus"),
        sad = drawable("cyber_assistant_sad"),
        completed = drawable("cyber_assistant_completed")
    )

    val KNIGHT = ChibiExpressions(
        neutral = drawable("tiny_knight_neutral"),
        happy = drawable("tiny_knight_happy"),
        focus = drawable("tiny_knight_focus"),
        sad = drawable("tiny_knight_sad"),
        completed = drawable("tiny_knight_completed")
    )

    val SCHOLAR = ChibiExpressions(
        neutral = drawable("studio_artist_neutral"),
        happy = drawable("studio_artist_happy"),
        focus = drawable("studio_artist_focus"),
        sad = drawable("studio_artist_sad"),
        completed = drawable("studio_artist_completed")
    )

    val RANGER = ChibiExpressions(
        neutral = drawable("shadow_ranger_neutral"),
        happy = drawable("shadow_ranger_happy"),
        focus = drawable("shadow_ranger_focus"),
        sad = drawable("shadow_ranger_sad"),
        completed = drawable("shadow_ranger_completed")
    )

    val DRAGON = ChibiExpressions(
        neutral = drawable("dragon_keeper_neutral"),
        happy = drawable("dragon_keeper_happy"),
        focus = drawable("dragon_keeper_focus"),
        sad = drawable("dragon_keeper_sad"),
        completed = drawable("dragon_keeper_completed")
    )

    fun getExpressions(id: String): ChibiExpressions {
        return when (id) {
            "Cyber" -> CYBER
            "Knight" -> KNIGHT
            "Scholar" -> SCHOLAR
            "Artist" -> SCHOLAR
            "Ranger" -> RANGER
            "Dragon" -> DRAGON
            else -> CYBER
        }
    }
}
