package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CargoCatalogItemView;
import com.gestioncaravana.application.model.CaravanCargoSummaryView;
import com.gestioncaravana.application.model.CaravanCargoView;

final class CaravanCargoResponseMapper {

  private CaravanCargoResponseMapper() {}

  static CargoCatalogItemResponse toResponse(CargoCatalogItemView view) {
    return new CargoCatalogItemResponse(
        view.code(),
        view.name(),
        view.category(),
        view.priceExpression(),
        view.defaultQuantity(),
        view.quantityEditable(),
        view.defaultCargoUnits(),
        view.cargoUnitsEditable(),
        view.quantityLabel(),
        view.benefitText(),
        view.description(),
        view.notes(),
        view.requiredMetadataKeys(),
        view.allowedWagonCodes());
  }

  static CaravanCargoResponse toResponse(CaravanCargoView view) {
    return new CaravanCargoResponse(
        view.id(),
        view.caravanId(),
        view.sourceType(),
        view.sourceTypeLabel(),
        view.catalogCode(),
        view.catalogName(),
        view.displayName(),
        view.category(),
        view.quantity(),
        view.cargoUnits(),
        view.wagonId(),
        view.wagonName(),
        view.origin(),
        view.specificCommodity(),
        view.deity(),
        view.notes(),
        view.currentProvisions(),
        view.dayPassed(),
        view.priceExpression(),
        view.createdAt(),
        view.updatedAt());
  }

  static CaravanCargoSummaryResponse toResponse(CaravanCargoSummaryView view) {
    return new CaravanCargoSummaryResponse(
        view.wagonId(),
        view.wagonName(),
        view.cargoCapacity(),
        view.usedCargoUnits(),
        view.remainingCargoUnits(),
        view.cargoEntryCount());
  }
}
