package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanCampaignView;
import com.gestioncaravana.application.model.CaravanMainStatsView;

final class CaravanResponseMapper {

  private CaravanResponseMapper() {}

  static CaravanResponse toResponse(CaravanCampaignView view) {
    return new CaravanResponse(
        view.id(),
        view.name(),
        view.description(),
        view.level(),
        toResponse(view.mainStats()),
        view.discontent(),
        view.status(),
        view.active(),
        view.createdAt(),
        view.updatedAt(),
        view.wagons(),
        view.travelers(),
        view.beasts(),
        view.feats());
  }

  private static CaravanMainStatsResponse toResponse(CaravanMainStatsView view) {
    return new CaravanMainStatsResponse(
        view.offense(), view.defense(), view.mobility(), view.morale(), view.unassignedPoints());
  }
}

