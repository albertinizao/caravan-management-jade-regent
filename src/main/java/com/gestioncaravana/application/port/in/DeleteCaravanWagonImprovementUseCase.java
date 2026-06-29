package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonView;
import java.util.UUID;

public interface DeleteCaravanWagonImprovementUseCase {

  CaravanWagonView execute(UUID caravanId, UUID wagonId, UUID improvementId);
}
