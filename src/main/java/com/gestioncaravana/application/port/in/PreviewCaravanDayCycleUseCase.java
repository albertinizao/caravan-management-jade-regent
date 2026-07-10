package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanDayCyclePreviewView;
import java.util.UUID;

public interface PreviewCaravanDayCycleUseCase {

  CaravanDayCyclePreviewView preview(UUID caravanId);
}
