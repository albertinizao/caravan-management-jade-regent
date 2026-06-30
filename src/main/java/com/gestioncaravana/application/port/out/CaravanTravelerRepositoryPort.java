package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanTraveler;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanTravelerRepositoryPort {
  CaravanTraveler save(CaravanTraveler traveler);

  List<CaravanTraveler> findAllByCaravanId(UUID caravanId);

  Optional<CaravanTraveler> findById(UUID caravanId, UUID travelerId);

  long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId);

  void deleteByCaravanIdAndId(UUID caravanId, UUID travelerId);

  void deleteByCaravanId(UUID caravanId);
}
