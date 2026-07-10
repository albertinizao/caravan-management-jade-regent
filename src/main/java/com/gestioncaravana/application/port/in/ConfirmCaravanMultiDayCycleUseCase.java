package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanMultiDayCyclePreviewView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public interface ConfirmCaravanMultiDayCycleUseCase {

  CaravanMultiDayCyclePreviewView confirm(UUID caravanId, ConfirmCaravanMultiDayCycleCommand command);

  record ConfirmCaravanMultiDayCycleCommand(
      @NotNull @Min(1) @Max(30) Integer days,
      @NotBlank String basePreviewFingerprint) {}
}
