package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CalendarMonthView;
import java.util.UUID;

public interface GetCaravanCalendarMonthUseCase {

  CalendarMonthView getMonth(UUID caravanId, int year, int month, boolean showSecretsVisible);
}
