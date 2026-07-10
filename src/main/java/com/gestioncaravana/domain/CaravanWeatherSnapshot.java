package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWeatherSnapshot(
    UUID caravanId,
    GolarionDate date,
    WeatherSnapshot weather,
    Instant generatedAt) {

  public CaravanWeatherSnapshot {
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (date == null) {
      throw new IllegalArgumentException("date is required");
    }
    if (weather == null) {
      throw new IllegalArgumentException("weather is required");
    }
    if (generatedAt == null) {
      throw new IllegalArgumentException("generatedAt is required");
    }
  }
}
