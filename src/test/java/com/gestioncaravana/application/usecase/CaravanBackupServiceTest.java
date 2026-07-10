package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.application.model.CaravanBackupView;
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
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanDayResolution;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanMainStats;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.TravelerContract;
import com.gestioncaravana.domain.TravelerRoleCatalog;
import com.gestioncaravana.domain.TravelerRoleData;
import com.gestioncaravana.domain.GolarionDate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaravanBackupServiceTest {

  private InMemoryCaravanRepository campaignRepository;
  private InMemoryCaravanWagonRepository wagonRepository;
  private InMemoryWagonImprovementRepository wagonImprovementRepository;
  private InMemoryTravelerRepository travelerRepository;
  private InMemoryCargoRepository cargoRepository;
  private InMemoryBeastRepository beastRepository;
  private InMemoryFeatRepository featRepository;
  private InMemorySupplyStateRepository supplyStateRepository;
  private InMemoryDayResolutionRepository dayResolutionRepository;
  private InMemoryActiveSelection activeSelection;
  private InMemoryWeatherProfileRepository weatherProfileRepository;
  private InMemoryWeatherSnapshotRepository weatherSnapshotRepository;
  private CaravanManagementService managementService;
  private CaravanBackupService backupService;

  @BeforeEach
  void setUp() {
    campaignRepository = new InMemoryCaravanRepository();
    wagonRepository = new InMemoryCaravanWagonRepository();
    wagonImprovementRepository = new InMemoryWagonImprovementRepository();
    travelerRepository = new InMemoryTravelerRepository();
    cargoRepository = new InMemoryCargoRepository();
    beastRepository = new InMemoryBeastRepository();
    featRepository = new InMemoryFeatRepository();
    supplyStateRepository = new InMemorySupplyStateRepository();
    dayResolutionRepository = new InMemoryDayResolutionRepository();
    activeSelection = new InMemoryActiveSelection();
    weatherProfileRepository = new InMemoryWeatherProfileRepository();
    weatherSnapshotRepository = new InMemoryWeatherSnapshotRepository();

    var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    var weatherService = new CaravanWeatherService(
        campaignRepository,
        weatherProfileRepository,
        weatherSnapshotRepository,
        clock);
    managementService = new CaravanManagementService(
        campaignRepository,
        wagonRepository,
        wagonImprovementRepository,
        travelerRepository,
        cargoRepository,
        beastRepository,
        featRepository,
        supplyStateRepository,
        dayResolutionRepository,
        weatherService,
        new NoopCaravanFeatCatalogPort(),
        activeSelection,
        clock);
    backupService = new CaravanBackupService(
        campaignRepository,
        wagonRepository,
        wagonImprovementRepository,
        travelerRepository,
        cargoRepository,
        beastRepository,
        featRepository,
        supplyStateRepository,
        dayResolutionRepository,
        activeSelection,
        managementService);
  }

  @Test
  void exportsAndRestoresACompleteBackup() {
    var created = managementService.execute(new CreateCaravanCommand("North Caravan", "Primary route", 2, 1, 1, 1));
    managementService.select(created.id());

    var wagonId = UUID.randomUUID();
    wagonRepository.save(CaravanWagon.create(
        wagonId,
        created.id(),
        "wagon",
        "Lead Wagon",
        "Spices",
        Instant.parse("2026-01-01T00:00:00Z")));
    wagonImprovementRepository.save(CaravanWagonImprovement.create(
        UUID.randomUUID(),
        created.id(),
        wagonId,
        "reinforced-axle",
        Instant.parse("2026-01-01T00:00:00Z")));
    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        created.id(),
        "Mara",
        "Scout",
        List.of(TravelerRoleCatalog.PASSENGER_CODE),
        List.of(TravelerRoleCatalog.PASSENGER_CODE),
        TravelerRoleCatalog.PASSENGER_CODE,
        1,
        TravelerRoleData.empty(),
        wagonId,
        null,
        TravelerContract.create(BigDecimal.valueOf(12.5), "Quarterly", Instant.parse("2026-01-01T00:00:00Z")),
        1,
        BigDecimal.ONE,
        Instant.parse("2026-01-01T00:00:00Z")));
    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        created.id(),
        CaravanCargoSourceType.CUSTOM,
        null,
        "Medical supplies",
        "Supplies",
        1,
        1,
        wagonId,
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));
    beastRepository.save(new CaravanBeast(
        UUID.randomUUID(),
        created.id(),
        CaravanBeastSourceType.CUSTOM,
        null,
        "Mule",
        "M",
        1,
        2,
        null,
        null,
        null,
        false,
        "Reliable",
        "Strong enough for the route",
        null,
        1,
        CaravanBeastAssignmentType.NONE,
        null,
        Instant.parse("2026-01-01T00:00:00Z"),
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
    dayResolutionRepository.save(new CaravanDayResolution(
        UUID.randomUUID(),
        created.id(),
        "backup-day-1",
        1,
        Instant.parse("2026-01-01T00:00:00Z"),
        10,
        8,
        4,
        2,
        -2,
        0,
        "+ 0 cargas de suministros",
        "choices",
        "contributions",
        "warnings"));

    var backup = backupService.export(created.id());

    campaignRepository.save(new CaravanCampaign(
        created.id(),
        "Mutated Caravan",
        "Mutated",
        created.level(),
        CaravanMainStats.withUpdatedAllocation(3, 1, 1, 1, 6),
        created.discontent(),
        created.status(),
        created.createdAt(),
        Instant.parse("2026-01-02T00:00:00Z")));
    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        created.id(),
        CaravanCargoSourceType.CUSTOM,
        null,
        "Temporary cargo",
        "Supplies",
        1,
        1,
        wagonId,
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z")));

    var restored = backupService.execute(backup);

    assertThat(backup.schemaVersion()).isEqualTo(CaravanBackupView.CURRENT_SCHEMA_VERSION);
    assertThat(backup.active()).isTrue();
    assertThat(backup.wagons()).hasSize(1);
    assertThat(restored.name()).isEqualTo("North Caravan");
    assertThat(campaignRepository.findById(created.id())).map(CaravanCampaign::name).hasValue("North Caravan");
    assertThat(cargoRepository.findAllByCaravanId(created.id())).hasSize(1);
    assertThat(travelerRepository.findAllByCaravanId(created.id())).hasSize(1);
    assertThat(wagonImprovementRepository.findAllByCaravanIdAndWagonId(created.id(), wagonId)).hasSize(1);
    assertThat(dayResolutionRepository.findAllByCaravanId(created.id())).hasSize(1);
    assertThat(activeSelection.getActiveCaravanId()).contains(created.id());
  }

  @Test
  void preservesSharedJobProductivityStateDuringBackupRoundTrip() {
    var created = managementService.execute(new CreateCaravanCommand("Teamwork Caravan", "Shared job state", 2, 1, 1, 1));
    supplyStateRepository.save(new CaravanSupplyState(
        created.id(),
        12,
        0,
        0,
        4,
        Instant.parse("2026-01-01T00:00:00Z"),
        "batidor=0.5=traveler-a,traveler-b|cocinero=0.25=traveler-c,traveler-d"));

    var backup = backupService.export(created.id());
    assertThat(backup.supplyState().sharedJobProductivityState())
        .isEqualTo("batidor=0.5=traveler-a,traveler-b|cocinero=0.25=traveler-c,traveler-d");

    backupService.execute(backup);

    assertThat(supplyStateRepository.findByCaravanId(created.id()))
        .hasValueSatisfying(state -> assertThat(state.sharedJobProductivityState())
            .isEqualTo("batidor=0.5=traveler-a,traveler-b|cocinero=0.25=traveler-c,traveler-d"));
  }

  @Test
  void importsVersionTwoBackupIntoAnEmptyDatabase() {
    var caravanId = UUID.randomUUID();
    var backup = new CaravanBackupView(
        2,
        true,
        new CaravanBackupView.CaravanSnapshot(
            caravanId,
            "Restored Caravan",
            "Recovered from backup",
            4,
            CaravanMainStats.withUpdatedAllocation(2, 2, 2, 2, 10),
            0,
            com.gestioncaravana.domain.CaravanCampaignStatus.ACTIVE,
            Instant.parse("2026-07-08T10:00:00Z"),
            Instant.parse("2026-07-08T10:00:00Z")),
        new CaravanBackupView.SupplyStateSnapshot(
            caravanId,
            0,
            0,
            0,
            0,
            Instant.parse("2026-07-08T10:00:00Z"),
            null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());

    var restored = backupService.execute(backup);

    assertThat(restored.id()).isEqualTo(caravanId);
    assertThat(campaignRepository.findById(caravanId)).hasValueSatisfying(campaign -> {
      assertThat(campaign.name()).isEqualTo("Restored Caravan");
      assertThat(campaign.level()).isEqualTo(4);
    });
    assertThat(supplyStateRepository.findByCaravanId(caravanId)).hasValueSatisfying(state ->
        assertThat(state.daysPassed()).isEqualTo(0));
    assertThat(activeSelection.getActiveCaravanId()).contains(caravanId);
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
    public void deleteByCaravanId(UUID caravanId) {
      snapshots.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":"));
    }

    private String key(UUID caravanId, GolarionDate date) {
      return caravanId + ":" + date.year() + ":" + date.month() + ":" + date.day();
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

  private static final class NoopCaravanFeatCatalogPort implements CaravanFeatCatalogPort {
    @Override
    public List<com.gestioncaravana.domain.CaravanFeatType> all() {
      return List.of();
    }

    @Override
    public Optional<com.gestioncaravana.domain.CaravanFeatType> findByCode(String code) {
      return Optional.empty();
    }
  }
}
