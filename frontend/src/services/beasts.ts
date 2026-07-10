import { fetchJson } from "@/services/http";
import type {
  AddCaravanBeastPayload,
  BeastAssignmentType,
  BeastCatalogItem,
  CaravanBeast,
  UpdateCaravanBeastPayload,
  UpdateCaravanBeastAssignmentPayload,
} from "@/types/beast";

export interface ListCaravanBeastsParams {
  query?: string;
  sourceType?: string;
  assignmentType?: BeastAssignmentType | string;
  wagonId?: string;
}

export function listBeastCatalog(caravanId: string) {
  return fetchJson<BeastCatalogItem[]>(`/caravans/${caravanId}/beasts/catalog`);
}

export function listCaravanBeasts(caravanId: string, params?: ListCaravanBeastsParams) {
  const searchParams = new URLSearchParams();
  if (params?.query) {
    searchParams.set("query", params.query);
  }
  if (params?.sourceType) {
    searchParams.set("sourceType", params.sourceType);
  }
  if (params?.assignmentType) {
    searchParams.set("assignmentType", params.assignmentType);
  }
  if (params?.wagonId) {
    searchParams.set("wagonId", params.wagonId);
  }

  const queryString = searchParams.toString();
  return fetchJson<CaravanBeast[]>(`/caravans/${caravanId}/beasts${queryString ? `?${queryString}` : ""}`);
}

export function getCaravanBeast(caravanId: string, beastId: string) {
  return fetchJson<CaravanBeast>(`/caravans/${caravanId}/beasts/${beastId}`);
}

export function updateCaravanBeast(caravanId: string, beastId: string, payload: UpdateCaravanBeastPayload) {
  return fetchJson<CaravanBeast>(`/caravans/${caravanId}/beasts/${beastId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteCaravanBeast(caravanId: string, beastId: string) {
  return fetchJson<void>(`/caravans/${caravanId}/beasts/${beastId}`, {
    method: "DELETE",
  });
}

export function deleteUnassignedCaravanBeasts(caravanId: string) {
  return fetchJson<void>(`/caravans/${caravanId}/beasts/unassigned`, {
    method: "DELETE",
  });
}

export function addCaravanBeast(caravanId: string, payload: AddCaravanBeastPayload) {
  return fetchJson<CaravanBeast>(`/caravans/${caravanId}/beasts`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function addCaravanBeastFromCatalog(caravanId: string, beastCode: string, quantity = 1) {
  const searchParams = new URLSearchParams();
  if (quantity > 1) {
    searchParams.set("quantity", String(quantity));
  }
  const queryString = searchParams.toString();
  return fetchJson<CaravanBeast>(`/caravans/${caravanId}/beasts/catalog/${beastCode}${queryString ? `?${queryString}` : ""}`, {
    method: "POST",
  });
}

export function updateCaravanBeastAssignment(
  caravanId: string,
  beastId: string,
  payload: UpdateCaravanBeastAssignmentPayload,
) {
  return fetchJson<CaravanBeast>(`/caravans/${caravanId}/beasts/${beastId}/assignment`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function clearCaravanBeastAssignment(caravanId: string, beastId: string) {
  return fetchJson<CaravanBeast>(`/caravans/${caravanId}/beasts/${beastId}/assignment`, {
    method: "DELETE",
  });
}
