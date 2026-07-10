package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanDayCyclePreviewView;
import java.util.UUID;

public interface ConfirmCaravanDayCycleUseCase {

  CaravanDayCyclePreviewView confirm(UUID caravanId, ConfirmCaravanDayCycleCommand command);

  record ConfirmCaravanDayCycleCommand(String previewFingerprint) {}
}
