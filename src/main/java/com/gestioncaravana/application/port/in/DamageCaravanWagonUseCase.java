package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonView;
import java.util.UUID;

public interface DamageCaravanWagonUseCase {

  CaravanWagonView execute(UUID caravanId, UUID wagonId, DamageCaravanWagonCommand command);

  record DamageCaravanWagonCommand(int damageAmount, boolean ignoreHardness) {}
}
