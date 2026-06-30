package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanCargo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanCargoRepositoryPort {

  CaravanCargo save(CaravanCargo cargo);

  List<CaravanCargo> findAllByCaravanId(UUID caravanId);

  Optional<CaravanCargo> findById(UUID caravanId, UUID cargoId);

  void deleteById(UUID caravanId, UUID cargoId);

  void deleteByCaravanId(UUID caravanId);

  long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId);
}
