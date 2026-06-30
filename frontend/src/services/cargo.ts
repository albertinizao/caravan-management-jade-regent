import { fetchJson } from "@/services/http";
import type {
  AddCaravanCargoFromCatalogPayload,
  AddCustomCaravanCargoPayload,
  CaravanCargo,
  CaravanCargoSummary,
  CargoCatalogItem,
  UpdateCaravanCargoPayload,
  UpdateCaravanCargoWagonPayload,
} from "@/types/cargo";

export function listCargoCatalog(caravanId: string) {
  return fetchJson<CargoCatalogItem[]>(`/caravans/${caravanId}/cargo/catalog`);
}

export function getCargoCatalogItem(caravanId: string, cargoCode: string) {
  return fetchJson<CargoCatalogItem>(`/caravans/${caravanId}/cargo/catalog/${cargoCode}`);
}

export function listCaravanCargo(
  caravanId: string,
  params?: { query?: string; sourceType?: string; category?: string; wagonId?: string | null },
) {
  const searchParams = new URLSearchParams();
  if (params?.query) searchParams.set("query", params.query);
  if (params?.sourceType) searchParams.set("sourceType", params.sourceType);
  if (params?.category) searchParams.set("category", params.category);
  if (params?.wagonId) searchParams.set("wagonId", params.wagonId);
  const suffix = searchParams.toString() ? `?${searchParams.toString()}` : "";
  return fetchJson<CaravanCargo[]>(`/caravans/${caravanId}/cargo${suffix}`);
}

export function getCaravanCargo(caravanId: string, cargoId: string) {
  return fetchJson<CaravanCargo>(`/caravans/${caravanId}/cargo/${cargoId}`);
}

export function listCaravanCargoSummary(caravanId: string) {
  return fetchJson<CaravanCargoSummary[]>(`/caravans/${caravanId}/cargo/summary`);
}

export function addCargoFromCatalog(caravanId: string, payload: AddCaravanCargoFromCatalogPayload) {
  return fetchJson<CaravanCargo>(`/caravans/${caravanId}/cargo/catalog`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function addCustomCargo(caravanId: string, payload: AddCustomCaravanCargoPayload) {
  return fetchJson<CaravanCargo>(`/caravans/${caravanId}/cargo/custom`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanCargo(caravanId: string, cargoId: string, payload: UpdateCaravanCargoPayload) {
  return fetchJson<CaravanCargo>(`/caravans/${caravanId}/cargo/${cargoId}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanCargoWagon(
  caravanId: string,
  cargoId: string,
  payload: UpdateCaravanCargoWagonPayload,
) {
  return fetchJson<CaravanCargo>(`/caravans/${caravanId}/cargo/${cargoId}/wagon`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteCaravanCargo(caravanId: string, cargoId: string) {
  return fetchJson<void>(`/caravans/${caravanId}/cargo/${cargoId}`, {
    method: "DELETE",
  });
}
