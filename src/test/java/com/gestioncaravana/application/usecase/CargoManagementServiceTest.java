package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.in.AddCaravanCargoUseCase;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CargoManagementServiceTest {

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryCargoRepository cargoRepository;
  private InMemoryWagonRepository wagonRepository;
  private InMemoryImprovementRepository improvementRepository;
  private InMemoryTravelerRepository travelerRepository;
  private InMemoryFeatRepository featRepository;
  private CargoManagementService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    cargoRepository = new InMemoryCargoRepository();
    wagonRepository = new InMemoryWagonRepository();
    improvementRepository = new InMemoryImprovementRepository();
    travelerRepository = new InMemoryTravelerRepository();
    featRepository = new InMemoryFeatRepository();
    service = new CargoManagementService(
        caravanRepository,
        cargoRepository,
        wagonRepository,
        improvementRepository,
        travelerRepository,
        featRepository,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @ParameterizedTest
  @CsvSource({"suministros", "suministros-perecederos"})
  @DisplayName("adds catalog supplies as individual unit rows")
  void addsCatalogSuppliesAsIndividualUnitRows(String catalogCode) {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-de-suministros", null, Instant.parse("2026-01-01T00:00:00Z")));

    var created = service.execute(
        caravan.id(),
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CATALOG,
            catalogCode,
            null,
            null,
            3,
            null,
            wagon.id(),
            null,
            null,
            null,
            null));

    assertThat(created.quantity()).isEqualTo(1);
    assertThat(created.catalogCode()).isEqualTo(catalogCode);
    assertThat(cargoRepository.findAllByCaravanId(caravan.id()))
        .hasSize(3)
        .allSatisfy(entry -> {
          assertThat(entry.quantity()).isEqualTo(1);
          assertThat(entry.cargoUnits()).isEqualTo(1);
          assertThat(entry.catalogCode()).isEqualTo(catalogCode);
          assertThat(entry.wagonId()).isEqualTo(wagon.id());
          assertThat(entry.currentProvisions()).isEqualTo(BigDecimal.TEN);
          assertThat(entry.dayPassed()).isFalse();
        });
  }

  @org.junit.jupiter.api.Test
  void exposesSupplyStateInCargoViews() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-de-suministros", null, Instant.parse("2026-01-01T00:00:00Z")));
    var cargo = cargoRepository.save(CaravanCargo.create(
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
        Instant.parse("2026-01-01T00:00:00Z")).withCurrentProvisions(BigDecimal.valueOf(7), true, Instant.parse("2026-01-02T00:00:00Z")));

    var viewed = service.getById(caravan.id(), cargo.id());

    assertThat(viewed.currentProvisions()).isEqualTo(BigDecimal.valueOf(7));
    assertThat(viewed.dayPassed()).isTrue();
  }

  @org.junit.jupiter.api.Test
  void allowsCustomCargoOnSpecificGoodsWagonByInheritingItsSpecificCommodity() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(
        UUID.randomUUID(),
        caravan.id(),
        "carro-de-mercancias-especificas",
        null,
        "Queso",
        Instant.parse("2026-01-01T00:00:00Z")));

    var created = service.execute(
        caravan.id(),
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CUSTOM,
            null,
            "Queso",
            "Lácteos",
            1,
            2,
            wagon.id(),
            null,
            null,
            null,
            null));

    assertThat(created.specificCommodity()).isEqualTo("Queso");
    assertThat(cargoRepository.findAllByCaravanId(caravan.id())).singleElement().satisfies(entry -> {
      assertThat(entry.wagonId()).isEqualTo(wagon.id());
      assertThat(entry.specificCommodity()).isEqualTo("Queso");
    });
  }

  @org.junit.jupiter.api.Test
  void allowsZeroCargoUnitsForRoyalWagonCatalogCargo() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(
        UUID.randomUUID(),
        caravan.id(),
        "carruaje-real",
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));

    var created = service.execute(
        caravan.id(),
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CATALOG,
            "sello-de-amatatsu",
            null,
            null,
            1,
            null,
            wagon.id(),
            null,
            null,
            null,
            null));

    assertThat(created.catalogCode()).isEqualTo("sello-de-amatatsu");
    assertThat(created.cargoUnits()).isZero();
    assertThat(cargoRepository.findAllByCaravanId(caravan.id()))
        .singleElement()
        .satisfies(entry -> {
          assertThat(entry.cargoUnits()).isZero();
          assertThat(entry.wagonId()).isEqualTo(wagon.id());
        });
  }

  @org.junit.jupiter.api.Test
  void validatesCargoCapacityWithCargoManagersAndOrganizationFeat() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-de-mercancias", null, Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Encargado 1",
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
    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Encargado 2",
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

    var created = service.execute(
        caravan.id(),
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CUSTOM,
            null,
            "Cajas",
            "Carga",
            13,
            1,
            wagon.id(),
            null,
            null,
            null,
            null));

    assertThat(created.quantity()).isEqualTo(13);
    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CUSTOM,
            null,
            "Sobrecarga",
            "Carga",
            1,
            1,
            wagon.id(),
            null,
            null,
            null,
            null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Wagon cargo capacity reached");
  }

  @org.junit.jupiter.api.Test
  void allowsCustomCargoWithZeroCargoUnits() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-de-mercancias", null, Instant.parse("2026-01-01T00:00:00Z")));

    var created = service.execute(
        caravan.id(),
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CUSTOM,
            null,
            "Bandera ceremonial",
            "Objeto",
            1,
            0,
            wagon.id(),
            null,
            null,
            null,
            null));

    assertThat(created.cargoUnits()).isZero();
    assertThat(cargoRepository.findAllByCaravanId(caravan.id()))
        .singleElement()
        .satisfies(entry -> assertThat(entry.cargoUnits()).isZero());
  }

  @org.junit.jupiter.api.Test
  void rejectsCustomCargoWhenSpecificGoodsWagonCommodityDoesNotMatch() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(
        UUID.randomUUID(),
        caravan.id(),
        "carro-de-mercancias-especificas",
        null,
        "Queso",
        Instant.parse("2026-01-01T00:00:00Z")));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.execute(
        caravan.id(),
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CUSTOM,
            null,
            "Manzanas",
            "Fruta",
            1,
            1,
            wagon.id(),
            null,
            "Manzanas",
            null,
            null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("specific merchandise");
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

  private static final class InMemoryCargoRepository implements CaravanCargoRepositoryPort {
    private final List<CaravanCargo> cargo = new ArrayList<>();

    @Override
    public CaravanCargo save(CaravanCargo cargoEntry) {
      cargo.removeIf(existing -> existing.id().equals(cargoEntry.id()));
      cargo.add(cargoEntry);
      return cargoEntry;
    }

    @Override
    public List<CaravanCargo> findAllByCaravanId(UUID caravanId) {
      return cargo.stream().filter(entry -> entry.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanCargo> findById(UUID caravanId, UUID cargoId) {
      return cargo.stream().filter(entry -> entry.caravanId().equals(caravanId) && entry.id().equals(cargoId)).findFirst();
    }

    @Override
    public void deleteById(UUID caravanId, UUID cargoId) {
      cargo.removeIf(entry -> entry.caravanId().equals(caravanId) && entry.id().equals(cargoId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      cargo.removeIf(entry -> entry.caravanId().equals(caravanId));
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return cargo.stream().filter(entry -> entry.caravanId().equals(caravanId) && wagonId.equals(entry.wagonId())).count();
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
      return wagons.stream().filter(wagon -> wagon.caravanId().equals(caravanId) && wagon.id().equals(wagonId)).findFirst();
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId) {}

    @Override
    public long countByCaravanId(UUID caravanId) {
      return wagons.stream().filter(wagon -> wagon.caravanId().equals(caravanId)).count();
    }

    @Override
    public long countByCaravanIdAndWagonTypeCode(UUID caravanId, String wagonTypeCode) {
      return wagons.stream().filter(wagon -> wagon.caravanId().equals(caravanId) && wagon.wagonTypeCode().equals(wagonTypeCode)).count();
    }
  }

  private static final class InMemoryImprovementRepository implements CaravanWagonImprovementRepositoryPort {
    @Override
    public CaravanWagonImprovement save(CaravanWagonImprovement improvement) {
      return improvement;
    }

    @Override
    public List<CaravanWagonImprovement> findAllByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return List.of();
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
      return travelers.stream().filter(traveler -> traveler.caravanId().equals(caravanId) && traveler.id().equals(travelerId)).findFirst();
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
      return feats.stream().filter(feat -> feat.caravanId().equals(caravanId) && feat.id().equals(featId)).findFirst();
    }

    @Override
    public long countByCaravanIdAndFeatTypeCode(UUID caravanId, String featTypeCode) {
      return feats.stream().filter(feat -> feat.caravanId().equals(caravanId) && feat.active() && feat.featTypeCode().equals(featTypeCode)).count();
    }

    @Override
    public void deleteById(UUID caravanId, UUID featId) {}

    @Override
    public void deleteByCaravanId(UUID caravanId) {}
  }
}

