import { fetchJson } from "@/services/http";
import type {
  Caravan,
  CaravanDayCyclePreview,
  CaravanDayCycleResult,
  CaravanStatistics,
} from "@/types/caravan";

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

export interface CaravanDayCycleChoicePayload {
  travelerId: string;
  mode: "HUNT" | "EXPLORE" | string;
}

export interface CaravanDayCyclePayload {
  idempotencyKey: string;
  fastingEnabled: boolean;
  choices: CaravanDayCycleChoicePayload[];
}

export interface UpdateCaravanDeltaPayload {
  delta: number;
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

export interface UpdateCaravanMainStatsPayload {
  offense: number;
  defense: number;
  mobility: number;
  morale: number;
}

export function updateCaravanMainStats(id: string, payload: UpdateCaravanMainStatsPayload) {
  return fetchJson<Caravan>(`/caravans/${id}/main-stats`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanLevel(id: string, payload: UpdateCaravanDeltaPayload) {
  return fetchJson<Caravan>(`/caravans/${id}/level`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanDiscontent(id: string, payload: UpdateCaravanDeltaPayload) {
  return fetchJson<Caravan>(`/caravans/${id}/discontent`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function previewCaravanDayCycle(id: string, payload: Omit<CaravanDayCyclePayload, "idempotencyKey">) {
  return fetchJson<CaravanDayCyclePreview>(`/caravans/${id}/day-cycle/preview`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function advanceCaravanDayCycle(id: string, payload: CaravanDayCyclePayload) {
  return fetchJson<CaravanDayCycleResult>(`/caravans/${id}/day-cycle/advance`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function deleteCaravan(id: string) {
  return fetchJson<void>(`/caravans/${id}`, {
    method: "DELETE",
  });
}
