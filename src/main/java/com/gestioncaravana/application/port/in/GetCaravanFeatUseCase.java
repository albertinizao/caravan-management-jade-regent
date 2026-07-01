package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanFeatView;
import java.util.UUID;

public interface GetCaravanFeatUseCase {
  CaravanFeatView getById(UUID caravanId, UUID featId);
}
