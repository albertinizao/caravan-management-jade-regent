package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CargoCatalogItemView;
import java.util.List;

public interface ListCargoCatalogUseCase {
  List<CargoCatalogItemView> list();
}
