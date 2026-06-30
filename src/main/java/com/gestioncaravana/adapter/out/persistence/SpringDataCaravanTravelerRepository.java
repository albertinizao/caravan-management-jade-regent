package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanTravelerRepository extends JpaRepository<CaravanTravelerJpaEntity, String> {
  List<CaravanTravelerJpaEntity> findAllByCaravanId(String caravanId);

  java.util.Optional<CaravanTravelerJpaEntity> findByCaravanIdAndId(String caravanId, String id);

  long countByCaravanIdAndWagonId(String caravanId, String wagonId);

  void deleteByCaravanIdAndId(String caravanId, String id);
}
