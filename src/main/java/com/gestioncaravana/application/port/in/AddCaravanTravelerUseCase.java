package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanTravelerView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AddCaravanTravelerUseCase {

  CaravanTravelerView execute(UUID caravanId, AddCaravanTravelerCommand command);

  record AddCaravanTravelerCommand(
      String fullName,
      String description,
      List<String> availableRoleCodes,
      List<String> activeRoleCodes,
      String activeRoleCode,
      Integer maxActiveRoleCount,
      BigDecimal salary,
      String contractConditions,
      Integer consumption,
      UUID servedTravelerId) {}
}
