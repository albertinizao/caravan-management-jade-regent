package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.application.model.CaravanCargoSummaryView;
import com.gestioncaravana.application.model.CaravanDerivedStatsView;
import com.gestioncaravana.application.model.CaravanMainStatsView;
import com.gestioncaravana.application.model.CaravanOtherStatsView;
import com.gestioncaravana.application.model.CaravanStatisticsView;
import com.gestioncaravana.application.port.in.ConfirmCaravanDayCycleUseCase.ConfirmCaravanDayCycleCommand;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.in.ListCaravanCargoSummaryUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanDayCycleResultRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanDayCycleResult;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CargoCatalog;
import com.gestioncaravana.domain.TravelerRoleData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaravanDayCycleServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryTravelerRepository travelerRepository;
  private InMemoryWagonRepository wagonRepository;
  private InMemoryCargoRepository cargoRepository;
  private InMemoryFeatRepository featRepository;
  private InMemorySupplyStateRepository supplyStateRepository;
  private InMemoryDayCycleResultRepository dayCycleResultRepository;
  private ListCaravanCargoSummaryUseCase cargoSummaryUseCase;
  private GetCaravanStatisticsUseCase statisticsUseCase;
  private CaravanDayCycleService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    travelerRepository = new InMemoryTravelerRepository();
    wagonRepository = new InMemoryWagonRepository();
    cargoRepository = new InMemoryCargoRepository();
    featRepository = new InMemoryFeatRepository();
    supplyStateRepository = new InMemorySupplyStateRepository();
    dayCycleResultRepository = new InMemoryDayCycleResultRepository();
    cargoSummaryUseCase = caravanId -> List.of();
    statisticsUseCase = caravanId -> new CaravanStatisticsView(
        caravanId,
        1,
        new CaravanMainStatsView(0, 0, 0, 0, 0),
        new CaravanDerivedStatsView(0, 0, 0, 0),
        new CaravanOtherStatsView(0, 10, 10, 0, 10, 0, 0, 0, 0, 1),
        0,
        0,
        List.of(),
        List.of(),
        NOW);
    service = new CaravanDayCycleService(
        caravanRepository,
        travelerRepository,
        wagonRepository,
        cargoRepository,
        featRepository,
        supplyStateRepository,
        cargoSummaryUseCase,
        statisticsUseCase,
        dayCycleResultRepository,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void previewUsesServantAvailableRoleAndOmitsRedundantNoServantMessage() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Caravana", null, NOW));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-cubierto", "Carro", NOW));
    supplyStateRepository.save(CaravanSupplyState.initial(caravan.id(), NOW));

    var farmer = travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Ayla",
        null,
        List.of("pasajero", "agricultor"),
        List.of("agricultor"),
        "agricultor",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        1,
        NOW));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Torvald Hagorsson",
        null,
        List.of("pasajero", "agricultor", "sirviente", "guarda"),
        List.of("sirviente"),
        "sirviente",
        1,
        new TravelerRoleData(farmer.id()),
        wagon.id(),
        null,
        0,
        NOW));

    var preview = service.preview(caravan.id());

    assertThat(preview.confirmed()).isFalse();
    var agriculturistEntry = preview.simulation().stream()
        .filter(entry -> entry.title().equals("Ayla"))
        .findFirst()
        .orElseThrow();
    assertThat(agriculturistEntry.details())
        .contains("Sirviente: Torvald Hagorsson (agricultor): +0.5")
        .doesNotContain("Sin sirviente asignado")
        .doesNotContain("El sirviente no puede ser agricultor: +0.25");
  }

  @Test
  void teamworkDoesNotApplyWithoutFeat() {
    var caravan = createCaravan();
    var wagon = createWagon(caravan.id());
    configureCargoSummary(wagon.id(), 20);
    supplyStateRepository.save(CaravanSupplyState.initial(caravan.id(), NOW));

    createTraveler(caravan.id(), wagon.id(), "Ayla", List.of("pasajero", "agricultor"), List.of("agricultor"), "agricultor", TravelerRoleData.empty());
    createTraveler(caravan.id(), wagon.id(), "Bran", List.of("pasajero", "agricultor"), List.of("agricultor"), "agricultor", TravelerRoleData.empty());

    var preview = service.preview(caravan.id());

    assertThat(preview.simulation().stream().flatMap(entry -> entry.details().stream()))
        .noneMatch(detail -> detail.contains("Trabajo en equipo"));
  }

  @Test
  void teamworkSelectsOnlyThreeAgricultorsByPriority() {
    var caravan = createCaravan();
    var wagon = createWagon(caravan.id());
    configureCargoSummary(wagon.id(), 20);
    supplyStateRepository.save(CaravanSupplyState.initial(caravan.id(), NOW));
    activateTeamworkFeat(caravan.id());

    var alpha = createTraveler(caravan.id(), wagon.id(), "Alpha", List.of("pasajero", "agricultor"), List.of("agricultor"), "agricultor", TravelerRoleData.empty());
    var bravo = createTraveler(caravan.id(), wagon.id(), "Bravo", List.of("pasajero", "agricultor"), List.of("agricultor"), "agricultor", TravelerRoleData.empty());
    var charlie = createTraveler(caravan.id(), wagon.id(), "Charlie", List.of("pasajero", "agricultor"), List.of("agricultor"), "agricultor", TravelerRoleData.empty());
    createTraveler(caravan.id(), wagon.id(), "Delta", List.of("pasajero", "agricultor"), List.of("agricultor"), "agricultor", TravelerRoleData.empty());

    createServant(caravan.id(), wagon.id(), "Servant Alpha", alpha.id(), List.of("pasajero", "sirviente", "agricultor"));
    createServant(caravan.id(), wagon.id(), "Servant Bravo", bravo.id(), List.of("pasajero", "sirviente", "guarda"));

    var preview = service.preview(caravan.id());

    assertThat(findSimulationEntry(preview, "Alpha").details()).anyMatch(detail -> detail.contains("Trabajo en equipo aplicado."));
    assertThat(findSimulationEntry(preview, "Bravo").details()).anyMatch(detail -> detail.contains("Trabajo en equipo aplicado."));
    assertThat(findSimulationEntry(preview, "Charlie").details()).anyMatch(detail -> detail.contains("Trabajo en equipo aplicado."));
    assertThat(findSimulationEntry(preview, "Delta").details()).noneMatch(detail -> detail.contains("Trabajo en equipo"));
  }

  @Test
  void teamworkBoostsBatidoresAndSummarizesRole() {
    var caravan = createCaravan();
    var wagon = createWagon(caravan.id());
    configureCargoSummary(wagon.id(), 20);
    supplyStateRepository.save(CaravanSupplyState.initial(caravan.id(), NOW));
    activateTeamworkFeat(caravan.id());

    var hunterA = createTraveler(caravan.id(), wagon.id(), "Hunter A", List.of("pasajero", "batidor"), List.of("batidor"), "batidor", TravelerRoleData.empty());
    createTraveler(caravan.id(), wagon.id(), "Hunter B", List.of("pasajero", "batidor"), List.of("batidor"), "batidor", TravelerRoleData.empty());
    createServant(caravan.id(), wagon.id(), "Servant Hunter A", hunterA.id(), List.of("pasajero", "sirviente", "guarda"));

    var preview = service.preview(caravan.id());

    assertThat(preview.generatedFood()).isEqualByComparingTo("6.25");
    assertThat(findSimulationEntry(preview, "Hunter A").details()).contains("Trabajo en equipo aplicado.");
    assertThat(findSimulationEntry(preview, "Resumen de batidores").details())
        .contains("Trabajo en equipo en batidores: 2 beneficiados, x1.25");
  }

  @Test
  void teamworkAppliesOnlyToFirstThreeCooksAndPreviewMatchesConfirm() {
    var caravan = createCaravan();
    var wagon = createWagon(caravan.id());
    configureCargoSummary(wagon.id(), 20);
    supplyStateRepository.save(CaravanSupplyState.initial(caravan.id(), NOW));
    activateTeamworkFeat(caravan.id());
    setConsumption(80);

    createSupply(caravan.id(), wagon.id(), "suministros");
    createSupply(caravan.id(), wagon.id(), "suministros");
    createSupply(caravan.id(), wagon.id(), "suministros");
    createSupply(caravan.id(), wagon.id(), "suministros");
    createCargo(caravan.id(), wagon.id(), "cocina-portatil");
    createCargo(caravan.id(), wagon.id(), "cocina-portatil");
    createCargo(caravan.id(), wagon.id(), "cocina-portatil");
    createCargo(caravan.id(), wagon.id(), "cocina-portatil");

    createTraveler(caravan.id(), wagon.id(), "Cook A", List.of("pasajero", "cocinero"), List.of("cocinero"), "cocinero", TravelerRoleData.empty());
    createTraveler(caravan.id(), wagon.id(), "Cook B", List.of("pasajero", "cocinero"), List.of("cocinero"), "cocinero", TravelerRoleData.empty());
    createTraveler(caravan.id(), wagon.id(), "Cook C", List.of("pasajero", "cocinero"), List.of("cocinero"), "cocinero", TravelerRoleData.empty());
    createTraveler(caravan.id(), wagon.id(), "Cook D", List.of("pasajero", "cocinero"), List.of("cocinero"), "cocinero", TravelerRoleData.empty());

    var preview = service.preview(caravan.id());
    var confirmed = service.confirm(caravan.id(), new ConfirmCaravanDayCycleCommand(preview.previewFingerprint()));

    assertThat(findSimulationEntry(preview, "Cook A").foodDelta()).isEqualByComparingTo("30.0");
    assertThat(findSimulationEntry(preview, "Cook A").details()).contains("Trabajo en equipo aplicado.");
    assertThat(findSimulationEntry(preview, "Cook B").details()).contains("Trabajo en equipo aplicado.");
    assertThat(findSimulationEntry(preview, "Cook C").details()).contains("Trabajo en equipo aplicado.");
    assertThat(preview.simulation().stream().filter(entry -> "cook".equals(entry.section())).map(entry -> entry.title()).toList())
        .containsExactly("Cook A", "Cook B", "Cook C");
    assertThat(findSimulationEntry(preview, "Trabajo en equipo en cocineros").details())
        .contains("Beneficiarios: 3", "Multiplicador aplicado: x1.5");
    assertThat(preview.generatedFood()).isEqualByComparingTo("90.0");
    assertThat(confirmed.generatedFood()).isEqualByComparingTo(preview.generatedFood());
    assertThat(confirmed.leftoverFood()).isEqualByComparingTo(preview.leftoverFood());
  }

  private CaravanCampaign createCaravan() {
    return caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Caravana", null, NOW));
  }

  private CaravanWagon createWagon(UUID caravanId) {
    return wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravanId, "carro-cubierto", "Carro", NOW));
  }

  private void configureCargoSummary(UUID wagonId, int remainingCargoUnits) {
    cargoSummaryUseCase = caravanId -> List.of(new CaravanCargoSummaryView(wagonId, "Carro", 20, 0, remainingCargoUnits, 0));
    rebuildService();
  }

  private void setConsumption(int consumption) {
    statisticsUseCase = caravanId -> new CaravanStatisticsView(
        caravanId,
        1,
        new CaravanMainStatsView(0, 0, 0, 0, 0),
        new CaravanDerivedStatsView(0, 0, 0, 0),
        new CaravanOtherStatsView(0, 10, 10, 0, 10, consumption, 0, 0, 0, 1),
        0,
        0,
        List.of(),
        List.of(),
        NOW);
    rebuildService();
  }

  private void activateTeamworkFeat(UUID caravanId) {
    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        caravanId,
        "trabajo-en-equipo",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "test",
        1,
        NOW));
  }

  private CaravanTraveler createTraveler(
      UUID caravanId,
      UUID wagonId,
      String name,
      List<String> availableRoles,
      List<String> activeRoles,
      String activeRole,
      TravelerRoleData roleData) {
    return travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravanId,
        name,
        null,
        availableRoles,
        activeRoles,
        activeRole,
        1,
        roleData,
        wagonId,
        null,
        1,
        NOW));
  }

  private CaravanTraveler createServant(UUID caravanId, UUID wagonId, String name, UUID servedTravelerId, List<String> availableRoles) {
    return travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravanId,
        name,
        null,
        availableRoles,
        List.of("sirviente"),
        "sirviente",
        1,
        new TravelerRoleData(servedTravelerId),
        wagonId,
        null,
        0,
        NOW));
  }

  private void createSupply(UUID caravanId, UUID wagonId, String catalogCode) {
    createCargo(caravanId, wagonId, catalogCode);
  }

  private void createCargo(UUID caravanId, UUID wagonId, String catalogCode) {
    var catalogItem = CargoCatalog.findByCode(catalogCode).orElseThrow();
    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravanId,
        com.gestioncaravana.domain.CaravanCargoSourceType.CATALOG,
        catalogCode,
        catalogItem.name(),
        catalogItem.category(),
        1,
        1,
        wagonId,
        null,
        null,
        null,
        null,
        NOW));
  }

  private void rebuildService() {
    service = new CaravanDayCycleService(
        caravanRepository,
        travelerRepository,
        wagonRepository,
        cargoRepository,
        featRepository,
        supplyStateRepository,
        cargoSummaryUseCase,
        statisticsUseCase,
        dayCycleResultRepository,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private com.gestioncaravana.application.model.CaravanDayCycleLogEntryView findSimulationEntry(
      com.gestioncaravana.application.model.CaravanDayCyclePreviewView preview,
      String title) {
    return preview.simulation().stream().filter(entry -> entry.title().equals(title)).findFirst().orElseThrow();
  }

  private static final class InMemoryCaravanRepository implements CaravanCampaignRepositoryPort {
    private final Map<UUID, CaravanCampaign> caravans = new HashMap<>();

    @Override
    public CaravanCampaign save(CaravanCampaign caravanCampaign) {
      caravans.put(caravanCampaign.id(), caravanCampaign);
      return caravanCampaign;
    }

    @Override
    public void deleteById(UUID id) {
      caravans.remove(id);
    }

    @Override
    public List<CaravanCampaign> findAll() {
      return new ArrayList<>(caravans.values());
    }

    @Override
    public Optional<CaravanCampaign> findById(UUID id) {
      return Optional.ofNullable(caravans.get(id));
    }
  }

  private static final class InMemoryTravelerRepository implements CaravanTravelerRepositoryPort {
    private final Map<UUID, Map<UUID, CaravanTraveler>> travelersByCaravan = new HashMap<>();

    @Override
    public CaravanTraveler save(CaravanTraveler traveler) {
      travelersByCaravan.computeIfAbsent(traveler.caravanId(), ignored -> new HashMap<>())
          .put(traveler.id(), traveler);
      return traveler;
    }

    @Override
    public List<CaravanTraveler> findAllByCaravanId(UUID caravanId) {
      return new ArrayList<>(travelersByCaravan.getOrDefault(caravanId, Map.of()).values());
    }

    @Override
    public Optional<CaravanTraveler> findById(UUID caravanId, UUID travelerId) {
      return Optional.ofNullable(travelersByCaravan.getOrDefault(caravanId, Map.of()).get(travelerId));
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return findAllByCaravanId(caravanId).stream().filter(traveler -> wagonId.equals(traveler.wagonId())).count();
    }

    @Override
    public void deleteByCaravanIdAndId(UUID caravanId, UUID travelerId) {
      var travelers = travelersByCaravan.get(caravanId);
      if (travelers != null) {
        travelers.remove(travelerId);
      }
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      travelersByCaravan.remove(caravanId);
    }
  }

  private static final class InMemoryWagonRepository implements CaravanWagonRepositoryPort {
    private final Map<UUID, Map<UUID, CaravanWagon>> wagonsByCaravan = new HashMap<>();

    @Override
    public CaravanWagon save(CaravanWagon wagon) {
      wagonsByCaravan.computeIfAbsent(wagon.caravanId(), ignored -> new HashMap<>())
          .put(wagon.id(), wagon);
      return wagon;
    }

    @Override
    public List<CaravanWagon> findAllByCaravanId(UUID caravanId) {
      return new ArrayList<>(wagonsByCaravan.getOrDefault(caravanId, Map.of()).values());
    }

    @Override
    public Optional<CaravanWagon> findById(UUID caravanId, UUID wagonId) {
      return Optional.ofNullable(wagonsByCaravan.getOrDefault(caravanId, Map.of()).get(wagonId));
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId) {
      var wagons = wagonsByCaravan.get(caravanId);
      if (wagons != null) {
        wagons.remove(wagonId);
      }
    }

    @Override
    public long countByCaravanId(UUID caravanId) {
      return wagonsByCaravan.getOrDefault(caravanId, Map.of()).size();
    }

    @Override
    public long countByCaravanIdAndWagonTypeCode(UUID caravanId, String wagonTypeCode) {
      return findAllByCaravanId(caravanId).stream().filter(wagon -> wagon.wagonTypeCode().equals(wagonTypeCode)).count();
    }
  }

  private static final class InMemoryCargoRepository implements CaravanCargoRepositoryPort {
    private final Map<UUID, Map<UUID, CaravanCargo>> cargoByCaravan = new HashMap<>();

    @Override
    public CaravanCargo save(CaravanCargo cargo) {
      cargoByCaravan.computeIfAbsent(cargo.caravanId(), ignored -> new HashMap<>())
          .put(cargo.id(), cargo);
      return cargo;
    }

    @Override
    public List<CaravanCargo> findAllByCaravanId(UUID caravanId) {
      return new ArrayList<>(cargoByCaravan.getOrDefault(caravanId, Map.of()).values());
    }

    @Override
    public Optional<CaravanCargo> findById(UUID caravanId, UUID cargoId) {
      return Optional.ofNullable(cargoByCaravan.getOrDefault(caravanId, Map.of()).get(cargoId));
    }

    @Override
    public void deleteById(UUID caravanId, UUID cargoId) {
      var cargo = cargoByCaravan.get(caravanId);
      if (cargo != null) {
        cargo.remove(cargoId);
      }
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      cargoByCaravan.remove(caravanId);
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return findAllByCaravanId(caravanId).stream().filter(item -> wagonId.equals(item.wagonId())).count();
    }
  }

  private static final class InMemoryFeatRepository implements CaravanFeatRepositoryPort {
    private final Map<UUID, Map<UUID, CaravanFeat>> featsByCaravan = new HashMap<>();

    @Override
    public CaravanFeat save(CaravanFeat feat) {
      featsByCaravan.computeIfAbsent(feat.caravanId(), ignored -> new HashMap<>()).put(feat.id(), feat);
      return feat;
    }

    @Override
    public List<CaravanFeat> findAllByCaravanId(UUID caravanId) {
      return new ArrayList<>(featsByCaravan.getOrDefault(caravanId, Map.of()).values());
    }

    @Override
    public Optional<CaravanFeat> findById(UUID caravanId, UUID featId) {
      return Optional.ofNullable(featsByCaravan.getOrDefault(caravanId, Map.of()).get(featId));
    }

    @Override
    public long countByCaravanIdAndFeatTypeCode(UUID caravanId, String featTypeCode) {
      return findAllByCaravanId(caravanId).stream().filter(feat -> feat.featTypeCode().equals(featTypeCode)).count();
    }

    @Override
    public void deleteById(UUID caravanId, UUID featId) {
      var feats = featsByCaravan.get(caravanId);
      if (feats != null) {
        feats.remove(featId);
      }
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      featsByCaravan.remove(caravanId);
    }
  }

  private static final class InMemorySupplyStateRepository implements CaravanSupplyStateRepositoryPort {
    private final Map<UUID, CaravanSupplyState> states = new HashMap<>();

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

  private static final class InMemoryDayCycleResultRepository implements CaravanDayCycleResultRepositoryPort {
    private final List<CaravanDayCycleResult> results = new ArrayList<>();

    @Override
    public CaravanDayCycleResult save(CaravanDayCycleResult result) {
      results.add(result);
      return result;
    }

    @Override
    public Optional<CaravanDayCycleResult> findLatestByCaravanId(UUID caravanId) {
      return results.stream().filter(result -> result.caravanId().equals(caravanId)).reduce((left, right) -> right);
    }

    @Override
    public List<CaravanDayCycleResult> findAllByCaravanId(UUID caravanId) {
      return results.stream().filter(result -> result.caravanId().equals(caravanId)).toList();
    }
  }
}
