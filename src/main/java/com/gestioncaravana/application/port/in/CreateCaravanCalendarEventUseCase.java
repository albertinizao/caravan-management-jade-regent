package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CalendarDayView;
import java.util.UUID;

public interface CreateCaravanCalendarEventUseCase {

  CalendarDayView create(UUID caravanId, CreateCaravanCalendarEventCommand command);

  record CreateCaravanCalendarEventCommand(
      Integer year,
      Integer month,
      Integer day,
      String name,
      String description,
      boolean secret) {}
}
