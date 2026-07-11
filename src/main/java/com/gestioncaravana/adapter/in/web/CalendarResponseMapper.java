package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CalendarDayView;
import com.gestioncaravana.application.model.CalendarEventView;
import com.gestioncaravana.application.model.CalendarMonthView;
import com.gestioncaravana.application.model.GolarionDateView;
import com.gestioncaravana.application.model.WeatherPeriodView;
import com.gestioncaravana.application.model.WeatherSnapshotView;

final class CalendarResponseMapper {

  private CalendarResponseMapper() {}

  static CalendarMonthResponse toResponse(CalendarMonthView view) {
    return new CalendarMonthResponse(
        view.caravanId(),
        toResponse(view.currentDate()),
        view.displayYear(),
        view.displayMonth(),
        view.displayMonthName(),
        view.weekDayHeaders(),
        view.days().stream().map(CalendarResponseMapper::toResponse).toList());
  }

  static CalendarDayResponse toResponse(CalendarDayView view) {
    return new CalendarDayResponse(
        toResponse(view.date()),
        view.currentDay(),
        view.inCurrentMonth(),
        view.canonicalEvents().stream().map(CalendarResponseMapper::toResponse).toList(),
        view.customEvents().stream().map(CalendarResponseMapper::toResponse).toList(),
        toResponse(view.weather()));
  }

  private static GolarionDateResponse toResponse(GolarionDateView view) {
    return new GolarionDateResponse(
        view.year(),
        view.month(),
        view.monthName(),
        view.day(),
        view.dayOfWeek(),
        view.dayOfWeekAbbreviation());
  }

  private static CalendarEventResponse toResponse(CalendarEventView view) {
    return new CalendarEventResponse(
        view.id(), view.name(), view.scope(), view.description(), view.category(), view.secret());
  }

  private static WeatherSnapshotResponse toResponse(WeatherSnapshotView view) {
    if (view == null) {
      return null;
    }
    return new WeatherSnapshotResponse(
        toResponse(view.midnightToDawn()),
        toResponse(view.dawnToNoon()),
        toResponse(view.noonToDusk()),
        toResponse(view.duskToMidnight()),
        view.crownLightCondition());
  }

  private static WeatherPeriodResponse toResponse(WeatherPeriodView view) {
    if (view == null) {
      return null;
    }
    return new WeatherPeriodResponse(
        view.precipitation(),
        view.windStrength(),
        view.temperatureC(),
        view.temperatureF());
  }
}
