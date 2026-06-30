package com.gestioncaravana.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCaravanCampaignRepository
    extends JpaRepository<CaravanCampaignJpaEntity, String> {}

