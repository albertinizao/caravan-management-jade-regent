package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanCampaignView;
import com.gestioncaravana.application.model.CaravanMainStatsView;
import com.gestioncaravana.application.port.in.CreateCaravanUseCase;
import com.gestioncaravana.application.port.in.GetActiveCaravanUseCase;
import com.gestioncaravana.application.port.in.GetCaravanUseCase;
import com.gestioncaravana.application.port.in.ListCaravansUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanUseCase;
import com.gestioncaravana.application.port.in.SelectActiveCaravanUseCase;
import com.gestioncaravana.application.port.out.ActiveCaravanSelectionPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.WagonCatalog;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CaravanManagementService
    implements CreateCaravanUseCase,
        ListCaravansUseCase,
        GetCaravanUseCase,
        DeleteCaravanUseCase,
        SelectActiveCaravanUseCase,
        GetActiveCaravanUseCase {

  private final CaravanCampaignRepositoryPort campaignRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final ActiveCaravanSelectionPort activeSelectionPort;
  private final Clock clock;

  public CaravanManagementService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanWagonRepositoryPort wagonRepository,
      ActiveCaravanSelectionPort activeSelectionPort,
      Clock clock) {
    this.campaignRepository = campaignRepository;
    this.wagonRepository = wagonRepository;
    this.activeSelectionPort = activeSelectionPort;
    this.clock = clock;
  }

  @Override
  public CaravanCampaignView execute(CreateCaravanCommand command) {
    var campaign = CaravanCampaign.create(
        UUID.randomUUID(),
        command.name(),
        command.description(),
        clock.instant());
    return toView(campaignRepository.save(campaign), false);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanCampaignView> list() {
    var activeId = activeSelectionPort.getActiveCaravanId();
    return campaignRepository.findAll().stream()
        .map(campaign -> toView(campaign, activeId.isPresent() && activeId.get().equals(campaign.id())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanCampaignView getById(UUID id) {
    var campaign = campaignRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + id));
    var activeId = activeSelectionPort.getActiveCaravanId();
    return toView(campaign, activeId.isPresent() && activeId.get().equals(campaign.id()));
  }

  @Override
  public CaravanCampaignView select(UUID caravanId) {
    var campaign = campaignRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
    activeSelectionPort.setActiveCaravanId(caravanId);
    return toView(campaign, true);
  }

  @Override
  public void delete(UUID id) {
    var exists = campaignRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + id));
    campaignRepository.deleteById(exists.id());

    activeSelectionPort.getActiveCaravanId()
        .filter(activeId -> activeId.equals(id))
        .ifPresent(ignored -> activeSelectionPort.clear());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CaravanCampaignView> getActive() {
    return activeSelectionPort.getActiveCaravanId()
        .flatMap(campaignRepository::findById)
        .map(campaign -> toView(campaign, true));
  }

  private CaravanCampaignView toView(CaravanCampaign campaign, boolean active) {
    var wagons = wagonRepository.findAllByCaravanId(campaign.id()).stream()
        .map(wagon -> WagonCatalog.findByCode(wagon.wagonTypeCode())
            .map(type -> type.name())
            .orElse(wagon.wagonTypeCode()))
        .toList();
    return new CaravanCampaignView(
        campaign.id(),
        campaign.name(),
        campaign.description(),
        campaign.level(),
        new CaravanMainStatsView(
            campaign.mainStats().offense(),
            campaign.mainStats().defense(),
            campaign.mainStats().mobility(),
            campaign.mainStats().morale(),
            campaign.mainStats().unassignedPoints()),
        campaign.discontent(),
        campaign.status(),
        active,
        campaign.createdAt(),
        campaign.updatedAt(),
        wagons,
        List.of(),
        List.of(),
        List.of());
  }
}
