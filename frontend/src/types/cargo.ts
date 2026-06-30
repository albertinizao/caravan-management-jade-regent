export interface CargoCatalogItem {
  code: string;
  name: string;
  category: string;
  priceExpression: string;
  defaultQuantity: number | null;
  quantityEditable: boolean;
  defaultCargoUnits: number | null;
  cargoUnitsEditable: boolean;
  quantityLabel: string;
  benefitText: string;
  description: string;
  notes: string | null;
  requiredMetadataKeys: string[];
  allowedWagonCodes: string[];
}

export interface CaravanCargo {
  id: string;
  caravanId: string;
  sourceType: "CATALOG" | "CUSTOM";
  sourceTypeLabel: string;
  catalogCode: string | null;
  catalogName: string | null;
  displayName: string;
  category: string;
  quantity: number;
  cargoUnits: number;
  wagonId: string | null;
  wagonName: string | null;
  origin: string | null;
  specificCommodity: string | null;
  deity: string | null;
  notes: string | null;
  priceExpression: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CaravanCargoSummary {
  wagonId: string;
  wagonName: string;
  cargoCapacity: number;
  usedCargoUnits: number;
  remainingCargoUnits: number;
  cargoEntryCount: number;
}

export interface AddCaravanCargoFromCatalogPayload {
  catalogCode: string;
  quantity?: number | null;
  cargoUnits?: number | null;
  wagonId: string;
  origin?: string | null;
  specificCommodity?: string | null;
  deity?: string | null;
  notes?: string | null;
}

export interface AddCustomCaravanCargoPayload {
  displayName: string;
  category: string;
  quantity: number;
  cargoUnits: number;
  wagonId: string;
  origin?: string | null;
  specificCommodity?: string | null;
  deity?: string | null;
  notes?: string | null;
}

export interface UpdateCaravanCargoPayload {
  displayName?: string | null;
  category?: string | null;
  quantity?: number | null;
  cargoUnits?: number | null;
  wagonId?: string | null;
  origin?: string | null;
  specificCommodity?: string | null;
  deity?: string | null;
  notes?: string | null;
}

export interface UpdateCaravanCargoWagonPayload {
  wagonId: string | null;
}
