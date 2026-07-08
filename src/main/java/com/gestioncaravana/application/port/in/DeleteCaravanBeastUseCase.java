package com.gestioncaravana.application.port.in;

import java.util.UUID;

public interface DeleteCaravanBeastUseCase {

  void delete(UUID caravanId, UUID beastId);
}
