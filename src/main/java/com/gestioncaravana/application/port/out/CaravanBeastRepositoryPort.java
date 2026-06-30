package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanBeastRepositoryPort {

  CaravanBeast save(CaravanBeast beast);

  List<CaravanBeast> findAllByCaravanId(UUID caravanId);

  List<CaravanBeast> findAllByCaravanIdAndAssignmentType(UUID caravanId, CaravanBeastAssignmentType assignmentType);

  List<CaravanBeast> findAllByCaravanIdAndWagonIdAndAssignmentType(
      UUID caravanId, UUID wagonId, CaravanBeastAssignmentType assignmentType);

  Optional<CaravanBeast> findById(UUID caravanId, UUID beastId);

  void deleteByCaravanId(UUID caravanId);
}
