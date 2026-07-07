package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;
import java.util.UUID;

public interface UpdateCaravanDiscontentUseCase {

  CaravanCampaignView execute(UUID caravanId, UpdateCaravanDiscontentCommand command);

  record UpdateCaravanDiscontentCommand(int delta) {}
}
