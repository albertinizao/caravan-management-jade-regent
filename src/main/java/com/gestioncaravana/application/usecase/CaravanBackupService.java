package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanBackupView;
import com.gestioncaravana.application.model.CaravanBackupView.BeastSnapshot;
import com.gestioncaravana.application.model.CaravanBackupView.CargoSnapshot;
import com.gestioncaravana.application.model.CaravanBackupView.CaravanSnapshot;
import com.gestioncaravana.application.model.CaravanBackupView.DayResolutionSnapshot;
import com.gestioncaravana.application.model.CaravanBackupView.FeatSnapshot;
import com.gestioncaravana.application.model.CaravanMainStatsView;
import com.gestioncaravana.application.model.CaravanBackupView.SupplyStateSnapshot;
import com.gestioncaravana.application.model.CaravanBackupView.TravelerSnapshot;
import com.gestioncaravana.application.model.CaravanBackupView.WagonImprovementSnapshot;
import com.gestioncaravana.application.model.CaravanBackupView.WagonSnapshot;
import com.gestioncaravana.application.model.CaravanCampaignView;
import com.gestioncaravana.application.port.in.DeleteCaravanUseCase;
import com.gestioncaravana.application.port.in.ExportCaravanBackupUseCase;
import com.gestioncaravana.application.port.in.ImportCaravanBackupUseCase;
import com.gestioncaravana.application.port.out.ActiveCaravanSelectionPort;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanDayResolutionRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanSupplyStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanDayResolution;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanSupplyState;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CaravanBackupService implements ExportCaravanBackupUseCase, ImportCaravanBackupUseCase {

  private final CaravanCampaignRepositoryPort campaignRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanWagonImprovementRepositoryPort wagonImprovementRepository;
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final CaravanCargoRepositoryPort cargoRepository;
  private final CaravanBeastRepositoryPort beastRepository;
  private final CaravanFeatRepositoryPort featRepository;
  private final CaravanSupplyStateRepositoryPort supplyStateRepository;
  private final CaravanDayResolutionRepositoryPort dayResolutionRepository;
  private final ActiveCaravanSelectionPort activeSelectionPort;
  private final DeleteCaravanUseCase deleteCaravanUseCase;

  public CaravanBackupService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort wagonImprovementRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanCargoRepositoryPort cargoRepository,
      CaravanBeastRepositoryPort beastRepository,
      CaravanFeatRepositoryPort featRepository,
      CaravanSupplyStateRepositoryPort supplyStateRepository,
      CaravanDayResolutionRepositoryPort dayResolutionRepository,
      ActiveCaravanSelectionPort activeSelectionPort,
      DeleteCaravanUseCase deleteCaravanUseCase) {
    this.campaignRepository = campaignRepository;
    this.wagonRepository = wagonRepository;
    this.wagonImprovementRepository = wagonImprovementRepository;
    this.travelerRepository = travelerRepository;
    this.cargoRepository = cargoRepository;
    this.beastRepository = beastRepository;
    this.featRepository = featRepository;
    this.supplyStateRepository = supplyStateRepository;
    this.dayResolutionRepository = dayResolutionRepository;
    this.activeSelectionPort = activeSelectionPort;
    this.deleteCaravanUseCase = deleteCaravanUseCase;
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanBackupView export(UUID caravanId) {
    var campaign = campaignRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
    var supplyState = supplyStateRepository.findByCaravanId(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Supply state not found for caravan: " + caravanId));
    var active = activeSelectionPort.getActiveCaravanId().filter(caravanId::equals).isPresent();

    return new CaravanBackupView(
        CaravanBackupView.CURRENT_SCHEMA_VERSION,
        active,
        toCampaignSnapshot(campaign),
        toSupplyStateSnapshot(supplyState),
        wagonRepository.findAllByCaravanId(caravanId).stream().map(CaravanBackupService::toWagonSnapshot).toList(),
        wagonRepository.findAllByCaravanId(caravanId).stream()
            .flatMap(wagon -> wagonImprovementRepository.findAllByCaravanIdAndWagonId(caravanId, wagon.id()).stream())
            .map(CaravanBackupService::toWagonImprovementSnapshot)
            .toList(),
        travelerRepository.findAllByCaravanId(caravanId).stream().map(CaravanBackupService::toTravelerSnapshot).toList(),
        cargoRepository.findAllByCaravanId(caravanId).stream().map(CaravanBackupService::toCargoSnapshot).toList(),
        beastRepository.findAllByCaravanId(caravanId).stream().map(CaravanBackupService::toBeastSnapshot).toList(),
        featRepository.findAllByCaravanId(caravanId).stream().map(CaravanBackupService::toFeatSnapshot).toList(),
        dayResolutionRepository.findAllByCaravanId(caravanId).stream().map(CaravanBackupService::toDayResolutionSnapshot).toList());
  }

  @Override
  public CaravanCampaignView execute(CaravanBackupView backup) {
    var caravanId = backup.caravan().id();
    deleteCaravanUseCase.delete(caravanId);

    var savedCampaign = campaignRepository.save(toCampaign(backup.caravan()));
    supplyStateRepository.save(toSupplyState(backup.supplyState()));

    for (var wagonSnapshot : backup.wagons()) {
      wagonRepository.save(toWagon(wagonSnapshot));
    }
    for (var improvementSnapshot : backup.wagonImprovements()) {
      wagonImprovementRepository.save(toWagonImprovement(improvementSnapshot));
    }
    for (var travelerSnapshot : backup.travelers()) {
      travelerRepository.save(toTraveler(travelerSnapshot));
    }
    for (var cargoSnapshot : backup.cargo()) {
      cargoRepository.save(toCargo(cargoSnapshot));
    }
    for (var beastSnapshot : backup.beasts()) {
      beastRepository.save(toBeast(beastSnapshot));
    }
    for (var featSnapshot : backup.feats()) {
      featRepository.save(toFeat(featSnapshot));
    }
    for (var dayResolutionSnapshot : backup.dayResolutions()) {
      dayResolutionRepository.save(toDayResolution(dayResolutionSnapshot));
    }

    if (backup.active()) {
      activeSelectionPort.setActiveCaravanId(caravanId);
    }

    return new CaravanCampaignView(
        savedCampaign.id(),
        savedCampaign.name(),
        savedCampaign.description(),
        savedCampaign.level(),
        new CaravanMainStatsView(
            savedCampaign.mainStats().offense(),
            savedCampaign.mainStats().defense(),
            savedCampaign.mainStats().mobility(),
            savedCampaign.mainStats().morale(),
            savedCampaign.mainStats().unassignedPoints()),
        savedCampaign.discontent(),
        savedCampaign.status(),
        backup.active(),
        savedCampaign.createdAt(),
        savedCampaign.updatedAt(),
        wagonRepository.findAllByCaravanId(caravanId).stream().map(wagon -> wagon.displayNameOr(wagon.wagonTypeCode())).toList(),
        travelerRepository.findAllByCaravanId(caravanId).stream().map(traveler -> traveler.fullName() + " · " + traveler.activeRoleCode()).toList(),
        beastRepository.findAllByCaravanId(caravanId).stream().map(CaravanBeast::name).toList(),
        featRepository.findAllByCaravanId(caravanId).stream().map(CaravanFeat::featTypeCode).toList());
  }

  private static CaravanSnapshot toCampaignSnapshot(com.gestioncaravana.domain.CaravanCampaign campaign) {
    return new CaravanSnapshot(
        campaign.id(),
        campaign.name(),
        campaign.description(),
        campaign.level(),
        campaign.mainStats(),
        campaign.discontent(),
        campaign.status(),
        campaign.createdAt(),
        campaign.updatedAt());
  }

  private static com.gestioncaravana.domain.CaravanCampaign toCampaign(CaravanSnapshot snapshot) {
    return new com.gestioncaravana.domain.CaravanCampaign(
        snapshot.id(),
        snapshot.name(),
        snapshot.description(),
        snapshot.level(),
        snapshot.mainStats(),
        snapshot.discontent(),
        snapshot.status(),
        snapshot.createdAt(),
        snapshot.updatedAt());
  }

  private static SupplyStateSnapshot toSupplyStateSnapshot(CaravanSupplyState state) {
    return new SupplyStateSnapshot(
        state.caravanId(),
        state.provisionReserve(),
        state.standardReserve(),
        state.perishableReserve(),
        state.daysPassed(),
        state.updatedAt());
  }

  private static CaravanSupplyState toSupplyState(SupplyStateSnapshot snapshot) {
    return new CaravanSupplyState(
        snapshot.caravanId(),
        snapshot.provisionReserve(),
        snapshot.standardReserve(),
        snapshot.perishableReserve(),
        snapshot.daysPassed(),
        snapshot.updatedAt());
  }

  private static WagonSnapshot toWagonSnapshot(CaravanWagon wagon) {
    return new WagonSnapshot(
        wagon.id(),
        wagon.caravanId(),
        wagon.wagonTypeCode(),
        wagon.displayName(),
        wagon.specificCommodity(),
        wagon.currentHitPoints(),
        wagon.createdAt(),
        wagon.updatedAt());
  }

  private static CaravanWagon toWagon(WagonSnapshot snapshot) {
    return new CaravanWagon(
        snapshot.id(),
        snapshot.caravanId(),
        snapshot.wagonTypeCode(),
        snapshot.displayName(),
        snapshot.specificCommodity(),
        snapshot.currentHitPoints(),
        snapshot.createdAt(),
        snapshot.updatedAt());
  }

  private static WagonImprovementSnapshot toWagonImprovementSnapshot(CaravanWagonImprovement improvement) {
    return new WagonImprovementSnapshot(
        improvement.id(),
        improvement.caravanId(),
        improvement.wagonId(),
        improvement.improvementTypeCode(),
        improvement.createdAt(),
        improvement.updatedAt());
  }

  private static CaravanWagonImprovement toWagonImprovement(WagonImprovementSnapshot snapshot) {
    return new CaravanWagonImprovement(
        snapshot.id(),
        snapshot.caravanId(),
        snapshot.wagonId(),
        snapshot.improvementTypeCode(),
        snapshot.createdAt(),
        snapshot.updatedAt());
  }

  private static TravelerSnapshot toTravelerSnapshot(CaravanTraveler traveler) {
    return new TravelerSnapshot(
        traveler.id(),
        traveler.caravanId(),
        traveler.fullName(),
        traveler.description(),
        traveler.availableRoleCodes(),
        traveler.activeRoleCodes(),
        traveler.activeRoleCode(),
        traveler.maxActiveRoleCount(),
        traveler.roleSpecificData(),
        traveler.wagonId(),
        traveler.drivingWagonId(),
        traveler.contract(),
        traveler.consumption(),
        traveler.occupiedSpace(),
        traveler.createdAt(),
        traveler.updatedAt());
  }

  private static CaravanTraveler toTraveler(TravelerSnapshot snapshot) {
    return CaravanTraveler.create(
        snapshot.id(),
        snapshot.caravanId(),
        snapshot.fullName(),
        snapshot.description(),
        snapshot.availableRoleCodes(),
        snapshot.activeRoleCodes(),
        snapshot.activeRoleCode(),
        snapshot.maxActiveRoleCount(),
        snapshot.roleSpecificData(),
        snapshot.wagonId(),
        snapshot.drivingWagonId(),
        snapshot.contract(),
        snapshot.consumption(),
        snapshot.occupiedSpace(),
        snapshot.createdAt());
  }

  private static CargoSnapshot toCargoSnapshot(CaravanCargo cargo) {
    return new CargoSnapshot(
        cargo.id(),
        cargo.caravanId(),
        cargo.sourceType(),
        cargo.catalogCode(),
        cargo.displayName(),
        cargo.category(),
        cargo.quantity(),
        cargo.cargoUnits(),
        cargo.currentProvisions(),
        cargo.dayPassed(),
        cargo.wagonId(),
        cargo.origin(),
        cargo.specificCommodity(),
        cargo.deity(),
        cargo.notes(),
        cargo.createdAt(),
        cargo.updatedAt());
  }

  private static CaravanCargo toCargo(CargoSnapshot snapshot) {
    return new CaravanCargo(
        snapshot.id(),
        snapshot.caravanId(),
        snapshot.sourceType(),
        snapshot.catalogCode(),
        snapshot.displayName(),
        snapshot.category(),
        snapshot.quantity(),
        snapshot.cargoUnits(),
        snapshot.currentProvisions(),
        snapshot.dayPassed(),
        snapshot.wagonId(),
        snapshot.origin(),
        snapshot.specificCommodity(),
        snapshot.deity(),
        snapshot.notes(),
        snapshot.createdAt(),
        snapshot.updatedAt());
  }

  private static BeastSnapshot toBeastSnapshot(CaravanBeast beast) {
    return new BeastSnapshot(
        beast.id(),
        beast.caravanId(),
        beast.sourceType(),
        beast.catalogBeastCode(),
        beast.name(),
        beast.size(),
        beast.strength(),
        beast.speed(),
        beast.thermalAdaptation(),
        beast.basePrice(),
        beast.trainedPrice(),
        beast.fourLegged(),
        beast.specialNote(),
        beast.description(),
        beast.customNotes(),
        beast.consumption(),
        beast.assignmentType(),
        beast.assignedWagonId(),
        beast.createdAt(),
        beast.updatedAt(),
        beast.occupiedSpace());
  }

  private static CaravanBeast toBeast(BeastSnapshot snapshot) {
    return new CaravanBeast(
        snapshot.id(),
        snapshot.caravanId(),
        snapshot.sourceType(),
        snapshot.catalogBeastCode(),
        snapshot.name(),
        snapshot.size(),
        snapshot.strength(),
        snapshot.speed(),
        snapshot.thermalAdaptation(),
        snapshot.basePrice(),
        snapshot.trainedPrice(),
        snapshot.fourLegged(),
        snapshot.specialNote(),
        snapshot.description(),
        snapshot.customNotes(),
        snapshot.consumption(),
        snapshot.assignmentType(),
        snapshot.assignedWagonId(),
        snapshot.createdAt(),
        snapshot.updatedAt(),
        snapshot.occupiedSpace());
  }

  private static FeatSnapshot toFeatSnapshot(CaravanFeat feat) {
    return new FeatSnapshot(
        feat.id(),
        feat.caravanId(),
        feat.featTypeCode(),
        feat.acquisitionSourceType(),
        feat.acquisitionLevel(),
        feat.acquisitionCause(),
        feat.selectionIndex(),
        feat.active(),
        feat.manualApplies(),
        feat.manualAppliesReason(),
        feat.createdAt(),
        feat.updatedAt());
  }

  private static CaravanFeat toFeat(FeatSnapshot snapshot) {
    return new CaravanFeat(
        snapshot.id(),
        snapshot.caravanId(),
        snapshot.featTypeCode(),
        snapshot.acquisitionSourceType(),
        snapshot.acquisitionLevel(),
        snapshot.acquisitionCause(),
        snapshot.selectionIndex(),
        snapshot.active(),
        snapshot.manualApplies(),
        snapshot.manualAppliesReason(),
        snapshot.createdAt(),
        snapshot.updatedAt());
  }

  private static DayResolutionSnapshot toDayResolutionSnapshot(CaravanDayResolution resolution) {
    return new DayResolutionSnapshot(
        resolution.id(),
        resolution.caravanId(),
        resolution.idempotencyKey(),
        resolution.resolvedDayIndex(),
        resolution.resolvedAt(),
        resolution.startingReserve(),
        resolution.endingReserve(),
        resolution.totalConsumption(),
        resolution.totalGeneration(),
        resolution.netDelta(),
        resolution.shortage(),
        resolution.choicesSummary(),
        resolution.contributionsSummary(),
        resolution.warningsSummary());
  }

  private static CaravanDayResolution toDayResolution(DayResolutionSnapshot snapshot) {
    return new CaravanDayResolution(
        snapshot.id(),
        snapshot.caravanId(),
        snapshot.idempotencyKey(),
        snapshot.resolvedDayIndex(),
        snapshot.resolvedAt(),
        snapshot.startingReserve(),
        snapshot.endingReserve(),
        snapshot.totalConsumption(),
        snapshot.totalGeneration(),
        snapshot.netDelta(),
        snapshot.shortage(),
        snapshot.choicesSummary(),
        snapshot.contributionsSummary(),
        snapshot.warningsSummary());
  }
}
