package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanWagonView;
import com.gestioncaravana.application.model.WagonCatalogItemView;
import com.gestioncaravana.application.port.in.AddCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.GetCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.ListCaravanWagonsUseCase;
import com.gestioncaravana.application.port.in.ListWagonCatalogUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.WagonType;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WagonManagementService
    implements ListWagonCatalogUseCase,
        ListCaravanWagonsUseCase,
        GetCaravanWagonUseCase,
        AddCaravanWagonUseCase,
        DeleteCaravanWagonUseCase {

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final Clock clock;

  public WagonManagementService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanWagonRepositoryPort wagonRepository,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.wagonRepository = wagonRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<WagonCatalogItemView> list() {
    return WagonCatalog.all().stream().map(this::toCatalogView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanWagonView> list(UUID caravanId) {
    requireCaravan(caravanId);
    return wagonRepository.findAllByCaravanId(caravanId).stream().map(this::toView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanWagonView getById(UUID caravanId, UUID wagonId) {
    requireCaravan(caravanId);
    return wagonRepository.findById(caravanId, wagonId)
        .map(this::toView)
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
    return toView(wagonRepository.save(wagon));
  }

  @Override
  public void delete(UUID caravanId, UUID wagonId) {
    requireCaravan(caravanId);
    wagonRepository.findById(caravanId, wagonId)
        .orElseThrow(() -> new IllegalArgumentException("Wagon not found: " + wagonId));
    wagonRepository.deleteById(caravanId, wagonId);
  }

  private com.gestioncaravana.domain.CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
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

  private CaravanWagonView toView(CaravanWagon wagon) {
    var wagonType = WagonCatalog.findByCode(wagon.wagonTypeCode())
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagon.wagonTypeCode()));
    return new CaravanWagonView(
        wagon.id(),
        wagon.caravanId(),
        wagon.wagonTypeCode(),
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
        wagonType.notes(),
        wagon.createdAt(),
        wagon.updatedAt());
  }
}
