package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCaravanCalendarEventRepository
    extends JpaRepository<CaravanCalendarEventJpaEntity, Long> {

  List<CaravanCalendarEventJpaEntity> findByCaravanIdAndDateKeyOrderByCreatedAtAsc(
      String caravanId, String dateKey);

  List<CaravanCalendarEventJpaEntity> findByCaravanIdAndDateKeyBetweenOrderByDateKeyAscCreatedAtAsc(
      String caravanId, String startDateKey, String endDateKey);

  Optional<CaravanCalendarEventJpaEntity> findByCaravanIdAndId(String caravanId, Long id);

  void deleteByCaravanIdAndId(String caravanId, Long id);

  void deleteByCaravanId(String caravanId);
}
