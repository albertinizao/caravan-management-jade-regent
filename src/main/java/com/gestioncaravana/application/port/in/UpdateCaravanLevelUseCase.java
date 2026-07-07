package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;
import java.util.UUID;

public interface UpdateCaravanLevelUseCase {

  CaravanCampaignView execute(UUID caravanId, UpdateCaravanLevelCommand command);

  record UpdateCaravanLevelCommand(int delta) {}
}
