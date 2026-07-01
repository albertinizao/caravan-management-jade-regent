package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanFeat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanFeatRepositoryPort {
  CaravanFeat save(CaravanFeat feat);

  List<CaravanFeat> findAllByCaravanId(UUID caravanId);

  Optional<CaravanFeat> findById(UUID caravanId, UUID featId);

  long countByCaravanIdAndFeatTypeCode(UUID caravanId, String featTypeCode);

  void deleteById(UUID caravanId, UUID featId);

  void deleteByCaravanId(UUID caravanId);
}
