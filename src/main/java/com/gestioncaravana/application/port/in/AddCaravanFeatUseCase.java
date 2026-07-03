package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanFeatView;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import java.util.UUID;

public interface AddCaravanFeatUseCase {

  CaravanFeatView execute(UUID caravanId, AddCaravanFeatCommand command);

  record AddCaravanFeatCommand(
      String featTypeCode,
      CaravanFeatAcquisitionSourceType acquisitionSourceType,
      Integer acquisitionLevel,
      String acquisitionCause,
      Boolean active,
      Boolean manualApplies,
      String manualAppliesReason) {}
}
