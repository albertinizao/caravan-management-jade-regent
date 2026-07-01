package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanFeatRepositoryAdapter implements CaravanFeatRepositoryPort {

  private final SpringDataCaravanFeatRepository repository;

  public CaravanFeatRepositoryAdapter(SpringDataCaravanFeatRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanFeat save(CaravanFeat feat) {
    return toDomain(repository.save(toEntity(feat)));
  }

  @Override
  public List<CaravanFeat> findAllByCaravanId(UUID caravanId) {
    return repository.findAllByCaravanIdOrderBySelectionIndexAscIdAsc(caravanId.toString()).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Optional<CaravanFeat> findById(UUID caravanId, UUID featId) {
    return repository.findByCaravanIdAndId(caravanId.toString(), featId.toString()).map(this::toDomain);
  }

  @Override
  public long countByCaravanIdAndFeatTypeCode(UUID caravanId, String featTypeCode) {
    return repository.countByCaravanIdAndFeatTypeCode(caravanId.toString(), featTypeCode);
  }

  @Override
  public void deleteById(UUID caravanId, UUID featId) {
    repository.deleteByCaravanIdAndId(caravanId.toString(), featId.toString());
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteByCaravanId(caravanId.toString());
  }

  private CaravanFeatJpaEntity toEntity(CaravanFeat feat) {
    var entity = new CaravanFeatJpaEntity();
    entity.setId(feat.id().toString());
    entity.setCaravanId(feat.caravanId().toString());
    entity.setFeatTypeCode(feat.featTypeCode());
    entity.setAcquisitionSourceType(feat.acquisitionSourceType().name());
    entity.setAcquisitionLevel(feat.acquisitionLevel());
    entity.setAcquisitionCause(feat.acquisitionCause());
    entity.setSelectionIndex(feat.selectionIndex());
    entity.setActive(feat.active());
    entity.setCreatedAt(feat.createdAt());
    entity.setUpdatedAt(feat.updatedAt());
    return entity;
  }

  private CaravanFeat toDomain(CaravanFeatJpaEntity entity) {
    return new CaravanFeat(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        entity.getFeatTypeCode(),
        CaravanFeatAcquisitionSourceType.valueOf(entity.getAcquisitionSourceType()),
        entity.getAcquisitionLevel(),
        entity.getAcquisitionCause(),
        entity.getSelectionIndex(),
        entity.getActive() == null ? true : entity.getActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
