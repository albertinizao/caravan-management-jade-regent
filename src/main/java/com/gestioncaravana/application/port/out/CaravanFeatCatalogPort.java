package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanFeatType;
import java.util.List;
import java.util.Optional;

public interface CaravanFeatCatalogPort {
  List<CaravanFeatType> all();

  Optional<CaravanFeatType> findByCode(String code);
}
