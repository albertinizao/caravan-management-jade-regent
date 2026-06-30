import { fetchJson } from "@/services/http";
import type {
  CaravanWagon,
  CaravanWagonImprovement,
  WagonCatalogItem,
  WagonImprovementCatalogItem,
} from "@/types/wagon";

export interface AddCaravanWagonPayload {
  wagonTypeCode: string;
}

export interface AddCaravanWagonImprovementPayload {
  improvementTypeCode: string;
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

export function listWagonImprovementCatalog(caravanId: string, wagonId: string) {
  return fetchJson<WagonImprovementCatalogItem[]>(
    `/caravans/${caravanId}/wagons/${wagonId}/improvements/catalog`
  );
}

export function listCaravanWagonImprovements(caravanId: string, wagonId: string) {
  return fetchJson<CaravanWagonImprovement[]>(
    `/caravans/${caravanId}/wagons/${wagonId}/improvements`
  );
}

export function addCaravanWagon(caravanId: string, payload: AddCaravanWagonPayload) {
  return fetchJson<CaravanWagon>(`/caravans/${caravanId}/wagons`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function addCaravanWagonImprovement(
  caravanId: string,
  wagonId: string,
  payload: AddCaravanWagonImprovementPayload,
) {
  return fetchJson<CaravanWagon>(`/caravans/${caravanId}/wagons/${wagonId}/improvements`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function deleteCaravanWagonImprovement(
  caravanId: string,
  wagonId: string,
  improvementId: string,
) {
  return fetchJson<CaravanWagon>(
    `/caravans/${caravanId}/wagons/${wagonId}/improvements/${improvementId}`,
    {
      method: "DELETE",
    },
  );
}

export function deleteCaravanWagon(caravanId: string, wagonId: string) {
  return fetchJson<void>(`/caravans/${caravanId}/wagons/${wagonId}`, {
    method: "DELETE",
  });
}
