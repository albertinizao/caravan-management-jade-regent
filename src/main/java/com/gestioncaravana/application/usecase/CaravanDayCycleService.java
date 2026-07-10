package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanDayCycleLogEntryView;
import com.gestioncaravana.application.model.CaravanDayCyclePreviewView;
import com.gestioncaravana.application.model.CaravanMultiDayCyclePreviewView;
import com.gestioncaravana.application.model.CaravanCargoSummaryView;
import com.gestioncaravana.application.port.in.ConfirmCaravanMultiDayCycleUseCase;
import com.gestioncaravana.application.port.in.ConfirmCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.in.ListCaravanCargoSummaryUseCase;
import com.gestioncaravana.application.port.in.PreviewCaravanMultiDayCycleUseCase;
import com.gestioncaravana.application.port.in.PreviewCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanDayCycleResultRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanDayCycleResult;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CargoCatalog;
import com.gestioncaravana.domain.CargoCatalogItem;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.WagonType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CaravanDayCycleService implements
    PreviewCaravanDayCycleUseCase,
    ConfirmCaravanDayCycleUseCase,
    PreviewCaravanMultiDayCycleUseCase,
    ConfirmCaravanMultiDayCycleUseCase {

  private static final String SUPPLIES_CODE = "suministros";
  private static final String PERISHABLE_SUPPLIES_CODE = "suministros-perecederos";
  private static final String COOK_CODE = "cocinero";
  private static final String BATIDOR_CODE = "batidor";
  private static final String AGRICULTOR_CODE = "agricultor";
  private static final String BOTICARIO_CODE = "boticario";
  private static final String ARTESANO_CODE = "artesano";
  private static final String SERVANT_CODE = "sirviente";
  private static final String TEAMWORK_FEAT_CODE = "trabajo-en-equipo";
  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final BigDecimal QUARTER = BigDecimal.valueOf(0.25d);
  private static final BigDecimal FIVE = BigDecimal.valueOf(5);
  private static final BigDecimal TEN = BigDecimal.TEN;
  private static final BigDecimal HALF = BigDecimal.valueOf(0.5d);
  private static final BigDecimal FIFTEEN = BigDecimal.valueOf(15);

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanCargoRepositoryPort cargoRepository;
  private final CaravanFeatRepositoryPort featRepository;
  private final CaravanSupplyStateRepositoryPort supplyStateRepository;
  private final ListCaravanCargoSummaryUseCase cargoSummaryUseCase;
  private final GetCaravanStatisticsUseCase statisticsUseCase;
  private final CaravanDayCycleResultRepositoryPort dayCycleResultRepository;
  private final Clock clock;

  public CaravanDayCycleService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanCargoRepositoryPort cargoRepository,
      CaravanFeatRepositoryPort featRepository,
      CaravanSupplyStateRepositoryPort supplyStateRepository,
      ListCaravanCargoSummaryUseCase cargoSummaryUseCase,
      GetCaravanStatisticsUseCase statisticsUseCase,
      CaravanDayCycleResultRepositoryPort dayCycleResultRepository,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.travelerRepository = travelerRepository;
    this.wagonRepository = wagonRepository;
    this.cargoRepository = cargoRepository;
    this.featRepository = featRepository;
    this.supplyStateRepository = supplyStateRepository;
    this.cargoSummaryUseCase = cargoSummaryUseCase;
    this.statisticsUseCase = statisticsUseCase;
    this.dayCycleResultRepository = dayCycleResultRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanDayCyclePreviewView preview(UUID caravanId) {
    var state = loadState(caravanId);
    return simulate(state, false, null).previewView();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanMultiDayCyclePreviewView preview(UUID caravanId, PreviewCaravanMultiDayCycleCommand command) {
    var days = requireDays(command == null ? null : command.days());
    var state = loadState(caravanId);
    return simulateMultiple(state, days, false).previewView();
  }

  @Override
  public CaravanDayCyclePreviewView confirm(UUID caravanId, ConfirmCaravanDayCycleCommand command) {
    if (command == null || command.previewFingerprint() == null || command.previewFingerprint().isBlank()) {
      throw new IllegalArgumentException("previewFingerprint is required");
    }

    var state = loadState(caravanId);
    var currentFingerprint = fingerprint(state);
    if (!currentFingerprint.equals(command.previewFingerprint())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "The day preview is stale. Please refresh the simulation.");
    }

    var simulation = simulate(state, true, command.previewFingerprint());
    persistResolution(state, simulation);
    return simulation.previewView().withConfirmation(simulation.result().id(), simulation.result().resolvedAt());
  }

  @Override
  public CaravanMultiDayCyclePreviewView confirm(UUID caravanId, ConfirmCaravanMultiDayCycleCommand command) {
    if (command == null || command.basePreviewFingerprint() == null || command.basePreviewFingerprint().isBlank()) {
      throw new IllegalArgumentException("basePreviewFingerprint is required");
    }
    var days = requireDays(command.days());
    var state = loadState(caravanId);
    var currentFingerprint = fingerprint(state);
    if (!currentFingerprint.equals(command.basePreviewFingerprint())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "The day preview is stale. Please refresh the simulation.");
    }

    var simulation = simulateMultiple(state, days, true);
    persistMultiResolution(state, simulation);
    return simulation.previewView();
  }

  private void persistResolution(DayCycleState state, DayCycleSimulation simulation) {
    persistCargoAndSupplyState(state.caravan().id(), simulation.nextSupplyState(), simulation.updatedCargo());
    dayCycleResultRepository.save(simulation.result());
  }

  private void persistMultiResolution(DayCycleState initialState, MultiDayCycleSimulation simulation) {
    persistCargoAndSupplyState(
        initialState.caravan().id(),
        simulation.finalState().supplyState(),
        simulation.finalState().cargo());
    for (var daySimulation : simulation.daySimulations()) {
      dayCycleResultRepository.save(daySimulation.result());
    }
  }

  private void persistCargoAndSupplyState(
      UUID caravanId,
      CaravanSupplyState supplyState,
      List<CaravanCargo> cargoEntries) {
    cargoRepository.deleteByCaravanId(caravanId);
    for (var cargo : cargoEntries) {
      cargoRepository.save(cargo);
    }
    supplyStateRepository.save(supplyState);
  }

  private int requireDays(Integer days) {
    if (days == null) {
      throw new IllegalArgumentException("days is required");
    }
    if (days < 1 || days > 30) {
      throw new IllegalArgumentException("days must be between 1 and 30");
    }
    return days;
  }

  private DayCycleSimulation simulate(DayCycleState state, boolean confirmed, String confirmationFingerprint) {
    var now = clock.instant();
    var dayIndex = state.supplyState().daysPassed() + 1;
    var previewFingerprint = fingerprint(state);

    var travelers = state.travelers();
    var wagons = state.wagons();
    var cargo = state.cargo();
    var statistics = state.statistics();
    var requiredConsumption = BigDecimal.valueOf(statistics.otherStats().consumption());

    var logs = new ArrayList<CaravanDayCycleLogEntryView>();
    var warnings = new ArrayList<String>();
    var tempInventory = new ArrayList<TempCargoItem>();
    var summaryByWagon = state.cargoSummaries().stream().collect(java.util.stream.Collectors.toMap(
        CaravanCargoSummaryView::wagonId,
        summary -> summary,
        (left, right) -> left,
        LinkedHashMap::new));

    var initialSupplyUnits = countCargoUnits(cargo, SUPPLIES_CODE);
    var initialPerishableUnits = countCargoUnits(cargo, PERISHABLE_SUPPLIES_CODE);
    var initialPerishableFood = sumPerishableFood(cargo);

    var progressState = loadAgricultorProgress(state.supplyState().sharedJobProductivityState());
    var teamworkEnabled = hasActiveTeamworkFeat(state.feats());
    var agricultorTeamwork = resolveTeamworkAssignment(travelers, AGRICULTOR_CODE, teamworkEnabled);
    var boticarioTeamwork = resolveTeamworkAssignment(travelers, BOTICARIO_CODE, teamworkEnabled);
    var batidorTeamwork = resolveTeamworkAssignment(travelers, BATIDOR_CODE, teamworkEnabled);
    var cookTeamwork = resolveTeamworkAssignment(travelers, COOK_CODE, teamworkEnabled);

    var generatedSuppliesFromAgricultors = resolveAgricultors(
        state.caravan().id(), travelers, progressState, tempInventory, logs, agricultorTeamwork);
    var generatedAlchemyValueFromBoticarios = resolveBoticarios(travelers, logs, boticarioTeamwork);
    resolveArtesanos(travelers, logs);

    var generatedFood = resolveBatidores(travelers, logs, batidorTeamwork);
    generatedFood = generatedFood.add(initialPerishableFood);
    logs.add(new CaravanDayCycleLogEntryView(
        "food",
        "Suministros perecederos contabilizados",
        List.of("Se suma toda la comida contenida en suministros perecederos: " + formatBigDecimal(initialPerishableFood)),
        initialPerishableFood));

    var currentCargo = new ArrayList<CaravanCargo>(cargo);
    currentCargo.removeIf(entry -> isPerishableSupply(entry));

    if (generatedFood.compareTo(requiredConsumption) < 0) {
      moveRegularSuppliesToInventory(currentCargo, tempInventory, logs);
    }

    var cooks = resolveCooks(travelers, cookTeamwork);
    appendCookTeamworkSummary(logs, cookTeamwork);
    var portableKitchenUses = countCargoUnits(cargo, "cocina-portatil");
    var cookUsageCounter = 0;

    while (generatedFood.compareTo(requiredConsumption) < 0) {
      var nextSupply = takeNextSupply(tempInventory);
      if (nextSupply == null) {
        break;
      }

      var cook = takeNextCook(cooks);
      var portableKitchenUsed = cook != null && portableKitchenUses > 0;
      if (portableKitchenUsed) {
        portableKitchenUses--;
      }

      var cookedFood = resolveCookOutput(cook, portableKitchenUsed, logs, cookTeamwork);
      generatedFood = generatedFood.add(cookedFood);
      cookUsageCounter++;

      logs.add(new CaravanDayCycleLogEntryView(
          "food",
          "Se consume una unidad de suministros",
          List.of(
              "Comida generada: " + formatBigDecimal(cookedFood),
              cook == null ? "No había cocinero disponible." : "Cocinero: " + cook.traveler().fullName(),
              portableKitchenUsed ? "Cocina portátil aplicada." : "Sin cocina portátil."),
          cookedFood));
    }

    var leftoverFood = generatedFood.subtract(requiredConsumption);
    var consumptionCovered = leftoverFood.compareTo(BigDecimal.ZERO) >= 0;
    if (!consumptionCovered) {
      warnings.add("No se ha podido cubrir el consumo de la caravana.");
    }

    if (leftoverFood.compareTo(BigDecimal.ZERO) > 0) {
        addLeftoverAsPerishable(state.caravan().id(), leftoverFood, tempInventory, logs);
    }

    applyPerishableDecayAndPrepareInventory(currentCargo, tempInventory, logs);

    var rebuiltCargo = rebuildCargo(currentCargo, tempInventory, summaryByWagon, wagons, logs, warnings, now);
    var finalSupplyUnits = (int) rebuiltCargo.stream().filter(this::isSupply).count();
    var finalPerishableUnits = (int) rebuiltCargo.stream().filter(this::isPerishableSupply).count();
    var finalPerishableFood = sumPerishableFood(rebuiltCargo);

    var resultId = UUID.randomUUID();
    var result = new CaravanDayCycleResult(
        resultId,
        state.caravan().id(),
        previewFingerprint,
        dayIndex,
        now,
        initialSupplyUnits,
        initialPerishableFood,
        initialPerishableUnits,
        generatedSuppliesFromAgricultors,
        generatedAlchemyValueFromBoticarios,
        requiredConsumption,
        generatedFood,
        leftoverFood.max(BigDecimal.ZERO),
        finalSupplyUnits,
        finalPerishableUnits,
        finalPerishableFood,
        confirmed,
        serializeLogs(logs),
        serializeWarnings(warnings));

    var previewView = new CaravanDayCyclePreviewView(
        state.caravan().id(),
        previewFingerprint,
        confirmed,
        confirmed ? resultId : null,
        confirmed ? now : null,
        dayIndex,
        initialSupplyUnits,
        initialPerishableFood,
        initialPerishableUnits,
        generatedSuppliesFromAgricultors,
        generatedAlchemyValueFromBoticarios,
        requiredConsumption,
        consumptionCovered,
        generatedFood,
        leftoverFood.max(BigDecimal.ZERO),
        finalSupplyUnits,
        finalPerishableUnits,
        finalPerishableFood,
        cookUsageCounter,
        logs,
        warnings);

    var nextSupplyState = state.supplyState().advanceDay(now, serializeProgress(progressState));
    return new DayCycleSimulation(result, previewView, rebuiltCargo, nextSupplyState, logs, warnings);
  }

  private MultiDayCycleSimulation simulateMultiple(DayCycleState initialState, int days, boolean confirmed) {
    var basePreviewFingerprint = fingerprint(initialState);
    var daySimulations = new ArrayList<DayCycleSimulation>(days);
    var currentState = initialState;
    for (var index = 0; index < days; index++) {
      var simulation = simulate(currentState, confirmed, basePreviewFingerprint);
      daySimulations.add(simulation);
      currentState = advanceState(currentState, simulation);
    }
    return new MultiDayCycleSimulation(
        buildMultiDayPreview(initialState, basePreviewFingerprint, confirmed, daySimulations),
        daySimulations,
        currentState);
  }

  private DayCycleState advanceState(DayCycleState currentState, DayCycleSimulation simulation) {
    return new DayCycleState(
        currentState.caravan(),
        simulation.nextSupplyState(),
        currentState.travelers(),
        currentState.wagons(),
        simulation.updatedCargo(),
        currentState.feats(),
        recalculateCargoSummaries(currentState.cargoSummaries(), simulation.updatedCargo()),
        currentState.statistics());
  }

  private CaravanMultiDayCyclePreviewView buildMultiDayPreview(
      DayCycleState initialState,
      String basePreviewFingerprint,
      boolean confirmed,
      List<DayCycleSimulation> daySimulations) {
    var dayPreviews = daySimulations.stream().map(DayCycleSimulation::previewView).toList();
    var firstPreview = dayPreviews.getFirst();
    var lastPreview = dayPreviews.getLast();
    var warnings = dayPreviews.stream()
        .flatMap(preview -> preview.warnings().stream())
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    return new CaravanMultiDayCyclePreviewView(
        initialState.caravan().id(),
        basePreviewFingerprint,
        confirmed,
        daySimulations.size(),
        firstPreview.dayIndex(),
        lastPreview.dayIndex(),
        dayPreviews.stream().map(CaravanDayCyclePreviewView::requiredConsumption).reduce(BigDecimal.ZERO, BigDecimal::add),
        dayPreviews.stream().map(CaravanDayCyclePreviewView::generatedFood).reduce(BigDecimal.ZERO, BigDecimal::add),
        dayPreviews.stream().mapToInt(CaravanDayCyclePreviewView::generatedSuppliesFromAgricultors).sum(),
        dayPreviews.stream().map(CaravanDayCyclePreviewView::generatedAlchemyValueFromBoticarios).reduce(BigDecimal.ZERO, BigDecimal::add),
        dayPreviews.stream().mapToInt(CaravanDayCyclePreviewView::suppliesConsumed).sum(),
        (int) dayPreviews.stream().filter(preview -> !preview.consumptionCovered()).count(),
        lastPreview.finalSupplyUnits(),
        lastPreview.finalPerishableUnits(),
        lastPreview.finalPerishableFood(),
        confirmed ? daySimulations.getLast().result().resolvedAt() : null,
        List.copyOf(warnings),
        dayPreviews);
  }

  private DayCycleState loadState(UUID caravanId) {
    var caravan = caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
    var travelers = travelerRepository.findAllByCaravanId(caravanId);
    var wagons = wagonRepository.findAllByCaravanId(caravanId);
    var cargo = cargoRepository.findAllByCaravanId(caravanId);
    var feats = featRepository.findAllByCaravanId(caravanId);
    var supplyState = supplyStateRepository.findByCaravanId(caravanId)
        .orElseGet(() -> CaravanSupplyState.initial(caravanId, caravan.updatedAt()));
    var statistics = statisticsUseCase.getById(caravanId);
    var cargoSummaries = cargoSummaryUseCase.list(caravanId);
    return new DayCycleState(caravan, supplyState, travelers, wagons, cargo, feats, cargoSummaries, statistics);
  }

  private List<CaravanCargoSummaryView> recalculateCargoSummaries(
      List<CaravanCargoSummaryView> currentSummaries,
      List<CaravanCargo> updatedCargo) {
    return currentSummaries.stream()
        .map(summary -> {
          var usedCargoUnits = updatedCargo.stream()
              .filter(cargo -> summary.wagonId().equals(cargo.wagonId()))
              .mapToInt(cargo -> cargo.quantity() * cargo.cargoUnits())
              .sum();
          var remainingCargoUnits = Math.max(0, summary.cargoCapacity() - usedCargoUnits);
          var cargoEntryCount = (int) updatedCargo.stream()
              .filter(cargo -> summary.wagonId().equals(cargo.wagonId()))
              .count();
          return new CaravanCargoSummaryView(
              summary.wagonId(),
              summary.wagonName(),
              summary.cargoCapacity(),
              usedCargoUnits,
              remainingCargoUnits,
              cargoEntryCount);
        })
        .toList();
  }

  private int resolveAgricultors(
      UUID caravanId,
      List<CaravanTraveler> travelers,
      Map<UUID, BigDecimal> progressState,
      List<TempCargoItem> tempInventory,
      List<CaravanDayCycleLogEntryView> logs,
      TeamworkAssignment teamwork) {
    var agricultors = travelers.stream()
        .filter(traveler -> traveler.hasActiveRole(AGRICULTOR_CODE))
        .sorted(travelerOrder())
        .toList();
    var generated = 0;
    for (var agricultor : agricultors) {
      var servant = findServantFor(travelers, agricultor.id());
      var increment = BigDecimal.valueOf(0.5d);
      var details = new ArrayList<String>();
      details.add(agricultor.fullName() + " actúa como agricultor");
      if (servant != null) {
        var bonus = canExerciseRole(servant, AGRICULTOR_CODE) ? BigDecimal.valueOf(0.5d) : BigDecimal.valueOf(0.25d);
        increment = increment.add(bonus);
        details.add(canExerciseRole(servant, AGRICULTOR_CODE)
            ? "Sirviente: " + servant.fullName() + " (agricultor): +" + formatBigDecimalString(bonus)
            : "Sirviente: " + servant.fullName() + " (no puede ser agricultor): +" + formatBigDecimalString(bonus));
      }
      if (teamwork.appliesTo(agricultor.id())) {
        increment = increment.multiply(teamwork.multiplier());
        appendTeamworkDetails(details, teamwork);
      }

      var progress = progressState.getOrDefault(agricultor.id(), BigDecimal.ZERO).add(increment);
      var produced = 0;
      while (progress.compareTo(ONE) >= 0) {
        progress = progress.subtract(ONE);
        produced++;
        tempInventory.add(TempCargoItem.regular(caravanId, SUPPLIES_CODE, TEN, null));
      }
      progressState.put(agricultor.id(), progress);
      generated += produced;
      details.add("Progreso acumulado: " + formatBigDecimalString(progress));
      details.add("Suministros generados: " + produced);
      logs.add(new CaravanDayCycleLogEntryView("pre-food", agricultor.fullName(), details, BigDecimal.valueOf(produced)));
    }
    if (!agricultors.isEmpty()) {
      var summaryDetails = new ArrayList<String>();
      summaryDetails.add("Suministros generados por agricultores: " + generated);
      appendTeamworkSummary(summaryDetails, "agricultores", teamwork);
      logs.add(new CaravanDayCycleLogEntryView(
          "pre-food-summary",
          "Resumen de agricultores",
          summaryDetails,
          BigDecimal.valueOf(generated)));
    }
    return generated;
  }

  private BigDecimal resolveBoticarios(
      List<CaravanTraveler> travelers,
      List<CaravanDayCycleLogEntryView> logs,
      TeamworkAssignment teamwork) {
    var boticarios = travelers.stream()
        .filter(traveler -> traveler.hasActiveRole(BOTICARIO_CODE))
        .sorted(travelerOrder())
        .toList();
    var total = BigDecimal.ZERO;
    for (var boticario : boticarios) {
      var servant = findServantFor(travelers, boticario.id());
      var value = BigDecimal.valueOf(5);
      var details = new ArrayList<String>();
      details.add(boticario.fullName() + " actúa como boticario");
      if (servant != null) {
        var bonus = canExerciseRole(servant, BOTICARIO_CODE) ? BigDecimal.valueOf(5) : BigDecimal.valueOf(2.5d);
        value = value.add(bonus);
        details.add(canExerciseRole(servant, BOTICARIO_CODE)
            ? "Sirviente: " + servant.fullName() + " (boticario): +" + formatBigDecimalString(bonus) + " po"
            : "Sirviente: " + servant.fullName() + " (no puede ser boticario): +" + formatBigDecimalString(bonus) + " po");
      }
      if (teamwork.appliesTo(boticario.id())) {
        value = value.multiply(teamwork.multiplier());
        appendTeamworkDetails(details, teamwork);
      }
      total = total.add(value);
      details.add("Valor generado: " + formatBigDecimalString(value) + " po");
      logs.add(new CaravanDayCycleLogEntryView("pre-food", boticario.fullName(), details, value));
    }
    if (!boticarios.isEmpty()) {
      var summaryDetails = new ArrayList<String>();
      summaryDetails.add("Valor alquímico total generado: " + formatBigDecimal(total) + " po");
      appendTeamworkSummary(summaryDetails, "boticarios", teamwork);
      logs.add(new CaravanDayCycleLogEntryView(
          "pre-food-summary",
          "Resumen de boticarios",
          summaryDetails,
          total));
    }
    return total;
  }

  private void resolveArtesanos(List<CaravanTraveler> travelers, List<CaravanDayCycleLogEntryView> logs) {
    var artesanos = travelers.stream()
        .filter(traveler -> traveler.hasActiveRole(ARTESANO_CODE))
        .sorted(travelerOrder())
        .toList();
    if (artesanos.isEmpty()) {
      return;
    }
    var details = new ArrayList<String>();
    for (var artesano : artesanos) {
      var servant = findServantFor(travelers, artesano.id());
      details.add(artesano.fullName() + " actúa como artesano");
      if (servant != null) {
        details.add(" - sirviente: " + servant.fullName() + " · puede ser artesano: " + canExerciseRole(servant, ARTESANO_CODE));
      }
    }
    logs.add(new CaravanDayCycleLogEntryView("pre-food-summary", "Resumen de artesanos", details, BigDecimal.ZERO));
  }

  private BigDecimal resolveBatidores(
      List<CaravanTraveler> travelers,
      List<CaravanDayCycleLogEntryView> logs,
      TeamworkAssignment teamwork) {
    var batidores = travelers.stream()
        .filter(traveler -> traveler.hasActiveRole(BATIDOR_CODE))
        .sorted(travelerOrder())
        .toList();
    var total = BigDecimal.ZERO;
    for (var batidor : batidores) {
      var servant = findServantFor(travelers, batidor.id());
      var generated = BigDecimal.valueOf(2);
      var details = new ArrayList<String>();
      details.add(batidor.fullName() + " actúa como batidor");
      details.add("Modo asumido: caza");
      if (servant != null) {
        var bonus = canExerciseRole(servant, BATIDOR_CODE) ? BigDecimal.valueOf(2) : BigDecimal.ONE;
        generated = generated.add(bonus);
        details.add(canExerciseRole(servant, BATIDOR_CODE)
            ? "Sirviente: " + servant.fullName() + " (batidor): +" + formatBigDecimalString(bonus) + " comida"
            : "Sirviente: " + servant.fullName() + " (no puede ser batidor): +" + formatBigDecimalString(bonus) + " comida");
      }
      if (teamwork.appliesTo(batidor.id())) {
        generated = generated.multiply(teamwork.multiplier());
        appendTeamworkDetails(details, teamwork);
      }
      total = total.add(generated);
      details.add("Comida generada: " + formatBigDecimalString(generated));
      logs.add(new CaravanDayCycleLogEntryView("batidor", batidor.fullName(), details, generated));
    }
    if (!batidores.isEmpty()) {
      var summaryDetails = new ArrayList<String>();
      summaryDetails.add("Comida generada por batidores: " + formatBigDecimal(total));
      appendTeamworkSummary(summaryDetails, "batidores", teamwork);
      logs.add(new CaravanDayCycleLogEntryView(
          "batidor-summary",
          "Resumen de batidores",
          summaryDetails,
          total));
    }
    return total;
  }

  private void moveRegularSuppliesToInventory(
      List<CaravanCargo> currentCargo,
      List<TempCargoItem> tempInventory,
      List<CaravanDayCycleLogEntryView> logs) {
    var supplies = currentCargo.stream().filter(this::isSupply).sorted(cargoOrder()).toList();
    for (var supply : supplies) {
      tempInventory.add(TempCargoItem.fromCargo(supply));
      currentCargo.remove(supply);
    }
    if (!supplies.isEmpty()) {
      logs.add(new CaravanDayCycleLogEntryView(
          "inventory",
          "Suministros movidos al inventario temporal",
          List.of("Unidades trasladadas: " + supplies.size()),
          BigDecimal.ZERO));
    }
  }

  private void addLeftoverAsPerishable(
      UUID caravanId,
      BigDecimal leftoverFood,
      List<TempCargoItem> tempInventory,
      List<CaravanDayCycleLogEntryView> logs) {
    var remaining = leftoverFood;
    var created = 0;
    while (remaining.compareTo(TEN) >= 0) {
      tempInventory.add(TempCargoItem.perishable(caravanId, TEN, null));
      remaining = remaining.subtract(TEN);
      created++;
    }
    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      tempInventory.add(TempCargoItem.perishable(caravanId, remaining, null));
      created++;
    }
    logs.add(new CaravanDayCycleLogEntryView(
        "leftover",
        "Comida sobrante convertida",
        List.of(
            "Comida sobrante: " + formatBigDecimal(leftoverFood),
            "Unidades perecederas creadas: " + created),
        leftoverFood));
  }

  private void applyPerishableDecayAndPrepareInventory(
      List<CaravanCargo> currentCargo,
      List<TempCargoItem> tempInventory,
      List<CaravanDayCycleLogEntryView> logs) {
    var perishables = currentCargo.stream().filter(this::isPerishableSupply).sorted(cargoOrder()).toList();
    for (var perishable : perishables) {
      tempInventory.add(TempCargoItem.fromCargo(perishable));
      currentCargo.remove(perishable);
    }
    if (!perishables.isEmpty()) {
      logs.add(new CaravanDayCycleLogEntryView(
          "inventory",
          "Suministros perecederos trasladados al inventario temporal",
          List.of("Unidades trasladadas: " + perishables.size()),
          BigDecimal.ZERO));
    }
  }

  private List<CaravanCargo> rebuildCargo(
      List<CaravanCargo> currentCargo,
      List<TempCargoItem> tempInventory,
      Map<UUID, CaravanCargoSummaryView> summaryByWagon,
      List<CaravanWagon> wagons,
      List<CaravanDayCycleLogEntryView> logs,
      List<String> warnings,
      Instant now) {
    var wagonNameById = wagons.stream().collect(java.util.stream.Collectors.toMap(
        CaravanWagon::id,
        wagon -> wagon.displayNameOr(resolveWagonDisplayName(wagon)),
        (left, right) -> left,
        LinkedHashMap::new));
    var availableByWagon = new HashMap<UUID, Integer>();
    for (var wagon : wagons) {
      var summary = summaryByWagon.get(wagon.id());
      availableByWagon.put(wagon.id(), summary == null ? 0 : summary.remainingCargoUnits());
    }
    for (var item : tempInventory) {
      if (item.sourceWagonId() != null) {
        availableByWagon.merge(item.sourceWagonId(), 1, Integer::sum);
      }
    }

    var regularItems = tempInventory.stream().filter(item -> item.type() == TempCargoType.REGULAR).toList();
    var perishableItems = tempInventory.stream().filter(item -> item.type() == TempCargoType.PERISHABLE).toList();

    var rebuilt = new ArrayList<CaravanCargo>(currentCargo);

    for (var item : regularItems) {
      var wagonId = chooseWagon(availableByWagon, wagons);
      if (wagonId == null) {
        warnings.addAll(describeDiscardedInventory(tempInventory, item));
        break;
      }
      rebuilt.add(createCargoItem(item, wagonId, now));
      decrementCapacity(availableByWagon, wagonId);
      logs.add(new CaravanDayCycleLogEntryView(
          "cargo",
          "Suministros reasignados",
          List.of("Asignados a carro: " + wagonNameById.getOrDefault(wagonId, wagonId.toString()),
              "Comida: " + formatBigDecimal(item.foodAmount())),
          item.foodAmount()));
    }

    if (warnings.isEmpty()) {
      for (var item : perishableItems) {
        var decayed = item.foodAmount().subtract(HALF);
        logs.add(new CaravanDayCycleLogEntryView(
            "cargo",
            "Suministro perecedero degradado",
            List.of("Comida antes: " + formatBigDecimal(item.foodAmount()), "Comida después: " + formatBigDecimal(decayed)),
            decayed));
        if (decayed.compareTo(BigDecimal.ZERO) <= 0) {
          continue;
        }
        var wagonId = chooseWagon(availableByWagon, wagons);
        if (wagonId == null) {
          warnings.addAll(describeDiscardedInventory(tempInventory, item));
          break;
        }
        rebuilt.add(createCargoItem(item.withFoodAmount(decayed), wagonId, now));
        decrementCapacity(availableByWagon, wagonId);
      }
    }

    return rebuilt;
  }

  private List<String> describeDiscardedInventory(List<TempCargoItem> tempInventory, TempCargoItem currentItem) {
    var remaining = new ArrayList<String>();
    remaining.add(currentItem.type().name().toLowerCase() + ":" + formatBigDecimalString(currentItem.foodAmount()));
    tempInventory.stream()
        .filter(item -> item.sequence() > currentItem.sequence())
        .map(item -> item.type().name().toLowerCase() + ":" + formatBigDecimalString(item.foodAmount()))
        .forEach(remaining::add);
    return List.of("No hay carros con capacidad disponible para continuar.", "Inventario descartado: " + String.join(", ", remaining));
  }

  private CaravanCargo createCargoItem(TempCargoItem item, UUID wagonId, Instant now) {
    var catalogItem = CargoCatalog.findByCode(item.catalogCode()).orElseThrow();
    return new CaravanCargo(
        UUID.randomUUID(),
        item.caravanId(),
        CaravanCargoSourceType.CATALOG,
        item.catalogCode(),
        catalogItem.name(),
        catalogItem.category(),
        1,
        1,
        item.foodAmount(),
        false,
        wagonId,
        item.origin(),
        item.specificCommodity(),
        item.deity(),
        item.notes(),
        now,
        now);
  }

  private String resolveWagonDisplayName(CaravanWagon wagon) {
    return WagonCatalog.findByCode(wagon.wagonTypeCode())
        .map(WagonType::name)
        .orElse(wagon.wagonTypeCode());
  }

  private UUID chooseWagon(Map<UUID, Integer> availableByWagon, List<CaravanWagon> wagons) {
    return wagons.stream()
        .filter(wagon -> availableByWagon.getOrDefault(wagon.id(), 0) > 0)
        .sorted(Comparator
            .comparing((CaravanWagon wagon) -> !isSuppliesWagon(wagon))
            .thenComparing((CaravanWagon wagon) -> availableByWagon.getOrDefault(wagon.id(), 0), Comparator.reverseOrder())
            .thenComparing(CaravanWagon::id))
        .map(CaravanWagon::id)
        .findFirst()
        .orElse(null);
  }

  private void decrementCapacity(Map<UUID, Integer> availableByWagon, UUID wagonId) {
    availableByWagon.computeIfPresent(wagonId, (ignored, current) -> Math.max(0, current - 1));
  }

  private CaravanTraveler findServantFor(List<CaravanTraveler> travelers, UUID targetTravelerId) {
    return travelers.stream()
        .filter(traveler -> traveler.hasActiveRole(SERVANT_CODE))
        .filter(traveler -> traveler.roleSpecificData() != null)
        .filter(traveler -> targetTravelerId.equals(traveler.roleSpecificData().servedTravelerId()))
        .findFirst()
        .orElse(null);
  }

  private boolean canExerciseRole(CaravanTraveler traveler, String roleCode) {
    return traveler != null
        && roleCode != null
        && traveler.availableRoleCodes() != null
        && traveler.availableRoleCodes().contains(roleCode);
  }

  private List<CookCandidate> resolveCooks(List<CaravanTraveler> travelers, TeamworkAssignment teamwork) {
    return travelers.stream()
        .filter(traveler -> traveler.hasActiveRole(COOK_CODE))
        .map(traveler -> {
          var servant = findServantFor(travelers, traveler.id());
          var priority = servant == null ? 3 : canExerciseRole(servant, COOK_CODE) ? 1 : 2;
          return new CookCandidate(traveler, servant, priority, teamwork.appliesTo(traveler.id()));
        })
        .sorted(Comparator
            .comparingInt(CookCandidate::priority)
            .thenComparing(candidate -> candidate.traveler().fullName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(candidate -> candidate.traveler().id()))
        .toList();
  }

  private CookCandidate takeNextCook(List<CookCandidate> cooks) {
    for (var cook : cooks) {
      if (!cook.used()) {
        cook.used(true);
        return cook;
      }
    }
    return null;
  }

  private BigDecimal resolveCookOutput(
      CookCandidate cook,
      boolean portableKitchenUsed,
      List<CaravanDayCycleLogEntryView> logs,
      TeamworkAssignment teamwork) {
    if (cook == null) {
      return TEN;
    }
    var multiplier = BigDecimal.ONE;
    var details = new ArrayList<String>();
    details.add("Cocinero: " + cook.traveler().fullName());
    if (cook.servant() != null) {
      multiplier = multiplier.add(HALF);
      var servantCanCook = canExerciseRole(cook.servant(), COOK_CODE);
      details.add(servantCanCook
          ? "Sirviente: " + cook.servant().fullName() + " (cocinero): +0.5"
          : "Sirviente: " + cook.servant().fullName() + " (no puede ser cocinero): +0.5");
      if (servantCanCook) {
        multiplier = multiplier.add(HALF);
        details.add("El sirviente también puede ser cocinero: +0.5 adicional");
      }
    }
    if (portableKitchenUsed) {
      multiplier = multiplier.add(ONE);
      details.add("Cocina portátil aplicada: +100%");
    }
    if (cook.teamworkApplied()) {
      multiplier = multiplier.add(teamwork.cookBonus());
      appendTeamworkDetails(details, teamwork);
    }
    var food = FIFTEEN.multiply(multiplier);
    details.add("Comida obtenida por esta unidad: " + formatBigDecimalString(food));
    logs.add(new CaravanDayCycleLogEntryView("cook", cook.traveler().fullName(), details, food));
    return food;
  }

  private TempCargoItem takeNextSupply(List<TempCargoItem> tempInventory) {
    for (var index = 0; index < tempInventory.size(); index++) {
      var item = tempInventory.get(index);
      if (item.type() == TempCargoType.REGULAR) {
        tempInventory.remove(index);
        return item;
      }
    }
    return null;
  }

  private boolean hasActiveTeamworkFeat(List<CaravanFeat> feats) {
    return feats.stream().anyMatch(feat -> feat.active() && TEAMWORK_FEAT_CODE.equals(feat.featTypeCode()));
  }

  private TeamworkAssignment resolveTeamworkAssignment(List<CaravanTraveler> travelers, String roleCode, boolean enabled) {
    if (!enabled) {
      return TeamworkAssignment.disabled(roleCode);
    }
    var selectedTravelerIds = travelers.stream()
        .filter(traveler -> traveler.hasActiveRole(roleCode))
        .map(traveler -> {
          var servant = findServantFor(travelers, traveler.id());
          var priority = servant == null ? 3 : canExerciseRole(servant, roleCode) ? 1 : 2;
          return new TeamworkCandidate(traveler.id(), priority, traveler.fullName());
        })
        .sorted(Comparator
            .comparingInt(TeamworkCandidate::priority)
            .thenComparing(TeamworkCandidate::travelerFullName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TeamworkCandidate::travelerId))
        .limit(3)
        .map(TeamworkCandidate::travelerId)
        .toList();
    return TeamworkAssignment.of(roleCode, selectedTravelerIds);
  }

  private void appendTeamworkDetails(List<String> details, TeamworkAssignment teamwork) {
    if (!teamwork.hasBonus()) {
      return;
    }
    details.add("Trabajo en equipo aplicado.");
  }

  private void appendTeamworkSummary(List<String> details, String roleLabel, TeamworkAssignment teamwork) {
    if (!teamwork.hasBonus()) {
      return;
    }
    details.add("Trabajo en equipo en " + roleLabel + ": "
        + teamwork.beneficiaryCount()
        + " beneficiados, x"
        + formatBigDecimalString(teamwork.multiplier()));
  }

  private void appendCookTeamworkSummary(List<CaravanDayCycleLogEntryView> logs, TeamworkAssignment teamwork) {
    if (!teamwork.hasBonus()) {
      return;
    }
    logs.add(new CaravanDayCycleLogEntryView(
        "cook-summary",
        "Trabajo en equipo en cocineros",
        List.of(
            "Beneficiarios: " + teamwork.beneficiaryCount(),
            "Multiplicador aplicado: x" + formatBigDecimalString(ONE.add(teamwork.cookBonus()))),
        BigDecimal.ZERO));
  }

  private Map<UUID, BigDecimal> loadAgricultorProgress(String sharedState) {
    var result = new HashMap<UUID, BigDecimal>();
    if (sharedState == null || sharedState.isBlank()) {
      return result;
    }
    if (!sharedState.startsWith("agricultorProgress=")) {
      return result;
    }
    var payload = sharedState.substring("agricultorProgress=".length());
    if (payload.isBlank()) {
      return result;
    }
    for (var entry : payload.split(";")) {
      if (entry.isBlank() || !entry.contains(":")) {
        continue;
      }
      var parts = entry.split(":", 2);
      try {
        result.put(UUID.fromString(parts[0]), new BigDecimal(parts[1]));
      } catch (RuntimeException ignored) {
        // Ignore malformed state and continue with the rest.
      }
    }
    return result;
  }

  private String serializeProgress(Map<UUID, BigDecimal> progressState) {
    if (progressState.isEmpty()) {
      return null;
    }
    var builder = new StringBuilder("agricultorProgress=");
    var first = true;
    for (var entry : progressState.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
      if (!first) {
        builder.append(';');
      }
      first = false;
      builder.append(entry.getKey()).append(':').append(entry.getValue().stripTrailingZeros().toPlainString());
    }
    return builder.toString();
  }

  private String serializeLogs(List<CaravanDayCycleLogEntryView> logs) {
    var builder = new StringBuilder();
    builder.append('[');
    for (var index = 0; index < logs.size(); index++) {
      var entry = logs.get(index);
      if (index > 0) {
        builder.append(',');
      }
      builder.append('{')
          .append("\"section\":\"").append(escapeJson(entry.section())).append("\",")
          .append("\"title\":\"").append(escapeJson(entry.title())).append("\",")
          .append("\"details\":[");
      for (var detailIndex = 0; detailIndex < entry.details().size(); detailIndex++) {
        if (detailIndex > 0) {
          builder.append(',');
        }
        builder.append('\"').append(escapeJson(entry.details().get(detailIndex))).append('\"');
      }
      builder.append("],")
          .append("\"foodDelta\":").append(entry.foodDelta().stripTrailingZeros().toPlainString())
          .append('}');
    }
    builder.append(']');
    return builder.toString();
  }

  private String serializeWarnings(List<String> warnings) {
    var builder = new StringBuilder();
    builder.append('[');
    for (var index = 0; index < warnings.size(); index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append('\"').append(escapeJson(warnings.get(index))).append('\"');
    }
    builder.append(']');
    return builder.toString();
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private String fingerprint(DayCycleState state) {
    var builder = new StringBuilder();
    builder.append(state.caravan().id()).append('|').append(state.caravan().updatedAt()).append('|');
    builder.append(state.supplyState().updatedAt()).append('|').append(state.supplyState().daysPassed()).append('|');
    state.feats().stream().sorted(Comparator.comparing(CaravanFeat::id)).forEach(feat ->
        builder.append(feat.id()).append(':').append(feat.updatedAt()).append(':').append(feat.featTypeCode()).append(':')
            .append(feat.active()).append('|'));
    state.travelers().stream().sorted(Comparator.comparing(CaravanTraveler::id)).forEach(traveler ->
        builder.append(traveler.id()).append(':').append(traveler.updatedAt()).append(':').append(traveler.activeRoleCode()).append('|'));
    state.wagons().stream().sorted(Comparator.comparing(CaravanWagon::id)).forEach(wagon ->
        builder.append(wagon.id()).append(':').append(wagon.updatedAt()).append(':').append(wagon.wagonTypeCode()).append('|'));
    state.cargo().stream().sorted(Comparator.comparing(CaravanCargo::id)).forEach(cargo ->
        builder.append(cargo.id()).append(':').append(cargo.updatedAt()).append(':').append(cargo.catalogCode()).append(':')
            .append(cargo.quantity()).append(':').append(cargo.cargoUnits()).append(':')
            .append(cargo.currentProvisions()).append(':').append(cargo.dayPassed()).append(':')
            .append(cargo.wagonId()).append('|'));
    return sha256(builder.toString());
  }

  private String sha256(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      var hex = new StringBuilder(hash.length * 2);
      for (var b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  private BigDecimal sumPerishableFood(List<CaravanCargo> cargo) {
    return cargo.stream()
        .filter(this::isPerishableSupply)
        .map(CaravanCargo::currentProvisions)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private int countCargoUnits(List<CaravanCargo> cargo, String catalogCode) {
    return cargo.stream()
        .filter(entry -> catalogCode.equals(entry.catalogCode()))
        .mapToInt(CaravanCargo::quantity)
        .sum();
  }

  private boolean isSupply(CaravanCargo cargo) {
    return SUPPLIES_CODE.equals(cargo.catalogCode());
  }

  private boolean isPerishableSupply(CaravanCargo cargo) {
    return PERISHABLE_SUPPLIES_CODE.equals(cargo.catalogCode());
  }

  private boolean isSuppliesWagon(CaravanWagon wagon) {
    return "carro-de-suministros".equals(wagon.wagonTypeCode());
  }

  private Comparator<CaravanTraveler> travelerOrder() {
    return Comparator.comparing(CaravanTraveler::fullName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(CaravanTraveler::id);
  }

  private Comparator<CaravanCargo> cargoOrder() {
    return Comparator.comparing(CaravanCargo::createdAt)
        .thenComparing(CaravanCargo::id);
  }

  private BigDecimal formatBigDecimal(BigDecimal value) {
    return value.setScale(Math.max(0, value.scale()), RoundingMode.HALF_UP);
  }

  private String formatBigDecimalString(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private record DayCycleState(
      com.gestioncaravana.domain.CaravanCampaign caravan,
      CaravanSupplyState supplyState,
      List<CaravanTraveler> travelers,
      List<CaravanWagon> wagons,
      List<CaravanCargo> cargo,
      List<CaravanFeat> feats,
      List<CaravanCargoSummaryView> cargoSummaries,
      com.gestioncaravana.application.model.CaravanStatisticsView statistics) {}

  private record DayCycleSimulation(
      CaravanDayCycleResult result,
      CaravanDayCyclePreviewView previewView,
      List<CaravanCargo> updatedCargo,
      CaravanSupplyState nextSupplyState,
      List<CaravanDayCycleLogEntryView> logs,
      List<String> warnings) {}

  private record MultiDayCycleSimulation(
      CaravanMultiDayCyclePreviewView previewView,
      List<DayCycleSimulation> daySimulations,
      DayCycleState finalState) {}

  private record TeamworkCandidate(UUID travelerId, int priority, String travelerFullName) {}

  private record TeamworkAssignment(
      String roleCode,
      List<UUID> beneficiaryTravelerIds,
      BigDecimal multiplier,
      BigDecimal cookBonus) {

    static TeamworkAssignment disabled(String roleCode) {
      return new TeamworkAssignment(roleCode, List.of(), ONE, BigDecimal.ZERO);
    }

    static TeamworkAssignment of(String roleCode, List<UUID> beneficiaryTravelerIds) {
      var count = beneficiaryTravelerIds.size();
      var multiplier = ONE.add(QUARTER.multiply(BigDecimal.valueOf(Math.max(0, count - 1L))));
      var cookBonus = HALF.multiply(BigDecimal.valueOf(Math.max(0, count - 1L)));
      return new TeamworkAssignment(roleCode, List.copyOf(beneficiaryTravelerIds), multiplier, cookBonus);
    }

    boolean appliesTo(UUID travelerId) {
      return hasBonus() && beneficiaryTravelerIds.contains(travelerId);
    }

    boolean hasBonus() {
      return beneficiaryTravelerIds.size() >= 2;
    }

    int beneficiaryCount() {
      return beneficiaryTravelerIds.size();
    }
  }

  private enum TempCargoType {
    REGULAR,
    PERISHABLE
  }

  private record TempCargoItem(
      TempCargoType type,
      String catalogCode,
      BigDecimal foodAmount,
      UUID sourceWagonId,
      UUID caravanId,
      String origin,
      String specificCommodity,
      String deity,
      String notes,
      long sequence) {

    private static long SEQUENCE = 0;

    static TempCargoItem regular(UUID caravanId, String catalogCode, BigDecimal foodAmount, UUID sourceWagonId) {
      return new TempCargoItem(TempCargoType.REGULAR, catalogCode, foodAmount, sourceWagonId, caravanId, null, null, null, null, nextSequence());
    }

    static TempCargoItem fromCargo(CaravanCargo cargo) {
      return new TempCargoItem(
          cargo.catalogCode() == null || SUPPLIES_CODE.equals(cargo.catalogCode())
              ? TempCargoType.REGULAR
              : TempCargoType.PERISHABLE,
          cargo.catalogCode(),
          cargo.currentProvisions() == null ? BigDecimal.ZERO : cargo.currentProvisions(),
          cargo.wagonId(),
          cargo.caravanId(),
          cargo.origin(),
          cargo.specificCommodity(),
          cargo.deity(),
          cargo.notes(),
          nextSequence());
    }

    static TempCargoItem perishable(UUID caravanId, BigDecimal foodAmount, UUID sourceWagonId) {
      return new TempCargoItem(TempCargoType.PERISHABLE, PERISHABLE_SUPPLIES_CODE, foodAmount, sourceWagonId, caravanId, null, null, null, null, nextSequence());
    }

    TempCargoItem withFoodAmount(BigDecimal foodAmount) {
      return new TempCargoItem(type, catalogCode, foodAmount, sourceWagonId, caravanId, origin, specificCommodity, deity, notes, sequence);
    }

    private static synchronized long nextSequence() {
      return ++SEQUENCE;
    }
  }

  private static final class CookCandidate {
    private final CaravanTraveler traveler;
    private final CaravanTraveler servant;
    private final int priority;
    private final boolean teamworkApplied;
    private boolean used;

    private CookCandidate(CaravanTraveler traveler, CaravanTraveler servant, int priority, boolean teamworkApplied) {
      this.traveler = traveler;
      this.servant = servant;
      this.priority = priority;
      this.teamworkApplied = teamworkApplied;
    }

    private CaravanTraveler traveler() {
      return traveler;
    }

    private CaravanTraveler servant() {
      return servant;
    }

    private int priority() {
      return priority;
    }

    private boolean teamworkApplied() {
      return teamworkApplied;
    }

    private boolean used() {
      return used;
    }

    private void used(boolean used) {
      this.used = used;
    }
  }
}
