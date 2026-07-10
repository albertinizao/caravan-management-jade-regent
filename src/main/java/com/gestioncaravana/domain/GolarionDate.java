package com.gestioncaravana.domain;

public record GolarionDate(int year, int month, int day) implements Comparable<GolarionDate> {

  public GolarionDate {
    if (year < 1) {
      throw new IllegalArgumentException("year must be greater than or equal to 1");
    }
    var resolvedMonth = GolarionMonth.fromValue(month);
    var maxDay = resolvedMonth.length(GolarionCalendar.isLeapYear(year));
    if (day < 1 || day > maxDay) {
      throw new IllegalArgumentException("day is out of range for the given month");
    }
  }

  public GolarionMonth monthEnum() {
    return GolarionMonth.fromValue(month);
  }

  public String monthName() {
    return monthEnum().displayName();
  }

  public GolarionDayOfWeek dayOfWeek() {
    return GolarionCalendar.dayOfWeek(this);
  }

  @Override
  public int compareTo(GolarionDate other) {
    if (year != other.year) {
      return Integer.compare(year, other.year);
    }
    if (month != other.month) {
      return Integer.compare(month, other.month);
    }
    return Integer.compare(day, other.day);
  }
}
