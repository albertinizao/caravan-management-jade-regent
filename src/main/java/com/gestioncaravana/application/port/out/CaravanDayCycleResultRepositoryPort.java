package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanDayCycleResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanDayCycleResultRepositoryPort {

  CaravanDayCycleResult save(CaravanDayCycleResult result);

  Optional<CaravanDayCycleResult> findLatestByCaravanId(UUID caravanId);

  List<CaravanDayCycleResult> findAllByCaravanId(UUID caravanId);
}
