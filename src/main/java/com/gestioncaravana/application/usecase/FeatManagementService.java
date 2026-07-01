package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanFeatCatalogItemView;
import com.gestioncaravana.application.model.CaravanFeatView;
import com.gestioncaravana.application.model.CaravanStatisticsView;
import com.gestioncaravana.application.port.in.AddCaravanFeatUseCase;
import com.gestioncaravana.application.port.in.GetCaravanFeatUseCase;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.in.ListCaravanFeatCatalogUseCase;
import com.gestioncaravana.application.port.in.ListCaravanFeatsUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanFeatUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatCatalogPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanFeatType;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeatManagementService
    implements ListCaravanFeatCatalogUseCase,
        ListCaravanFeatsUseCase,
        GetCaravanFeatUseCase,
        AddCaravanFeatUseCase,
        UpdateCaravanFeatUseCase {

  private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanFeatRepositoryPort featRepository;
  private final CaravanFeatCatalogPort catalogPort;
  private final GetCaravanStatisticsUseCase statisticsUseCase;
  private final Clock clock;

  public FeatManagementService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanFeatRepositoryPort featRepository,
      CaravanFeatCatalogPort catalogPort,
      GetCaravanStatisticsUseCase statisticsUseCase,
      Clock clock) {
    this.caravanRepository = caravanRepository;
    this.featRepository = featRepository;
    this.catalogPort = catalogPort;
    this.statisticsUseCase = statisticsUseCase;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanFeatCatalogItemView> listCatalog(UUID caravanId) {
    var caravan = requireCaravan(caravanId);
    var stats = statisticsUseCase.getById(caravanId);
    var owned = featRepository.findAllByCaravanId(caravanId);

    return catalogPort.all().stream()
        .map(featType -> toCatalogView(caravan, featType, owned, stats))
        .sorted(Comparator.comparing(CaravanFeatCatalogItemView::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaravanFeatView> list(UUID caravanId) {
    var caravan = requireCaravan(caravanId);
    var stats = statisticsUseCase.getById(caravanId);
    var owned = featRepository.findAllByCaravanId(caravanId);

    return owned.stream()
        .map(feat -> toView(caravan, feat, owned, stats))
        .sorted(
            Comparator.comparing(CaravanFeatView::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(CaravanFeatView::selectionIndex))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanFeatView getById(UUID caravanId, UUID featId) {
    var caravan = requireCaravan(caravanId);
    var stats = statisticsUseCase.getById(caravanId);
    var owned = featRepository.findAllByCaravanId(caravanId);

    return featRepository.findById(caravanId, featId)
        .map(feat -> toView(caravan, feat, owned, stats))
        .orElseThrow(() -> new IllegalArgumentException("Feat not found: " + featId));
  }

  @Override
  public CaravanFeatView execute(UUID caravanId, AddCaravanFeatCommand command) {
    var caravan = requireCaravan(caravanId);
    validateAcquisition(
        caravan.level(),
        command.acquisitionSourceType(),
        command.acquisitionLevel(),
        command.acquisitionCause());

    var featType = requireFeatType(command.featTypeCode());
    var stats = statisticsUseCase.getById(caravanId);
    var owned = featRepository.findAllByCaravanId(caravanId);
    var blockedReason = calculateCatalogBlockedReason(caravan, featType, owned, stats);
    if (blockedReason != null) {
      throw new IllegalArgumentException(blockedReason);
    }

    var selectionIndex =
        (int)
                owned.stream()
                    .filter(feat -> feat.featTypeCode().equals(featType.code()))
                    .count()
            + 1;
    var created =
        CaravanFeat.create(
            UUID.randomUUID(),
            caravanId,
            featType.code(),
            command.acquisitionSourceType(),
            command.acquisitionLevel(),
            command.acquisitionCause(),
            selectionIndex,
            command.active() == null ? true : command.active(),
            clock.instant());
    var saved = featRepository.save(created);
    var refreshedOwned = featRepository.findAllByCaravanId(caravanId);
    return toView(caravan, saved, refreshedOwned, stats);
  }

  @Override
  public CaravanFeatView execute(UUID caravanId, UUID featId, UpdateCaravanFeatCommand command) {
    var caravan = requireCaravan(caravanId);
    validateAcquisition(
        caravan.level(),
        command.acquisitionSourceType(),
        command.acquisitionLevel(),
        command.acquisitionCause());

    var existing =
        featRepository
            .findById(caravanId, featId)
            .orElseThrow(() -> new IllegalArgumentException("Feat not found: " + featId));
    var updated =
        existing.updateAcquisition(
            command.acquisitionSourceType(),
            command.acquisitionLevel(),
            command.acquisitionCause(),
            command.active(),
            clock.instant());
    var saved = featRepository.save(updated);
    var stats = statisticsUseCase.getById(caravanId);
    var owned = featRepository.findAllByCaravanId(caravanId);
    return toView(caravan, saved, owned, stats);
  }

  private CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository
        .findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private CaravanFeatType requireFeatType(String featTypeCode) {
    return catalogPort.findByCode(featTypeCode)
        .orElseThrow(() -> new IllegalArgumentException("Feat type not found: " + featTypeCode));
  }

  private void validateAcquisition(
      int caravanLevel,
      CaravanFeatAcquisitionSourceType sourceType,
      Integer acquisitionLevel,
      String acquisitionCause) {
    if (sourceType == null) {
      throw new IllegalArgumentException("acquisitionSourceType is required");
    }
    if (sourceType == CaravanFeatAcquisitionSourceType.LEVEL_UP) {
      if (acquisitionLevel == null || acquisitionLevel < 1) {
        throw new IllegalArgumentException("acquisitionLevel is required for level-up feats");
      }
      if (acquisitionLevel > caravanLevel) {
        throw new IllegalArgumentException("acquisitionLevel cannot be greater than caravan level");
      }
      if (acquisitionCause != null && !acquisitionCause.isBlank()) {
        throw new IllegalArgumentException("acquisitionCause must be empty for level-up feats");
      }
      return;
    }

    if (acquisitionCause == null || acquisitionCause.isBlank()) {
      throw new IllegalArgumentException("acquisitionCause is required for non-level-up feats");
    }
    if (acquisitionLevel != null) {
      throw new IllegalArgumentException("acquisitionLevel must be empty for non-level-up feats");
    }
  }

  private CaravanFeatCatalogItemView toCatalogView(
      CaravanCampaign caravan,
      CaravanFeatType featType,
      List<CaravanFeat> owned,
      CaravanStatisticsView stats) {
    var ownedCount = ownedCount(owned, featType.code());
    var blockedReason = calculateCatalogBlockedReason(caravan, featType, owned, stats);
    return new CaravanFeatCatalogItemView(
        featType.code(),
        featType.name(),
        featType.prerequisites(),
        featType.benefitText(),
        featType.specialText(),
        featType.notes(),
        featType.repeatable(),
        featType.selectionLimit(),
        featType.minimumLevel(),
        ownedCount,
        blockedReason == null,
        blockedReason);
  }

  private CaravanFeatView toView(
      CaravanCampaign caravan,
      CaravanFeat feat,
      List<CaravanFeat> owned,
      CaravanStatisticsView stats) {
    var featType = requireFeatType(feat.featTypeCode());
    var blockedReason = calculateOwnedBlockedReason(caravan, featType, owned, stats, feat.id());
    return new CaravanFeatView(
        feat.id(),
        feat.caravanId(),
        feat.featTypeCode(),
        featType.name(),
        featType.prerequisites(),
        featType.benefitText(),
        featType.specialText(),
        featType.notes(),
        feat.acquisitionSourceType(),
        feat.acquisitionLevel(),
        feat.acquisitionCause(),
        feat.selectionIndex(),
        feat.active() && blockedReason == null,
        blockedReason,
        feat.createdAt(),
        feat.updatedAt());
  }

  private String calculateCatalogBlockedReason(
      CaravanCampaign caravan,
      CaravanFeatType featType,
      List<CaravanFeat> owned,
      CaravanStatisticsView stats) {
    var requirementBlockedReason = calculateRequirementBlockedReason(caravan, featType, owned, stats, null);
    if (requirementBlockedReason != null) {
      return requirementBlockedReason;
    }

    var sameTypeCount = ownedCount(owned, featType.code());
    if (!featType.repeatable() && sameTypeCount > 0) {
      return "Feat already owned";
    }
    if (sameTypeCount >= featType.selectionLimit()) {
      return "Repeat limit reached";
    }
    return null;
  }

  private String calculateOwnedBlockedReason(
      CaravanCampaign caravan,
      CaravanFeatType featType,
      List<CaravanFeat> owned,
      CaravanStatisticsView stats,
      UUID featId) {
    return calculateRequirementBlockedReason(caravan, featType, owned, stats, featId);
  }

  private String calculateRequirementBlockedReason(
      CaravanCampaign caravan,
      CaravanFeatType featType,
      List<CaravanFeat> owned,
      CaravanStatisticsView stats,
      UUID ignoredFeatId) {
    if (featType.minimumLevel() != null && caravan.level() < featType.minimumLevel()) {
      return "Requires caravan level " + featType.minimumLevel();
    }

    for (var requirement : featType.prerequisites()) {
      var blockedReason = evaluateRequirement(requirement, caravan, owned, stats, ignoredFeatId);
      if (blockedReason != null) {
        return blockedReason;
      }
    }

    return null;
  }

  private String evaluateRequirement(
      String requirement,
      CaravanCampaign caravan,
      List<CaravanFeat> owned,
      CaravanStatisticsView stats,
      UUID ignoredFeatId) {
    var normalized = normalize(requirement);

    if (normalized.contains("nivel")) {
      var value = extractNumber(normalized);
      if (value != null && caravan.level() < value) {
        return "Requires caravan level " + value;
      }
    }
    if (normalized.contains("seguridad")) {
      var value = extractNumber(normalized);
      if (value != null && stats.derivedStats().security() < value) {
        return "Requires Seguridad " + value;
      }
    }
    if (normalized.contains("determinacion")) {
      var value = extractNumber(normalized);
      if (value != null && stats.derivedStats().determination() < value) {
        return "Requires Determinación " + value;
      }
    }
    if (normalized.contains("ofensiva")) {
      var value = extractNumber(normalized);
      if (value != null && stats.mainStats().offense() < value) {
        return "Requires Ofensiva " + value;
      }
    }
    if (normalized.contains("defensa") || normalized.contains("defensiva")) {
      var value = extractNumber(normalized);
      if (value != null && stats.mainStats().defense() < value) {
        return "Requires Defensa " + value;
      }
    }
    if (normalized.contains("movilidad")) {
      var value = extractNumber(normalized);
      if (value != null && stats.mainStats().mobility() < value) {
        return "Requires Movilidad " + value;
      }
    }
    if (normalized.contains("moral")) {
      var value = extractNumber(normalized);
      if (value != null && stats.mainStats().morale() < value) {
        return "Requires Moral " + value;
      }
    }

    for (var dependency : catalogPort.all()) {
      if (dependency.code().equals(featTypeCodeFromIgnored(owned, ignoredFeatId))) {
        continue;
      }
      var dependencyName = normalize(dependency.name());
      if (!normalized.contains(dependencyName)) {
        continue;
      }

      var dependencyOwned =
          owned.stream()
              .filter(feat -> !feat.id().equals(ignoredFeatId))
              .anyMatch(feat -> feat.featTypeCode().equals(dependency.code()));
      if (!dependencyOwned) {
        return "Requires " + dependency.name();
      }
    }

    return null;
  }

  private String featTypeCodeFromIgnored(List<CaravanFeat> owned, UUID ignoredFeatId) {
    if (ignoredFeatId == null) {
      return null;
    }
    return owned.stream()
        .filter(feat -> feat.id().equals(ignoredFeatId))
        .map(CaravanFeat::featTypeCode)
        .findFirst()
        .orElse(null);
  }

  private int ownedCount(List<CaravanFeat> owned, String featTypeCode) {
    return (int) owned.stream().filter(feat -> feat.featTypeCode().equals(featTypeCode)).count();
  }

  private Integer extractNumber(String text) {
    var matcher = NUMBER_PATTERN.matcher(text);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
  }

  private String normalize(String text) {
    if (text == null) {
      return "";
    }
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
