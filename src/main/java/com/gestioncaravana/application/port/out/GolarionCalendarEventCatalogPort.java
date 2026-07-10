package com.gestioncaravana.application.port.out;

import com.gestioncaravana.application.model.CalendarEventView;
import com.gestioncaravana.domain.GolarionDate;
import java.util.List;

public interface GolarionCalendarEventCatalogPort {

  List<CalendarEventView> findEventsByDate(GolarionDate date);
}
