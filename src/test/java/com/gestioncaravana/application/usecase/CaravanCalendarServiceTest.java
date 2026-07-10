package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.model.CalendarEventView;
import com.gestioncaravana.application.port.in.AdvanceCaravanCalendarUseCase.AdvanceCaravanCalendarCommand;
import com.gestioncaravana.application.port.in.SetCaravanCalendarCurrentDateUseCase.SetCaravanCalendarCurrentDateCommand;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.GolarionCalendarEventCatalogPort;
import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
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
  private InMemoryWeatherProfileRepository weatherProfileRepository;
  private InMemoryWeatherSnapshotRepository weatherSnapshotRepository;
  private CaravanCalendarService service;
  private UUID caravanId;

  @BeforeEach
  void setUp() {
    campaignRepository = new InMemoryCaravanRepository();
    supplyStateRepository = new InMemorySupplyStateRepository();
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
        date -> {
          if (date.equals(new GolarionDate(4712, 1, 1))) {
            return List.of(new CalendarEventView("New Year", "todo Golarion", "Inicio del año civil.", "CANONICAL"));
          }
          if (date.equals(new GolarionDate(4712, 3, 20))) {
            return List.of(new CalendarEventView("Equinoccio de primavera", null, "20 Farasto", "ASTRONOMICAL"));
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
    public void deleteByCaravanId(UUID caravanId) {
      snapshots.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":"));
    }

    private String key(UUID caravanId, GolarionDate date) {
      return caravanId + ":" + date.year() + ":" + date.month() + ":" + date.day();
    }
  }
}
