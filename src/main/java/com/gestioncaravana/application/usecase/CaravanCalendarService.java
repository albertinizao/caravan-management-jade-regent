package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CalendarDayView;
import com.gestioncaravana.application.model.CalendarEventView;
import com.gestioncaravana.application.model.CalendarMonthView;
import com.gestioncaravana.application.model.GolarionDateView;
import com.gestioncaravana.application.port.in.AdvanceCaravanCalendarUseCase;
import com.gestioncaravana.application.port.in.CreateCaravanCalendarEventUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanCalendarEventUseCase;
import com.gestioncaravana.application.port.in.GetCaravanCalendarDayUseCase;
import com.gestioncaravana.application.port.in.GetCaravanCalendarMonthUseCase;
import com.gestioncaravana.application.port.in.GetCaravanWeatherSnapshotUseCase;
import com.gestioncaravana.application.port.in.SetCaravanCalendarCurrentDateUseCase;
import com.gestioncaravana.application.port.out.CaravanCalendarEventRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.GolarionCalendarEventCatalogPort;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CustomCalendarEvent;
import com.gestioncaravana.domain.GolarionCalendar;
import com.gestioncaravana.domain.GolarionDate;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CaravanCalendarService
    implements GetCaravanCalendarMonthUseCase,
        GetCaravanCalendarDayUseCase,
        SetCaravanCalendarCurrentDateUseCase,
        AdvanceCaravanCalendarUseCase,
        CreateCaravanCalendarEventUseCase,
        DeleteCaravanCalendarEventUseCase {

  private static final List<String> WEEK_HEADERS = List.of("Lun", "Tra", "For", "Jur", "Fue", "Est", "Sol");
  private static final List<CalendarEventView> NO_CUSTOM_EVENTS = List.of();

  private final CaravanCampaignRepositoryPort campaignRepository;
  private final CaravanSupplyStateRepositoryPort supplyStateRepository;
  private final CaravanCalendarEventRepositoryPort calendarEventRepository;
  private final GolarionCalendarEventCatalogPort eventCatalogPort;
  private final GetCaravanWeatherSnapshotUseCase weatherSnapshotUseCase;
  private final Clock clock;

  public CaravanCalendarService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanSupplyStateRepositoryPort supplyStateRepository,
      CaravanCalendarEventRepositoryPort calendarEventRepository,
      GolarionCalendarEventCatalogPort eventCatalogPort,
      GetCaravanWeatherSnapshotUseCase weatherSnapshotUseCase,
      Clock clock) {
    this.campaignRepository = campaignRepository;
    this.supplyStateRepository = supplyStateRepository;
    this.calendarEventRepository = calendarEventRepository;
    this.eventCatalogPort = eventCatalogPort;
    this.weatherSnapshotUseCase = weatherSnapshotUseCase;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public CalendarMonthView getMonth(UUID caravanId, int year, int month) {
    requireCaravan(caravanId);
    var currentDate = currentDate(caravanId);
    var displayMonthAnchor = new GolarionDate(year, month, 1);
    GolarionCalendar.validateSupportedRange(displayMonthAnchor);

    var firstDay = displayMonthAnchor;
    var leadingDays = firstDay.dayOfWeek().index();
    var gridStart = GolarionCalendar.addDays(firstDay, -leadingDays);
    var gridEnd = GolarionCalendar.addDays(gridStart, 41);
    var customEventsByDate = groupCustomEventsByDate(
        calendarEventRepository.findByCaravanIdAndDateBetween(caravanId, gridStart, gridEnd));
    var cells = java.util.stream.IntStream.range(0, 42)
        .mapToObj(index -> {
          var date = GolarionCalendar.addDays(gridStart, index);
          return toDayView(caravanId, date, currentDate, month, customEventsByDate.getOrDefault(date, NO_CUSTOM_EVENTS));
        })
        .toList();

    return new CalendarMonthView(
        caravanId,
        toView(currentDate),
        year,
        month,
        displayMonthAnchor.monthName(),
        WEEK_HEADERS,
        cells);
  }

  @Override
  @Transactional(readOnly = true)
  public CalendarDayView getDay(UUID caravanId, int year, int month, int day) {
    requireCaravan(caravanId);
    var currentDate = currentDate(caravanId);
    var requestedDate = new GolarionDate(year, month, day);
    GolarionCalendar.validateSupportedRange(requestedDate);
    return toDayView(
        caravanId,
        requestedDate,
        currentDate,
        month,
        calendarEventRepository.findByCaravanIdAndDate(caravanId, requestedDate).stream()
            .map(this::toView)
            .toList());
  }

  @Override
  public CalendarDayView create(UUID caravanId, CreateCaravanCalendarEventCommand command) {
    requireCaravan(caravanId);
    if (command.year() == null || command.month() == null || command.day() == null) {
      throw new IllegalArgumentException("year, month and day are required");
    }
    if (command.name() == null || command.name().isBlank()) {
      throw new IllegalArgumentException("name is required");
    }

    var requestedDate = new GolarionDate(command.year(), command.month(), command.day());
    GolarionCalendar.validateSupportedRange(requestedDate);

    calendarEventRepository.save(
        new CustomCalendarEvent(
            null,
            caravanId,
            requestedDate,
            command.name().trim(),
            normalizeDescription(command.description()),
            command.secret(),
            clock.instant()));

    var currentDate = currentDate(caravanId);
    return toDayView(
        caravanId,
        requestedDate,
        currentDate,
        requestedDate.month(),
        calendarEventRepository.findByCaravanIdAndDate(caravanId, requestedDate).stream()
            .map(this::toView)
            .toList());
  }

  @Override
  public CalendarDayView delete(UUID caravanId, Long eventId) {
    requireCaravan(caravanId);
    if (eventId == null) {
      throw new IllegalArgumentException("eventId is required");
    }

    var event = calendarEventRepository.findByCaravanIdAndId(caravanId, eventId)
        .orElseThrow(() -> new IllegalArgumentException("Calendar event not found: " + eventId));
    calendarEventRepository.deleteByCaravanIdAndId(caravanId, eventId);

    var currentDate = currentDate(caravanId);
    return toDayView(
        caravanId,
        event.date(),
        currentDate,
        event.date().month(),
        calendarEventRepository.findByCaravanIdAndDate(caravanId, event.date()).stream()
            .map(this::toView)
            .toList());
  }

  @Override
  public CalendarDayView setCurrentDate(UUID caravanId, SetCaravanCalendarCurrentDateCommand command) {
    requireCaravan(caravanId);
    if (command.year() == null || command.month() == null || command.day() == null) {
      throw new IllegalArgumentException("year, month and day are required");
    }
    var requestedDate = new GolarionDate(command.year(), command.month(), command.day());
    GolarionCalendar.validateSupportedRange(requestedDate);

    var state = requireSupplyState(caravanId);
    var updated = new CaravanSupplyState(
        state.caravanId(),
        state.provisionReserve(),
        state.standardReserve(),
        state.perishableReserve(),
        GolarionCalendar.toOffset(requestedDate),
        clock.instant(),
        state.sharedJobProductivityState());
    supplyStateRepository.save(updated);
    return toDayView(
        caravanId,
        requestedDate,
        requestedDate,
        requestedDate.month(),
        calendarEventRepository.findByCaravanIdAndDate(caravanId, requestedDate).stream()
            .map(this::toView)
            .toList());
  }

  @Override
  public CalendarDayView advance(UUID caravanId, AdvanceCaravanCalendarCommand command) {
    requireCaravan(caravanId);
    if (command.days() == null) {
      throw new IllegalArgumentException("days is required");
    }
    if (command.days() == 0) {
      throw new IllegalArgumentException("days must not be 0");
    }
    var state = requireSupplyState(caravanId);
    var nextDate = GolarionCalendar.fromOffset(state.daysPassed() + command.days());
    var updated = new CaravanSupplyState(
        state.caravanId(),
        state.provisionReserve(),
        state.standardReserve(),
        state.perishableReserve(),
        GolarionCalendar.toOffset(nextDate),
        clock.instant(),
        state.sharedJobProductivityState());
    supplyStateRepository.save(updated);
    return toDayView(
        caravanId,
        nextDate,
        nextDate,
        nextDate.month(),
        calendarEventRepository.findByCaravanIdAndDate(caravanId, nextDate).stream()
            .map(this::toView)
            .toList());
  }

  private CalendarDayView toDayView(
      UUID caravanId,
      GolarionDate date,
      GolarionDate currentDate,
      int requestedMonth,
      List<CalendarEventView> customEvents) {
    var canonicalEvents = List.copyOf(eventCatalogPort.findEventsByDate(date));
    return new CalendarDayView(
        toView(date),
        date.compareTo(currentDate) == 0,
        date.month() == requestedMonth,
        canonicalEvents,
        List.copyOf(customEvents),
        weatherSnapshotUseCase.getWeather(caravanId, date));
  }

  private GolarionDateView toView(GolarionDate date) {
    var dayOfWeek = date.dayOfWeek();
    return new GolarionDateView(
        date.year(),
        date.month(),
        date.monthName(),
        date.day(),
        dayOfWeek.displayName(),
        dayOfWeek.abbreviation());
  }

  private Map<GolarionDate, List<CalendarEventView>> groupCustomEventsByDate(List<CustomCalendarEvent> events) {
    var grouped = new LinkedHashMap<GolarionDate, List<CalendarEventView>>();
    for (var event : events) {
      grouped.computeIfAbsent(event.date(), ignored -> new java.util.ArrayList<>()).add(toView(event));
    }
    grouped.replaceAll((date, eventViews) -> List.copyOf(eventViews));
    return Map.copyOf(grouped);
  }

  private CalendarEventView toView(CustomCalendarEvent event) {
    return new CalendarEventView(event.id(), event.name(), null, event.description(), "CUSTOM", event.secret());
  }

  private String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }

  private GolarionDate currentDate(UUID caravanId) {
    return GolarionCalendar.fromOffset(requireSupplyState(caravanId).daysPassed());
  }

  private CaravanSupplyState requireSupplyState(UUID caravanId) {
    return supplyStateRepository.findByCaravanId(caravanId)
        .orElseGet(() -> supplyStateRepository.save(CaravanSupplyState.initial(caravanId, clock.instant())));
  }

  private void requireCaravan(UUID caravanId) {
    campaignRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }
}
