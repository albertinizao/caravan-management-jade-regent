package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.TravelerRoleData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaravanStatisticsServiceTest {

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryWagonRepository wagonRepository;
  private InMemoryImprovementRepository improvementRepository;
  private InMemoryTravelerRepository travelerRepository;
  private InMemoryBeastRepository beastRepository;
  private InMemoryCargoRepository cargoRepository;
  private InMemoryFeatRepository featRepository;
  private CaravanStatisticsService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    wagonRepository = new InMemoryWagonRepository();
    improvementRepository = new InMemoryImprovementRepository();
    travelerRepository = new InMemoryTravelerRepository();
    beastRepository = new InMemoryBeastRepository();
    cargoRepository = new InMemoryCargoRepository();
    featRepository = new InMemoryFeatRepository();
    service = new CaravanStatisticsService(
        caravanRepository,
        wagonRepository,
        improvementRepository,
        travelerRepository,
        beastRepository,
        cargoRepository,
        featRepository);
  }

  @Test
  void derivesAFullStatSheetFromTheCurrentCaravanComposition() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-cubierto", null, Instant.parse("2026-01-01T00:00:00Z")));

    beastRepository.save(CaravanBeast.createCustom(
        UUID.randomUUID(),
        caravan.id(),
        "Mula",
        "G",
        3,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Bestia de tiro",
        null,
        Instant.parse("2026-01-01T00:00:00Z")).assignDraft(wagon.id(), Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Guarda",
        null,
        List.of("pasajero", "guarda"),
        List.of("guarda"),
        "guarda",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        1,
        Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Comediante",
        null,
        List.of("pasajero", "comediante"),
        List.of("comediante"),
        "comediante",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        1,
        Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Batidor",
        null,
        List.of("pasajero", "batidor"),
        List.of("batidor"),
        "batidor",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        1,
        Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Encargado",
        null,
        List.of("pasajero", "encargado-de-suministros"),
        List.of("encargado-de-suministros"),
        "encargado-de-suministros",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        1,
        Instant.parse("2026-01-01T00:00:00Z")));

    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros",
        "Suministros",
        "Carga",
        1,
        1,
        wagon.id(),
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));

    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        caravan.id(),
        "caravana-familiar",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "Mesa",
        1,
        true,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));
    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        caravan.id(),
        "lider-de-la-caravana",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "Mesa",
        1,
        true,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")).updateAcquisition(
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "Mesa",
            false,
            null,
            null,
            Instant.parse("2026-01-01T00:00:00Z")));

    var statistics = service.getById(caravan.id());

    assertThat(statistics.derivedStats().attack()).isEqualTo(2);
    assertThat(statistics.derivedStats().armorClass()).isEqualTo(11);
    assertThat(statistics.derivedStats().security()).isEqualTo(0);
    assertThat(statistics.derivedStats().determination()).isEqualTo(1);
    assertThat(statistics.otherStats().speed()).isEqualTo(16);
    assertThat(statistics.otherStats().travelerCapacity()).isEqualTo(8);
    assertThat(statistics.otherStats().cargoCapacity()).isEqualTo(4);
    assertThat(statistics.otherStats().cargoLoad()).isEqualTo(1);
    assertThat(statistics.otherStats().consumption()).isEqualTo(5);
    assertThat(statistics.contributions()).anyMatch(contribution ->
        contribution.statCode().equals("consumption")
            && contribution.sourceType().equals("ROLE")
            && contribution.sourceName().equals("Batidor")
            && contribution.modifier().equals("0"));
    assertThat(statistics.warnings()).anyMatch(message -> message.contains("adivino"));
  }

  @Test
  void addsCargoCapacityBonusesForOrganizationFeatAndSupplyManagers() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-cubierto", null, Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Encargado",
        null,
        List.of("pasajero", "encargado-de-suministros"),
        List.of("encargado-de-suministros"),
        "encargado-de-suministros",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        1,
        Instant.parse("2026-01-01T00:00:00Z")));

    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        caravan.id(),
        "organizacion-impecable",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "Mesa",
        1,
        true,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));

    var statistics = service.getById(caravan.id());

    assertThat(statistics.otherStats().cargoCapacity()).isEqualTo(5);
  }

  @Test
  void appliesManualFeatBonusesWhenTheyAreActive() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        caravan.id(),
        "lider-de-la-caravana",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "Mesa",
        1,
        true,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));

    var statistics = service.getById(caravan.id());

    assertThat(statistics.mainStats().morale()).isEqualTo(2);
    assertThat(statistics.derivedStats().security()).isEqualTo(0);
    assertThat(statistics.derivedStats().determination()).isEqualTo(1);
  }

  @Test
  void warnsWhenTheCaravanExceedsItsWagonLimit() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    for (int i = 0; i < 12; i++) {
      wagonRepository.save(CaravanWagon.create(
          UUID.randomUUID(),
          caravan.id(),
          "carro-cubierto",
          null,
          Instant.parse("2026-01-01T00:00:00Z")));
    }

    var statistics = service.getById(caravan.id());

    assertThat(statistics.otherStats().wagonCount()).isEqualTo(12);
    assertThat(statistics.otherStats().maxWagons()).isEqualTo(11);
    assertThat(statistics.warnings()).anyMatch(message -> message.contains("cada carro adicional"));
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
    public void deleteById(UUID id) {}

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
    public void deleteById(UUID caravanId, UUID wagonId) {}

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
      return Optional.empty();
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId, UUID improvementId) {}
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
      return Optional.empty();
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return travelers.stream()
          .filter(traveler -> traveler.caravanId().equals(caravanId) && wagonId.equals(traveler.wagonId()))
          .count();
    }

    @Override
    public void deleteByCaravanIdAndId(UUID caravanId, UUID travelerId) {}

    @Override
    public void deleteByCaravanId(UUID caravanId) {}
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
              && wagonId.equals(beast.assignedWagonId())
              && beast.assignmentType() == assignmentType)
          .toList();
    }

    @Override
    public Optional<CaravanBeast> findById(UUID caravanId, UUID beastId) {
      return Optional.empty();
    }

    @Override
    public void deleteByCaravanIdAndId(UUID caravanId, UUID beastId) {
      beasts.removeIf(beast -> beast.caravanId().equals(caravanId) && beast.id().equals(beastId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {}
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
      return Optional.empty();
    }

    @Override
    public void deleteById(UUID caravanId, UUID cargoId) {}

    @Override
    public void deleteByCaravanId(UUID caravanId) {}

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return cargos.stream()
          .filter(cargo -> cargo.caravanId().equals(caravanId) && wagonId.equals(cargo.wagonId()))
          .count();
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
    public void deleteById(UUID caravanId, UUID featId) {}

    @Override
    public void deleteByCaravanId(UUID caravanId) {}
  }
}

