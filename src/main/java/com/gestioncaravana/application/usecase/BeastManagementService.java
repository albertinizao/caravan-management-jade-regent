package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanBeastCatalogItemView;
import com.gestioncaravana.application.model.CaravanBeastView;
import com.gestioncaravana.application.port.in.AddCaravanBeastUseCase;
import com.gestioncaravana.application.port.in.ClearCaravanBeastAssignmentUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanBeastUseCase;
import com.gestioncaravana.application.port.in.DeleteUnassignedCaravanBeastsUseCase;
import com.gestioncaravana.application.port.in.GetCaravanBeastUseCase;
import com.gestioncaravana.application.port.in.ListBeastCatalogUseCase;
import com.gestioncaravana.application.port.in.ListCaravanBeastsUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanBeastAssignmentUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanBeastUseCase;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastCatalog;
import com.gestioncaravana.domain.CaravanBeastCatalogItem;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.TravelerRoleCatalog;
import com.gestioncaravana.domain.WagonDraftConstraint;
import com.gestioncaravana.domain.WagonImprovementCatalog;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.CaravanWagon;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BeastManagementService
    implements ListBeastCatalogUseCase,
        ListCaravanBeastsUseCase,
        GetCaravanBeastUseCase,
        AddCaravanBeastUseCase,
        DeleteCaravanBeastUseCase,
        DeleteUnassignedCaravanBeastsUseCase,
        UpdateCaravanBeastAssignmentUseCase,
        UpdateCaravanBeastUseCase,
        ClearCaravanBeastAssignmentUseCase {

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanBeastRepositoryPort beastRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanWagonImprovementRepositoryPort improvementRepository;
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final Clock clock;

  public BeastManagementService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanBeastRepositoryPort beastRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort improvementRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.beastRepository = beastRepository;
    this.wagonRepository = wagonRepository;
    this.improvementRepository = improvementRepository;
    this.travelerRepository = travelerRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanBeastCatalogItemView> list() {
    return CaravanBeastCatalog.all().stream().map(this::toView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanBeastView> list(UUID caravanId, String query, String sourceType, String assignmentType, UUID wagonId) {
    requireCaravan(caravanId);
    var normalizedQuery = query == null ? "" : query.trim().toLowerCase();
    return beastRepository.findAllByCaravanId(caravanId).stream()
        .filter(beast -> normalizedQuery.isBlank() || beast.name().toLowerCase().contains(normalizedQuery))
        .filter(beast -> sourceType == null || sourceType.isBlank() || beast.sourceType().name().equalsIgnoreCase(sourceType))
        .filter(beast -> assignmentType == null || assignmentType.isBlank() || beast.assignmentType().name().equalsIgnoreCase(assignmentType))
        .filter(beast -> wagonId == null || wagonId.equals(beast.assignedWagonId()))
        .sorted(Comparator.comparing(CaravanBeast::name, String.CASE_INSENSITIVE_ORDER))
        .map(this::toView)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanBeastView getById(UUID caravanId, UUID beastId) {
    requireCaravan(caravanId);
    return toView(requireBeast(caravanId, beastId));
  }

  @Override
  public CaravanBeastView execute(UUID caravanId, AddCaravanBeastCommand command) {
    requireCaravan(caravanId);
    var now = clock.instant();
    var quantity = command.quantity() == null ? 1 : command.quantity();
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be greater than or equal to 1");
    }
    var sourceType = command.sourceType() == null ? CaravanBeastSourceType.CUSTOM : command.sourceType();
    if (sourceType == CaravanBeastSourceType.CATALOG) {
      var catalogCode = command.catalogBeastCode();
      if (catalogCode == null || catalogCode.isBlank()) {
        throw new IllegalArgumentException("catalogBeastCode is required for catalog beasts");
      }
      var catalogItem = CaravanBeastCatalog.findByCode(catalogCode)
          .orElseThrow(() -> new IllegalArgumentException("Beast type not found: " + catalogCode));
      CaravanBeast saved = null;
      for (var index = 0; index < quantity; index++) {
        saved = beastRepository.save(CaravanBeast.createFromCatalog(UUID.randomUUID(), caravanId, catalogItem, now));
      }
      return toView(saved);
    }

    validateCustomPayload(command);
    CaravanBeast saved = null;
    for (var index = 0; index < quantity; index++) {
      saved = beastRepository.save(CaravanBeast.createCustom(
          UUID.randomUUID(),
          caravanId,
          command.name(),
          command.size(),
          command.strength(),
          command.speed(),
          command.thermalAdaptation(),
          command.basePrice(),
          command.trainedPrice(),
          command.fourLegged() != null && command.fourLegged(),
          command.specialNote(),
          command.description(),
          command.customNotes(),
          command.consumption(),
          command.occupiedSpace(),
          now));
    }
    return toView(saved);
  }

  @Override
  public void delete(UUID caravanId, UUID beastId) {
    requireCaravan(caravanId);
    requireBeast(caravanId, beastId);
    beastRepository.deleteByCaravanIdAndId(caravanId, beastId);
  }

  @Override
  public void delete(UUID caravanId) {
    requireCaravan(caravanId);
    beastRepository.findAllByCaravanIdAndAssignmentType(caravanId, CaravanBeastAssignmentType.NONE).stream()
        .map(CaravanBeast::id)
        .forEach(beastId -> beastRepository.deleteByCaravanIdAndId(caravanId, beastId));
  }

  @Override
  public CaravanBeastView execute(UUID caravanId, UUID beastId, UpdateCaravanBeastAssignmentCommand command) {
    requireCaravan(caravanId);
    var beast = requireBeast(caravanId, beastId);
    if (command.assignmentType() == null || command.assignmentType() == CaravanBeastAssignmentType.NONE) {
      return toView(beastRepository.save(beast.clearAssignment(clock.instant())));
    }

    if (beast.assignmentType() != CaravanBeastAssignmentType.NONE
        && beast.assignmentType() != command.assignmentType()) {
      throw new IllegalArgumentException("Beast must be unassigned before changing assignment type");
    }

    var wagonId = command.wagonId();
    if (wagonId == null) {
      throw new IllegalArgumentException("wagonId is required");
    }
    var wagon = wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));

    if (command.assignmentType() == CaravanBeastAssignmentType.DRAFT) {
      validateDraftAssignment(caravanId, beast, wagon);
      return toView(beastRepository.save(beast.assignDraft(wagonId, clock.instant())));
    }

    if (command.assignmentType() == CaravanBeastAssignmentType.TRAVELER) {
      validateTravelerAssignment(caravanId, beast, wagon, beast.assignedWagonId());
      var availableRoleCodes = beast.sourceType() == CaravanBeastSourceType.CUSTOM
          ? normalizeTravelerAvailableRoleCodes(command.availableRoleCodes())
          : null;
      var activeRoleCode = beast.sourceType() == CaravanBeastSourceType.CUSTOM
          ? normalizeTravelerActiveRoleCode(command.activeRoleCode(), availableRoleCodes)
          : null;
      validateTravelerRoleAssignment(beast, availableRoleCodes, activeRoleCode);
      return toView(beastRepository.save(beast.assignTraveler(
          wagonId,
          availableRoleCodes,
          activeRoleCode,
          clock.instant())));
    }

    throw new IllegalArgumentException("Unsupported assignment type: " + command.assignmentType());
  }

  @Override
  public CaravanBeastView execute(UUID caravanId, UUID beastId, UpdateCaravanBeastCommand command) {
    requireCaravan(caravanId);
    var beast = requireBeast(caravanId, beastId);
    if (beast.sourceType() != CaravanBeastSourceType.CUSTOM) {
      throw new IllegalArgumentException("Only custom beasts can be edited");
    }
    validateCustomEconomy(command);
    return toView(beastRepository.save(beast.updateCustomEconomy(command.consumption(), command.occupiedSpace(), clock.instant())));
  }

  @Override
  public CaravanBeastView execute(UUID caravanId, UUID beastId) {
    requireCaravan(caravanId);
    var beast = requireBeast(caravanId, beastId);
    return toView(beastRepository.save(beast.clearAssignment(clock.instant())));
  }

  private com.gestioncaravana.domain.CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private CaravanBeast requireBeast(UUID caravanId, UUID beastId) {
    return beastRepository.findById(caravanId, beastId)
        .orElseThrow(() -> new IllegalArgumentException("Beast not found: " + beastId));
  }

  private void validateCustomPayload(AddCaravanBeastCommand command) {
    if (command.name() == null || command.name().isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (command.size() == null || command.size().isBlank()) {
      throw new IllegalArgumentException("size is required");
    }
    if (command.strength() == null) {
      throw new IllegalArgumentException("strength is required");
    }
    if (command.speed() == null) {
      throw new IllegalArgumentException("speed is required");
    }
    if (command.fourLegged() == null) {
      throw new IllegalArgumentException("fourLegged is required");
    }
    if (command.specialNote() == null || command.specialNote().isBlank()) {
      throw new IllegalArgumentException("specialNote is required");
    }
    if (command.description() == null || command.description().isBlank()) {
      throw new IllegalArgumentException("description is required");
    }
    if (command.consumption() != null && command.consumption() < 0) {
      throw new IllegalArgumentException("consumption must be greater than or equal to 0");
    }
  }

  private void validateCustomEconomy(UpdateCaravanBeastCommand command) {
    if (command.consumption() != null && command.consumption() < 0) {
      throw new IllegalArgumentException("consumption must be greater than or equal to 0");
    }
    if (command.occupiedSpace() != null) {
      if (command.occupiedSpace().signum() < 0) {
        throw new IllegalArgumentException("occupiedSpace must be greater than or equal to 0");
      }
      if (command.occupiedSpace().compareTo(BigDecimal.valueOf(4)) > 0) {
        throw new IllegalArgumentException("occupiedSpace must be less than or equal to 4");
      }
      if (command.occupiedSpace().multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
        throw new IllegalArgumentException("occupiedSpace must use 0.5 increments");
      }
    }
  }

  private void validateDraftAssignment(UUID caravanId, CaravanBeast beast, CaravanWagon wagon) {
    if (!isDraftEligibleSize(beast.size())) {
      throw new IllegalArgumentException("Beast size is not eligible for draft duty");
    }

    var constraint = draftConstraint(caravanId, wagon);
    var currentDraftBeasts = beastRepository.findAllByCaravanIdAndWagonIdAndAssignmentType(
        caravanId, wagon.id(), CaravanBeastAssignmentType.DRAFT);
    var replacementSet = currentDraftBeasts.stream()
        .filter(existing -> !existing.id().equals(beast.id()))
        .toList();
    var largeCount = replacementSet.stream().filter(existing -> isLarge(existing.size())).count();
    var mediumCount = replacementSet.stream().filter(existing -> isMedium(existing.size())).count();
    var additionalLarge = isLarge(beast.size()) ? 1 : 0;
    var additionalMedium = isMedium(beast.size()) ? 1 : 0;
    var totalLarge = largeCount + additionalLarge;
    var totalMediumSlots = mediumCount + additionalMedium
        + ((largeCount + additionalLarge) * constraint.mediumSlotsConsumedByLargeBeast());

    if (totalLarge > constraint.maxLargeBeasts()) {
      throw new IllegalArgumentException("Wagon draft large-beast limit reached");
    }
    if (totalMediumSlots > constraint.maxMediumBeasts()) {
      throw new IllegalArgumentException("Wagon draft medium-beast limit reached");
    }
  }

  private void validateTravelerAssignment(UUID caravanId, CaravanBeast beast, CaravanWagon wagon, UUID currentWagonId) {
    var currentCount = travelerOccupancySpace(caravanId, wagon.id());
    var alreadyOccupiesTargetSlot = beast.assignmentType() == CaravanBeastAssignmentType.TRAVELER
        && wagon.id().equals(currentWagonId);
    if (!alreadyOccupiesTargetSlot) {
      if (currentCount.add(beast.occupiedSpace()).compareTo(BigDecimal.valueOf(currentWagonCapacity(caravanId, wagon.id()))) > 0) {
        throw new IllegalArgumentException("Wagon capacity reached");
      }
    }
  }

  private void validateTravelerRoleAssignment(
      CaravanBeast beast,
      List<String> availableRoleCodes,
      String activeRoleCode) {
    if (beast.sourceType() != CaravanBeastSourceType.CUSTOM) {
      return;
    }
    if (availableRoleCodes.stream().anyMatch(code -> TravelerRoleCatalog.findByCode(code).isEmpty())) {
      throw new IllegalArgumentException("availableRoleCodes must contain known traveler role codes");
    }
    if (!availableRoleCodes.contains(activeRoleCode)) {
      throw new IllegalArgumentException("activeRoleCode must be one of availableRoleCodes");
    }
  }

  private List<String> normalizeTravelerAvailableRoleCodes(List<String> availableRoleCodes) {
    var normalized = availableRoleCodes == null ? List.<String>of() : availableRoleCodes.stream()
        .filter(code -> code != null && !code.isBlank())
        .distinct()
        .toList();
    if (normalized.contains(TravelerRoleCatalog.PASSENGER_CODE)) {
      return normalized;
    }
    return Stream.concat(Stream.of(TravelerRoleCatalog.PASSENGER_CODE), normalized.stream()).distinct().toList();
  }

  private String normalizeTravelerActiveRoleCode(String activeRoleCode, List<String> availableRoleCodes) {
    if (activeRoleCode == null || activeRoleCode.isBlank()) {
      return TravelerRoleCatalog.PASSENGER_CODE;
    }
    if (availableRoleCodes == null || !availableRoleCodes.contains(activeRoleCode)) {
      throw new IllegalArgumentException("activeRoleCode must be one of availableRoleCodes");
    }
    return activeRoleCode;
  }

  private BigDecimal travelerOccupancySpace(UUID caravanId, UUID wagonId) {
    var travelerSpace = travelerRepository.findAllByCaravanId(caravanId).stream()
        .filter(traveler -> wagonId.equals(traveler.wagonId()))
        .map(com.gestioncaravana.domain.CaravanTraveler::occupiedSpace)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    var beastSpace = beastRepository.findAllByCaravanIdAndWagonIdAndAssignmentType(
        caravanId, wagonId, CaravanBeastAssignmentType.TRAVELER).stream()
        .map(CaravanBeast::occupiedSpace)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return travelerSpace.add(beastSpace);
  }

  private int currentWagonCapacity(UUID caravanId, UUID wagonId) {
    var wagon = wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));
    return deriveTravelerCapacity(caravanId, wagon);
  }

  private int deriveTravelerCapacity(UUID caravanId, CaravanWagon wagon) {
    var capacity = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()))
        .travelerCapacity();
    var improvements = improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagon.id());
    for (var improvement : improvements) {
      var type = WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
          .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode()));
      if (type.travelerCapacityOverride() != null) {
        capacity = type.travelerCapacityOverride();
      } else if (type.travelerCapacityMultiplier() != null) {
        var minIncrement = type.travelerCapacityMinimumIncrement() == null ? 0 : type.travelerCapacityMinimumIncrement();
        capacity += Math.max(minIncrement, (int) Math.round(capacity * (type.travelerCapacityMultiplier() - 1)));
      } else if (type.travelerCapacityBonus() != null) {
        capacity += type.travelerCapacityBonus();
      }
    }
    return Math.max(0, capacity);
  }

  private WagonDraftConstraint draftConstraint(UUID caravanId, CaravanWagon wagon) {
    var type = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
    var constraint = WagonDraftConstraint.parse(type.propulsion());
    var improvements = improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagon.id());
    for (var improvement : improvements) {
      var improvementType = WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
          .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode()));
      if (improvementType.propulsionEffect() != null && !improvementType.propulsionEffect().isBlank()) {
        var draftEffect = WagonDraftConstraint.tryParse(improvementType.propulsionEffect());
        if (draftEffect.isPresent()) {
          constraint = constraint.plus(draftEffect.get());
        }
      }
    }
    return constraint;
  }

  private CaravanBeastCatalogItemView toView(CaravanBeastCatalogItem item) {
    return new CaravanBeastCatalogItemView(
        item.code(),
        item.name(),
        item.basePrice(),
        item.trainedPrice(),
        item.size(),
        item.strength(),
        item.speed(),
        item.thermalAdaptation(),
        item.fourLegged(),
        item.specialNote(),
        item.description(),
        item.notes(),
        item.occupiedSpace());
  }

  private CaravanBeastView toView(CaravanBeast beast) {
    var assignedWagonName = beast.assignedWagonId() == null
        ? null
        : wagonRepository.findById(beast.caravanId(), beast.assignedWagonId())
            .flatMap(wagon -> WagonCatalog.findByCode(wagon.wagonTypeCode()).map(type -> wagon.displayNameOr(type.name())))
            .orElse(null);
    return new CaravanBeastView(
        beast.id(),
        beast.caravanId(),
        beast.sourceType(),
        beast.catalogBeastCode(),
        beast.name(),
        beast.size(),
        beast.strength(),
        beast.speed(),
        beast.thermalAdaptation(),
        beast.basePrice(),
        beast.trainedPrice(),
        beast.fourLegged(),
        beast.specialNote(),
        beast.description(),
        beast.customNotes(),
        beast.consumption(),
        beast.availableRoleCodes(),
        beast.activeRoleCode(),
        beast.assignmentType(),
        beast.assignedWagonId(),
        assignedWagonName,
        beast.createdAt(),
        beast.updatedAt(),
        beast.occupiedSpace());
  }

  private boolean isDraftEligibleSize(String size) {
    return isLarge(size) || isMedium(size);
  }

  private boolean isLarge(String size) {
    return "G".equalsIgnoreCase(size);
  }

  private boolean isMedium(String size) {
    return "M".equalsIgnoreCase(size);
  }
}
