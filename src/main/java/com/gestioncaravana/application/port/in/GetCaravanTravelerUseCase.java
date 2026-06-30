package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanTravelerView;
import java.util.UUID;

public interface GetCaravanTravelerUseCase {
  CaravanTravelerView getById(UUID caravanId, UUID travelerId);
}
