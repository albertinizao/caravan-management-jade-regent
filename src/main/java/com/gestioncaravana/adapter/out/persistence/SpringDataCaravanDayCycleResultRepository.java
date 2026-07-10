package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCaravanDayCycleResultRepository extends JpaRepository<CaravanDayCycleResultJpaEntity, String> {
  Optional<CaravanDayCycleResultJpaEntity> findFirstByCaravanIdOrderByDayIndexDesc(String caravanId);

  List<CaravanDayCycleResultJpaEntity> findAllByCaravanIdOrderByDayIndexAsc(String caravanId);
}
