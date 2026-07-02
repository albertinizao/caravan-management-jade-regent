package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanDailyChoiceView;
import com.gestioncaravana.application.model.CaravanDailyContributionView;
import com.gestioncaravana.application.model.CaravanDayPreviewView;
import com.gestioncaravana.application.model.CaravanDayResolutionView;
import com.gestioncaravana.application.port.in.AdvanceCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.in.PreviewCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanDayResolutionRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.domain.CargoCatalog;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanDayResolution;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.TravelerRoleCatalog;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.WagonImprovementCatalog;
import com.gestioncaravana.domain.WagonType;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CaravanDayCycleService implements PreviewCaravanDayCycleUseCase, AdvanceCaravanDayCycleUseCase {

  private static final String BATIDOR_ROLE = "batidor";
  private static final String AGRICULTURIST_ROLE = "agricultor";
  private static final String COOK_ROLE = "cocinero";
  private static final String SERVANT_ROLE = "sirviente";
  private static final String AUTONOMY_FEAT = "autonomia-extrema";
  private static final String FASTING_FEAT = "ayuno-intermitente";
  private static final String EFFICIENT_CONSUMPTION_FEAT = "consumo-eficiente";
  private static final String PORTABLE_KITCHEN_CODE = "cocina-portatil";
  private static final String SUPPLIES_CODE = "suministros";
  private static final String PERISHABLE_SUPPLIES_CODE = "suministros-perecederos";
  private static final int STANDARD_SUPPLY_VALUE = 10;
  private static final String HUNT_MODE = "HUNT";
  private static final String EXPLORE_MODE = "EXPLORE";

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanWagonImprovementRepositoryPort improvementRepository;
  private final CaravanFeatRepositoryPort featRepository;
  private final CaravanCargoRepositoryPort cargoRepository;
  private final CaravanSupplyStateRepositoryPort supplyStateRepository;
  private final CaravanDayResolutionRepositoryPort resolutionRepository;
  private final GetCaravanStatisticsUseCase statisticsUseCase;
  private final Clock clock;

  public CaravanDayCycleService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort improvementRepository,
      CaravanFeatRepositoryPort featRepository,
      CaravanCargoRepositoryPort cargoRepository,
      CaravanSupplyStateRepositoryPort supplyStateRepository,
      CaravanDayResolutionRepositoryPort resolutionRepository,
      GetCaravanStatisticsUseCase statisticsUseCase,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.travelerRepository = travelerRepository;
    this.wagonRepository = wagonRepository;
    this.improvementRepository = improvementRepository;
    this.featRepository = featRepository;
    this.cargoRepository = cargoRepository;
    this.supplyStateRepository = supplyStateRepository;
    this.resolutionRepository = resolutionRepository;
    this.statisticsUseCase = statisticsUseCase;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanDayPreviewView preview(UUID caravanId, PreviewCaravanDayCycleCommand command) {
    var caravan = requireCaravan(caravanId);
    var state = loadSupplyState(caravan.id(), false);
    var computation = compute(caravan, state, command.fastingEnabled(), toPreviewChoiceMap(command.choices()), false);
    return toPreview(caravan.id(), computation);
  }

  @Override
  public CaravanDayResolutionView execute(UUID caravanId, AdvanceCaravanDayCycleCommand command) {
    if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
      throw new IllegalArgumentException("idempotencyKey is required");
    }

    var existing = resolutionRepository.findByCaravanIdAndIdempotencyKey(caravanId, command.idempotencyKey());
    if (existing.isPresent()) {
      return toView(existing.get());
    }

    var caravan = requireCaravan(caravanId);
    var state = loadSupplyState(caravan.id(), true);
    var computation = compute(caravan, state, command.fastingEnabled(), toChoiceMap(command.choices()), true);
    var now = clock.instant();
    var resolution = new CaravanDayResolution(
        UUID.randomUUID(),
        caravan.id(),
        command.idempotencyKey(),
        computation.dayIndex(),
        now,
        computation.startingReserve(),
        computation.endingReserve(),
        computation.totalConsumption(),
        computation.totalGeneration(),
        computation.netDelta(),
        computation.shortage(),
        computation.choicesSummary(),
        computation.contributionsSummary(),
        computation.warningsSummary());

    supplyStateRepository.save(new CaravanSupplyState(
        caravan.id(),
        computation.generatedEndingReserve(),
        0,
        0,
        state.daysPassed() + 1,
        now));
    return toView(resolutionRepository.save(resolution), computation);
  }

  private CaravanDayComputation compute(
      CaravanCampaign caravan,
      CaravanSupplyState state,
      boolean fastingEnabled,
      Map<UUID, String> batidorModes,
      boolean persistCargoChanges) {
    var statistics = statisticsUseCase.getById(caravan.id());
    var travelers = new ArrayList<>(travelerRepository.findAllByCaravanId(caravan.id()));
    var feats = featRepository.findAllByCaravanId(caravan.id());
    var wagons = wagonRepository.findAllByCaravanId(caravan.id());
    var now = clock.instant();
    var cargo = advancePerishableCargoDayMarker(caravan.id(), cargoRepository.findAllByCaravanId(caravan.id()), now, persistCargoChanges);
    var travelersForDay = advanceServantServiceDays(travelers, persistCargoChanges, now);

    var cargoReserveBefore = cargo.stream().mapToInt(this::cargoProvisions).sum();
    var startingReserve = state.provisionReserve() + cargoReserveBefore;
    var generatedReserveBefore = state.provisionReserve();

    var baseConsumption = statistics.otherStats().consumption();
    var travelerConsumption = travelersForDay.stream()
        .filter(traveler -> !traveler.hasActiveRole(BATIDOR_ROLE))
        .mapToInt(CaravanTraveler::consumption)
        .sum();
    var wagonConsumption = Math.max(0, baseConsumption - travelerConsumption);
    var totalConsumption = wagonConsumption + travelerConsumption;

    var consumptionContributions = new ArrayList<CaravanDailyContributionView>();
    consumptionContributions.add(contribution(
        "consumption",
        "BASE",
        caravan.id().toString(),
        "Caravana",
        "ADD",
        baseConsumption,
        "Consumo base diario calculado a partir de viajeros y carros",
        true,
        null));

    if (fastingEnabled && hasActiveFeat(feats, FASTING_FEAT)) {
      var reducedTravelerConsumption = (int) Math.ceil(travelerConsumption / 2.0);
      consumptionContributions.add(contribution(
          "consumption",
          "FEAT",
          FASTING_FEAT,
          "Ayuno Intermitente",
          "ADD",
          reducedTravelerConsumption - travelerConsumption,
          "Reduce a la mitad el consumo de los viajeros",
          true,
          null));
      totalConsumption = wagonConsumption + reducedTravelerConsumption;
      travelerConsumption = reducedTravelerConsumption;
    }

    if (hasActiveFeat(feats, EFFICIENT_CONSUMPTION_FEAT)) {
      var reduced = Math.max(wagonConsumption, totalConsumption - 2);
      consumptionContributions.add(contribution(
          "consumption",
          "FEAT",
          EFFICIENT_CONSUMPTION_FEAT,
          "Consumo Eficiente",
          "ADD",
          reduced - totalConsumption,
          "Reduce el consumo total en 2 sin bajar del consumo base de carros",
          true,
          null));
      totalConsumption = reduced;
    }

    var generationContributions = new ArrayList<CaravanDailyContributionView>();
    var warnings = new ArrayList<String>();
    var rawGeneration = 0;
    var stagedGeneratedCargo = new ArrayList<CaravanCargo>();
    var autonomy = hasActiveFeat(feats, AUTONOMY_FEAT);
    var servantsByMaster = travelersForDay.stream()
        .filter(traveler -> traveler.hasActiveRole(SERVANT_ROLE))
        .filter(traveler -> traveler.roleSpecificData() != null && traveler.roleSpecificData().servedTravelerId() != null)
        .collect(java.util.stream.Collectors.groupingBy(
            traveler -> traveler.roleSpecificData().servedTravelerId(),
            java.util.stream.Collectors.toList()));

    for (var traveler : travelersForDay) {
      if (traveler.hasActiveRole(AGRICULTURIST_ROLE)) {
        var servants = servantsForMaster(traveler.id(), servantsByMaster);
        if (traveler.roleSpecificData() == null || !traveler.roleSpecificData().generatingFood()) {
          if (persistCargoChanges) {
            travelerRepository.save(traveler.updateDetails(
                null,
                null,
                null,
                null,
                null,
                traveler.maxActiveRoleCount(),
                generatingFoodData(traveler, true),
                traveler.wagonId(),
                traveler.contract(),
                traveler.consumption(),
                now));
          }
          generationContributions.add(contribution(
              "generation",
              "TRAVELER",
              traveler.id().toString(),
              traveler.fullName(),
              "ADD",
              0,
              "El agricultor deja la siguiente unidad preparada para el día siguiente",
              true,
              null));
          continue;
        }

        var placementCargo = new ArrayList<CaravanCargo>(cargo);
        placementCargo.addAll(stagedGeneratedCargo);
        var generatedQuantity = 1 + farmerHelperBonusSupplies(traveler, AGRICULTURIST_ROLE, servants);
        var generatedCargo = createAndPlaceSupplyForTraveler(
            caravan.id(),
            wagons,
            placementCargo,
            now,
            generatedQuantity);
        if (persistCargoChanges) {
          travelerRepository.save(traveler.updateDetails(
              null,
              null,
              null,
              null,
              null,
              traveler.maxActiveRoleCount(),
              generatingFoodData(traveler, false),
              traveler.wagonId(),
              traveler.contract(),
              traveler.consumption(),
              now));
        }

        if (generatedCargo == null) {
          generationContributions.add(contribution(
              "generation",
              "TRAVELER",
              traveler.id().toString(),
              traveler.fullName(),
              "ADD",
              0,
              "El agricultor genera una unidad de suministros",
              false,
              "No hay hueco para almacenar la unidad de suministros"));
          warnings.add(generatedQuantity == 1
              ? "Se pierde 1 unidad de suministros por no tener hueco."
              : "Se pierden " + generatedQuantity + " unidades de suministros por no tener hueco.");
          continue;
        }

        stagedGeneratedCargo.add(generatedCargo);
        var amount = cargoProvisions(generatedCargo);
        generationContributions.add(contribution(
              "generation",
              "TRAVELER",
              traveler.id().toString(),
              traveler.fullName(),
              "ADD",
              amount,
              farmerContributionReason(servants, generatedQuantity),
              true,
              null));
      } else if (traveler.hasActiveRole(BATIDOR_ROLE)) {
        var mode = batidorModes.getOrDefault(traveler.id(), HUNT_MODE);
        if (EXPLORE_MODE.equalsIgnoreCase(mode)) {
          generationContributions.add(contribution(
              "generation",
              "TRAVELER",
              traveler.id().toString(),
              traveler.fullName(),
              "ADD",
              0,
              "El batidor se centró en explorar",
              true,
              null));
          continue;
        }

        var amount = (autonomy ? 1 : 2) + servantBoost(traveler.id(), servantsByMaster);
        rawGeneration += amount;
        generationContributions.add(contribution(
            "generation",
            "TRAVELER",
            traveler.id().toString(),
            traveler.fullName(),
            "ADD",
            amount,
            autonomy ? "Autonomía Extrema reduce la producción del batidor" : "Batidor en modo caza",
            true,
            null));
      }
    }

    var cookTravelersForDay = travelersForDay.stream()
        .filter(traveler -> traveler.hasActiveRole(COOK_ROLE))
        .toList();
    var cookCount = travelersForDay.stream().filter(traveler -> traveler.hasActiveRole(COOK_ROLE)).count();
    var portableKitchenCount = (int) cargo.stream().filter(entry -> PORTABLE_KITCHEN_CODE.equals(entry.catalogCode())).count();
    var cookGenerationBonus = calculateCookBonus(rawGeneration, cookTravelersForDay, servantsByMaster, portableKitchenCount);
    var cookCargoBonus = calculateCookBonus(cargo.stream().mapToInt(this::cargoProvisions).sum(), cookTravelersForDay, servantsByMaster, portableKitchenCount);

    var totalGeneration = rawGeneration + cookGenerationBonus + cookCargoBonus;
    if (cookGenerationBonus > 0) {
      generationContributions.add(contribution(
          "generation",
          "ROLE",
          COOK_ROLE,
          "Cocineros",
          "ADD",
          cookGenerationBonus,
          portableKitchenCount > 0
              ? "La Cocina Portátil duplica el rendimiento del cocinero asignado"
              : "Cada cocinero aprovecha un bloque completo de 10 suministros para aportar 5 extra",
          true,
          null));
    }
    if (cookCargoBonus > 0) {
      generationContributions.add(contribution(
          "generation",
          "ROLE",
          COOK_ROLE,
          "Cocineros",
          "ADD",
          cookCargoBonus,
          portableKitchenCount > 0
              ? "La Cocina Portátil duplica el rendimiento del cocinero asignado sobre los suministros consumidos"
              : "El cocinero mejora los suministros almacenados al gastarse",
          true,
          null));
    }

    if (fastingEnabled && !hasActiveFeat(feats, FASTING_FEAT)) {
      throw new IllegalArgumentException("Ayuno Intermitente solo está disponible si la caravana tiene la dote activa");
    }

    consumeCargoForShortage(
        caravan.id(),
        cargo,
        generatedReserveBefore,
        totalGeneration,
        totalConsumption,
        now,
        generationContributions,
        persistCargoChanges);

    if (portableKitchenCount > 0 && cookCount == 0) {
      warnings.add("La Cocina Portátil está presente, pero no hay cocineros asignados.");
    }

    var netDelta = totalGeneration - totalConsumption;
    var endingReserveRaw = startingReserve + netDelta;
    var shortage = Math.max(0, totalConsumption - startingReserve - totalGeneration);
    var endingReserve = Math.max(0, endingReserveRaw);
    if (persistCargoChanges) {
      for (var generatedCargo : stagedGeneratedCargo) {
        cargoRepository.save(generatedCargo);
      }
    }

    var generatedEndingReserve = Math.max(0, generatedReserveBefore + totalGeneration - totalConsumption);

    var choiceViews = choicesToViews(batidorModes, fastingEnabled);
    var contributions = new ArrayList<CaravanDailyContributionView>();
    contributions.addAll(consumptionContributions);
    contributions.addAll(generationContributions);
    contributions.sort(Comparator.comparing(CaravanDailyContributionView::effectCode).thenComparing(CaravanDailyContributionView::sourceName));

    return new CaravanDayComputation(
        state.daysPassed() + 1,
        startingReserve,
        totalConsumption,
        totalGeneration,
        netDelta,
        endingReserve,
        shortage,
        generatedEndingReserve,
        0,
        0,
        choiceViews,
        contributions,
        warnings,
        join(choiceViews.stream().map(String::valueOf).toList()),
        join(contributions.stream().map(String::valueOf).toList()),
        join(warnings));
  }

  private int servantBoost(UUID travelerId, Map<UUID, List<CaravanTraveler>> servantsByMaster) {
    return servantsForMaster(travelerId, servantsByMaster).isEmpty() ? 0 : 1;
  }

  private List<CaravanTraveler> advanceServantServiceDays(List<CaravanTraveler> travelers, boolean persistChanges, java.time.Instant now) {
    var updatedTravelers = new ArrayList<CaravanTraveler>(travelers.size());
    for (var traveler : travelers) {
      if (traveler.hasActiveRole(SERVANT_ROLE)
          && traveler.roleSpecificData() != null
          && traveler.roleSpecificData().servedTravelerId() != null) {
        var updatedRoleData = traveler.roleSpecificData().incrementDaysServing();
        var updatedTraveler = traveler.withRoleSpecificData(updatedRoleData, now);
        if (persistChanges) {
          travelerRepository.save(updatedTraveler);
        }
        updatedTravelers.add(updatedTraveler);
      } else {
        updatedTravelers.add(traveler);
      }
    }
    return updatedTravelers;
  }

  private com.gestioncaravana.domain.TravelerRoleData generatingFoodData(CaravanTraveler traveler, boolean generatingFood) {
    var roleData = traveler.roleSpecificData() == null
        ? com.gestioncaravana.domain.TravelerRoleData.empty()
        : traveler.roleSpecificData();
    return roleData.withGeneratingFood(generatingFood);
  }

  private int calculateCookBonus(
      int baseValue,
      List<CaravanTraveler> cookTravelers,
      Map<UUID, List<CaravanTraveler>> servantsByMaster,
      int portableKitchenCount) {
    if (baseValue <= 0 || cookTravelers.isEmpty()) {
      return 0;
    }

    var availableBlocks = baseValue / STANDARD_SUPPLY_VALUE;
    if (availableBlocks <= 0) {
      return 0;
    }

    var candidates = cookTravelers.stream()
        .map(cook -> {
          var servants = servantsForMaster(cook.id(), servantsByMaster);
          var baseBonus = cookBonusFor(masterBonusMultiplier(cook, COOK_ROLE, servants), false);
          return new CookBonusCandidate(cook, baseBonus, servants);
        })
        .sorted(Comparator.comparingInt(CookBonusCandidate::baseBonus).reversed()
            .thenComparing(candidate -> candidate.cook().fullName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(candidate -> candidate.cook().id()))
        .limit(availableBlocks)
        .toList();

    var assignedKitchenCookIds = candidates.stream()
        .limit(Math.min(portableKitchenCount, candidates.size()))
        .map(candidate -> candidate.cook().id())
        .collect(java.util.stream.Collectors.toSet());

    var bonus = 0;
    for (var candidate : candidates) {
      bonus += assignedKitchenCookIds.contains(candidate.cook().id())
          ? candidate.baseBonus() * 2
          : candidate.baseBonus();
    }
    return bonus;
  }

  private int farmerHelperBonusSupplies(CaravanTraveler master, String masterRoleCode, List<CaravanTraveler> servants) {
    if (servants.isEmpty()) {
      return 0;
    }
    return servants.stream()
        .mapToInt(servant -> periodicHelperBonus(master, servant, masterRoleCode))
        .sum();
  }

  private String farmerContributionReason(List<CaravanTraveler> servants, int generatedQuantity) {
    if (servants.isEmpty()) {
      return "El agricultor genera una unidad de suministros";
    }
    if (generatedQuantity > 1) {
      return "El agricultor genera " + generatedQuantity + " unidades de suministros con ayuda de " + (servants.size() == 1 ? "su sirviente" : "sus sirvientes");
    }
    return "El agricultor genera una unidad de suministros con ayuda de su sirviente";
  }

  private double masterBonusMultiplier(CaravanTraveler master, String masterRoleCode, List<CaravanTraveler> servants) {
    if (master.hasActiveRole(SERVANT_ROLE) || SERVANT_ROLE.equals(masterRoleCode)) {
      return 0.0;
    }
    var role = TravelerRoleCatalog.findByCode(masterRoleCode).orElse(null);
    if (role == null || role.helperBenefitMode() != com.gestioncaravana.domain.TravelerRoleHelperBenefitMode.DAILY) {
      return 0.0;
    }
    return servants.stream()
        .mapToDouble(servant -> servantCanExerciseRole(servant, masterRoleCode) ? 1.0 : 0.5)
        .sum();
  }

  private int cookBonusFor(double multiplier, boolean hasPortableKitchen) {
    var totalMultiplier = (hasPortableKitchen ? 2.0 : 1.0) * (1.0 + multiplier);
    return (int) Math.round(5 * totalMultiplier);
  }

  private int periodicHelperBonus(CaravanTraveler master, CaravanTraveler servant, String masterRoleCode) {
    if (master.hasActiveRole(SERVANT_ROLE) || SERVANT_ROLE.equals(masterRoleCode)) {
      return 0;
    }
    var role = TravelerRoleCatalog.findByCode(masterRoleCode).orElse(null);
    if (role == null || role.helperBenefitMode() != com.gestioncaravana.domain.TravelerRoleHelperBenefitMode.PERIODIC) {
      return 0;
    }
    var roleData = servant.roleSpecificData();
    if (roleData == null || roleData.daysServing() <= 0) {
      return 0;
    }

    var effectivePeriod = servantCanExerciseRole(servant, masterRoleCode)
        ? Math.max(1, role.helperPeriodDays() / 2)
        : role.helperPeriodDays();
    return roleData.daysServing() % effectivePeriod == 0 ? 1 : 0;
  }

  private boolean servantCanExerciseRole(CaravanTraveler servant, String roleCode) {
    return servant.availableRoleCodes().contains(roleCode);
  }

  private record CookBonusCandidate(CaravanTraveler cook, int baseBonus, List<CaravanTraveler> servants) {}

  private List<CaravanTraveler> servantsForMaster(UUID travelerId, Map<UUID, List<CaravanTraveler>> servantsByMaster) {
    return servantsByMaster.getOrDefault(travelerId, List.of());
  }

  private int consumeCargoForShortage(
      UUID caravanId,
      List<CaravanCargo> cargo,
      int startingReserve,
      int totalGeneration,
      int totalConsumption,
      java.time.Instant now,
      List<CaravanDailyContributionView> generationContributions,
      boolean persistChanges) {
    var deficit = Math.max(0, totalConsumption - startingReserve - totalGeneration);
    if (deficit <= 0) {
      return 0;
    }

    var converted = 0;
    converted += consumeCargoType(caravanId, cargo, PERISHABLE_SUPPLIES_CODE, deficit, now, generationContributions, persistChanges);
    deficit = Math.max(0, deficit - converted);
    if (deficit > 0) {
      converted += consumeCargoType(caravanId, cargo, SUPPLIES_CODE, deficit, now, generationContributions, persistChanges);
    }
    return converted;
  }

  private int consumeCargoType(
      UUID caravanId,
      List<CaravanCargo> cargo,
      String catalogCode,
      int deficit,
      java.time.Instant now,
      List<CaravanDailyContributionView> generationContributions,
      boolean persistChanges) {
    if (deficit <= 0) {
      return 0;
    }

    var entries = cargo.stream()
        .filter(entry -> catalogCode.equals(entry.catalogCode()))
        .sorted(Comparator.comparing((CaravanCargo entry) -> isStarted(entry)).reversed()
            .thenComparing(CaravanCargo::updatedAt)
            .thenComparing(CaravanCargo::id))
        .toList();

    var converted = 0;
    var remainingDeficit = deficit;
    for (var entry : entries) {
      if (remainingDeficit <= 0) {
        break;
      }

      var availableProvisions = cargoProvisions(entry);
      if (availableProvisions <= 0) {
        break;
      }

      var consumedProvisions = Math.min(availableProvisions, remainingDeficit);
      if (consumedProvisions <= 0) {
        break;
      }

      var produced = consumedProvisions;
      converted += produced;
      remainingDeficit -= produced;
      generationContributions.add(contribution(
          "generation",
          "CARGO",
          entry.id().toString(),
          entry.displayName(),
          "ADD",
          produced,
          catalogCode.equals(PERISHABLE_SUPPLIES_CODE)
              ? "Los suministros perecederos se convierten en provisiones para cubrir el consumo del día"
              : "Los suministros se convierten en provisiones para cubrir el consumo del día",
          true,
          null));

      var remainingProvisions = availableProvisions - consumedProvisions;
      if (persistChanges) {
        if (remainingProvisions <= 0) {
          cargoRepository.deleteById(caravanId, entry.id());
        } else {
          cargoRepository.save(entry.withCurrentProvisions(remainingProvisions, entry.dayPassed(), now));
        }
      }
    }

    return converted;
  }

  private boolean isStarted(CaravanCargo entry) {
    return cargoProvisions(entry) < initialCargoProvisions(entry);
  }

  private List<CaravanCargo> advancePerishableCargoDayMarker(UUID caravanId, List<CaravanCargo> cargo, java.time.Instant now, boolean persistChanges) {
    var normalized = new ArrayList<CaravanCargo>(cargo.size());
    for (var entry : cargo) {
      var normalizedEntry = entry;
      var currentProvisions = cargoProvisions(entry);

      if (PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode())) {
        if (currentProvisions <= 0) {
          if (persistChanges) {
            cargoRepository.deleteById(caravanId, entry.id());
          }
          continue;
        }

        var dayPassed = Boolean.TRUE.equals(entry.dayPassed());
        if (dayPassed) {
          var remainingProvisions = Math.max(0, currentProvisions - entry.quantity());
          if (remainingProvisions <= 0) {
            if (persistChanges) {
              cargoRepository.deleteById(caravanId, entry.id());
            }
            continue;
          }
          normalizedEntry = entry.withCurrentProvisions(remainingProvisions, false, now);
        } else {
          normalizedEntry = entry.withDayPassed(true, now);
        }
      } else if (entry.currentProvisions() == null && SUPPLIES_CODE.equals(entry.catalogCode())) {
        normalizedEntry = entry.withCurrentProvisions(currentProvisions, entry.dayPassed(), now);
      }

      if (persistChanges && normalizedEntry != entry) {
        cargoRepository.save(normalizedEntry);
      }
      normalized.add(normalizedEntry);
    }
    return normalized;
  }

  private int cargoProvisions(CaravanCargo entry) {
    return entry.currentProvisions() == null ? initialCargoProvisions(entry) : entry.currentProvisions();
  }

  private int initialCargoProvisions(CaravanCargo entry) {
    if (SUPPLIES_CODE.equals(entry.catalogCode()) || PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode())) {
      return entry.quantity() * STANDARD_SUPPLY_VALUE;
    }
    return entry.currentProvisions() == null ? 0 : entry.currentProvisions();
  }

  private CaravanCargo createAndPlaceSupplyForTraveler(
      UUID caravanId,
      List<CaravanWagon> wagons,
      List<CaravanCargo> cargo,
      java.time.Instant now,
      int generatedQuantity) {
    var wagon = selectWagonForGeneratedSupply(caravanId, wagons, cargo);
    if (wagon == null) {
      return null;
    }

    var catalogItem = CargoCatalog.findByCode(SUPPLIES_CODE)
        .orElseThrow(() -> new IllegalStateException("Unknown cargo catalog entry: " + SUPPLIES_CODE));
    var generatedCargo = CaravanCargo.create(
        UUID.randomUUID(),
        caravanId,
        com.gestioncaravana.domain.CaravanCargoSourceType.CATALOG,
        catalogItem.code(),
        catalogItem.name(),
        catalogItem.category(),
        generatedQuantity,
        catalogItem.resolvedDefaultCargoUnits(),
        wagon.id(),
        null,
        null,
        null,
        null,
        now);
    return generatedCargo;
  }

  private CaravanWagon selectWagonForGeneratedSupply(UUID caravanId, List<CaravanWagon> wagons, List<CaravanCargo> cargo) {
    return wagons.stream()
        .filter(wagon -> isSupplyWagon(wagon))
        .filter(wagon -> hasCargoCapacity(caravanId, wagon, cargo, 1))
        .findFirst()
        .orElseGet(() -> wagons.stream()
            .filter(wagon -> hasCargoCapacity(caravanId, wagon, cargo, 1))
            .findFirst()
            .orElse(null));
  }

  private boolean isSupplyWagon(CaravanWagon wagon) {
    return "carro-de-suministros".equals(wagon.wagonTypeCode());
  }

  private boolean hasCargoCapacity(UUID caravanId, CaravanWagon wagon, List<CaravanCargo> cargo, int additionalCargoUnits) {
    var wagonType = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
    var currentUsed = cargo.stream()
        .filter(entry -> wagon.id().equals(entry.wagonId()))
        .mapToInt(entry -> entry.quantity() * entry.cargoUnits())
        .sum();
    var capacity = deriveCargoCapacity(caravanId, wagon, wagonType);
    return currentUsed + additionalCargoUnits <= capacity;
  }

  private int deriveCargoCapacity(UUID caravanId, CaravanWagon wagon, WagonType wagonType) {
    var currentCargoCapacity = wagonType.cargoCapacity();
    for (var improvement : improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagon.id())) {
      var type = WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
          .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode()));
      if (type.cargoCapacityOverride() != null) {
        currentCargoCapacity = type.cargoCapacityOverride();
      } else if (type.cargoCapacityMultiplier() != null) {
        var minIncrement = type.cargoCapacityMinimumIncrement() == null ? 0 : type.cargoCapacityMinimumIncrement();
        currentCargoCapacity += Math.max(minIncrement, (int) Math.round(currentCargoCapacity * (type.cargoCapacityMultiplier() - 1)));
      } else if (type.cargoCapacityBonus() != null) {
        currentCargoCapacity += type.cargoCapacityBonus();
      }
    }
    return Math.max(0, currentCargoCapacity);
  }

  private CaravanSupplyState loadSupplyState(UUID caravanId, boolean persistIfMissing) {
    return supplyStateRepository.findByCaravanId(caravanId)
        .orElseGet(() -> {
          var initial = new CaravanSupplyState(caravanId, 0, 0, 0, 0, clock.instant());
          return persistIfMissing ? supplyStateRepository.save(initial) : initial;
        });
  }

  private CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private boolean hasActiveFeat(List<CaravanFeat> feats, String featTypeCode) {
    return feats.stream().anyMatch(feat -> feat.active() && feat.featTypeCode().equals(featTypeCode));
  }

  private Map<UUID, String> toChoiceMap(List<AdvanceCaravanDayCycleUseCase.CaravanDailyChoiceCommand> choices) {
    var result = new HashMap<UUID, String>();
    if (choices == null) {
      return result;
    }
    for (var choice : choices) {
      if (choice != null && choice.travelerId() != null && choice.mode() != null && !choice.mode().isBlank()) {
        result.put(choice.travelerId(), choice.mode().trim().toUpperCase());
      }
    }
    return result;
  }

  private Map<UUID, String> toPreviewChoiceMap(List<PreviewCaravanDayCycleUseCase.CaravanDailyChoiceCommand> choices) {
    var result = new HashMap<UUID, String>();
    if (choices == null) {
      return result;
    }
    for (var choice : choices) {
      if (choice != null && choice.travelerId() != null && choice.mode() != null && !choice.mode().isBlank()) {
        result.put(choice.travelerId(), choice.mode().trim().toUpperCase());
      }
    }
    return result;
  }

  private List<CaravanDailyChoiceView> choicesToViews(Map<UUID, String> batidorModes, boolean fastingEnabled) {
    var result = new ArrayList<CaravanDailyChoiceView>();
    if (fastingEnabled) {
      result.add(new CaravanDailyChoiceView(null, "FASTING"));
    }
    batidorModes.forEach((travelerId, mode) -> result.add(new CaravanDailyChoiceView(travelerId, mode)));
    return result;
  }

  private CaravanDailyContributionView contribution(
      String effectCode,
      String sourceType,
      String sourceId,
      String sourceName,
      String operation,
      int quantity,
      String reason,
      boolean applied,
      String ignoredReason) {
    return new CaravanDailyContributionView(effectCode, sourceType, sourceId, sourceName, operation, quantity, reason, applied, ignoredReason);
  }

  private String join(List<String> items) {
    return String.join("\n", items);
  }

  private CaravanDayPreviewView toPreview(UUID caravanId, CaravanDayComputation computation) {
    return new CaravanDayPreviewView(
        caravanId,
        computation.dayIndex(),
        computation.startingReserve(),
        computation.totalConsumption(),
        computation.totalGeneration(),
        computation.netDelta(),
        computation.endingReserve(),
        computation.shortage(),
        computation.warnings(),
        computation.choices(),
        computation.contributions());
  }

  private CaravanDayResolutionView toView(CaravanDayResolution resolution, CaravanDayComputation computation) {
    return new CaravanDayResolutionView(
        resolution.id(),
        resolution.caravanId(),
        resolution.idempotencyKey(),
        resolution.resolvedDayIndex(),
        resolution.resolvedAt(),
        resolution.startingReserve(),
        resolution.endingReserve(),
        resolution.totalConsumption(),
        resolution.totalGeneration(),
        resolution.netDelta(),
        resolution.shortage(),
        computation.choices(),
        computation.contributions(),
        computation.warnings());
  }

  private CaravanDayResolutionView toView(CaravanDayResolution resolution) {
    return new CaravanDayResolutionView(
        resolution.id(),
        resolution.caravanId(),
        resolution.idempotencyKey(),
        resolution.resolvedDayIndex(),
        resolution.resolvedAt(),
        resolution.startingReserve(),
        resolution.endingReserve(),
        resolution.totalConsumption(),
        resolution.totalGeneration(),
        resolution.netDelta(),
        resolution.shortage(),
        List.of(),
        List.of(),
        splitLines(resolution.warningsSummary()));
  }

  private List<String> splitLines(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    return List.of(text.split("\\R"));
  }

  private record CaravanDayComputation(
      int dayIndex,
      int startingReserve,
      int totalConsumption,
      int totalGeneration,
      int netDelta,
      int endingReserve,
      int shortage,
      int generatedEndingReserve,
      int endingStandardReserve,
      int endingPerishableReserve,
      List<CaravanDailyChoiceView> choices,
      List<CaravanDailyContributionView> contributions,
      List<String> warnings,
      String choicesSummary,
      String contributionsSummary,
      String warningsSummary) {}
}
