package com.gestioncaravana.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GolarionCalendarTest {

  @Test
  void convertsOffsetsBackAndForth() {
    var anchor = new GolarionDate(4712, 1, 1);
    var springEquinox = new GolarionDate(4712, 3, 20);
    var lastSupported = new GolarionDate(4722, 12, 31);

    assertThat(GolarionCalendar.fromOffset(GolarionCalendar.toOffset(anchor))).isEqualTo(anchor);
    assertThat(GolarionCalendar.fromOffset(GolarionCalendar.toOffset(springEquinox))).isEqualTo(springEquinox);
    assertThat(GolarionCalendar.fromOffset(GolarionCalendar.toOffset(lastSupported))).isEqualTo(lastSupported);
  }

  @Test
  void marksLeapYearsEveryEightYears() {
    assertThat(GolarionCalendar.isLeapYear(4712)).isTrue();
    assertThat(GolarionCalendar.isLeapYear(4720)).isTrue();
    assertThat(GolarionCalendar.isLeapYear(4716)).isFalse();
    assertThat(GolarionCalendar.monthLength(4712, 2)).isEqualTo(29);
    assertThat(GolarionCalendar.monthLength(4716, 2)).isEqualTo(28);
  }

  @Test
  void calculatesWeekdaysFromCampaignAnchor() {
    assertThat(new GolarionDate(4712, 1, 1).dayOfWeek()).isEqualTo(GolarionDayOfWeek.MOONDAY);
    assertThat(new GolarionDate(4712, 1, 2).dayOfWeek()).isEqualTo(GolarionDayOfWeek.TOILDAY);
    assertThat(new GolarionDate(4712, 3, 20).dayOfWeek()).isEqualTo(GolarionDayOfWeek.WEALDAY);
  }

  @Test
  void rejectsDatesOutsideSupportedRange() {
    assertThatThrownBy(() -> GolarionCalendar.validateSupportedRange(new GolarionDate(4711, 12, 31)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GolarionCalendar.fromOffset(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GolarionCalendar.validateSupportedRange(new GolarionDate(4723, 1, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
