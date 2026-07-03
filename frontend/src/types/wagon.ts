import type { CaravanBeast } from "@/types/beast";

export interface WagonCatalogItem {
  code: string;
  name: string;
  category: string;
  cost: number;
  hitPoints: number;
  currentHitPoints: number | null;
  hardness: number;
  propulsion: string;
  travelerCapacity: number;
  cargoCapacity: number;
  limitKind: "UNLIMITED" | "FIXED" | "RATIO_OF_CARAVAN_CAPACITY";
  limitFixedMax: number | null;
  limitRatioDenominator: number | null;
  limit: string;
  consumption: number;
  specialBenefit: string;
  description: string;
  notes: string | null;
}

export interface WagonImprovementCatalogItem {
  code: string;
  name: string;
  category: string;
  costExpression: string;
  hitPointsBonus: number | null;
  hitPointsMultiplier: number | null;
  hardnessBonus: number | null;
  hardnessMultiplier: number | null;
  propulsionEffect: string;
  travelerCapacityBonus: number | null;
  travelerCapacityMultiplier: number | null;
  travelerCapacityMinimumIncrement: number | null;
  travelerCapacityOverride: number | null;
  cargoCapacityBonus: number | null;
  cargoCapacityMultiplier: number | null;
  cargoCapacityMinimumIncrement: number | null;
  cargoCapacityOverride: number | null;
  consumptionBonus: number | null;
  maxPerWagon: number;
  repeatable: boolean;
  requiredBasePropulsionFragment: string | null;
  ownedCount: number;
  available: boolean;
  blockedReason: string | null;
  specialBenefit: string;
  description: string;
  notes: string | null;
}

export interface CaravanWagonImprovement {
  id: string;
  caravanId: string;
  wagonId: string;
  improvementTypeCode: string;
  name: string;
  category: string;
  costExpression: string;
  specialBenefit: string;
  description: string;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CaravanWagon {
  id: string;
  caravanId: string;
  wagonTypeCode: string;
  name: string;
  specificCommodity: string | null;
  category: string;
  cost: number;
  hitPoints: number;
  currentHitPoints: number | null;
  hardness: number;
  propulsion: string;
  travelerCapacity: number;
  cargoCapacity: number;
  limitKind: "UNLIMITED" | "FIXED" | "RATIO_OF_CARAVAN_CAPACITY";
  limitFixedMax: number | null;
  limitRatioDenominator: number | null;
  limit: string;
  consumption: number;
  specialBenefit: string;
  description: string;
  notes: string | null;
  draftBeasts: CaravanBeast[];
  draftStrength: number;
  draftRequiredStrength: number;
  carreteroId: string | null;
  carreteroName: string | null;
  improvements: CaravanWagonImprovement[];
  createdAt: string;
  updatedAt: string;
}
