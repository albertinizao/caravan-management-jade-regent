package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastView;
import java.util.UUID;

public interface GetCaravanBeastUseCase {
  CaravanBeastView getById(UUID caravanId, UUID beastId);
}
