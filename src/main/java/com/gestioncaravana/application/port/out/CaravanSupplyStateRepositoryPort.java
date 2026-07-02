package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanSupplyState;
import java.util.Optional;
import java.util.UUID;

public interface CaravanSupplyStateRepositoryPort {

  CaravanSupplyState save(CaravanSupplyState state);

  Optional<CaravanSupplyState> findByCaravanId(UUID caravanId);

  void deleteByCaravanId(UUID caravanId);
}
