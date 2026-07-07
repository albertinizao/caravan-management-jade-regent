package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;
import java.util.UUID;

public interface UpdateCaravanMainStatsUseCase {

  CaravanCampaignView execute(UUID caravanId, UpdateCaravanMainStatsCommand command);

  record UpdateCaravanMainStatsCommand(int offense, int defense, int mobility, int morale) {}
}
