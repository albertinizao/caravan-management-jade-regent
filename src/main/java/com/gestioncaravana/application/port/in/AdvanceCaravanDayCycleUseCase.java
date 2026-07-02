package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanDayResolutionView;
import java.util.List;
import java.util.UUID;

public interface AdvanceCaravanDayCycleUseCase {

  CaravanDayResolutionView execute(UUID caravanId, AdvanceCaravanDayCycleCommand command);

  record AdvanceCaravanDayCycleCommand(
      String idempotencyKey,
      boolean fastingEnabled,
      List<CaravanDailyChoiceCommand> choices) {}

  record CaravanDailyChoiceCommand(UUID travelerId, String mode) {}
}
