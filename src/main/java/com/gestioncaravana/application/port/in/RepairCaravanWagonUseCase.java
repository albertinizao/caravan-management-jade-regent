package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonView;
import java.util.UUID;

public interface RepairCaravanWagonUseCase {

  CaravanWagonView execute(UUID caravanId, UUID wagonId, RepairCaravanWagonCommand command);

  record RepairCaravanWagonCommand(int repairAmount) {}
}
