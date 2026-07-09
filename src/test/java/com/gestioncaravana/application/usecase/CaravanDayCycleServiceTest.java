package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.application.model.CaravanCargoSummaryView;
import com.gestioncaravana.application.model.CaravanDerivedStatsView;
import com.gestioncaravana.application.model.CaravanMainStatsView;
import com.gestioncaravana.application.model.CaravanOtherStatsView;
import com.gestioncaravana.application.model.CaravanStatisticsView;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.in.ListCaravanCargoSummaryUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanDayCycleResultRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanDayCycleResult;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
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
        .contains("Sirviente: Torvald Hagorsson")
        .contains("El sirviente también puede ser agricultor: +0.5")
        .doesNotContain("Sin sirviente asignado")
        .doesNotContain("El sirviente no puede ser agricultor: +0.25");
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
