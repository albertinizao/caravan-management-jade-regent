package com.gestioncaravana.adapter.in.web;

public record WeatherPeriodResponse(
    String precipitation,
    String windStrength,
    Integer temperatureC,
    Integer temperatureF) {}
