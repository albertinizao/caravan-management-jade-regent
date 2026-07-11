package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CalendarDayView;
import java.util.UUID;

public interface GetCaravanCalendarDayUseCase {

  CalendarDayView getDay(UUID caravanId, int year, int month, int day, boolean showSecretsVisible);
}
