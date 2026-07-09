package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanDailyChoiceView;
import com.gestioncaravana.application.model.CaravanDailyContributionView;
import com.gestioncaravana.application.model.CaravanDayPreviewView;
import com.gestioncaravana.application.model.CaravanDayResolutionView;
import com.gestioncaravana.application.model.CaravanSupplyConsumptionView;

final class CaravanDayCycleResponseMapper {

  private CaravanDayCycleResponseMapper() {}

  static CaravanDayCycleResponse toResponse(CaravanDayPreviewView view) {
    return new CaravanDayCycleResponse(
        view.caravanId(),
        null,
        view.dayIndex(),
        view.currentReserve(),
        view.initialProvisionsInConsumption().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.provisionsInConsumption().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.expectedConsumption(),
        view.expectedGeneration(),
        view.expectedNetDelta(),
        view.expectedReserveAfterResolution(),
        view.expectedShortage(),
        view.generatedProvisions(),
        view.generatedFood(),
        view.consumedProvisions(),
        view.surplusProvisions(),
        null,
        view.requiredChoices().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.contributions().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.warnings(),
        view.cargoMovementSummary());
  }

  static CaravanDayCycleResponse toResponse(CaravanDayResolutionView view) {
    return new CaravanDayCycleResponse(
        view.caravanId(),
        view.idempotencyKey(),
        view.resolvedDayIndex(),
        view.startingReserve(),
        view.initialProvisionsInConsumption().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.provisionsInConsumption().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.totalConsumption(),
        view.totalGeneration(),
        view.netDelta(),
        view.endingReserve(),
        view.shortage(),
        view.generatedProvisions(),
        view.generatedFood(),
        view.consumedProvisions(),
        view.surplusProvisions(),
        view.resolvedAt(),
        view.choices().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.contributions().stream().map(CaravanDayCycleResponseMapper::toResponse).toList(),
        view.warnings(),
        view.cargoMovementSummary());
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
        view.sourceRoleName(),
        view.operation(),
        view.quantity(),
        view.quantityUnit(),
        view.reason(),
        view.applied(),
        view.ignoredReason());
  }

  private static CaravanSupplyConsumptionResponse toResponse(CaravanSupplyConsumptionView view) {
    return new CaravanSupplyConsumptionResponse(view.remainingFood());
  }
}
