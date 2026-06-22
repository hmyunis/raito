package com.example.update

object VersionComparator {
  fun compare(left: String?, right: String?): Int {
    val leftVersion = parse(left)
    val rightVersion = parse(right)

    val maxParts = maxOf(leftVersion.coreParts.size, rightVersion.coreParts.size)
    for (index in 0 until maxParts) {
      val leftPart = leftVersion.coreParts.getOrElse(index) { 0 }
      val rightPart = rightVersion.coreParts.getOrElse(index) { 0 }
      if (leftPart != rightPart) {
        return leftPart.compareTo(rightPart)
      }
    }

    if (leftVersion.preRelease.isEmpty() && rightVersion.preRelease.isEmpty()) return 0
    if (leftVersion.preRelease.isEmpty()) return 1
    if (rightVersion.preRelease.isEmpty()) return -1

    val maxPreReleaseParts = maxOf(leftVersion.preRelease.size, rightVersion.preRelease.size)
    for (index in 0 until maxPreReleaseParts) {
      val leftIdentifier = leftVersion.preRelease.getOrNull(index) ?: return -1
      val rightIdentifier = rightVersion.preRelease.getOrNull(index) ?: return 1

      val leftNumber = leftIdentifier.toIntOrNull()
      val rightNumber = rightIdentifier.toIntOrNull()

      val comparison = when {
        leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
        leftNumber != null -> -1
        rightNumber != null -> 1
        else -> leftIdentifier.compareTo(rightIdentifier)
      }

      if (comparison != 0) {
        return comparison
      }
    }

    return 0
  }

  private fun parse(rawVersion: String?): ParsedVersion {
    val normalized = rawVersion
      .orEmpty()
      .trim()
      .removePrefix("v")
      .removePrefix("V")
      .substringBefore('+')

    val core = normalized.substringBefore('-')
    val preRelease = normalized.substringAfter('-', missingDelimiterValue = "")

    val coreParts = core
      .split('.')
      .mapNotNull { it.toIntOrNull() }
      .ifEmpty { listOf(0) }

    val preReleaseParts = if (preRelease.isBlank()) {
      emptyList()
    } else {
      preRelease.split('.').filter { it.isNotBlank() }
    }

    return ParsedVersion(coreParts = coreParts, preRelease = preReleaseParts)
  }

  private data class ParsedVersion(
    val coreParts: List<Int>,
    val preRelease: List<String>
  )
}
