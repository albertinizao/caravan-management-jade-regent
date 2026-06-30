package com.gestioncaravana.application.port.in;

import java.util.UUID;

public interface DeleteCaravanTravelerUseCase {

  void delete(UUID caravanId, UUID travelerId);
}
