package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.port.in.CreateCaravanUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.GetActiveCaravanUseCase;
import com.gestioncaravana.application.port.in.GetCaravanUseCase;
import com.gestioncaravana.application.port.in.ListCaravansUseCase;
import com.gestioncaravana.application.port.in.ListCaravanWagonsUseCase;
import com.gestioncaravana.application.port.in.ListWagonCatalogUseCase;
import com.gestioncaravana.application.port.in.AddCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase;
import com.gestioncaravana.application.port.in.GetCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanWagonImprovementUseCase;
import com.gestioncaravana.application.port.in.SelectActiveCaravanUseCase;
import com.gestioncaravana.application.port.in.ListCaravanWagonImprovementsUseCase;
import com.gestioncaravana.application.port.in.ListWagonImprovementCatalogUseCase;
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
  private final ListWagonCatalogUseCase listWagonCatalogUseCase;
  private final ListWagonImprovementCatalogUseCase listWagonImprovementCatalogUseCase;
  private final ListCaravanWagonImprovementsUseCase listCaravanWagonImprovementsUseCase;
  private final ListCaravanWagonsUseCase listCaravanWagonsUseCase;
  private final GetCaravanWagonUseCase getCaravanWagonUseCase;
  private final AddCaravanWagonUseCase addCaravanWagonUseCase;
  private final AddCaravanWagonImprovementUseCase addCaravanWagonImprovementUseCase;
  private final DeleteCaravanWagonImprovementUseCase deleteCaravanWagonImprovementUseCase;
  private final DeleteCaravanWagonUseCase deleteCaravanWagonUseCase;

  public CaravanController(
      CreateCaravanUseCase createCaravanUseCase,
      DeleteCaravanUseCase deleteCaravanUseCase,
      ListCaravansUseCase listCaravansUseCase,
      GetCaravanUseCase getCaravanUseCase,
      SelectActiveCaravanUseCase selectActiveCaravanUseCase,
      GetActiveCaravanUseCase getActiveCaravanUseCase,
      ListWagonCatalogUseCase listWagonCatalogUseCase,
      ListWagonImprovementCatalogUseCase listWagonImprovementCatalogUseCase,
      ListCaravanWagonImprovementsUseCase listCaravanWagonImprovementsUseCase,
      ListCaravanWagonsUseCase listCaravanWagonsUseCase,
      GetCaravanWagonUseCase getCaravanWagonUseCase,
      AddCaravanWagonUseCase addCaravanWagonUseCase,
      AddCaravanWagonImprovementUseCase addCaravanWagonImprovementUseCase,
      DeleteCaravanWagonImprovementUseCase deleteCaravanWagonImprovementUseCase,
      DeleteCaravanWagonUseCase deleteCaravanWagonUseCase) {
    this.createCaravanUseCase = createCaravanUseCase;
    this.deleteCaravanUseCase = deleteCaravanUseCase;
    this.listCaravansUseCase = listCaravansUseCase;
    this.getCaravanUseCase = getCaravanUseCase;
    this.selectActiveCaravanUseCase = selectActiveCaravanUseCase;
    this.getActiveCaravanUseCase = getActiveCaravanUseCase;
    this.listWagonCatalogUseCase = listWagonCatalogUseCase;
    this.listWagonImprovementCatalogUseCase = listWagonImprovementCatalogUseCase;
    this.listCaravanWagonImprovementsUseCase = listCaravanWagonImprovementsUseCase;
    this.listCaravanWagonsUseCase = listCaravanWagonsUseCase;
    this.getCaravanWagonUseCase = getCaravanWagonUseCase;
    this.addCaravanWagonUseCase = addCaravanWagonUseCase;
    this.addCaravanWagonImprovementUseCase = addCaravanWagonImprovementUseCase;
    this.deleteCaravanWagonImprovementUseCase = deleteCaravanWagonImprovementUseCase;
    this.deleteCaravanWagonUseCase = deleteCaravanWagonUseCase;
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

  @GetMapping("/caravans/{caravanId}/wagons/catalog")
  List<WagonCatalogItemResponse> listWagonCatalog(@PathVariable UUID caravanId) {
    getCaravanUseCase.getById(caravanId);
    return listWagonCatalogUseCase.list().stream().map(CaravanWagonResponseMapper::toResponse).toList();
  }

  @GetMapping("/caravans/{caravanId}/wagons/{wagonId}/improvements/catalog")
  List<WagonImprovementCatalogItemResponse> listWagonImprovementCatalog(
      @PathVariable UUID caravanId, @PathVariable UUID wagonId) {
    getCaravanWagonUseCase.getById(caravanId, wagonId);
    return listWagonImprovementCatalogUseCase.listCatalog(caravanId, wagonId).stream()
        .map(CaravanWagonResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/caravans/{caravanId}/wagons/{wagonId}/improvements")
  List<CaravanWagonImprovementResponse> listCaravanWagonImprovements(
      @PathVariable UUID caravanId, @PathVariable UUID wagonId) {
    getCaravanWagonUseCase.getById(caravanId, wagonId);
    return listCaravanWagonImprovementsUseCase.listImprovements(caravanId, wagonId).stream()
        .map(CaravanWagonResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/caravans/{caravanId}/wagons")
  List<CaravanWagonResponse> listCaravanWagons(@PathVariable UUID caravanId) {
    return listCaravanWagonsUseCase.list(caravanId).stream().map(CaravanWagonResponseMapper::toResponse).toList();
  }

  @GetMapping("/caravans/{caravanId}/wagons/{wagonId}")
  CaravanWagonResponse getCaravanWagon(@PathVariable UUID caravanId, @PathVariable UUID wagonId) {
    return CaravanWagonResponseMapper.toResponse(getCaravanWagonUseCase.getById(caravanId, wagonId));
  }

  @PostMapping("/caravans/{caravanId}/wagons")
  ResponseEntity<CaravanWagonResponse> addCaravanWagon(
      @PathVariable UUID caravanId, @Valid @RequestBody AddCaravanWagonRequest request) {
    var created = addCaravanWagonUseCase.execute(
        caravanId, new AddCaravanWagonUseCase.AddCaravanWagonCommand(request.wagonTypeCode()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanWagonResponseMapper.toResponse(created));
  }

  @PostMapping("/caravans/{caravanId}/wagons/{wagonId}/improvements")
  ResponseEntity<CaravanWagonResponse> addCaravanWagonImprovement(
      @PathVariable UUID caravanId,
      @PathVariable UUID wagonId,
      @Valid @RequestBody AddCaravanWagonImprovementRequest request) {
    var updated = addCaravanWagonImprovementUseCase.execute(
        caravanId, wagonId, new AddCaravanWagonImprovementUseCase.AddCaravanWagonImprovementCommand(request.improvementTypeCode()));
    return ResponseEntity.ok(CaravanWagonResponseMapper.toResponse(updated));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/wagons/{wagonId}/improvements/{improvementId}")
  ResponseEntity<CaravanWagonResponse> deleteCaravanWagonImprovement(
      @PathVariable UUID caravanId, @PathVariable UUID wagonId, @PathVariable UUID improvementId) {
    var updated = deleteCaravanWagonImprovementUseCase.execute(caravanId, wagonId, improvementId);
    return ResponseEntity.ok(CaravanWagonResponseMapper.toResponse(updated));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/wagons/{wagonId}")
  ResponseEntity<Void> deleteCaravanWagon(@PathVariable UUID caravanId, @PathVariable UUID wagonId) {
    deleteCaravanWagonUseCase.delete(caravanId, wagonId);
    return ResponseEntity.noContent().build();
  }
}
