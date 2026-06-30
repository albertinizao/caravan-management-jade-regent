package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCargoView;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import java.util.UUID;

public interface AddCaravanCargoUseCase {

  CaravanCargoView execute(UUID caravanId, AddCaravanCargoCommand command);

  record AddCaravanCargoCommand(
      CaravanCargoSourceType sourceType,
      String catalogCode,
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
