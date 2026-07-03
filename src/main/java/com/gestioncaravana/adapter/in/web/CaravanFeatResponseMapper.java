package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanFeatCatalogItemView;
import com.gestioncaravana.application.model.CaravanFeatView;

final class CaravanFeatResponseMapper {

  private CaravanFeatResponseMapper() {}

  static CaravanFeatCatalogItemResponse toResponse(CaravanFeatCatalogItemView view) {
    return new CaravanFeatCatalogItemResponse(
        view.code(),
        view.name(),
        view.description(),
        view.prerequisites(),
        view.benefitText(),
        view.specialText(),
        view.notes(),
        view.repeatable(),
        view.selectionLimit(),
        view.minimumLevel(),
        view.automationMode(),
        view.automationStateInputs(),
        view.automationExactAutomation(),
        view.ownedCount(),
        view.available(),
        view.blockedReason());
  }

  static CaravanFeatResponse toResponse(CaravanFeatView view) {
    return new CaravanFeatResponse(
        view.id(),
        view.caravanId(),
        view.featTypeCode(),
        view.name(),
        view.description(),
        view.prerequisites(),
        view.benefitText(),
        view.specialText(),
        view.notes(),
        view.acquisitionSourceType(),
        view.acquisitionLevel(),
        view.acquisitionCause(),
        view.selectionIndex(),
        view.active(),
        view.blockedReason(),
        view.manualApplies(),
        view.manualAppliesReason(),
        view.automationMode(),
        view.automationStateInputs(),
        view.automationExactAutomation(),
        view.createdAt(),
        view.updatedAt());
  }
}
