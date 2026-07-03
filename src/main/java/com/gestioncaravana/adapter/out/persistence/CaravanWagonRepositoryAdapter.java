package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanWagon;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanWagonRepositoryAdapter implements CaravanWagonRepositoryPort {

  private final SpringDataCaravanWagonRepository repository;

  public CaravanWagonRepositoryAdapter(SpringDataCaravanWagonRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanWagon save(CaravanWagon wagon) {
    return toDomain(repository.save(toEntity(wagon)));
  }

  @Override
  public List<CaravanWagon> findAllByCaravanId(UUID caravanId) {
    return repository.findAllByCaravanId(caravanId.toString()).stream().map(this::toDomain).toList();
  }

  @Override
  public Optional<CaravanWagon> findById(UUID caravanId, UUID wagonId) {
    return repository.findByCaravanIdAndId(caravanId.toString(), wagonId.toString()).map(this::toDomain);
  }

  @Override
  public void deleteById(UUID caravanId, UUID wagonId) {
    repository.deleteByCaravanIdAndId(caravanId.toString(), wagonId.toString());
  }

  @Override
  public long countByCaravanId(UUID caravanId) {
    return repository.countByCaravanId(caravanId.toString());
  }

  @Override
  public long countByCaravanIdAndWagonTypeCode(UUID caravanId, String wagonTypeCode) {
    return repository.countByCaravanIdAndWagonTypeCode(caravanId.toString(), wagonTypeCode);
  }

  private CaravanWagonJpaEntity toEntity(CaravanWagon wagon) {
    var entity = new CaravanWagonJpaEntity();
    entity.setId(wagon.id().toString());
    entity.setCaravanId(wagon.caravanId().toString());
    entity.setWagonTypeCode(wagon.wagonTypeCode());
    entity.setDisplayName(wagon.displayName());
    entity.setSpecificCommodity(wagon.specificCommodity());
    entity.setCurrentHitPoints(wagon.currentHitPoints());
    entity.setCreatedAt(wagon.createdAt());
    entity.setUpdatedAt(wagon.updatedAt());
    return entity;
  }

  private CaravanWagon toDomain(CaravanWagonJpaEntity entity) {
    return new CaravanWagon(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        entity.getWagonTypeCode(),
        entity.getDisplayName(),
        entity.getSpecificCommodity(),
        entity.getCurrentHitPoints(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
