package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCargoView;
import java.util.UUID;

public interface UpdateCaravanCargoWagonUseCase {

  CaravanCargoView execute(UUID caravanId, UUID cargoId, UpdateCaravanCargoWagonCommand command);

  record UpdateCaravanCargoWagonCommand(UUID wagonId) {}
}
