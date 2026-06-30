package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanWagon;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanWagonRepositoryPort {

  CaravanWagon save(CaravanWagon wagon);

  List<CaravanWagon> findAllByCaravanId(UUID caravanId);

  Optional<CaravanWagon> findById(UUID caravanId, UUID wagonId);

  void deleteById(UUID caravanId, UUID wagonId);

  long countByCaravanId(UUID caravanId);

  long countByCaravanIdAndWagonTypeCode(UUID caravanId, String wagonTypeCode);
}
