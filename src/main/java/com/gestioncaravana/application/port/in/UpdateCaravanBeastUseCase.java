package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastView;
import java.math.BigDecimal;
import java.util.UUID;

public interface UpdateCaravanBeastUseCase {

  CaravanBeastView execute(UUID caravanId, UUID beastId, UpdateCaravanBeastCommand command);

  record UpdateCaravanBeastCommand(Integer consumption, BigDecimal occupiedSpace) {}
}
