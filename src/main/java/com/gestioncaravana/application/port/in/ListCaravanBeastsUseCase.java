package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanBeastsUseCase {
  List<CaravanBeastView> list(UUID caravanId, String query, String sourceType, String assignmentType, UUID wagonId);
}
