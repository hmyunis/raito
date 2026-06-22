package com.example.widget

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.example.R

object TaskWidgetAccent {
  fun resolve(context: Context, auraInk: String?): Int {
    val fallback = ContextCompat.getColor(context, R.color.widget_ink)
    if (auraInk.isNullOrBlank()) return fallback

    if (auraInk.startsWith("#")) {
      return try {
        Color.parseColor(auraInk)
      } catch (_: Exception) {
        fallback
      }
    }

    return when (auraInk.uppercase()) {
      "RED" -> ContextCompat.getColor(context, R.color.widget_warning)
      "TEAL" -> ContextCompat.getColor(context, R.color.widget_teal)
      "PURPLE" -> ContextCompat.getColor(context, R.color.widget_purple)
      "PINK" -> ContextCompat.getColor(context, R.color.widget_pink)
      "ORANGE" -> ContextCompat.getColor(context, R.color.widget_orange)
      "GREEN" -> ContextCompat.getColor(context, R.color.widget_positive)
      "YELLOW" -> ContextCompat.getColor(context, R.color.widget_yellow)
      "INDIGO" -> ContextCompat.getColor(context, R.color.widget_indigo)
      "BLUE" -> ContextCompat.getColor(context, R.color.widget_blue)
      "BLACK" -> fallback
      else -> fallback
    }
  }
}
