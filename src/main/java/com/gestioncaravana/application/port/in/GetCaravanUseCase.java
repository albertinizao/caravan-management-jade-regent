package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;
import java.util.UUID;

public interface GetCaravanUseCase {

  CaravanCampaignView getById(UUID id);
}
