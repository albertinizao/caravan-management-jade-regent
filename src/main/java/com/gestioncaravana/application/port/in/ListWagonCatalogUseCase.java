package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.WagonCatalogItemView;
import java.util.List;

public interface ListWagonCatalogUseCase {

  List<WagonCatalogItemView> list();
}
