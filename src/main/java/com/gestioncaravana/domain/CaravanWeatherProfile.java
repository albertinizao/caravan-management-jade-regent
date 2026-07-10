package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWeatherProfile(
    UUID caravanId,
    WeatherClimateBaseline climateBaseline,
    WeatherElevation elevation,
    boolean crownOfWorld,
    Instant updatedAt) {

  public CaravanWeatherProfile {
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (climateBaseline == null) {
      throw new IllegalArgumentException("climateBaseline is required");
    }
    if (elevation == null) {
      throw new IllegalArgumentException("elevation is required");
    }
    if (updatedAt == null) {
      throw new IllegalArgumentException("updatedAt is required");
    }
  }

  public static CaravanWeatherProfile defaultProfile(UUID caravanId, Instant now) {
    return new CaravanWeatherProfile(caravanId, WeatherClimateBaseline.TEMPERATE, WeatherElevation.SEA_LEVEL, false, now);
  }
}
