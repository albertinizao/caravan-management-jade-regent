package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanDailyChoiceView;
import com.gestioncaravana.application.model.CaravanDailyContributionView;
import com.gestioncaravana.application.model.CaravanDayPreviewView;
import com.gestioncaravana.application.model.CaravanDayResolutionView;
import com.gestioncaravana.application.model.CaravanSupplyConsumptionView;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private static final String TEAMWORK_FEAT = "trabajo-en-equipo";
  private static final String AUTONOMY_FEAT = "autonomia-extrema";
  private static final String FASTING_FEAT = "ayuno-intermitente";
  private static final String EFFICIENT_CONSUMPTION_FEAT = "consumo-eficiente";
  private static final int TEAMWORK_MAX_TRAVELERS = 3;
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
        computation.cargoMovementSummary(),
        computation.choicesSummary(),
        computation.contributionsSummary(),
        computation.warningsSummary());

    supplyStateRepository.save(new CaravanSupplyState(
        caravan.id(),
        computation.generatedEndingReserve(),
        0,
        0,
        state.daysPassed() + 1,
        now,
        computation.sharedJobProductivityState()));
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
    var cargoMovementSummary = new CargoMovementSummaryBuilder();
    var generatedProvisions = 0;

    var startingReserve = state.provisionReserve();
    var generatedReserveBefore = state.provisionReserve();
    var displayCurrentReserve = reserveSupplyCargoQuantity(cargo);
    var initialProvisionsInConsumption = openedSupplyConsumptionViews(cargo, List.of(), java.util.Set.of());

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
        "provisiones",
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
          "provisiones",
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
          "provisiones",
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
    var teamworkActive = hasActiveFeat(feats, TEAMWORK_FEAT);
    var sharedJobProductivityState = SharedJobProductivityTracker.parse(state.sharedJobProductivityState());
    if (!teamworkActive) {
      sharedJobProductivityState.clear();
    }
    var farmerBaseGeneration = BigDecimal.ZERO;
    var farmerContributorIds = new ArrayList<UUID>();
    var batidorBaseGeneration = BigDecimal.ZERO;
    var batidorContributorIds = new ArrayList<UUID>();
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
              "Agricultor",
              "ADD",
              0,
              "cargas de suministros",
              "El agricultor deja la siguiente carga preparada para el día siguiente",
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
              "Agricultor",
              "ADD",
              0,
              "cargas de suministros",
              "El agricultor genera una carga de suministros",
              false,
              "No hay hueco para almacenar la unidad de suministros"));
          warnings.add(generatedQuantity == 1
              ? "Se pierde 1 carga de suministros por no tener hueco."
              : "Se pierden " + generatedQuantity + " cargas de suministros por no tener hueco.");
          continue;
        }

        stagedGeneratedCargo.add(generatedCargo);
        var amount = cargoProvisions(generatedCargo);
        generatedProvisions += generatedQuantity;
        farmerBaseGeneration = farmerBaseGeneration.add(BigDecimal.valueOf(amount));
        farmerContributorIds.add(traveler.id());
        generationContributions.add(contribution(
              "generation",
              "TRAVELER",
              traveler.id().toString(),
              traveler.fullName(),
              "Agricultor",
              "ADD",
              generatedQuantity,
              "cargas de suministros",
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
              "Batidor",
              "ADD",
              0,
              "de comida",
              "El batidor se centró en explorar",
              true,
              null));
          continue;
        }

        var amount = (autonomy ? 1 : 2) + servantBoost(traveler.id(), servantsByMaster);
        rawGeneration += amount;
        batidorBaseGeneration = batidorBaseGeneration.add(BigDecimal.valueOf(amount));
        batidorContributorIds.add(traveler.id());
        generationContributions.add(contribution(
            "generation",
            "TRAVELER",
            traveler.id().toString(),
            traveler.fullName(),
            "Batidor",
            "ADD",
            amount,
            "de comida",
            autonomy ? "Autonomía Extrema reduce la producción del batidor" : "Batidor en modo caza",
            true,
            null));
      }
    }

    if (teamworkActive && batidorBaseGeneration.compareTo(BigDecimal.ZERO) > 0) {
      var teamworkBonus = sharedJobProductivityState.apply(BATIDOR_ROLE, batidorContributorIds, batidorBaseGeneration);
      if (teamworkBonus.compareTo(BigDecimal.ZERO) > 0) {
        rawGeneration += teamworkBonus.intValueExact();
        generationContributions.add(contribution(
            "generation",
            "FEAT",
            TEAMWORK_FEAT,
            "Trabajo En Equipo",
            "Batidor",
            "ADD",
            teamworkBonus.intValueExact(),
            "de comida",
            batidorTeamworkReason(batidorContributorIds.size()),
            true,
            null));
      }
    }

    var cookTravelersForDay = travelersForDay.stream()
        .filter(traveler -> traveler.hasActiveRole(COOK_ROLE))
        .toList();
    var cookCount = travelersForDay.stream().filter(traveler -> traveler.hasActiveRole(COOK_ROLE)).count();
    var cookGeneratedCargo = new ArrayList<CaravanCargo>();
    for (var cook : cookTravelersForDay) {
      cookGeneratedCargo.add(createUnassignedSupplyForCook(caravan.id(), now));
    }
    var cookGeneratedCargoIds = cookGeneratedCargo.stream().map(CaravanCargo::id).collect(java.util.stream.Collectors.toUnmodifiableSet());

    var cargoAvailableForCooking = new ArrayList<CaravanCargo>(cargo);
    cargoAvailableForCooking.addAll(stagedGeneratedCargo);
    cargoAvailableForCooking.addAll(cookGeneratedCargo);
    var cookableSupplyEntries = cargoAvailableForCooking.stream()
        .filter(this::isCookableSupply)
        .sorted(Comparator.comparing((CaravanCargo entry) -> entry.wagonId() == null)
            .reversed()
            .thenComparing(CaravanCargo::updatedAt)
            .thenComparing(CaravanCargo::id))
        .toList();
    var portableKitchenCount = cargoAvailableForCooking.stream()
        .filter(entry -> PORTABLE_KITCHEN_CODE.equals(entry.catalogCode()))
        .mapToInt(CaravanCargo::quantity)
        .sum();
    var cookBonusBreakdown = calculateCookBonus(
        cookableSupplyEntries.stream().mapToInt(this::cargoProvisions).sum(),
        cookTravelersForDay,
        servantsByMaster,
        portableKitchenCount,
        "Convierte 1 unidad de suministros en 15 de comida",
        "Convierte 1 unidad de suministros en 20 de comida");
    var cookBaseOutput = cookBonusBreakdown.totalOutputBeforeTeamwork();

    applyCookedSupplyState(
        cookableSupplyEntries,
        cookBonusBreakdown.contributions(),
        cargo,
        stagedGeneratedCargo,
        cookGeneratedCargo,
        now,
        persistCargoChanges);

    if (!cookBonusBreakdown.contributions().isEmpty()) {
      generationContributions.addAll(cookBonusBreakdown.contributions());
    }

    var cookTeamworkBonus = 0;
    if (teamworkActive && cookBaseOutput > 0) {
      var teamworkBonus = sharedJobProductivityState.apply(
          COOK_ROLE,
          cookTravelersForDay.stream().map(CaravanTraveler::id).toList(),
          BigDecimal.valueOf(cookBaseOutput));
      if (teamworkBonus.compareTo(BigDecimal.ZERO) > 0) {
        cookTeamworkBonus = teamworkBonus.intValueExact();
        generationContributions.add(contribution(
            "generation",
            "FEAT",
            TEAMWORK_FEAT,
            "Trabajo En Equipo",
            "Cocinero",
            "ADD",
            teamworkBonus.intValueExact(),
            "de comida",
            cookTeamworkReason(cookTravelersForDay.size()),
            true,
            null));
      }
    }

    if (teamworkActive && farmerBaseGeneration.compareTo(BigDecimal.ZERO) > 0) {
      var teamworkBonus = sharedJobProductivityState.apply(AGRICULTURIST_ROLE, farmerContributorIds, farmerBaseGeneration);
      if (teamworkBonus.compareTo(BigDecimal.ZERO) > 0) {
        var teamworkCargoQuantity = teamworkBonus
            .divide(BigDecimal.valueOf(STANDARD_SUPPLY_VALUE), 0, RoundingMode.FLOOR)
            .intValueExact();
        if (teamworkCargoQuantity <= 0) {
          generationContributions.add(contribution(
              "generation",
              "FEAT",
              TEAMWORK_FEAT,
              "Trabajo En Equipo",
              "Agricultor",
              "ADD",
              teamworkBonus.intValueExact(),
              "provisiones",
              farmerTeamworkReason(farmerContributorIds.size()),
              true,
              null));
        } else {
          var placementCargo = new ArrayList<CaravanCargo>(cargo);
          placementCargo.addAll(stagedGeneratedCargo);
          var generatedCargo = createAndPlaceSupplyForTraveler(
              caravan.id(),
              wagons,
              placementCargo,
              now,
              teamworkCargoQuantity);
          if (generatedCargo == null) {
            generationContributions.add(contribution(
                "generation",
                "FEAT",
                TEAMWORK_FEAT,
                "Trabajo En Equipo",
                "Agricultor",
                "ADD",
                0,
                "provisiones",
                farmerTeamworkReason(farmerContributorIds.size()),
                false,
                "No hay hueco para almacenar la producción adicional del equipo"));
            warnings.add(teamworkCargoQuantity == 1
                ? "Se pierde 1 unidad adicional por falta de hueco para el trabajo en equipo."
                : "Se pierden " + teamworkCargoQuantity + " unidades adicionales por falta de hueco para el trabajo en equipo.");
          } else {
            stagedGeneratedCargo.add(generatedCargo);
            generatedProvisions += teamworkCargoQuantity;
            generationContributions.add(contribution(
                "generation",
                "FEAT",
                TEAMWORK_FEAT,
                "Trabajo En Equipo",
                "Agricultor",
                "ADD",
                teamworkCargoQuantity,
                "cargas de suministros",
                farmerTeamworkReason(farmerContributorIds.size()),
                true,
                null));
          }
        }
      }
    }

    if (fastingEnabled && !hasActiveFeat(feats, FASTING_FEAT)) {
      throw new IllegalArgumentException("Ayuno Intermitente solo está disponible si la caravana tiene la dote activa");
    }

    var foodBeforeLateSupply = rawGeneration + cookBaseOutput + cookTeamworkBonus;
    var deficitBeforeGeneratedCargo = Math.max(0, totalConsumption - foodBeforeLateSupply);
    var cookFoodConsumed = consumeGeneratedCargoForShortage(
        caravan.id(),
        cookGeneratedCargo,
        deficitBeforeGeneratedCargo,
        now,
        generationContributions,
        persistCargoChanges,
        false,
        false,
        false,
        cargoMovementSummary,
        "Los suministros generados por los cocineros se usan para cubrir el consumo del día");

    var deficitAfterCookGeneratedCargo = deficitBeforeGeneratedCargo;
    var openedStoredCargoConsumed = consumeCargoPoolForShortage(
          caravan.id(),
          cargo,
          deficitAfterCookGeneratedCargo,
          now,
          generationContributions,
          persistCargoChanges,
          entry -> isOpenedCargo(entry) && !cookGeneratedCargoIds.contains(entry.id()),
          false,
          true,
          cargoMovementSummary,
          "Los suministros almacenados ya abiertos se consumen para cubrir el consumo del día");

    var deficitAfterOpenedStoredCargo = Math.max(0, deficitAfterCookGeneratedCargo - openedStoredCargoConsumed);
    var generatedCargoConsumed = consumeGeneratedCargoForShortage(
        caravan.id(),
        stagedGeneratedCargo,
        deficitAfterOpenedStoredCargo,
        now,
        generationContributions,
        persistCargoChanges,
        true,
        true,
        true,
        cargoMovementSummary,
        "Los suministros generados por los agricultores se usan para cubrir el consumo del día");

    var deficitAfterGeneratedCargo = Math.max(0, deficitAfterOpenedStoredCargo - generatedCargoConsumed);
    consumeCargoPoolForShortage(
        caravan.id(),
        cargo,
        deficitAfterGeneratedCargo,
        now,
        generationContributions,
        persistCargoChanges,
        this::isUnopenedCargo,
        true,
        true,
        cargoMovementSummary,
        "Los suministros almacenados se abren para cubrir el consumo del día");

    if (portableKitchenCount > 0 && cookCount == 0) {
      warnings.add("La Cocina Portátil está presente, pero no hay cocineros asignados.");
    }

    var lateSupplyLoadsConsumed = cargoMovementSummary.consumedLoads();
    var lateSupplyFoodGenerated = lateSupplyLoadsConsumed * STANDARD_SUPPLY_VALUE;
    var totalGeneration = foodBeforeLateSupply + lateSupplyFoodGenerated;
    var displayTotalGeneration = totalGeneration;

    var netDelta = totalGeneration - totalConsumption;
    var endingReserveRaw = startingReserve + generatedProvisions - lateSupplyLoadsConsumed;
    var shortage = deficitAfterGeneratedCargo;
    var endingReserve = Math.max(0, endingReserveRaw);
    if (persistCargoChanges) {
      for (var generatedCargo : stagedGeneratedCargo) {
        cargoRepository.save(generatedCargo);
      }
    }
    if (shortage == 0) {
      storeGeneratedCookCargo(caravan.id(), cookGeneratedCargo, wagons, cargo, stagedGeneratedCargo, now, persistCargoChanges);
    } else if (persistCargoChanges) {
      for (var generatedCargo : cookGeneratedCargo) {
        cargoRepository.save(generatedCargo);
      }
    }

    var generatedEndingReserve = Math.max(0, generatedReserveBefore + generatedProvisions - lateSupplyLoadsConsumed);
    var displayEndingReserve = Math.max(0, displayCurrentReserve + generatedProvisions - lateSupplyLoadsConsumed);
    var sharedJobProductivitySerialized = sharedJobProductivityState.serialize();

    var choiceViews = choicesToViews(batidorModes, fastingEnabled);
      var provisionsInConsumption = openedSupplyConsumptionViews(cargo, stagedGeneratedCargo, cookGeneratedCargoIds);
    var consumedCookProvisions = (int) Math.ceil(cookFoodConsumed / (double) STANDARD_SUPPLY_VALUE);
    var consumedProvisions = consumedCookProvisions + lateSupplyLoadsConsumed + provisionsInConsumption.size();
    var generatedFood = rawGeneration;
    var surplusProvisions = Math.max(0, generatedProvisions - consumedProvisions);
    var contributions = new ArrayList<CaravanDailyContributionView>();
    contributions.addAll(consumptionContributions);
    contributions.addAll(generationContributions);
    contributions.sort(Comparator.comparing(CaravanDailyContributionView::effectCode).thenComparing(CaravanDailyContributionView::sourceName));

    return new CaravanDayComputation(
          state.daysPassed() + 1,
          startingReserve,
          initialProvisionsInConsumption,
          provisionsInConsumption,
          totalConsumption,
        totalGeneration,
        netDelta,
        endingReserve,
        shortage,
        generatedProvisions,
        generatedFood,
        consumedProvisions,
        surplusProvisions,
        generatedEndingReserve,
        displayCurrentReserve,
        displayTotalGeneration,
        displayEndingReserve,
        0,
        0,
        choiceViews,
        contributions,
        warnings,
        cargoMovementSummary.toSummary(),
        join(choiceViews.stream().map(String::valueOf).toList()),
        join(contributions.stream().map(String::valueOf).toList()),
        join(warnings),
        sharedJobProductivitySerialized);
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

  private CookBonusBreakdown calculateCookBonus(
      int baseValue,
      List<CaravanTraveler> cookTravelers,
      Map<UUID, List<CaravanTraveler>> servantsByMaster,
      int portableKitchenCount,
      String baseReason,
      String kitchenReason) {
    if (baseValue <= 0 || cookTravelers.isEmpty()) {
      return new CookBonusBreakdown(0, 0, 0, List.of());
    }

    var availableBlocks = baseValue / STANDARD_SUPPLY_VALUE;
    if (availableBlocks <= 0) {
      return new CookBonusBreakdown(0, 0, 0, List.of());
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
    var outputBeforeTeamwork = 0;
    var contributions = new ArrayList<CaravanDailyContributionView>();
    for (var candidate : candidates) {
      var hasPortableKitchen = assignedKitchenCookIds.contains(candidate.cook().id());
      var individualBonus = candidate.baseBonus() + (hasPortableKitchen ? 5 : 0);
      var individualOutput = STANDARD_SUPPLY_VALUE + individualBonus;
      bonus += individualBonus;
      outputBeforeTeamwork += individualOutput;
      contributions.add(contribution(
          "generation",
          "ROLE",
          candidate.cook().id().toString(),
          candidate.cook().fullName(),
          "Cocinero",
          "ADD",
          individualOutput,
          "de comida",
          hasPortableKitchen
              ? kitchenReason
              : baseReason,
          true,
          null));
    }
    return new CookBonusBreakdown(bonus, outputBeforeTeamwork, candidates.size(), contributions);
  }

  private int consumeCargoFromPool(
      List<CaravanCargo> cargoPool,
      int provisionsToConsume,
      java.time.Instant now,
      boolean persistChanges,
      boolean generatedCargoPool) {
    if (provisionsToConsume <= 0 || cargoPool.isEmpty()) {
      return provisionsToConsume;
    }

      var remaining = provisionsToConsume;
    var updatedCargo = new ArrayList<CaravanCargo>(cargoPool.size());
    var orderedCargo = cargoPool.stream()
        .sorted(supplyConsumptionComparator())
        .toList();
    for (var entry : orderedCargo) {
      if (remaining <= 0) {
        updatedCargo.add(entry);
        continue;
      }

      var availableProvisions = cargoProvisions(entry);
      if (availableProvisions <= 0) {
        updatedCargo.add(entry);
        continue;
      }

      var consumedProvisions = Math.min(availableProvisions, remaining);
      remaining -= consumedProvisions;

      var remainingProvisions = availableProvisions - consumedProvisions;
      if (remainingProvisions > 0) {
        updatedCargo.add(entry.withCurrentProvisions(remainingProvisions, entry.dayPassed(), now));
        if (!generatedCargoPool && persistChanges) {
          cargoRepository.save(entry.withCurrentProvisions(remainingProvisions, entry.dayPassed(), now));
        }
      } else if (!generatedCargoPool && persistChanges) {
        cargoRepository.deleteById(entry.caravanId(), entry.id());
      }
    }

    cargoPool.clear();
    cargoPool.addAll(updatedCargo);
    return remaining;
  }

  private String batidorTeamworkReason(int teamSize) {
    return teamSize <= 1
        ? "El batidor trabaja en solitario"
        : "Los batidores coordinan la caza y aprovechan mejor la ruta";
  }

  private String cookTeamworkReason(int teamSize) {
    return teamSize <= 1
        ? "El cocinero trabaja en solitario"
        : "Los cocineros coordinan mejor la preparación de suministros";
  }

  private String farmerTeamworkReason(int teamSize) {
    return teamSize <= 1
        ? "El agricultor trabaja en solitario"
        : "Los agricultores coordinan el trabajo y producen más suministros";
  }

  private CaravanCargo createUnassignedSupplyForCook(UUID caravanId, java.time.Instant now) {
    var catalogItem = CargoCatalog.findByCode(SUPPLIES_CODE)
        .orElseThrow(() -> new IllegalStateException("Unknown cargo catalog entry: " + SUPPLIES_CODE));
    return CaravanCargo.create(
        UUID.randomUUID(),
        caravanId,
        com.gestioncaravana.domain.CaravanCargoSourceType.CATALOG,
        catalogItem.code(),
        catalogItem.name(),
        catalogItem.category(),
        1,
        catalogItem.resolvedDefaultCargoUnits(),
        null,
        null,
        null,
        null,
        null,
        now);
  }

  private boolean isCookableSupply(CaravanCargo entry) {
    return SUPPLIES_CODE.equals(entry.catalogCode()) && !Boolean.TRUE.equals(entry.dayPassed());
  }

  private void applyCookedSupplyState(
      List<CaravanCargo> cookableSupplyEntries,
      List<CaravanDailyContributionView> cookContributions,
      List<CaravanCargo> cargo,
      List<CaravanCargo> stagedGeneratedCargo,
      List<CaravanCargo> cookGeneratedCargo,
      java.time.Instant now,
      boolean persistChanges) {
    if (cookableSupplyEntries.isEmpty() || cookContributions.isEmpty()) {
      return;
    }

    var limit = Math.min(cookableSupplyEntries.size(), cookContributions.size());
    for (var i = 0; i < limit; i++) {
      var source = cookableSupplyEntries.get(i);
      var contribution = cookContributions.get(i);
      var cooked = source.withCurrentProvisions(contribution.quantity(), true, now);
      var wasPersistedCargo = cargo.stream().anyMatch(entry -> entry.id().equals(source.id()));
      replaceCargoEntryInPools(source.id(), cooked, cargo, stagedGeneratedCargo, cookGeneratedCargo);
      if (persistChanges && wasPersistedCargo) {
        cargoRepository.save(cooked);
      }
    }
  }

  private void replaceCargoEntryInPools(
      UUID cargoId,
      CaravanCargo replacement,
      List<CaravanCargo> cargo,
      List<CaravanCargo> stagedGeneratedCargo,
      List<CaravanCargo> cookGeneratedCargo) {
    replaceCargoEntry(cargoId, replacement, cargo);
    replaceCargoEntry(cargoId, replacement, stagedGeneratedCargo);
    replaceCargoEntry(cargoId, replacement, cookGeneratedCargo);
  }

  private void replaceCargoEntry(UUID cargoId, CaravanCargo replacement, List<CaravanCargo> pool) {
    for (var index = 0; index < pool.size(); index++) {
      if (pool.get(index).id().equals(cargoId)) {
        pool.set(index, replacement);
        return;
      }
    }
  }

  private void storeGeneratedCookCargo(
      UUID caravanId,
      List<CaravanCargo> generatedCargo,
      List<CaravanWagon> wagons,
      List<CaravanCargo> cargo,
      List<CaravanCargo> stagedGeneratedCargo,
      java.time.Instant now,
      boolean persistChanges) {
    if (generatedCargo.isEmpty()) {
      return;
    }

    var remaining = new ArrayList<CaravanCargo>(generatedCargo.size());
    for (var entry : generatedCargo) {
      var placementCargo = new ArrayList<CaravanCargo>(cargo);
      placementCargo.addAll(stagedGeneratedCargo);
      placementCargo.addAll(remaining);
      var wagon = selectWagonForGeneratedSupply(caravanId, wagons, placementCargo);
      if (wagon == null) {
        remaining.add(entry);
        continue;
      }

      var stored = entry.assignWagon(wagon.id(), now);
      remaining.add(stored);
      if (persistChanges) {
        cargoRepository.save(stored);
      }
    }

    generatedCargo.clear();
    generatedCargo.addAll(remaining);
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
      return generatedQuantity == 1
          ? "El agricultor genera una carga de suministros"
          : "El agricultor genera " + generatedQuantity + " cargas de suministros";
    }
    if (generatedQuantity > 1) {
      return "El agricultor genera " + generatedQuantity + " cargas de suministros con ayuda de " + (servants.size() == 1 ? "su sirviente" : "sus sirvientes");
    }
    return "El agricultor genera una carga de suministros con ayuda de su sirviente";
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

  private static final class SharedJobProductivityTracker {
    private final Map<String, SharedJobBucketState> buckets;

    private SharedJobProductivityTracker(Map<String, SharedJobBucketState> buckets) {
      this.buckets = buckets;
    }

    static SharedJobProductivityTracker parse(String serialized) {
      var buckets = new HashMap<String, SharedJobBucketState>();
      if (serialized != null && !serialized.isBlank()) {
        for (var entry : serialized.split("\\|")) {
          if (entry.isBlank()) {
            continue;
          }
          var parts = entry.split("=", 3);
          if (parts.length < 3 || parts[0].isBlank()) {
            continue;
          }
          var carryover = parseBigDecimal(parts[1]);
          buckets.put(parts[0], new SharedJobBucketState(parts[2], carryover));
        }
      }
      return new SharedJobProductivityTracker(buckets);
    }

    void clear() {
      buckets.clear();
    }

    BigDecimal apply(String jobCode, List<UUID> contributorIds, BigDecimal baseAmount) {
      if (jobCode == null || jobCode.isBlank() || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
        return BigDecimal.ZERO;
      }

      var signature = contributorIds == null || contributorIds.isEmpty()
          ? ""
          : contributorIds.stream().map(UUID::toString).sorted().reduce((left, right) -> left + "," + right).orElse("");
      var contributorCount = contributorIds == null ? 0 : contributorIds.size();
      if (contributorCount <= 1) {
        buckets.put(jobCode, new SharedJobBucketState(signature, BigDecimal.ZERO));
        return BigDecimal.ZERO;
      }

      var bucket = buckets.get(jobCode);
      var carryover = bucket != null && signature.equals(bucket.signature()) ? bucket.carryover() : BigDecimal.ZERO;
      var multiplier = BigDecimal.ONE.add(
          BigDecimal.valueOf(Math.min(contributorCount, TEAMWORK_MAX_TRAVELERS) - 1).multiply(BigDecimal.valueOf(0.25)));
      var adjusted = baseAmount.multiply(multiplier).add(carryover);
      var produced = adjusted.setScale(0, RoundingMode.FLOOR);
      var bonus = produced.subtract(baseAmount);
      buckets.put(jobCode, new SharedJobBucketState(signature, adjusted.subtract(produced)));
      return bonus;
    }

    String serialize() {
      if (buckets.isEmpty()) {
        return null;
      }
      return buckets.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(entry -> entry.getKey() + "=" + entry.getValue().carryover().toPlainString() + "=" + entry.getValue().signature())
          .reduce((left, right) -> left + "|" + right)
          .orElse(null);
    }

    private static BigDecimal parseBigDecimal(String value) {
      if (value == null || value.isBlank()) {
        return BigDecimal.ZERO;
      }
      try {
        return new BigDecimal(value.trim());
      } catch (NumberFormatException ex) {
        return BigDecimal.ZERO;
      }
    }
  }

  private record SharedJobBucketState(String signature, BigDecimal carryover) {}

  private record CookBonusCandidate(CaravanTraveler cook, int baseBonus, List<CaravanTraveler> servants) {}

  private record CookBonusBreakdown(int totalBonus, int totalOutputBeforeTeamwork, int consumedBlocks, List<CaravanDailyContributionView> contributions) {}

  private List<CaravanTraveler> servantsForMaster(UUID travelerId, Map<UUID, List<CaravanTraveler>> servantsByMaster) {
    return servantsByMaster.getOrDefault(travelerId, List.of());
  }

  private int consumeCargoPoolForShortage(
      UUID caravanId,
      List<CaravanCargo> cargo,
      int deficit,
      java.time.Instant now,
      List<CaravanDailyContributionView> generationContributions,
      boolean persistChanges,
      java.util.function.Predicate<CaravanCargo> filter,
      boolean openRemaining,
      boolean reportMovementSummary,
      CargoMovementSummaryBuilder cargoMovementSummary,
      String reason) {
    if (deficit <= 0) {
      return 0;
    }

    var entries = cargo.stream()
        .filter(filter)
        .sorted(supplyConsumptionComparator())
        .toList();

    var converted = 0;
    var remainingDeficit = deficit;
    for (var entry : entries) {
      if (remainingDeficit <= 0) {
        break;
      }

      var availableProvisions = cargoProvisions(entry);
      if (availableProvisions <= 0) {
        continue;
      }

      var consumedProvisions = Math.min(availableProvisions, remainingDeficit);
      if (consumedProvisions <= 0) {
        continue;
      }

      converted += consumedProvisions;
      remainingDeficit -= consumedProvisions;
      generationContributions.add(contribution(
          "generation",
          "CARGO",
          entry.id().toString(),
          entry.displayName(),
          "ADD",
          consumedProvisions,
          "provisiones",
          reason,
          true,
          null));

      var remainingProvisions = availableProvisions - consumedProvisions;
      if (reportMovementSummary) {
        cargoMovementSummary.recordConsumedLoad(remainingProvisions);
      }
      if (persistChanges) {
        if (remainingProvisions <= 0) {
          cargoRepository.deleteById(caravanId, entry.id());
        } else {
          cargoRepository.save(entry.withCurrentProvisions(
              remainingProvisions,
              openRemaining || Boolean.TRUE.equals(entry.dayPassed()),
              now));
        }
      }

      var updatedEntry = remainingProvisions <= 0
          ? null
          : entry.withCurrentProvisions(
              remainingProvisions,
              openRemaining || Boolean.TRUE.equals(entry.dayPassed()),
              now);
      removeCargoEntry(cargo, entry.id(), updatedEntry);
    }

    return converted;
  }

  private int consumeGeneratedCargoForShortage(
      UUID caravanId,
      List<CaravanCargo> generatedCargo,
      int deficit,
      java.time.Instant now,
      List<CaravanDailyContributionView> generationContributions,
      boolean persistChanges,
      boolean openRemaining,
      boolean reportContribution,
      boolean reportMovementSummary,
      CargoMovementSummaryBuilder cargoMovementSummary,
      String reason) {
    if (deficit <= 0 || generatedCargo.isEmpty()) {
      return 0;
    }

    var converted = 0;
    var remainingDeficit = deficit;
    var remainingCargo = new ArrayList<CaravanCargo>(generatedCargo.size());
    var orderedCargo = generatedCargo.stream()
        .sorted(supplyConsumptionComparator())
        .toList();
    for (var entry : orderedCargo) {
      if (remainingDeficit <= 0) {
        remainingCargo.add(entry);
        continue;
      }

      var availableProvisions = cargoProvisions(entry);
      if (availableProvisions <= 0) {
        continue;
      }

      var consumedProvisions = Math.min(availableProvisions, remainingDeficit);
      if (consumedProvisions <= 0) {
        remainingCargo.add(entry);
        continue;
      }

      converted += consumedProvisions;
      remainingDeficit -= consumedProvisions;
      if (reportContribution) {
        generationContributions.add(contribution(
            "generation",
            "CARGO",
            entry.id().toString(),
            entry.displayName(),
            "ADD",
            consumedProvisions,
            "provisiones",
            reason,
            true,
            null));
      }
      if (reportMovementSummary) {
        cargoMovementSummary.recordConsumedLoad(consumedProvisions, availableProvisions - consumedProvisions);
      }

      var remainingProvisions = availableProvisions - consumedProvisions;
      if (remainingProvisions > 0) {
        remainingCargo.add(entry.withCurrentProvisions(
            remainingProvisions,
            openRemaining || Boolean.TRUE.equals(entry.dayPassed()),
            now));
      }
    }

    generatedCargo.clear();
    generatedCargo.addAll(remainingCargo);
    return converted;
  }

  private boolean isOpenedCargo(CaravanCargo entry) {
    return Boolean.TRUE.equals(entry.dayPassed())
        && (SUPPLIES_CODE.equals(entry.catalogCode()) || PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode()));
  }

  private Comparator<CaravanCargo> supplyConsumptionComparator() {
    return Comparator
        .comparingInt(CaravanDayCycleService::supplyRemainingProvisions)
        .thenComparing(entry -> !PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode()))
        .thenComparing(CaravanCargo::updatedAt)
        .thenComparing(CaravanCargo::id);
  }

  private static int supplyRemainingProvisions(CaravanCargo entry) {
    return entry.currentProvisions() == null ? 0 : entry.currentProvisions();
  }

  private boolean isUnopenedCargo(CaravanCargo entry) {
    return !Boolean.TRUE.equals(entry.dayPassed())
        && (SUPPLIES_CODE.equals(entry.catalogCode()) || PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode()));
  }

  private void removeCargoEntry(List<CaravanCargo> cargo, UUID cargoId, CaravanCargo replacement) {
    for (var index = 0; index < cargo.size(); index++) {
      if (cargo.get(index).id().equals(cargoId)) {
        if (replacement == null) {
          cargo.remove(index);
        } else {
          cargo.set(index, replacement);
        }
        return;
      }
    }
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
        .sorted(Comparator
            .comparingInt((CaravanWagon wagon) -> cargo.stream()
                .filter(entry -> wagon.id().equals(entry.wagonId()))
                .mapToInt(entry -> entry.quantity() * entry.cargoUnits())
                .sum())
            .thenComparing(CaravanWagon::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CaravanWagon::id))
        .findFirst()
        .orElseGet(() -> wagons.stream()
            .filter(wagon -> hasCargoCapacity(caravanId, wagon, cargo, 1))
            .sorted(Comparator
                .comparingInt((CaravanWagon wagon) -> cargo.stream()
                    .filter(entry -> wagon.id().equals(entry.wagonId()))
                    .mapToInt(entry -> entry.quantity() * entry.cargoUnits())
                    .sum())
                .thenComparing(CaravanWagon::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CaravanWagon::id))
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
      String quantityUnit,
      String reason,
      boolean applied,
      String ignoredReason) {
    return contribution(effectCode, sourceType, sourceId, sourceName, null, operation, quantity, quantityUnit, reason, applied, ignoredReason);
  }

  private CaravanDailyContributionView contribution(
      String effectCode,
      String sourceType,
      String sourceId,
      String sourceName,
      String sourceRoleName,
      String operation,
      int quantity,
      String quantityUnit,
      String reason,
      boolean applied,
      String ignoredReason) {
    return new CaravanDailyContributionView(effectCode, sourceType, sourceId, sourceName, sourceRoleName, operation, quantity, quantityUnit, reason, applied, ignoredReason);
  }

  private String join(List<String> items) {
    return String.join("\n", items);
  }

  private int supplyCargoQuantity(List<CaravanCargo> cargoEntries) {
    return cargoEntries.stream()
        .filter(entry -> SUPPLIES_CODE.equals(entry.catalogCode()) || PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode()))
        .mapToInt(CaravanCargo::quantity)
        .sum();
  }

  private int reserveSupplyCargoQuantity(List<CaravanCargo> cargoEntries) {
    return cargoEntries.stream()
        .filter(entry -> (SUPPLIES_CODE.equals(entry.catalogCode()) || PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode()))
            && !Boolean.TRUE.equals(entry.dayPassed()))
        .mapToInt(CaravanCargo::quantity)
        .sum();
  }

  private String formatCargoMovementSummary(int cargoDelta) {
    var sign = cargoDelta >= 0 ? "+" : "-";
    return sign + " " + Math.abs(cargoDelta) + " cargas de suministros";
  }

  private List<CaravanSupplyConsumptionView> openedSupplyConsumptionViews(
        List<CaravanCargo> cargoEntries,
        List<CaravanCargo> stagedGeneratedCargo,
        java.util.Set<UUID> excludedCargoIds) {
    return java.util.stream.Stream.concat(cargoEntries.stream(), stagedGeneratedCargo.stream())
        .filter(entry -> !excludedCargoIds.contains(entry.id()))
        .filter(entry -> (SUPPLIES_CODE.equals(entry.catalogCode()) || PERISHABLE_SUPPLIES_CODE.equals(entry.catalogCode()))
            && Boolean.TRUE.equals(entry.dayPassed())
            && cargoProvisions(entry) > 0)
        .sorted(Comparator.comparing(CaravanCargo::updatedAt).thenComparing(CaravanCargo::id))
        .map(entry -> new CaravanSupplyConsumptionView(cargoProvisions(entry) % STANDARD_SUPPLY_VALUE))
        .filter(view -> view.remainingFood() > 0)
        .toList();
  }

  private CaravanDayPreviewView toPreview(UUID caravanId, CaravanDayComputation computation) {
    return new CaravanDayPreviewView(
        caravanId,
        computation.dayIndex(),
        computation.displayCurrentReserve(),
        computation.initialProvisionsInConsumption(),
        computation.provisionsInConsumption(),
        computation.totalConsumption(),
        computation.displayTotalGeneration(),
        computation.displayTotalGeneration() - computation.totalConsumption(),
        computation.displayEndingReserve(),
        Math.max(computation.totalConsumption() - computation.displayTotalGeneration(), 0),
        computation.generatedProvisions(),
        computation.generatedFood(),
        computation.consumedProvisions(),
        computation.surplusProvisions(),
        computation.warnings(),
        computation.choices(),
        computation.contributions(),
        computation.cargoMovementSummary());
  }

  private CaravanDayResolutionView toView(CaravanDayResolution resolution) {
    return new CaravanDayResolutionView(
        resolution.id(),
        resolution.caravanId(),
        resolution.idempotencyKey(),
        resolution.resolvedDayIndex(),
        resolution.resolvedAt(),
        resolution.startingReserve(),
        List.of(),
        List.of(),
        resolution.endingReserve(),
        resolution.totalConsumption(),
        resolution.totalGeneration(),
        resolution.netDelta(),
        resolution.shortage(),
        0,
        0,
        0,
        0,
        List.of(),
        List.of(),
        splitLines(resolution.warningsSummary()),
        resolution.cargoMovementSummary());
  }

  private CaravanDayResolutionView toView(CaravanDayResolution resolution, CaravanDayComputation computation) {
    return new CaravanDayResolutionView(
        resolution.id(),
        resolution.caravanId(),
        resolution.idempotencyKey(),
        resolution.resolvedDayIndex(),
        resolution.resolvedAt(),
        computation.displayCurrentReserve(),
        computation.initialProvisionsInConsumption(),
        computation.provisionsInConsumption(),
        computation.displayEndingReserve(),
        resolution.totalConsumption(),
        computation.displayTotalGeneration(),
        computation.displayTotalGeneration() - resolution.totalConsumption(),
        Math.max(resolution.totalConsumption() - computation.displayTotalGeneration(), 0),
        computation.generatedProvisions(),
        computation.generatedFood(),
        computation.consumedProvisions(),
        computation.surplusProvisions(),
        computation.choices(),
        computation.contributions(),
        computation.warnings(),
        resolution.cargoMovementSummary());
  }

  private List<String> splitLines(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    return List.of(text.split("\\R"));
  }

  private static final class CargoMovementSummaryBuilder {
    private int consumedLoads;
    private Integer partialRemainingProvisions;

    void recordConsumedLoad(int remainingProvisions) {
      recordConsumedLoad(1, remainingProvisions);
    }

    void recordConsumedLoad(int consumedProvisions, int remainingProvisions) {
      consumedLoads += (int) Math.ceil(consumedProvisions / (double) STANDARD_SUPPLY_VALUE);
      var normalizedRemainingProvisions = remainingProvisions % STANDARD_SUPPLY_VALUE;
      if (normalizedRemainingProvisions > 0) {
        partialRemainingProvisions = normalizedRemainingProvisions;
      }
    }

    String toSummary() {
      if (consumedLoads <= 0) {
        return "+ 0 cargas de suministros";
      }
      var lines = new ArrayList<String>();
      lines.add("+ " + consumedLoads + " cargas de suministros");
      if (partialRemainingProvisions != null && partialRemainingProvisions > 0) {
        lines.add("+ 1 carga de suministros con " + partialRemainingProvisions + " de comida restante");
      }
      return String.join("\n", lines);
    }

    int consumedLoads() {
      return consumedLoads;
    }
  }

  private record CaravanDayComputation(
      int dayIndex,
      int startingReserve,
      List<CaravanSupplyConsumptionView> initialProvisionsInConsumption,
      List<CaravanSupplyConsumptionView> provisionsInConsumption,
      int totalConsumption,
      int totalGeneration,
      int netDelta,
      int endingReserve,
      int shortage,
      int generatedProvisions,
      int generatedFood,
      int consumedProvisions,
      int surplusProvisions,
      int generatedEndingReserve,
      int displayCurrentReserve,
      int displayTotalGeneration,
      int displayEndingReserve,
      int endingStandardReserve,
      int endingPerishableReserve,
      List<CaravanDailyChoiceView> choices,
      List<CaravanDailyContributionView> contributions,
      List<String> warnings,
      String cargoMovementSummary,
      String choicesSummary,
      String contributionsSummary,
      String warningsSummary,
      String sharedJobProductivityState) {}
}
