package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCargoView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanCargoUseCase {
  List<CaravanCargoView> list(UUID caravanId, String query, String sourceType, String category, UUID wagonId);
}
