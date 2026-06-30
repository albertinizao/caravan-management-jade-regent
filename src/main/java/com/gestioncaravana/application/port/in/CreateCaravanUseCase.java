package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCampaignView;

public interface CreateCaravanUseCase {

  CaravanCampaignView execute(CreateCaravanCommand command);

  record CreateCaravanCommand(
      String name,
      String description,
      Integer offense,
      Integer defense,
      Integer mobility,
      Integer morale) {}
}

