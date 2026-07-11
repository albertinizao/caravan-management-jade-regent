package com.gestioncaravana.application.model;

public record WeatherSnapshotView(
    WeatherPeriodView midnightToDawn,
    WeatherPeriodView dawnToNoon,
    WeatherPeriodView noonToDusk,
    WeatherPeriodView duskToMidnight,
    String crownLightCondition) {}
