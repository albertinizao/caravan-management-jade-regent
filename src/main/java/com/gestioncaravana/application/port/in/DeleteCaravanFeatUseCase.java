package com.gestioncaravana.application.port.in;

import java.util.UUID;

public interface DeleteCaravanFeatUseCase {
  void delete(UUID caravanId, UUID featId);
}
