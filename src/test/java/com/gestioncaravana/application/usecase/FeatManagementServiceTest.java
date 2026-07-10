package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.model.CaravanDerivedStatsView;
import com.gestioncaravana.application.model.CaravanFeatCatalogItemView;
import com.gestioncaravana.application.model.CaravanMainStatsView;
import com.gestioncaravana.application.model.CaravanOtherStatsView;
import com.gestioncaravana.application.model.CaravanStatisticsView;
import com.gestioncaravana.application.model.CaravanStatContributionView;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatCatalogPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanFeatType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatManagementServiceTest {

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryFeatRepository featRepository;
  private FeatManagementService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    featRepository = new InMemoryFeatRepository();
    service = new FeatManagementService(
        caravanRepository,
        featRepository,
        new InMemoryCatalogPort(),
        new StubStatisticsUseCase(),
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void listsAndLoadsCatalogFromConfiguredPort() {
    var caravan = createCaravan(2);

    var catalog = service.listCatalog(caravan.id());

    assertThat(catalog).extracting(CaravanFeatCatalogItemView::code)
        .contains("caravana-mejorada", "caravana-santificada");
  }

  @Test
  void addsAndUpdatesFeatWithAcquisitionMetadata() {
    var caravan = createCaravan(2);

    var created = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-mejorada",
            CaravanFeatAcquisitionSourceType.LEVEL_UP,
            2,
            null,
            true,
            null,
            null));

    assertThat(created.name()).isEqualTo("Caravana Mejorada");
    assertThat(created.acquisitionLevel()).isEqualTo(2);
    assertThat(created.active()).isTrue();

    var updated = service.execute(
        caravan.id(),
        created.id(),
        new com.gestioncaravana.application.port.in.UpdateCaravanFeatUseCase.UpdateCaravanFeatCommand(
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "recompensa de campaña",
            false,
            null,
            null));

    assertThat(updated.acquisitionSourceType()).isEqualTo(CaravanFeatAcquisitionSourceType.OTHER);
    assertThat(updated.acquisitionCause()).isEqualTo("recompensa de campaña");
    assertThat(updated.active()).isFalse();
  }

  @Test
  void grantsAndRemovesTwoFreePointsForEachActiveCaravanaMejoradaInstance() {
    var caravan = createCaravan(2);

    var created = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-mejorada",
            CaravanFeatAcquisitionSourceType.LEVEL_UP,
            2,
            null,
            true,
            null,
            null));

    assertThat(created.active()).isTrue();
    assertThat(caravanRepository.findById(caravan.id()).orElseThrow().mainStats().unassignedPoints()).isEqualTo(5);

    service.execute(
        caravan.id(),
        created.id(),
        new com.gestioncaravana.application.port.in.UpdateCaravanFeatUseCase.UpdateCaravanFeatCommand(
            CaravanFeatAcquisitionSourceType.LEVEL_UP,
            2,
            null,
            false,
            null,
            null));

    assertThat(caravanRepository.findById(caravan.id()).orElseThrow().mainStats().unassignedPoints()).isEqualTo(3);
  }

  @Test
  void deletesActiveCaravanaMejoradaAndRestoresItsBonusPoints() {
    var caravan = createCaravan(2);

    var created = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-mejorada",
            CaravanFeatAcquisitionSourceType.LEVEL_UP,
            2,
            null,
            true,
            null,
            null));

    assertThat(caravanRepository.findById(caravan.id()).orElseThrow().mainStats().unassignedPoints()).isEqualTo(5);

    service.delete(caravan.id(), created.id());

    assertThat(featRepository.findById(caravan.id(), created.id())).isEmpty();
    assertThat(caravanRepository.findById(caravan.id()).orElseThrow().mainStats().unassignedPoints()).isEqualTo(3);
  }

  @Test
  void keepsRepeatableSelectionIndexesMonotonicAfterDeletion() {
    var caravan = createCaravan(2);

    var first = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-santificada",
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "ritual",
            true,
            true,
            "primera selección"));
    var second = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-santificada",
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "ritual",
            true,
            true,
            "segunda selección"));

    service.delete(caravan.id(), first.id());

    var third = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-santificada",
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "ritual",
            true,
            true,
            "tercera selección"));

    assertThat(second.selectionIndex()).isEqualTo(2);
    assertThat(third.selectionIndex()).isEqualTo(3);
  }

  @Test
  void persistsManualAppliesStateForManualFeats() {
    var caravan = createCaravan(2);

    var created = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-santificada",
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "ritual",
            true,
            false,
            "pendiente de altar"));

    assertThat(created.manualApplies()).isFalse();
    assertThat(created.manualAppliesReason()).isEqualTo("pendiente de altar");
    assertThat(created.active()).isFalse();
  }

  @Test
  void rejectsLevelUpAcquisitionAboveCaravanLevel() {
    var caravan = createCaravan(1);

    assertThatThrownBy(() -> service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "caravana-mejorada",
            CaravanFeatAcquisitionSourceType.LEVEL_UP,
            2,
            null,
            true,
            null,
            null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("acquisitionLevel cannot be greater than caravan level");
  }

  @Test
  void allowsRepeatableFeatsUntilTheirLimitIsReached() {
    var caravan = createCaravan(4);

    var first = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "carros-adicionales",
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "campaña",
            true,
            null,
            null));
    var second = service.execute(
        caravan.id(),
        new com.gestioncaravana.application.port.in.AddCaravanFeatUseCase.AddCaravanFeatCommand(
            "carros-adicionales",
            CaravanFeatAcquisitionSourceType.OTHER,
            null,
            "campaña",
            true,
            null,
            null));

    assertThat(first.selectionIndex()).isEqualTo(1);
    assertThat(second.selectionIndex()).isEqualTo(2);
  }

  @Test
  void marksFeatInactiveWhenNumericPrerequisiteIsMissing() {
    var caravan = createCaravan(1);
    caravanRepository.save(new CaravanCampaign(
        caravan.id(),
        caravan.name(),
        caravan.description(),
        caravan.level(),
        caravan.mainStats(),
        caravan.discontent(),
        caravan.status(),
        caravan.createdAt(),
        caravan.updatedAt()));
    featRepository.save(CaravanFeat.create(
        UUID.randomUUID(),
        caravan.id(),
        "caravana-mejorada",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "campaña",
        1,
        true,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z")));

    var feats = service.list(caravan.id());

    assertThat(feats).singleElement().satisfies(feat -> {
      assertThat(feat.active()).isFalse();
      assertThat(feat.blockedReason()).contains("level 2");
    });
  }

  private CaravanCampaign createCaravan(int level) {
    var caravan = CaravanCampaign.create(
        UUID.randomUUID(),
        "Campaign",
        null,
        Instant.parse("2026-01-01T00:00:00Z"));
    var adjusted = new CaravanCampaign(
        caravan.id(),
        caravan.name(),
        caravan.description(),
        level,
        caravan.mainStats(),
        caravan.discontent(),
        caravan.status(),
        caravan.createdAt(),
        caravan.updatedAt());
    caravanRepository.save(adjusted);
    return adjusted;
  }

  private static final class InMemoryCaravanRepository implements CaravanCampaignRepositoryPort {
    private final List<CaravanCampaign> caravans = new ArrayList<>();

    @Override
    public CaravanCampaign save(CaravanCampaign caravanCampaign) {
      caravans.removeIf(existing -> existing.id().equals(caravanCampaign.id()));
      caravans.add(caravanCampaign);
      return caravanCampaign;
    }

    @Override
    public void deleteById(UUID id) {
      caravans.removeIf(existing -> existing.id().equals(id));
    }

    @Override
    public List<CaravanCampaign> findAll() {
      return List.copyOf(caravans);
    }

    @Override
    public Optional<CaravanCampaign> findById(UUID id) {
      return caravans.stream().filter(caravan -> caravan.id().equals(id)).findFirst();
    }
  }

  private static final class InMemoryFeatRepository implements CaravanFeatRepositoryPort {
    private final List<CaravanFeat> feats = new ArrayList<>();

    @Override
    public CaravanFeat save(CaravanFeat feat) {
      feats.removeIf(existing -> existing.id().equals(feat.id()));
      feats.add(feat);
      return feat;
    }

    @Override
    public List<CaravanFeat> findAllByCaravanId(UUID caravanId) {
      return feats.stream().filter(feat -> feat.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanFeat> findById(UUID caravanId, UUID featId) {
      return feats.stream()
          .filter(feat -> feat.caravanId().equals(caravanId) && feat.id().equals(featId))
          .findFirst();
    }

    @Override
    public long countByCaravanIdAndFeatTypeCode(UUID caravanId, String featTypeCode) {
      return feats.stream()
          .filter(feat -> feat.caravanId().equals(caravanId) && feat.featTypeCode().equals(featTypeCode))
          .count();
    }

    @Override
    public void deleteById(UUID caravanId, UUID featId) {
      feats.removeIf(feat -> feat.caravanId().equals(caravanId) && feat.id().equals(featId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      feats.removeIf(feat -> feat.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryCatalogPort implements CaravanFeatCatalogPort {
    private final List<CaravanFeatType> feats = List.of(
        new CaravanFeatType(
            "caravana-mejorada",
            "Caravana Mejorada",
            "La caravana mejora su rendimiento general.",
            List.of("Nivel 2"),
            "Cada instancia añade 2 puntos libres a las estadísticas principales de la caravana.",
            "Esta dote puede seleccionarse varias veces.",
            null,
            true,
            999,
            2,
            "selection-based passive",
            "unassignedMainStatPoints",
            "Each active instance grants 2 free main-stat points, stacks once per active instance, and recalculates caravan stats after every feat change."),
        new CaravanFeatType(
            "caravana-santificada",
            "Caravana Santificada",
            "La caravana cuenta con la bendición de una deidad.",
            List.of("Determinación 5."),
            "Elige una deidad.",
            "Puede elegirse esta dote varias veces.",
            null,
            true,
            999,
            null,
            "manual plus passive calculation",
            "manualApplies:boolean, selectedDeityCode, selectedAltars[]",
            "The user marks whether the relationship conditions currently hold. When manualApplies=true, apply +3 effective caster level to divine spells cast by that deity’s clerics within 200 feet of the caravan."),
        new CaravanFeatType(
            "carros-adicionales",
            "Carros Adicionales",
            "La caravana aprende a transportar más carros sin penalizadores.",
            List.of(),
            "Aumenta el número máximo de carros que se pueden añadir a la caravana igual al nivel de la caravana, sin incurrir en ningún penalizador por exceso de carros.",
            null,
            null,
            true,
            999,
            null,
            "passive / selection-based",
            "selectionCount:int",
            "Increase the wagon limit by the caravan level per selection. The bonus stacks with repeated selections."));

    @Override
    public List<CaravanFeatType> all() {
      return feats;
    }

    @Override
    public Optional<CaravanFeatType> findByCode(String code) {
      return feats.stream().filter(feat -> feat.code().equals(code)).findFirst();
    }
  }

  private static final class StubStatisticsUseCase implements GetCaravanStatisticsUseCase {
    @Override
    public CaravanStatisticsView getById(UUID caravanId) {
      return new CaravanStatisticsView(
          caravanId,
          2,
          new CaravanMainStatsView(5, 5, 5, 5, 0),
          new CaravanDerivedStatsView(0, 0, 8, 12),
          new CaravanOtherStatsView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
          0,
          0,
          List.<CaravanStatContributionView>of(),
          List.of(),
          Instant.parse("2026-01-01T00:00:00Z"));
    }
  }
}
