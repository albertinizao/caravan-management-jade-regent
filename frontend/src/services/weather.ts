import { fetchJson } from "@/services/http";
import type { CaravanWeatherProfile } from "@/types/weather";

export interface UpdateCaravanWeatherProfilePayload {
  climateBaseline: CaravanWeatherProfile["climateBaseline"];
  elevation: CaravanWeatherProfile["elevation"];
  crownOfWorld: boolean;
  effectiveFromYear: number;
  effectiveFromMonth: number;
  effectiveFromDay: number;
}

export function getCaravanWeatherProfile(caravanId: string) {
  return fetchJson<CaravanWeatherProfile>(`/caravans/${caravanId}/weather/profile`);
}

export function updateCaravanWeatherProfile(caravanId: string, payload: UpdateCaravanWeatherProfilePayload) {
  return fetchJson<CaravanWeatherProfile>(`/caravans/${caravanId}/weather/profile`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
