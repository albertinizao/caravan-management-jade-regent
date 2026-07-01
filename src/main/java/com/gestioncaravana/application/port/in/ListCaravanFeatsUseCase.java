package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanFeatView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanFeatsUseCase {
  List<CaravanFeatView> list(UUID caravanId);
}
