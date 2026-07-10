package com.gestioncaravana.application.model;

import java.util.List;
import java.util.UUID;

public record CalendarMonthView(
    UUID caravanId,
    GolarionDateView currentDate,
    int displayYear,
    int displayMonth,
    String displayMonthName,
    List<String> weekDayHeaders,
    List<CalendarDayView> days) {}
