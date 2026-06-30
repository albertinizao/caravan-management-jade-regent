package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCargoView;
import java.util.UUID;

public interface GetCaravanCargoUseCase {
  CaravanCargoView getById(UUID caravanId, UUID cargoId);
}
