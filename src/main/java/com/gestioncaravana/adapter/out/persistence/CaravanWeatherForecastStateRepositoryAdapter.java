package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanWeatherForecastStateRepositoryPort;
import com.gestioncaravana.domain.CaravanWeatherForecastState;
import com.gestioncaravana.domain.GolarionDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanWeatherForecastStateRepositoryAdapter implements CaravanWeatherForecastStateRepositoryPort {

  private final SpringDataCaravanWeatherForecastStateRepository repository;

  public CaravanWeatherForecastStateRepositoryAdapter(SpringDataCaravanWeatherForecastStateRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanWeatherForecastState save(CaravanWeatherForecastState state) {
    return toDomain(repository.save(toEntity(state)));
  }

  @Override
  public Optional<CaravanWeatherForecastState> findByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
    return repository.findById(new CaravanWeatherForecastStateId(
            caravanId.toString(),
            date.year(),
            date.month(),
            date.day()))
        .map(this::toDomain);
  }

  @Override
  public void deleteByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
    repository.deleteById(new CaravanWeatherForecastStateId(
        caravanId.toString(),
        date.year(),
        date.month(),
        date.day()));
  }

  @Override
  public void deleteFromDate(UUID caravanId, GolarionDate fromDate) {
    repository.deleteAllById(repository.findAll().stream()
        .map(CaravanWeatherForecastStateJpaEntity::getId)
        .filter(id -> caravanId.toString().equals(id.getCaravanId()))
        .filter(id -> new GolarionDate(id.getYear(), id.getMonth(), id.getDay()).compareTo(fromDate) >= 0)
        .toList());
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteAllById(repository.findAll().stream()
        .map(CaravanWeatherForecastStateJpaEntity::getId)
        .filter(id -> caravanId.toString().equals(id.getCaravanId()))
        .toList());
  }

  private CaravanWeatherForecastStateJpaEntity toEntity(CaravanWeatherForecastState state) {
    var entity = new CaravanWeatherForecastStateJpaEntity();
    entity.setId(new CaravanWeatherForecastStateId(
        state.caravanId().toString(),
        state.date().year(),
        state.date().month(),
        state.date().day()));
    entity.setTargetTemperatureF(state.targetTemperatureF());
    entity.setRemainingTargetDays(state.remainingTargetDays());
    entity.setDayBaseTemperatureF(state.dayBaseTemperatureF());
    entity.setNightBaseTemperatureF(state.nightBaseTemperatureF());
    entity.setCarryOverPrecipitation(state.carryOverPrecipitation());
    entity.setCarryOverRemainingPeriods(state.carryOverRemainingPeriods());
    entity.setSevereEvent(state.severeEvent());
    entity.setGeneratedAt(state.generatedAt());
    return entity;
  }

  private CaravanWeatherForecastState toDomain(CaravanWeatherForecastStateJpaEntity entity) {
    var id = entity.getId();
    return new CaravanWeatherForecastState(
        UUID.fromString(id.getCaravanId()),
        new GolarionDate(id.getYear(), id.getMonth(), id.getDay()),
        entity.getTargetTemperatureF(),
        entity.getRemainingTargetDays(),
        entity.getDayBaseTemperatureF(),
        entity.getNightBaseTemperatureF(),
        entity.getCarryOverPrecipitation(),
        entity.getCarryOverRemainingPeriods(),
        entity.getSevereEvent(),
        entity.getGeneratedAt());
  }
}
