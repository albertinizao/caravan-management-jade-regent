package com.gestioncaravana.application.model;

import com.gestioncaravana.domain.WeatherClimateBaseline;
import com.gestioncaravana.domain.WeatherElevation;
import java.time.Instant;
import java.util.UUID;

public record CaravanWeatherProfileView(
    UUID caravanId,
    WeatherClimateBaseline climateBaseline,
    WeatherElevation elevation,
    boolean crownOfWorld,
    Instant updatedAt) {}
