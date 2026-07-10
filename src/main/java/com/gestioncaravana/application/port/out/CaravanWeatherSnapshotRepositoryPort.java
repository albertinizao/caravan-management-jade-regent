package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.GolarionDate;
import java.util.Optional;
import java.util.UUID;

public interface CaravanWeatherSnapshotRepositoryPort {

  CaravanWeatherSnapshot save(CaravanWeatherSnapshot snapshot);

  Optional<CaravanWeatherSnapshot> findByCaravanIdAndDate(UUID caravanId, GolarionDate date);

  void deleteFromDate(UUID caravanId, GolarionDate fromDate);

  void deleteByCaravanId(UUID caravanId);
}
