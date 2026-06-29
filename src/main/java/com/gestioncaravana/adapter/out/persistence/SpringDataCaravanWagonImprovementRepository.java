package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanWagonImprovementRepository
    extends JpaRepository<CaravanWagonImprovementJpaEntity, String> {

  List<CaravanWagonImprovementJpaEntity> findAllByCaravanIdAndWagonId(String caravanId, String wagonId);

  Optional<CaravanWagonImprovementJpaEntity> findByCaravanIdAndWagonIdAndId(String caravanId, String wagonId, String id);

  void deleteByCaravanIdAndWagonIdAndId(String caravanId, String wagonId, String id);
}
