export type WeatherClimateBaseline = "COLD" | "TEMPERATE" | "TROPICAL";
export type WeatherElevation = "SEA_LEVEL" | "LOWLAND" | "HIGHLAND" | "PEAK";

export interface CaravanWeatherProfile {
  caravanId: string;
  climateBaseline: WeatherClimateBaseline;
  elevation: WeatherElevation;
  crownOfWorld: boolean;
  updatedAt: string;
}
