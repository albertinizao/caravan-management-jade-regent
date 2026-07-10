package com.gestioncaravana.adapter.in.web;

import java.util.List;

public record CalendarDayResponse(
    GolarionDateResponse date,
    boolean isCurrentDay,
    boolean isInCurrentMonth,
    List<CalendarEventResponse> canonicalEvents,
    List<CalendarEventResponse> customEvents,
    WeatherSnapshotResponse weather) {}
