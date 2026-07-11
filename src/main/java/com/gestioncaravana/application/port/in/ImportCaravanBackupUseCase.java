package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBackupView;
import com.gestioncaravana.application.model.CaravanBackupImportResultView;

public interface ImportCaravanBackupUseCase {

  CaravanBackupImportResultView execute(CaravanBackupView backup);
}
