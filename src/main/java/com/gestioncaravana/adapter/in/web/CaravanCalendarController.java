package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.port.in.AdvanceCaravanCalendarUseCase;
import com.gestioncaravana.application.port.in.GetCaravanCalendarDayUseCase;
import com.gestioncaravana.application.port.in.GetCaravanCalendarMonthUseCase;
import com.gestioncaravana.application.port.in.SetCaravanCalendarCurrentDateUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caravans/{caravanId}/calendar")
public class CaravanCalendarController {

  private final GetCaravanCalendarMonthUseCase getCaravanCalendarMonthUseCase;
  private final GetCaravanCalendarDayUseCase getCaravanCalendarDayUseCase;
  private final SetCaravanCalendarCurrentDateUseCase setCaravanCalendarCurrentDateUseCase;
  private final AdvanceCaravanCalendarUseCase advanceCaravanCalendarUseCase;

  public CaravanCalendarController(
      GetCaravanCalendarMonthUseCase getCaravanCalendarMonthUseCase,
      GetCaravanCalendarDayUseCase getCaravanCalendarDayUseCase,
      SetCaravanCalendarCurrentDateUseCase setCaravanCalendarCurrentDateUseCase,
      AdvanceCaravanCalendarUseCase advanceCaravanCalendarUseCase) {
    this.getCaravanCalendarMonthUseCase = getCaravanCalendarMonthUseCase;
    this.getCaravanCalendarDayUseCase = getCaravanCalendarDayUseCase;
    this.setCaravanCalendarCurrentDateUseCase = setCaravanCalendarCurrentDateUseCase;
    this.advanceCaravanCalendarUseCase = advanceCaravanCalendarUseCase;
  }

  @GetMapping
  CalendarMonthResponse getMonth(
      @PathVariable UUID caravanId,
      @RequestParam int year,
      @RequestParam int month) {
    return CalendarResponseMapper.toResponse(
        getCaravanCalendarMonthUseCase.getMonth(caravanId, year, month));
  }

  @GetMapping("/day")
  CalendarDayResponse getDay(
      @PathVariable UUID caravanId,
      @RequestParam int year,
      @RequestParam int month,
      @RequestParam int day) {
    return CalendarResponseMapper.toResponse(
        getCaravanCalendarDayUseCase.getDay(caravanId, year, month, day));
  }

  @PutMapping("/current-date")
  CalendarDayResponse setCurrentDate(
      @PathVariable UUID caravanId,
      @Valid @RequestBody SetCaravanCalendarCurrentDateRequest request) {
    return CalendarResponseMapper.toResponse(
        setCaravanCalendarCurrentDateUseCase.setCurrentDate(
            caravanId,
            new SetCaravanCalendarCurrentDateUseCase.SetCaravanCalendarCurrentDateCommand(
                request.year(),
                request.month(),
                request.day())));
  }

  @PostMapping("/advance")
  CalendarDayResponse advance(
      @PathVariable UUID caravanId,
      @Valid @RequestBody AdvanceCaravanCalendarRequest request) {
    return CalendarResponseMapper.toResponse(
        advanceCaravanCalendarUseCase.advance(
            caravanId,
            new AdvanceCaravanCalendarUseCase.AdvanceCaravanCalendarCommand(request.days())));
  }
}
