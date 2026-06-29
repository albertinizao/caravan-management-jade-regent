package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;
import java.util.List;

public interface ListCaravansUseCase {

  List<CaravanCampaignView> list();
}
