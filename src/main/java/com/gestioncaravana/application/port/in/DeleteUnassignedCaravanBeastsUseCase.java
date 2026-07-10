package com.gestioncaravana.application.port.in;

import java.util.UUID;

public interface DeleteUnassignedCaravanBeastsUseCase {

  void delete(UUID caravanId);
}
