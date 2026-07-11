package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWeatherProfile(
    UUID caravanId,
    WeatherClimateBaseline climateBaseline,
    WeatherElevation elevation,
    CrownWeatherRegion crownRegion,
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
    if (climateBaseline == WeatherClimateBaseline.CROWN_OF_THE_WORLD && crownRegion == null) {
      throw new IllegalArgumentException("crownRegion is required for Crown of the World");
    }
    if (climateBaseline != WeatherClimateBaseline.CROWN_OF_THE_WORLD && crownRegion != null) {
      throw new IllegalArgumentException("crownRegion requires Crown of the World baseline");
    }
    if (crownRegion != null && !isValidCrownElevation(crownRegion, elevation)) {
      throw new IllegalArgumentException("elevation is not valid for the selected crownRegion");
    }
    if (updatedAt == null) {
      throw new IllegalArgumentException("updatedAt is required");
    }
  }

  public static CaravanWeatherProfile defaultProfile(UUID caravanId, Instant now) {
    return new CaravanWeatherProfile(caravanId, WeatherClimateBaseline.TEMPERATE, WeatherElevation.SEA_LEVEL, null, now);
  }

  public boolean isCrownOfTheWorld() {
    return climateBaseline == WeatherClimateBaseline.CROWN_OF_THE_WORLD;
  }

  private static boolean isValidCrownElevation(CrownWeatherRegion region, WeatherElevation elevation) {
    return switch (region) {
      case OUTER_RIM -> elevation == WeatherElevation.LOWLAND;
      case HIGH_ICE, BOREAL_EXPANSE -> elevation == WeatherElevation.HIGHLAND || elevation == WeatherElevation.PEAK;
    };
  }
}
