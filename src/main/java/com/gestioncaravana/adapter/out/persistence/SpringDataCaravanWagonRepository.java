package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanWagonRepository extends JpaRepository<CaravanWagonJpaEntity, String> {

  List<CaravanWagonJpaEntity> findAllByCaravanId(String caravanId);

  long countByCaravanId(String caravanId);

  long countByCaravanIdAndWagonTypeCode(String caravanId, String wagonTypeCode);

  java.util.Optional<CaravanWagonJpaEntity> findByCaravanIdAndId(String caravanId, String id);

  void deleteByCaravanIdAndId(String caravanId, String id);
}
