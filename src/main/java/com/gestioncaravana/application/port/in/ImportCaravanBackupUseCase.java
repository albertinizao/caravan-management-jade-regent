package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBackupView;
import com.gestioncaravana.application.model.CaravanCampaignView;

public interface ImportCaravanBackupUseCase {

  CaravanCampaignView execute(CaravanBackupView backup);
}
