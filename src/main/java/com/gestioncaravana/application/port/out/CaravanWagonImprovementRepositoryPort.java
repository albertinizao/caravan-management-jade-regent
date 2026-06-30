package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanWagonImprovement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanWagonImprovementRepositoryPort {

  CaravanWagonImprovement save(CaravanWagonImprovement improvement);

  List<CaravanWagonImprovement> findAllByCaravanIdAndWagonId(UUID caravanId, UUID wagonId);

  Optional<CaravanWagonImprovement> findById(UUID caravanId, UUID wagonId, UUID improvementId);

  void deleteById(UUID caravanId, UUID wagonId, UUID improvementId);
}
