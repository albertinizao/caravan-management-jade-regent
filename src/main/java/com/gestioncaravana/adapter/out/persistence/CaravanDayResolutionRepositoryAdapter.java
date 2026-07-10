package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanDayResolutionRepositoryPort;
import com.gestioncaravana.domain.CaravanDayResolution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanDayResolutionRepositoryAdapter implements CaravanDayResolutionRepositoryPort {

  private final SpringDataCaravanDayResolutionRepository repository;

  public CaravanDayResolutionRepositoryAdapter(SpringDataCaravanDayResolutionRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanDayResolution save(CaravanDayResolution resolution) {
    return toDomain(repository.save(toEntity(resolution)));
  }

  @Override
  public Optional<CaravanDayResolution> findByCaravanIdAndIdempotencyKey(UUID caravanId, String idempotencyKey) {
    return repository.findByCaravanIdAndIdempotencyKey(caravanId.toString(), idempotencyKey).map(this::toDomain);
  }

  @Override
  public List<CaravanDayResolution> findAllByCaravanId(UUID caravanId) {
    return repository.findAllByCaravanIdOrderByResolvedDayIndexAsc(caravanId.toString()).stream().map(this::toDomain).toList();
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteByCaravanId(caravanId.toString());
  }

  private CaravanDayResolutionJpaEntity toEntity(CaravanDayResolution resolution) {
    var entity = new CaravanDayResolutionJpaEntity();
    entity.setId(resolution.id().toString());
    entity.setCaravanId(resolution.caravanId().toString());
    entity.setIdempotencyKey(resolution.idempotencyKey());
    entity.setResolvedDayIndex(resolution.resolvedDayIndex());
    entity.setResolvedAt(resolution.resolvedAt());
    entity.setStartingReserve(resolution.startingReserve());
    entity.setEndingReserve(resolution.endingReserve());
    entity.setTotalConsumption(resolution.totalConsumption());
    entity.setTotalGeneration(resolution.totalGeneration());
    entity.setNetDelta(resolution.netDelta());
    entity.setShortage(resolution.shortage());
    entity.setCargoMovementSummary(resolution.cargoMovementSummary());
    entity.setChoicesSummary(resolution.choicesSummary());
    entity.setContributionsSummary(resolution.contributionsSummary());
    entity.setWarningsSummary(resolution.warningsSummary());
    return entity;
  }

  private CaravanDayResolution toDomain(CaravanDayResolutionJpaEntity entity) {
    return new CaravanDayResolution(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        entity.getIdempotencyKey(),
        entity.getResolvedDayIndex(),
        entity.getResolvedAt(),
        entity.getStartingReserve(),
        entity.getEndingReserve(),
        entity.getTotalConsumption(),
        entity.getTotalGeneration(),
        entity.getNetDelta(),
        entity.getShortage(),
        entity.getCargoMovementSummary(),
        entity.getChoicesSummary(),
        entity.getContributionsSummary(),
        entity.getWarningsSummary());
  }
}
