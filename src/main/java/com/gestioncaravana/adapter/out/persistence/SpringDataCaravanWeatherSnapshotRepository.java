package com.gestioncaravana.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanWeatherSnapshotRepository
    extends JpaRepository<CaravanWeatherSnapshotJpaEntity, CaravanWeatherSnapshotId> {}
