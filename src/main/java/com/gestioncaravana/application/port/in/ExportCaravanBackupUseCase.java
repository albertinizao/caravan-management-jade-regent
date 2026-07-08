package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBackupView;
import java.util.UUID;

public interface ExportCaravanBackupUseCase {

  CaravanBackupView export(UUID caravanId);
}
