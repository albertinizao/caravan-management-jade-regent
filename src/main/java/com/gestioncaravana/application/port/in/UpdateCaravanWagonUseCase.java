package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonView;
import java.util.UUID;

public interface UpdateCaravanWagonUseCase {

  CaravanWagonView execute(UUID caravanId, UUID wagonId, UpdateCaravanWagonCommand command);

  record UpdateCaravanWagonCommand(String displayName) {}
}
