package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanTravelerView;
import com.gestioncaravana.application.model.TravelerRoleCatalogItemView;
import com.gestioncaravana.application.port.in.AddCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.GetCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.ListCaravanTravelersUseCase;
import com.gestioncaravana.application.port.in.ListTravelerRoleCatalogUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerRoleUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerWagonUseCase;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.TravelerContract;
import com.gestioncaravana.domain.TravelerRoleCatalog;
import com.gestioncaravana.domain.TravelerRoleCatalogItem;
import com.gestioncaravana.domain.TravelerRoleData;
import com.gestioncaravana.domain.WagonImprovementCatalog;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TravelerManagementService
    implements ListTravelerRoleCatalogUseCase,
        ListCaravanTravelersUseCase,
        GetCaravanTravelerUseCase,
        AddCaravanTravelerUseCase,
        DeleteCaravanTravelerUseCase,
        UpdateCaravanTravelerUseCase,
        UpdateCaravanTravelerWagonUseCase,
        UpdateCaravanTravelerRoleUseCase {

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanWagonImprovementRepositoryPort improvementRepository;
  private final CaravanBeastRepositoryPort beastRepository;
  private final Clock clock;

  public TravelerManagementService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort improvementRepository,
      CaravanBeastRepositoryPort beastRepository,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.travelerRepository = travelerRepository;
    this.wagonRepository = wagonRepository;
    this.improvementRepository = improvementRepository;
    this.beastRepository = beastRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TravelerRoleCatalogItemView> list() {
    return TravelerRoleCatalog.all().stream().map(this::toView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanTravelerView> list(UUID caravanId, String query, String roleCode, UUID wagonId) {
    requireCaravan(caravanId);
    var normalizedQuery = query == null ? "" : query.trim().toLowerCase();
    return travelerRepository.findAllByCaravanId(caravanId).stream()
        .filter(traveler -> normalizedQuery.isBlank() || traveler.fullName().toLowerCase().contains(normalizedQuery))
        .filter(traveler -> roleCode == null || roleCode.isBlank()
            || traveler.activeRoleCodes().contains(roleCode)
            || traveler.activeRoleCode().equals(roleCode))
        .filter(traveler -> wagonId == null || wagonId.equals(traveler.wagonId()))
        .sorted((left, right) -> left.fullName().compareToIgnoreCase(right.fullName()))
        .map(this::toView)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanTravelerView getById(UUID caravanId, UUID travelerId) {
    requireCaravan(caravanId);
    return toView(requireTraveler(caravanId, travelerId));
  }

  @Override
  public CaravanTravelerView execute(UUID caravanId, AddCaravanTravelerCommand command) {
    requireCaravan(caravanId);
    var availableRoleCodes = ensurePassengerRole(command.availableRoleCodes());
    if (availableRoleCodes == null || availableRoleCodes.isEmpty()) {
      throw new IllegalArgumentException("availableRoleCodes is required");
    }

    var now = clock.instant();
    var contract = resolveContract(command.salary(), command.contractConditions(), now);
    var activeRoleCodes = command.activeRoleCodes() == null || command.activeRoleCodes().isEmpty()
        ? List.of(com.gestioncaravana.domain.TravelerRoleCatalog.PASSENGER_CODE)
        : command.activeRoleCodes();
    var activeRoleCode = command.activeRoleCode() == null || command.activeRoleCode().isBlank()
        ? activeRoleCodes.getFirst()
        : command.activeRoleCode();
    var maxActiveRoleCount = command.maxActiveRoleCount() == null
        ? Math.max(1, activeRoleCodes.size())
        : command.maxActiveRoleCount();
    if (activeRoleCodes.size() > maxActiveRoleCount) {
      throw new IllegalArgumentException("activeRoleCodes cannot exceed maxActiveRoleCount");
    }
    var roleData = activeRoleCodes.stream().anyMatch(TravelerRoleCatalog::requiresTargetTraveler)
        ? new TravelerRoleData(validateRoleTarget(caravanId, null, command.servedTravelerId()).id())
        : TravelerRoleData.empty();
    var traveler = CaravanTraveler.create(
        UUID.randomUUID(),
        caravanId,
        command.fullName(),
        command.description(),
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleData,
        null,
        contract,
        command.consumption() == null ? 1 : command.consumption(),
        now);
    return toView(travelerRepository.save(traveler));
  }

  @Override
  public void delete(UUID caravanId, UUID travelerId) {
    requireCaravan(caravanId);
    requireTraveler(caravanId, travelerId);

    var dependentTravelers = travelerRepository.findAllByCaravanId(caravanId).stream()
        .filter(other -> travelerId.equals(other.roleSpecificData() == null ? null : other.roleSpecificData().servedTravelerId()))
        .toList();

    var blockingDependents = dependentTravelers.stream()
        .filter(other -> other.activeRoleCodes().stream().anyMatch(TravelerRoleCatalog::requiresTargetTraveler))
        .map(CaravanTraveler::fullName)
        .toList();
    if (!blockingDependents.isEmpty()) {
      throw new IllegalArgumentException("No se puede eliminar este viajero porque es objetivo de: " + String.join(", ", blockingDependents));
    }

    dependentTravelers.stream()
        .map(other -> other.changeRoles(
            other.activeRoleCodes(),
            other.activeRoleCode(),
            other.maxActiveRoleCount(),
            com.gestioncaravana.domain.TravelerRoleData.empty(),
            clock.instant()))
        .forEach(travelerRepository::save);

    travelerRepository.deleteByCaravanIdAndId(caravanId, travelerId);
  }

  @Override
  public CaravanTravelerView execute(UUID caravanId, UUID travelerId, UpdateCaravanTravelerCommand command) {
    requireCaravan(caravanId);
    var traveler = requireTraveler(caravanId, travelerId);
    var availableRoleCodes = command.availableRoleCodes() == null || command.availableRoleCodes().isEmpty()
        ? traveler.availableRoleCodes()
        : ensurePassengerRole(command.availableRoleCodes());
    if (availableRoleCodes == null || availableRoleCodes.isEmpty()) {
      throw new IllegalArgumentException("availableRoleCodes is required");
    }
    var activeRoleCodes = command.activeRoleCodes() == null || command.activeRoleCodes().isEmpty()
        ? List.of(com.gestioncaravana.domain.TravelerRoleCatalog.PASSENGER_CODE)
        : command.activeRoleCodes();
    var activeRoleCode = command.activeRoleCode() == null || command.activeRoleCode().isBlank()
        ? activeRoleCodes.getFirst()
        : command.activeRoleCode();
    var maxActiveRoleCount = command.maxActiveRoleCount() == null
        ? Math.max(1, activeRoleCodes.size())
        : command.maxActiveRoleCount();
    if (maxActiveRoleCount < 1) {
      throw new IllegalArgumentException("maxActiveRoleCount must be greater than or equal to 1");
    }
    if (activeRoleCodes.size() > maxActiveRoleCount) {
      throw new IllegalArgumentException("activeRoleCodes cannot exceed maxActiveRoleCount");
    }
    if (activeRoleCodes.stream().anyMatch(code -> !availableRoleCodes.contains(code))) {
      throw new IllegalArgumentException("Role not available: " + activeRoleCodes);
    }
    var contract = resolveContract(command.salary(), command.contractConditions(), clock.instant());
    var roleData = activeRoleCodes.stream().anyMatch(TravelerRoleCatalog::requiresTargetTraveler)
        ? new TravelerRoleData(validateRoleTarget(caravanId, travelerId, command.servedTravelerId()).id())
        : TravelerRoleData.empty();

    var wagonId = command.wagonId();
    if (wagonId != null) {
    var wagon = wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));
    var currentCount = travelerRepository.countByCaravanIdAndWagonId(caravanId, wagonId);
    var beastCount = beastRepository.findAllByCaravanIdAndWagonIdAndAssignmentType(
        caravanId, wagonId, com.gestioncaravana.domain.CaravanBeastAssignmentType.TRAVELER).size();
    if (traveler.wagonId() == null || !traveler.wagonId().equals(wagonId)) {
      if (currentCount + beastCount >= currentWagonCapacity(caravanId, wagon.id())) {
        throw new IllegalArgumentException("Wagon capacity reached");
      }
    }
    }

    var updated = traveler.updateDetails(
        command.fullName(),
        command.description(),
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleData,
        wagonId,
        contract,
        command.consumption() == null ? traveler.consumption() : command.consumption(),
        clock.instant());
    return toView(travelerRepository.save(updated));
  }

  @Override
  public CaravanTravelerView execute(UUID caravanId, UUID travelerId, UpdateCaravanTravelerWagonCommand command) {
    requireCaravan(caravanId);
    var traveler = requireTraveler(caravanId, travelerId);
    if (command.wagonId() == null) {
      return toView(travelerRepository.save(traveler.assignWagon(null, clock.instant())));
    }

    var wagon = wagonRepository.findById(caravanId, command.wagonId())
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + command.wagonId()));
    var currentCount = travelerRepository.countByCaravanIdAndWagonId(caravanId, command.wagonId());
    var beastCount = beastRepository.findAllByCaravanIdAndWagonIdAndAssignmentType(
        caravanId, command.wagonId(), com.gestioncaravana.domain.CaravanBeastAssignmentType.TRAVELER).size();
    if (traveler.wagonId() == null || !traveler.wagonId().equals(command.wagonId())) {
      if (currentCount + beastCount >= currentWagonCapacity(caravanId, wagon.id())) {
        throw new IllegalArgumentException("Wagon capacity reached");
      }
    }
    return toView(travelerRepository.save(traveler.assignWagon(command.wagonId(), clock.instant())));
  }

  @Override
  public CaravanTravelerView execute(UUID caravanId, UUID travelerId, UpdateCaravanTravelerRoleCommand command) {
    requireCaravan(caravanId);
    var traveler = requireTraveler(caravanId, travelerId);
    var activeRoleCodes = command.activeRoleCodes() == null || command.activeRoleCodes().isEmpty()
        ? List.of(com.gestioncaravana.domain.TravelerRoleCatalog.PASSENGER_CODE)
        : command.activeRoleCodes();
    var activeRoleCode = command.activeRoleCode() == null || command.activeRoleCode().isBlank()
        ? activeRoleCodes.getFirst()
        : command.activeRoleCode();
    if (activeRoleCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
      throw new IllegalArgumentException("activeRoleCodes is required");
    }
    if (command.maxActiveRoleCount() != null && command.maxActiveRoleCount() < 1) {
      throw new IllegalArgumentException("maxActiveRoleCount must be greater than or equal to 1");
    }
    if (activeRoleCodes.stream().anyMatch(code -> !traveler.availableRoleCodes().contains(code))) {
      throw new IllegalArgumentException("Role not available: " + activeRoleCodes);
    }
    var maxActiveRoleCount = command.maxActiveRoleCount() == null ? traveler.maxActiveRoleCount() : command.maxActiveRoleCount();
    if (activeRoleCodes.size() > maxActiveRoleCount) {
      throw new IllegalArgumentException("activeRoleCodes cannot exceed maxActiveRoleCount");
    }

    var roleData = activeRoleCodes.stream().anyMatch(TravelerRoleCatalog::requiresTargetTraveler)
        ? new TravelerRoleData(validateRoleTarget(caravanId, travelerId, command.servedTravelerId()).id())
        : TravelerRoleData.empty();

    return toView(travelerRepository.save(traveler.changeRoles(
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleData,
        clock.instant())));
  }

  private CaravanTraveler requireCaravanTraveler(UUID caravanId, UUID travelerId) {
    return requireTraveler(caravanId, travelerId);
  }

  private CaravanTraveler requireTraveler(UUID caravanId, UUID travelerId) {
    return travelerRepository.findById(caravanId, travelerId)
        .orElseThrow(() -> new IllegalArgumentException("Traveler not found: " + travelerId));
  }

  private com.gestioncaravana.domain.CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private CaravanTraveler requireTargetTraveler(UUID caravanId, UUID targetTravelerId) {
    if (targetTravelerId == null) {
      throw new IllegalArgumentException("servedTravelerId is required");
    }
    return travelerRepository.findById(caravanId, targetTravelerId)
        .orElseThrow(() -> new IllegalArgumentException("Target traveler not found: " + targetTravelerId));
  }

  private CaravanTraveler validateRoleTarget(UUID caravanId, UUID travelerId, UUID targetTravelerId) {
    var target = requireTargetTraveler(caravanId, targetTravelerId);
    if (target.id().equals(travelerId)) {
      throw new IllegalArgumentException("servedTravelerId cannot point to the same traveler");
    }
    if (isTargetAlreadyServedByAnotherTraveler(caravanId, targetTravelerId, travelerId)) {
      throw new IllegalArgumentException("servedTravelerId is already assigned to another servant");
    }
    return target;
  }

  private boolean isTargetAlreadyServedByAnotherTraveler(UUID caravanId, UUID targetTravelerId, UUID excludedTravelerId) {
    return travelerRepository.findAllByCaravanId(caravanId).stream()
        .filter(other -> excludedTravelerId == null || !other.id().equals(excludedTravelerId))
        .filter(other -> other.hasActiveRole("sirviente"))
        .anyMatch(other -> other.roleSpecificData() != null && targetTravelerId.equals(other.roleSpecificData().servedTravelerId()));
  }

  private int currentWagonCapacity(UUID caravanId, UUID wagonId) {
    var wagon = wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));
    var wagonType = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
    var improvements = improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId);
    return deriveTravelerCapacity(wagonType.travelerCapacity(), improvements);
  }

  private int deriveTravelerCapacity(int baseCapacity, List<CaravanWagonImprovement> improvements) {
    var capacity = baseCapacity;
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

  private TravelerRoleCatalogItemView toView(TravelerRoleCatalogItem item) {
    return new TravelerRoleCatalogItemView(
        item.code(),
        item.name(),
        item.description(),
        item.requirements(),
        item.requiresTargetTraveler(),
        item.helperBenefitMode().name(),
        item.helperPeriodDays());
  }

  private List<String> ensurePassengerRole(List<String> roleCodes) {
    if (roleCodes == null || roleCodes.isEmpty()) {
      return roleCodes;
    }
    if (roleCodes.contains(com.gestioncaravana.domain.TravelerRoleCatalog.PASSENGER_CODE)) {
      return roleCodes;
    }
    return List.copyOf(java.util.stream.Stream.concat(
        roleCodes.stream(),
        java.util.stream.Stream.of(com.gestioncaravana.domain.TravelerRoleCatalog.PASSENGER_CODE)).distinct().toList());
  }

  private TravelerContract resolveContract(BigDecimal salary, String conditions, java.time.Instant now) {
    if (salary == null && (conditions == null || conditions.isBlank())) {
      return null;
    }
    return TravelerContract.create(salary, conditions, now);
  }

  private CaravanTravelerView toView(CaravanTraveler traveler) {
    var wagonName = traveler.wagonId() == null
        ? null
        : wagonRepository.findById(traveler.caravanId(), traveler.wagonId())
            .flatMap(wagon -> WagonCatalog.findByCode(wagon.wagonTypeCode()).map(type -> type.name()))
            .orElse(null);
    var roleName = TravelerRoleCatalog.findByCode(traveler.activeRoleCode()).map(TravelerRoleCatalogItem::name).orElse(traveler.activeRoleCode());
    var servedTravelerName = traveler.roleSpecificData() == null || traveler.roleSpecificData().servedTravelerId() == null
        ? null
        : travelerRepository.findById(traveler.caravanId(), traveler.roleSpecificData().servedTravelerId())
            .map(CaravanTraveler::fullName)
            .orElse(null);
    return new CaravanTravelerView(
        traveler.id(),
        traveler.caravanId(),
        traveler.fullName(),
        traveler.description(),
        traveler.availableRoleCodes(),
        traveler.activeRoleCodes(),
        traveler.activeRoleCode(),
        roleName,
        traveler.wagonId(),
        wagonName,
        traveler.maxActiveRoleCount(),
        traveler.contract() == null ? null : traveler.contract().salary(),
        traveler.contract() == null ? null : traveler.contract().conditions(),
        traveler.consumption(),
        traveler.roleSpecificData() == null ? null : traveler.roleSpecificData().servedTravelerId(),
        servedTravelerName,
        traveler.createdAt(),
        traveler.updatedAt());
  }
}
