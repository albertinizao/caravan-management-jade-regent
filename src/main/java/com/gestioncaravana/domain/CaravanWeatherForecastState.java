package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWeatherForecastState(
    UUID caravanId,
    GolarionDate date,
    Integer targetTemperatureF,
    Integer remainingTargetDays,
    Integer dayBaseTemperatureF,
    Integer nightBaseTemperatureF,
    String carryOverPrecipitation,
    Integer carryOverRemainingPeriods,
    String severeEvent,
    Instant generatedAt) {

  public CaravanWeatherForecastState {
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (date == null) {
      throw new IllegalArgumentException("date is required");
    }
    if (targetTemperatureF == null) {
      throw new IllegalArgumentException("targetTemperatureF is required");
    }
    if (remainingTargetDays == null) {
      throw new IllegalArgumentException("remainingTargetDays is required");
    }
    if (dayBaseTemperatureF == null) {
      throw new IllegalArgumentException("dayBaseTemperatureF is required");
    }
    if (nightBaseTemperatureF == null) {
      throw new IllegalArgumentException("nightBaseTemperatureF is required");
    }
    if (carryOverRemainingPeriods == null) {
      throw new IllegalArgumentException("carryOverRemainingPeriods is required");
    }
    if (generatedAt == null) {
      throw new IllegalArgumentException("generatedAt is required");
    }
  }
}
