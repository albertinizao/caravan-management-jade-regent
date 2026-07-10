package com.gestioncaravana.adapter.in.web;

public record GolarionDateResponse(
    int year,
    int month,
    String monthName,
    int day,
    String dayOfWeek,
    String dayOfWeekAbbreviation) {}
