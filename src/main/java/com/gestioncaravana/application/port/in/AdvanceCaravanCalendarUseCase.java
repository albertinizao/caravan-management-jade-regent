package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CalendarDayView;
import java.util.UUID;

public interface AdvanceCaravanCalendarUseCase {

  CalendarDayView advance(UUID caravanId, AdvanceCaravanCalendarCommand command);

  record AdvanceCaravanCalendarCommand(Integer days) {}
}
