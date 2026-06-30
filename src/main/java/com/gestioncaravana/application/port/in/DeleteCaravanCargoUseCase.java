package com.gestioncaravana.application.port.in;

import java.util.UUID;

public interface DeleteCaravanCargoUseCase {
  void delete(UUID caravanId, UUID cargoId);
}
