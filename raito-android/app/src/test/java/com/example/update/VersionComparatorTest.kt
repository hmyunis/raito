package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionComparatorTest {
  @Test
  fun comparesNumericSegmentsCorrectly() {
    assertEquals(1, VersionComparator.compare("1.2.0", "1.1.9"))
    assertEquals(0, VersionComparator.compare("1.0", "1.0.0"))
    assertEquals(-1, VersionComparator.compare("1.0.9", "1.0.10"))
  }

  @Test
  fun stableBuildRanksAbovePrerelease() {
    assertEquals(1, VersionComparator.compare("1.0.0", "1.0.0-rc.1"))
    assertEquals(-1, VersionComparator.compare("1.0.0-beta.2", "1.0.0"))
  }

  @Test
  fun ignoresLeadingVAndBuildMetadata() {
    assertEquals(0, VersionComparator.compare("v1.4.0+9", "1.4.0"))
  }
}
