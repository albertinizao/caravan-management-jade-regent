package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanCargoSummaryView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanCargoSummaryUseCase {
  List<CaravanCargoSummaryView> list(UUID caravanId);
}
