package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanFeatCatalogItemView;
import java.util.List;
import java.util.UUID;

public interface ListCaravanFeatCatalogUseCase {
  List<CaravanFeatCatalogItemView> listCatalog(UUID caravanId);
}
