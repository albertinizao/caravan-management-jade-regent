package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanWeatherProfile;
import java.util.Optional;
import java.util.UUID;

public interface CaravanWeatherProfileRepositoryPort {

  CaravanWeatherProfile save(CaravanWeatherProfile profile);

  Optional<CaravanWeatherProfile> findByCaravanId(UUID caravanId);

  void deleteByCaravanId(UUID caravanId);
}
