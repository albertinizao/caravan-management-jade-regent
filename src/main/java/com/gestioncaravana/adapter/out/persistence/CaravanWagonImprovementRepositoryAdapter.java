package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanWagonImprovementRepositoryAdapter implements CaravanWagonImprovementRepositoryPort {

  private final SpringDataCaravanWagonImprovementRepository repository;

  public CaravanWagonImprovementRepositoryAdapter(SpringDataCaravanWagonImprovementRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanWagonImprovement save(CaravanWagonImprovement improvement) {
    return toDomain(repository.save(toEntity(improvement)));
  }

  @Override
  public List<CaravanWagonImprovement> findAllByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
    return repository.findAllByCaravanIdAndWagonId(caravanId.toString(), wagonId.toString()).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Optional<CaravanWagonImprovement> findById(UUID caravanId, UUID wagonId, UUID improvementId) {
    return repository.findByCaravanIdAndWagonIdAndId(caravanId.toString(), wagonId.toString(), improvementId.toString())
        .map(this::toDomain);
  }

  @Override
  public void deleteById(UUID caravanId, UUID wagonId, UUID improvementId) {
    repository.deleteByCaravanIdAndWagonIdAndId(caravanId.toString(), wagonId.toString(), improvementId.toString());
  }

  private CaravanWagonImprovementJpaEntity toEntity(CaravanWagonImprovement improvement) {
    var entity = new CaravanWagonImprovementJpaEntity();
    entity.setId(improvement.id().toString());
    entity.setCaravanId(improvement.caravanId().toString());
    entity.setWagonId(improvement.wagonId().toString());
    entity.setImprovementTypeCode(improvement.improvementTypeCode());
    entity.setCreatedAt(improvement.createdAt());
    entity.setUpdatedAt(improvement.updatedAt());
    return entity;
  }

  private CaravanWagonImprovement toDomain(CaravanWagonImprovementJpaEntity entity) {
    return new CaravanWagonImprovement(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        UUID.fromString(entity.getWagonId()),
        entity.getImprovementTypeCode(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
