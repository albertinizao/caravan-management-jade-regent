export interface TravelerRoleCatalogItem {
  code: string;
  name: string;
  description: string;
  requirements: string;
  requiresTargetTraveler: boolean;
}

export interface CaravanTraveler {
  id: string;
  caravanId: string;
  fullName: string;
  description: string | null;
  availableRoleCodes: string[];
  activeRoleCodes: string[];
  activeRoleCode: string;
  activeRoleName: string;
  wagonId: string | null;
  wagonName: string | null;
  maxActiveRoleCount: number;
  salary: number | null;
  contractConditions: string | null;
  consumption: number;
  servedTravelerId: string | null;
  servedTravelerName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AddCaravanTravelerPayload {
  fullName: string;
  description?: string;
  availableRoleCodes: string[];
  activeRoleCodes: string[];
  activeRoleCode?: string;
  maxActiveRoleCount?: number | null;
  salary?: number | null;
  contractConditions?: string | null;
  consumption?: number | null;
  servedTravelerId?: string | null;
}

export interface UpdateCaravanTravelerRolePayload {
  activeRoleCodes: string[];
  activeRoleCode: string;
  maxActiveRoleCount?: number | null;
  servedTravelerId?: string | null;
}

export interface UpdateCaravanTravelerWagonPayload {
  wagonId: string | null;
}

export interface UpdateCaravanTravelerPayload {
  fullName: string;
  description?: string | null;
  availableRoleCodes?: string[] | null;
  activeRoleCodes: string[];
  activeRoleCode: string;
  maxActiveRoleCount?: number | null;
  wagonId?: string | null;
  salary?: number | null;
  contractConditions?: string | null;
  consumption?: number | null;
  servedTravelerId?: string | null;
}
