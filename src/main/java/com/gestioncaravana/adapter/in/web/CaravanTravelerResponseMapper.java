package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanTravelerView;
import com.gestioncaravana.application.model.TravelerRoleCatalogItemView;

final class CaravanTravelerResponseMapper {

  private CaravanTravelerResponseMapper() {}

  static CaravanTravelerResponse toResponse(CaravanTravelerView view) {
    return new CaravanTravelerResponse(
        view.id(),
        view.caravanId(),
        view.fullName(),
        view.description(),
        view.availableRoleCodes(),
        view.activeRoleCodes(),
        view.activeRoleCode(),
        view.activeRoleName(),
        view.wagonId(),
        view.wagonName(),
        view.drivingWagonId(),
        view.drivingWagonName(),
        view.maxActiveRoleCount(),
        view.salary(),
        view.contractConditions(),
        view.consumption(),
        view.servedTravelerId(),
        view.servedTravelerName(),
        view.createdAt(),
        view.updatedAt());
  }

  static TravelerRoleCatalogItemResponse toResponse(TravelerRoleCatalogItemView view) {
    return new TravelerRoleCatalogItemResponse(
        view.code(),
        view.name(),
        view.description(),
        view.requirements(),
        view.requiresTargetTraveler(),
        view.helperBenefitMode(),
        view.helperPeriodDays());
  }
}
