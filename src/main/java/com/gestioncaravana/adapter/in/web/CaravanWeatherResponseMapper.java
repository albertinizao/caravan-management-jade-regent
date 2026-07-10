package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.model.CaravanWeatherProfileView;

final class CaravanWeatherResponseMapper {

  private CaravanWeatherResponseMapper() {}

  static CaravanWeatherProfileResponse toResponse(CaravanWeatherProfileView view) {
    return new CaravanWeatherProfileResponse(
        view.climateBaseline().name(),
        view.elevation().name(),
        view.crownOfWorld(),
        view.updatedAt().toString());
  }
}
