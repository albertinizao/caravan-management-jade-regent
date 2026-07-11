package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.in.CreateCaravanUseCase.CreateCaravanCommand;
import com.gestioncaravana.application.port.out.ActiveCaravanSelectionPort;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanDayResolutionRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatCatalogPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherForecastStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanDayResolution;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.CaravanWeatherForecastState;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.GolarionDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaravanManagementServiceTest {

  private InMemoryCaravanRepository repository;
  private InMemoryActiveSelection activeSelection;
  private InMemoryCaravanWagonRepository wagonRepository;
  private InMemoryWagonImprovementRepository wagonImprovementRepository;
  private InMemoryCargoRepository cargoRepository;
  private InMemoryFeatRepository featRepository;
  private InMemoryWeatherProfileRepository weatherProfileRepository;
  private InMemoryWeatherForecastStateRepository weatherForecastStateRepository;
  private InMemoryWeatherSnapshotRepository weatherSnapshotRepository;
  private CaravanManagementService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCaravanRepository();
    activeSelection = new InMemoryActiveSelection();
    wagonRepository = new InMemoryCaravanWagonRepository();
    wagonImprovementRepository = new InMemoryWagonImprovementRepository();
    cargoRepository = new InMemoryCargoRepository();
    featRepository = new InMemoryFeatRepository();
    weatherProfileRepository = new InMemoryWeatherProfileRepository();
    weatherForecastStateRepository = new InMemoryWeatherForecastStateRepository();
    weatherSnapshotRepository = new InMemoryWeatherSnapshotRepository();
    var weatherService = new CaravanWeatherService(
        repository,
        weatherForecastStateRepository,
        weatherProfileRepository,
        weatherSnapshotRepository,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    service = new CaravanManagementService(
        repository,
        wagonRepository,
        wagonImprovementRepository,
        new InMemoryTravelerRepository(),
        cargoRepository,
        new InMemoryBeastRepository(),
        featRepository,
        new InMemorySupplyStateRepository(),
        new InMemoryDayResolutionRepository(),
        weatherService,
        new InMemoryFeatCatalogPort(),
        activeSelection,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void createsAndListsCaravans() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, null, null, null, null));

    assertThat(created.name()).isEqualTo("Campaign");
    assertThat(created.level()).isEqualTo(1);
    assertThat(created.active()).isFalse();
    assertThat(service.list()).hasSize(1);
  }

  @Test
  void selectsAndReturnsTheActiveCaravan() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, null, null, null, null));

    var selected = service.select(created.id());

    assertThat(selected.active()).isTrue();
    assertThat(service.getActive()).isPresent();
    assertThat(service.getActive()).map(caravan -> caravan.id()).hasValue(created.id());
  }

  @Test
  void deletesCaravansAndClearsTheActiveSelectionWhenNeeded() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, null, null, null, null));
    var wagonId = UUID.randomUUID();
    wagonRepository.save(CaravanWagon.create(wagonId, created.id(), "wagon", "Load Bearer", Instant.parse("2026-01-01T00:00:00Z")));
    wagonImprovementRepository.save(CaravanWagonImprovement.create(
        UUID.randomUUID(),
        created.id(),
        wagonId,
        "reinforced-axle",
        Instant.parse("2026-01-01T00:00:00Z")));
    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        created.id(),
        CaravanCargoSourceType.CUSTOM,
        null,
        "Supplies",
        "Food",
        1,
        1,
        wagonId,
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));
    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        created.id(),
        "caravana-mejorada",
        CaravanFeatAcquisitionSourceType.LEVEL_UP,
        2,
        null,
        1,
        true,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));
    service.select(created.id());

    service.delete(created.id());

    assertThat(service.list()).isEmpty();
    assertThat(service.getActive()).isEmpty();
    assertThat(wagonRepository.findAllByCaravanId(created.id())).isEmpty();
    assertThat(wagonImprovementRepository.findAllByCaravanIdAndWagonId(created.id(), wagonId)).isEmpty();
    assertThat(cargoRepository.findAllByCaravanId(created.id())).isEmpty();
    assertThat(featRepository.findAllByCaravanId(created.id())).isEmpty();
  }

  @Test
  void throwsWhenSelectingUnknownCaravan() {
    assertThatThrownBy(() -> service.select(UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Caravan not found");
  }

  @Test
  void showsFeatNamesInTheCaravanSummary() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, null, null, null, null));
    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        created.id(),
        "caravana-mejorada",
        CaravanFeatAcquisitionSourceType.LEVEL_UP,
        2,
        null,
        1,
        true,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));

    var caravan = service.getById(created.id());

    assertThat(caravan.feats()).containsExactly("Caravana Mejorada");
  }

  @Test
  void adjustsLevelAndDiscontentForTheSelectedCaravan() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, null, null, null, null));
    service.select(created.id());

    var leveledUp = service.execute(created.id(), new com.gestioncaravana.application.port.in.UpdateCaravanLevelUseCase.UpdateCaravanLevelCommand(1));
    var moreDiscontent = service.execute(created.id(), new com.gestioncaravana.application.port.in.UpdateCaravanDiscontentUseCase.UpdateCaravanDiscontentCommand(2));
    var leveledDown = service.execute(created.id(), new com.gestioncaravana.application.port.in.UpdateCaravanLevelUseCase.UpdateCaravanLevelCommand(-1));
    var lessDiscontent = service.execute(created.id(), new com.gestioncaravana.application.port.in.UpdateCaravanDiscontentUseCase.UpdateCaravanDiscontentCommand(-1));

    assertThat(leveledUp.level()).isEqualTo(2);
    assertThat(moreDiscontent.discontent()).isEqualTo(2);
    assertThat(leveledDown.level()).isEqualTo(1);
    assertThat(lessDiscontent.discontent()).isEqualTo(1);
    assertThat(lessDiscontent.active()).isTrue();
    assertThat(service.getActive()).map(caravan -> caravan.level()).hasValue(1);
  }

  @Test
  void rejectsUpdatesThatWouldBreakTheMinimumBounds() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, null, null, null, null));

    assertThatThrownBy(() -> service.execute(
        created.id(),
        new com.gestioncaravana.application.port.in.UpdateCaravanLevelUseCase.UpdateCaravanLevelCommand(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("level must be greater than or equal to 1");
    assertThatThrownBy(() -> service.execute(
        created.id(),
        new com.gestioncaravana.application.port.in.UpdateCaravanDiscontentUseCase.UpdateCaravanDiscontentCommand(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("discontent must be greater than or equal to 0");
  }

  @Test
  void updatesMainStatsOnlyWhenUnassignedPointsAreAvailable() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, 2, 2, 1, 1));

    var updated = service.execute(
        created.id(),
        new com.gestioncaravana.application.port.in.UpdateCaravanMainStatsUseCase.UpdateCaravanMainStatsCommand(3, 2, 1, 1));

    assertThat(updated.mainStats().offense()).isEqualTo(3);
    assertThat(updated.mainStats().defense()).isEqualTo(2);
    assertThat(updated.mainStats().mobility()).isEqualTo(1);
    assertThat(updated.mainStats().morale()).isEqualTo(1);
    assertThat(updated.mainStats().unassignedPoints()).isZero();
  }

  @Test
  void rejectsMainStatsUpdatesWhenThereAreNoFreePoints() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null, 2, 2, 2, 1));

    assertThatThrownBy(() -> service.execute(
        created.id(),
        new com.gestioncaravana.application.port.in.UpdateCaravanMainStatsUseCase.UpdateCaravanMainStatsCommand(4, 2, 1, 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No unassigned points available");
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

  private static final class InMemoryCaravanWagonRepository implements CaravanWagonRepositoryPort {
    private final List<CaravanWagon> wagons = new ArrayList<>();

    @Override
    public CaravanWagon save(CaravanWagon wagon) {
      wagons.removeIf(existing -> existing.id().equals(wagon.id()));
      wagons.add(wagon);
      return wagon;
    }

    @Override
    public List<CaravanWagon> findAllByCaravanId(UUID caravanId) {
      return wagons.stream().filter(wagon -> wagon.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanWagon> findById(UUID caravanId, UUID wagonId) {
      return wagons.stream()
          .filter(wagon -> wagon.caravanId().equals(caravanId) && wagon.id().equals(wagonId))
          .findFirst();
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId) {
      wagons.removeIf(wagon -> wagon.caravanId().equals(caravanId) && wagon.id().equals(wagonId));
    }

    @Override
    public long countByCaravanId(UUID caravanId) {
      return wagons.stream().filter(wagon -> wagon.caravanId().equals(caravanId)).count();
    }

    @Override
    public long countByCaravanIdAndWagonTypeCode(UUID caravanId, String wagonTypeCode) {
      return wagons.stream()
          .filter(wagon -> wagon.caravanId().equals(caravanId) && wagon.wagonTypeCode().equals(wagonTypeCode))
          .count();
    }
  }

  private static final class InMemoryTravelerRepository implements CaravanTravelerRepositoryPort {
    private final List<CaravanTraveler> travelers = new ArrayList<>();

    @Override
    public CaravanTraveler save(CaravanTraveler traveler) {
      travelers.removeIf(existing -> existing.id().equals(traveler.id()));
      travelers.add(traveler);
      return traveler;
    }

    @Override
    public List<CaravanTraveler> findAllByCaravanId(UUID caravanId) {
      return travelers.stream().filter(traveler -> traveler.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanTraveler> findById(UUID caravanId, UUID travelerId) {
      return travelers.stream()
          .filter(traveler -> traveler.caravanId().equals(caravanId) && traveler.id().equals(travelerId))
          .findFirst();
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return travelers.stream()
          .filter(traveler -> traveler.caravanId().equals(caravanId) && wagonId.equals(traveler.wagonId()))
          .count();
    }

    @Override
    public void deleteByCaravanIdAndId(UUID caravanId, UUID travelerId) {
      travelers.removeIf(traveler -> traveler.caravanId().equals(caravanId) && traveler.id().equals(travelerId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      travelers.removeIf(traveler -> traveler.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryCargoRepository implements CaravanCargoRepositoryPort {
    private final List<CaravanCargo> cargos = new ArrayList<>();

    @Override
    public CaravanCargo save(CaravanCargo cargo) {
      cargos.removeIf(existing -> existing.id().equals(cargo.id()));
      cargos.add(cargo);
      return cargo;
    }

    @Override
    public List<CaravanCargo> findAllByCaravanId(UUID caravanId) {
      return cargos.stream().filter(cargo -> cargo.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanCargo> findById(UUID caravanId, UUID cargoId) {
      return cargos.stream()
          .filter(cargo -> cargo.caravanId().equals(caravanId) && cargo.id().equals(cargoId))
          .findFirst();
    }

    @Override
    public void deleteById(UUID caravanId, UUID cargoId) {
      cargos.removeIf(cargo -> cargo.caravanId().equals(caravanId) && cargo.id().equals(cargoId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      cargos.removeIf(cargo -> cargo.caravanId().equals(caravanId));
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return cargos.stream()
          .filter(cargo -> cargo.caravanId().equals(caravanId) && wagonId.equals(cargo.wagonId()))
          .count();
    }
  }

  private static final class InMemoryBeastRepository implements CaravanBeastRepositoryPort {
    private final List<CaravanBeast> beasts = new ArrayList<>();

    @Override
    public CaravanBeast save(CaravanBeast beast) {
      beasts.removeIf(existing -> existing.id().equals(beast.id()));
      beasts.add(beast);
      return beast;
    }

    @Override
    public List<CaravanBeast> findAllByCaravanId(UUID caravanId) {
      return beasts.stream().filter(beast -> beast.caravanId().equals(caravanId)).toList();
    }

    @Override
    public List<CaravanBeast> findAllByCaravanIdAndAssignmentType(UUID caravanId, CaravanBeastAssignmentType assignmentType) {
      return beasts.stream()
          .filter(beast -> beast.caravanId().equals(caravanId) && beast.assignmentType() == assignmentType)
          .toList();
    }

    @Override
    public List<CaravanBeast> findAllByCaravanIdAndWagonIdAndAssignmentType(
        UUID caravanId, UUID wagonId, CaravanBeastAssignmentType assignmentType) {
      return beasts.stream()
          .filter(beast -> beast.caravanId().equals(caravanId)
              && beast.assignmentType() == assignmentType
              && wagonId.equals(beast.assignedWagonId()))
          .toList();
    }

    @Override
    public Optional<CaravanBeast> findById(UUID caravanId, UUID beastId) {
      return beasts.stream()
          .filter(beast -> beast.caravanId().equals(caravanId) && beast.id().equals(beastId))
          .findFirst();
    }

    @Override
    public void deleteByCaravanIdAndId(UUID caravanId, UUID beastId) {
      beasts.removeIf(beast -> beast.caravanId().equals(caravanId) && beast.id().equals(beastId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      beasts.removeIf(beast -> beast.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryWagonImprovementRepository implements CaravanWagonImprovementRepositoryPort {
    private final List<CaravanWagonImprovement> improvements = new ArrayList<>();

    @Override
    public CaravanWagonImprovement save(CaravanWagonImprovement improvement) {
      improvements.removeIf(existing -> existing.id().equals(improvement.id()));
      improvements.add(improvement);
      return improvement;
    }

    @Override
    public List<CaravanWagonImprovement> findAllByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return improvements.stream()
          .filter(improvement -> improvement.caravanId().equals(caravanId) && improvement.wagonId().equals(wagonId))
          .toList();
    }

    @Override
    public Optional<CaravanWagonImprovement> findById(UUID caravanId, UUID wagonId, UUID improvementId) {
      return improvements.stream()
          .filter(improvement ->
              improvement.caravanId().equals(caravanId)
                  && improvement.wagonId().equals(wagonId)
                  && improvement.id().equals(improvementId))
          .findFirst();
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId, UUID improvementId) {
      improvements.removeIf(improvement ->
          improvement.caravanId().equals(caravanId)
              && improvement.wagonId().equals(wagonId)
              && improvement.id().equals(improvementId));
    }
  }

  private static final class InMemoryFeatRepository implements CaravanFeatRepositoryPort {
    private final List<CaravanFeat> feats = new ArrayList<>();

    @Override
    public CaravanFeat save(CaravanFeat feat) {
      feats.removeIf(existing -> existing.id().equals(feat.id()));
      feats.add(feat);
      return feat;
    }

    @Override
    public List<CaravanFeat> findAllByCaravanId(UUID caravanId) {
      return feats.stream().filter(feat -> feat.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanFeat> findById(UUID caravanId, UUID featId) {
      return feats.stream()
          .filter(feat -> feat.caravanId().equals(caravanId) && feat.id().equals(featId))
          .findFirst();
    }

    @Override
    public long countByCaravanIdAndFeatTypeCode(UUID caravanId, String featTypeCode) {
      return feats.stream()
          .filter(feat -> feat.caravanId().equals(caravanId) && feat.featTypeCode().equals(featTypeCode))
          .count();
    }

    @Override
    public void deleteById(UUID caravanId, UUID featId) {
      feats.removeIf(feat -> feat.caravanId().equals(caravanId) && feat.id().equals(featId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      feats.removeIf(feat -> feat.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryFeatCatalogPort implements CaravanFeatCatalogPort {
    @Override
    public List<com.gestioncaravana.domain.CaravanFeatType> all() {
      return List.of(
          new com.gestioncaravana.domain.CaravanFeatType(
              "caravana-mejorada",
              "Caravana Mejorada",
              "La caravana mejora su rendimiento general.",
              List.of("Nivel 2"),
              "Aumenta en 1 dos de las estadísticas principales hasta una puntuación máxima de +10.",
              "Esta dote puede seleccionarse varias veces.",
              null,
              true,
              999,
              2,
              null,
              null,
              null));
    }

    @Override
    public Optional<com.gestioncaravana.domain.CaravanFeatType> findByCode(String code) {
      return all().stream().filter(feat -> feat.code().equals(code)).findFirst();
    }
  }

  private static final class InMemorySupplyStateRepository implements CaravanSupplyStateRepositoryPort {
    private final List<CaravanSupplyState> states = new ArrayList<>();

    @Override
    public CaravanSupplyState save(CaravanSupplyState state) {
      states.removeIf(existing -> existing.caravanId().equals(state.caravanId()));
      states.add(state);
      return state;
    }

    @Override
    public Optional<CaravanSupplyState> findByCaravanId(UUID caravanId) {
      return states.stream().filter(state -> state.caravanId().equals(caravanId)).findFirst();
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      states.removeIf(state -> state.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryDayResolutionRepository implements CaravanDayResolutionRepositoryPort {
    private final List<CaravanDayResolution> resolutions = new ArrayList<>();

    @Override
    public CaravanDayResolution save(CaravanDayResolution resolution) {
      resolutions.removeIf(existing -> existing.id().equals(resolution.id()));
      resolutions.add(resolution);
      return resolution;
    }

    @Override
    public Optional<CaravanDayResolution> findByCaravanIdAndIdempotencyKey(UUID caravanId, String idempotencyKey) {
      return resolutions.stream()
          .filter(resolution -> resolution.caravanId().equals(caravanId) && resolution.idempotencyKey().equals(idempotencyKey))
          .findFirst();
    }

    @Override
    public List<CaravanDayResolution> findAllByCaravanId(UUID caravanId) {
      return resolutions.stream().filter(resolution -> resolution.caravanId().equals(caravanId)).toList();
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      resolutions.removeIf(resolution -> resolution.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryWeatherProfileRepository implements CaravanWeatherProfileRepositoryPort {
    private final java.util.Map<UUID, CaravanWeatherProfile> profiles = new java.util.HashMap<>();

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

  private static final class InMemoryWeatherForecastStateRepository
      implements CaravanWeatherForecastStateRepositoryPort {
    private final java.util.Map<String, CaravanWeatherForecastState> states = new java.util.HashMap<>();

    @Override
    public CaravanWeatherForecastState save(CaravanWeatherForecastState state) {
      states.put(key(state.caravanId(), state.date()), state);
      return state;
    }

    @Override
    public Optional<CaravanWeatherForecastState> findByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
      return Optional.ofNullable(states.get(key(caravanId, date)));
    }

    @Override
    public void deleteByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
      states.remove(key(caravanId, date));
    }

    @Override
    public void deleteFromDate(UUID caravanId, GolarionDate fromDate) {
      states.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":")
          && toDate(entry.getKey()).compareTo(fromDate) >= 0);
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      states.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":"));
    }

    private String key(UUID caravanId, GolarionDate date) {
      return caravanId + ":" + date.year() + ":" + date.month() + ":" + date.day();
    }

    private GolarionDate toDate(String key) {
      var parts = key.split(":");
      return new GolarionDate(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
  }

  private static final class InMemoryWeatherSnapshotRepository implements CaravanWeatherSnapshotRepositoryPort {
    private final java.util.Map<String, CaravanWeatherSnapshot> snapshots = new java.util.HashMap<>();

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

  private static final class InMemoryActiveSelection implements ActiveCaravanSelectionPort {
    private UUID activeId;

    @Override
    public Optional<UUID> getActiveCaravanId() {
      return Optional.ofNullable(activeId);
    }

    @Override
    public void setActiveCaravanId(UUID caravanId) {
      this.activeId = caravanId;
    }

    @Override
    public void clear() {
      this.activeId = null;
    }
  }
}
