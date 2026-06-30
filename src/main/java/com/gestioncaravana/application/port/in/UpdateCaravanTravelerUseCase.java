package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanTravelerView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface UpdateCaravanTravelerUseCase {
  CaravanTravelerView execute(UUID caravanId, UUID travelerId, UpdateCaravanTravelerCommand command);

  record UpdateCaravanTravelerCommand(
      String fullName,
      String description,
      List<String> availableRoleCodes,
      List<String> activeRoleCodes,
      String activeRoleCode,
      Integer maxActiveRoleCount,
      UUID wagonId,
      BigDecimal salary,
      String contractConditions,
      Integer consumption,
      UUID servedTravelerId) {}
}
