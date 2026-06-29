package com.gestioncaravana.application.port.in;

import java.util.UUID;

public interface DeleteCaravanWagonUseCase {

  void delete(UUID caravanId, UUID wagonId);
}
