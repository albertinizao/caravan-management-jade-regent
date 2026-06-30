package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.in.AddCaravanTravelerUseCase.AddCaravanTravelerCommand;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerRoleUseCase.UpdateCaravanTravelerRoleCommand;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerWagonUseCase.UpdateCaravanTravelerWagonCommand;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
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

class TravelerManagementServiceTest {

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryTravelerRepository travelerRepository;
  private InMemoryWagonRepository wagonRepository;
  private InMemoryImprovementRepository improvementRepository;
  private InMemoryBeastRepository beastRepository;
  private TravelerManagementService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    travelerRepository = new InMemoryTravelerRepository();
    wagonRepository = new InMemoryWagonRepository();
    improvementRepository = new InMemoryImprovementRepository();
    beastRepository = new InMemoryBeastRepository();
    service = new TravelerManagementService(
        caravanRepository,
        travelerRepository,
        wagonRepository,
        improvementRepository,
        beastRepository,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void listsTravelerRoleCatalog() {
    assertThat(service.list()).extracting("code").contains("pasajero", "sirviente", "adivino");
  }

  @Test
  void createsListsAndFiltersTravelers() {
    var caravan = createCaravan();
    var wagon = createWagon(caravan.id(), "carro-cubierto");

    var created = service.execute(
        caravan.id(),
        new AddCaravanTravelerCommand(
            "María",
            "Description",
            List.of("pasajero"),
            List.of("pasajero"),
            null,
            1,
            BigDecimal.valueOf(120),
            "Contract",
            null,
            null));

    service.execute(caravan.id(), created.id(), new UpdateCaravanTravelerWagonCommand(wagon.id()));

    assertThat(created.fullName()).isEqualTo("María");
    assertThat(created.consumption()).isEqualTo(1);
    assertThat(service.list(caravan.id(), "mar", null, null)).hasSize(1);
    assertThat(service.list(caravan.id(), null, "pasajero", null)).hasSize(1);
    assertThat(service.list(caravan.id(), null, null, wagon.id())).hasSize(1);
    assertThat(service.getById(caravan.id(), created.id()).wagonName()).isEqualTo("Carro Cubierto");
  }

  @Test
  void changesTravelerRoleAndRequiresTargetForServant() {
    var caravan = createCaravan();
    var target = service.execute(
        caravan.id(),
        new AddCaravanTravelerCommand("Objetivo", null, List.of("pasajero"), List.of("pasajero"), null, 1, null, null, 1, null));
    var traveler = service.execute(
        caravan.id(),
        new AddCaravanTravelerCommand(
            "Sirviente",
            null,
            List.of("pasajero", "sirviente"),
            List.of("pasajero"),
            null,
            2,
            null,
            null,
            1,
            null));

    var updated = service.execute(
        caravan.id(),
        traveler.id(),
        new UpdateCaravanTravelerRoleCommand(List.of("pasajero", "sirviente"), "sirviente", 2, target.id()));

    assertThat(updated.activeRoleCode()).isEqualTo("sirviente");
    assertThat(updated.servedTravelerId()).isEqualTo(target.id());

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        traveler.id(),
        new UpdateCaravanTravelerRoleCommand(List.of("pasajero", "sirviente"), "sirviente", 2, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("servedTravelerId");
  }

  @Test
  void rejectsUnknownRolesOnCreation() {
    var caravan = createCaravan();

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        new AddCaravanTravelerCommand("Tester", null, List.of("unknown-role"), List.of("unknown-role"), null, 1, null, null, 1, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("known traveler role codes");
  }

  private CaravanCampaign createCaravan() {
    var caravan = CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z"));
    caravanRepository.save(caravan);
    return caravan;
  }

  private CaravanWagon createWagon(UUID caravanId, String wagonTypeCode) {
    var wagon = CaravanWagon.create(UUID.randomUUID(), caravanId, wagonTypeCode, Instant.parse("2026-01-01T00:00:00Z"));
    wagonRepository.save(wagon);
    return wagon;
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
    public void deleteByCaravanId(UUID caravanId) {
      travelers.removeIf(traveler -> traveler.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryWagonRepository implements CaravanWagonRepositoryPort {
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

  private static final class InMemoryImprovementRepository implements CaravanWagonImprovementRepositoryPort {
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
          .filter(improvement -> improvement.caravanId().equals(caravanId)
              && improvement.wagonId().equals(wagonId)
              && improvement.id().equals(improvementId))
          .findFirst();
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId, UUID improvementId) {
      improvements.removeIf(improvement -> improvement.caravanId().equals(caravanId)
          && improvement.wagonId().equals(wagonId)
          && improvement.id().equals(improvementId));
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
    public void deleteByCaravanId(UUID caravanId) {
      beasts.removeIf(beast -> beast.caravanId().equals(caravanId));
    }
  }
}
