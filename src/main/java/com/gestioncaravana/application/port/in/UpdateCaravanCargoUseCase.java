package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCargoView;
import java.util.UUID;

public interface UpdateCaravanCargoUseCase {

  CaravanCargoView execute(UUID caravanId, UUID cargoId, UpdateCaravanCargoCommand command);

  record UpdateCaravanCargoCommand(
      String displayName,
      String category,
      Integer quantity,
      Integer cargoUnits,
      UUID wagonId,
      String origin,
      String specificCommodity,
      String deity,
      String notes) {}
}
