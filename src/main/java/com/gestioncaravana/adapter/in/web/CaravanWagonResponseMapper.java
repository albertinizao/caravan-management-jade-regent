package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanWagonImprovementView;
import com.gestioncaravana.application.model.CaravanWagonView;
import com.gestioncaravana.application.model.WagonImprovementCatalogItemView;
import com.gestioncaravana.application.model.WagonCatalogItemView;
import java.util.List;

final class CaravanWagonResponseMapper {

  private CaravanWagonResponseMapper() {}

  static WagonCatalogItemResponse toResponse(WagonCatalogItemView view) {
    return new WagonCatalogItemResponse(
        view.code(),
        view.name(),
        view.category(),
        view.cost(),
        view.hitPoints(),
        view.hardness(),
        view.propulsion(),
        view.travelerCapacity(),
        view.cargoCapacity(),
        view.limitKind(),
        view.limitFixedMax(),
        view.limitRatioDenominator(),
        view.limit(),
        view.consumption(),
        view.specialBenefit(),
        view.description(),
        view.notes());
  }

  static CaravanWagonResponse toResponse(CaravanWagonView view) {
    return new CaravanWagonResponse(
        view.id(),
        view.caravanId(),
        view.wagonTypeCode(),
        view.name(),
        view.specificCommodity(),
        view.category(),
        view.cost(),
        view.hitPoints(),
        view.hardness(),
        view.propulsion(),
        view.travelerCapacity(),
        view.cargoCapacity(),
        view.limitKind(),
        view.limitFixedMax(),
        view.limitRatioDenominator(),
        view.limit(),
        view.consumption(),
        view.specialBenefit(),
        view.description(),
        view.notes(),
        view.draftBeasts().stream().map(CaravanBeastResponseMapper::toResponse).toList(),
        view.draftStrength(),
        view.draftRequiredStrength(),
        view.improvements().stream().map(CaravanWagonResponseMapper::toResponse).toList(),
        view.createdAt(),
        view.updatedAt());
  }

  static WagonImprovementCatalogItemResponse toResponse(WagonImprovementCatalogItemView view) {
    return new WagonImprovementCatalogItemResponse(
        view.code(),
        view.name(),
        view.category(),
        view.costExpression(),
        view.hitPointsBonus(),
        view.hitPointsMultiplier(),
        view.hardnessBonus(),
        view.hardnessMultiplier(),
        view.propulsionEffect(),
        view.travelerCapacityBonus(),
        view.travelerCapacityMultiplier(),
        view.travelerCapacityMinimumIncrement(),
        view.travelerCapacityOverride(),
        view.cargoCapacityBonus(),
        view.cargoCapacityMultiplier(),
        view.cargoCapacityMinimumIncrement(),
        view.cargoCapacityOverride(),
        view.consumptionBonus(),
        view.maxPerWagon(),
        view.repeatable(),
        view.requiredBasePropulsionFragment(),
        view.ownedCount(),
        view.available(),
        view.blockedReason(),
        view.specialBenefit(),
        view.description(),
        view.notes());
  }

  static CaravanWagonImprovementResponse toResponse(CaravanWagonImprovementView view) {
    return new CaravanWagonImprovementResponse(
        view.id(),
        view.caravanId(),
        view.wagonId(),
        view.improvementTypeCode(),
        view.name(),
        view.category(),
        view.costExpression(),
        view.specialBenefit(),
        view.description(),
        view.notes(),
        view.createdAt(),
        view.updatedAt());
  }
}
