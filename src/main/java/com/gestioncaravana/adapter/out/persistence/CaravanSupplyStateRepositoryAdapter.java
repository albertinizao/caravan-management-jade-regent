package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.domain.CaravanSupplyState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanSupplyStateRepositoryAdapter implements CaravanSupplyStateRepositoryPort {

  private final SpringDataCaravanSupplyStateRepository repository;

  public CaravanSupplyStateRepositoryAdapter(SpringDataCaravanSupplyStateRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanSupplyState save(CaravanSupplyState state) {
    return toDomain(repository.save(toEntity(state)));
  }

  @Override
  public Optional<CaravanSupplyState> findByCaravanId(UUID caravanId) {
    return repository.findByCaravanId(caravanId.toString()).map(this::toDomain);
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteByCaravanId(caravanId.toString());
  }

  private CaravanSupplyStateJpaEntity toEntity(CaravanSupplyState state) {
    var entity = new CaravanSupplyStateJpaEntity();
    entity.setCaravanId(state.caravanId().toString());
    entity.setProvisionReserve(state.provisionReserve());
    entity.setStandardReserve(state.standardReserve());
    entity.setPerishableReserve(state.perishableReserve());
    entity.setDaysPassed(state.daysPassed());
    entity.setUpdatedAt(state.updatedAt());
    entity.setSharedJobProductivityState(state.sharedJobProductivityState());
    return entity;
  }

  private CaravanSupplyState toDomain(CaravanSupplyStateJpaEntity entity) {
    return new CaravanSupplyState(
        UUID.fromString(entity.getCaravanId()),
        valueOrZero(entity.getProvisionReserve()),
        valueOrZero(entity.getStandardReserve()),
        valueOrZero(entity.getPerishableReserve()),
        valueOrZero(entity.getDaysPassed()),
        entity.getUpdatedAt(),
        entity.getSharedJobProductivityState());
  }

  private int valueOrZero(Integer value) {
    return value == null ? 0 : value;
  }
}
