export interface WagonCatalogItem {
  code: string;
  name: string;
  category: string;
  cost: number;
  hitPoints: number;
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

export interface CaravanWagon {
  id: string;
  caravanId: string;
  wagonTypeCode: string;
  name: string;
  category: string;
  cost: number;
  hitPoints: number;
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
  createdAt: string;
  updatedAt: string;
}
