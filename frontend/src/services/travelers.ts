import { fetchJson } from "@/services/http";
import type {
  AddCaravanTravelerPayload,
  CaravanTraveler,
  TravelerRoleCatalogItem,
  UpdateCaravanTravelerPayload,
  UpdateCaravanTravelerRolePayload,
  UpdateCaravanTravelerWagonPayload,
} from "@/types/traveler";

export interface ListCaravanTravelersParams {
  query?: string;
  roleCode?: string;
  wagonId?: string;
}

export function listTravelerRoleCatalog(caravanId: string) {
  return fetchJson<TravelerRoleCatalogItem[]>(`/caravans/${caravanId}/travelers/roles/catalog`);
}

export function listCaravanTravelers(caravanId: string, params?: ListCaravanTravelersParams) {
  const searchParams = new URLSearchParams();
  if (params?.query) {
    searchParams.set("query", params.query);
  }
  if (params?.roleCode) {
    searchParams.set("roleCode", params.roleCode);
  }
  if (params?.wagonId) {
    searchParams.set("wagonId", params.wagonId);
  }

  const queryString = searchParams.toString();
  return fetchJson<CaravanTraveler[]>(
    `/caravans/${caravanId}/travelers${queryString ? `?${queryString}` : ""}`
  );
}

export function getCaravanTraveler(caravanId: string, travelerId: string) {
  return fetchJson<CaravanTraveler>(`/caravans/${caravanId}/travelers/${travelerId}`);
}

export function addCaravanTraveler(caravanId: string, payload: AddCaravanTravelerPayload) {
  return fetchJson<CaravanTraveler>(`/caravans/${caravanId}/travelers`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanTraveler(
  caravanId: string,
  travelerId: string,
  payload: UpdateCaravanTravelerPayload,
) {
  return fetchJson<CaravanTraveler>(`/caravans/${caravanId}/travelers/${travelerId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanTravelerWagon(
  caravanId: string,
  travelerId: string,
  payload: UpdateCaravanTravelerWagonPayload,
) {
  return fetchJson<CaravanTraveler>(`/caravans/${caravanId}/travelers/${travelerId}/wagon`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanTravelerRole(
  caravanId: string,
  travelerId: string,
  payload: UpdateCaravanTravelerRolePayload,
) {
  return fetchJson<CaravanTraveler>(`/caravans/${caravanId}/travelers/${travelerId}/role`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
