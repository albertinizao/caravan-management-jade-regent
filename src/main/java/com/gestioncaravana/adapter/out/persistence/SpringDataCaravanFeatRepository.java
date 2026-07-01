package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanFeatRepository extends JpaRepository<CaravanFeatJpaEntity, String> {
  List<CaravanFeatJpaEntity> findAllByCaravanIdOrderBySelectionIndexAscIdAsc(String caravanId);

  Optional<CaravanFeatJpaEntity> findByCaravanIdAndId(String caravanId, String id);

  long countByCaravanIdAndFeatTypeCode(String caravanId, String featTypeCode);

  void deleteByCaravanIdAndId(String caravanId, String id);

  void deleteByCaravanId(String caravanId);
}
