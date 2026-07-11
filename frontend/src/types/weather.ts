export type WeatherClimateBaseline = "COLD" | "CROWN_OF_THE_WORLD" | "TEMPERATE" | "TROPICAL";
export type WeatherElevation = "SEA_LEVEL" | "LOWLAND" | "HIGHLAND" | "PEAK";
export type CrownWeatherRegion = "OUTER_RIM" | "HIGH_ICE" | "BOREAL_EXPANSE";

export interface CaravanWeatherProfile {
  caravanId: string;
  climateBaseline: WeatherClimateBaseline;
  elevation: WeatherElevation;
  crownRegion: CrownWeatherRegion | null;
  updatedAt: string;
}
