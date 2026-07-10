package com.gestioncaravana.domain;

public record WeatherSnapshot(
    WeatherPeriod midnightToDawn,
    WeatherPeriod dawnToNoon,
    WeatherPeriod noonToDusk,
    WeatherPeriod duskToMidnight) {}
