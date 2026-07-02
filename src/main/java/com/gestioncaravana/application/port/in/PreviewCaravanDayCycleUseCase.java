package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanDayPreviewView;
import java.util.List;
import java.util.UUID;

public interface PreviewCaravanDayCycleUseCase {

  CaravanDayPreviewView preview(UUID caravanId, PreviewCaravanDayCycleCommand command);

  record PreviewCaravanDayCycleCommand(
      boolean fastingEnabled,
      List<CaravanDailyChoiceCommand> choices) {}

  record CaravanDailyChoiceCommand(UUID travelerId, String mode) {}
}
