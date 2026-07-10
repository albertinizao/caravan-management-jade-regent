package com.gestioncaravana.domain;

public enum WeatherSeason {
  WINTER,
  SPRING,
  SUMMER,
  FALL;

  public static WeatherSeason fromMonth(int month) {
    return switch (month) {
      case 12, 1, 2 -> WINTER;
      case 3, 4, 5 -> SPRING;
      case 6, 7, 8 -> SUMMER;
      case 9, 10, 11 -> FALL;
      default -> throw new IllegalArgumentException("Unsupported month: " + month);
    };
  }
}
