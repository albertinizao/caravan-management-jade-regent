package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonView;
import java.util.UUID;

public interface AddCaravanWagonImprovementUseCase {

  CaravanWagonView execute(UUID caravanId, UUID wagonId, AddCaravanWagonImprovementCommand command);

  record AddCaravanWagonImprovementCommand(String improvementTypeCode) {}
}
