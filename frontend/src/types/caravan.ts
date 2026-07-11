import type { GolarionDate, WeatherSnapshot } from "./calendar";

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
  travelingBeastCount: number;
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

export interface CaravanDayCycleLogEntry {
  section: string;
  title: string;
  details: string[];
  foodDelta: number;
}

export interface CaravanDayCyclePreview {
  caravanId: string;
  previewFingerprint: string;
  confirmed: boolean;
  resolutionId: string | null;
  confirmedAt: string | null;
  dayIndex: number;
  currentSupplyUnits: number;
  currentPerishableFood: number;
  currentPerishableUnits: number;
  generatedSuppliesFromAgricultors: number;
  generatedAlchemyValueFromBoticarios: number;
  requiredConsumption: number;
  consumptionCovered: boolean;
  generatedFood: number;
  leftoverFood: number;
  finalSupplyUnits: number;
  finalPerishableUnits: number;
  finalPerishableFood: number;
  suppliesConsumed: number;
  simulation: CaravanDayCycleLogEntry[];
  warnings: string[];
}

export interface CaravanMultiDayCyclePreview {
  caravanId: string;
  basePreviewFingerprint: string;
  confirmed: boolean;
  requestedDays: number;
  startDayIndex: number;
  endDayIndex: number;
  totalRequiredConsumption: number;
  totalGeneratedFood: number;
  totalGeneratedSuppliesFromAgricultors: number;
  totalGeneratedAlchemyValueFromBoticarios: number;
  totalSuppliesConsumed: number;
  daysWithUncoveredConsumption: number;
  finalSupplyUnits: number;
  finalPerishableUnits: number;
  finalPerishableFood: number;
  confirmedAt: string | null;
  warnings: string[];
  dayPreviews: CaravanDayCyclePreview[];
}

export interface CaravanSupplyConsumption {
  remainingFood: number;
}

export interface CaravanBackupTravelerContract {
  salary: number | null;
  conditions: string | null;
  startedAt: string;
  endedAt: string | null;
}

export interface CaravanBackupTravelerRoleData {
  servedTravelerId: string | null;
  generatingFood: boolean;
  daysServing: number;
}

export interface CaravanBackupCalendarEvent {
  id: number | null;
  caravanId: string;
  date: GolarionDate;
  name: string;
  description: string | null;
  secret: boolean;
  createdAt: string;
}

export interface CaravanBackupWeatherProfile {
  caravanId: string;
  climateBaseline: string;
  elevation: string;
  crownRegion: string | null;
  updatedAt: string;
}

export interface CaravanBackupWeatherForecastState {
  caravanId: string;
  date: GolarionDate;
  targetTemperatureF: number;
  remainingTargetDays: number;
  dayBaseTemperatureF: number;
  nightBaseTemperatureF: number;
  carryOverPrecipitation: string | null;
  carryOverRemainingPeriods: number;
  severeEvent: string | null;
  generatedAt: string;
}

export interface CaravanBackupWeatherSnapshot {
  caravanId: string;
  date: GolarionDate;
  weather: WeatherSnapshot;
  generatedAt: string;
}

export interface CaravanBackup {
  schemaVersion: number;
  active: boolean;
  caravan: {
    id: string;
    name: string;
    description: string | null;
    level: number;
    mainStats: CaravanMainStats;
    discontent: number;
    status: CaravanStatus;
    createdAt: string;
    updatedAt: string;
  };
  supplyState: {
    caravanId: string;
    provisionReserve: number;
    standardReserve: number;
    perishableReserve: number;
    daysPassed: number;
    updatedAt: string;
  };
  wagons: Array<{
    id: string;
    caravanId: string;
    wagonTypeCode: string;
    displayName: string | null;
    specificCommodity: string | null;
    currentHitPoints: number | null;
    createdAt: string;
    updatedAt: string;
  }>;
  wagonImprovements: Array<{
    id: string;
    caravanId: string;
    wagonId: string;
    improvementTypeCode: string;
    createdAt: string;
    updatedAt: string;
  }>;
  travelers: Array<{
    id: string;
    caravanId: string;
    fullName: string;
    description: string | null;
    availableRoleCodes: string[];
    activeRoleCodes: string[];
    activeRoleCode: string;
    maxActiveRoleCount: number;
    roleSpecificData: CaravanBackupTravelerRoleData;
    wagonId: string | null;
    drivingWagonId: string | null;
    contract: CaravanBackupTravelerContract | null;
    consumption: number;
    occupiedSpace: number;
    createdAt: string;
    updatedAt: string;
  }>;
  cargo: Array<{
    id: string;
    caravanId: string;
    sourceType: "CATALOG" | "CUSTOM";
    catalogCode: string | null;
    displayName: string;
    category: string;
    quantity: number;
    cargoUnits: number;
    currentProvisions: number | null;
    dayPassed: boolean;
    wagonId: string | null;
    origin: string | null;
    specificCommodity: string | null;
    deity: string | null;
    notes: string | null;
    createdAt: string;
    updatedAt: string;
  }>;
  beasts: Array<{
    id: string;
    caravanId: string;
    sourceType: "CATALOG" | "CUSTOM";
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
    assignmentType: "NONE" | "DRAFT" | "TRAVELER";
    assignedWagonId: string | null;
    createdAt: string;
    updatedAt: string;
    occupiedSpace: number;
  }>;
  feats: Array<{
    id: string;
    caravanId: string;
    featTypeCode: string;
    acquisitionSourceType: "LEVEL_UP" | "OTHER";
    acquisitionLevel: number | null;
    acquisitionCause: string | null;
    selectionIndex: number;
    active: boolean;
    manualApplies: boolean | null;
    manualAppliesReason: string | null;
    createdAt: string;
    updatedAt: string;
  }>;
  dayResolutions: Array<{
    id: string;
    caravanId: string;
    idempotencyKey: string;
    resolvedDayIndex: number;
    resolvedAt: string;
    startingReserve: number;
    provisionsInConsumption: CaravanSupplyConsumption[];
    endingReserve: number;
    totalConsumption: number;
    totalGeneration: number;
    netDelta: number;
    shortage: number;
    generatedProvisions: number;
    generatedFood: number;
    consumedProvisions: number;
    surplusProvisions: number;
    cargoMovementSummary: string;
    choicesSummary: string;
    contributionsSummary: string;
    warningsSummary: string;
  }>;
  calendarEvents: CaravanBackupCalendarEvent[];
  weatherProfile: CaravanBackupWeatherProfile | null;
  weatherForecastStates: CaravanBackupWeatherForecastState[];
  weatherSnapshots: CaravanBackupWeatherSnapshot[];
}

export interface CaravanBackupImportSummary {
  caravans: number;
  supplyStates: number;
  wagons: number;
  wagonImprovements: number;
  travelers: number;
  cargo: number;
  beasts: number;
  feats: number;
  dayResolutions: number;
  calendarEvents: number;
  weatherProfiles: number;
  weatherForecastStates: number;
  weatherSnapshots: number;
}

export interface CaravanBackupImportResult {
  caravan: Caravan;
  summary: CaravanBackupImportSummary;
}

