package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWagonView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanWagonsUseCase {

  List<CaravanWagonView> list(UUID caravanId);
}
