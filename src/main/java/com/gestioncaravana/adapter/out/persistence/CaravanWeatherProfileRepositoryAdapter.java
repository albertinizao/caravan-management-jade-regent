package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CrownWeatherRegion;
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
    entity.setCrownRegion(profile.crownRegion() == null ? null : profile.crownRegion().name());
    entity.setLegacyCrownOfWorld(profile.isCrownOfTheWorld());
    entity.setUpdatedAt(profile.updatedAt());
    return entity;
  }

  private CaravanWeatherProfile toDomain(CaravanWeatherProfileJpaEntity entity) {
    var baseline = WeatherClimateBaseline.valueOf(entity.getClimateBaseline());
    var elevation = WeatherElevation.valueOf(entity.getElevation());
    var crownRegion = entity.getCrownRegion() == null ? null : CrownWeatherRegion.valueOf(entity.getCrownRegion());

    if (Boolean.TRUE.equals(entity.getLegacyCrownOfWorld()) && baseline != WeatherClimateBaseline.CROWN_OF_THE_WORLD) {
      baseline = WeatherClimateBaseline.CROWN_OF_THE_WORLD;
    }
    if (baseline == WeatherClimateBaseline.CROWN_OF_THE_WORLD && crownRegion == null) {
      crownRegion = inferLegacyRegion(elevation);
    }
    if (baseline == WeatherClimateBaseline.CROWN_OF_THE_WORLD && elevation == WeatherElevation.SEA_LEVEL) {
      elevation = WeatherElevation.LOWLAND;
    }

    return new CaravanWeatherProfile(
        UUID.fromString(entity.getCaravanId()),
        baseline,
        elevation,
        crownRegion,
        entity.getUpdatedAt());
  }

  private CrownWeatherRegion inferLegacyRegion(WeatherElevation elevation) {
    return switch (elevation) {
      case SEA_LEVEL, LOWLAND -> CrownWeatherRegion.OUTER_RIM;
      case HIGHLAND -> CrownWeatherRegion.HIGH_ICE;
      case PEAK -> CrownWeatherRegion.BOREAL_EXPANSE;
    };
  }
}
