package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.GolarionDate;
import com.gestioncaravana.domain.WeatherPeriod;
import com.gestioncaravana.domain.WeatherSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanWeatherSnapshotRepositoryAdapter implements CaravanWeatherSnapshotRepositoryPort {

  private final SpringDataCaravanWeatherSnapshotRepository repository;

  public CaravanWeatherSnapshotRepositoryAdapter(SpringDataCaravanWeatherSnapshotRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanWeatherSnapshot save(CaravanWeatherSnapshot snapshot) {
    return toDomain(repository.save(toEntity(snapshot)));
  }

  @Override
  public Optional<CaravanWeatherSnapshot> findByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
    return repository.findById(new CaravanWeatherSnapshotId(caravanId.toString(), date.year(), date.month(), date.day()))
        .map(this::toDomain);
  }

  @Override
  public void deleteFromDate(UUID caravanId, GolarionDate fromDate) {
    repository.deleteAllById(repository.findAll().stream()
        .map(CaravanWeatherSnapshotJpaEntity::getId)
        .filter(id -> caravanId.toString().equals(id.getCaravanId()))
        .filter(id -> new GolarionDate(id.getYear(), id.getMonth(), id.getDay()).compareTo(fromDate) >= 0)
        .toList());
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteAllById(repository.findAll().stream()
        .map(CaravanWeatherSnapshotJpaEntity::getId)
        .filter(id -> caravanId.toString().equals(id.getCaravanId()))
        .toList());
  }

  private CaravanWeatherSnapshotJpaEntity toEntity(CaravanWeatherSnapshot snapshot) {
    var entity = new CaravanWeatherSnapshotJpaEntity();
    entity.setId(new CaravanWeatherSnapshotId(
        snapshot.caravanId().toString(),
        snapshot.date().year(),
        snapshot.date().month(),
        snapshot.date().day()));
    entity.setMidnightToDawn(toEmbeddable(snapshot.weather().midnightToDawn()));
    entity.setDawnToNoon(toEmbeddable(snapshot.weather().dawnToNoon()));
    entity.setNoonToDusk(toEmbeddable(snapshot.weather().noonToDusk()));
    entity.setDuskToMidnight(toEmbeddable(snapshot.weather().duskToMidnight()));
    entity.setGeneratedAt(snapshot.generatedAt());
    return entity;
  }

  private CaravanWeatherSnapshot toDomain(CaravanWeatherSnapshotJpaEntity entity) {
    var id = entity.getId();
    return new CaravanWeatherSnapshot(
        UUID.fromString(id.getCaravanId()),
        new GolarionDate(id.getYear(), id.getMonth(), id.getDay()),
        new WeatherSnapshot(
            toDomain(entity.getMidnightToDawn()),
            toDomain(entity.getDawnToNoon()),
            toDomain(entity.getNoonToDusk()),
            toDomain(entity.getDuskToMidnight())),
        entity.getGeneratedAt());
  }

  private WeatherPeriodEmbeddable toEmbeddable(WeatherPeriod weather) {
    var embeddable = new WeatherPeriodEmbeddable();
    embeddable.setPrecipitation(weather.precipitation());
    embeddable.setWindStrength(weather.windStrength());
    embeddable.setTemperatureC(weather.temperatureC());
    embeddable.setTemperatureF(weather.temperatureF());
    return embeddable;
  }

  private WeatherPeriod toDomain(WeatherPeriodEmbeddable embeddable) {
    return new WeatherPeriod(
        embeddable.getPrecipitation(),
        embeddable.getWindStrength(),
        embeddable.getTemperatureC(),
        embeddable.getTemperatureF());
  }
}
