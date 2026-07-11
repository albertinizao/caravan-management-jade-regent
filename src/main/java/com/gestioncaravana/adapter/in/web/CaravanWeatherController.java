package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.port.in.GetCaravanWeatherProfileUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanWeatherProfileUseCase;
import com.gestioncaravana.domain.CrownWeatherRegion;
import com.gestioncaravana.domain.GolarionDate;
import com.gestioncaravana.domain.WeatherClimateBaseline;
import com.gestioncaravana.domain.WeatherElevation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caravans/{caravanId}/weather/profile")
public class CaravanWeatherController {

  private final GetCaravanWeatherProfileUseCase getCaravanWeatherProfileUseCase;
  private final UpdateCaravanWeatherProfileUseCase updateCaravanWeatherProfileUseCase;

  public CaravanWeatherController(
      GetCaravanWeatherProfileUseCase getCaravanWeatherProfileUseCase,
      UpdateCaravanWeatherProfileUseCase updateCaravanWeatherProfileUseCase) {
    this.getCaravanWeatherProfileUseCase = getCaravanWeatherProfileUseCase;
    this.updateCaravanWeatherProfileUseCase = updateCaravanWeatherProfileUseCase;
  }

  @GetMapping
  public CaravanWeatherProfileResponse get(@PathVariable UUID caravanId) {
    return CaravanWeatherResponseMapper.toResponse(getCaravanWeatherProfileUseCase.getProfile(caravanId));
  }

  @PutMapping
  public CaravanWeatherProfileResponse update(
      @PathVariable UUID caravanId,
      @Valid @RequestBody CaravanWeatherProfileRequest request) {
    return CaravanWeatherResponseMapper.toResponse(
        updateCaravanWeatherProfileUseCase.updateProfile(
            caravanId,
            new UpdateCaravanWeatherProfileUseCase.UpdateCaravanWeatherProfileCommand(
                WeatherClimateBaseline.valueOf(request.climateBaseline()),
                WeatherElevation.valueOf(request.elevation()),
                request.crownRegion() == null ? null : CrownWeatherRegion.valueOf(request.crownRegion()),
                new GolarionDate(request.effectiveFromYear(), request.effectiveFromMonth(), request.effectiveFromDay()))));
  }
}
