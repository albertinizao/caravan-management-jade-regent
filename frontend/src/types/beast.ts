export type BeastSourceType = "CATALOG" | "CUSTOM";
export type BeastAssignmentType = "NONE" | "DRAFT" | "TRAVELER";

export interface BeastCatalogItem {
  code: string;
  name: string;
  basePrice: number | null;
  trainedPrice: number | null;
  size: string;
  strength: number;
  speed: number;
  thermalAdaptation: number | null;
  fourLegged: boolean;
  specialNote: string;
  description: string;
  notes: string | null;
}

export interface CaravanBeast {
  id: string;
  caravanId: string;
  sourceType: BeastSourceType;
  catalogBeastCode: string | null;
  name: string;
  size: string;
  strength: number;
  speed: number;
  thermalAdaptation: number | null;
  basePrice: number | null;
  trainedPrice: number | null;
  fourLegged: boolean;
  specialNote: string;
  description: string;
  customNotes: string | null;
  consumption: number;
  occupiedSpace: number;
  availableRoleCodes: string[] | null;
  activeRoleCode: string | null;
  assignmentType: BeastAssignmentType;
  assignedWagonId: string | null;
  assignedWagonName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AddCaravanBeastPayload {
  sourceType: BeastSourceType;
  catalogBeastCode?: string | null;
  name: string;
  size: string;
  strength?: number | null;
  speed?: number | null;
  thermalAdaptation?: number | null;
  basePrice?: number | null;
  trainedPrice?: number | null;
  fourLegged?: boolean | null;
  specialNote: string;
  description: string;
  customNotes?: string | null;
  consumption?: number | null;
  occupiedSpace?: number | null;
  quantity?: number | null;
}

export interface UpdateCaravanBeastPayload {
  consumption?: number | null;
  occupiedSpace?: number | null;
}

export interface UpdateCaravanBeastAssignmentPayload {
  assignmentType: BeastAssignmentType;
  wagonId: string | null;
  availableRoleCodes?: string[] | null;
  activeRoleCode?: string | null;
}
