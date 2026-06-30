package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanDerivedStatsView;
import com.gestioncaravana.application.model.CaravanMainStatsView;
import com.gestioncaravana.application.model.CaravanOtherStatsView;
import com.gestioncaravana.application.model.CaravanStatContributionView;
import com.gestioncaravana.application.model.CaravanStatisticsView;

final class CaravanStatisticsResponseMapper {

  private CaravanStatisticsResponseMapper() {}

  static CaravanStatisticsResponse toResponse(CaravanStatisticsView view) {
    return new CaravanStatisticsResponse(
        view.caravanId(),
        view.level(),
        toResponse(view.mainStats()),
        toResponse(view.derivedStats()),
        toResponse(view.otherStats()),
        view.discontent(),
        view.moraleThreshold(),
        view.contributions().stream().map(CaravanStatisticsResponseMapper::toResponse).toList(),
        view.warnings(),
        view.updatedAt());
  }

  private static CaravanMainStatsResponse toResponse(CaravanMainStatsView view) {
    return new CaravanMainStatsResponse(view.offense(), view.defense(), view.mobility(), view.morale(), view.unassignedPoints());
  }

  private static CaravanDerivedStatsResponse toResponse(CaravanDerivedStatsView view) {
    return new CaravanDerivedStatsResponse(view.attack(), view.armorClass(), view.security(), view.determination());
  }

  private static CaravanOtherStatsResponse toResponse(CaravanOtherStatsView view) {
    return new CaravanOtherStatsResponse(
        view.speed(),
        view.travelerCapacity(),
        view.cargoCapacity(),
        view.cargoLoad(),
        view.cargoRemaining(),
        view.consumption(),
        view.travelerCount(),
        view.wagonCount(),
        view.beastCount(),
        view.maxWagons());
  }

  private static CaravanStatContributionResponse toResponse(CaravanStatContributionView view) {
    return new CaravanStatContributionResponse(
        view.statCode(),
        view.sourceType(),
        view.sourceId(),
        view.sourceName(),
        view.modifier(),
        view.operation(),
        view.reason());
  }
}
