package com.gestioncaravana.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCaravanSupplyStateRepository extends JpaRepository<CaravanSupplyStateJpaEntity, String> {
  Optional<CaravanSupplyStateJpaEntity> findByCaravanId(String caravanId);
  void deleteByCaravanId(String caravanId);
}
