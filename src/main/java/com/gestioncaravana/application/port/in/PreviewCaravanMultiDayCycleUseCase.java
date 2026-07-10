package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanMultiDayCyclePreviewView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public interface PreviewCaravanMultiDayCycleUseCase {

  CaravanMultiDayCyclePreviewView preview(UUID caravanId, PreviewCaravanMultiDayCycleCommand command);

  record PreviewCaravanMultiDayCycleCommand(
      @NotNull @Min(1) @Max(30) Integer days) {}
}
