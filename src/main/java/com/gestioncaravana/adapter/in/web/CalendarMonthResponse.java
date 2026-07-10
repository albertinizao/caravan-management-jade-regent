package com.gestioncaravana.adapter.in.web;

import java.util.List;
import java.util.UUID;

public record CalendarMonthResponse(
    UUID caravanId,
    GolarionDateResponse currentDate,
    int displayYear,
    int displayMonth,
    String displayMonthName,
    List<String> weekDayHeaders,
    List<CalendarDayResponse> days) {}
