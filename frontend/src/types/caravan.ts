export type CaravanStatus = "ACTIVE" | "ARCHIVED";

export interface CaravanMainStats {
  offense: number;
  defense: number;
  mobility: number;
  morale: number;
  unassignedPoints: number;
}

export interface CaravanDerivedStats {
  attack: number;
  armorClass: number;
  security: number;
  determination: number;
}

export interface CaravanOtherStats {
  speed: number;
  travelerCapacity: number;
  cargoCapacity: number;
  cargoLoad: number;
  cargoRemaining: number;
  consumption: number;
  travelerCount: number;
  wagonCount: number;
  beastCount: number;
  maxWagons: number;
}

export interface CaravanStatContribution {
  statCode: string;
  sourceType: string;
  sourceId: string;
  sourceName: string;
  modifier: string;
  operation: string;
  reason: string;
}

export interface Caravan {
  id: string;
  name: string;
  description: string | null;
  level: number;
  mainStats: CaravanMainStats;
  discontent: number;
  status: CaravanStatus;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  wagons: string[];
  travelers: string[];
  beasts: string[];
  feats: string[];
}

export interface CaravanListResponse extends Array<Caravan> {}

export interface CaravanStatistics {
  caravanId: string;
  level: number;
  mainStats: CaravanMainStats;
  derivedStats: CaravanDerivedStats;
  otherStats: CaravanOtherStats;
  discontent: number;
  moraleThreshold: number;
  contributions: CaravanStatContribution[];
  warnings: string[];
  updatedAt: string;
}

