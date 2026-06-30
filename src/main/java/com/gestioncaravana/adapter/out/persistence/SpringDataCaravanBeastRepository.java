package com.gestioncaravana.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanBeastRepository extends JpaRepository<CaravanBeastJpaEntity, String> {
  List<CaravanBeastJpaEntity> findAllByCaravanId(String caravanId);

  java.util.Optional<CaravanBeastJpaEntity> findByCaravanIdAndId(String caravanId, String id);
}
