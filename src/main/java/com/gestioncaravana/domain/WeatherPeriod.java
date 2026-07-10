package com.gestioncaravana.domain;

public record WeatherPeriod(
    String precipitation,
    String windStrength,
    Integer temperatureC,
    Integer temperatureF) {}
