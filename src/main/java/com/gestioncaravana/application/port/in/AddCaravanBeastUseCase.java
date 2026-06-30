package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastView;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import java.util.UUID;

public interface AddCaravanBeastUseCase {

  CaravanBeastView execute(UUID caravanId, AddCaravanBeastCommand command);

  record AddCaravanBeastCommand(
      CaravanBeastSourceType sourceType,
      String catalogBeastCode,
      String name,
      String size,
      Integer strength,
      Integer speed,
      Integer thermalAdaptation,
      Integer basePrice,
      Integer trainedPrice,
      Boolean fourLegged,
      String specialNote,
      String description,
      String customNotes) {}
}
