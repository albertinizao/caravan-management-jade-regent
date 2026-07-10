package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanCargoRepositoryAdapter implements CaravanCargoRepositoryPort {

  private final SpringDataCaravanCargoRepository repository;

  public CaravanCargoRepositoryAdapter(SpringDataCaravanCargoRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanCargo save(CaravanCargo cargo) {
    return toDomain(repository.save(toEntity(cargo)));
  }

  @Override
  public List<CaravanCargo> findAllByCaravanId(UUID caravanId) {
    return repository.findAllByCaravanId(caravanId.toString()).stream().map(this::toDomain).toList();
  }

  @Override
  public Optional<CaravanCargo> findById(UUID caravanId, UUID cargoId) {
    return repository.findByCaravanIdAndId(caravanId.toString(), cargoId.toString()).map(this::toDomain);
  }

  @Override
  public void deleteById(UUID caravanId, UUID cargoId) {
    repository.deleteByCaravanIdAndId(caravanId.toString(), cargoId.toString());
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteAll(repository.findAllByCaravanId(caravanId.toString()));
  }

  @Override
  public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
    return repository.countByCaravanIdAndWagonId(caravanId.toString(), wagonId.toString());
  }

  private CaravanCargoJpaEntity toEntity(CaravanCargo cargo) {
    var entity = new CaravanCargoJpaEntity();
    entity.setId(cargo.id().toString());
    entity.setCaravanId(cargo.caravanId().toString());
    entity.setSourceType(cargo.sourceType().name());
    entity.setCatalogCode(cargo.catalogCode());
    entity.setDisplayName(cargo.displayName());
    entity.setCategory(cargo.category());
    entity.setQuantity(cargo.quantity());
    entity.setCargoUnits(cargo.cargoUnits());
    entity.setCurrentProvisions(cargo.currentProvisions());
    entity.setDayPassed(cargo.dayPassed());
    entity.setWagonId(cargo.wagonId() == null ? null : cargo.wagonId().toString());
    entity.setOrigin(cargo.origin());
    entity.setSpecificCommodity(cargo.specificCommodity());
    entity.setDeity(cargo.deity());
    entity.setNotes(cargo.notes());
    entity.setCreatedAt(cargo.createdAt());
    entity.setUpdatedAt(cargo.updatedAt());
    return entity;
  }

  private CaravanCargo toDomain(CaravanCargoJpaEntity entity) {
    return new CaravanCargo(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        CaravanCargoSourceType.valueOf(entity.getSourceType()),
        entity.getCatalogCode(),
        entity.getDisplayName(),
        entity.getCategory(),
        entity.getQuantity(),
        entity.getCargoUnits(),
        entity.getCurrentProvisions() == null ? null : entity.getCurrentProvisions(),
        entity.getDayPassed() == null ? Boolean.FALSE : entity.getDayPassed(),
        entity.getWagonId() == null ? null : UUID.fromString(entity.getWagonId()),
        entity.getOrigin(),
        entity.getSpecificCommodity(),
        entity.getDeity(),
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
