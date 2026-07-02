export type CaravanFeatAcquisitionSourceType = "LEVEL_UP" | "OTHER";

export interface CaravanFeatCatalogItem {
  code: string;
  name: string;
  description: string;
  prerequisites: string[];
  benefitText: string;
  specialText: string | null;
  notes: string | null;
  repeatable: boolean;
  selectionLimit: number;
  minimumLevel: number | null;
  ownedCount: number;
  available: boolean;
  blockedReason: string | null;
}

export interface CaravanFeat {
  id: string;
  caravanId: string;
  featTypeCode: string;
  name: string;
  description: string;
  prerequisites: string[];
  benefitText: string;
  specialText: string | null;
  notes: string | null;
  acquisitionSourceType: CaravanFeatAcquisitionSourceType;
  acquisitionLevel: number | null;
  acquisitionCause: string | null;
  selectionIndex: number;
  active: boolean;
  blockedReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AddCaravanFeatPayload {
  featTypeCode: string;
  acquisitionSourceType: CaravanFeatAcquisitionSourceType;
  acquisitionLevel?: number | null;
  acquisitionCause?: string | null;
  active?: boolean | null;
}

export interface UpdateCaravanFeatPayload {
  acquisitionSourceType: CaravanFeatAcquisitionSourceType;
  acquisitionLevel?: number | null;
  acquisitionCause?: string | null;
  active?: boolean | null;
}
