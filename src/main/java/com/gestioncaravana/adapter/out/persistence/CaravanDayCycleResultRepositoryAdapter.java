package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanDayCycleResultRepositoryPort;
import com.gestioncaravana.domain.CaravanDayCycleResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanDayCycleResultRepositoryAdapter implements CaravanDayCycleResultRepositoryPort {

  private final SpringDataCaravanDayCycleResultRepository repository;

  public CaravanDayCycleResultRepositoryAdapter(SpringDataCaravanDayCycleResultRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanDayCycleResult save(CaravanDayCycleResult result) {
    return toDomain(repository.save(toEntity(result)));
  }

  @Override
  public Optional<CaravanDayCycleResult> findLatestByCaravanId(UUID caravanId) {
    return repository.findFirstByCaravanIdOrderByDayIndexDesc(caravanId.toString()).map(this::toDomain);
  }

  @Override
  public List<CaravanDayCycleResult> findAllByCaravanId(UUID caravanId) {
    return repository.findAllByCaravanIdOrderByDayIndexAsc(caravanId.toString()).stream().map(this::toDomain).toList();
  }

  private CaravanDayCycleResultJpaEntity toEntity(CaravanDayCycleResult result) {
    var entity = new CaravanDayCycleResultJpaEntity();
    entity.setId(result.id().toString());
    entity.setCaravanId(result.caravanId().toString());
    entity.setPreviewFingerprint(result.previewFingerprint());
    entity.setDayIndex(result.dayIndex());
    entity.setResolvedAt(result.resolvedAt());
    entity.setStartingSupplyUnits(result.startingSupplyUnits());
    entity.setStartingPerishableFood(result.startingPerishableFood());
    entity.setStartingPerishableUnits(result.startingPerishableUnits());
    entity.setGeneratedSuppliesFromAgricultors(result.generatedSuppliesFromAgricultors());
    entity.setGeneratedAlchemyValueFromBoticarios(result.generatedAlchemyValueFromBoticarios());
    entity.setRequiredConsumption(result.requiredConsumption());
    entity.setGeneratedFood(result.generatedFood());
    entity.setLeftoverFood(result.leftoverFood());
    entity.setFinalSupplyUnits(result.finalSupplyUnits());
    entity.setFinalPerishableUnits(result.finalPerishableUnits());
    entity.setFinalPerishableFood(result.finalPerishableFood());
    entity.setConfirmed(result.confirmed());
    entity.setSimulationJson(result.simulationJson());
    entity.setWarningsJson(result.warningsJson());
    return entity;
  }

  private CaravanDayCycleResult toDomain(CaravanDayCycleResultJpaEntity entity) {
    return new CaravanDayCycleResult(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        entity.getPreviewFingerprint(),
        entity.getDayIndex(),
        entity.getResolvedAt(),
        entity.getStartingSupplyUnits(),
        entity.getStartingPerishableFood(),
        entity.getStartingPerishableUnits(),
        entity.getGeneratedSuppliesFromAgricultors(),
        entity.getGeneratedAlchemyValueFromBoticarios(),
        entity.getRequiredConsumption(),
        entity.getGeneratedFood(),
        entity.getLeftoverFood(),
        entity.getFinalSupplyUnits(),
        entity.getFinalPerishableUnits(),
        entity.getFinalPerishableFood(),
        entity.isConfirmed(),
        entity.getSimulationJson(),
        entity.getWarningsJson());
  }
}
