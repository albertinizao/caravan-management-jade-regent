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
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatCatalogPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanFeatType;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.TravelerRoleCatalog;
import com.gestioncaravana.domain.TravelerRoleCatalogItem;
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
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final CaravanBeastRepositoryPort beastRepository;
  private final CaravanFeatRepositoryPort featRepository;
  private final CaravanFeatCatalogPort featCatalogPort;
  private final ActiveCaravanSelectionPort activeSelectionPort;
  private final Clock clock;

  @org.springframework.beans.factory.annotation.Autowired
  public CaravanManagementService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanBeastRepositoryPort beastRepository,
      CaravanFeatRepositoryPort featRepository,
      CaravanFeatCatalogPort featCatalogPort,
      ActiveCaravanSelectionPort activeSelectionPort,
      Clock clock) {
    this.campaignRepository = campaignRepository;
    this.wagonRepository = wagonRepository;
    this.travelerRepository = travelerRepository;
    this.beastRepository = beastRepository;
    this.featRepository = featRepository;
    this.featCatalogPort = featCatalogPort;
    this.activeSelectionPort = activeSelectionPort;
    this.clock = clock;
  }

  CaravanManagementService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanBeastRepositoryPort beastRepository,
      CaravanFeatRepositoryPort featRepository,
      ActiveCaravanSelectionPort activeSelectionPort,
      Clock clock) {
    this(
        campaignRepository,
        wagonRepository,
        travelerRepository,
        beastRepository,
        featRepository,
        new NoopCaravanFeatCatalogPort(),
        activeSelectionPort,
        clock);
  }

  CaravanManagementService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanBeastRepositoryPort beastRepository,
      ActiveCaravanSelectionPort activeSelectionPort,
      Clock clock) {
    this(
        campaignRepository,
        wagonRepository,
        travelerRepository,
        beastRepository,
        new NoopCaravanFeatRepositoryPort(),
        new NoopCaravanFeatCatalogPort(),
        activeSelectionPort,
        clock);
  }

  @Override
  public CaravanCampaignView execute(CreateCaravanCommand command) {
    var campaign = CaravanCampaign.create(
        UUID.randomUUID(),
        command.name(),
        command.description(),
        com.gestioncaravana.domain.CaravanMainStats.withInitialAllocation(
            command.offense(),
            command.defense(),
            command.mobility(),
            command.morale()),
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
    travelerRepository.deleteByCaravanId(exists.id());
    beastRepository.deleteByCaravanId(exists.id());
    featRepository.deleteByCaravanId(exists.id());

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
    var travelers = travelerRepository.findAllByCaravanId(campaign.id()).stream()
        .map(traveler -> traveler.fullName() + " · "
            + TravelerRoleCatalog.findByCode(traveler.activeRoleCode())
                .map(TravelerRoleCatalogItem::name)
                .orElse(traveler.activeRoleCode()))
        .toList();
    var beasts = beastRepository.findAllByCaravanId(campaign.id()).stream()
        .map(beast -> beast.name() + " · " + beast.assignmentType().name().toLowerCase())
        .toList();
    var feats = featRepository.findAllByCaravanId(campaign.id()).stream()
        .map(feat -> featCatalogPort.findByCode(feat.featTypeCode())
            .map(CaravanFeatType::name)
            .orElse(feat.featTypeCode()))
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
        travelers,
        beasts,
        feats);
  }

  private static final class NoopCaravanFeatRepositoryPort implements CaravanFeatRepositoryPort {

    @Override
    public com.gestioncaravana.domain.CaravanFeat save(com.gestioncaravana.domain.CaravanFeat feat) {
      return feat;
    }

    @Override
    public List<com.gestioncaravana.domain.CaravanFeat> findAllByCaravanId(java.util.UUID caravanId) {
      return List.of();
    }

    @Override
    public java.util.Optional<com.gestioncaravana.domain.CaravanFeat> findById(java.util.UUID caravanId, java.util.UUID featId) {
      return java.util.Optional.empty();
    }

    @Override
    public long countByCaravanIdAndFeatTypeCode(java.util.UUID caravanId, String featTypeCode) {
      return 0;
    }

    @Override
    public void deleteById(java.util.UUID caravanId, java.util.UUID featId) {}

    @Override
    public void deleteByCaravanId(java.util.UUID caravanId) {}
  }

  private static final class NoopCaravanFeatCatalogPort implements CaravanFeatCatalogPort {

    @Override
    public List<CaravanFeatType> all() {
      return List.of();
    }

    @Override
    public Optional<CaravanFeatType> findByCode(String code) {
      return Optional.empty();
    }
  }
}
