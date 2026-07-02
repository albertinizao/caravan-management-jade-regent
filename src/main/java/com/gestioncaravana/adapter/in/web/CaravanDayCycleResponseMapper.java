package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanDailyChoiceView;
import com.gestioncaravana.application.model.CaravanDailyContributionView;
import com.gestioncaravana.application.model.CaravanDayPreviewView;
import com.gestioncaravana.application.model.CaravanDayResolutionView;

final class CaravanDayCycleResponseMapper {

  private CaravanDayCycleResponseMapper() {}

  static CaravanDayCycleResponse toResponse(CaravanDayPreviewView view) {
    return new CaravanDayCycleResponse(
        view.caravanId(),
        null,
        view.dayIndex(),
        view.currentReserve(),
        view.expectedConsumption(),
        view.expectedGeneration(),
        view.expectedNetDelta(),
        view.expectedReserveAfterResolution(),
        view.expectedShortage(),
        null,
        view.requiredChoices().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.contributions().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.warnings());
  }

  static CaravanDayCycleResponse toResponse(CaravanDayResolutionView view) {
    return new CaravanDayCycleResponse(
        view.caravanId(),
        view.idempotencyKey(),
        view.resolvedDayIndex(),
        view.startingReserve(),
        view.totalConsumption(),
        view.totalGeneration(),
        view.netDelta(),
        view.endingReserve(),
        view.shortage(),
        view.resolvedAt(),
        view.choices().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.contributions().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.warnings());
  }

  private static CaravanDailyChoiceResponse toResponse(CaravanDailyChoiceView view) {
    return new CaravanDailyChoiceResponse(view.travelerId(), view.mode());
  }

  private static CaravanDailyContributionResponse toResponse(CaravanDailyContributionView view) {
    return new CaravanDailyContributionResponse(
        view.effectCode(),
        view.sourceType(),
        view.sourceId(),
        view.sourceName(),
        view.operation(),
        view.quantity(),
        view.reason(),
        view.applied(),
        view.ignoredReason());
  }
}
