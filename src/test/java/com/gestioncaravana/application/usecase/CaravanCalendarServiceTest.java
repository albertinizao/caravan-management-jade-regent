package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.model.CalendarEventView;
import com.gestioncaravana.application.port.in.AdvanceCaravanCalendarUseCase.AdvanceCaravanCalendarCommand;
import com.gestioncaravana.application.port.in.CreateCaravanCalendarEventUseCase.CreateCaravanCalendarEventCommand;
import com.gestioncaravana.application.port.in.SetCaravanCalendarCurrentDateUseCase.SetCaravanCalendarCurrentDateCommand;
import com.gestioncaravana.application.port.out.CaravanCalendarEventRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.GolarionCalendarEventCatalogPort;
import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.CustomCalendarEvent;
import com.gestioncaravana.domain.GolarionDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaravanCalendarServiceTest {

  private InMemoryCaravanRepository campaignRepository;
  private InMemorySupplyStateRepository supplyStateRepository;
  private InMemoryCalendarEventRepository calendarEventRepository;
  private InMemoryWeatherProfileRepository weatherProfileRepository;
  private InMemoryWeatherSnapshotRepository weatherSnapshotRepository;
  private CaravanCalendarService service;
  private UUID caravanId;

  @BeforeEach
  void setUp() {
    campaignRepository = new InMemoryCaravanRepository();
    supplyStateRepository = new InMemorySupplyStateRepository();
    calendarEventRepository = new InMemoryCalendarEventRepository();
    weatherProfileRepository = new InMemoryWeatherProfileRepository();
    weatherSnapshotRepository = new InMemoryWeatherSnapshotRepository();
    var weatherService = new CaravanWeatherService(
        campaignRepository,
        weatherProfileRepository,
        weatherSnapshotRepository,
        Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC));
    service = new CaravanCalendarService(
        campaignRepository,
        supplyStateRepository,
        calendarEventRepository,
        date -> {
          if (date.equals(new GolarionDate(4712, 1, 1))) {
            return List.of(new CalendarEventView(null, "New Year", "todo Golarion", "Inicio del año civil.", "CANONICAL", false));
          }
          if (date.equals(new GolarionDate(4712, 3, 20))) {
            return List.of(new CalendarEventView(null, "Equinoccio de primavera", null, "20 Farasto", "ASTRONOMICAL", false));
          }
          return List.of();
        },
        weatherService,
        Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC));

    caravanId = UUID.randomUUID();
    campaignRepository.save(CaravanCampaign.create(caravanId, "Calendario", null, Instant.parse("2026-01-01T00:00:00Z")));
    supplyStateRepository.save(CaravanSupplyState.initial(caravanId, Instant.parse("2026-01-01T00:00:00Z")));
  }

  @Test
  void returnsCurrentMonthAndMarksCurrentDay() {
    var month = service.getMonth(caravanId, 4712, 1);

    assertThat(month.currentDate().year()).isEqualTo(4712);
    assertThat(month.currentDate().month()).isEqualTo(1);
    assertThat(month.currentDate().day()).isEqualTo(1);
    assertThat(month.days()).hasSize(42);
    assertThat(month.days()).anySatisfy(day -> {
      assertThat(day.currentDay()).isTrue();
      assertThat(day.date().year()).isEqualTo(4712);
      assertThat(day.date().month()).isEqualTo(1);
      assertThat(day.date().day()).isEqualTo(1);
    });
  }

  @Test
  void returnsResolvedEventsForADay() {
    var day = service.getDay(caravanId, 4712, 3, 20);

    assertThat(day.canonicalEvents()).extracting(CalendarEventView::name)
        .contains("Equinoccio de primavera");
    assertThat(day.weather()).isNotNull();
    assertThat(day.weather().dawnToNoon()).isNotNull();
  }

  @Test
  void createsCustomEventsAndReturnsThemInDayDetails() {
    var updated = service.create(
        caravanId,
        new CreateCaravanCalendarEventCommand(4712, 3, 20, "Reunión secreta", "Solo para la caravana", true));

    assertThat(updated.customEvents()).singleElement()
        .satisfies(event -> {
          assertThat(event.id()).isNotNull();
          assertThat(event.name()).isEqualTo("Reunión secreta");
          assertThat(event.description()).isEqualTo("Solo para la caravana");
          assertThat(event.secret()).isTrue();
          assertThat(event.category()).isEqualTo("CUSTOM");
        });
  }

  @Test
  void deletesCustomEventsAndReturnsUpdatedDay() {
    var created = service.create(
        caravanId,
        new CreateCaravanCalendarEventCommand(4712, 3, 20, "Reunión secreta", "Solo para la caravana", true));
    var customEventId = created.customEvents().getFirst().id();

    var updated = service.delete(caravanId, customEventId);

    assertThat(updated.customEvents()).isEmpty();
    assertThat(calendarEventRepository.findByCaravanIdAndId(caravanId, customEventId)).isEmpty();
  }

  @Test
  void returnsCustomEventsInMonthGridAcrossAdjacentDays() {
    calendarEventRepository.save(new CustomCalendarEvent(
        null,
        caravanId,
        new GolarionDate(4712, 3, 1),
        "Evento de marzo",
        null,
        false,
        Instant.parse("2026-01-01T00:00:00Z")));

    var month = service.getMonth(caravanId, 4712, 2);

    assertThat(month.days())
        .filteredOn(day -> day.date().year() == 4712 && day.date().month() == 3 && day.date().day() == 1)
        .singleElement()
        .satisfies(day -> assertThat(day.customEvents()).extracting(CalendarEventView::name).contains("Evento de marzo"));
  }

  @Test
  void setsCurrentDateByRecalculatingDaysPassed() {
    var updated = service.setCurrentDate(caravanId, new SetCaravanCalendarCurrentDateCommand(4712, 3, 20));

    assertThat(updated.currentDay()).isTrue();
    assertThat(updated.date().year()).isEqualTo(4712);
    assertThat(updated.date().month()).isEqualTo(3);
    assertThat(updated.date().day()).isEqualTo(20);
    assertThat(supplyStateRepository.findByCaravanId(caravanId))
        .hasValueSatisfying(state -> assertThat(state.daysPassed()).isEqualTo(79));
  }

  @Test
  void advancesDaysInBulk() {
    service.setCurrentDate(caravanId, new SetCaravanCalendarCurrentDateCommand(4712, 3, 20));

    var updated = service.advance(caravanId, new AdvanceCaravanCalendarCommand(10));

    assertThat(updated.date().year()).isEqualTo(4712);
    assertThat(updated.date().month()).isEqualTo(3);
    assertThat(updated.date().day()).isEqualTo(30);
  }

  @Test
  void rejectsDatesOutsideSupportedRange() {
    assertThatThrownBy(() -> service.setCurrentDate(caravanId, new SetCaravanCalendarCurrentDateCommand(4723, 1, 1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.advance(caravanId, new AdvanceCaravanCalendarCommand(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static final class InMemoryCaravanRepository implements CaravanCampaignRepositoryPort {
    private final List<CaravanCampaign> caravans = new ArrayList<>();

    @Override
    public CaravanCampaign save(CaravanCampaign caravanCampaign) {
      caravans.removeIf(existing -> existing.id().equals(caravanCampaign.id()));
      caravans.add(caravanCampaign);
      return caravanCampaign;
    }

    @Override
    public void deleteById(UUID id) {
      caravans.removeIf(existing -> existing.id().equals(id));
    }

    @Override
    public List<CaravanCampaign> findAll() {
      return List.copyOf(caravans);
    }

    @Override
    public Optional<CaravanCampaign> findById(UUID id) {
      return caravans.stream().filter(caravan -> caravan.id().equals(id)).findFirst();
    }
  }

  private static final class InMemorySupplyStateRepository implements CaravanSupplyStateRepositoryPort {
    private final Map<UUID, CaravanSupplyState> states = new java.util.HashMap<>();

    @Override
    public CaravanSupplyState save(CaravanSupplyState state) {
      states.put(state.caravanId(), state);
      return state;
    }

    @Override
    public Optional<CaravanSupplyState> findByCaravanId(UUID caravanId) {
      return Optional.ofNullable(states.get(caravanId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      states.remove(caravanId);
    }
  }

  private static final class InMemoryCalendarEventRepository implements CaravanCalendarEventRepositoryPort {
    private final java.util.List<CustomCalendarEvent> events = new java.util.ArrayList<>();

    @Override
    public CustomCalendarEvent save(CustomCalendarEvent event) {
      var saved = new CustomCalendarEvent(
          event.id() == null ? (long) events.size() + 1 : event.id(),
          event.caravanId(),
          event.date(),
          event.name(),
          event.description(),
          event.secret(),
          event.createdAt());
      events.add(saved);
      return saved;
    }

    @Override
    public java.util.List<CustomCalendarEvent> findByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
      return events.stream()
          .filter(event -> event.caravanId().equals(caravanId) && event.date().equals(date))
          .toList();
    }

    @Override
    public java.util.List<CustomCalendarEvent> findByCaravanIdAndDateBetween(
        UUID caravanId, GolarionDate startDate, GolarionDate endDate) {
      return events.stream()
          .filter(event -> event.caravanId().equals(caravanId))
          .filter(event -> event.date().compareTo(startDate) >= 0 && event.date().compareTo(endDate) <= 0)
          .toList();
    }

    @Override
    public Optional<CustomCalendarEvent> findByCaravanIdAndId(UUID caravanId, Long eventId) {
      return events.stream()
          .filter(event -> event.caravanId().equals(caravanId) && event.id().equals(eventId))
          .findFirst();
    }

    @Override
    public void deleteByCaravanIdAndId(UUID caravanId, Long eventId) {
      events.removeIf(event -> event.caravanId().equals(caravanId) && event.id().equals(eventId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      events.removeIf(event -> event.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryWeatherProfileRepository implements CaravanWeatherProfileRepositoryPort {
    private final Map<UUID, CaravanWeatherProfile> profiles = new java.util.HashMap<>();

    @Override
    public CaravanWeatherProfile save(CaravanWeatherProfile profile) {
      profiles.put(profile.caravanId(), profile);
      return profile;
    }

    @Override
    public Optional<CaravanWeatherProfile> findByCaravanId(UUID caravanId) {
      return Optional.ofNullable(profiles.get(caravanId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      profiles.remove(caravanId);
    }
  }

  private static final class InMemoryWeatherSnapshotRepository implements CaravanWeatherSnapshotRepositoryPort {
    private final Map<String, CaravanWeatherSnapshot> snapshots = new java.util.HashMap<>();

    @Override
    public CaravanWeatherSnapshot save(CaravanWeatherSnapshot snapshot) {
      snapshots.put(key(snapshot.caravanId(), snapshot.date()), snapshot);
      return snapshot;
    }

    @Override
    public Optional<CaravanWeatherSnapshot> findByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
      return Optional.ofNullable(snapshots.get(key(caravanId, date)));
    }

    @Override
    public void deleteFromDate(UUID caravanId, GolarionDate fromDate) {
      snapshots.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":")
          && toDate(entry.getKey()).compareTo(fromDate) >= 0);
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      snapshots.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":"));
    }

    private String key(UUID caravanId, GolarionDate date) {
      return caravanId + ":" + date.year() + ":" + date.month() + ":" + date.day();
    }

    private GolarionDate toDate(String key) {
      var parts = key.split(":");
      return new GolarionDate(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
  }
}
