package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.domain.CaravanCargoSourceType;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class CaravanCargoSupplyNormalizationMigration {

  private static final String SUPPLIES_CODE = "suministros";
  private static final String PERISHABLE_SUPPLIES_CODE = "suministros-perecederos";
  private static final int STANDARD_SUPPLY_VALUE = 10;

  private final SpringDataCaravanCargoRepository repository;
  private final Clock clock;

  public CaravanCargoSupplyNormalizationMigration(SpringDataCaravanCargoRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void normalizeLegacySupplyStacks() {
    var now = clock.instant();
    var cargo = repository.findAll();
    for (var entry : cargo) {
      if (!isUnitBasedSupply(entry.getSourceType(), entry.getCatalogCode()) || entry.getQuantity() <= 1) {
        continue;
      }

      var unitCargo = splitIntoUnits(entry, now);
      if (unitCargo.isEmpty()) {
        repository.deleteByCaravanIdAndId(entry.getCaravanId(), entry.getId());
        continue;
      }

      for (var unit : unitCargo) {
        repository.save(unit);
      }
    }
  }

  private boolean isUnitBasedSupply(String sourceType, String catalogCode) {
    return CaravanCargoSourceType.CATALOG.name().equals(sourceType)
        && (SUPPLIES_CODE.equals(catalogCode) || PERISHABLE_SUPPLIES_CODE.equals(catalogCode));
  }

  private List<CaravanCargoJpaEntity> splitIntoUnits(CaravanCargoJpaEntity entry, java.time.Instant now) {
    var totalProvisions = currentProvisions(entry);
    if (totalProvisions.compareTo(BigDecimal.ZERO) <= 0) {
      return List.of();
    }

    var quantity = entry.getQuantity();
    var base = totalProvisions.divide(BigDecimal.valueOf(quantity), java.math.RoundingMode.DOWN);
    var distributed = base.multiply(BigDecimal.valueOf(quantity));
    var remainder = totalProvisions.subtract(distributed);
    var units = new ArrayList<CaravanCargoJpaEntity>();
    var currentUnitId = entry.getId();
    for (var index = 0; index < quantity; index++) {
      var provisions = base.add(index < remainder.intValueExact() ? BigDecimal.ONE : BigDecimal.ZERO);
      if (provisions.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      var unit = copy(entry, currentUnitId, provisions, now);
      units.add(unit);
      currentUnitId = UUID.randomUUID().toString();
    }
    return units;
  }

  private BigDecimal currentProvisions(CaravanCargoJpaEntity entry) {
    if (entry.getCurrentProvisions() != null) {
      return entry.getCurrentProvisions();
    }
    if (SUPPLIES_CODE.equals(entry.getCatalogCode()) || PERISHABLE_SUPPLIES_CODE.equals(entry.getCatalogCode())) {
      return BigDecimal.valueOf(entry.getQuantity()).multiply(BigDecimal.valueOf(STANDARD_SUPPLY_VALUE));
    }
    return BigDecimal.ZERO;
  }

  private CaravanCargoJpaEntity copy(CaravanCargoJpaEntity source, String id, BigDecimal provisions, java.time.Instant now) {
    var entity = new CaravanCargoJpaEntity();
    entity.setId(id);
    entity.setCaravanId(source.getCaravanId());
    entity.setSourceType(source.getSourceType());
    entity.setCatalogCode(source.getCatalogCode());
    entity.setDisplayName(source.getDisplayName());
    entity.setCategory(source.getCategory());
    entity.setQuantity(1);
    entity.setCargoUnits(source.getCargoUnits());
    entity.setCurrentProvisions(provisions);
    entity.setDayPassed(Boolean.TRUE.equals(source.getDayPassed()));
    entity.setWagonId(source.getWagonId());
    entity.setOrigin(source.getOrigin());
    entity.setSpecificCommodity(source.getSpecificCommodity());
    entity.setDeity(source.getDeity());
    entity.setNotes(source.getNotes());
    entity.setCreatedAt(source.getCreatedAt());
    entity.setUpdatedAt(now);
    return entity;
  }
}
