package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.in.AdvanceCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.in.PreviewCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanDayResolutionRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanDayResolution;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
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

class CaravanDayCycleServiceTest {

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryWagonRepository wagonRepository;
  private InMemoryTravelerRepository travelerRepository;
  private InMemoryBeastRepository beastRepository;
  private InMemoryCargoRepository cargoRepository;
  private InMemoryFeatRepository featRepository;
  private InMemorySupplyStateRepository supplyStateRepository;
  private InMemoryResolutionRepository resolutionRepository;
  private CaravanDayCycleService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    wagonRepository = new InMemoryWagonRepository();
    travelerRepository = new InMemoryTravelerRepository();
    beastRepository = new InMemoryBeastRepository();
    cargoRepository = new InMemoryCargoRepository();
    featRepository = new InMemoryFeatRepository();
    supplyStateRepository = new InMemorySupplyStateRepository();
    resolutionRepository = new InMemoryResolutionRepository();
    var statisticsUseCase = new CaravanStatisticsService(
        caravanRepository,
        wagonRepository,
        new InMemoryImprovementRepository(),
        travelerRepository,
        beastRepository,
        cargoRepository,
        featRepository);
    service = new CaravanDayCycleService(
        caravanRepository,
        travelerRepository,
        wagonRepository,
        new InMemoryImprovementRepository(),
        featRepository,
        cargoRepository,
        supplyStateRepository,
        resolutionRepository,
        statisticsUseCase,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void farmersToggleTheirProductionMarkerAndPreferSupplyWagonsWhenGenerating() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var huertoWagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-huerto", Instant.parse("2026-01-01T00:00:00Z")));
    var suppliesWagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-de-suministros", Instant.parse("2026-01-01T00:00:00Z")));

    var farmer = travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Agricultor",
        null,
        List.of("pasajero", "agricultor"),
        List.of("agricultor"),
        "agricultor",
        1,
        TravelerRoleData.empty(),
        huertoWagon.id(),
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 1, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    var firstDay = service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-1",
        false,
        List.of()));

    assertThat(firstDay.totalGeneration()).isEqualTo(0);
    assertThat(firstDay.endingReserve()).isEqualTo(0);
    assertThat(travelerRepository.findById(caravan.id(), farmer.id()))
        .hasValueSatisfying(traveler -> assertThat(traveler.roleSpecificData().generatingFood()).isTrue());
    assertThat(cargoRepository.findAllByCaravanId(caravan.id())).isEmpty();

    var secondDay = service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-2",
        false,
        List.of()));

    assertThat(secondDay.totalGeneration()).isEqualTo(0);
    assertThat(travelerRepository.findById(caravan.id(), farmer.id()))
        .hasValueSatisfying(traveler -> assertThat(traveler.roleSpecificData().generatingFood()).isFalse());
    assertThat(cargoRepository.findAllByCaravanId(caravan.id())).singleElement().satisfies(entry -> {
      assertThat(entry.catalogCode()).isEqualTo("suministros");
      assertThat(entry.wagonId()).isEqualTo(suppliesWagon.id());
    });
  }

  @Test
  void farmersFallBackToAnyWagonWithFreeSpaceWhenNoSupplyWagonExists() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var huertoWagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-huerto", Instant.parse("2026-01-01T00:00:00Z")));
    var farmer = travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Agricultor",
        null,
        List.of("pasajero", "agricultor"),
        List.of("agricultor"),
        "agricultor",
        1,
        TravelerRoleData.empty(),
        huertoWagon.id(),
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 1, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-fallback-1",
        false,
        List.of()));

    var secondDay = service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-fallback-2",
        false,
        List.of()));

    assertThat(secondDay.totalGeneration()).isEqualTo(0);
    assertThat(travelerRepository.findById(caravan.id(), farmer.id()))
        .hasValueSatisfying(traveler -> assertThat(traveler.roleSpecificData().generatingFood()).isFalse());
    assertThat(cargoRepository.findAllByCaravanId(caravan.id())).singleElement().satisfies(entry -> {
      assertThat(entry.catalogCode()).isEqualTo("suministros");
      assertThat(entry.wagonId()).isEqualTo(huertoWagon.id());
    });
  }

  @Test
  void farmersWarnWhenTheirGeneratedSupplyCannotFitAnywhere() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var huertoWagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-huerto", Instant.parse("2026-01-01T00:00:00Z")));
    var farmer = travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Agricultor",
        null,
        List.of("pasajero", "agricultor"),
        List.of("agricultor"),
        "agricultor",
        1,
        TravelerRoleData.empty(),
        huertoWagon.id(),
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "mercancias-locales",
        "Mercancías Locales",
        "Artículos de mercancía",
        1,
        1,
        huertoWagon.id(),
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 1, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-warning-1",
        false,
        List.of()));

    var secondDay = service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-warning-2",
        false,
        List.of()));

    assertThat(secondDay.warnings()).contains("Se pierde 1 unidad de suministros por no tener hueco.");
    assertThat(secondDay.totalGeneration()).isEqualTo(0);
    assertThat(travelerRepository.findById(caravan.id(), farmer.id()))
        .hasValueSatisfying(traveler -> assertThat(traveler.roleSpecificData().generatingFood()).isFalse());
    assertThat(cargoRepository.findAllByCaravanId(caravan.id())).hasSize(1);
  }

  @Test
  void cooksAddFivePerFullTenOfBaseGenerationAndStopAtAvailableBlocks() {
    var oneCookCaravan = createCookBonusScenario(1);
    var twoCookCaravan = createCookBonusScenario(2);
    var threeCookCaravan = createCookBonusScenario(3);

    assertThat(service.preview(oneCookCaravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of())).expectedGeneration())
        .isEqualTo(27);
    assertThat(service.preview(twoCookCaravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of())).expectedGeneration())
        .isEqualTo(32);
    assertThat(service.preview(threeCookCaravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of())).expectedGeneration())
        .isEqualTo(32);
  }

  @Test
  void servantsIncreaseCookPerformanceByFiftyPercent() {
    var caravan = createCookBonusScenario(1);
    var cook = travelerRepository.findAllByCaravanId(caravan.id()).stream()
        .filter(traveler -> "cocinero".equals(traveler.activeRoleCode()))
        .findFirst()
        .orElseThrow();

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Sirviente del cocinero",
        null,
        List.of("pasajero", "sirviente"),
        List.of("sirviente"),
        "sirviente",
        1,
        new TravelerRoleData(cook.id()),
        null,
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    assertThat(service.preview(caravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of())).expectedGeneration())
        .isEqualTo(30);
  }

  @Test
  void servantsThatCanAlsoCookDoubleTheCookBonus() {
    var caravan = createCookBonusScenario(1);
    var cook = travelerRepository.findAllByCaravanId(caravan.id()).stream()
        .filter(traveler -> "cocinero".equals(traveler.activeRoleCode()))
        .findFirst()
        .orElseThrow();

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Sirviente del cocinero",
        null,
        List.of("pasajero", "sirviente", "cocinero"),
        List.of("sirviente"),
        "sirviente",
        1,
        new TravelerRoleData(cook.id()),
        null,
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    assertThat(service.preview(caravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of())).expectedGeneration())
        .isEqualTo(32);
  }

  @Test
  void cooksAlsoBoostTheStoredSupplyStockThatWillBeSpentThatDay() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Cargo-Cook", null, Instant.parse("2026-01-01T00:00:00Z")));

    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros",
        "Suministros",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")).withCurrentProvisions(10, false, Instant.parse("2026-01-01T00:00:00Z")));
    cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros",
        "Suministros",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")).withCurrentProvisions(10, false, Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Cocinero",
        null,
        List.of("pasajero", "cocinero"),
        List.of("cocinero"),
        "cocinero",
        1,
        TravelerRoleData.empty(),
        null,
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    var preview = service.preview(caravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of()));

    assertThat(preview.currentReserve()).isEqualTo(20);
    assertThat(preview.expectedGeneration()).isEqualTo(5);
    assertThat(preview.expectedReserveAfterResolution()).isEqualTo(24);
  }

  @Test
  void servantsGiveFarmersAnExtraSupplyEveryFourDays() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Farmer-Servant", null, Instant.parse("2026-01-01T00:00:00Z")));
    var huertoWagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-huerto", Instant.parse("2026-01-01T00:00:00Z")));
    var farmer = travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Agricultor",
        null,
        List.of("pasajero", "agricultor"),
        List.of("agricultor"),
        "agricultor",
        1,
        new TravelerRoleData(null, true, 3),
        huertoWagon.id(),
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Sirviente del agricultor",
        null,
        List.of("pasajero", "sirviente"),
        List.of("sirviente"),
        "sirviente",
        1,
        new TravelerRoleData(farmer.id(), false, 3),
        null,
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Sirviente del agricultor",
        null,
        List.of("pasajero", "sirviente"),
        List.of("sirviente"),
        "sirviente",
        1,
        new TravelerRoleData(farmer.id(), false, 3),
        null,
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 1, 0, 0, 3, Instant.parse("2026-01-01T00:00:00Z")));

    var dayFourPreview = service.preview(caravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of()));
    assertThat(dayFourPreview.contributions())
        .anySatisfy(contribution -> {
          assertThat(contribution.sourceName()).isEqualTo("Agricultor");
          assertThat(contribution.quantity()).isEqualTo(30);
          assertThat(contribution.reason()).contains("ayuda de sus sirvientes");
        });
  }

  @Test
  void servantsThatCanAlsoFarmTriggerTheBonusEveryTwoDays() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Farmer-Servant-Compat", null, Instant.parse("2026-01-01T00:00:00Z")));
    var huertoWagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-huerto", Instant.parse("2026-01-01T00:00:00Z")));
    var farmer = travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Agricultor",
        null,
        List.of("pasajero", "agricultor"),
        List.of("agricultor"),
        "agricultor",
        1,
        new TravelerRoleData(null, true, 1),
        huertoWagon.id(),
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Sirviente del agricultor",
        null,
        List.of("pasajero", "sirviente", "agricultor"),
        List.of("sirviente"),
        "sirviente",
        1,
        new TravelerRoleData(farmer.id(), false, 1),
        null,
        null,
        1,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 1, 0, 0, 1, Instant.parse("2026-01-01T00:00:00Z")));

    var dayTwoPreview = service.preview(caravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(false, List.of()));
    assertThat(dayTwoPreview.contributions())
        .anySatisfy(contribution -> {
          assertThat(contribution.sourceName()).isEqualTo("Agricultor");
          assertThat(contribution.quantity()).isEqualTo(20);
          assertThat(contribution.reason()).contains("ayuda de su sirviente");
        });
  }

  @Test
  void rejectsFastingWhenTheCaravanDoesNotHaveTheFeat() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    assertThatThrownBy(() -> service.preview(caravan.id(), new PreviewCaravanDayCycleUseCase.PreviewCaravanDayCycleCommand(true, List.of())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Ayuno Intermitente");
  }

  @Test
  void consumesPerishableCargoBeforeStandardCargoAndRemovesConsumedEntries() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var perishableCargo = cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros-perecederos",
        "Suministros Perecederos",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")));
    var standardCargo = cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros",
        "Suministros",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-order",
        false,
        List.of()));

    assertThat(cargoRepository.findById(caravan.id(), perishableCargo.id())).hasValueSatisfying(entry -> {
      assertThat(entry.currentProvisions()).isEqualTo(10);
      assertThat(entry.dayPassed()).isTrue();
    });
    assertThat(cargoRepository.findById(caravan.id(), standardCargo.id())).hasValueSatisfying(entry -> {
      assertThat(entry.currentProvisions()).isEqualTo(10);
    });
    assertThat(supplyStateRepository.findByCaravanId(caravan.id())).hasValueSatisfying(state -> {
      assertThat(state.provisionReserve()).isEqualTo(0);
      assertThat(state.perishableReserve()).isEqualTo(0);
      assertThat(state.standardReserve()).isEqualTo(0);
      assertThat(state.daysPassed()).isEqualTo(1);
    });
  }

  @Test
  void togglesPerishableDayPassedAndReducesOneProvisionOnTheSecondAcceptedDay() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var perishableCargo = cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros-perecederos",
        "Suministros Perecederos",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-decay",
        false,
        List.of()));

    assertThat(cargoRepository.findById(caravan.id(), perishableCargo.id())).hasValueSatisfying(entry -> {
      assertThat(entry.currentProvisions()).isEqualTo(10);
      assertThat(entry.dayPassed()).isTrue();
    });

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-decay-2",
        false,
        List.of()));

    assertThat(cargoRepository.findById(caravan.id(), perishableCargo.id())).hasValueSatisfying(entry -> {
      assertThat(entry.currentProvisions()).isEqualTo(9);
      assertThat(entry.dayPassed()).isFalse();
    });
  }

  @Test
  void reducesOneProvisionPerPerishableUnitEveryTwoDays() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var firstUnit = cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros-perecederos",
        "Suministros Perecederos",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")));
    var secondUnit = cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros-perecederos",
        "Suministros Perecederos",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-decay-two-units",
        false,
        List.of()));

    assertThat(cargoRepository.findById(caravan.id(), firstUnit.id())).hasValueSatisfying(entry -> {
      assertThat(entry.quantity()).isEqualTo(1);
      assertThat(entry.currentProvisions()).isEqualTo(10);
      assertThat(entry.dayPassed()).isTrue();
    });
    assertThat(cargoRepository.findById(caravan.id(), secondUnit.id())).hasValueSatisfying(entry -> {
      assertThat(entry.quantity()).isEqualTo(1);
      assertThat(entry.currentProvisions()).isEqualTo(10);
      assertThat(entry.dayPassed()).isTrue();
    });

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-decay-two-units-2",
        false,
        List.of()));

    assertThat(cargoRepository.findById(caravan.id(), firstUnit.id())).hasValueSatisfying(entry -> {
      assertThat(entry.currentProvisions()).isEqualTo(9);
      assertThat(entry.dayPassed()).isFalse();
    });
    assertThat(cargoRepository.findById(caravan.id(), secondUnit.id())).hasValueSatisfying(entry -> {
      assertThat(entry.currentProvisions()).isEqualTo(9);
      assertThat(entry.dayPassed()).isFalse();
    });
  }

  @Test
  void deletesConsumedPerishableUnitImmediatelyWhenConsumptionUsesItUp() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var wagon = wagonRepository.save(CaravanWagon.create(UUID.randomUUID(), caravan.id(), "carro-de-suministros", Instant.parse("2026-01-01T00:00:00Z")));

    travelerRepository.save(CaravanTraveler.create(
        UUID.randomUUID(),
        caravan.id(),
        "Pasajero",
        null,
        List.of("pasajero"),
        List.of("pasajero"),
        "pasajero",
        1,
        TravelerRoleData.empty(),
        wagon.id(),
        null,
        2,
        Instant.parse("2025-12-30T00:00:00Z")));

    var perishableCargo = cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros-perecederos",
        "Suministros Perecederos",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")).withCurrentProvisions(1, true, Instant.parse("2026-01-01T00:00:00Z")));

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-consumption-delete",
        false,
        List.of()));

    assertThat(cargoRepository.findById(caravan.id(), perishableCargo.id())).isEmpty();
  }

  @Test
  void deletesPerishableCargoWhenItsProvisionsReachZero() {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z")));
    var perishableCargo = cargoRepository.save(CaravanCargo.create(
        UUID.randomUUID(),
        caravan.id(),
        CaravanCargoSourceType.CATALOG,
        "suministros-perecederos",
        "Suministros Perecederos",
        "Artículos de mercancía",
        1,
        1,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2025-12-30T00:00:00Z")));

    cargoRepository.save(perishableCargo.withCurrentProvisions(1, true, Instant.parse("2026-01-01T00:00:00Z")));
    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));

    service.execute(caravan.id(), new AdvanceCaravanDayCycleUseCase.AdvanceCaravanDayCycleCommand(
        "day-delete-zero",
        false,
        List.of()));

    assertThat(cargoRepository.findById(caravan.id(), perishableCargo.id())).isEmpty();
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

  private static final class InMemoryTravelerRepository implements CaravanTravelerRepositoryPort {
    private final List<CaravanTraveler> travelers = new ArrayList<>();

    @Override
    public CaravanTraveler save(CaravanTraveler traveler) {
      for (var i = travelers.size() - 1; i >= 0; i--) {
        if (java.util.Objects.equals(travelers.get(i).id(), traveler.id())) {
          travelers.remove(i);
        }
      }
      travelers.add(traveler);
      return traveler;
    }

    @Override
    public List<CaravanTraveler> findAllByCaravanId(UUID caravanId) {
      return travelers.stream().filter(traveler -> traveler.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanTraveler> findById(UUID caravanId, UUID travelerId) {
      for (var i = travelers.size() - 1; i >= 0; i--) {
        var traveler = travelers.get(i);
        if (traveler.caravanId().equals(caravanId) && java.util.Objects.equals(traveler.id(), travelerId)) {
          return Optional.of(traveler);
        }
      }
      return Optional.empty();
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return travelers.stream().filter(traveler -> traveler.caravanId().equals(caravanId) && wagonId.equals(traveler.wagonId())).count();
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
      return List.of();
    }

    @Override
    public List<CaravanBeast> findAllByCaravanIdAndWagonIdAndAssignmentType(UUID caravanId, UUID wagonId, CaravanBeastAssignmentType assignmentType) {
      return List.of();
    }

    @Override
    public Optional<CaravanBeast> findById(UUID caravanId, UUID beastId) {
      return beasts.stream().filter(beast -> beast.caravanId().equals(caravanId) && beast.id().equals(beastId)).findFirst();
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {}
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
      return feats.stream().filter(feat -> feat.caravanId().equals(caravanId) && feat.featTypeCode().equals(featTypeCode)).count();
    }

    @Override
    public void deleteById(UUID caravanId, UUID featId) {}

    @Override
    public void deleteByCaravanId(UUID caravanId) {}
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
    public void deleteByCaravanId(UUID caravanId) {}
  }

  private static final class InMemoryResolutionRepository implements CaravanDayResolutionRepositoryPort {
    private final List<CaravanDayResolution> resolutions = new ArrayList<>();

    @Override
    public CaravanDayResolution save(CaravanDayResolution resolution) {
      resolutions.removeIf(existing -> existing.id().equals(resolution.id()));
      resolutions.add(resolution);
      return resolution;
    }

    @Override
    public Optional<CaravanDayResolution> findByCaravanIdAndIdempotencyKey(UUID caravanId, String idempotencyKey) {
      return resolutions.stream().filter(resolution -> resolution.caravanId().equals(caravanId) && resolution.idempotencyKey().equals(idempotencyKey)).findFirst();
    }

    @Override
    public List<CaravanDayResolution> findAllByCaravanId(UUID caravanId) {
      return resolutions.stream().filter(resolution -> resolution.caravanId().equals(caravanId)).toList();
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {}
  }

  private static final class InMemoryImprovementRepository implements com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort {
    @Override
    public com.gestioncaravana.domain.CaravanWagonImprovement save(com.gestioncaravana.domain.CaravanWagonImprovement improvement) {
      return improvement;
    }

    @Override
    public List<com.gestioncaravana.domain.CaravanWagonImprovement> findAllByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return List.of();
    }

    @Override
    public Optional<com.gestioncaravana.domain.CaravanWagonImprovement> findById(UUID caravanId, UUID wagonId, UUID improvementId) {
      return Optional.empty();
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId, UUID improvementId) {}
  }

  private CaravanCampaign createCookBonusScenario(int cookCount) {
    var caravan = caravanRepository.save(CaravanCampaign.create(UUID.randomUUID(), "Campaign-" + cookCount, null, Instant.parse("2026-01-01T00:00:00Z")));

    for (var i = 0; i < 11; i++) {
      travelerRepository.save(CaravanTraveler.create(
          UUID.randomUUID(),
          caravan.id(),
          "Batidor " + i,
          null,
          List.of("pasajero", "batidor"),
          List.of("batidor"),
          "batidor",
          1,
          TravelerRoleData.empty(),
          null,
          null,
          1,
          Instant.parse("2025-12-30T00:00:00Z")));
    }

    for (var i = 0; i < cookCount; i++) {
      travelerRepository.save(CaravanTraveler.create(
          UUID.randomUUID(),
          caravan.id(),
          "Cocinero " + i,
          null,
          List.of("pasajero", "cocinero"),
          List.of("cocinero"),
          "cocinero",
          1,
          TravelerRoleData.empty(),
          null,
          null,
          1,
          Instant.parse("2025-12-30T00:00:00Z")));
    }

    supplyStateRepository.save(new CaravanSupplyState(caravan.id(), 0, 0, 0, 0, Instant.parse("2026-01-01T00:00:00Z")));
    return caravan;
  }
}
