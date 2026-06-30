package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastView;
import java.util.UUID;

public interface ClearCaravanBeastAssignmentUseCase {
  CaravanBeastView execute(UUID caravanId, UUID beastId);
}
