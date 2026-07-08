package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.in.AddCaravanBeastUseCase.AddCaravanBeastCommand;
import com.gestioncaravana.application.port.in.DeleteUnassignedCaravanBeastsUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanBeastAssignmentUseCase.UpdateCaravanBeastAssignmentCommand;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.application.model.CaravanBeastView;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.TravelerRoleData;
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

class BeastManagementServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryBeastRepository beastRepository;
  private InMemoryWagonRepository wagonRepository;
  private InMemoryImprovementRepository improvementRepository;
  private InMemoryTravelerRepository travelerRepository;
  private BeastManagementService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    beastRepository = new InMemoryBeastRepository();
    wagonRepository = new InMemoryWagonRepository();
    improvementRepository = new InMemoryImprovementRepository();
    travelerRepository = new InMemoryTravelerRepository();
    service = new BeastManagementService(
        caravanRepository,
        beastRepository,
        wagonRepository,
        improvementRepository,
        travelerRepository,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void listsTheCatalog() {
    assertThat(service.list()).extracting("code").contains("caballo-pesado", "poni", "perro-de-trineo");
  }

  @Test
  void createsCatalogAndCustomBeastsAndSupportsAssignmentFlows() {
    var caravan = createCaravan();
    var draftWagon = createWagon(caravan.id(), "carro-escuela");
    var travelerWagon = createWagon(caravan.id(), "carro-cubierto");

    var catalogBeast = service.execute(
        caravan.id(),
        new AddCaravanBeastCommand(
            CaravanBeastSourceType.CATALOG,
            "caballo-pesado",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null));

    var customBeast = service.execute(
        caravan.id(),
        new AddCaravanBeastCommand(
            CaravanBeastSourceType.CUSTOM,
            null,
            "Mula propia",
            "M",
            2,
            30,
            null,
            null,
            null,
            true,
            "Ninguno",
            "Bestia personalizada",
            "Notas internas"));

    var assignedDraft = service.execute(
        caravan.id(),
        catalogBeast.id(),
        new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));
    var assignedTraveler = service.execute(
        caravan.id(),
        customBeast.id(),
        new UpdateCaravanBeastAssignmentCommand(
            CaravanBeastAssignmentType.TRAVELER,
            travelerWagon.id(),
            List.of("pasajero", "carretero"),
            "carretero"));

    assertThat(assignedDraft.assignmentType()).isEqualTo(CaravanBeastAssignmentType.DRAFT);
    assertThat(assignedDraft.assignedWagonId()).isEqualTo(draftWagon.id());
    assertThat(assignedTraveler.assignmentType()).isEqualTo(CaravanBeastAssignmentType.TRAVELER);
    assertThat(assignedTraveler.availableRoleCodes()).containsExactly("pasajero", "carretero");
    assertThat(assignedTraveler.activeRoleCode()).isEqualTo("carretero");
    assertThat(assignedTraveler.customNotes()).isEqualTo("Notas internas");
    assertThat(service.list(caravan.id(), null, "catalog", "draft", draftWagon.id())).hasSize(1);
    assertThat(service.list(caravan.id(), "mula", "custom", "traveler", travelerWagon.id())).hasSize(1);
    assertThat(service.getById(caravan.id(), catalogBeast.id()).assignedWagonName()).isEqualTo("Carro Escuela");
  }

  @Test
  void deletesBeastsById() {
    var caravan = createCaravan();
    var beast = addCatalogBeast(caravan.id(), "poni");

    service.delete(caravan.id(), beast.id());

    assertThat(service.list(caravan.id(), null, null, null, null)).isEmpty();
    assertThatThrownBy(() -> service.getById(caravan.id(), beast.id()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Beast not found");
  }

  @Test
  void createsMultipleBeastsWhenQuantityIsProvided() {
    var caravan = createCaravan();

    service.execute(
        caravan.id(),
        new AddCaravanBeastCommand(
            CaravanBeastSourceType.CATALOG,
            "yak",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            3));

    assertThat(service.list(caravan.id(), null, null, null, null)).hasSize(3);
  }

  @Test
  void defaultsCustomTravelerAssignmentsToPassengerWhenNoRoleDataIsProvided() {
    var caravan = createCaravan();
    var travelerWagon = createWagon(caravan.id(), "carro-cubierto");
    var customBeast = service.execute(
        caravan.id(),
        new AddCaravanBeastCommand(
            CaravanBeastSourceType.CUSTOM,
            null,
            "Mula propia",
            "M",
            2,
            30,
            null,
            null,
            null,
            true,
            "Ninguno",
            "Bestia personalizada",
            "Notas internas"));

    var assignedTraveler = service.execute(
        caravan.id(),
        customBeast.id(),
        new UpdateCaravanBeastAssignmentCommand(
            CaravanBeastAssignmentType.TRAVELER,
            travelerWagon.id(),
            null,
            null));

    assertThat(assignedTraveler.availableRoleCodes()).containsExactly("pasajero");
    assertThat(assignedTraveler.activeRoleCode()).isEqualTo("pasajero");
  }

  @Test
  void rejectsCustomTravelerAssignmentsWhenTheActiveRoleIsNotInTheAvailableList() {
    var caravan = createCaravan();
    var travelerWagon = createWagon(caravan.id(), "carro-cubierto");
    var customBeast = service.execute(
        caravan.id(),
        new AddCaravanBeastCommand(
            CaravanBeastSourceType.CUSTOM,
            null,
            "Mula propia",
            "M",
            2,
            30,
            null,
            null,
            null,
            true,
            "Ninguno",
            "Bestia personalizada",
            "Notas internas"));

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        customBeast.id(),
        new UpdateCaravanBeastAssignmentCommand(
            CaravanBeastAssignmentType.TRAVELER,
            travelerWagon.id(),
            List.of("pasajero"),
            "carretero")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("activeRoleCode");
  }

  @Test
  void deletesOnlyUnassignedBeasts() {
    var caravan = createCaravan();
    var unassigned = addCatalogBeast(caravan.id(), "yak");
    var assigned = addCatalogBeast(caravan.id(), "caballo-pesado");
    var draftWagon = createWagon(caravan.id(), "carro-escuela");
    service.execute(caravan.id(), assigned.id(), new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));

    service.delete(caravan.id());

    assertThat(service.list(caravan.id(), null, null, null, null)).singleElement().extracting("id").isEqualTo(assigned.id());
    assertThatThrownBy(() -> service.getById(caravan.id(), unassigned.id()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Beast not found");
  }

  @Test
  void rejectsDraftAssignmentsWhenTheWagonHasNoMoreLargeSlots() {
    var caravan = createCaravan();
    var draftWagon = createWagon(caravan.id(), "carro-escuela");

    var first = addCatalogBeast(caravan.id(), "caballo-pesado");
    var second = addCatalogBeast(caravan.id(), "bisonte");
    var third = addCatalogBeast(caravan.id(), "yak");

    service.execute(caravan.id(), first.id(), new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));
    service.execute(caravan.id(), second.id(), new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        third.id(),
        new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("large-beast limit");
  }

  @Test
  void rejectsDraftAssignmentsWhenAMediumBeastWouldExceedTheRealSlotCapacityAfterALargeBeast() {
    var caravan = createCaravan();
    var draftWagon = createWagon(caravan.id(), "carro-cubierto");

    var largeBeast = addCatalogBeast(caravan.id(), "caballo-ligero");
    var mediumBeast = addCatalogBeast(caravan.id(), "perro-de-monta");

    service.execute(caravan.id(), largeBeast.id(), new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        mediumBeast.id(),
        new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("medium-beast limit");
  }

  @Test
  void rejectsChangingABeastFromDraftToTravelerWithoutClearingItFirst() {
    var caravan = createCaravan();
    var draftWagon = createWagon(caravan.id(), "carro-cubierto");
    var travelerWagon = createWagon(caravan.id(), "carro-escuela");

    var beast = addCatalogBeast(caravan.id(), "caballo-ligero");
    service.execute(caravan.id(), beast.id(), new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        beast.id(),
        new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.TRAVELER, travelerWagon.id(), null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unassigned");
  }

  @Test
  void allowsDraftAssignmentsEvenWhenTheWagonDoesNotReachMinimumStrength() {
    var caravan = createCaravan();
    var draftWagon = createWagon(caravan.id(), "carro-de-prisioneros");
    improvementRepository.save(CaravanWagonImprovement.create(
        UUID.randomUUID(),
        caravan.id(),
        draftWagon.id(),
        "tiro-de-dos-caballos",
        NOW));

    var first = addCatalogBeast(caravan.id(), "caballo-pesado");
    var second = addCatalogBeast(caravan.id(), "bisonte");

    service.execute(caravan.id(), first.id(), new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));
    service.execute(caravan.id(), second.id(), new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.DRAFT, draftWagon.id(), null, null));

    assertThat(service.list(caravan.id(), null, "catalog", "draft", draftWagon.id())).hasSize(2);
  }

  @Test
  void rejectsTravelerAssignmentsWhenTheWagonIsFull() {
    var caravan = createCaravan();
    var wagon = createWagon(caravan.id(), "carro-cubierto");
    for (var index = 0; index < 8; index++) {
      travelerRepository.save(CaravanTraveler.create(
          UUID.randomUUID(),
          caravan.id(),
          "Traveler " + index,
          null,
          List.of("pasajero"),
          List.of("pasajero"),
          null,
          1,
          TravelerRoleData.empty(),
          wagon.id(),
          null,
          1,
          NOW));
    }

    var beast = addCatalogBeast(caravan.id(), "poni");

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        beast.id(),
        new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.TRAVELER, wagon.id(), null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Wagon capacity reached");
  }

  @Test
  void allowsTravelerBeastWhenTheRemainingSpaceIsFractional() {
    var caravan = createCaravan();
    var wagon = createWagon(caravan.id(), "carro-cubierto");
    for (var index = 0; index < 4; index++) {
      travelerRepository.save(CaravanTraveler.create(
          UUID.randomUUID(),
          caravan.id(),
          "Traveler " + index,
          null,
          List.of("pasajero"),
          List.of("pasajero"),
          null,
          1,
          TravelerRoleData.empty(),
          wagon.id(),
          null,
          1,
          NOW));
    }
    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Traveler zero",
        null,
        List.of("pasajero"),
        List.of("pasajero"),
        null,
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        0,
        NOW));

    var beast = service.execute(
        caravan.id(),
        new AddCaravanBeastCommand(
            CaravanBeastSourceType.CUSTOM,
            null,
            "Perro de Renji",
            "M",
            2,
            30,
            null,
            null,
            null,
            true,
            "Ninguno",
            "Bestia personalizada",
            "Notas internas",
            1,
            BigDecimal.valueOf(0.5)));

    var assignedTraveler = service.execute(
        caravan.id(),
        beast.id(),
        new UpdateCaravanBeastAssignmentCommand(CaravanBeastAssignmentType.TRAVELER, wagon.id(), null, null));

    assertThat(assignedTraveler.assignmentType()).isEqualTo(CaravanBeastAssignmentType.TRAVELER);
    assertThat(assignedTraveler.assignedWagonId()).isEqualTo(wagon.id());
  }

  private CaravanBeastView addCatalogBeast(UUID caravanId, String catalogCode) {
    return service.execute(
        caravanId,
        new AddCaravanBeastCommand(
            CaravanBeastSourceType.CATALOG,
            catalogCode,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null));
  }

  private CaravanCampaign createCaravan() {
    var caravan = CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, NOW);
    caravanRepository.save(caravan);
    return caravan;
  }

  private CaravanWagon createWagon(UUID caravanId, String wagonTypeCode) {
    var wagon = CaravanWagon.create(UUID.randomUUID(), caravanId, wagonTypeCode, null, NOW);
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
}

