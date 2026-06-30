package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.port.in.AddCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.GetCaravanUseCase;
import com.gestioncaravana.application.port.in.GetCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.ListCaravanCargoSummaryUseCase;
import com.gestioncaravana.application.port.in.ListCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.ListCargoCatalogUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanCargoUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanCargoWagonUseCase;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CaravanCargoController {

  private final ListCargoCatalogUseCase listCargoCatalogUseCase;
  private final GetCaravanUseCase getCaravanUseCase;
  private final ListCaravanCargoUseCase listCaravanCargoUseCase;
  private final GetCaravanCargoUseCase getCaravanCargoUseCase;
  private final ListCaravanCargoSummaryUseCase listCaravanCargoSummaryUseCase;
  private final AddCaravanCargoUseCase addCaravanCargoUseCase;
  private final UpdateCaravanCargoUseCase updateCaravanCargoUseCase;
  private final UpdateCaravanCargoWagonUseCase updateCaravanCargoWagonUseCase;
  private final DeleteCaravanCargoUseCase deleteCaravanCargoUseCase;

  public CaravanCargoController(
      ListCargoCatalogUseCase listCargoCatalogUseCase,
      GetCaravanUseCase getCaravanUseCase,
      ListCaravanCargoUseCase listCaravanCargoUseCase,
      GetCaravanCargoUseCase getCaravanCargoUseCase,
      ListCaravanCargoSummaryUseCase listCaravanCargoSummaryUseCase,
      AddCaravanCargoUseCase addCaravanCargoUseCase,
      UpdateCaravanCargoUseCase updateCaravanCargoUseCase,
      UpdateCaravanCargoWagonUseCase updateCaravanCargoWagonUseCase,
      DeleteCaravanCargoUseCase deleteCaravanCargoUseCase) {
    this.listCargoCatalogUseCase = listCargoCatalogUseCase;
    this.getCaravanUseCase = getCaravanUseCase;
    this.listCaravanCargoUseCase = listCaravanCargoUseCase;
    this.getCaravanCargoUseCase = getCaravanCargoUseCase;
    this.listCaravanCargoSummaryUseCase = listCaravanCargoSummaryUseCase;
    this.addCaravanCargoUseCase = addCaravanCargoUseCase;
    this.updateCaravanCargoUseCase = updateCaravanCargoUseCase;
    this.updateCaravanCargoWagonUseCase = updateCaravanCargoWagonUseCase;
    this.deleteCaravanCargoUseCase = deleteCaravanCargoUseCase;
  }

  @GetMapping("/caravans/{caravanId}/cargo/catalog")
  List<CargoCatalogItemResponse> listCatalog(@PathVariable UUID caravanId) {
    getCaravanUseCase.getById(caravanId);
    return listCargoCatalogUseCase.list().stream().map(CaravanCargoResponseMapper::toResponse).toList();
  }

  @GetMapping("/caravans/{caravanId}/cargo/catalog/{cargoCode}")
  CargoCatalogItemResponse getCatalogItem(@PathVariable UUID caravanId, @PathVariable String cargoCode) {
    getCaravanUseCase.getById(caravanId);
    return listCargoCatalogUseCase.list().stream()
        .filter(item -> item.code().equals(cargoCode))
        .findFirst()
        .map(CaravanCargoResponseMapper::toResponse)
        .orElseThrow(() -> new IllegalArgumentException("Cargo not found: " + cargoCode));
  }

  @GetMapping("/caravans/{caravanId}/cargo")
  List<CaravanCargoResponse> listCargo(
      @PathVariable UUID caravanId,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String sourceType,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) UUID wagonId) {
    getCaravanUseCase.getById(caravanId);
    return listCaravanCargoUseCase.list(caravanId, query, sourceType, category, wagonId).stream()
        .map(CaravanCargoResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/caravans/{caravanId}/cargo/{cargoId}")
  CaravanCargoResponse getCargo(@PathVariable UUID caravanId, @PathVariable UUID cargoId) {
    getCaravanUseCase.getById(caravanId);
    return CaravanCargoResponseMapper.toResponse(getCaravanCargoUseCase.getById(caravanId, cargoId));
  }

  @GetMapping("/caravans/{caravanId}/cargo/summary")
  List<CaravanCargoSummaryResponse> listCargoSummary(@PathVariable UUID caravanId) {
    getCaravanUseCase.getById(caravanId);
    return listCaravanCargoSummaryUseCase.list(caravanId).stream().map(CaravanCargoResponseMapper::toResponse).toList();
  }

  @PostMapping("/caravans/{caravanId}/cargo/catalog")
  ResponseEntity<CaravanCargoResponse> addCatalogCargo(
      @PathVariable UUID caravanId, @Valid @RequestBody AddCargoFromCatalogRequest request) {
    getCaravanUseCase.getById(caravanId);
    var created = addCaravanCargoUseCase.execute(
        caravanId,
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CATALOG,
            request.catalogCode(),
            null,
            null,
            request.quantity(),
            request.cargoUnits(),
            request.wagonId(),
            request.origin(),
            request.specificCommodity(),
            request.deity(),
            request.notes()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanCargoResponseMapper.toResponse(created));
  }

  @PostMapping("/caravans/{caravanId}/cargo/custom")
  ResponseEntity<CaravanCargoResponse> addCustomCargo(
      @PathVariable UUID caravanId, @Valid @RequestBody AddCustomCargoRequest request) {
    getCaravanUseCase.getById(caravanId);
    var created = addCaravanCargoUseCase.execute(
        caravanId,
        new AddCaravanCargoUseCase.AddCaravanCargoCommand(
            CaravanCargoSourceType.CUSTOM,
            null,
            request.displayName(),
            request.category(),
            request.quantity(),
            request.cargoUnits(),
            request.wagonId(),
            request.origin(),
            request.specificCommodity(),
            request.deity(),
            request.notes()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanCargoResponseMapper.toResponse(created));
  }

  @PatchMapping("/caravans/{caravanId}/cargo/{cargoId}")
  CaravanCargoResponse updateCargo(
      @PathVariable UUID caravanId,
      @PathVariable UUID cargoId,
      @RequestBody UpdateCaravanCargoRequest request) {
    getCaravanUseCase.getById(caravanId);
    return CaravanCargoResponseMapper.toResponse(
        updateCaravanCargoUseCase.execute(
            caravanId,
            cargoId,
            new UpdateCaravanCargoUseCase.UpdateCaravanCargoCommand(
                request.displayName(),
                request.category(),
                request.quantity(),
                request.cargoUnits(),
                request.wagonId(),
                request.origin(),
                request.specificCommodity(),
                request.deity(),
                request.notes())));
  }

  @PutMapping("/caravans/{caravanId}/cargo/{cargoId}/wagon")
  CaravanCargoResponse updateCargoWagon(
      @PathVariable UUID caravanId,
      @PathVariable UUID cargoId,
      @RequestBody UpdateCaravanCargoWagonRequest request) {
    getCaravanUseCase.getById(caravanId);
    return CaravanCargoResponseMapper.toResponse(
        updateCaravanCargoWagonUseCase.execute(
            caravanId, cargoId, new UpdateCaravanCargoWagonUseCase.UpdateCaravanCargoWagonCommand(request.wagonId())));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/cargo/{cargoId}")
  ResponseEntity<Void> deleteCargo(@PathVariable UUID caravanId, @PathVariable UUID cargoId) {
    getCaravanUseCase.getById(caravanId);
    deleteCaravanCargoUseCase.delete(caravanId, cargoId);
    return ResponseEntity.noContent().build();
  }
}
