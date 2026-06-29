import { fetchJson } from "@/services/http";
import type { CaravanWagon, WagonCatalogItem } from "@/types/wagon";

export interface AddCaravanWagonPayload {
  wagonTypeCode: string;
}

export function listWagonCatalog(caravanId: string) {
  return fetchJson<WagonCatalogItem[]>(`/caravans/${caravanId}/wagons/catalog`);
}

export function listCaravanWagons(caravanId: string) {
  return fetchJson<CaravanWagon[]>(`/caravans/${caravanId}/wagons`);
}

export function getCaravanWagon(caravanId: string, wagonId: string) {
  return fetchJson<CaravanWagon>(`/caravans/${caravanId}/wagons/${wagonId}`);
}

export function addCaravanWagon(caravanId: string, payload: AddCaravanWagonPayload) {
  return fetchJson<CaravanWagon>(`/caravans/${caravanId}/wagons`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function deleteCaravanWagon(caravanId: string, wagonId: string) {
  return fetchJson<void>(`/caravans/${caravanId}/wagons/${wagonId}`, {
    method: "DELETE",
  });
}
