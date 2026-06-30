package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanTravelerView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanTravelersUseCase {
  List<CaravanTravelerView> list(UUID caravanId, String query, String roleCode, UUID wagonId);
}
