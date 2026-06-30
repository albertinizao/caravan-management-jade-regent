import { fetchJson } from "@/services/http";
import type { Caravan, CaravanStatistics } from "@/types/caravan";

export interface CreateCaravanPayload {
  name: string;
  description?: string;
  offense?: number;
  defense?: number;
  mobility?: number;
  morale?: number;
}

export interface SelectActiveCaravanPayload {
  caravanId: string;
}

export interface ActiveCaravanResponse {
  caravan: Caravan | null;
}

export function listCaravans() {
  return fetchJson<Caravan[]>("/caravans");
}

export function createCaravan(payload: CreateCaravanPayload) {
  return fetchJson<Caravan>("/caravans", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function selectActiveCaravan(payload: SelectActiveCaravanPayload) {
  return fetchJson<ActiveCaravanResponse>("/session/active-caravan", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function getActiveCaravan() {
  return fetchJson<ActiveCaravanResponse>("/session/active-caravan");
}

export function getCaravan(id: string) {
  return fetchJson<Caravan>(`/caravans/${id}`);
}

export function getCaravanStatistics(id: string) {
  return fetchJson<CaravanStatistics>(`/caravans/${id}/statistics`);
}

export function deleteCaravan(id: string) {
  return fetchJson<void>(`/caravans/${id}`, {
    method: "DELETE",
  });
}
