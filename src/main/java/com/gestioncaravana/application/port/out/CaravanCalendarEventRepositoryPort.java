package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CustomCalendarEvent;
import com.gestioncaravana.domain.GolarionDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanCalendarEventRepositoryPort {

  CustomCalendarEvent save(CustomCalendarEvent event);

  List<CustomCalendarEvent> findByCaravanIdAndDate(UUID caravanId, GolarionDate date);

  List<CustomCalendarEvent> findByCaravanIdAndDateBetween(
      UUID caravanId, GolarionDate startDate, GolarionDate endDate);

  Optional<CustomCalendarEvent> findByCaravanIdAndId(UUID caravanId, Long eventId);

  void deleteByCaravanIdAndId(UUID caravanId, Long eventId);

  void deleteByCaravanId(UUID caravanId);
}
