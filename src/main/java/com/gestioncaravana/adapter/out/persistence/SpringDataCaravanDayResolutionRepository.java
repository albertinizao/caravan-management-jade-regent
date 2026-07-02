package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCaravanDayResolutionRepository extends JpaRepository<CaravanDayResolutionJpaEntity, String> {
  Optional<CaravanDayResolutionJpaEntity> findByCaravanIdAndIdempotencyKey(String caravanId, String idempotencyKey);
  List<CaravanDayResolutionJpaEntity> findAllByCaravanIdOrderByResolvedDayIndexAsc(String caravanId);
  void deleteByCaravanId(String caravanId);
}
