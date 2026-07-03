package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanBeastCatalogItemView;
import com.gestioncaravana.application.model.CaravanBeastView;

final class CaravanBeastResponseMapper {

  private CaravanBeastResponseMapper() {}

  static CaravanBeastCatalogItemResponse toResponse(CaravanBeastCatalogItemView view) {
    return new CaravanBeastCatalogItemResponse(
        view.code(),
        view.name(),
        view.basePrice(),
        view.trainedPrice(),
        view.size(),
        view.strength(),
        view.speed(),
        view.thermalAdaptation(),
        view.fourLegged(),
        view.specialNote(),
        view.description(),
        view.notes(),
        view.occupiedSpace());
  }

  static CaravanBeastResponse toResponse(CaravanBeastView view) {
    return new CaravanBeastResponse(
        view.id(),
        view.caravanId(),
        view.sourceType(),
        view.catalogBeastCode(),
        view.name(),
        view.size(),
        view.strength(),
        view.speed(),
        view.thermalAdaptation(),
        view.basePrice(),
        view.trainedPrice(),
        view.fourLegged(),
        view.specialNote(),
        view.description(),
        view.customNotes(),
        view.assignmentType(),
        view.assignedWagonId(),
        view.assignedWagonName(),
        view.createdAt(),
        view.updatedAt(),
        view.occupiedSpace());
  }
}
