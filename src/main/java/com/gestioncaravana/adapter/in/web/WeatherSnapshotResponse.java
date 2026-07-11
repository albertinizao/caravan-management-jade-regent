package com.gestioncaravana.adapter.in.web;

public record WeatherSnapshotResponse(
    WeatherPeriodResponse midnightToDawn,
    WeatherPeriodResponse dawnToNoon,
    WeatherPeriodResponse noonToDusk,
    WeatherPeriodResponse duskToMidnight,
    String crownLightCondition) {}
