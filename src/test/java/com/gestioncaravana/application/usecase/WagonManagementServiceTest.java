package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanMainStats;
import com.gestioncaravana.domain.CaravanCampaignStatus;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.TravelerRoleData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WagonManagementServiceTest {

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryWagonRepository wagonRepository;
  private InMemoryImprovementRepository improvementRepository;
  private InMemoryBeastRepository beastRepository;
  private InMemoryTravelerRepository travelerRepository;
  private WagonManagementService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    wagonRepository = new InMemoryWagonRepository();
    improvementRepository = new InMemoryImprovementRepository();
    beastRepository = new InMemoryBeastRepository();
    travelerRepository = new InMemoryTravelerRepository();
    service = new WagonManagementService(
        caravanRepository,
        wagonRepository,
        improvementRepository,
        beastRepository,
        travelerRepository,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void listsTheCatalog() {
    assertThat(service.list()).extracting("code").contains("carro-cubierto", "carro-de-viajeros", "carro-vacio");
  }

  @Test
  void addsAndListsWagonsForTheCaravan() {
    var caravan = createCaravan();

    var created = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto", "Carromato del grupo"));

    assertThat(created.name()).isEqualTo("Carromato del grupo");
    assertThat(service.list(caravan.id())).hasSize(1);
    assertThat(service.getById(caravan.id(), created.id()).wagonTypeCode()).isEqualTo("carro-cubierto");
    assertThat(service.getById(caravan.id(), created.id()).improvements()).isEmpty();
  }

  @Test
  void renamesWagonWithoutChangingItsType() {
    var caravan = createCaravan();
    var wagon = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto", null));

    var renamed = service.execute(caravan.id(), wagon.id(), new com.gestioncaravana.application.port.in.UpdateCaravanWagonUseCase.UpdateCaravanWagonCommand("La Rueda Roja"));

    assertThat(renamed.name()).isEqualTo("La Rueda Roja");
    assertThat(renamed.wagonTypeCode()).isEqualTo("carro-cubierto");
    assertThat(service.getById(caravan.id(), wagon.id()).name()).isEqualTo("La Rueda Roja");
  }

  @Test
  void rejectsUnknownWagonTypes() {
    var caravan = createCaravan();

    assertThatThrownBy(() -> service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("unknown", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Wagon type not found");
  }

  @Test
  void enforcesPerTypeLimits() {
    var caravan = createCaravan();
    service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-arcano", null));

    assertThatThrownBy(() -> service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-arcano", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Wagon type limit reached");
  }

  @Test
  void allowsCaravanToExceedItsMaximumWagonCount() {
    var caravan = createCaravan();
    for (int i = 0; i < 12; i++) {
      service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto", null));
    }

    assertThat(service.list(caravan.id())).hasSize(12);
  }

  @Test
  void deletesWagonsFromAcaravan() {
    var caravan = createCaravan();
    var created = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto", null));

    service.delete(caravan.id(), created.id());

    assertThat(service.list(caravan.id())).isEmpty();
  }

  @Test
  void deletesWagonAndClearsAssignedBeastsAndTravelers() {
    var caravan = createCaravan();
    var wagon = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto", null));

    var draftBeast = beastRepository.save(CaravanBeast.createCustom(
        UUID.randomUUID(),
        caravan.id(),
        "Bestia de tiro",
        "G",
        6,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Bestia de tiro",
        null,
        Instant.parse("2026-01-01T00:00:00Z")).assignDraft(wagon.id(), Instant.parse("2026-01-01T00:00:00Z")));

    var traveler = travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Viajero",
        null,
        List.of("pasajero"),
        List.of("pasajero"),
        "pasajero",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        1,
        Instant.parse("2026-01-01T00:00:00Z")));

    service.delete(caravan.id(), wagon.id());

    assertThat(service.list(caravan.id())).isEmpty();
    assertThat(beastRepository.findById(caravan.id(), draftBeast.id())).hasValueSatisfying(beast -> {
      assertThat(beast.assignmentType()).isEqualTo(CaravanBeastAssignmentType.NONE);
      assertThat(beast.assignedWagonId()).isNull();
    });
    assertThat(travelerRepository.findById(caravan.id(), traveler.id())).hasValueSatisfying(updated -> {
      assertThat(updated.wagonId()).isNull();
    });
  }

  @Test
  void addsAndRemovesImprovementAndUpdatesDerivedStats() {
    var caravan = createCaravan();
    var wagon = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto", null));

    var withImprovement = service.execute(
        caravan.id(),
        wagon.id(),
        new com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase.AddCaravanWagonImprovementCommand("refuerzo-para-carros"));

    assertThat(withImprovement.hitPoints()).isEqualTo(40);
    assertThat(withImprovement.cargoCapacity()).isEqualTo(3);
    assertThat(withImprovement.improvements()).hasSize(1);

    var improvementId = withImprovement.improvements().getFirst().id();
    var afterRemoval = service.execute(caravan.id(), wagon.id(), improvementId);

    assertThat(afterRemoval.hitPoints()).isEqualTo(30);
    assertThat(afterRemoval.improvements()).isEmpty();
  }

  @Test
  void updatesDraftAndAllowsHigherTierDraftImprovementAfterThePreviousOne() {
    var caravan = createCaravan();
    var wagon = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-de-prisioneros", null));

    var afterTwoHorses = service.execute(
        caravan.id(),
        wagon.id(),
        new com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase.AddCaravanWagonImprovementCommand("tiro-de-dos-caballos"));

    assertThat(afterTwoHorses.propulsion()).isEqualTo("2 Criatura grande / 8 medianas (+10 Fuerza)");

    var afterFourHorses = service.execute(
        caravan.id(),
        wagon.id(),
        new com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase.AddCaravanWagonImprovementCommand("tiro-de-cuatro-caballos"));

    assertThat(afterFourHorses.propulsion()).isEqualTo("4 Criatura grande / 16 medianas (+20 Fuerza)");
    assertThat(afterFourHorses.improvements()).extracting("improvementTypeCode")
        .containsExactly("tiro-de-dos-caballos", "tiro-de-cuatro-caballos");
  }

  @Test
  void derivesPropulsionForMediumOnlyDraftVehiclesWithoutBreakingTheFormat() {
    var caravan = createCaravan();
    var wagon = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("trineo-de-pasajeros", null));

    assertThat(wagon.propulsion()).isEqualTo("2 medianas (+4 Fuerza)");
  }

  @Test
  void includesCurrentDraftBeastsAndTheirEffectiveStrengthInWagonDetails() {
    var caravan = createCaravan();
    var wagon = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-de-prisioneros", null));

    beastRepository.save(CaravanBeast.createCustom(
        UUID.randomUUID(),
        caravan.id(),
        "Mula de tiro",
        "G",
        6,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Bestia de tiro",
        null,
        Instant.parse("2026-01-01T00:00:00Z")).assignDraft(wagon.id(), Instant.parse("2026-01-01T00:00:00Z")));

    var detail = service.getById(caravan.id(), wagon.id());

    assertThat(detail.draftBeasts()).hasSize(1);
    assertThat(detail.draftBeasts().getFirst().name()).isEqualTo("Mula de tiro");
    assertThat(detail.draftStrength()).isEqualTo(12);
    assertThat(detail.draftRequiredStrength()).isEqualTo(5);
  }

  @Test
  void rejectsIncompatibleImprovements() {
    var caravan = createCaravan();
    var wagon = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto", null));
    service.execute(caravan.id(), wagon.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase.AddCaravanWagonImprovementCommand("patines-de-hielo"));

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        wagon.id(),
        new com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase.AddCaravanWagonImprovementCommand("ruedas-mejoradas")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Incompatible");
  }

  private CaravanCampaign createCaravan() {
    var caravan = CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z"));
    caravanRepository.save(caravan);
    return caravan;
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
    public List<CaravanBeast> findAllByCaravanIdAndWagonIdAndAssignmentType(UUID caravanId, UUID wagonId, CaravanBeastAssignmentType assignmentType) {
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

