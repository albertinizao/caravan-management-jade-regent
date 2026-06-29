package com.gestioncaravana.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface ActiveCaravanSelectionPort {

  Optional<UUID> getActiveCaravanId();

  void setActiveCaravanId(UUID caravanId);

  void clear();
}

