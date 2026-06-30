package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanWagonView;
import com.gestioncaravana.application.model.CaravanBeastView;
import com.gestioncaravana.application.model.CaravanWagonImprovementView;
import com.gestioncaravana.application.model.WagonImprovementCatalogItemView;
import com.gestioncaravana.application.model.WagonCatalogItemView;
import com.gestioncaravana.application.port.in.AddCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanWagonImprovementUseCase;
import com.gestioncaravana.application.port.in.GetCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.ListCaravanWagonImprovementsUseCase;
import com.gestioncaravana.application.port.in.ListCaravanWagonsUseCase;
import com.gestioncaravana.application.port.in.ListWagonImprovementCatalogUseCase;
import com.gestioncaravana.application.port.in.ListWagonCatalogUseCase;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.WagonImprovementCatalog;
import com.gestioncaravana.domain.WagonImprovementType;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.WagonDraftConstraint;
import com.gestioncaravana.domain.WagonType;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WagonManagementService
    implements ListWagonCatalogUseCase,
        ListWagonImprovementCatalogUseCase,
        ListCaravanWagonImprovementsUseCase,
        ListCaravanWagonsUseCase,
        GetCaravanWagonUseCase,
        AddCaravanWagonUseCase,
        AddCaravanWagonImprovementUseCase,
        DeleteCaravanWagonImprovementUseCase,
        DeleteCaravanWagonUseCase {

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanWagonImprovementRepositoryPort improvementRepository;
  private final CaravanBeastRepositoryPort beastRepository;
  private final CaravanCargoRepositoryPort cargoRepository;
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final Clock clock;

  @Autowired
  public WagonManagementService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort improvementRepository,
      CaravanBeastRepositoryPort beastRepository,
      CaravanCargoRepositoryPort cargoRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.wagonRepository = wagonRepository;
    this.improvementRepository = improvementRepository;
    this.beastRepository = beastRepository;
    this.cargoRepository = cargoRepository;
    this.travelerRepository = travelerRepository;
    this.clock = clock;
  }

  WagonManagementService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort improvementRepository,
      CaravanBeastRepositoryPort beastRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      Clock clock) {
    this(
        caravanRepository,
        wagonRepository,
        improvementRepository,
        beastRepository,
        new NoopCaravanCargoRepositoryPort(),
        travelerRepository,
        clock);
  }

  @Override
  @Transactional(readOnly = true)
  public List<WagonCatalogItemView> list() {
    return WagonCatalog.all().stream().map(this::toCatalogView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<WagonImprovementCatalogItemView> listCatalog(UUID caravanId, UUID wagonId) {
    var wagon = requireWagon(caravanId, wagonId);
    var wagonType = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
    var improvements = improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId);
    return WagonImprovementCatalog.all().stream()
        .map(type -> toCatalogView(type, wagonType, improvements))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanWagonImprovementView> listImprovements(UUID caravanId, UUID wagonId) {
    requireWagon(caravanId, wagonId);
    return improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId).stream()
        .map(this::toView)
        .sorted(Comparator.comparing(CaravanWagonImprovementView::createdAt))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanWagonView> list(UUID caravanId) {
    requireCaravan(caravanId);
    return wagonRepository.findAllByCaravanId(caravanId).stream()
        .map(wagon -> toView(wagon, improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagon.id())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanWagonView getById(UUID caravanId, UUID wagonId) {
    requireCaravan(caravanId);
    return wagonRepository.findById(caravanId, wagonId)
        .map(wagon -> toView(wagon, improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId)))
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));
  }

  @Override
  public CaravanWagonView execute(UUID caravanId, AddCaravanWagonCommand command) {
    var caravan = requireCaravan(caravanId);
    var wagonType = WagonCatalog.findByCode(command.wagonTypeCode())
        .orElseThrow(() -> new IllegalArgumentException("Wagon type not found: " + command.wagonTypeCode()));

    var maxWagons = 10 + caravan.level();
    var currentTotal = wagonRepository.countByCaravanId(caravanId);
    if (currentTotal >= maxWagons) {
      throw new IllegalArgumentException("Caravan wagon limit reached");
    }

    var maxOfType = wagonType.limit().resolveMaxAllowed(maxWagons);
    var currentOfType = wagonRepository.countByCaravanIdAndWagonTypeCode(caravanId, wagonType.code());
    if (currentOfType >= maxOfType) {
      throw new IllegalArgumentException("Wagon type limit reached: " + wagonType.name());
    }

    var now = clock.instant();
    var wagon = CaravanWagon.create(UUID.randomUUID(), caravanId, wagonType.code(), now);
    return toView(wagonRepository.save(wagon), List.of());
  }

  @Override
  public CaravanWagonView execute(UUID caravanId, UUID wagonId, AddCaravanWagonImprovementCommand command) {
    requireCaravan(caravanId);
    var wagon = requireWagon(caravanId, wagonId);
    var wagonType = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
    var improvementType = WagonImprovementCatalog.findByCode(command.improvementTypeCode())
        .orElseThrow(() -> new IllegalArgumentException("Improvement type not found: " + command.improvementTypeCode()));
    var existing = improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId);
    validateImprovementAddition(wagonType, improvementType, existing);
    var now = clock.instant();
    improvementRepository.save(CaravanWagonImprovement.create(UUID.randomUUID(), caravanId, wagonId, improvementType.code(), now));
    return toView(wagon, improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId));
  }

  @Override
  public CaravanWagonView execute(UUID caravanId, UUID wagonId, UUID improvementId) {
    requireCaravan(caravanId);
    var wagon = requireWagon(caravanId, wagonId);
    var improvements = improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId);
    var target = improvements.stream()
        .filter(improvement -> improvement.id().equals(improvementId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Improvement not found: " + improvementId));
    validateImprovementRemoval(target, improvements);
    improvementRepository.deleteById(caravanId, wagonId, improvementId);
    return toView(wagon, improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagonId));
  }

  @Override
  public void delete(UUID caravanId, UUID wagonId) {
    requireCaravan(caravanId);
    wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));

    if (cargoRepository.countByCaravanIdAndWagonId(caravanId, wagonId) > 0) {
      throw new IllegalArgumentException("Wagon has cargo assigned");
    }

    beastRepository.findAllByCaravanId(caravanId).stream()
        .filter(beast -> wagonId.equals(beast.assignedWagonId()))
        .forEach(beast -> beastRepository.save(beast.clearAssignment(clock.instant())));

    travelerRepository.findAllByCaravanId(caravanId).stream()
        .filter(traveler -> wagonId.equals(traveler.wagonId()))
        .forEach(traveler -> travelerRepository.save(traveler.assignWagon(null, clock.instant())));

    wagonRepository.deleteById(caravanId, wagonId);
  }

  private com.gestioncaravana.domain.CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private CaravanWagon requireWagon(UUID caravanId, UUID wagonId) {
    return wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));
  }

  private WagonCatalogItemView toCatalogView(WagonType wagonType) {
    return new WagonCatalogItemView(
        wagonType.code(),
        wagonType.name(),
        wagonType.category(),
        wagonType.cost(),
        wagonType.hitPoints(),
        wagonType.hardness(),
        wagonType.propulsion(),
        wagonType.travelerCapacity(),
        wagonType.cargoCapacity(),
        wagonType.limit().kind().name(),
        wagonType.limit().fixedMax(),
        wagonType.limit().ratioDenominator(),
        wagonType.limitLabel(),
        wagonType.consumption(),
        wagonType.specialBenefit(),
        wagonType.description(),
        wagonType.notes());
  }

  private WagonImprovementCatalogItemView toCatalogView(
      WagonImprovementType improvementType,
      WagonType wagonType,
      List<CaravanWagonImprovement> improvements) {
    var ownedCount = (int) improvements.stream()
        .filter(improvement -> improvement.improvementTypeCode().equals(improvementType.code()))
        .count();
    var blockedReason = calculateImprovementBlockedReason(improvementType, wagonType, improvements);
    return new WagonImprovementCatalogItemView(
        improvementType.code(),
        improvementType.name(),
        improvementType.category(),
        improvementType.costExpression(),
        improvementType.hitPointsBonus(),
        improvementType.hitPointsMultiplier(),
        improvementType.hardnessBonus(),
        improvementType.hardnessMultiplier(),
        improvementType.propulsionEffect(),
        improvementType.travelerCapacityBonus(),
        improvementType.travelerCapacityMultiplier(),
        improvementType.travelerCapacityMinimumIncrement(),
        improvementType.travelerCapacityOverride(),
        improvementType.cargoCapacityBonus(),
        improvementType.cargoCapacityMultiplier(),
        improvementType.cargoCapacityMinimumIncrement(),
        improvementType.cargoCapacityOverride(),
        improvementType.consumptionBonus(),
        improvementType.maxPerWagon(),
        improvementType.isRepeatable(),
        improvementType.requiredBasePropulsionFragment(),
        ownedCount,
        blockedReason == null,
        blockedReason,
        improvementType.specialBenefit(),
        improvementType.description(),
        improvementType.notes());
  }

  private CaravanWagonView toView(CaravanWagon wagon, List<CaravanWagonImprovement> improvements) {
    var wagonType = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
    var derived = deriveWagonStats(wagonType, improvements);
    var draftBeasts = draftBeasts(wagon.caravanId(), wagon.id());
    var draftStrength = draftBeasts.stream().mapToInt(this::effectiveDraftStrength).sum();
    return new CaravanWagonView(
        wagon.id(),
        wagon.caravanId(),
        wagon.wagonTypeCode(),
        wagonType.name(),
        wagonType.category(),
        derived.cost(),
        derived.hitPoints(),
        derived.hardness(),
        derived.propulsion(),
        derived.travelerCapacity(),
        derived.cargoCapacity(),
        wagonType.limit().kind().name(),
        wagonType.limit().fixedMax(),
        wagonType.limit().ratioDenominator(),
        wagonType.limitLabel(),
        derived.consumption(),
        wagonType.specialBenefit(),
        wagonType.description(),
        wagonType.notes(),
        draftBeasts,
        draftStrength,
        derivedDraftStrength(wagonType, improvements),
        improvements.stream().map(this::toView).sorted(Comparator.comparing(CaravanWagonImprovementView::createdAt)).toList(),
        wagon.createdAt(),
        wagon.updatedAt());
  }

  private CaravanWagonImprovementView toView(CaravanWagonImprovement improvement) {
    var type = WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode()));
    return new CaravanWagonImprovementView(
        improvement.id(),
        improvement.caravanId(),
        improvement.wagonId(),
        improvement.improvementTypeCode(),
        type.name(),
        type.category(),
        type.costExpression(),
        type.specialBenefit(),
        type.description(),
        type.notes(),
        improvement.createdAt(),
        improvement.updatedAt());
  }

  private CaravanBeastView toView(CaravanBeast beast) {
    var assignedWagonName = beast.assignedWagonId() == null
        ? null
        : wagonRepository.findById(beast.caravanId(), beast.assignedWagonId())
            .flatMap(wagon -> WagonCatalog.findByCode(wagon.wagonTypeCode()).map(WagonType::name))
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
        beast.assignmentType(),
        beast.assignedWagonId(),
        assignedWagonName,
        beast.createdAt(),
        beast.updatedAt());
  }

  private DerivedWagonStats deriveWagonStats(WagonType wagonType, List<CaravanWagonImprovement> improvements) {
    var currentHitPoints = wagonType.hitPoints();
    var currentHardness = wagonType.hardness();
    var currentTravelerCapacity = wagonType.travelerCapacity();
    var currentCargoCapacity = wagonType.cargoCapacity();
    var currentConsumption = wagonType.consumption();
    var currentDraftConstraint = WagonDraftConstraint.parse(wagonType.propulsion());

    for (var improvement : improvements) {
      var type = WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
          .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode()));

      if (type.hitPointsMultiplier() != null) {
        currentHitPoints = roundStat(currentHitPoints * type.hitPointsMultiplier());
      }
      if (type.hitPointsBonus() != null) {
        currentHitPoints += type.hitPointsBonus();
      }
      if (type.hardnessMultiplier() != null) {
        currentHardness = roundStat(currentHardness * type.hardnessMultiplier());
      }
      if (type.hardnessBonus() != null) {
        currentHardness += type.hardnessBonus();
      }
      if (type.travelerCapacityOverride() != null) {
        currentTravelerCapacity = type.travelerCapacityOverride();
      } else if (type.travelerCapacityMultiplier() != null) {
        var minIncrement = type.travelerCapacityMinimumIncrement() == null ? 0 : type.travelerCapacityMinimumIncrement();
        currentTravelerCapacity += Math.max(minIncrement, roundStat(currentTravelerCapacity * (type.travelerCapacityMultiplier() - 1)));
      } else if (type.travelerCapacityBonus() != null) {
        currentTravelerCapacity += type.travelerCapacityBonus();
      }
      if (type.cargoCapacityOverride() != null) {
        currentCargoCapacity = type.cargoCapacityOverride();
      } else if (type.cargoCapacityMultiplier() != null) {
        var minIncrement = type.cargoCapacityMinimumIncrement() == null ? 0 : type.cargoCapacityMinimumIncrement();
        currentCargoCapacity += Math.max(minIncrement, roundStat(currentCargoCapacity * (type.cargoCapacityMultiplier() - 1)));
      } else if (type.cargoCapacityBonus() != null) {
        currentCargoCapacity += type.cargoCapacityBonus();
      }
      if (type.consumptionBonus() != null) {
        currentConsumption += type.consumptionBonus();
      }
      if (type.propulsionEffect() != null && !type.propulsionEffect().isBlank()) {
        var draftEffect = WagonDraftConstraint.tryParse(type.propulsionEffect());
        if (draftEffect.isPresent()) {
          currentDraftConstraint = currentDraftConstraint.plus(draftEffect.get());
        }
      }
    }

    return new DerivedWagonStats(
        wagonType.cost(),
        Math.max(0, currentHitPoints),
        Math.max(0, currentHardness),
        currentDraftConstraint.format(),
        Math.max(0, currentTravelerCapacity),
        Math.max(0, currentCargoCapacity),
        Math.max(0, currentConsumption));
  }

  private int derivedDraftStrength(WagonType wagonType, List<CaravanWagonImprovement> improvements) {
    var constraint = WagonDraftConstraint.parse(wagonType.propulsion());
    for (var improvement : improvements) {
      var type = WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
          .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode()));
      if (type.propulsionEffect() != null && !type.propulsionEffect().isBlank()) {
        var draftEffect = WagonDraftConstraint.tryParse(type.propulsionEffect());
        if (draftEffect.isPresent()) {
          constraint = constraint.plus(draftEffect.get());
        }
      }
    }
    return constraint.minimumStrength();
  }

  private List<CaravanBeastView> draftBeasts(UUID caravanId, UUID wagonId) {
    return beastRepository.findAllByCaravanIdAndWagonIdAndAssignmentType(caravanId, wagonId, CaravanBeastAssignmentType.DRAFT).stream()
        .map(this::toView)
        .toList();
  }

  private int effectiveDraftStrength(CaravanBeastView beast) {
    return beast.strength() * (beast.fourLegged() ? 2 : 1);
  }

  private int roundStat(double value) {
    return (int) Math.round(value);
  }

  private String calculateImprovementBlockedReason(
      WagonImprovementType improvementType,
      WagonType wagonType,
      List<CaravanWagonImprovement> improvements) {
    var currentDraftConstraint = deriveCurrentDraftConstraint(wagonType, improvements);
    if (improvements.stream().anyMatch(improvement -> improvement.improvementTypeCode().equals(improvementType.code()))
        && !improvementType.isRepeatable()
        && improvements.stream().filter(improvement -> improvement.improvementTypeCode().equals(improvementType.code())).count() >= improvementType.maxPerWagon()) {
      return "Mejora ya aplicada";
    }
    if (improvementType.requiredBasePropulsionFragment() != null
        && !currentDraftConstraint.format().contains(improvementType.requiredBasePropulsionFragment())) {
      return "Propulsión insuficiente";
    }
    for (var prerequisite : improvementType.prerequisites()) {
      if (improvements.stream().noneMatch(improvement -> improvement.improvementTypeCode().equals(prerequisite))) {
        return "Falta el requisito: " + prerequisite;
      }
    }
    for (var incompatibility : improvementType.incompatibilities()) {
      if (improvements.stream().anyMatch(improvement -> improvement.improvementTypeCode().equals(incompatibility))) {
        return "Incompatible con: " + incompatibility;
      }
    }
    var count = improvements.stream().filter(improvement -> improvement.improvementTypeCode().equals(improvementType.code())).count();
    if (count >= improvementType.maxPerWagon()) {
      return "Límite alcanzado";
    }
    return null;
  }

  private WagonDraftConstraint deriveCurrentDraftConstraint(
      WagonType wagonType,
      List<CaravanWagonImprovement> improvements) {
    var currentDraftConstraint = WagonDraftConstraint.parse(wagonType.propulsion());
    for (var improvement : improvements) {
      var type = WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
          .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode()));
      if (type.propulsionEffect() != null && !type.propulsionEffect().isBlank()) {
        var draftEffect = WagonDraftConstraint.tryParse(type.propulsionEffect());
        if (draftEffect.isPresent()) {
          currentDraftConstraint = currentDraftConstraint.plus(draftEffect.get());
        }
      }
    }
    return currentDraftConstraint;
  }

  private void validateImprovementAddition(
      WagonType wagonType,
      WagonImprovementType improvementType,
      List<CaravanWagonImprovement> improvements) {
    var blockedReason = calculateImprovementBlockedReason(improvementType, wagonType, improvements);
    if (blockedReason != null) {
      throw new IllegalArgumentException(blockedReason);
    }
  }

  private void validateImprovementRemoval(CaravanWagonImprovement target, List<CaravanWagonImprovement> improvements) {
    var remaining = improvements.stream()
        .filter(improvement -> !improvement.id().equals(target.id()))
        .toList();
    var removedCode = target.improvementTypeCode();
    var dependent = remaining.stream()
        .map(improvement -> WagonImprovementCatalog.findByCode(improvement.improvementTypeCode())
            .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + improvement.improvementTypeCode())))
        .filter(type -> type.prerequisites().contains(removedCode))
        .findFirst();
    if (dependent.isPresent()) {
      throw new IllegalArgumentException("No se puede eliminar la mejora porque es requisito de: " + dependent.get().name());
    }
  }

  private record DerivedWagonStats(
      int cost,
      int hitPoints,
      int hardness,
      String propulsion,
      int travelerCapacity,
      int cargoCapacity,
      int consumption) {}

  private static final class NoopCaravanCargoRepositoryPort implements CaravanCargoRepositoryPort {

    @Override
    public com.gestioncaravana.domain.CaravanCargo save(com.gestioncaravana.domain.CaravanCargo cargo) {
      return cargo;
    }

    @Override
    public List<com.gestioncaravana.domain.CaravanCargo> findAllByCaravanId(UUID caravanId) {
      return List.of();
    }

    @Override
    public java.util.Optional<com.gestioncaravana.domain.CaravanCargo> findById(UUID caravanId, UUID cargoId) {
      return java.util.Optional.empty();
    }

    @Override
    public void deleteById(UUID caravanId, UUID cargoId) {}

    @Override
    public void deleteByCaravanId(UUID caravanId) {}

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return 0;
    }
  }
}
