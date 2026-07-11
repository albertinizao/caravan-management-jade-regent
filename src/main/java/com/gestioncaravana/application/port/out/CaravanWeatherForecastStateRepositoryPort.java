package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanWeatherForecastState;
import com.gestioncaravana.domain.GolarionDate;
import java.util.Optional;
import java.util.UUID;

public interface CaravanWeatherForecastStateRepositoryPort {

  CaravanWeatherForecastState save(CaravanWeatherForecastState state);

  Optional<CaravanWeatherForecastState> findByCaravanIdAndDate(UUID caravanId, GolarionDate date);

  void deleteByCaravanIdAndDate(UUID caravanId, GolarionDate date);

  void deleteFromDate(UUID caravanId, GolarionDate fromDate);

  void deleteByCaravanId(UUID caravanId);
}
