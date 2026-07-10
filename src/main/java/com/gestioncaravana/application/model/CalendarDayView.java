package com.gestioncaravana.application.model;

import java.util.List;

public record CalendarDayView(
    GolarionDateView date,
    boolean currentDay,
    boolean inCurrentMonth,
    List<CalendarEventView> canonicalEvents,
    List<CalendarEventView> customEvents,
    WeatherSnapshotView weather) {}
