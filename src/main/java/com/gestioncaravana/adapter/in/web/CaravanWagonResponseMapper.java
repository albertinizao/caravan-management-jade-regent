package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanWagonView;
import com.gestioncaravana.application.model.WagonCatalogItemView;

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
        view.createdAt(),
        view.updatedAt());
  }
}
