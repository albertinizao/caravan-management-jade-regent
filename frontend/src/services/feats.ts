import { fetchJson } from "@/services/http";
import type {
  AddCaravanFeatPayload,
  CaravanFeat,
  CaravanFeatCatalogItem,
  UpdateCaravanFeatPayload,
} from "@/types/feat";

export function listCaravanFeatCatalog(caravanId: string) {
  return fetchJson<CaravanFeatCatalogItem[]>(`/caravans/${caravanId}/feats/catalog`);
}

export function listCaravanFeats(caravanId: string) {
  return fetchJson<CaravanFeat[]>(`/caravans/${caravanId}/feats`);
}

export function getCaravanFeat(caravanId: string, featId: string) {
  return fetchJson<CaravanFeat>(`/caravans/${caravanId}/feats/${featId}`);
}

export function addCaravanFeat(caravanId: string, payload: AddCaravanFeatPayload) {
  return fetchJson<CaravanFeat>(`/caravans/${caravanId}/feats`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateCaravanFeat(caravanId: string, featId: string, payload: UpdateCaravanFeatPayload) {
  return fetchJson<CaravanFeat>(`/caravans/${caravanId}/feats/${featId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
