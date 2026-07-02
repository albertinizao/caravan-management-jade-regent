package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanDayResolution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanDayResolutionRepositoryPort {

  CaravanDayResolution save(CaravanDayResolution resolution);

  Optional<CaravanDayResolution> findByCaravanIdAndIdempotencyKey(UUID caravanId, String idempotencyKey);

  List<CaravanDayResolution> findAllByCaravanId(UUID caravanId);

  void deleteByCaravanId(UUID caravanId);
}
