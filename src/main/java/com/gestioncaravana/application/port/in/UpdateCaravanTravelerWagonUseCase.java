package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanTravelerView;
import java.util.UUID;

public interface UpdateCaravanTravelerWagonUseCase {
  CaravanTravelerView execute(UUID caravanId, UUID travelerId, UpdateCaravanTravelerWagonCommand command);

  record UpdateCaravanTravelerWagonCommand(UUID wagonId) {}
}
