package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.WeatherClimateBaseline;
import com.gestioncaravana.domain.WeatherElevation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanWeatherProfileRepositoryAdapter implements CaravanWeatherProfileRepositoryPort {

  private final SpringDataCaravanWeatherProfileRepository repository;

  public CaravanWeatherProfileRepositoryAdapter(SpringDataCaravanWeatherProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanWeatherProfile save(CaravanWeatherProfile profile) {
    return toDomain(repository.save(toEntity(profile)));
  }

  @Override
  public Optional<CaravanWeatherProfile> findByCaravanId(UUID caravanId) {
    return repository.findById(caravanId.toString()).map(this::toDomain);
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteById(caravanId.toString());
  }

  private CaravanWeatherProfileJpaEntity toEntity(CaravanWeatherProfile profile) {
    var entity = new CaravanWeatherProfileJpaEntity();
    entity.setCaravanId(profile.caravanId().toString());
    entity.setClimateBaseline(profile.climateBaseline().name());
    entity.setElevation(profile.elevation().name());
    entity.setCrownOfWorld(profile.crownOfWorld());
    entity.setUpdatedAt(profile.updatedAt());
    return entity;
  }

  private CaravanWeatherProfile toDomain(CaravanWeatherProfileJpaEntity entity) {
    return new CaravanWeatherProfile(
        UUID.fromString(entity.getCaravanId()),
        WeatherClimateBaseline.valueOf(entity.getClimateBaseline()),
        WeatherElevation.valueOf(entity.getElevation()),
        entity.isCrownOfWorld(),
        entity.getUpdatedAt());
  }
}
