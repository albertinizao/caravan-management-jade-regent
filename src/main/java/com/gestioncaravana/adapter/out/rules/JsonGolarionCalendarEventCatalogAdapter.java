package com.gestioncaravana.adapter.out.rules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestioncaravana.application.model.CalendarEventView;
import com.gestioncaravana.application.port.out.GolarionCalendarEventCatalogPort;
import com.gestioncaravana.domain.GolarionDate;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonGolarionCalendarEventCatalogAdapter implements GolarionCalendarEventCatalogPort {

  private final Map<String, List<CalendarEventView>> eventsByDate;

  public JsonGolarionCalendarEventCatalogAdapter() {
    this.eventsByDate = load(new ObjectMapper());
  }

  @Override
  public List<CalendarEventView> findEventsByDate(GolarionDate date) {
    return eventsByDate.getOrDefault(formatKey(date), List.of());
  }

  private Map<String, List<CalendarEventView>> load(ObjectMapper objectMapper) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("golarion-calendar-events.json")) {
      if (inputStream == null) {
        throw new IllegalStateException("Calendar event catalog resource not found");
      }
      return objectMapper.readValue(inputStream, CatalogPayload.class).toViews();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load calendar event catalog", e);
    }
  }

  private String formatKey(GolarionDate date) {
    return "%04d-%02d-%02d".formatted(date.year(), date.month(), date.day());
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record CatalogPayload(Map<String, List<EventPayload>> dates) {
    Map<String, List<CalendarEventView>> toViews() {
      return dates.entrySet().stream()
          .collect(java.util.stream.Collectors.toUnmodifiableMap(
              Map.Entry::getKey,
              entry -> entry.getValue().stream().map(EventPayload::toView).toList()));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record EventPayload(
      String name,
      String scope,
      String description,
      String category) {
    CalendarEventView toView() {
      return new CalendarEventView(name, scope, description, category);
    }
  }
}
