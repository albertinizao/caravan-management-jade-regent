package com.gestioncaravana.application.port.out;

import com.gestioncaravana.domain.CaravanCampaign;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaravanCampaignRepositoryPort {

  CaravanCampaign save(CaravanCampaign caravanCampaign);

  void deleteById(UUID id);

  List<CaravanCampaign> findAll();

  Optional<CaravanCampaign> findById(UUID id);
}
