package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonImprovementView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanWagonImprovementsUseCase {

  List<CaravanWagonImprovementView> listImprovements(UUID caravanId, UUID wagonId);
}
