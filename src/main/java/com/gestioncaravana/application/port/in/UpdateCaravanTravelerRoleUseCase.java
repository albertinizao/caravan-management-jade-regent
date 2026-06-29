package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanTravelerView;
import java.util.UUID;

public interface UpdateCaravanTravelerRoleUseCase {
  CaravanTravelerView execute(UUID caravanId, UUID travelerId, UpdateCaravanTravelerRoleCommand command);

  record UpdateCaravanTravelerRoleCommand(
      java.util.List<String> activeRoleCodes,
      String activeRoleCode,
      Integer maxActiveRoleCount,
      UUID servedTravelerId) {}
}
