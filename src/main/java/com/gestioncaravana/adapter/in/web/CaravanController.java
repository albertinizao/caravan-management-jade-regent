package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.application.port.in.CreateCaravanUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanBeastUseCase;
import com.gestioncaravana.application.port.in.DeleteUnassignedCaravanBeastsUseCase;
import com.gestioncaravana.application.port.in.ExportCaravanBackupUseCase;
import com.gestioncaravana.application.port.in.ImportCaravanBackupUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.AddCaravanBeastUseCase;
import com.gestioncaravana.application.port.in.ClearCaravanBeastAssignmentUseCase;
import com.gestioncaravana.application.port.in.GetActiveCaravanUseCase;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.in.GetCaravanBeastUseCase;
import com.gestioncaravana.application.port.in.GetCaravanUseCase;
import com.gestioncaravana.application.port.in.ListCaravansUseCase;
import com.gestioncaravana.application.port.in.ListBeastCatalogUseCase;
import com.gestioncaravana.application.port.in.ListCaravanBeastsUseCase;
import com.gestioncaravana.application.port.in.ListCaravanWagonsUseCase;
import com.gestioncaravana.application.port.in.ListWagonCatalogUseCase;
import com.gestioncaravana.application.port.in.AddCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.AddCaravanWagonImprovementUseCase;
import com.gestioncaravana.application.port.in.AddCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.DamageCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.GetCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.GetCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanWagonImprovementUseCase;
import com.gestioncaravana.application.port.in.SelectActiveCaravanUseCase;
import com.gestioncaravana.application.port.in.ListCaravanTravelersUseCase;
import com.gestioncaravana.application.port.in.ListCaravanFeatCatalogUseCase;
import com.gestioncaravana.application.port.in.ListCaravanFeatsUseCase;
import com.gestioncaravana.application.port.in.ListCaravanWagonImprovementsUseCase;
import com.gestioncaravana.application.port.in.ListWagonImprovementCatalogUseCase;
import com.gestioncaravana.application.port.in.ListTravelerRoleCatalogUseCase;
import com.gestioncaravana.application.port.in.GetCaravanFeatUseCase;
import com.gestioncaravana.application.port.in.AddCaravanFeatUseCase;
import com.gestioncaravana.application.port.in.DeleteCaravanFeatUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanFeatUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerRoleUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanTravelerWagonUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanBeastAssignmentUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanBeastUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanDiscontentUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanLevelUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanMainStatsUseCase;
import com.gestioncaravana.application.port.in.RepairCaravanWagonUseCase;
import com.gestioncaravana.application.port.in.PreviewCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.in.ConfirmCaravanDayCycleUseCase;
import com.gestioncaravana.application.port.in.PreviewCaravanMultiDayCycleUseCase;
import com.gestioncaravana.application.port.in.ConfirmCaravanMultiDayCycleUseCase;
import com.gestioncaravana.application.model.CaravanBackupView;
import com.gestioncaravana.application.model.CaravanDayCyclePreviewView;
import com.gestioncaravana.application.model.CaravanMultiDayCyclePreviewView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CaravanController {

  private final CreateCaravanUseCase createCaravanUseCase;
  private final DeleteCaravanUseCase deleteCaravanUseCase;
  private final ExportCaravanBackupUseCase exportCaravanBackupUseCase;
  private final ImportCaravanBackupUseCase importCaravanBackupUseCase;
  private final ListCaravansUseCase listCaravansUseCase;
  private final GetCaravanUseCase getCaravanUseCase;
  private final GetCaravanStatisticsUseCase getCaravanStatisticsUseCase;
  private final PreviewCaravanDayCycleUseCase previewCaravanDayCycleUseCase;
  private final ConfirmCaravanDayCycleUseCase confirmCaravanDayCycleUseCase;
  private final PreviewCaravanMultiDayCycleUseCase previewCaravanMultiDayCycleUseCase;
  private final ConfirmCaravanMultiDayCycleUseCase confirmCaravanMultiDayCycleUseCase;
  private final SelectActiveCaravanUseCase selectActiveCaravanUseCase;
  private final GetActiveCaravanUseCase getActiveCaravanUseCase;
  private final ListWagonCatalogUseCase listWagonCatalogUseCase;
  private final ListBeastCatalogUseCase listBeastCatalogUseCase;
  private final ListWagonImprovementCatalogUseCase listWagonImprovementCatalogUseCase;
  private final ListTravelerRoleCatalogUseCase listTravelerRoleCatalogUseCase;
  private final ListCaravanBeastsUseCase listCaravanBeastsUseCase;
  private final GetCaravanBeastUseCase getCaravanBeastUseCase;
  private final ListCaravanTravelersUseCase listCaravanTravelersUseCase;
  private final ListCaravanFeatCatalogUseCase listCaravanFeatCatalogUseCase;
  private final ListCaravanFeatsUseCase listCaravanFeatsUseCase;
  private final GetCaravanFeatUseCase getCaravanFeatUseCase;
  private final AddCaravanFeatUseCase addCaravanFeatUseCase;
  private final DeleteCaravanFeatUseCase deleteCaravanFeatUseCase;
  private final UpdateCaravanFeatUseCase updateCaravanFeatUseCase;
  private final GetCaravanTravelerUseCase getCaravanTravelerUseCase;
  private final AddCaravanBeastUseCase addCaravanBeastUseCase;
  private final DeleteCaravanBeastUseCase deleteCaravanBeastUseCase;
  private final DeleteUnassignedCaravanBeastsUseCase deleteUnassignedCaravanBeastsUseCase;
  private final UpdateCaravanBeastAssignmentUseCase updateCaravanBeastAssignmentUseCase;
  private final UpdateCaravanBeastUseCase updateCaravanBeastUseCase;
  private final ClearCaravanBeastAssignmentUseCase clearCaravanBeastAssignmentUseCase;
  private final AddCaravanTravelerUseCase addCaravanTravelerUseCase;
  private final DeleteCaravanTravelerUseCase deleteCaravanTravelerUseCase;
  private final UpdateCaravanTravelerUseCase updateCaravanTravelerUseCase;
  private final UpdateCaravanTravelerWagonUseCase updateCaravanTravelerWagonUseCase;
  private final UpdateCaravanTravelerRoleUseCase updateCaravanTravelerRoleUseCase;
  private final ListCaravanWagonImprovementsUseCase listCaravanWagonImprovementsUseCase;
  private final ListCaravanWagonsUseCase listCaravanWagonsUseCase;
  private final GetCaravanWagonUseCase getCaravanWagonUseCase;
  private final AddCaravanWagonUseCase addCaravanWagonUseCase;
  private final UpdateCaravanWagonUseCase updateCaravanWagonUseCase;
  private final DamageCaravanWagonUseCase damageCaravanWagonUseCase;
  private final RepairCaravanWagonUseCase repairCaravanWagonUseCase;
  private final AddCaravanWagonImprovementUseCase addCaravanWagonImprovementUseCase;
  private final DeleteCaravanWagonImprovementUseCase deleteCaravanWagonImprovementUseCase;
  private final DeleteCaravanWagonUseCase deleteCaravanWagonUseCase;
  private final UpdateCaravanLevelUseCase updateCaravanLevelUseCase;
  private final UpdateCaravanDiscontentUseCase updateCaravanDiscontentUseCase;
  private final UpdateCaravanMainStatsUseCase updateCaravanMainStatsUseCase;

  public CaravanController(
      CreateCaravanUseCase createCaravanUseCase,
      DeleteCaravanUseCase deleteCaravanUseCase,
      ExportCaravanBackupUseCase exportCaravanBackupUseCase,
      ImportCaravanBackupUseCase importCaravanBackupUseCase,
      ListCaravansUseCase listCaravansUseCase,
      GetCaravanUseCase getCaravanUseCase,
      GetCaravanStatisticsUseCase getCaravanStatisticsUseCase,
      PreviewCaravanDayCycleUseCase previewCaravanDayCycleUseCase,
      ConfirmCaravanDayCycleUseCase confirmCaravanDayCycleUseCase,
      PreviewCaravanMultiDayCycleUseCase previewCaravanMultiDayCycleUseCase,
      ConfirmCaravanMultiDayCycleUseCase confirmCaravanMultiDayCycleUseCase,
      SelectActiveCaravanUseCase selectActiveCaravanUseCase,
      GetActiveCaravanUseCase getActiveCaravanUseCase,
      ListWagonCatalogUseCase listWagonCatalogUseCase,
      ListBeastCatalogUseCase listBeastCatalogUseCase,
      ListWagonImprovementCatalogUseCase listWagonImprovementCatalogUseCase,
      ListTravelerRoleCatalogUseCase listTravelerRoleCatalogUseCase,
      ListCaravanBeastsUseCase listCaravanBeastsUseCase,
      GetCaravanBeastUseCase getCaravanBeastUseCase,
      ListCaravanTravelersUseCase listCaravanTravelersUseCase,
      ListCaravanFeatCatalogUseCase listCaravanFeatCatalogUseCase,
      ListCaravanFeatsUseCase listCaravanFeatsUseCase,
      GetCaravanFeatUseCase getCaravanFeatUseCase,
      AddCaravanFeatUseCase addCaravanFeatUseCase,
      DeleteCaravanFeatUseCase deleteCaravanFeatUseCase,
      UpdateCaravanFeatUseCase updateCaravanFeatUseCase,
      GetCaravanTravelerUseCase getCaravanTravelerUseCase,
      AddCaravanBeastUseCase addCaravanBeastUseCase,
      DeleteCaravanBeastUseCase deleteCaravanBeastUseCase,
      DeleteUnassignedCaravanBeastsUseCase deleteUnassignedCaravanBeastsUseCase,
      UpdateCaravanBeastAssignmentUseCase updateCaravanBeastAssignmentUseCase,
      UpdateCaravanBeastUseCase updateCaravanBeastUseCase,
      ClearCaravanBeastAssignmentUseCase clearCaravanBeastAssignmentUseCase,
      AddCaravanTravelerUseCase addCaravanTravelerUseCase,
      DeleteCaravanTravelerUseCase deleteCaravanTravelerUseCase,
      UpdateCaravanTravelerUseCase updateCaravanTravelerUseCase,
      UpdateCaravanTravelerWagonUseCase updateCaravanTravelerWagonUseCase,
      UpdateCaravanTravelerRoleUseCase updateCaravanTravelerRoleUseCase,
      ListCaravanWagonImprovementsUseCase listCaravanWagonImprovementsUseCase,
      ListCaravanWagonsUseCase listCaravanWagonsUseCase,
      GetCaravanWagonUseCase getCaravanWagonUseCase,
      AddCaravanWagonUseCase addCaravanWagonUseCase,
      UpdateCaravanWagonUseCase updateCaravanWagonUseCase,
      DamageCaravanWagonUseCase damageCaravanWagonUseCase,
      RepairCaravanWagonUseCase repairCaravanWagonUseCase,
      AddCaravanWagonImprovementUseCase addCaravanWagonImprovementUseCase,
      DeleteCaravanWagonImprovementUseCase deleteCaravanWagonImprovementUseCase,
      DeleteCaravanWagonUseCase deleteCaravanWagonUseCase,
      UpdateCaravanLevelUseCase updateCaravanLevelUseCase,
      UpdateCaravanDiscontentUseCase updateCaravanDiscontentUseCase,
      UpdateCaravanMainStatsUseCase updateCaravanMainStatsUseCase) {
    this.createCaravanUseCase = createCaravanUseCase;
    this.deleteCaravanUseCase = deleteCaravanUseCase;
    this.exportCaravanBackupUseCase = exportCaravanBackupUseCase;
    this.importCaravanBackupUseCase = importCaravanBackupUseCase;
    this.listCaravansUseCase = listCaravansUseCase;
    this.getCaravanUseCase = getCaravanUseCase;
    this.getCaravanStatisticsUseCase = getCaravanStatisticsUseCase;
    this.previewCaravanDayCycleUseCase = previewCaravanDayCycleUseCase;
    this.confirmCaravanDayCycleUseCase = confirmCaravanDayCycleUseCase;
    this.previewCaravanMultiDayCycleUseCase = previewCaravanMultiDayCycleUseCase;
    this.confirmCaravanMultiDayCycleUseCase = confirmCaravanMultiDayCycleUseCase;
    this.selectActiveCaravanUseCase = selectActiveCaravanUseCase;
    this.getActiveCaravanUseCase = getActiveCaravanUseCase;
    this.listWagonCatalogUseCase = listWagonCatalogUseCase;
    this.listBeastCatalogUseCase = listBeastCatalogUseCase;
    this.listWagonImprovementCatalogUseCase = listWagonImprovementCatalogUseCase;
    this.listTravelerRoleCatalogUseCase = listTravelerRoleCatalogUseCase;
    this.listCaravanBeastsUseCase = listCaravanBeastsUseCase;
    this.getCaravanBeastUseCase = getCaravanBeastUseCase;
    this.listCaravanTravelersUseCase = listCaravanTravelersUseCase;
    this.listCaravanFeatCatalogUseCase = listCaravanFeatCatalogUseCase;
    this.listCaravanFeatsUseCase = listCaravanFeatsUseCase;
    this.getCaravanFeatUseCase = getCaravanFeatUseCase;
    this.addCaravanFeatUseCase = addCaravanFeatUseCase;
    this.deleteCaravanFeatUseCase = deleteCaravanFeatUseCase;
    this.updateCaravanFeatUseCase = updateCaravanFeatUseCase;
    this.getCaravanTravelerUseCase = getCaravanTravelerUseCase;
    this.addCaravanBeastUseCase = addCaravanBeastUseCase;
    this.deleteCaravanBeastUseCase = deleteCaravanBeastUseCase;
    this.deleteUnassignedCaravanBeastsUseCase = deleteUnassignedCaravanBeastsUseCase;
    this.updateCaravanBeastAssignmentUseCase = updateCaravanBeastAssignmentUseCase;
    this.updateCaravanBeastUseCase = updateCaravanBeastUseCase;
    this.clearCaravanBeastAssignmentUseCase = clearCaravanBeastAssignmentUseCase;
    this.addCaravanTravelerUseCase = addCaravanTravelerUseCase;
    this.deleteCaravanTravelerUseCase = deleteCaravanTravelerUseCase;
    this.updateCaravanTravelerUseCase = updateCaravanTravelerUseCase;
    this.updateCaravanTravelerWagonUseCase = updateCaravanTravelerWagonUseCase;
    this.updateCaravanTravelerRoleUseCase = updateCaravanTravelerRoleUseCase;
    this.listCaravanWagonImprovementsUseCase = listCaravanWagonImprovementsUseCase;
    this.listCaravanWagonsUseCase = listCaravanWagonsUseCase;
    this.getCaravanWagonUseCase = getCaravanWagonUseCase;
    this.addCaravanWagonUseCase = addCaravanWagonUseCase;
    this.updateCaravanWagonUseCase = updateCaravanWagonUseCase;
    this.damageCaravanWagonUseCase = damageCaravanWagonUseCase;
    this.repairCaravanWagonUseCase = repairCaravanWagonUseCase;
    this.addCaravanWagonImprovementUseCase = addCaravanWagonImprovementUseCase;
    this.deleteCaravanWagonImprovementUseCase = deleteCaravanWagonImprovementUseCase;
    this.deleteCaravanWagonUseCase = deleteCaravanWagonUseCase;
    this.updateCaravanLevelUseCase = updateCaravanLevelUseCase;
    this.updateCaravanDiscontentUseCase = updateCaravanDiscontentUseCase;
    this.updateCaravanMainStatsUseCase = updateCaravanMainStatsUseCase;
  }

  @PostMapping("/caravans")
  ResponseEntity<CaravanResponse> create(@Valid @RequestBody CaravanRequest request) {
    var created = createCaravanUseCase.execute(
        new CreateCaravanUseCase.CreateCaravanCommand(
            request.name(),
            request.description(),
            request.offense(),
            request.defense(),
            request.mobility(),
            request.morale()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanResponseMapper.toResponse(created));
  }

  @GetMapping("/caravans/{caravanId}/feats/catalog")
  List<CaravanFeatCatalogItemResponse> listCaravanFeatCatalog(@PathVariable UUID caravanId) {
    getCaravanUseCase.getById(caravanId);
    return listCaravanFeatCatalogUseCase.listCatalog(caravanId).stream()
        .map(CaravanFeatResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/caravans/{caravanId}/feats")
  List<CaravanFeatResponse> listCaravanFeats(@PathVariable UUID caravanId) {
    return listCaravanFeatsUseCase.list(caravanId).stream()
        .map(CaravanFeatResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/caravans/{caravanId}/feats/{featId}")
  CaravanFeatResponse getCaravanFeat(@PathVariable UUID caravanId, @PathVariable UUID featId) {
    return CaravanFeatResponseMapper.toResponse(getCaravanFeatUseCase.getById(caravanId, featId));
  }

  @PostMapping("/caravans/{caravanId}/feats")
  ResponseEntity<CaravanFeatResponse> addCaravanFeat(
      @PathVariable UUID caravanId,
      @Valid @RequestBody AddCaravanFeatRequest request) {
    var created = addCaravanFeatUseCase.execute(
        caravanId,
        new AddCaravanFeatUseCase.AddCaravanFeatCommand(
            request.featTypeCode(),
            request.acquisitionSourceType(),
            request.acquisitionLevel(),
            request.acquisitionCause(),
            request.active(),
            request.manualApplies(),
            request.manualAppliesReason()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanFeatResponseMapper.toResponse(created));
  }

  @PutMapping("/caravans/{caravanId}/feats/{featId}")
  CaravanFeatResponse updateCaravanFeat(
      @PathVariable UUID caravanId,
      @PathVariable UUID featId,
      @Valid @RequestBody UpdateCaravanFeatRequest request) {
    return CaravanFeatResponseMapper.toResponse(
        updateCaravanFeatUseCase.execute(
            caravanId,
            featId,
            new UpdateCaravanFeatUseCase.UpdateCaravanFeatCommand(
                request.acquisitionSourceType(),
                request.acquisitionLevel(),
                request.acquisitionCause(),
                request.active(),
                request.manualApplies(),
                request.manualAppliesReason())));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/feats/{featId}")
  ResponseEntity<Void> deleteCaravanFeat(@PathVariable UUID caravanId, @PathVariable UUID featId) {
    deleteCaravanFeatUseCase.delete(caravanId, featId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/caravans")
  List<CaravanResponse> list() {
    return listCaravansUseCase.list().stream().map(CaravanResponseMapper::toResponse).toList();
  }

  @GetMapping("/caravans/{id}")
  CaravanResponse getById(@PathVariable UUID id) {
    return CaravanResponseMapper.toResponse(getCaravanUseCase.getById(id));
  }

  @GetMapping("/caravans/{caravanId}/backup")
  CaravanBackupView exportBackup(@PathVariable UUID caravanId) {
    getCaravanUseCase.getById(caravanId);
    return exportCaravanBackupUseCase.export(caravanId);
  }

  @PostMapping("/caravans/backup")
  CaravanResponse importBackup(@Valid @RequestBody CaravanBackupView backup) {
    return CaravanResponseMapper.toResponse(importCaravanBackupUseCase.execute(backup));
  }

  @PostMapping("/caravans/{caravanId}/day-cycle/preview")
  CaravanDayCyclePreviewView previewCaravanDayCycle(@PathVariable UUID caravanId) {
    return previewCaravanDayCycleUseCase.preview(caravanId);
  }

  @PostMapping("/caravans/{caravanId}/day-cycle/confirm")
  CaravanDayCyclePreviewView confirmCaravanDayCycle(
      @PathVariable UUID caravanId,
      @Valid @RequestBody ConfirmCaravanDayCycleUseCase.ConfirmCaravanDayCycleCommand request) {
    return confirmCaravanDayCycleUseCase.confirm(caravanId, request);
  }

  @PostMapping("/caravans/{caravanId}/day-cycle/preview-multi")
  CaravanMultiDayCyclePreviewView previewCaravanMultiDayCycle(
      @PathVariable UUID caravanId,
      @Valid @RequestBody PreviewCaravanMultiDayCycleUseCase.PreviewCaravanMultiDayCycleCommand request) {
    return previewCaravanMultiDayCycleUseCase.preview(caravanId, request);
  }

  @PostMapping("/caravans/{caravanId}/day-cycle/confirm-multi")
  CaravanMultiDayCyclePreviewView confirmCaravanMultiDayCycle(
      @PathVariable UUID caravanId,
      @Valid @RequestBody ConfirmCaravanMultiDayCycleUseCase.ConfirmCaravanMultiDayCycleCommand request) {
    return confirmCaravanMultiDayCycleUseCase.confirm(caravanId, request);
  }

  @PatchMapping("/caravans/{caravanId}/main-stats")
  CaravanResponse updateCaravanMainStats(
      @PathVariable UUID caravanId,
      @Valid @RequestBody UpdateCaravanMainStatsRequest request) {
    return CaravanResponseMapper.toResponse(
        updateCaravanMainStatsUseCase.execute(
            caravanId,
            new UpdateCaravanMainStatsUseCase.UpdateCaravanMainStatsCommand(
                request.offense(),
                request.defense(),
                request.mobility(),
                request.morale())));
  }

  @PatchMapping("/caravans/{caravanId}/level")
  CaravanResponse updateCaravanLevel(
      @PathVariable UUID caravanId,
      @Valid @RequestBody UpdateCaravanDeltaRequest request) {
    return CaravanResponseMapper.toResponse(
        updateCaravanLevelUseCase.execute(
            caravanId,
            new UpdateCaravanLevelUseCase.UpdateCaravanLevelCommand(requireDelta(request.delta()))));
  }

  @PatchMapping("/caravans/{caravanId}/discontent")
  CaravanResponse updateCaravanDiscontent(
      @PathVariable UUID caravanId,
      @Valid @RequestBody UpdateCaravanDeltaRequest request) {
    return CaravanResponseMapper.toResponse(
        updateCaravanDiscontentUseCase.execute(
            caravanId,
            new UpdateCaravanDiscontentUseCase.UpdateCaravanDiscontentCommand(requireDelta(request.delta()))));
  }

  @GetMapping("/caravans/{id}/statistics")
  CaravanStatisticsResponse getStatistics(@PathVariable UUID id) {
    return CaravanStatisticsResponseMapper.toResponse(getCaravanStatisticsUseCase.getById(id));
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

  @GetMapping("/caravans/{caravanId}/beasts/catalog")
  List<CaravanBeastCatalogItemResponse> listBeastCatalog(@PathVariable UUID caravanId) {
    getCaravanUseCase.getById(caravanId);
    return listBeastCatalogUseCase.list().stream().map(CaravanBeastResponseMapper::toResponse).toList();
  }

  @GetMapping("/caravans/{caravanId}/beasts")
  List<CaravanBeastResponse> listCaravanBeasts(
      @PathVariable UUID caravanId,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String query,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String sourceType,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String assignmentType,
      @org.springframework.web.bind.annotation.RequestParam(required = false) UUID wagonId) {
    return listCaravanBeastsUseCase.list(caravanId, query, sourceType, assignmentType, wagonId).stream()
        .map(CaravanBeastResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/caravans/{caravanId}/beasts/{beastId}")
  CaravanBeastResponse getCaravanBeast(@PathVariable UUID caravanId, @PathVariable UUID beastId) {
    return CaravanBeastResponseMapper.toResponse(getCaravanBeastUseCase.getById(caravanId, beastId));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/beasts/{beastId}")
  ResponseEntity<Void> deleteCaravanBeast(@PathVariable UUID caravanId, @PathVariable UUID beastId) {
    deleteCaravanBeastUseCase.delete(caravanId, beastId);
    return ResponseEntity.noContent().build();
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/beasts/unassigned")
  ResponseEntity<Void> deleteUnassignedCaravanBeasts(@PathVariable UUID caravanId) {
    deleteUnassignedCaravanBeastsUseCase.delete(caravanId);
    return ResponseEntity.noContent().build();
  }

  private static int requireDelta(Integer delta) {
    if (delta == null) {
      throw new IllegalArgumentException("delta is required");
    }
    if (delta == 0) {
      throw new IllegalArgumentException("delta must not be 0");
    }
    return delta;
  }

  @PostMapping("/caravans/{caravanId}/beasts/catalog/{beastCode}")
  ResponseEntity<CaravanBeastResponse> addCaravanBeastFromCatalog(
      @PathVariable UUID caravanId,
      @PathVariable String beastCode,
      @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "1") Integer quantity) {
    var created = addCaravanBeastUseCase.execute(
        caravanId,
        new AddCaravanBeastUseCase.AddCaravanBeastCommand(
            com.gestioncaravana.domain.CaravanBeastSourceType.CATALOG,
            beastCode,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            quantity));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanBeastResponseMapper.toResponse(created));
  }

  @PostMapping("/caravans/{caravanId}/beasts")
  ResponseEntity<CaravanBeastResponse> addCaravanBeast(
      @PathVariable UUID caravanId,
      @Valid @RequestBody AddCaravanBeastRequest request) {
    var created = addCaravanBeastUseCase.execute(
        caravanId,
        new AddCaravanBeastUseCase.AddCaravanBeastCommand(
            request.sourceType(),
            request.catalogBeastCode(),
            request.name(),
            request.size(),
            request.strength(),
            request.speed(),
            request.thermalAdaptation(),
            request.basePrice(),
            request.trainedPrice(),
            request.fourLegged(),
            request.specialNote(),
            request.description(),
            request.customNotes(),
            request.consumption(),
            request.occupiedSpace(),
            request.quantity()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanBeastResponseMapper.toResponse(created));
  }

  @PutMapping("/caravans/{caravanId}/beasts/{beastId}/assignment")
  CaravanBeastResponse updateCaravanBeastAssignment(
      @PathVariable UUID caravanId,
      @PathVariable UUID beastId,
      @Valid @RequestBody UpdateCaravanBeastAssignmentRequest request) {
    return CaravanBeastResponseMapper.toResponse(
        updateCaravanBeastAssignmentUseCase.execute(
            caravanId,
            beastId,
            new UpdateCaravanBeastAssignmentUseCase.UpdateCaravanBeastAssignmentCommand(
                request.assignmentType(),
                request.wagonId(),
                request.availableRoleCodes(),
                request.activeRoleCode())));
  }

  @PutMapping("/caravans/{caravanId}/beasts/{beastId}")
  CaravanBeastResponse updateCaravanBeast(
      @PathVariable UUID caravanId,
      @PathVariable UUID beastId,
      @Valid @RequestBody UpdateCaravanBeastRequest request) {
    return CaravanBeastResponseMapper.toResponse(
        updateCaravanBeastUseCase.execute(
            caravanId,
            beastId,
            new UpdateCaravanBeastUseCase.UpdateCaravanBeastCommand(
                request.consumption(),
                request.occupiedSpace())));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/beasts/{beastId}/assignment")
  CaravanBeastResponse clearCaravanBeastAssignment(@PathVariable UUID caravanId, @PathVariable UUID beastId) {
    return CaravanBeastResponseMapper.toResponse(clearCaravanBeastAssignmentUseCase.execute(caravanId, beastId));
  }

  @GetMapping("/caravans/{caravanId}/travelers/roles/catalog")
  List<TravelerRoleCatalogItemResponse> listTravelerRoleCatalog(@PathVariable UUID caravanId) {
    getCaravanUseCase.getById(caravanId);
    return listTravelerRoleCatalogUseCase.list().stream().map(CaravanTravelerResponseMapper::toResponse).toList();
  }

  @GetMapping("/caravans/{caravanId}/travelers")
  List<CaravanTravelerResponse> listCaravanTravelers(
      @PathVariable UUID caravanId,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String query,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String roleCode,
      @org.springframework.web.bind.annotation.RequestParam(required = false) UUID wagonId) {
    return listCaravanTravelersUseCase.list(caravanId, query, roleCode, wagonId).stream()
        .map(CaravanTravelerResponseMapper::toResponse)
        .toList();
  }

  @GetMapping("/caravans/{caravanId}/travelers/{travelerId}")
  CaravanTravelerResponse getCaravanTraveler(@PathVariable UUID caravanId, @PathVariable UUID travelerId) {
    return CaravanTravelerResponseMapper.toResponse(getCaravanTravelerUseCase.getById(caravanId, travelerId));
  }

  @PostMapping("/caravans/{caravanId}/travelers")
  ResponseEntity<CaravanTravelerResponse> addCaravanTraveler(
      @PathVariable UUID caravanId, @Valid @RequestBody AddCaravanTravelerRequest request) {
    var created = addCaravanTravelerUseCase.execute(
        caravanId,
        new AddCaravanTravelerUseCase.AddCaravanTravelerCommand(
            request.fullName(),
            request.description(),
            request.availableRoleCodes(),
            request.activeRoleCodes(),
            request.activeRoleCode(),
            request.maxActiveRoleCount(),
            request.salary(),
            request.contractConditions(),
            request.consumption(),
            request.occupiedSpace(),
            request.wagonId(),
            request.drivingWagonId(),
            request.servedTravelerId()));
    return ResponseEntity.status(HttpStatus.CREATED).body(CaravanTravelerResponseMapper.toResponse(created));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/caravans/{caravanId}/travelers/{travelerId}")
  ResponseEntity<Void> deleteCaravanTraveler(@PathVariable UUID caravanId, @PathVariable UUID travelerId) {
    deleteCaravanTravelerUseCase.delete(caravanId, travelerId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/caravans/{caravanId}/travelers/{travelerId}")
  CaravanTravelerResponse updateCaravanTraveler(
      @PathVariable UUID caravanId,
      @PathVariable UUID travelerId,
      @Valid @RequestBody UpdateCaravanTravelerRequest request) {
    return CaravanTravelerResponseMapper.toResponse(
        updateCaravanTravelerUseCase.execute(
            caravanId,
            travelerId,
            new UpdateCaravanTravelerUseCase.UpdateCaravanTravelerCommand(
                request.fullName(),
                request.description(),
                request.availableRoleCodes(),
                request.activeRoleCodes(),
                request.activeRoleCode(),
                request.maxActiveRoleCount(),
                request.wagonId(),
                request.drivingWagonId(),
                request.salary(),
                request.contractConditions(),
                request.consumption(),
                request.occupiedSpace(),
                request.servedTravelerId())));
  }

  @PutMapping("/caravans/{caravanId}/travelers/{travelerId}/wagon")
  CaravanTravelerResponse updateCaravanTravelerWagon(
      @PathVariable UUID caravanId,
      @PathVariable UUID travelerId,
      @RequestBody UpdateCaravanTravelerWagonRequest request) {
    return CaravanTravelerResponseMapper.toResponse(
        updateCaravanTravelerWagonUseCase.execute(
            caravanId, travelerId, new UpdateCaravanTravelerWagonUseCase.UpdateCaravanTravelerWagonCommand(request.wagonId())));
  }

  @PutMapping("/caravans/{caravanId}/travelers/{travelerId}/role")
  CaravanTravelerResponse updateCaravanTravelerRole(
      @PathVariable UUID caravanId,
      @PathVariable UUID travelerId,
      @Valid @RequestBody UpdateCaravanTravelerRoleRequest request) {
    return CaravanTravelerResponseMapper.toResponse(
        updateCaravanTravelerRoleUseCase.execute(
            caravanId, travelerId, new UpdateCaravanTravelerRoleUseCase.UpdateCaravanTravelerRoleCommand(
                request.activeRoleCodes(),
                request.activeRoleCode(),
                request.maxActiveRoleCount(),
                request.servedTravelerId())));
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
          caravanId,
          new AddCaravanWagonUseCase.AddCaravanWagonCommand(
              request.wagonTypeCode(), request.displayName(), request.specificCommodity()));
      return ResponseEntity.status(HttpStatus.CREATED).body(CaravanWagonResponseMapper.toResponse(created));
    }

  @PutMapping("/caravans/{caravanId}/wagons/{wagonId}")
  CaravanWagonResponse updateCaravanWagon(
      @PathVariable UUID caravanId,
      @PathVariable UUID wagonId,
      @RequestBody UpdateCaravanWagonRequest request) {
    return CaravanWagonResponseMapper.toResponse(
        updateCaravanWagonUseCase.execute(caravanId, wagonId, new UpdateCaravanWagonUseCase.UpdateCaravanWagonCommand(request.displayName())));
  }

  @PostMapping("/caravans/{caravanId}/wagons/{wagonId}/damage")
  CaravanWagonResponse damageCaravanWagon(
      @PathVariable UUID caravanId,
      @PathVariable UUID wagonId,
      @Valid @RequestBody DamageCaravanWagonRequest request) {
    return CaravanWagonResponseMapper.toResponse(
        damageCaravanWagonUseCase.execute(
            caravanId,
            wagonId,
            new DamageCaravanWagonUseCase.DamageCaravanWagonCommand(request.damageAmount(), request.ignoreHardness())));
  }

  @PostMapping("/caravans/{caravanId}/wagons/{wagonId}/repair")
  CaravanWagonResponse repairCaravanWagon(
      @PathVariable UUID caravanId,
      @PathVariable UUID wagonId,
      @Valid @RequestBody RepairCaravanWagonRequest request) {
    return CaravanWagonResponseMapper.toResponse(
        repairCaravanWagonUseCase.execute(
            caravanId,
            wagonId,
            new RepairCaravanWagonUseCase.RepairCaravanWagonCommand(request.repairAmount())));
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
