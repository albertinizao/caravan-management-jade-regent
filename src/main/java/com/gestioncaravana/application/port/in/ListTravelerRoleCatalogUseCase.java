package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.TravelerRoleCatalogItemView;
import java.util.List;

public interface ListTravelerRoleCatalogUseCase {
  List<TravelerRoleCatalogItemView> list();
}
