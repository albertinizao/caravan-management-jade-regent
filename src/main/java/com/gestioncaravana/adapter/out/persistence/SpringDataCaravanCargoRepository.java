package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanCargoRepository extends JpaRepository<CaravanCargoJpaEntity, String> {

  List<CaravanCargoJpaEntity> findAllByCaravanId(String caravanId);

  java.util.Optional<CaravanCargoJpaEntity> findByCaravanIdAndId(String caravanId, String id);

  void deleteByCaravanIdAndId(String caravanId, String id);

  long countByCaravanIdAndWagonId(String caravanId, String wagonId);
}
