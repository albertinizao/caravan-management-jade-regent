package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastCatalogItemView;
import java.util.List;

public interface ListBeastCatalogUseCase {
  List<CaravanBeastCatalogItemView> list();
}
