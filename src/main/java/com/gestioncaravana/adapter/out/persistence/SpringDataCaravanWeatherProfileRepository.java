package com.gestioncaravana.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanWeatherProfileRepository
    extends JpaRepository<CaravanWeatherProfileJpaEntity, String> {}
