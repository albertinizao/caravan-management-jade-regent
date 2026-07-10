package com.gestioncaravana.application.model;

public record WeatherPeriodView(
    String precipitation,
    String windStrength,
    Integer temperatureC,
    Integer temperatureF) {}
