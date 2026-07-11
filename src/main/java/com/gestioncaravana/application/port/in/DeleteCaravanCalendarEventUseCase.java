package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CalendarDayView;
import java.util.UUID;

public interface DeleteCaravanCalendarEventUseCase {

  CalendarDayView delete(UUID caravanId, Long eventId);
}
