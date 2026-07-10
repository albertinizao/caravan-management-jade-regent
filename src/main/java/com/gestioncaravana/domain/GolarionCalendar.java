package com.gestioncaravana.domain;

public final class GolarionCalendar {

  public static final GolarionDate MIN_SUPPORTED_DATE = new GolarionDate(4712, 1, 1);
  public static final GolarionDate MAX_SUPPORTED_DATE = new GolarionDate(4722, 12, 31);
  public static final GolarionDate CAMPAIGN_ANCHOR_DATE = MIN_SUPPORTED_DATE;
  private static final int MIN_OFFSET = 0;
  private static final int MAX_OFFSET = toOffset(MAX_SUPPORTED_DATE);

  private GolarionCalendar() {}

  public static boolean isLeapYear(int year) {
    return year % 8 == 0;
  }

  public static int monthLength(int year, int month) {
    return GolarionMonth.fromValue(month).length(isLeapYear(year));
  }

  public static GolarionDayOfWeek dayOfWeek(GolarionDate date) {
    return GolarionDayOfWeek.fromIndex(toOffset(date));
  }

  public static int toOffset(GolarionDate date) {
    validateSupportedRange(date);
    var total = 0;
    for (var year = CAMPAIGN_ANCHOR_DATE.year(); year < date.year(); year++) {
      total += isLeapYear(year) ? 366 : 365;
    }
    for (var month = 1; month < date.month(); month++) {
      total += monthLength(date.year(), month);
    }
    return total + date.day() - 1;
  }

  public static GolarionDate fromOffset(int offset) {
    if (offset < MIN_OFFSET || offset > MAX_OFFSET) {
      throw new IllegalArgumentException("date is out of supported Golarion range");
    }
    var year = CAMPAIGN_ANCHOR_DATE.year();
    var remaining = offset;
    while (remaining >= yearLength(year)) {
      remaining -= yearLength(year);
      year++;
    }
    var month = 1;
    while (remaining >= monthLength(year, month)) {
      remaining -= monthLength(year, month);
      month++;
    }
    return new GolarionDate(year, month, remaining + 1);
  }

  public static GolarionDate addDays(GolarionDate date, int days) {
    return fromOffset(toOffset(date) + days);
  }

  public static void validateSupportedRange(GolarionDate date) {
    if (date.compareTo(MIN_SUPPORTED_DATE) < 0 || date.compareTo(MAX_SUPPORTED_DATE) > 0) {
      throw new IllegalArgumentException("date is out of supported Golarion range");
    }
  }

  private static int yearLength(int year) {
    return isLeapYear(year) ? 366 : 365;
  }
}
