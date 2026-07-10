package com.gestioncaravana.application.model;

public record GolarionDateView(
    int year,
    int month,
    String monthName,
    int day,
    String dayOfWeek,
    String dayOfWeekAbbreviation) {}
