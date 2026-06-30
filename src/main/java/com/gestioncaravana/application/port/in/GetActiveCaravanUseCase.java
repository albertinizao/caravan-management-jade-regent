package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;
import java.util.Optional;

public interface GetActiveCaravanUseCase {

  Optional<CaravanCampaignView> getActive();
}
