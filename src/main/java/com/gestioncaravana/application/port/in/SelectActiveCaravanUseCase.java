package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;
import java.util.UUID;

public interface SelectActiveCaravanUseCase {

  CaravanCampaignView select(UUID caravanId);
}
