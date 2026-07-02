package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonView;
import java.util.UUID;

public interface AddCaravanWagonUseCase {

  CaravanWagonView execute(UUID caravanId, AddCaravanWagonCommand command);

  record AddCaravanWagonCommand(String wagonTypeCode, String displayName, String specificCommodity) {
    public AddCaravanWagonCommand(String wagonTypeCode, String displayName) {
      this(wagonTypeCode, displayName, null);
    }
  }
}
