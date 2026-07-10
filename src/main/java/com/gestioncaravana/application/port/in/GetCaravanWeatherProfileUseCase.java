package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanWeatherProfileView;
import java.util.UUID;

public interface GetCaravanWeatherProfileUseCase {

  CaravanWeatherProfileView getProfile(UUID caravanId);
}
