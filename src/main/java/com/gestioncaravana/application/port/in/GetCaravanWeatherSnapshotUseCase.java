package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.WeatherSnapshotView;
import com.gestioncaravana.domain.GolarionDate;
import java.util.UUID;

public interface GetCaravanWeatherSnapshotUseCase {

  WeatherSnapshotView getWeather(UUID caravanId, GolarionDate date);
}
