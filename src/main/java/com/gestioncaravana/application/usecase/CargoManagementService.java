package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CargoCatalogItemView;
import com.gestioncaravana.application.model.CaravanCargoSummaryView;
import com.gestioncaravana.application.model.CaravanCargoView;
import com.gestioncaravana.application.port.in.AddCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.GetCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.ListCaravanCargoSummaryUseCase;
import com.gestioncaravana.application.port.in.ListCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.ListCargoCatalogUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanCargoWagonUseCase;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CargoCatalog;
import com.gestioncaravana.domain.CargoCatalogItem;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.WagonImprovementCatalog;
import com.gestioncaravana.domain.WagonType;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CargoManagementService
    implements ListCargoCatalogUseCase,
        ListCaravanCargoUseCase,
        GetCaravanCargoUseCase,
        ListCaravanCargoSummaryUseCase,
        AddCaravanCargoUseCase,
        UpdateCaravanCargoUseCase,
        UpdateCaravanCargoWagonUseCase,
        DeleteCaravanCargoUseCase {

  private static final String SUPPLIES_WAGON_CODE = "carro-de-suministros";
  private static final String SPECIFIC_GOODS_WAGON_CODE = "carro-de-mercancias-especificas";
  private static final String TREASURE_CODE = "tesoro";
  private static final String SUPPLIES_CODE = "suministros";
  private static final String PERISHABLE_SUPPLIES_CODE = "suministros-perecederos";
  private static final String SPECIFIC_GOODS_CODE = "mercancias-especificas";

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanCargoRepositoryPort cargoRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanWagonImprovementRepositoryPort improvementRepository;
  private final Clock clock;

  public CargoManagementService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanCargoRepositoryPort cargoRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort improvementRepository,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.cargoRepository = cargoRepository;
    this.wagonRepository = wagonRepository;
    this.improvementRepository = improvementRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CargoCatalogItemView> list() {
    return CargoCatalog.all().stream().map(this::toView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanCargoView> list(UUID caravanId, String query, String sourceType, String category, UUID wagonId) {
    requireCaravan(caravanId);
    var normalizedQuery = normalize(query);
    return cargoRepository.findAllByCaravanId(caravanId).stream()
        .filter(cargo -> normalizedQuery.isBlank() || containsIgnoreCase(cargo.displayName(), normalizedQuery)
            || containsIgnoreCase(cargo.category(), normalizedQuery)
            || containsIgnoreCase(cargo.catalogCode(), normalizedQuery))
        .filter(cargo -> sourceType == null || sourceType.isBlank()
            || cargo.sourceType().name().equalsIgnoreCase(sourceType))
        .filter(cargo -> category == null || category.isBlank() || cargo.category().equalsIgnoreCase(category))
        .filter(cargo -> wagonId == null || wagonId.equals(cargo.wagonId()))
        .sorted(Comparator.comparing(CaravanCargo::displayName, String.CASE_INSENSITIVE_ORDER))
        .map(this::toView)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanCargoView getById(UUID caravanId, UUID cargoId) {
    requireCaravan(caravanId);
    return toView(requireCargo(caravanId, cargoId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanCargoSummaryView> list(UUID caravanId) {
    requireCaravan(caravanId);
    var wagons = wagonRepository.findAllByCaravanId(caravanId);
    var cargo = cargoRepository.findAllByCaravanId(caravanId);
    return wagons.stream()
        .map(wagon -> toSummary(caravanId, wagon, cargo))
        .toList();
  }

  @Override
  public CaravanCargoView execute(UUID caravanId, AddCaravanCargoCommand command) {
    requireCaravan(caravanId);
    if (command.sourceType() == null) {
      throw new IllegalArgumentException("sourceType is required");
    }

    if (command.wagonId() == null) {
      throw new IllegalArgumentException("wagonId is required");
    }

    var wagon = requireWagon(caravanId, command.wagonId());
    var wagonType = requireWagonType(wagon);
    var catalogItem = command.sourceType() == CaravanCargoSourceType.CATALOG
        ? CargoCatalog.findByCode(command.catalogCode())
            .orElseThrow(() -> new IllegalArgumentException("Cargo not found: " + command.catalogCode()))
        : null;

    validateCargoForWagon(wagonType, catalogItem, command.catalogCode(), command.specificCommodity(), command.origin(), command.deity());
    var quantity = resolveQuantity(catalogItem, command.quantity());
    var cargoUnits = resolveCargoUnits(catalogItem, command.cargoUnits());

    validateCargoCapacity(
        caravanId,
        wagon,
        quantity,
        cargoUnits,
        command.catalogCode(),
        command.specificCommodity(),
        null);

    var now = clock.instant();
    var displayName = command.sourceType() == CaravanCargoSourceType.CATALOG ? catalogItem.name() : requireText(command.displayName(), "displayName");
    var category = command.sourceType() == CaravanCargoSourceType.CATALOG ? catalogItem.category() : requireText(command.category(), "category");
    var cargo = CaravanCargo.create(
        UUID.randomUUID(),
        caravanId,
        command.sourceType(),
        command.catalogCode(),
        displayName,
        category,
        quantity,
        cargoUnits,
        wagon.id(),
        command.origin(),
        command.specificCommodity(),
        command.deity(),
        command.notes(),
        now);
    return toView(cargoRepository.save(cargo));
  }

  @Override
  public CaravanCargoView execute(UUID caravanId, UUID cargoId, UpdateCaravanCargoCommand command) {
    requireCaravan(caravanId);
    var cargo = requireCargo(caravanId, cargoId);
    var wagonId = command.wagonId() == null ? cargo.wagonId() : command.wagonId();
    var wagon = requireWagon(caravanId, wagonId);
    var wagonType = requireWagonType(wagon);
    var catalogItem = cargo.sourceType() == CaravanCargoSourceType.CATALOG
        ? CargoCatalog.findByCode(cargo.catalogCode()).orElse(null)
        : null;

    validateCargoForWagon(
        wagonType,
        catalogItem,
        cargo.catalogCode(),
        command.specificCommodity() != null ? command.specificCommodity() : cargo.specificCommodity(),
        command.origin() != null ? command.origin() : cargo.origin(),
        command.deity() != null ? command.deity() : cargo.deity());
    var quantity = command.quantity() == null ? cargo.quantity() : command.quantity();
    var cargoUnits = command.cargoUnits() == null ? cargo.cargoUnits() : command.cargoUnits();

    validateCargoCapacity(
        caravanId,
        wagon,
        quantity,
        cargoUnits,
        cargo.catalogCode(),
        command.specificCommodity() != null ? command.specificCommodity() : cargo.specificCommodity(),
        cargo.id());

    var updated = cargo.update(
        command.displayName(),
        command.category(),
        command.quantity(),
        command.cargoUnits(),
        command.origin(),
        command.specificCommodity(),
        command.deity(),
        command.notes(),
        clock.instant()).assignWagon(wagonId, clock.instant());
    return toView(cargoRepository.save(updated));
  }

  @Override
  public CaravanCargoView execute(UUID caravanId, UUID cargoId, UpdateCaravanCargoWagonCommand command) {
    requireCaravan(caravanId);
    var cargo = requireCargo(caravanId, cargoId);
    if (command.wagonId() == null) {
      throw new IllegalArgumentException("wagonId is required");
    }
    var wagon = requireWagon(caravanId, command.wagonId());
    var wagonType = requireWagonType(wagon);
    var catalogItem = cargo.sourceType() == CaravanCargoSourceType.CATALOG
        ? CargoCatalog.findByCode(cargo.catalogCode()).orElse(null)
        : null;

    validateCargoForWagon(wagonType, catalogItem, cargo.catalogCode(), cargo.specificCommodity(), cargo.origin(), cargo.deity());
    validateCargoCapacity(caravanId, wagon, cargo.quantity(), cargo.cargoUnits(), cargo.catalogCode(), cargo.specificCommodity(), cargo.id());
    return toView(cargoRepository.save(cargo.assignWagon(command.wagonId(), clock.instant())));
  }

  @Override
  public void delete(UUID caravanId, UUID cargoId) {
    requireCaravan(caravanId);
    requireCargo(caravanId, cargoId);
    cargoRepository.deleteById(caravanId, cargoId);
  }

  private com.gestioncaravana.domain.CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private CaravanCargo requireCargo(UUID caravanId, UUID cargoId) {
    return cargoRepository.findById(caravanId, cargoId)
        .orElseThrow(() -> new IllegalArgumentException("Cargo not found: " + cargoId));
  }

  private CaravanWagon requireWagon(UUID caravanId, UUID wagonId) {
    return wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));
  }

  private WagonType requireWagonType(CaravanWagon wagon) {
    return WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
  }

  private CargoCatalogItemView toView(CargoCatalogItem item) {
    return new CargoCatalogItemView(
        item.code(),
        item.name(),
        item.category(),
        item.priceExpression(),
        item.defaultQuantity(),
        true,
        item.defaultCargoUnits(),
        item.cargoUnitsEditable(),
        item.quantityLabel(),
        item.benefitText(),
        item.description(),
        item.notes(),
        item.requiredMetadataKeys(),
        item.allowedWagonCodes());
  }

  private CaravanCargoView toView(CaravanCargo cargo) {
    var wagonName = cargo.wagonId() == null
        ? null
        : wagonRepository.findById(cargo.caravanId(), cargo.wagonId())
            .map(wagon -> requireWagonType(wagon).name())
            .orElse(null);
    var catalogItem = cargo.catalogCode() == null ? null : CargoCatalog.findByCode(cargo.catalogCode()).orElse(null);
    return new CaravanCargoView(
        cargo.id(),
        cargo.caravanId(),
        cargo.sourceType(),
        cargo.sourceType().name().equals("CATALOG") ? "Catálogo" : "Personalizada",
        cargo.catalogCode(),
        catalogItem == null ? null : catalogItem.name(),
        cargo.displayName(),
        cargo.category(),
        cargo.quantity(),
        cargo.cargoUnits(),
        cargo.wagonId(),
        wagonName,
        cargo.origin(),
        cargo.specificCommodity(),
        cargo.deity(),
        cargo.notes(),
        catalogItem == null ? null : catalogItem.priceExpression(),
        cargo.createdAt(),
        cargo.updatedAt());
  }

  private CaravanCargoSummaryView toSummary(UUID caravanId, CaravanWagon wagon, List<CaravanCargo> cargo) {
    var wagonType = requireWagonType(wagon);
    var filtered = cargo.stream().filter(entry -> wagon.id().equals(entry.wagonId())).toList();
    var used = filtered.stream().mapToInt(entry -> entry.quantity() * entry.cargoUnits()).sum();
    var capacity = deriveCargoCapacity(wagonType, improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagon.id()));
    return new CaravanCargoSummaryView(
        wagon.id(),
        wagonType.name(),
        capacity,
        used,
        Math.max(0, capacity - used),
        filtered.size());
  }

  private void validateCargoForWagon(
      WagonType wagonType,
      CargoCatalogItem catalogItem,
      String catalogCode,
      String specificCommodity,
      String origin,
      String deity) {
    var restrictedWagon = wagonType.code().equals(SUPPLIES_WAGON_CODE)
        || wagonType.code().equals(SPECIFIC_GOODS_WAGON_CODE);
    if (catalogCode == null && restrictedWagon) {
      throw new IllegalArgumentException("This wagon only accepts catalog cargo with explicit placement rules");
    }
    if (wagonType.code().equals(SUPPLIES_WAGON_CODE)
        && !SUPPLIES_CODE.equals(catalogCode)
        && !PERISHABLE_SUPPLIES_CODE.equals(catalogCode)) {
      throw new IllegalArgumentException("Carro De Suministros only accepts supplies or perishable supplies");
    }
    if (wagonType.code().equals(SPECIFIC_GOODS_WAGON_CODE) && !SPECIFIC_GOODS_CODE.equals(catalogCode)) {
      throw new IllegalArgumentException("Carro De Mercancías Específicas only accepts specific merchandise");
    }
    if (SPECIFIC_GOODS_CODE.equals(catalogCode) && (specificCommodity == null || specificCommodity.isBlank())) {
      throw new IllegalArgumentException("specificCommodity is required");
    }
    if (catalogItem != null) {
      if (catalogItem.requiredMetadataKeys().contains("origin") && (origin == null || origin.isBlank())) {
        throw new IllegalArgumentException("origin is required");
      }
      if (catalogItem.requiredMetadataKeys().contains("specificCommodity") && (specificCommodity == null || specificCommodity.isBlank())) {
        throw new IllegalArgumentException("specificCommodity is required");
      }
      if (catalogItem.requiredMetadataKeys().contains("deity") && (deity == null || deity.isBlank())) {
        throw new IllegalArgumentException("deity is required");
      }
    }
  }

  private void validateCargoCapacity(
      UUID caravanId,
      CaravanWagon wagon,
      int quantity,
      int cargoUnits,
      String catalogCode,
      String specificCommodity,
      UUID excludeCargoId) {
    var currentUsed = cargoRepository.findAllByCaravanId(caravanId).stream()
        .filter(entry -> excludeCargoId == null || !excludeCargoId.equals(entry.id()))
        .filter(entry -> wagon.id().equals(entry.wagonId()))
        .mapToInt(entry -> entry.quantity() * entry.cargoUnits())
        .sum();
    var capacity = deriveCargoCapacity(requireWagonType(wagon), improvementRepository.findAllByCaravanIdAndWagonId(caravanId, wagon.id()));
    var totalCargoUnits = quantity * cargoUnits;
    if (currentUsed + totalCargoUnits > capacity) {
      throw new IllegalArgumentException("Wagon cargo capacity reached");
    }
    if (SPECIFIC_GOODS_CODE.equals(catalogCode) && specificCommodity != null && !specificCommodity.isBlank()) {
      var incompatibleSpecificCargo = cargoRepository.findAllByCaravanId(caravanId).stream()
          .filter(entry -> excludeCargoId == null || !excludeCargoId.equals(entry.id()))
          .filter(entry -> wagon.id().equals(entry.wagonId()))
          .filter(entry -> SPECIFIC_GOODS_CODE.equals(entry.catalogCode()))
          .filter(entry -> entry.specificCommodity() != null && !entry.specificCommodity().equalsIgnoreCase(specificCommodity))
          .findFirst();
      if (incompatibleSpecificCargo.isPresent()) {
        throw new IllegalArgumentException("This wagon already carries different specific merchandise");
      }
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be greater than or equal to 1");
    }
  }

  private int resolveQuantity(CargoCatalogItem catalogItem, Integer quantity) {
    if (quantity == null) {
      return catalogItem == null ? 1 : catalogItem.resolvedDefaultQuantity();
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be greater than or equal to 1");
    }
    return quantity;
  }

  private int resolveCargoUnits(CargoCatalogItem catalogItem, Integer cargoUnits) {
    if (catalogItem == null) {
      return cargoUnits == null ? 1 : cargoUnits;
    }
    if (!catalogItem.cargoUnitsEditable()) {
      var defaultCargoUnits = catalogItem.resolvedDefaultCargoUnits();
      if (cargoUnits != null && cargoUnits != defaultCargoUnits) {
        throw new IllegalArgumentException("cargoUnits are not editable for " + catalogItem.name());
      }
      return defaultCargoUnits;
    }
    return cargoUnits == null ? catalogItem.resolvedDefaultCargoUnits() : cargoUnits;
  }

  private int deriveCargoCapacity(WagonType wagonType, List<CaravanWagonImprovement> improvements) {
    var currentCargoCapacity = wagonType.cargoCapacity();
    for (var improvement : improvements) {
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

  private String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }

  private boolean containsIgnoreCase(String value, String query) {
    if (value == null || query == null || query.isBlank()) {
      return false;
    }
    return value.toLowerCase().contains(query.toLowerCase());
  }
}
