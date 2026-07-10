package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CalendarDayView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public interface SetCaravanCalendarCurrentDateUseCase {

  CalendarDayView setCurrentDate(UUID caravanId, SetCaravanCalendarCurrentDateCommand command);

  record SetCaravanCalendarCurrentDateCommand(
      @Min(4712) @Max(4722) Integer year,
      @Min(1) @Max(12) Integer month,
      @Min(1) @Max(31) Integer day) {}
}
