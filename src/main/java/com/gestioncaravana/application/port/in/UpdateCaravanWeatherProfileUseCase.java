package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWeatherProfileView;
import com.gestioncaravana.domain.GolarionDate;
import com.gestioncaravana.domain.WeatherClimateBaseline;
import com.gestioncaravana.domain.WeatherElevation;
import java.util.UUID;

public interface UpdateCaravanWeatherProfileUseCase {

  CaravanWeatherProfileView updateProfile(UUID caravanId, UpdateCaravanWeatherProfileCommand command);

  record UpdateCaravanWeatherProfileCommand(
      WeatherClimateBaseline climateBaseline,
      WeatherElevation elevation,
      boolean crownOfWorld,
      GolarionDate effectiveFrom) {}
}
