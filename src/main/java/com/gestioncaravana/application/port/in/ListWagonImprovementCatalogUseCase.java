package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.WagonImprovementCatalogItemView;
import java.util.UUID;
import java.util.List;

public interface ListWagonImprovementCatalogUseCase {

  List<WagonImprovementCatalogItemView> listCatalog(UUID caravanId, UUID wagonId);
}
