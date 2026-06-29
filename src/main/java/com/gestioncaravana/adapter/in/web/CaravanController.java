package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.port.in.CreateCaravanUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanUseCase;
import com.gestioncaravana.application.port.in.GetActiveCaravanUseCase;
import com.gestioncaravana.application.port.in.GetCaravanUseCase;
import com.gestioncaravana.application.port.in.ListCaravansUseCase;
import com.gestioncaravana.application.port.in.SelectActiveCaravanUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CaravanController {

  private final CreateCaravanUseCase createCaravanUseCase;
  private final DeleteCaravanUseCase deleteCaravanUseCase;
  private final ListCaravansUseCase listCaravansUseCase;
  private final GetCaravanUseCase getCaravanUseCase;
  private final SelectActiveCaravanUseCase selectActiveCaravanUseCase;
  private final GetActiveCaravanUseCase getActiveCaravanUseCase;

  public CaravanController(
      CreateCaravanUseCase createCaravanUseCase,
      DeleteCaravanUseCase deleteCaravanUseCase,
      ListCaravansUseCase listCaravansUseCase,
      GetCaravanUseCase getCaravanUseCase,
      SelectActiveCaravanUseCase selectActiveCaravanUseCase,
      GetActiveCaravanUseCase getActiveCaravanUseCase) {
    this.createCaravanUseCase = createCaravanUseCase;
    this.deleteCaravanUseCase = deleteCaravanUseCase;
    this.listCaravansUseCase = listCaravansUseCase;
    this.getCaravanUseCase = getCaravanUseCase;
    this.selectActiveCaravanUseCase = selectActiveCaravanUseCase;
    this.getActiveCaravanUseCase = getActiveCaravanUseCase;
  }

  @PostMapping("/caravans")
  ResponseEntity<CaravanResponse> create(@Valid @RequestBody CaravanRequest request) {
    var created = createCaravanUseCase.execute(
        new CreateCaravanUseCase.CreateCaravanCommand(request.name(), request.description()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanResponseMapper.toResponse(created));
  }

  @GetMapping("/caravans")
  List<CaravanResponse> list() {
    return listCaravansUseCase.list().stream().map(CaravanResponseMapper::toResponse).toList();
  }

  @GetMapping("/caravans/{id}")
  CaravanResponse getById(@PathVariable UUID id) {
    return CaravanResponseMapper.toResponse(getCaravanUseCase.getById(id));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{id}")
  ResponseEntity<Void> delete(@PathVariable UUID id) {
    deleteCaravanUseCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/session/active-caravan")
  ActiveCaravanResponse selectActive(@Valid @RequestBody SelectActiveCaravanRequest request) {
    return new ActiveCaravanResponse(
        CaravanResponseMapper.toResponse(selectActiveCaravanUseCase.select(request.caravanId())));
  }

  @GetMapping("/session/active-caravan")
  ActiveCaravanResponse getActive() {
    return new ActiveCaravanResponse(
        getActiveCaravanUseCase.getActive().map(CaravanResponseMapper::toResponse).orElse(null));
  }
}
