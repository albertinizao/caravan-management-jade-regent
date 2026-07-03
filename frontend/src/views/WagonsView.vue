<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

import { useToast } from "@/composables/useToast";
import { getActiveCaravan, listCaravans } from "@/services/caravans";
import {
  addCargoFromCatalog,
  addCustomCargo,
  deleteCaravanCargo,
  listCaravanCargo,
  listCaravanCargoSummary,
  listCargoCatalog,
} from "@/services/cargo";
import { listCaravanBeasts, updateCaravanBeastAssignment, clearCaravanBeastAssignment } from "@/services/beasts";
import {
  addCaravanWagon,
  addCaravanWagonImprovement,
  damageCaravanWagon,
  deleteCaravanWagon,
  deleteCaravanWagonImprovement,
  getCaravanWagon,
  listCaravanWagons,
  listWagonCatalog,
  listWagonImprovementCatalog,
  repairCaravanWagon,
  updateCaravanWagon,
} from "@/services/wagons";
import { listCaravanTravelers, updateCaravanTravelerWagon } from "@/services/travelers";
import type { Caravan } from "@/types/caravan";
import type { CaravanBeast } from "@/types/beast";
import type { CaravanCargo, CaravanCargoSummary, CargoCatalogItem } from "@/types/cargo";
import type { CaravanTraveler } from "@/types/traveler";
import type {
  CaravanWagon,
  CaravanWagonImprovement,
  WagonCatalogItem,
  WagonImprovementCatalogItem,
} from "@/types/wagon";

const caravans = ref<Caravan[]>([]);
const activeCaravan = ref<Caravan | null>(null);
const catalog = ref<WagonCatalogItem[]>([]);
const wagons = ref<CaravanWagon[]>([]);
const travelers = ref<CaravanTraveler[]>([]);
const beasts = ref<CaravanBeast[]>([]);
const cargoSummary = ref<CaravanCargoSummary[]>([]);
const loading = ref(true);
const submitting = ref(false);
const pendingAction = ref<string | null>(null);
const error = ref<string | null>(null);
const addModalOpen = ref(false);
const selectedCatalogCode = ref<string | null>(null);
const addWagonName = ref("");
const specificCommodityOptions = [
  "Madera",
  "Hierro",
  "Cobre",
  "Cristal",
  "Cuero",
  "Plata",
  "Sal",
  "Oro",
  "Adamantita",
  "Mithril",
  "Platino",
  "Carbón",
  "Hielo",
  "Leña",
  "Materiales mágicos",
  "Munición (concreta)",
] as const;
const addWagonSpecificCommodityOption = ref("");
const addWagonSpecificCommodityCustom = ref("");
const selectedWagon = ref<CaravanWagon | null>(null);
const wagonAlertsModalOpen = ref(false);
const wagonAlertsModalTarget = ref<CaravanWagon | null>(null);
const wagonNameDraft = ref("");
const wagonNameEditorOpen = ref(false);
const selectedWagonCargo = ref<CaravanCargo[]>([]);
const wagonCargoCatalog = ref<CargoCatalogItem[]>([]);
const assignmentModalOpen = ref(false);
const assignmentModalMode = ref<"traveler" | "beast-traveler" | "beast-draft">("traveler");
const assignmentModalError = ref<string | null>(null);
const assignmentModalSuccess = ref<{ id: number; message: string } | null>(null);
let assignmentModalSuccessId = 0;
let assignmentModalSuccessTimer: number | null = null;
const improvementCatalog = ref<WagonImprovementCatalogItem[]>([]);
const improvementModalOpen = ref(false);
const selectedImprovementCode = ref<string | null>(null);
const improvementsExpanded = ref(false);
const improvementDeleteMode = ref(false);
const draftPanelExpanded = ref(true);
const cargoModalOpen = ref(false);
const cargoModalMode = ref<"catalog" | "custom">("catalog");
const cargoModalError = ref<string | null>(null);
const healthModalOpen = ref(false);
const healthModalMode = ref<"damage" | "repair">("damage");
const healthModalError = ref<string | null>(null);
const healthModalAmount = ref("");
const healthModalIgnoreHardness = ref(false);
const cargoCatalogCode = ref("");
const cargoQuantity = ref("1");
const cargoUnits = ref("1");
const cargoDisplayName = ref("");
const cargoCategory = ref("");
const cargoOrigin = ref("");
const cargoSpecificCommodityOption = ref("");
const cargoSpecificCommodityCustom = ref("");
const cargoDeity = ref("");
const cargoNotes = ref("");
const addTypeFilter = ref<"all" | "viajeros" | "mercancias" | "especiales">("all");
const addSearch = ref("");
const improvementSearch = ref("");
const wagonsTypeFilter = ref<"all" | "viajeros" | "mercancias" | "especiales">("all");
const wagonsSearch = ref("");
const draftMediumSlotsPerLargeBeast = 4;
const carreteroRoleCode = "carretero";
const spaceFormatter = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 1,
});
const { showToast } = useToast();

function isPending(action: string) {
  return pendingAction.value === action;
}

interface DraftRequirement {
  maxLargeBeasts: number;
  maxMediumBeasts: number;
  minimumStrength: number;
}

interface DraftStrengthState {
  className: string;
  label: string;
  description: string;
}

interface WagonDraftRequirement {
  maxLargeBeasts: number;
  maxMediumBeasts: number;
  minimumStrength: number;
}

interface WagonDraftBand {
  label: string;
  className: string;
  startPercent: number;
  endPercent: number;
  widthPercent: number;
}

interface WagonAlert {
  kind: "danger" | "warning" | "info";
  title: string;
  description: string;
}

interface WagonMeterSegment {
  key: string;
  label: string;
  widthPercent: number;
  color: string;
}

interface WagonRowSummary {
  wagon: CaravanWagon;
  currentHitPoints: number;
  travelerCount: number;
  travelerBeastSpace: number;
  travelerCapacity: number;
  cargoLoad: number;
  cargoCapacity: number;
  draftLargeCount: number;
  draftMediumCount: number;
  draftLargeCapacity: number;
  draftMediumCapacity: number;
  draftRequirement: WagonDraftRequirement | null;
  draftStrength: number;
  draftBands: WagonDraftBand[];
  speed: number;
  totalConsumption: number;
  hasCarretero: boolean;
  alerts: WagonAlert[];
}

interface DraftBeastGroup {
  key: string;
  name: string;
  size: string;
  sizeLabel: string;
  count: number;
  fourLegged: boolean;
  baseStrength: number;
  effectiveStrength: number;
  totalEffectiveStrength: number;
}

interface GroupedCargoRow {
  key: string;
  representative: CaravanCargo;
  entries: CaravanCargo[];
  quantity: number;
  totalCargoLoad: number;
}

const selectedCatalogItem = computed(() => {
  if (selectedCatalogCode.value) {
    return catalog.value.find((item) => item.code === selectedCatalogCode.value) ?? null;
  }

  return null;
});

const selectedImprovementItem = computed(() => {
  if (!selectedImprovementCode.value) {
    return null;
  }

  return improvementCatalog.value.find((item) => item.code === selectedImprovementCode.value) ?? null;
});

const totalCapacity = computed(() => (activeCaravan.value ? 10 + activeCaravan.value.level : 0));
const caravanWagonLimitState = computed(() => {
  if (!activeCaravan.value) {
    return null;
  }

  const maxWagons = totalCapacity.value;
  const currentWagons = wagons.value.length;

  if (currentWagons > maxWagons) {
    return {
      kind: "danger" as const,
      message: `La caravana supera su límite de carros en ${currentWagons - maxWagons}. Aplica -1 a cualquier tirada por cada carro adicional.`,
    };
  }

  if (currentWagons === maxWagons) {
    return {
      kind: "warning" as const,
      message: "La caravana ha alcanzado su límite de carros. El siguiente carro lo superará y aplicará -1 a cualquier tirada por cada carro adicional.",
    };
  }

  return null;
});
const selectedCatalogNeedsOverride = computed(() =>
  selectedCatalogItem.value ? isLimitExceeded(selectedCatalogItem.value) : false
);
const selectedCatalogRequiresSpecificCommodity = computed(
  () => selectedCatalogItem.value?.code === "carro-de-mercancias-especificas",
);
const addWagonSpecificCommodityValue = computed(() => {
  if (!selectedCatalogRequiresSpecificCommodity.value) {
    return null;
  }

  if (addWagonSpecificCommodityOption.value === "custom") {
    return addWagonSpecificCommodityCustom.value.trim() || null;
  }

  return addWagonSpecificCommodityOption.value || null;
});
watch(addWagonSpecificCommodityOption, (option) => {
  if (option !== "custom") {
    addWagonSpecificCommodityCustom.value = "";
  }
});
const visibleCatalogItems = computed(() =>
  catalog.value.filter((item) => matchesTypeFilter(item.category, addTypeFilter.value))
    .filter((item) => matchesSearch(item.name, addSearch.value))
);
const visibleWagons = computed(() =>
  wagons.value.filter((item) => matchesTypeFilter(item.category, wagonsTypeFilter.value))
    .filter((item) => matchesSearch(item.name, wagonsSearch.value))
);
const cargoSummaryByWagonId = computed(() =>
  Object.fromEntries(cargoSummary.value.map((item) => [item.wagonId, item] as const)),
);

function wagonTravelersFor(wagonId: string) {
  return travelers.value.filter((traveler) => traveler.wagonId === wagonId);
}

function wagonTravelerBeastsFor(wagonId: string) {
  return beasts.value.filter((beast) => beast.assignmentType === "TRAVELER" && beast.assignedWagonId === wagonId);
}

function wagonTravelerBeastSpaceFor(wagonId: string) {
  return wagonTravelerBeastsFor(wagonId).reduce((total, beast) => total + beast.occupiedSpace, 0);
}

function wagonDraftBeastsFor(wagonId: string) {
  return beasts.value.filter((beast) => beast.assignmentType === "DRAFT" && beast.assignedWagonId === wagonId);
}

function wagonCarreteroFor(wagonId: string) {
  return travelers.value.find((traveler) =>
    traveler.drivingWagonId === wagonId
    && (traveler.activeRoleCodes.includes(carreteroRoleCode) || traveler.activeRoleCode === carreteroRoleCode),
  ) ?? null;
}

function wagonCargoLoadFor(wagonId: string) {
  return cargoSummaryByWagonId.value[wagonId]?.usedCargoUnits ?? 0;
}

function wagonCargoCapacityFor(wagonId: string) {
  return cargoSummaryByWagonId.value[wagonId]?.cargoCapacity ?? 0;
}

function percentageOf(value: number, max: number) {
  if (max <= 0) {
    return 0;
  }

  return Math.max(0, Math.min(100, (value / max) * 100));
}

function formatSpace(value: number) {
  return spaceFormatter.format(value);
}

const meterPalette = [
  "#3b82f6",
  "#10b981",
  "#f59e0b",
  "#ef4444",
  "#8b5cf6",
  "#14b8a6",
  "#f97316",
  "#ec4899",
  "#6366f1",
  "#84cc16",
];

function hashText(value: string) {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) | 0;
  }
  return Math.abs(hash);
}

function colorForKey(key: string) {
  return meterPalette[hashText(key) % meterPalette.length];
}

function buildSegments(
  entries: Array<{ key: string; label: string; amount: number }>,
  total: number,
  remainderLabel: string,
): WagonMeterSegment[] {
  const used = entries.reduce((sum, entry) => sum + Math.max(0, entry.amount), 0);
  const barTotal = total > 0 ? Math.max(total, used) : Math.max(used, 1);
  const segments = entries
    .filter((entry) => entry.amount > 0)
    .map((entry) => ({
      key: entry.key,
      label: entry.label,
      widthPercent: percentageOf(entry.amount, barTotal),
      color: colorForKey(entry.key),
    }));

  const remainder = Math.max(0, barTotal - used);
  if (barTotal > 0 && remainder > 0) {
    segments.push({
      key: `${remainderLabel}:remaining`,
      label: `${remainder} libres`,
      widthPercent: percentageOf(remainder, barTotal),
      color: "#e5e7eb",
    });
  }

  return segments;
}

function draftBandColor(className: string) {
  if (className === "draft-band--danger") {
    return "#fca5a5";
  }
  if (className === "draft-band--warning") {
    return "#fdba74";
  }
  if (className === "draft-band--success") {
    return "#86efac";
  }
  return "#93c5fd";
}

function wagonSpeedFor(wagon: CaravanWagon) {
  const draftBeasts = wagonDraftBeastsFor(wagon.id);
  const carretero = wagonCarreteroFor(wagon.id);
  const requirement = wagonDraftRequirementFor(wagon);
  const requiredStrength = requirement?.minimumStrength ?? 0;
  const totalStrength = wagonDraftStrengthFor(wagon);

  if (draftBeasts.length === 0 || !carretero || (requiredStrength > 0 && totalStrength < requiredStrength)) {
    return 0;
  }

  const slowestBeastSpeed = Math.min(...draftBeasts.map((beast) => beast.speed));
  return Number.isFinite(slowestBeastSpeed) ? Math.max(0, slowestBeastSpeed) : 0;
}

function wagonDraftRequirementFor(wagon: CaravanWagon): WagonDraftRequirement | null {
  const requirement = parseDraftRequirement(wagon.propulsion);
  if (!requirement) {
    return null;
  }

  return requirement;
}

function draftBandsForRequirement(required: number, total: number): WagonDraftBand[] {
  if (required <= 0) {
    return [
      {
        label: "Sin requisito",
        className: "draft-band--neutral",
        startPercent: 0,
        endPercent: 100,
        widthPercent: 100,
      },
    ];
  }

  const thresholds = [required, Math.ceil(required * 1.5), required * 2, Math.max(total, required * 2)];
  const max = Math.max(...thresholds, 1);
  const points = [0, required, Math.ceil(required * 1.5), required * 2, max].filter((value, index, array) => index === 0 || value > array[index - 1]);
  const bands: WagonDraftBand[] = [];
  const classes = ["draft-band--danger", "draft-band--warning", "draft-band--success", "draft-band--boost"];
  const labels = [
    `Necesita ${required}`,
    `Umbral +50% (${Math.ceil(required * 1.5)})`,
    `Umbral +100% (${required * 2})`,
    `Excedente`,
  ];

  for (let index = 1; index < points.length; index += 1) {
    const start = points[index - 1];
    const end = points[index];
    if (end <= start) {
      continue;
    }

    bands.push({
      label: labels[index - 1] ?? `Tramo ${index}`,
      className: classes[index - 1] ?? "draft-band--boost",
      startPercent: (start / max) * 100,
      endPercent: (end / max) * 100,
      widthPercent: ((end - start) / max) * 100,
    });
  }

  return bands;
}

function wagonDraftStrengthFor(wagon: CaravanWagon) {
  return Math.max(0, wagon.draftStrength);
}

function wagonAlertsFor(wagon: CaravanWagon): WagonAlert[] {
  const alerts: WagonAlert[] = [];
  const requirement = wagonDraftRequirementFor(wagon);
  const draftBeasts = wagonDraftBeastsFor(wagon.id);
  const carretero = wagonCarreteroFor(wagon.id);
  const travelersCount = wagonTravelersFor(wagon.id).length;
  const travelerBeastSpace = wagonTravelerBeastSpaceFor(wagon.id);
  const travelerCapacity = wagon.travelerCapacity;
  const cargoLoad = wagonCargoLoadFor(wagon.id);
  const cargoCapacity = wagonCargoCapacityFor(wagon.id);
  const totalStrength = wagonDraftStrengthFor(wagon);
  const requiredStrength = requirement?.minimumStrength ?? 0;

  if (!carretero) {
    alerts.push({
      kind: "warning",
      title: "Sin carretero asignado",
      description: "El carro no tiene un viajero con rol carretero asignado a su tiro.",
    });
  }

  if (draftBeasts.length === 0) {
    alerts.push({
      kind: "warning",
      title: "Tiro vacío",
      description: "No hay bestias asignadas al tiro de este carro.",
    });
  } else if (requirement && totalStrength < requiredStrength) {
    alerts.push({
      kind: "danger",
      title: "Tiro insuficiente",
      description: `La fuerza efectiva del tiro (${totalStrength}) no alcanza la requerida (${requiredStrength}).`,
    });
  }

  if (travelersCount + travelerBeastSpace > travelerCapacity) {
    alerts.push({
      kind: "danger",
      title: "Capacidad de viajeros superada",
      description: `Transporta ${formatSpace(travelersCount + travelerBeastSpace)} plazas ocupadas para una capacidad máxima de ${travelerCapacity}.`,
    });
  }

  if (cargoLoad > cargoCapacity) {
    alerts.push({
      kind: "danger",
      title: "Carga superada",
      description: `Lleva ${cargoLoad} unidades de carga para una capacidad máxima de ${cargoCapacity}.`,
    });
  }

  return alerts;
}

function buildWagonRowSummary(wagon: CaravanWagon): WagonRowSummary {
  const draftRequirement = wagonDraftRequirementFor(wagon);
  const draftBeasts = wagonDraftBeastsFor(wagon.id);
  const travelerCount = wagonTravelersFor(wagon.id).length;
  const travelerBeastSpace = wagonTravelerBeastSpaceFor(wagon.id);
  const travelerCapacity = wagon.travelerCapacity;
  const cargoLoad = wagonCargoLoadFor(wagon.id);
  const cargoCapacity = wagonCargoCapacityFor(wagon.id);
  const draftLargeCount = draftBeasts.filter((beast) => beast.size.toUpperCase() === "G").length;
  const draftMediumCount = draftBeasts.filter((beast) => beast.size.toUpperCase() === "M").length;
  const draftLargeCapacity = draftRequirement?.maxLargeBeasts ?? 0;
  const draftMediumCapacity = draftRequirement?.maxMediumBeasts ?? 0;
  const draftStrength = wagonDraftStrengthFor(wagon);
  const currentHitPoints = wagon.currentHitPoints ?? wagon.hitPoints;
  const hasCarretero = wagonCarreteroFor(wagon.id) !== null;
  const speed = wagonSpeedFor(wagon);
  const totalConsumption = wagon.consumption
    + wagonTravelersFor(wagon.id).reduce((total, traveler) => total + traveler.consumption, 0);

  return {
    wagon,
    currentHitPoints,
    travelerCount,
    travelerBeastSpace,
    travelerCapacity,
    cargoLoad,
    cargoCapacity,
    draftLargeCount,
    draftMediumCount,
    draftLargeCapacity,
    draftMediumCapacity,
    draftRequirement,
    draftStrength,
    draftBands: draftBandsForRequirement(draftRequirement?.minimumStrength ?? 0, draftStrength),
    speed,
    totalConsumption,
    hasCarretero,
    alerts: wagonAlertsFor(wagon),
  };
}

const visibleWagonRows = computed(() => visibleWagons.value.map((wagon) => buildWagonRowSummary(wagon)));
const visibleWagonTotalConsumption = computed(() =>
  visibleWagonRows.value.reduce((total, row) => total + row.totalConsumption, 0),
);
const wagonAlertsSummary = computed<WagonRowSummary | null>(() =>
  wagonAlertsModalTarget.value ? buildWagonRowSummary(wagonAlertsModalTarget.value) : null,
);

const selectedWagonTravelers = computed(() =>
  selectedWagon.value ? travelers.value.filter((traveler) => traveler.wagonId === selectedWagon.value?.id) : []
);

const selectedWagonCarreteros = computed(() =>
  selectedWagon.value
    ? travelers.value.filter((traveler) =>
      traveler.drivingWagonId === selectedWagon.value?.id
      && (traveler.activeRoleCodes.includes(carreteroRoleCode) || traveler.activeRoleCode === carreteroRoleCode),
    )
    : [],
);

const selectedWagonCarretero = computed(() => selectedWagonCarreteros.value[0] ?? null);

const selectedWagonTravelerBeasts = computed(() =>
  selectedWagon.value
    ? beasts.value.filter((beast) => beast.assignmentType === "TRAVELER" && beast.assignedWagonId === selectedWagon.value?.id)
    : []
);

const selectedWagonDraftBeasts = computed(() =>
  selectedWagon.value
    ? beasts.value.filter((beast) => beast.assignmentType === "DRAFT" && beast.assignedWagonId === selectedWagon.value?.id)
    : []
);

const selectedWagonCargoLoad = computed(() =>
  selectedWagonCargo.value.reduce((total, entry) => total + entry.quantity * entry.cargoUnits, 0),
);
const groupedSelectedWagonCargo = computed<GroupedCargoRow[]>(() => {
  const groups = new Map<string, GroupedCargoRow>();

  for (const item of selectedWagonCargo.value) {
    const key = wagonCargoGroupKey(item);
    const existing = groups.get(key);
    const itemLoad = totalCargoUnits(item.quantity, item.cargoUnits);

    if (existing) {
      existing.entries.push(item);
      existing.quantity += item.quantity;
      existing.totalCargoLoad += itemLoad;
      continue;
    }

    groups.set(key, {
      key,
      representative: item,
      entries: [item],
      quantity: item.quantity,
      totalCargoLoad: itemLoad,
    });
  }

  return Array.from(groups.values());
});

const selectedWagonTypeName = computed(() => {
  if (!selectedWagon.value) {
    return "";
  }

  return catalog.value.find((item) => item.code === selectedWagon.value?.wagonTypeCode)?.name
    ?? selectedWagon.value.wagonTypeCode;
});

const selectedWagonCurrentHitPoints = computed(() =>
  selectedWagon.value ? (selectedWagon.value.currentHitPoints ?? selectedWagon.value.hitPoints) : 0,
);

const selectedWagonHitPoints = computed(() => selectedWagon.value?.hitPoints ?? 0);

const selectedWagonHitPointPercentage = computed(() =>
  percentageOf(selectedWagonCurrentHitPoints.value, selectedWagonHitPoints.value),
);

const selectedWagonTotalConsumption = computed(() =>
  selectedWagon.value
    ? selectedWagon.value.consumption
      + selectedWagonTravelers.value.reduce((total, traveler) => total + traveler.consumption, 0)
    : 0,
);

const selectedWagonSpecificCommodityLabel = computed(() => {
  if (!selectedWagon.value || selectedWagon.value.wagonTypeCode !== "carro-de-mercancias-especificas") {
    return null;
  }

  return selectedWagon.value.specificCommodity?.trim() || "Sin mercancía específica asignada";
});

const selectedWagonCargoRemaining = computed(() =>
  selectedWagon.value ? Math.max(0, selectedWagon.value.cargoCapacity - selectedWagonCargoLoad.value) : 0,
);

const cargoCatalogByCode = computed(() =>
  Object.fromEntries(wagonCargoCatalog.value.map((item) => [item.code, item] as const)),
);

const selectedCargoCatalogItem = computed(() =>
  cargoCatalogCode.value ? cargoCatalogByCode.value[cargoCatalogCode.value] ?? null : null,
);

const cargoQuantityValue = computed(() => parsePositiveInteger(cargoQuantity.value, selectedCargoCatalogItem.value?.defaultQuantity ?? 1));
const cargoUnitsValue = computed(() => parsePositiveInteger(cargoUnits.value, selectedCargoCatalogItem.value?.defaultCargoUnits ?? 1));
const cargoQuantityMax = computed(() => maxQuantityForCargo(cargoUnitsValue.value));
const selectedCargoRequiredMetadataKeys = computed(
  () => selectedCargoCatalogItem.value?.requiredMetadataKeys ?? [],
);
const cargoRequiresOrigin = computed(() => selectedCargoRequiredMetadataKeys.value.includes("origin"));
const cargoRequiresSpecificCommodity = computed(() =>
  selectedCargoRequiredMetadataKeys.value.includes("specificCommodity"),
);
const cargoRequiresDeity = computed(() => selectedCargoRequiredMetadataKeys.value.includes("deity"));
const cargoSpecificCommodityLocked = computed(
  () => selectedWagon.value?.wagonTypeCode === "carro-de-mercancias-especificas",
);
const cargoSpecificCommodityIsCustom = computed(() => {
  if (!cargoSpecificCommodityLocked.value) {
    return false;
  }

  const wagonSpecificCommodity = selectedWagon.value?.specificCommodity?.trim() ?? "";
  if (!wagonSpecificCommodity) {
    return false;
  }

  return !specificCommodityOptions.some(
    (option) => option.toLowerCase() === wagonSpecificCommodity.toLowerCase(),
  );
});
const cargoSpecificCommodityValue = computed(() => {
  if (cargoSpecificCommodityOption.value === "custom") {
    return cargoSpecificCommodityCustom.value.trim() || null;
  }

  return cargoSpecificCommodityOption.value || null;
});
function syncCargoSpecificCommoditySelection() {
  if (!cargoModalOpen.value || cargoModalMode.value !== "catalog") {
    return;
  }

  const item = selectedCargoCatalogItem.value;
  if (!item || !item.requiredMetadataKeys.includes("specificCommodity")) {
    cargoSpecificCommodityOption.value = "";
    cargoSpecificCommodityCustom.value = "";
    return;
  }

  if (cargoSpecificCommodityLocked.value) {
    const wagonSpecificCommodity = selectedWagon.value?.specificCommodity?.trim() ?? "";
    cargoSpecificCommodityOption.value = wagonSpecificCommodity;
    cargoSpecificCommodityCustom.value = "";
    return;
  }

  if (cargoSpecificCommodityValue.value) {
    return;
  }

  const wagonSpecificCommodity = selectedWagon.value?.specificCommodity?.trim();
  if (!wagonSpecificCommodity) {
    return;
  }

  const matchingOption = specificCommodityOptions.find(
    (option) => option.toLowerCase() === wagonSpecificCommodity.toLowerCase(),
  );

  cargoSpecificCommodityOption.value = matchingOption ?? "custom";
  cargoSpecificCommodityCustom.value = matchingOption ? "" : wagonSpecificCommodity;
}
const addableCargoCatalogItems = computed(() =>
  wagonCargoCatalog.value.filter((item) =>
    selectedWagon.value
      ? isWagonCompatibleWithCatalogItem(selectedWagon.value, item, selectedWagon.value.specificCommodity)
      : false
  ),
);

const unassignedTravelers = computed(() => travelers.value.filter((traveler) => !traveler.wagonId));

const unassignedBeasts = computed(() => beasts.value.filter((beast) => beast.assignmentType === "NONE"));

const selectedWagonOccupancy = computed(() =>
  selectedWagonTravelers.value.length
  + selectedWagonTravelerBeasts.value.reduce((total, beast) => total + beast.occupiedSpace, 0),
);
const selectedWagonCapacityRemaining = computed(() =>
  selectedWagon.value ? Math.max(0, selectedWagon.value.travelerCapacity - selectedWagonOccupancy.value) : 0
);
const selectedWagonCrewSegments = computed<WagonMeterSegment[]>(() => {
  const capacity = selectedWagon.value?.travelerCapacity ?? 0;
  return buildSegments(
    [
      {
        key: "crew-travelers",
        label: `${selectedWagonTravelers.value.length} viajeros`,
        amount: selectedWagonTravelers.value.length,
      },
      {
        key: "crew-beasts",
        label: `${formatSpace(selectedWagonTravelerBeasts.value.reduce((total, beast) => total + beast.occupiedSpace, 0))} espacio bestias`,
        amount: selectedWagonTravelerBeasts.value.reduce((total, beast) => total + beast.occupiedSpace, 0),
      },
    ],
    capacity,
    "crew",
  );
});
const selectedWagonCargoSegments = computed<WagonMeterSegment[]>(() =>
  buildSegments(
    groupedSelectedWagonCargo.value.map((entry) => ({
      key: entry.key,
      label: `${entry.representative.displayName} · ${entry.totalCargoLoad}`,
      amount: entry.totalCargoLoad,
    })),
    selectedWagon.value?.cargoCapacity ?? 0,
    "cargo",
  ),
);
const selectedWagonDraftRequirement = computed(() =>
  selectedWagon.value ? parseDraftRequirement(selectedWagon.value.propulsion) : null,
);
const selectedWagonDraftGroups = computed(() => (selectedWagon.value ? groupedDraftBeasts(selectedWagon.value) : []));
const selectedWagonDraftOccupancySegments = computed<WagonMeterSegment[]>(() => {
  const requirement = selectedWagonDraftRequirement.value;
  const totalSlots = requirement
    ? requirement.maxLargeBeasts + requirement.maxMediumBeasts
    : selectedWagonDraftGroups.value.reduce((sum, group) => sum + group.count, 0);

  return buildSegments(
    selectedWagonDraftGroups.value.map((group) => ({
      key: group.key,
      label: `${group.name} · ${group.count}`,
      amount: group.count,
    })),
    Math.max(1, totalSlots),
    "draft-occupancy",
  );
});
const availableTravelersForSelectedWagon = computed(() =>
  selectedWagonCapacityRemaining.value >= 1 ? unassignedTravelers.value : []
);
const availableTravelerBeastsForSelectedWagon = computed(() =>
  selectedWagon.value
    ? unassignedBeasts.value.filter((beast) => beast.occupiedSpace <= selectedWagonCapacityRemaining.value)
    : []
);
const availableDraftBeastsForSelectedWagon = computed(() => {
  const wagon = selectedWagon.value;
  if (!wagon || !selectedWagonDraftRequirement.value) {
    return [];
  }

  return unassignedBeasts.value.filter((beast) => canAssignBeastToDraft(wagon, beast));
});

const assignmentModalTitle = computed(() => {
  if (assignmentModalMode.value === "traveler") {
    return "Asignar viajeros sin carro";
  }
  if (assignmentModalMode.value === "beast-traveler") {
    return "Asignar bestias como viajeras";
  }
  return "Asignar bestias al tiro";
});

const assignmentModalHint = computed(() => {
  if (!selectedWagon.value) {
    return "";
  }

  if (assignmentModalMode.value === "traveler") {
    return selectedWagonCapacityRemaining.value < 1
      ? "Este carro ya no tiene plazas libres para viajeros."
      : `Se mostrarán solo viajeros sin asignar. Quedan ${formatSpace(selectedWagonCapacityRemaining.value)} plazas libres en este carro.`;
  }

  if (assignmentModalMode.value === "beast-traveler") {
    return selectedWagonCapacityRemaining.value < 0.5
      ? "Este carro ya no tiene plazas libres para bestias viajeras."
      : `Se mostrarán solo bestias sin asignar. Quedan ${formatSpace(selectedWagonCapacityRemaining.value)} plazas libres en este carro.`;
  }

  return availableDraftBeastsForSelectedWagon.value.length === 0
    ? "No hay bestias válidas para el tiro de este carro."
    : "Se mostrarán solo bestias sin asignar que cumplan el requisito de tiro.";
});

const visibleImprovementItems = computed(() =>
  improvementCatalog.value
    .filter((item) => matchesSearch(item.name, improvementSearch.value))
    .sort((left, right) => Number(right.available) - Number(left.available))
);

watch(selectedCargoCatalogItem, (item) => {
  if (!cargoModalOpen.value || cargoModalMode.value !== "catalog" || !item) {
    return;
  }

  cargoUnits.value = String(item.defaultCargoUnits ?? 1);
  cargoQuantity.value = String(item.defaultQuantity ?? 1);
  syncCargoSpecificCommoditySelection();
});

watch(cargoSpecificCommodityOption, (option) => {
  if (option !== "custom") {
    cargoSpecificCommodityCustom.value = "";
  }
});

watch([addableCargoCatalogItems, cargoModalOpen, cargoModalMode], () => {
  syncCargoFormSelection();
  syncCargoSpecificCommoditySelection();
});

async function refresh() {
  const previousAction = pendingAction.value;
  const trackRefresh = previousAction === null;

  if (trackRefresh) {
    pendingAction.value = "refresh";
  }

  loading.value = true;
  error.value = null;

  try {
    const [caravanList, activeResponse] = await Promise.all([listCaravans(), getActiveCaravan()]);
    caravans.value = caravanList;
    activeCaravan.value = activeResponse.caravan;

    if (activeResponse.caravan) {
      const [catalogResponse, wagonsResponse, travelerResponse, beastResponse, cargoCatalogResponse, cargoSummaryResponse] = await Promise.all([
        listWagonCatalog(activeResponse.caravan.id),
        listCaravanWagons(activeResponse.caravan.id),
        listCaravanTravelers(activeResponse.caravan.id),
        listCaravanBeasts(activeResponse.caravan.id),
        listCargoCatalog(activeResponse.caravan.id),
        listCaravanCargoSummary(activeResponse.caravan.id),
      ]);

      catalog.value = catalogResponse;
      wagons.value = wagonsResponse;
      travelers.value = travelerResponse;
      beasts.value = beastResponse;
      wagonCargoCatalog.value = cargoCatalogResponse;
      cargoSummary.value = cargoSummaryResponse;

      if (
        catalogResponse.length > 0
        && !catalogResponse.some((item) => item.code === selectedCatalogCode.value)
      ) {
        selectedCatalogCode.value = catalogResponse[0].code;
      }

      if (selectedWagon.value) {
        const refreshedWagon = wagonsResponse.find((wagon) => wagon.id === selectedWagon.value?.id);
        if (refreshedWagon) {
          selectedWagon.value = await getCaravanWagon(activeResponse.caravan.id, refreshedWagon.id);
          wagonNameDraft.value = selectedWagon.value.name;
          await loadSelectedWagonCargo(activeResponse.caravan.id, refreshedWagon.id);
        } else {
          closeModal();
        }
      }
    } else {
      catalog.value = [];
      wagons.value = [];
      travelers.value = [];
      beasts.value = [];
      wagonCargoCatalog.value = [];
      cargoSummary.value = [];
      selectedWagonCargo.value = [];
      selectedCatalogCode.value = null;
      closeModal();
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load wagons";
  } finally {
    loading.value = false;
    pendingAction.value = trackRefresh ? null : previousAction;
  }
}

async function handleAddSelected() {
  if (!activeCaravan.value || !selectedCatalogItem.value) {
    return;
  }

  if (selectedCatalogRequiresSpecificCommodity.value && !addWagonSpecificCommodityValue.value) {
    error.value = "Debes seleccionar de qué mercancía específica es este carro";
    return;
  }

  submitting.value = true;
  pendingAction.value = "add-wagon";
  error.value = null;

    try {
    const wagonName = addWagonName.value.trim() || selectedCatalogItem.value.name;
    await addCaravanWagon(activeCaravan.value.id, {
      wagonTypeCode: selectedCatalogItem.value.code,
      displayName: addWagonName.value.trim() || null,
      specificCommodity: addWagonSpecificCommodityValue.value,
    });
    await refresh();
    addModalOpen.value = false;
    selectedCatalogCode.value = selectedCatalogItem.value.code;
    if (caravanWagonLimitState.value?.kind === "danger") {
      showToast(`Carro añadido: ${wagonName}. ${caravanWagonLimitState.value.message}`, "warning");
    } else {
      showToast(`Carro añadido: ${wagonName}.`);
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to add wagon";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function openAddModal() {
  selectedCatalogCode.value = null;
  addWagonName.value = "";
  addWagonSpecificCommodityOption.value = "";
  addWagonSpecificCommodityCustom.value = "";
  addModalOpen.value = true;
}

function selectCatalogItem(code: string) {
  selectedCatalogCode.value = code;
  addWagonName.value = selectedCatalogItem.value?.name ?? "";
  if (selectedCatalogItem.value?.code !== "carro-de-mercancias-especificas") {
    addWagonSpecificCommodityOption.value = "";
    addWagonSpecificCommodityCustom.value = "";
  }
}

async function openWagonDetails(wagon: CaravanWagon) {
  if (!activeCaravan.value) {
    return;
  }

  try {
    selectedWagon.value = await getCaravanWagon(activeCaravan.value.id, wagon.id);
    wagonNameDraft.value = selectedWagon.value.name;
    wagonNameEditorOpen.value = false;
    closeHealthModal();
    await loadSelectedWagonCargo(activeCaravan.value.id, wagon.id);
    improvementModalOpen.value = false;
    selectedImprovementCode.value = null;
    improvementSearch.value = "";
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load wagon details";
  }
}

function openWagonAlerts(wagon: CaravanWagon) {
  wagonAlertsModalTarget.value = wagon;
  wagonAlertsModalOpen.value = true;
}

function closeWagonAlertsModal() {
  wagonAlertsModalOpen.value = false;
  wagonAlertsModalTarget.value = null;
}

function openAssignmentModal(mode: "traveler" | "beast-traveler" | "beast-draft") {
  if (!selectedWagon.value) {
    return;
  }

  if (
    (mode === "traveler" && availableTravelersForSelectedWagon.value.length === 0)
    || (mode === "beast-traveler" && availableTravelerBeastsForSelectedWagon.value.length === 0)
    || (mode === "beast-draft" && availableDraftBeastsForSelectedWagon.value.length === 0)
  ) {
    return;
  }

  assignmentModalMode.value = mode;
  assignmentModalError.value = null;
  assignmentModalSuccess.value = null;
  assignmentModalOpen.value = true;
}

function closeAssignmentModal() {
  assignmentModalOpen.value = false;
  assignmentModalError.value = null;
  assignmentModalSuccess.value = null;
  if (assignmentModalSuccessTimer !== null) {
    window.clearTimeout(assignmentModalSuccessTimer);
    assignmentModalSuccessTimer = null;
  }
}

function showAssignmentSuccess(message: string) {
  assignmentModalSuccessId += 1;
  assignmentModalSuccess.value = { id: assignmentModalSuccessId, message };

  if (assignmentModalSuccessTimer !== null) {
    window.clearTimeout(assignmentModalSuccessTimer);
  }

  assignmentModalSuccessTimer = window.setTimeout(() => {
    assignmentModalSuccess.value = null;
    assignmentModalSuccessTimer = null;
  }, 2200);
}

async function handleDeleteSelectedWagon() {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  const confirmed = window.confirm(
    `¿Seguro que quieres eliminar "${selectedWagon.value.name}"? Esta acción no se puede deshacer.`
  );

  if (!confirmed) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `delete-wagon:${selectedWagon.value.id}`;
  error.value = null;

  try {
    await deleteCaravanWagon(activeCaravan.value.id, selectedWagon.value.id);
    selectedWagon.value = null;
    await refresh();
    showToast("Carro eliminado.");
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to delete wagon";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function closeModal() {
  selectedWagon.value = null;
  wagonNameDraft.value = "";
  wagonNameEditorOpen.value = false;
  selectedWagonCargo.value = [];
  closeWagonAlertsModal();
  closeCargoModal();
  closeHealthModal();
  closeAssignmentModal();
  improvementModalOpen.value = false;
  selectedImprovementCode.value = null;
  improvementCatalog.value = [];
  improvementsExpanded.value = false;
  improvementDeleteMode.value = false;
  draftPanelExpanded.value = true;
}

function closeAddModal() {
  addModalOpen.value = false;
  selectedCatalogCode.value = null;
  addWagonName.value = "";
  addWagonSpecificCommodityOption.value = "";
  addWagonSpecificCommodityCustom.value = "";
}

async function handleRenameSelectedWagon() {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `rename-wagon:${selectedWagon.value.id}`;
  error.value = null;

  try {
    const updatedWagon = await updateCaravanWagon(activeCaravan.value.id, selectedWagon.value.id, {
      displayName: wagonNameDraft.value.trim() || null,
      });
      selectedWagon.value = updatedWagon;
      wagonNameDraft.value = updatedWagon.name;
      wagonNameEditorOpen.value = false;
      replaceWagonInList(updatedWagon);
    showToast(`Nombre actualizado: ${updatedWagon.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to rename wagon";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function openHealthModal(mode: "damage" | "repair") {
  if (!selectedWagon.value) {
    return;
  }

  healthModalMode.value = mode;
  healthModalAmount.value = "";
  healthModalIgnoreHardness.value = false;
  healthModalError.value = null;
  healthModalOpen.value = true;
}

function closeHealthModal() {
  healthModalOpen.value = false;
  healthModalError.value = null;
  healthModalAmount.value = "";
  healthModalIgnoreHardness.value = false;
}

async function handleApplyHealthChange() {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  const amount = Number(healthModalAmount.value);
  if (!Number.isInteger(amount) || amount < 1) {
    healthModalError.value = healthModalMode.value === "damage"
      ? "Indica cuánto daño va a recibir el carro"
      : "Indica cuánta vida vas a reparar";
    return;
  }

  submitting.value = true;
  pendingAction.value = `${healthModalMode.value}-wagon:${selectedWagon.value.id}`;
  healthModalError.value = null;

  try {
    const updatedWagon = healthModalMode.value === "damage"
      ? await damageCaravanWagon(activeCaravan.value.id, selectedWagon.value.id, {
        damageAmount: amount,
        ignoreHardness: healthModalIgnoreHardness.value,
      })
      : await repairCaravanWagon(activeCaravan.value.id, selectedWagon.value.id, {
        repairAmount: amount,
      });

    selectedWagon.value = updatedWagon;
    selectedWagonCargo.value = await listCaravanCargo(activeCaravan.value.id, { wagonId: updatedWagon.id });
    wagonNameDraft.value = updatedWagon.name;
    replaceWagonInList(updatedWagon);
    closeHealthModal();
    showToast(
      healthModalMode.value === "damage"
        ? `Carro dañado. Vida actual: ${updatedWagon.currentHitPoints ?? updatedWagon.hitPoints}/${updatedWagon.hitPoints}.`
        : `Carro reparado. Vida actual: ${updatedWagon.currentHitPoints ?? updatedWagon.hitPoints}/${updatedWagon.hitPoints}.`,
    );
  } catch (cause) {
    healthModalError.value = cause instanceof Error ? cause.message : "Failed to update wagon health";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function closeCargoModal() {
  cargoModalOpen.value = false;
  cargoModalError.value = null;
}

async function loadSelectedWagonCargo(caravanId: string, wagonId: string) {
  selectedWagonCargo.value = await listCaravanCargo(caravanId, { wagonId });
}

function wagonCargoGroupKey(item: CaravanCargo) {
  return [
    item.wagonId ?? "",
    item.sourceType,
    item.catalogCode ?? "",
    item.displayName,
    item.category,
    item.origin ?? "",
    item.specificCommodity ?? "",
    item.deity ?? "",
    item.notes ?? "",
    item.priceExpression ?? "",
  ].join("\u0001");
}

function parsePositiveInteger(value: string, fallback: number) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 1) {
    return fallback;
  }

  return Math.floor(parsed);
}

function totalCargoUnits(quantity: number, cargoUnits: number) {
  return quantity * cargoUnits;
}

function selectedWagonRemainingCargoUnits(wagon: CaravanWagon | null) {
  if (!wagon) {
    return 0;
  }

  return Math.max(0, wagon.cargoCapacity - selectedWagonCargoLoad.value);
}

function maxQuantityForCargo(cargoUnits: number) {
  if (!selectedWagon.value || cargoUnits < 1) {
    return 1;
  }

  return Math.max(1, Math.floor(selectedWagonRemainingCargoUnits(selectedWagon.value) / cargoUnits));
}

function isWagonCompatibleWithCatalogItem(
  wagon: CaravanWagon,
  item: CargoCatalogItem | null,
  specificCommodity?: string | null,
) {
  if (!item) {
    return false;
  }

  if (item.allowedWagonCodes.length > 0) {
    return item.allowedWagonCodes.includes(wagon.wagonTypeCode);
  }

  if (item.requiredMetadataKeys.includes("specificCommodity")) {
    if (wagon.wagonTypeCode === "carro-de-mercancias-especificas") {
      const normalizedSpecificCommodity = specificCommodity?.trim() ?? "";
      return normalizedSpecificCommodity.length > 0;
    }

    return wagon.wagonTypeCode !== "carro-de-suministros";
  }

  return wagon.wagonTypeCode !== "carro-de-suministros"
    && wagon.wagonTypeCode !== "carro-de-mercancias-especificas";
}

function isWagonCompatibleForCustomCargo(wagon: CaravanWagon) {
  return wagon.wagonTypeCode !== "carro-de-suministros"
    && wagon.wagonTypeCode !== "carro-de-mercancias-especificas";
}

function openCargoModal(mode: "catalog" | "custom") {
  if (!selectedWagon.value || !activeCaravan.value) {
    return;
  }

  if (mode === "custom" && selectedWagon.value.wagonTypeCode === "carro-de-mercancias-especificas") {
    cargoModalError.value = "Este carro solo admite mercancía de catálogo";
    return;
  }

  cargoModalMode.value = mode;
  cargoModalError.value = null;
  resetCargoForm();
  cargoModalOpen.value = true;
}

function resetCargoForm() {
  const wagon = selectedWagon.value;
  if (!wagon) {
    cargoCatalogCode.value = "";
    cargoQuantity.value = "1";
    cargoUnits.value = "1";
    cargoDisplayName.value = "";
    cargoCategory.value = "";
    cargoOrigin.value = "";
    cargoSpecificCommodityOption.value = "";
    cargoSpecificCommodityCustom.value = "";
    cargoDeity.value = "";
    cargoNotes.value = "";
    return;
  }

  cargoQuantity.value = "1";
  cargoOrigin.value = "";
  cargoSpecificCommodityOption.value = "";
  cargoSpecificCommodityCustom.value = "";
  cargoDeity.value = "";
  cargoNotes.value = "";

  if (cargoModalMode.value === "custom") {
    cargoCatalogCode.value = "";
    cargoUnits.value = "1";
    cargoDisplayName.value = "";
    cargoCategory.value = "";
    return;
  }

  const compatibleCatalogItems = wagonCargoCatalog.value.filter((item) =>
    isWagonCompatibleWithCatalogItem(wagon, item, wagon.specificCommodity),
  );
  cargoCatalogCode.value = compatibleCatalogItems[0]?.code ?? "";
  cargoUnits.value = String(compatibleCatalogItems[0]?.defaultCargoUnits ?? 1);
  cargoDisplayName.value = "";
  cargoCategory.value = "";
  syncCargoSpecificCommoditySelection();
}

function syncCargoFormSelection() {
  if (!cargoModalOpen.value || cargoModalMode.value !== "catalog") {
    return;
  }

  const available = addableCargoCatalogItems.value;
  if (available.length === 0) {
    cargoCatalogCode.value = "";
    return;
  }

  if (!available.some((item) => item.code === cargoCatalogCode.value)) {
    cargoCatalogCode.value = available[0].code;
  }
  syncCargoSpecificCommoditySelection();
}

async function handleAddCatalogCargoToSelectedWagon() {
  if (!activeCaravan.value || !selectedWagon.value || !selectedCargoCatalogItem.value) {
    return;
  }

  const quantity = cargoQuantityValue.value;
  const cargoUnitsPerEntry = cargoUnitsValue.value;

  if (cargoRequiresOrigin.value && !cargoOrigin.value.trim()) {
    cargoModalError.value = "El origen es obligatorio para esta mercancía";
    return;
  }
  if (cargoRequiresSpecificCommodity.value && !cargoSpecificCommodityValue.value) {
    cargoModalError.value = "Debes seleccionar qué mercancía específica es";
    return;
  }
  if (cargoRequiresDeity.value && !cargoDeity.value.trim()) {
    cargoModalError.value = "La deidad es obligatoria para esta mercancía";
    return;
  }

  if (totalCargoUnits(quantity, cargoUnitsPerEntry) > selectedWagonRemainingCargoUnits(selectedWagon.value)) {
    cargoModalError.value = "La cantidad supera la capacidad disponible del carro";
    return;
  }

  submitting.value = true;
  pendingAction.value = "add-cargo";
  cargoModalError.value = null;

  try {
    await addCargoFromCatalog(activeCaravan.value.id, {
      catalogCode: selectedCargoCatalogItem.value.code,
      quantity,
      cargoUnits: cargoUnitsPerEntry,
      wagonId: selectedWagon.value.id,
      origin: cargoOrigin.value.trim() || null,
      specificCommodity: cargoSpecificCommodityValue.value,
      deity: cargoDeity.value.trim() || null,
      notes: cargoNotes.value.trim() || null,
    });
    closeCargoModal();
    await refresh();
    showToast(`Mercancía añadida a ${selectedWagon.value.name}.`);
  } catch (cause) {
    cargoModalError.value = cause instanceof Error ? cause.message : "Failed to add cargo";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleAddCustomCargoToSelectedWagon() {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  const displayName = cargoDisplayName.value.trim();
  const category = cargoCategory.value.trim();
  const quantity = cargoQuantityValue.value;
  const cargoUnitsPerEntry = cargoUnitsValue.value;

  if (!displayName) {
    cargoModalError.value = "El nombre es obligatorio";
    return;
  }
  if (!category) {
    cargoModalError.value = "La categoría es obligatoria";
    return;
  }
  if (!isWagonCompatibleForCustomCargo(selectedWagon.value)) {
    cargoModalError.value = "Solo se muestran carros válidos para esta carga";
    return;
  }
  if (totalCargoUnits(quantity, cargoUnitsPerEntry) > selectedWagonRemainingCargoUnits(selectedWagon.value)) {
    cargoModalError.value = "La cantidad supera la capacidad disponible del carro";
    return;
  }

  submitting.value = true;
  pendingAction.value = "add-custom-cargo";
  cargoModalError.value = null;

  try {
    await addCustomCargo(activeCaravan.value.id, {
      displayName,
      category,
      quantity,
      cargoUnits: cargoUnitsPerEntry,
      wagonId: selectedWagon.value.id,
      origin: cargoOrigin.value.trim() || null,
      specificCommodity: cargoSpecificCommodityValue.value,
      deity: cargoDeity.value.trim() || null,
      notes: cargoNotes.value.trim() || null,
    });
    closeCargoModal();
    await refresh();
    showToast(`Mercancía añadida a ${selectedWagon.value.name}.`);
  } catch (cause) {
    cargoModalError.value = cause instanceof Error ? cause.message : "Failed to add cargo";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleDeleteSelectedWagonCargo(entry: CaravanCargo) {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  const confirmed = window.confirm(
    `¿Seguro que quieres eliminar "${entry.displayName}" de este carro? Esta acción no se puede deshacer.`
  );

  if (!confirmed) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `delete-cargo:${entry.id}`;
  error.value = null;

  try {
    await deleteCaravanCargo(activeCaravan.value.id, entry.id);
    await loadSelectedWagonCargo(activeCaravan.value.id, selectedWagon.value.id);
    await refresh();
    showToast(`Mercancía eliminada: ${entry.displayName}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to delete cargo";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function replaceWagonInList(updatedWagon: CaravanWagon) {
  const index = wagons.value.findIndex((wagon) => wagon.id === updatedWagon.id);
  if (index >= 0) {
    wagons.value.splice(index, 1, updatedWagon);
  }
}

async function assignTravelerToSelectedWagon(traveler: CaravanTraveler) {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  if (selectedWagonCapacityRemaining.value < 1) {
    assignmentModalError.value = "Este carro ya no tiene plazas libres para viajeros.";
    return;
  }

  submitting.value = true;
  pendingAction.value = `assign-traveler:${traveler.id}`;
  assignmentModalError.value = null;
  error.value = null;

  try {
    await updateCaravanTravelerWagon(activeCaravan.value.id, traveler.id, {
      wagonId: selectedWagon.value.id,
    });
    showAssignmentSuccess(`Viajero añadido: ${traveler.fullName}.`);
    showToast(`Viajero añadido al carro: ${traveler.fullName}.`);
    await refresh();
  } catch (cause) {
    assignmentModalError.value = cause instanceof Error ? cause.message : "Failed to assign traveler";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function canAssignBeastToDraft(wagon: CaravanWagon, beast: CaravanBeast): boolean {
  const requirement = parseDraftRequirement(wagon.propulsion);
  if (!requirement || beast.assignmentType !== "NONE") {
    return false;
  }

  const size = beast.size.toUpperCase();
  if (size !== "G" && size !== "M") {
    return false;
  }

  const occupancy = draftOccupancyForWagon(wagon);
  const additionalLarge = size === "G" ? 1 : 0;
  const additionalMedium = size === "M" ? 1 : 0;
  const totalLarge = occupancy.largeCount + additionalLarge;
  const totalMediumSlots = occupancy.mediumCount + additionalMedium
    + (totalLarge * draftMediumSlotsPerLargeBeast);

  return totalLarge <= requirement.maxLargeBeasts && totalMediumSlots <= requirement.maxMediumBeasts;
}

async function assignBeastToSelectedWagon(beast: CaravanBeast, assignmentType: "TRAVELER" | "DRAFT") {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  if (assignmentType === "TRAVELER" && selectedWagonCapacityRemaining.value < beast.occupiedSpace) {
    assignmentModalError.value = "Este carro ya no tiene plazas libres para bestias viajeras.";
    return;
  }

  if (assignmentType === "DRAFT" && !canAssignBeastToDraft(selectedWagon.value, beast)) {
    assignmentModalError.value = "Solo las bestias medianas o grandes pueden ir al tiro.";
    return;
  }

  submitting.value = true;
  pendingAction.value = `assign-beast-${assignmentType.toLowerCase()}:${beast.id}`;
  assignmentModalError.value = null;
  error.value = null;

  try {
    await updateCaravanBeastAssignment(activeCaravan.value.id, beast.id, {
      assignmentType,
      wagonId: selectedWagon.value.id,
    });
    showAssignmentSuccess(
      assignmentType === "DRAFT"
        ? `Bestia al tiro añadida: ${beast.name}.`
        : `Bestia viajera añadida: ${beast.name}.`,
    );
    showToast(
      assignmentType === "DRAFT"
        ? `Bestia añadida al tiro: ${beast.name}.`
        : `Bestia añadida como viajera: ${beast.name}.`,
    );
    await refresh();
  } catch (cause) {
    assignmentModalError.value = cause instanceof Error ? cause.message : "Failed to assign beast";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function clearTravelerAssignment(traveler: CaravanTraveler) {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `clear-traveler:${traveler.id}`;
  error.value = null;

  try {
    await updateCaravanTravelerWagon(activeCaravan.value.id, traveler.id, { wagonId: null });
    showAssignmentSuccess(`Viajero quitado: ${traveler.fullName}.`);
    showToast(`Viajero quitado del carro: ${traveler.fullName}.`);
    await refresh();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to clear traveler assignment";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function clearBeastAssignment(beast: CaravanBeast) {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `clear-beast:${beast.id}`;
  error.value = null;

  try {
    await clearCaravanBeastAssignment(activeCaravan.value.id, beast.id);
    showAssignmentSuccess(`Bestia quitada: ${beast.name}.`);
    showToast(`Bestia quitada del carro: ${beast.name}.`);
    await refresh();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to clear beast assignment";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function openImprovementModal() {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  submitting.value = true;
  pendingAction.value = "open-improvements";
  error.value = null;

  try {
    improvementCatalog.value = await listWagonImprovementCatalog(activeCaravan.value.id, selectedWagon.value.id);
    selectedImprovementCode.value = improvementCatalog.value.find((item) => item.available)?.code
      ?? improvementCatalog.value[0]?.code
      ?? null;
    improvementSearch.value = "";
    improvementModalOpen.value = true;
    improvementsExpanded.value = true;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load improvement catalog";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function toggleImprovementsExpanded() {
  improvementsExpanded.value = !improvementsExpanded.value;
  if (!improvementsExpanded.value) {
    improvementDeleteMode.value = false;
  }
}

function toggleImprovementDeleteMode() {
  improvementDeleteMode.value = !improvementDeleteMode.value;
  if (improvementDeleteMode.value) {
    improvementsExpanded.value = true;
  }
}

async function handleAddSelectedImprovement() {
  if (!activeCaravan.value || !selectedWagon.value || !selectedImprovementItem.value) {
    return;
  }

  submitting.value = true;
  pendingAction.value = "add-improvement";
  error.value = null;

  try {
    const updatedWagon = await addCaravanWagonImprovement(activeCaravan.value.id, selectedWagon.value.id, {
      improvementTypeCode: selectedImprovementItem.value.code,
    });
    selectedWagon.value = updatedWagon;
    replaceWagonInList(updatedWagon);
    improvementCatalog.value = await listWagonImprovementCatalog(activeCaravan.value.id, selectedWagon.value.id);
    selectedImprovementCode.value = selectedImprovementItem.value.code;
    showToast(`Mejora añadida: ${selectedImprovementItem.value.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to add improvement";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleRemoveImprovement(improvement: CaravanWagonImprovement) {
  if (!activeCaravan.value || !selectedWagon.value) {
    return;
  }

  const confirmed = window.confirm(
    `¿Seguro que quieres eliminar "${improvement.name}" del carro "${selectedWagon.value.name}"?`
  );

  if (!confirmed) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `delete-improvement:${improvement.id}`;
  error.value = null;

  try {
    const updatedWagon = await deleteCaravanWagonImprovement(
      activeCaravan.value.id,
      selectedWagon.value.id,
      improvement.id,
    );
    selectedWagon.value = updatedWagon;
    replaceWagonInList(updatedWagon);
    improvementCatalog.value = await listWagonImprovementCatalog(activeCaravan.value.id, selectedWagon.value.id);
    selectedImprovementCode.value = selectedImprovementItem.value?.code ?? null;
    showToast(`Mejora eliminada: ${improvement.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to delete improvement";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function categoryFilterKey(category: string) {
  const normalized = category.trim().toLowerCase();
  if (normalized.includes("viaj")) {
    return "viajeros";
  }
  if (normalized.includes("carg")) {
    return "mercancias";
  }
  return "especiales";
}

function matchesTypeFilter(category: string, filter: "all" | "viajeros" | "mercancias" | "especiales") {
  return filter === "all" || categoryFilterKey(category) === filter;
}

function matchesSearch(name: string, search: string) {
  return name.toLowerCase().includes(search.trim().toLowerCase());
}

function wagonCountForType(code: string) {
  return wagons.value.filter((wagon) => wagon.wagonTypeCode === code).length;
}

function maxAllowedFor(item: WagonCatalogItem) {
  if (!activeCaravan.value) {
    return 0;
  }

  const caravanCapacity = totalCapacity.value;

  if (item.limitKind === "UNLIMITED") {
    return Number.POSITIVE_INFINITY;
  }

  if (item.limitKind === "FIXED") {
    return item.limitFixedMax ?? 0;
  }

  if (item.limitKind === "RATIO_OF_CARAVAN_CAPACITY") {
    const denominator = item.limitRatioDenominator ?? 1;
    return Math.max(1, Math.floor(caravanCapacity / denominator));
  }

  return 0;
}

function isLimitExceeded(item: WagonCatalogItem) {
  const maxAllowed = maxAllowedFor(item);
  if (!Number.isFinite(maxAllowed)) {
    return false;
  }
  return wagonCountForType(item.code) >= maxAllowed;
}

function limitMessage(item: WagonCatalogItem) {
  const current = wagonCountForType(item.code);
  const maxAllowed = maxAllowedFor(item);

  if (!Number.isFinite(maxAllowed)) {
    return "Sin límite";
  }

  if (current >= maxAllowed) {
    return `Límite alcanzado (${current}/${maxAllowed})`;
  }

  return `${current}/${maxAllowed}`;
}

function parseDraftRequirement(propulsion: string): DraftRequirement | null {
  const match = propulsion.match(/(?:(\d+)\s+criatura[s]?\s+grande[s]?\s*\/\s*)?(\d+)\s+mediana[s]?\s*\(\+(\d+)\s+fuerza\)/i);
  if (!match) {
    return null;
  }

  return {
    maxLargeBeasts: Number(match[1] ?? 0),
    maxMediumBeasts: Number(match[2]),
    minimumStrength: Number(match[3]),
  };
}

function draftOccupancyForWagon(wagon: CaravanWagon) {
  const largeBeasts = wagon.draftBeasts.filter((beast) => beast.size.toUpperCase() === "G");
  const mediumBeasts = wagon.draftBeasts.filter((beast) => beast.size.toUpperCase() === "M");

  return {
    largeBeasts,
    mediumBeasts,
    largeCount: largeBeasts.length,
    mediumCount: mediumBeasts.length,
  };
}

function draftBeastGroupKey(beast: CaravanBeast) {
  if (beast.catalogBeastCode) {
    return `catalog:${beast.catalogBeastCode}`;
  }

  return `custom:${beast.name}:${beast.size}:${beast.strength}:${beast.fourLegged}:${beast.speed}:${beast.thermalAdaptation ?? "na"}`;
}

function draftBeastSizeLabel(size: string) {
  if (size === "G") {
    return "Grande";
  }
  if (size === "M") {
    return "Mediana";
  }
  return size;
}

function groupedDraftBeasts(wagon: CaravanWagon): DraftBeastGroup[] {
  const groups = new Map<string, DraftBeastGroup>();

  for (const beast of wagon.draftBeasts) {
    const key = draftBeastGroupKey(beast);
    const existing = groups.get(key);
    const effectiveStrength = draftEffectiveStrength(beast);
    if (existing) {
      existing.count += 1;
      existing.totalEffectiveStrength += effectiveStrength;
      continue;
    }

    groups.set(key, {
      key,
      name: beast.name,
      size: beast.size,
      sizeLabel: draftBeastSizeLabel(beast.size),
      count: 1,
      fourLegged: beast.fourLegged,
      baseStrength: beast.strength,
      effectiveStrength,
      totalEffectiveStrength: effectiveStrength,
    });
  }

  return Array.from(groups.values()).sort((left, right) => left.name.localeCompare(right.name, "es"));
}

function draftEffectiveStrength(beast: CaravanBeast) {
  return beast.strength * (beast.fourLegged ? 2 : 1);
}

function draftStrengthState(wagon: CaravanWagon): DraftStrengthState {
  const required = Math.max(0, wagon.draftRequiredStrength);
  const total = Math.max(0, wagon.draftStrength);

  if (required === 0) {
    return {
      className: "limit-ok",
      label: "Sin requisito",
      description: "Este carro no necesita fuerza mínima para tirar.",
    };
  }

  if (total < required) {
    return {
      className: "limit-warning",
      label: `No alcanza (${total}/${required})`,
      description: "Las criaturas asignadas no alcanzan la fuerza necesaria para mover el carro.",
    };
  }

  if (total >= required * 2) {
    return {
      className: "draft-boost-high",
      label: "Supera la fuerza requerida en un 100% o más",
      description: "No necesitan descanso por este motivo.",
    };
  }

  if (total >= Math.ceil(required * 1.5)) {
    return {
      className: "draft-boost-mid",
      label: "Supera la fuerza requerida en un 50% o más",
      description: "Quedarán fatigadas tras 10 días ininterrumpidos.",
    };
  }

  return {
    className: "draft-boost-low",
    label: "Iguala o supera la fuerza requerida",
    description: "Quedarán fatigadas tras 5 días ininterrumpidos.",
  };
}

function toggleDraftPanel() {
  draftPanelExpanded.value = !draftPanelExpanded.value;
}

onMounted(refresh);
</script>

<template>
  <main class="page">
    <section class="shell">
      <header class="hero">
        <div>
          <p class="eyebrow">Caravana activa</p>
          <h1>Carros de la caravana</h1>
          <p class="subtitle">
            Revisa el catálogo completo, añade un carro y consulta sus detalles en una vista dedicada.
          </p>
        </div>

        <button class="ghost-button" type="button" :disabled="loading || submitting" @click="refresh">
          <span class="button-with-spinner">
            <span v-if="isPending('refresh')" class="button-spinner" aria-hidden="true"></span>
            <span>{{ isPending('refresh') ? "Refrescando…" : "Refrescar" }}</span>
          </span>
        </button>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <section v-if="!activeCaravan" class="card empty-state">
        <h2>No hay caravana activa</h2>
        <p class="muted">
          Debes seleccionar una caravana antes de añadir carros.
        </p>
        <RouterLink class="primary-link" to="/">Ir a caravanas</RouterLink>
      </section>

      <template v-else>
        <section class="summary card">
          <div>
            <h2>{{ activeCaravan.name }}</h2>
            <p class="muted" v-if="activeCaravan.description">{{ activeCaravan.description }}</p>
          </div>
          <div class="summary-actions">
            <div class="summary-stats">
              <div><span>Carros</span><strong>{{ wagons.length }} / {{ totalCapacity }}</strong></div>
              <div><span>Nivel</span><strong>{{ activeCaravan.level }}</strong></div>
              <div><span>Descontento</span><strong>{{ activeCaravan.discontent }}</strong></div>
            </div>
            <button class="primary-button" type="button" :disabled="loading || submitting" @click="openAddModal">
              Añadir
            </button>
          </div>
        </section>

        <div v-if="caravanWagonLimitState" class="warning-block caravan-limit-alert">
          <strong>Alerta</strong>
          <p>{{ caravanWagonLimitState.message }}</p>
        </div>

        <section class="card">
          <div class="section-header">
            <div>
              <h2>Carros de la caravana</h2>
              <p class="muted">Haz clic en un carro para ver todos sus detalles</p>
            </div>
            <div class="section-header-summary">
              <span class="pill">Consumo total visible: {{ visibleWagonTotalConsumption }}</span>
            </div>
          </div>

          <div class="filters">
            <label>
              <span>Tipo de carro</span>
              <select v-model="wagonsTypeFilter">
                <option value="all">Todos</option>
                <option value="viajeros">Viajeros</option>
                <option value="mercancias">Mercancías</option>
                <option value="especiales">Especiales</option>
              </select>
            </label>
            <label class="search-field">
              <span>Buscador</span>
              <input v-model="wagonsSearch" type="search" placeholder="Buscar por nombre" />
            </label>
          </div>

          <div v-if="loading" class="muted">Cargando carros…</div>
          <div v-else-if="visibleWagons.length === 0" class="empty-state-inline">
            <p class="muted">No hay carros que coincidan con el filtro.</p>
          </div>

          <div v-else class="table-wrap">
            <table class="wagon-table">
              <colgroup>
                <col class="wagon-col-name" />
                <col class="wagon-col-life" />
                <col class="wagon-col-dr" />
                <col class="wagon-col-travelers" />
                <col class="wagon-col-cargo" />
                <col class="wagon-col-draft" />
                <col class="wagon-col-alerts" />
              </colgroup>
              <thead>
                <tr>
                  <th>Carro</th>
                  <th>Vida</th>
                  <th>DR</th>
                  <th>Viajeros</th>
                  <th>Carga</th>
                  <th>Tiro</th>
                  <th aria-label="Alertas"></th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in visibleWagonRows"
                  :key="row.wagon.id"
                  tabindex="0"
                  role="button"
                  @click="openWagonDetails(row.wagon)"
                  @keyup.enter="openWagonDetails(row.wagon)"
                >
                  <td>
                    <strong>{{ row.wagon.name }}</strong>
                    <p v-if="row.wagon.specificCommodity" class="muted">Mercancía específica: {{ row.wagon.specificCommodity }}</p>
                    <p class="muted">{{ row.wagon.specialBenefit }}</p>
                  </td>
                  <td>
                    <p class="muted">{{ row.currentHitPoints }} / {{ row.wagon.hitPoints }}</p>
                    <div
                      class="health-meter"
                      :aria-label="`Vida ${row.currentHitPoints} de ${row.wagon.hitPoints}`"
                      :title="`Vida mínima: 0 · Vida máxima: ${row.wagon.hitPoints}`"
                    >
                      <span
                        class="health-meter-fill"
                        :style="{ width: `${percentageOf(row.currentHitPoints, row.wagon.hitPoints)}%` }"
                      ></span>
                    </div>
                  </td>
                  <td>{{ row.wagon.hardness }}</td>
                  <td>
                    <strong>{{ formatSpace(row.travelerCount + row.travelerBeastSpace) }} / {{ row.travelerCapacity }}</strong>
                    <div
                      class="capacity-meter"
                      :aria-label="`Viajeros ocupados ${formatSpace(row.travelerCount + row.travelerBeastSpace)} de ${row.travelerCapacity}`"
                      :title="`Ocupación: ${formatSpace(row.travelerCount + row.travelerBeastSpace)} / ${row.travelerCapacity}`"
                    >
                      <span
                        class="capacity-meter-fill capacity-meter-fill--travelers"
                        :style="{ width: `${percentageOf(row.travelerCount + row.travelerBeastSpace, row.travelerCapacity)}%` }"
                      ></span>
                    </div>
                  </td>
                  <td>
                    <strong>{{ row.cargoLoad }} / {{ row.cargoCapacity }}</strong>
                    <div
                      class="capacity-meter"
                      :aria-label="`Carga ocupada ${row.cargoLoad} de ${row.cargoCapacity}`"
                      :title="`Carga: ${row.cargoLoad} / ${row.cargoCapacity}`"
                    >
                      <span
                        class="capacity-meter-fill capacity-meter-fill--cargo"
                        :style="{ width: `${percentageOf(row.cargoLoad, row.cargoCapacity)}%` }"
                      ></span>
                    </div>
                  </td>
                  <td>
                    <div class="draft-summary-cell">
                      <p class="draft-summary-line">
                        <strong>{{ row.draftLargeCount }}</strong>/<span>{{ row.draftLargeCapacity }}</span> grandes ·
                        <strong>{{ row.draftMediumCount }}</strong>/<span>{{ row.draftMediumCapacity }}</span> medianas
                      </p>
                      <p v-if="row.draftRequirement === null" class="muted">Este carro no requiere tiro.</p>
                      <div class="draft-strength-meter" :aria-label="`Fuerza de tiro ${row.draftStrength} de ${row.draftRequirement?.minimumStrength ?? 0}`">
                        <div class="draft-strength-track">
                          <span
                            v-for="band in row.draftBands"
                            :key="`${row.wagon.id}-${band.label}`"
                            class="draft-band"
                            :class="band.className"
                            :style="{ width: `${band.widthPercent}%` }"
                            :title="band.label"
                          ></span>
                          <span
                            class="draft-strength-marker"
                            :style="{ left: `${Math.min(100, row.draftRequirement?.minimumStrength ? (row.draftStrength / Math.max(row.draftRequirement.minimumStrength * 2, row.draftStrength, 1)) * 100 : 0)}%` }"
                          ></span>
                        </div>
                        <div class="draft-strength-labels">
                          <span>0</span>
                          <span>{{ row.draftRequirement?.minimumStrength ?? 0 }}</span>
                          <span>{{ row.draftRequirement ? Math.ceil(row.draftRequirement.minimumStrength * 1.5) : 0 }}</span>
                          <span>{{ row.draftRequirement ? row.draftRequirement.minimumStrength * 2 : 0 }}</span>
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <button
                      v-if="row.alerts.length > 0"
                      class="alert-button"
                      type="button"
                      :class="{ danger: row.alerts.some((alert) => alert.kind === 'danger') }"
                      aria-label="Ver alertas"
                      @click.stop="openWagonAlerts(row.wagon)"
                    >
                      ⚠
                    </button>
                    <span v-else class="muted">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </section>

    <teleport to="body">
      <div v-if="addModalOpen" class="modal-backdrop" @click.self="closeAddModal">
        <div class="modal modal-add">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Añadir carro</p>
              <h2>Selecciona un carro</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeAddModal">Cerrar</button>
          </div>

          <div v-if="caravanWagonLimitState" class="warning-block caravan-limit-alert">
            <strong>Alerta</strong>
            <p>{{ caravanWagonLimitState.message }}</p>
          </div>

          <div class="filters">
            <label>
              <span>Tipo de carro</span>
              <select v-model="addTypeFilter">
                <option value="all">Todos</option>
                <option value="viajeros">Viajeros</option>
                <option value="mercancias">Mercancías</option>
                <option value="especiales">Especiales</option>
              </select>
            </label>
            <div class="search-action-row">
              <label class="search-field">
                <span>Buscador</span>
                <input v-model="addSearch" type="search" placeholder="Buscar por nombre" />
              </label>
              <button
                class="primary-button confirm-button"
                :class="{
                  disabled:
                    submitting ||
                    !selectedCatalogItem ||
                    selectedCatalogNeedsOverride ||
                    (selectedCatalogRequiresSpecificCommodity && !addWagonSpecificCommodityValue),
                  loading: submitting,
                }"
                type="button"
                :disabled="
                  loading ||
                  submitting ||
                  !selectedCatalogItem ||
                  selectedCatalogNeedsOverride ||
                  (selectedCatalogRequiresSpecificCommodity && !addWagonSpecificCommodityValue)
                "
                :aria-busy="isPending('add-wagon')"
                @click="handleAddSelected"
              >
                <span class="button-with-spinner">
                  <span v-if="isPending('add-wagon')" class="button-spinner" aria-hidden="true"></span>
                  <span class="confirm-label">{{ isPending('add-wagon') ? "Añadiendo…" : "Confirmar" }}</span>
                  <span v-if="!isPending('add-wagon')" class="confirm-arrow" aria-hidden="true">→</span>
                </span>
              </button>
            </div>
          </div>

          <div class="add-layout">
            <div class="catalog-list">
              <button
                v-for="item in visibleCatalogItems"
                :key="item.code"
                class="catalog-item"
                :class="{ selected: item.code === selectedCatalogItem?.code }"
                type="button"
                @click="selectCatalogItem(item.code)"
              >
                <div>
                  <strong>{{ item.name }}</strong>
                  <p class="muted">{{ item.category }}</p>
                  <p v-if="isLimitExceeded(item)" class="limit-warning">No cumple requisitos: {{ limitMessage(item) }}</p>
                  <p v-else class="limit-ok">Límite disponible: {{ limitMessage(item) }}</p>
                </div>
                <div class="catalog-meta">
                  <span>{{ item.cost }} po</span>
                  <span>{{ item.limit }}</span>
                </div>
              </button>
              <p v-if="visibleCatalogItems.length === 0" class="muted empty-catalog">
                No hay carros que coincidan con el filtro.
              </p>
            </div>

            <div v-if="selectedCatalogItem" class="preview">
              <div class="preview-top">
                <div>
                  <h3>{{ selectedCatalogItem.name }}</h3>
                  <p class="muted">{{ selectedCatalogItem.category }}</p>
                </div>
              </div>

                <label class="search-field add-name-field">
                  <span>Nombre del nuevo carro</span>
                  <input v-model="addWagonName" type="text" placeholder="Se mostrará en el frontal" />
                </label>

                <div v-if="selectedCatalogRequiresSpecificCommodity" class="two-columns">
                  <label class="search-field add-name-field">
                    <span>Mercancía específica</span>
                    <select v-model="addWagonSpecificCommodityOption" class="styled-select">
                      <option value="" disabled>Selecciona una mercancía</option>
                      <option v-for="option in specificCommodityOptions" :key="option" :value="option">
                        {{ option }}
                      </option>
                      <option value="custom">Carga personalizada</option>
                    </select>
                  </label>

                  <label v-if="addWagonSpecificCommodityOption === 'custom'" class="search-field add-name-field">
                    <span>Nombre de la carga</span>
                    <input
                      v-model="addWagonSpecificCommodityCustom"
                      type="text"
                      placeholder="Escribe el nombre"
                    />
                  </label>
                </div>

                <dl class="stats">
                  <div><dt>Coste</dt><dd>{{ selectedCatalogItem.cost }} po</dd></div>
                <div><dt>PG</dt><dd>{{ selectedCatalogItem.hitPoints }}</dd></div>
                <div><dt>Dureza</dt><dd>{{ selectedCatalogItem.hardness }}</dd></div>
                <div><dt>Propulsión</dt><dd>{{ selectedCatalogItem.propulsion }}</dd></div>
                <div><dt>Viajeros</dt><dd>{{ selectedCatalogItem.travelerCapacity }}</dd></div>
                <div><dt>Cargamento</dt><dd>{{ selectedCatalogItem.cargoCapacity }}</dd></div>
                <div><dt>Límite</dt><dd>{{ selectedCatalogItem.limit }}</dd></div>
                <div><dt>Consumo</dt><dd>{{ selectedCatalogItem.consumption }}</dd></div>
              </dl>

              <section class="info-block">
                <h4>Beneficio especial</h4>
                <p>{{ selectedCatalogItem.specialBenefit }}</p>
              </section>

              <section class="info-block">
                <h4>Descripción</h4>
                <p>{{ selectedCatalogItem.description }}</p>
              </section>

              <section v-if="selectedCatalogItem.notes" class="info-block">
                <h4>Notas</h4>
                <p>{{ selectedCatalogItem.notes }}</p>
              </section>

              <div v-if="selectedCatalogNeedsOverride" class="warning-block">
                <strong>Atención</strong>
                <p>
                  Este carro ya alcanzó su límite de tipo. Actualmente hay
                  {{ wagonCountForType(selectedCatalogItem.code) }} y el máximo permitido es
                  {{ maxAllowedFor(selectedCatalogItem) }}.
                </p>
                <p>No puede añadirse hasta que cambie esta regla.</p>
              </div>
            </div>

            <div v-else class="preview empty-preview">
              <p class="muted">Selecciona un carro para ver sus estadísticas y confirmar la acción.</p>
            </div>
          </div>
        </div>
      </div>

      <div v-if="selectedWagon" class="modal-backdrop" @click.self="closeModal">
          <div class="modal">
            <div class="modal-header">
              <div>
                <p class="eyebrow">Detalle del carro</p>
                <div class="wagon-title-row">
                  <h2>{{ selectedWagon.name }}</h2>
                  <button
                    class="ghost-button name-edit-button"
                    type="button"
                    :disabled="loading || submitting"
                    :aria-expanded="wagonNameEditorOpen"
                    aria-label="Editar nombre"
                    @click="wagonNameEditorOpen = true"
                  >
                    ✏️
                  </button>
                </div>
                <p v-if="selectedWagonSpecificCommodityLabel" class="muted">
                  Mercancía específica: {{ selectedWagonSpecificCommodityLabel }}
                </p>
              </div>
            <div class="detail-actions">
              <button class="secondary-button" type="button" :disabled="loading || submitting" @click="openImprovementModal">
                <span class="button-with-spinner">
                  <span v-if="isPending('open-improvements')" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending('open-improvements') ? "Cargando…" : "Mejoras" }}</span>
                </span>
              </button>
              <button class="danger-button" type="button" :disabled="loading || submitting" @click="handleDeleteSelectedWagon">
                <span class="button-with-spinner">
                  <span v-if="isPending(`delete-wagon:${selectedWagon.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending(`delete-wagon:${selectedWagon.id}`) ? "Eliminando…" : "Eliminar" }}</span>
                </span>
              </button>
              <button class="ghost-button" type="button" @click="closeModal">Cerrar</button>
            </div>
          </div>

          <div v-if="wagonNameEditorOpen" class="name-editor-card">
            <div class="name-editor-header">
              <div>
                <p class="eyebrow">Nombre</p>
              </div>
              <button
                class="primary-button"
                type="button"
                :disabled="loading || submitting"
                :aria-busy="isPending(`rename-wagon:${selectedWagon.id}`)"
                @click="handleRenameSelectedWagon"
              >
                <span class="button-with-spinner">
                  <span v-if="isPending(`rename-wagon:${selectedWagon.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending(`rename-wagon:${selectedWagon.id}`) ? "Guardando…" : "Guardar cambios" }}</span>
                </span>
              </button>
            </div>

            <label class="name-editor-field">
              <span>Nombre</span>
              <input
                v-model="wagonNameDraft"
                type="text"
                placeholder="Nombre del carro"
              />
            </label>
          </div>

          <section v-if="wagonAlertsSummary" class="info-block">
            <h3>Detalle de alertas</h3>
            <div v-if="wagonAlertsSummary.alerts.length === 0" class="muted">
              Este carro no tiene alertas activas.
            </div>
            <div v-else class="alerts-list">
              <article
                v-for="alert in wagonAlertsSummary.alerts"
                :key="`${wagonAlertsSummary.wagon.id}-${alert.title}`"
                class="alert-item"
                :class="alert.kind"
              >
                <strong>{{ alert.title }}</strong>
                <p>{{ alert.description }}</p>
              </article>
            </div>
          </section>

          <section class="info-block wagon-health-card">
            <div class="section-header">
              <div>
                <h3>Vida</h3>
                <p class="muted">Gestiona el daño y la reparación del carro.</p>
              </div>
            </div>

            <div class="wagon-health-controls" role="group" aria-label="Controles de vida del carro">
              <button
                class="life-mini-button"
                type="button"
                :disabled="loading || submitting"
                aria-label="Abrir modal de daño"
                title="Dañar carro"
                @click="openHealthModal('damage')"
              >
                -
              </button>

              <div class="health-meter wagon-health-meter">
                <span
                  class="health-meter-fill"
                  :style="{ width: `${selectedWagonHitPointPercentage}%` }"
                ></span>
              </div>

              <button
                class="life-mini-button"
                type="button"
                :disabled="loading || submitting"
                aria-label="Abrir modal de reparación"
                title="Reparar carro"
                @click="openHealthModal('repair')"
              >
                +
              </button>
            </div>

            <div class="wagon-health-values">
              <span><strong>{{ selectedWagonCurrentHitPoints }}</strong> vida actual</span>
              <span><strong>{{ selectedWagonHitPoints }}</strong> vida máxima</span>
            </div>
          </section>

          <dl class="stats modal-stats">
            <div><dt>Tipo</dt><dd>{{ selectedWagonTypeName }}</dd></div>
            <div><dt>Coste</dt><dd>{{ selectedWagon.cost }} po</dd></div>
            <div><dt>Dureza</dt><dd>{{ selectedWagon.hardness }}</dd></div>
            <div><dt>Límite</dt><dd>{{ selectedWagon.limit }}</dd></div>
            <div><dt>Consumo</dt><dd>{{ selectedWagon.consumption }}</dd></div>
            <div><dt>Consumo total</dt><dd>{{ selectedWagonTotalConsumption }}</dd></div>
          </dl>

          <section class="info-block">
            <div class="section-header">
              <div>
                <h3>Carretero del carro</h3>
                <p class="muted">El viajero que debe estar asignado a este carro para moverlo.</p>
              </div>
            </div>

            <div v-if="selectedWagonCarretero || selectedWagon.carreteroName" class="assignment-item assignment-item--stacked">
              <div>
                <strong>{{ selectedWagonCarretero?.fullName ?? selectedWagon.carreteroName }}</strong>
                <p class="muted">{{ selectedWagonCarretero?.activeRoleName ?? "Carretero asignado" }}</p>
              </div>
              <span class="pill">Carretero</span>
            </div>
            <div v-else class="muted">
              Este carro todavía no tiene un carretero asignado.
            </div>
          </section>

          <section class="info-block">
            <div class="section-header">
              <div>
                <h3>Tripulación asignada</h3>
                <p class="muted">
                  {{ formatSpace(selectedWagonOccupancy) }}/{{ selectedWagon.travelerCapacity }} plazas ocupadas entre viajeros y bestias viajeras.
                </p>
              </div>
              <div class="accordion-actions">
                <button
                  class="secondary-button"
                  type="button"
                  :disabled="loading || submitting || availableTravelersForSelectedWagon.length === 0"
                  @click="openAssignmentModal('traveler')"
                >
                  Añadir viajeros
                </button>
                <button
                  class="secondary-button"
                  type="button"
                  :disabled="loading || submitting || availableTravelerBeastsForSelectedWagon.length === 0"
                  @click="openAssignmentModal('beast-traveler')"
                >
                  Añadir bestias viajeras
                </button>
              </div>
            </div>

            <div class="summary-meter-block">
              <div
                class="meter-strip"
                :aria-label="`Ocupación de tripulación ${formatSpace(selectedWagonOccupancy)} de ${selectedWagon.travelerCapacity}`"
              >
                <span
                  v-for="segment in selectedWagonCrewSegments"
                  :key="segment.key"
                  class="meter-segment"
                  :style="{ width: `${segment.widthPercent}%`, background: segment.color }"
                  :title="segment.label"
                ></span>
              </div>
              <div class="meter-legend">
                <span>Viajeros</span>
                <span>{{ selectedWagonTravelers.length }}</span>
                <span>Bestias viajeras</span>
                <span>{{ formatSpace(selectedWagonTravelerBeasts.reduce((total, beast) => total + beast.occupiedSpace, 0)) }}</span>
                <span>Libre</span>
                <span>{{ formatSpace(selectedWagonCapacityRemaining) }}</span>
              </div>
            </div>

            <div class="assigned-grid">
              <article class="assigned-panel">
                <div class="assigned-panel-header">
                  <div>
                    <h4>Viajeros</h4>
                    <p class="muted">{{ selectedWagonTravelers.length }} viajeros asignados</p>
                  </div>
                  <span class="pill">{{ selectedWagonTravelers.length }}</span>
                </div>

                <div v-if="selectedWagonTravelers.length === 0" class="muted">
                  No hay viajeros asignados a este carro.
                </div>
                <div v-else class="assignment-list">
                  <article v-for="traveler in selectedWagonTravelers" :key="traveler.id" class="assignment-item">
                    <div>
                      <strong>{{ traveler.fullName }}</strong>
                      <p class="muted">{{ traveler.activeRoleName }}</p>
                    </div>
                    <button
                      class="trash-button"
                      type="button"
                      :disabled="loading || submitting"
                      :aria-label="`Quitar a ${traveler.fullName} del carro`"
                      @click="clearTravelerAssignment(traveler)"
                    >
                      <span class="button-with-spinner">
                        <span v-if="isPending(`clear-traveler:${traveler.id}`)" class="button-spinner" aria-hidden="true"></span>
                        <span>🗑</span>
                      </span>
                    </button>
                  </article>
                </div>
              </article>

              <article class="assigned-panel">
                <div class="assigned-panel-header">
                  <div>
                    <h4>Bestias viajeras</h4>
                    <p class="muted">{{ selectedWagonTravelerBeasts.length }} bestias asignadas como viajeras</p>
                  </div>
                  <span class="pill">{{ selectedWagonTravelerBeasts.length }}</span>
                </div>

                <div v-if="selectedWagonTravelerBeasts.length === 0" class="muted">
                  No hay bestias asignadas como viajeras.
                </div>
                <div v-else class="assignment-list">
                  <article v-for="beast in selectedWagonTravelerBeasts" :key="beast.id" class="assignment-item">
                    <div>
                      <strong>{{ beast.name }}</strong>
                      <p class="muted">
                        {{ beast.size }} · Fuerza {{ beast.strength }} ·
                        {{ beast.sourceType === "CATALOG" ? "Catálogo" : "Personalizada" }}
                      </p>
                    </div>
                    <button
                      class="trash-button"
                      type="button"
                      :disabled="loading || submitting"
                      :aria-label="`Quitar a ${beast.name} del carro`"
                      @click="clearBeastAssignment(beast)"
                    >
                      <span class="button-with-spinner">
                        <span v-if="isPending(`clear-beast:${beast.id}`)" class="button-spinner" aria-hidden="true"></span>
                        <span>🗑</span>
                      </span>
                    </button>
                  </article>
                </div>
              </article>
            </div>
          </section>

          <section class="info-block">
            <div class="section-header">
                <div>
                  <h3>Mercancías transportadas</h3>
                  <p class="muted">
                  {{ groupedSelectedWagonCargo.length }} entradas · {{ selectedWagonCargoLoad }} / {{ selectedWagon.cargoCapacity }}
                  usadas · quedan {{ selectedWagonCargoRemaining }}.
                </p>
                  <p v-if="selectedWagonSpecificCommodityLabel" class="muted">
                    Mercancía específica del carro: {{ selectedWagonSpecificCommodityLabel }}
                  </p>
                </div>
              <div class="accordion-actions">
                <button
                  class="secondary-button"
                  type="button"
                  :disabled="loading || submitting || selectedWagonCargoRemaining === 0"
                  @click="openCargoModal('catalog')"
                >
                  Añadir de catálogo
                </button>
                <button
                  v-if="selectedWagon.wagonTypeCode !== 'carro-de-mercancias-especificas'"
                  class="secondary-button"
                  type="button"
                  :disabled="loading || submitting || selectedWagonCargoRemaining === 0"
                  @click="openCargoModal('custom')"
                >
                  Añadir personalizada
                </button>
              </div>
            </div>

            <div class="summary-meter-block">
              <div
                class="meter-strip"
                :aria-label="`Ocupación de carga ${selectedWagonCargoLoad} de ${selectedWagon.cargoCapacity}`"
              >
                <span
                  v-for="segment in selectedWagonCargoSegments"
                  :key="segment.key"
                  class="meter-segment"
                  :style="{ width: `${segment.widthPercent}%`, background: segment.color }"
                  :title="segment.label"
                ></span>
              </div>
              <div class="meter-legend">
                <span>Carga usada</span>
                <span>{{ selectedWagonCargoLoad }}</span>
                <span>Espacio libre</span>
                <span>{{ selectedWagonCargoRemaining }}</span>
              </div>
            </div>

            <div v-if="groupedSelectedWagonCargo.length === 0" class="muted">
              No hay mercancías asignadas a este carro.
            </div>
            <div v-else>
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Origen</th>
                    <th>Categoría</th>
                    <th>Cantidad</th>
                    <th>Carga total</th>
                    <th>Notas</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="entry in groupedSelectedWagonCargo" :key="entry.key">
                    <td>
                      <strong>{{ entry.representative.displayName }}</strong>
                      <p class="muted">{{ entry.representative.sourceTypeLabel }}</p>
                      <p v-if="entry.representative.specificCommodity" class="muted">
                        Mercancía específica: {{ entry.representative.specificCommodity }}
                      </p>
                      <p v-if="entry.entries.length > 1" class="muted">
                        {{ entry.entries.length }} entradas agrupadas
                      </p>
                    </td>
                    <td>{{ entry.representative.origin ?? "—" }}</td>
                    <td>{{ entry.representative.category }}</td>
                    <td>{{ entry.quantity }}</td>
                    <td>{{ entry.totalCargoLoad }}</td>
                    <td>{{ entry.representative.notes ?? "—" }}</td>
                    <td>
                      <button
                        class="trash-button"
                        type="button"
                        :disabled="loading || submitting"
                        :aria-label="`Eliminar ${entry.representative.displayName}`"
                        @click="handleDeleteSelectedWagonCargo(entry.representative)"
                      >
                        <span class="button-with-spinner">
                          <span
                            v-if="isPending(`delete-cargo:${entry.representative.id}`)"
                            class="button-spinner"
                            aria-hidden="true"
                          ></span>
                          <span>🗑</span>
                        </span>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section class="info-block">
            <div class="section-header">
              <div>
                <h3>Tiro actual</h3>
                <p class="muted">Criaturas asignadas al tiro y fuerza efectiva del conjunto.</p>
              </div>
              <div class="draft-header-actions">
                <span class="draft-power-badge" :class="draftStrengthState(selectedWagon).className">
                  {{ draftStrengthState(selectedWagon).label }}
                </span>
                <button
                  class="secondary-button draft-toggle-button"
                  type="button"
                  :disabled="loading || submitting || availableDraftBeastsForSelectedWagon.length === 0"
                  @click="openAssignmentModal('beast-draft')"
                >
                  Añadir bestias al tiro
                </button>
                <button class="secondary-button draft-toggle-button" type="button" @click="toggleDraftPanel">
                  {{ draftPanelExpanded ? "Ocultar" : "Mostrar" }}
                </button>
              </div>
            </div>

            <template v-if="draftPanelExpanded">
              <div
                v-if="selectedWagonDraftBeasts.length === 0"
                class="warning-block"
              >
                <strong>Alerta</strong>
                <p>No hay criaturas asignadas al tiro de este carro.</p>
              </div>
              <div v-else-if="draftStrengthState(selectedWagon).className === 'limit-warning'" class="warning-block">
                <strong>Alerta</strong>
                <p>{{ draftStrengthState(selectedWagon).description }}</p>
              </div>
              <template v-else>
                <div class="summary-meter-block">
                  <div
                    class="meter-strip"
                    :aria-label="`Tiro ocupado ${selectedWagonDraftBeasts.length} de ${(selectedWagonDraftRequirement?.maxLargeBeasts ?? 0) + (selectedWagonDraftRequirement?.maxMediumBeasts ?? 0)}`"
                  >
                    <span
                      v-for="segment in selectedWagonDraftOccupancySegments"
                      :key="segment.key"
                      class="meter-segment"
                      :style="{ width: `${segment.widthPercent}%`, background: segment.color }"
                      :title="segment.label"
                    ></span>
                  </div>
                  <div class="meter-legend">
                    <span>Grandes</span>
                    <span>{{ draftOccupancyForWagon(selectedWagon).largeCount }}/{{ selectedWagonDraftRequirement?.maxLargeBeasts ?? 0 }}</span>
                    <span>Medianas</span>
                    <span>{{ draftOccupancyForWagon(selectedWagon).mediumCount }}/{{ selectedWagonDraftRequirement?.maxMediumBeasts ?? 0 }}</span>
                  </div>
                </div>

                <div class="summary-meter-block">
                  <div
                    class="meter-strip meter-strip--strength"
                    :aria-label="`Fuerza requerida ${selectedWagon.draftStrength} de ${selectedWagon.draftRequiredStrength}`"
                  >
                    <span
                      v-for="band in draftBandsForRequirement(selectedWagon.draftRequiredStrength, selectedWagon.draftStrength)"
                      :key="`${selectedWagon.id}-${band.label}`"
                      class="meter-segment"
                      :style="{ width: `${band.widthPercent}%`, background: draftBandColor(band.className) }"
                      :title="band.label"
                    ></span>
                  </div>
                  <div class="meter-legend">
                    <span>Requerida</span>
                    <span>{{ selectedWagon.draftRequiredStrength }}</span>
                    <span>Actual</span>
                    <span>{{ selectedWagon.draftStrength }}</span>
                  </div>
                </div>

                <dl class="stats draft-stats">
                  <div>
                    <dt>Máx. grandes</dt>
                    <dd>{{ parseDraftRequirement(selectedWagon.propulsion)?.maxLargeBeasts ?? 0 }}</dd>
                  </div>
                  <div>
                    <dt>Máx. medianas</dt>
                    <dd>{{ parseDraftRequirement(selectedWagon.propulsion)?.maxMediumBeasts ?? 0 }}</dd>
                  </div>
                  <div>
                    <dt>Actual grandes</dt>
                    <dd>{{ draftOccupancyForWagon(selectedWagon).largeCount }}</dd>
                  </div>
                  <div>
                    <dt>Actual medianas</dt>
                    <dd>{{ draftOccupancyForWagon(selectedWagon).mediumCount }}</dd>
                  </div>
                  <div>
                    <dt>Fuerza requerida</dt>
                    <dd>{{ selectedWagon.draftRequiredStrength }}</dd>
                  </div>
                  <div>
                    <dt>Fuerza total</dt>
                    <dd>{{ selectedWagon.draftStrength }}</dd>
                  </div>
                </dl>

                <p class="draft-state-description" :class="draftStrengthState(selectedWagon).className">
                  {{ draftStrengthState(selectedWagon).description }}
                </p>

                <div class="draft-beast-list">
                  <article v-for="group in groupedDraftBeasts(selectedWagon)" :key="group.key" class="draft-beast-card">
                    <div class="draft-beast-main">
                      <div class="draft-beast-row">
                        <div class="draft-beast-name-line">
                          <strong>{{ group.name }}</strong>
                          <span class="draft-count-badge">{{ group.count }}</span>
                        </div>
                        <strong class="draft-base-strength">Fuerza base {{ group.baseStrength }}</strong>
                      </div>
                      <p class="muted">{{ group.sizeLabel }}</p>
                    </div>

                    <div class="draft-beast-stats">
                      <span v-if="group.fourLegged">Cuenta doble por cuatro patas</span>
                      <strong>Fuerza efectiva total {{ group.totalEffectiveStrength }}</strong>
                    </div>
                  </article>
                </div>
              </template>
            </template>
          </section>

          <section class="info-block">
            <h3>Beneficio especial</h3>
            <p>{{ selectedWagon.specialBenefit }}</p>
          </section>

          <section class="info-block">
            <h3>Descripción</h3>
            <p>{{ selectedWagon.description }}</p>
          </section>

          <section v-if="selectedWagon.notes" class="info-block">
            <h3>Notas</h3>
            <p>{{ selectedWagon.notes }}</p>
          </section>

          <section class="info-block improvements-accordion">
            <div class="accordion-header">
              <button class="accordion-title-button" type="button" @click="toggleImprovementsExpanded">
                <div class="accordion-title">
                  <h3>Mejoras aplicadas</h3>
                  <p class="muted">{{ selectedWagon.improvements.length }} mejoras</p>
                </div>
              </button>

              <div class="accordion-actions">
                <button class="secondary-button" type="button" :disabled="loading || submitting" @click="openImprovementModal">
                  <span class="button-with-spinner">
                    <span v-if="isPending('open-improvements')" class="button-spinner" aria-hidden="true"></span>
                    <span>{{ isPending('open-improvements') ? "Cargando…" : "Añadir mejora" }}</span>
                  </span>
                </button>
                <button class="secondary-button" type="button" :class="{ active: improvementDeleteMode }" @click="toggleImprovementDeleteMode">
                  Eliminar
                </button>
                <button class="secondary-button" type="button" @click="toggleImprovementsExpanded">
                  {{ improvementsExpanded ? "Ocultar" : "Mostrar" }}
                </button>
              </div>
            </div>

            <div v-if="improvementsExpanded">
              <div v-if="selectedWagon.improvements.length === 0" class="muted">
                Este carro todavía no tiene mejoras.
              </div>
              <div v-else class="improvements-list">
                <article v-for="improvement in selectedWagon.improvements" :key="improvement.id" class="improvement-card">
                  <div>
                    <strong>{{ improvement.name }}</strong>
                    <p class="muted">{{ improvement.category }}</p>
                    <p>{{ improvement.specialBenefit }}</p>
                  </div>
                  <div v-if="improvementDeleteMode" class="improvement-actions">
                    <button
                      class="trash-button"
                      type="button"
                      :disabled="loading || submitting"
                      :aria-label="`Eliminar ${improvement.name}`"
                      @click="handleRemoveImprovement(improvement)"
                    >
                      <span class="button-with-spinner">
                        <span v-if="isPending(`delete-improvement:${improvement.id}`)" class="button-spinner" aria-hidden="true"></span>
                        <span>🗑</span>
                      </span>
                    </button>
                  </div>
                </article>
              </div>
            </div>
          </section>

          <p class="muted meta-line">
            Añadido el {{ new Date(selectedWagon.createdAt).toLocaleString() }}
          </p>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="wagonAlertsModalOpen && wagonAlertsSummary" class="modal-backdrop" @click.self="closeWagonAlertsModal">
        <div class="modal modal-alerts">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Alertas del carro</p>
              <h2>{{ wagonAlertsSummary.wagon.name }}</h2>
              <p class="muted">{{ wagonAlertsSummary.alerts.length }} alerta{{ wagonAlertsSummary.alerts.length > 1 ? "s" : "" }} detectada{{ wagonAlertsSummary.alerts.length > 1 ? "s" : "" }}</p>
            </div>
            <button class="ghost-button" type="button" @click="closeWagonAlertsModal">Cerrar</button>
          </div>

          <section class="info-block">
            <div class="alert-summary-grid">
              <div>
                <span>Viajeros</span>
                <strong>{{ formatSpace(wagonAlertsSummary.travelerCount + wagonAlertsSummary.travelerBeastSpace) }} / {{ wagonAlertsSummary.travelerCapacity }}</strong>
              </div>
              <div><span>Carga</span><strong>{{ wagonAlertsSummary.cargoLoad }} / {{ wagonAlertsSummary.cargoCapacity }}</strong></div>
              <div><span>Carretero</span><strong>{{ wagonAlertsSummary.hasCarretero ? "Asignado" : "Pendiente" }}</strong></div>
              <div><span>Tiro</span><strong>{{ wagonAlertsSummary.draftStrength }} / {{ wagonAlertsSummary.draftRequirement?.minimumStrength ?? 0 }}</strong></div>
              <div><span>Velocidad</span><strong>{{ wagonAlertsSummary.speed }} mi/día</strong></div>
            </div>
          </section>

          <div class="modal-actions">
            <button class="primary-button" type="button" @click="closeWagonAlertsModal">Entendido</button>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="cargoModalOpen" class="modal-backdrop" @click.self="closeCargoModal">
        <div class="modal modal-add">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Añadir mercancía</p>
              <h2>{{ cargoModalMode === 'catalog' ? 'Añadir de catálogo' : 'Añadir personalizada' }}</h2>
              <p class="muted">Se añadirá directamente a {{ selectedWagon?.name }}</p>
            </div>
            <button class="ghost-button" type="button" @click="closeCargoModal">Cerrar</button>
          </div>

          <p v-if="cargoModalError" class="error">{{ cargoModalError }}</p>

          <div v-if="cargoModalMode === 'catalog'" class="modal-form">
            <label>
              <span>Mercancía de catálogo</span>
              <select v-model="cargoCatalogCode">
                <option value="">Selecciona una mercancía</option>
                <option v-for="item in addableCargoCatalogItems" :key="item.code" :value="item.code">
                  {{ item.name }} · {{ item.category }}
                </option>
              </select>
            </label>

            <div v-if="selectedCargoCatalogItem" class="two-columns">
              <label>
                <span>Cantidad</span>
                <input v-model="cargoQuantity" type="number" min="1" :max="cargoQuantityMax" />
              </label>
              <label>
                <span>Unidades de carga</span>
                <input
                  v-model="cargoUnits"
                  type="number"
                  min="1"
                  :disabled="!selectedCargoCatalogItem.cargoUnitsEditable"
                />
              </label>
            </div>

            <template v-if="selectedCargoCatalogItem">
              <div class="two-columns">
                <label v-if="cargoRequiresOrigin">
                  <span>Origen</span>
                  <input v-model="cargoOrigin" type="text" placeholder="Obligatorio" />
                </label>
                <label v-if="cargoRequiresSpecificCommodity">
                  <span>Mercancía específica</span>
                  <input
                    v-if="cargoSpecificCommodityLocked && cargoSpecificCommodityIsCustom"
                    :value="selectedWagon?.specificCommodity?.trim() || ''"
                    type="text"
                    readonly
                  />
                  <select
                    v-else-if="cargoSpecificCommodityLocked"
                    v-model="cargoSpecificCommodityOption"
                    class="styled-select"
                    disabled
                  >
                    <option :value="selectedWagon?.specificCommodity?.trim() || ''">
                      {{ selectedWagon?.specificCommodity?.trim() || "Sin mercancía específica asignada" }}
                    </option>
                  </select>
                  <select v-else v-model="cargoSpecificCommodityOption" class="styled-select">
                    <option value="" disabled>Selecciona una mercancía</option>
                    <option v-for="option in specificCommodityOptions" :key="option" :value="option">
                      {{ option }}
                    </option>
                    <option value="custom">Carga personalizada</option>
                  </select>
                </label>
              </div>

              <div
                v-if="cargoRequiresSpecificCommodity && !cargoSpecificCommodityLocked && cargoSpecificCommodityOption === 'custom'"
                class="two-columns"
              >
                <label>
                  <span>Nombre de la carga</span>
                  <input
                    v-model="cargoSpecificCommodityCustom"
                    type="text"
                    placeholder="Escribe el nombre"
                  />
                </label>
              </div>

              <div class="two-columns">
                <label v-if="cargoRequiresDeity">
                  <span>Deidad</span>
                  <input v-model="cargoDeity" type="text" placeholder="Obligatorio" />
                </label>
                <label>
                  <span>Notas</span>
                  <input v-model="cargoNotes" type="text" />
                </label>
              </div>
            </template>
          </div>

          <div v-else class="modal-form">
            <label>
              <span>Nombre</span>
              <input v-model="cargoDisplayName" type="text" />
            </label>

            <label>
              <span>Categoría</span>
              <input v-model="cargoCategory" type="text" />
            </label>

            <div class="two-columns">
              <label>
                <span>Cantidad</span>
                <input v-model="cargoQuantity" type="number" min="1" :max="cargoQuantityMax" />
              </label>
              <label>
                <span>Unidades de carga</span>
                <input v-model="cargoUnits" type="number" min="1" />
              </label>
            </div>

              <div class="two-columns">
                <label>
                  <span>Origen</span>
                  <input v-model="cargoOrigin" type="text" />
                </label>
                <label>
                  <span>Mercancía específica</span>
                  <select v-model="cargoSpecificCommodityOption" class="styled-select">
                    <option value="" disabled>Selecciona una mercancía</option>
                    <option v-for="option in specificCommodityOptions" :key="option" :value="option">
                      {{ option }}
                    </option>
                    <option value="custom">Carga personalizada</option>
                  </select>
                </label>
              </div>

            <div v-if="cargoSpecificCommodityOption === 'custom'" class="two-columns">
              <label>
                <span>Nombre de la carga</span>
                <input
                  v-model="cargoSpecificCommodityCustom"
                  type="text"
                  placeholder="Escribe el nombre"
                />
              </label>
            </div>

            <div class="two-columns">
              <label>
                <span>Deidad</span>
                <input v-model="cargoDeity" type="text" />
              </label>
              <label>
                <span>Notas</span>
                <input v-model="cargoNotes" type="text" />
              </label>
            </div>
          </div>

          <div class="modal-actions">
            <button
              class="primary-button confirm-button"
              type="button"
              :disabled="
                loading ||
                submitting ||
                !selectedWagon ||
                (cargoModalMode === 'catalog' && (!selectedCargoCatalogItem || (cargoRequiresSpecificCommodity && !cargoSpecificCommodityValue)))
              "
              :aria-busy="submitting"
              @click="cargoModalMode === 'catalog' ? handleAddCatalogCargoToSelectedWagon() : handleAddCustomCargoToSelectedWagon()"
            >
              <span class="button-with-spinner">
                <span
                  v-if="isPending(cargoModalMode === 'catalog' ? 'add-cargo' : 'add-custom-cargo')"
                  class="button-spinner"
                  aria-hidden="true"
                ></span>
                <span>{{ isPending(cargoModalMode === 'catalog' ? 'add-cargo' : 'add-custom-cargo') ? 'Guardando…' : 'Guardar' }}</span>
              </span>
            </button>
            <button class="ghost-button" type="button" @click="closeCargoModal">Cancelar</button>
          </div>
        </div>
      </div>

      <div v-if="healthModalOpen" class="modal-backdrop" @click.self="closeHealthModal">
        <div class="modal modal-add">
          <div class="modal-header">
            <div>
              <p class="eyebrow">{{ healthModalMode === 'damage' ? 'Dañar carro' : 'Reparar carro' }}</p>
              <h2>{{ healthModalMode === 'damage' ? 'Aplicar daño' : 'Reparar vida' }}</h2>
              <p class="muted">
                {{ healthModalMode === 'damage'
                  ? 'La dureza se resta del daño salvo que marques la opción para ignorarla.'
                  : 'La reparación no puede superar la vida máxima del carro.' }}
              </p>
            </div>
            <button class="ghost-button" type="button" @click="closeHealthModal">Cerrar</button>
          </div>

          <p v-if="healthModalError" class="error">{{ healthModalError }}</p>

          <div class="modal-form">
            <label>
              <span>{{ healthModalMode === 'damage' ? 'Daño a aplicar' : 'Vida a reparar' }}</span>
              <input v-model="healthModalAmount" type="number" min="1" step="1" />
            </label>

            <label v-if="healthModalMode === 'damage'" class="checkbox-field">
              <input v-model="healthModalIgnoreHardness" type="checkbox" />
              <span>Ignorar dureza</span>
            </label>
          </div>

          <div class="modal-actions">
            <button
              class="primary-button confirm-button"
              type="button"
              :disabled="loading || submitting || !selectedWagon"
              :aria-busy="submitting"
              @click="handleApplyHealthChange"
            >
              <span class="button-with-spinner">
                <span v-if="isPending(`${healthModalMode}-wagon:${selectedWagon?.id}`)" class="button-spinner" aria-hidden="true"></span>
                <span>{{ isPending(`${healthModalMode}-wagon:${selectedWagon?.id}`) ? 'Guardando…' : 'Guardar' }}</span>
              </span>
            </button>
            <button class="ghost-button" type="button" @click="closeHealthModal">Cancelar</button>
          </div>
        </div>
      </div>

      <div v-if="assignmentModalOpen" class="modal-backdrop" @click.self="closeAssignmentModal">
        <div class="modal modal-add">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Asignar al carro</p>
              <h2>{{ assignmentModalTitle }}</h2>
              <p class="muted">{{ assignmentModalHint }}</p>
            </div>
            <button class="ghost-button" type="button" @click="closeAssignmentModal">Cerrar</button>
          </div>

          <p v-if="assignmentModalError" class="error">{{ assignmentModalError }}</p>
          <Transition name="toast" mode="out-in">
            <p
              v-if="assignmentModalSuccess"
              :key="assignmentModalSuccess.id"
              class="success-banner"
              role="status"
              aria-live="polite"
            >
              {{ assignmentModalSuccess.message }}
            </p>
          </Transition>

          <div v-if="assignmentModalMode === 'traveler'" class="assignment-list">
            <div v-if="availableTravelersForSelectedWagon.length === 0" class="muted">
              No hay viajeros sin asignar.
            </div>
            <article
              v-for="traveler in availableTravelersForSelectedWagon"
              :key="traveler.id"
              class="assignment-item assignment-item--stacked"
            >
              <div>
                <strong>{{ traveler.fullName }}</strong>
                <p class="muted">{{ traveler.activeRoleName }}</p>
              </div>
              <button class="primary-button" type="button" :disabled="loading || submitting" @click="assignTravelerToSelectedWagon(traveler)">
                <span class="button-with-spinner">
                  <span v-if="isPending(`assign-traveler:${traveler.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending(`assign-traveler:${traveler.id}`) ? "Asignando…" : "Asignar al carro" }}</span>
                </span>
              </button>
            </article>
          </div>

          <div v-else class="assignment-list">
            <div v-if="assignmentModalMode === 'beast-traveler' && availableTravelerBeastsForSelectedWagon.length === 0" class="muted">
              No hay bestias sin asignar que quepan en este carro.
            </div>
            <div v-if="assignmentModalMode === 'beast-draft' && availableDraftBeastsForSelectedWagon.length === 0" class="muted">
              No hay bestias válidas para el tiro de este carro.
            </div>
            <article
              v-for="beast in assignmentModalMode === 'beast-traveler' ? availableTravelerBeastsForSelectedWagon : availableDraftBeastsForSelectedWagon"
              :key="beast.id"
              class="assignment-item assignment-item--stacked"
            >
              <div>
                <strong>{{ beast.name }}</strong>
                <p class="muted">
                  {{ beast.size }} · Fuerza {{ beast.strength }} ·
                  {{ beast.sourceType === "CATALOG" ? "Catálogo" : "Personalizada" }}
                </p>
              </div>
              <button
                class="primary-button"
                type="button"
                :disabled="loading || submitting"
                @click="assignBeastToSelectedWagon(beast, assignmentModalMode === 'beast-draft' ? 'DRAFT' : 'TRAVELER')"
              >
                <span class="button-with-spinner">
                  <span v-if="isPending(`assign-beast-${assignmentModalMode === 'beast-draft' ? 'draft' : 'traveler'}:${beast.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ assignmentModalMode === 'beast-draft' ? (isPending(`assign-beast-draft:${beast.id}`) ? "Asignando…" : "Asignar al tiro") : (isPending(`assign-beast-traveler:${beast.id}`) ? "Asignando…" : "Asignar como viajera") }}</span>
                </span>
              </button>
            </article>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="improvementModalOpen" class="modal-backdrop" @click.self="improvementModalOpen = false">
        <div class="modal modal-add">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Añadir mejora</p>
              <h2>{{ selectedWagon?.name }}</h2>
            </div>
            <button class="ghost-button" type="button" @click="improvementModalOpen = false">Cerrar</button>
          </div>

          <div class="filters">
            <label class="search-field">
              <span>Buscador</span>
              <input v-model="improvementSearch" type="search" placeholder="Buscar mejora" />
            </label>
            <div class="search-action-row">
              <button
                class="primary-button confirm-button"
                :class="{ loading: submitting }"
                type="button"
                :disabled="loading || submitting || !selectedImprovementItem || !selectedImprovementItem.available"
                @click="handleAddSelectedImprovement"
              >
                <span class="button-with-spinner">
                  <span v-if="isPending('add-improvement')" class="button-spinner" aria-hidden="true"></span>
                  <span class="confirm-label">{{ isPending('add-improvement') ? "Añadiendo…" : "Confirmar" }}</span>
                  <span v-if="!isPending('add-improvement')" class="confirm-arrow" aria-hidden="true">→</span>
                </span>
              </button>
            </div>
          </div>

          <div class="add-layout">
            <div class="catalog-list">
              <button
                v-for="item in visibleImprovementItems"
                :key="item.code"
                class="catalog-item"
                :class="{ selected: item.code === selectedImprovementItem?.code }"
                type="button"
                @click="selectedImprovementCode = item.code"
              >
                <div>
                  <strong>{{ item.name }}</strong>
                  <p class="muted">{{ item.category }}</p>
                  <p v-if="item.blockedReason" class="limit-warning">{{ item.blockedReason }}</p>
                  <p v-else class="limit-ok">
                    Disponible {{ item.repeatable ? `(${item.ownedCount}/${item.maxPerWagon})` : item.available ? "sí" : "no" }}
                  </p>
                </div>
                <div class="catalog-meta">
                  <span>{{ item.costExpression }}</span>
                  <span v-if="item.repeatable">Repetible</span>
                  <span v-else>Límite {{ item.maxPerWagon }}</span>
                </div>
              </button>
            </div>

            <div v-if="selectedImprovementItem" class="preview">
              <div class="preview-top">
                <div>
                  <h3>{{ selectedImprovementItem.name }}</h3>
                  <p class="muted">{{ selectedImprovementItem.category }}</p>
                </div>
              </div>

              <dl class="stats">
                <div><dt>Coste</dt><dd>{{ selectedImprovementItem.costExpression }}</dd></div>
                <div><dt>Máx.</dt><dd>{{ selectedImprovementItem.maxPerWagon }}</dd></div>
                <div><dt>Aplicadas</dt><dd>{{ selectedImprovementItem.ownedCount }}</dd></div>
                <div><dt>Repetible</dt><dd>{{ selectedImprovementItem.repeatable ? "Sí" : "No" }}</dd></div>
              </dl>

              <section class="info-block">
                <h4>Beneficio especial</h4>
                <p>{{ selectedImprovementItem.specialBenefit }}</p>
              </section>

              <section class="info-block">
                <h4>Descripción</h4>
                <p>{{ selectedImprovementItem.description }}</p>
              </section>

              <section v-if="selectedImprovementItem.notes" class="info-block">
                <h4>Notas</h4>
                <p>{{ selectedImprovementItem.notes }}</p>
              </section>

              <section class="info-block">
                <h4>Estado</h4>
                <p v-if="selectedImprovementItem.available" class="limit-ok">Disponible para añadir</p>
                <p v-else class="limit-warning">{{ selectedImprovementItem.blockedReason ?? "No disponible" }}</p>
              </section>
            </div>
          </div>
        </div>
      </div>
    </teleport>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 2rem;
}

.shell {
  width: min(1250px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 1.25rem;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.eyebrow {
  margin: 0 0 0.25rem;
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6b7280;
}

h1,
h2,
h3,
h4,
p {
  margin: 0;
}

.subtitle,
.muted {
  color: #6b7280;
}

.card {
  padding: 1.25rem;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: white;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.summary {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.summary-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.summary-stats div {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #f8fafc;
  min-width: 120px;
}

.summary-stats span {
  display: block;
  font-size: 0.8rem;
  color: #6b7280;
}

.summary-stats strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 1.1rem;
}

.grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 1.25rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1rem;
}

.section-header-summary {
  display: flex;
  justify-content: flex-end;
}

.filters {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 0.75rem;
  margin: 1rem 0;
}

.add-name-field {
  margin-top: 0.25rem;
}

.search-action-row {
  display: flex;
  align-items: end;
  gap: 0.75rem;
}

.filters label,
.search-field {
  display: grid;
  gap: 0.35rem;
}

.filters span {
  font-size: 0.85rem;
  color: #6b7280;
}

.filters select,
.filters input {
  width: 100%;
  padding: 0.75rem 0.9rem;
  border-radius: 0.8rem;
  border: 1px solid #d1d5db;
  font: inherit;
  background: white;
}

.catalog-list {
  display: grid;
  gap: 0.5rem;
  margin: 1rem 0;
  max-height: 500px;
  overflow: auto;
}

.catalog-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
  padding: 0.95rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.9rem;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.catalog-item.selected {
  border-color: #1d4ed8;
  background: #eff6ff;
}

.catalog-meta {
  display: grid;
  justify-items: end;
  color: #4b5563;
}

.limit-warning {
  color: #b91c1c;
  font-size: 0.88rem;
  margin-top: 0.2rem;
}

.limit-ok {
  color: #047857;
  font-size: 0.88rem;
  margin-top: 0.2rem;
}

.warning-block {
  padding: 0.9rem;
  border-radius: 0.85rem;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  display: grid;
  gap: 0.35rem;
}

.caravan-limit-alert {
  margin-top: 1rem;
}

.draft-power-badge {
  padding: 0.45rem 0.75rem;
  border-radius: 999px;
  font-weight: 700;
  border: 1px solid transparent;
}

.draft-boost-low {
  color: #92400e;
  background: #fef3c7;
  border-color: #f59e0b;
}

.draft-boost-mid {
  color: #166534;
  background: #dcfce7;
  border-color: #22c55e;
}

.draft-boost-high,
.limit-ok {
  color: #1d4ed8;
  background: #dbeafe;
  border-color: #93c5fd;
}

.draft-state-description {
  margin: 0;
  padding: 0.75rem 0.9rem;
  border-radius: 0.85rem;
  border: 1px solid transparent;
}

.draft-summary-cell {
  display: grid;
  gap: 0.45rem;
  min-width: 260px;
}

.capacity-meter,
.health-meter {
  position: relative;
  overflow: hidden;
  height: 0.55rem;
  border-radius: 999px;
  background: #e5e7eb;
}

.capacity-meter-fill,
.health-meter-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.capacity-meter-fill--travelers {
  background: linear-gradient(90deg, #bfdbfe, #3b82f6);
}

.capacity-meter-fill--cargo {
  background: linear-gradient(90deg, #fde68a, #f59e0b);
}

.health-meter-fill {
  background: linear-gradient(90deg, #86efac, #16a34a);
}

.draft-summary-line {
  margin: 0;
  font-size: 0.9rem;
  color: #334155;
}

.draft-strength-meter {
  display: grid;
  gap: 0.35rem;
}

.draft-strength-track {
  position: relative;
  display: flex;
  overflow: hidden;
  height: 0.85rem;
  border-radius: 999px;
  background: #e5e7eb;
}

.draft-band {
  display: block;
  height: 100%;
}

.draft-band--neutral {
  background: #94a3b8;
}

.draft-band--danger {
  background: linear-gradient(90deg, #fca5a5, #ef4444);
}

.draft-band--warning {
  background: linear-gradient(90deg, #fdba74, #f59e0b);
}

.draft-band--success {
  background: linear-gradient(90deg, #86efac, #22c55e);
}

.draft-band--boost {
  background: linear-gradient(90deg, #93c5fd, #3b82f6);
}

.draft-strength-marker {
  position: absolute;
  top: -0.2rem;
  width: 0.2rem;
  height: 1.25rem;
  border-radius: 999px;
  background: #111827;
  box-shadow: 0 0 0 2px #fff;
}

.draft-strength-labels {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  font-size: 0.72rem;
  color: #6b7280;
}

.alert-button {
  width: 100%;
  padding: 0.45rem 0.7rem;
  border-radius: 999px;
  border: 1px solid #f59e0b;
  background: #fffbeb;
  color: #92400e;
  font-weight: 700;
  cursor: pointer;
}

.alert-button.danger {
  border-color: #dc2626;
  background: #fef2f2;
  color: #b91c1c;
}

.modal-alerts {
  width: min(720px, 100%);
}

.alert-summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.75rem;
}

.alert-summary-grid div {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #f9fafb;
}

.alert-summary-grid span {
  display: block;
  font-size: 0.8rem;
  color: #6b7280;
}

.alert-summary-grid strong {
  display: block;
  margin-top: 0.2rem;
}

.alerts-list {
  display: grid;
  gap: 0.75rem;
}

.alert-item {
  display: grid;
  gap: 0.25rem;
  padding: 0.85rem 0.9rem;
  border-radius: 0.85rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.alert-item.warning {
  border-color: #f59e0b;
  background: #fffbeb;
}

.alert-item.danger {
  border-color: #dc2626;
  background: #fef2f2;
}

.alert-item.info {
  border-color: #3b82f6;
  background: #eff6ff;
}

.draft-collapsed-summary {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.draft-header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.draft-toggle-button {
  padding: 0.55rem 0.85rem;
}

.draft-beast-list {
  display: grid;
  gap: 0.75rem;
}

.draft-beast-card {
  display: flex;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.8rem 0.9rem;
  border-radius: 0.85rem;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.draft-beast-main {
  display: grid;
  gap: 0.25rem;
  min-width: 0;
  flex: 1;
}

.draft-beast-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.draft-beast-name-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.draft-base-strength {
  white-space: nowrap;
  text-align: right;
}

.draft-beast-stats {
  display: grid;
  justify-items: end;
  gap: 0.2rem;
  text-align: right;
  align-content: start;
  min-width: 0;
}

.draft-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2rem;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
  line-height: 1;
}

.draft-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.empty-catalog {
  padding: 0.6rem 0.2rem;
}

.empty-preview {
  min-height: 280px;
  border: 1px dashed #d1d5db;
  border-radius: 0.85rem;
}

.preview {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.preview-top {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.stats div {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #f9fafb;
}

dt {
  font-size: 0.8rem;
  color: #6b7280;
}

dd {
  margin: 0.2rem 0 0;
  font-size: 1rem;
  font-weight: 700;
}

.info-block {
  padding: 0.9rem;
  border-radius: 0.85rem;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  display: grid;
  gap: 0.35rem;
}

.success-banner {
  display: grid;
  gap: 0.25rem;
  padding: 0.9rem 1rem;
  border-radius: 0.9rem;
  background: #ecfdf5;
  border: 1px solid #86efac;
  color: #166534;
  box-shadow: 0 10px 24px rgba(34, 197, 94, 0.14);
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-0.4rem) scale(0.98);
}

.assigned-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.assigned-panel {
  display: grid;
  gap: 0.75rem;
  padding: 0.9rem;
  border-radius: 0.85rem;
  border: 1px solid #e5e7eb;
  background: white;
}

.assigned-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.assignment-list {
  display: grid;
  gap: 0.65rem;
  max-height: 320px;
  overflow: auto;
}

.assignment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.8rem 0.9rem;
  border-radius: 0.85rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.assignment-item--stacked {
  align-items: flex-start;
  flex-direction: column;
}

.assignment-item--stacked .primary-button {
  align-self: flex-end;
}

.improvements-accordion {
  gap: 0.75rem;
}

.accordion-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.accordion-title-button {
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.accordion-title {
  display: grid;
  gap: 0.15rem;
}

.accordion-state {
  color: #1d4ed8;
  font-weight: 700;
}

.accordion-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.improvements-list {
  display: grid;
  gap: 0.75rem;
}

.improvement-card {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.85rem;
  background: white;
}

.improvement-actions {
  display: flex;
  align-items: start;
}

.trash-button {
  width: 2.4rem;
  height: 2.4rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  border: 1px solid #fecaca;
  background: #fee2e2;
  color: #991b1b;
  cursor: pointer;
  font-size: 1.05rem;
}

.secondary-button.active {
  border-color: #1d4ed8;
  background: #eff6ff;
  color: #1d4ed8;
}

.table-wrap {
  overflow: auto;
  margin-top: 1rem;
}

.wagon-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.wagon-table colgroup col.wagon-col-name { width: 21%; }
.wagon-table colgroup col.wagon-col-life { width: 13%; }
.wagon-table colgroup col.wagon-col-dr { width: 8%; }
.wagon-table colgroup col.wagon-col-travelers { width: 14%; }
.wagon-table colgroup col.wagon-col-cargo { width: 14%; }
.wagon-table colgroup col.wagon-col-draft { width: 24%; }
.wagon-table colgroup col.wagon-col-alerts { width: 6%; }

.wagon-table th,
.wagon-table td {
  padding: 0.8rem;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  vertical-align: top;
}

.wagon-table th:first-child,
.wagon-table td:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
}

.wagon-table tbody tr {
  cursor: pointer;
}

.wagon-table tbody tr:hover {
  background: #f8fafc;
}

.empty-state,
.empty-state-inline {
  display: grid;
  place-items: center;
  text-align: center;
  gap: 0.5rem;
}

.primary-button,
.ghost-button,
.secondary-button,
.primary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.85rem;
  font: inherit;
}

.primary-button,
.secondary-button,
.ghost-button {
  padding: 0.8rem 1rem;
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.primary-button {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: white;
}

.confirm-button {
  min-width: 180px;
  align-self: end;
  justify-content: space-between;
  gap: 0.65rem;
  padding: 0.95rem 1.2rem;
  border-radius: 1rem;
  box-shadow:
    0 10px 24px rgba(29, 78, 216, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
  background: linear-gradient(180deg, #2f66ff 0%, #224ed6 100%);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.18);
  -webkit-text-fill-color: #ffffff;
}

.confirm-button:not(:disabled),
.confirm-button:not(:disabled) .confirm-label,
.confirm-button:not(:disabled) .confirm-arrow {
  color: #ffffff;
  -webkit-text-fill-color: #ffffff;
}

.confirm-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 14px 28px rgba(29, 78, 216, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.confirm-button:focus-visible {
  outline: 3px solid rgba(59, 130, 246, 0.35);
  outline-offset: 2px;
}

.primary-button.disabled {
  background: #9ca3af;
  border-color: #9ca3af;
  color: #f9fafb;
  cursor: not-allowed;
  text-shadow: none;
  -webkit-text-fill-color: #f9fafb;
}

.primary-button.loading {
  opacity: 0.95;
}

.spinner {
  width: 0.95rem;
  height: 0.95rem;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: white;
  display: inline-block;
  animation: spin 0.8s linear infinite;
  margin-right: 0.5rem;
}

.confirm-label {
  white-space: nowrap;
}

.confirm-arrow {
  font-size: 1.1rem;
  line-height: 1;
}

.ghost-button {
  background: white;
}

.secondary-button {
  background: #f8fafc;
  border-color: #d1d5db;
}

.primary-link {
  padding: 0.8rem 1rem;
  background: #1d4ed8;
  color: white;
  text-decoration: none;
}

.error {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  background: #fef2f2;
  color: #b91c1c;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  padding: 1.5rem;
}

.modal {
  width: min(900px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: white;
  border-radius: 1.2rem;
  padding: 1.25rem;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
  display: grid;
  gap: 1rem;
}

.modal-add {
  width: min(1100px, 100%);
}

.modal-form {
  display: grid;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid #dbe3f0;
  border-radius: 1rem;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.modal-form label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.modal-form input,
.modal-form select {
  width: 100%;
  padding: 0.85rem 0.95rem;
  border-radius: 0.9rem;
  border: 1px solid #cbd5e1;
  background: white;
  font: inherit;
  color: #0f172a;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.04);
}

.modal-form input:focus,
.modal-form select:focus {
  outline: none;
  border-color: #1d4ed8;
  box-shadow:
    0 0 0 3px rgba(29, 78, 216, 0.14),
    inset 0 1px 2px rgba(15, 23, 42, 0.04);
}

.modal-form select {
  min-height: 3rem;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 0.9rem;
  border: 1px solid #dbe3f0;
  border-radius: 0.9rem;
  background: #f8fafc;
}

.checkbox-field input {
  width: 1rem;
  height: 1rem;
  margin: 0;
}

.health-summary {
  display: grid;
  gap: 0.25rem;
  padding: 1rem 1.1rem;
  border: 1px solid #dbe3f0;
  border-radius: 1rem;
  background: #f8fafc;
}

.life-mini-button {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #111827;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.life-mini-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.wagon-health-card {
  gap: 0.85rem;
}

.wagon-health-controls {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.75rem;
}

.wagon-health-meter {
  height: 2.5rem;
  padding: 0.35rem 0.45rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.85rem;
  background: #fff;
}

.wagon-health-values {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  font-size: 0.92rem;
  color: #475569;
}

.wagon-health-values strong {
  color: #111827;
}

.summary-meter-block {
  display: grid;
  gap: 0.5rem;
  margin-bottom: 0.85rem;
}

.meter-strip {
  display: flex;
  overflow: hidden;
  min-height: 0.9rem;
  border-radius: 999px;
  background: #e5e7eb;
}

.meter-strip--strength {
  min-height: 0.85rem;
}

.meter-segment {
  display: block;
  height: 100%;
}

.meter-legend {
  display: grid;
  grid-template-columns: auto auto auto auto auto auto;
  gap: 0.45rem 0.75rem;
  align-items: center;
  font-size: 0.78rem;
  color: #475569;
}

.meter-legend span:nth-child(odd) {
  color: #64748b;
}

.two-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.add-layout {
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  gap: 1rem;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.detail-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.wagon-title-row {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.name-edit-button {
  padding: 0.45rem 0.6rem;
  min-height: 2.2rem;
  min-width: 2.2rem;
  line-height: 1;
}

.name-editor-card {
  display: grid;
  gap: 0.9rem;
  padding: 1rem 1rem 0.95rem;
  border: 1px solid #dbe3f0;
  border-radius: 1rem;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.name-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.name-editor-field {
  display: grid;
  gap: 0.4rem;
}

.name-editor-field span {
  font-size: 0.88rem;
  font-weight: 600;
  color: #475569;
}

.name-editor-field input,
.search-field input,
.search-field select,
.styled-select {
  width: 100%;
  padding: 0.85rem 0.95rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.9rem;
  background: white;
  font: inherit;
  font-size: 1rem;
  color: #0f172a;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.04);
}

.name-editor-field input:focus,
.search-field input:focus,
.search-field select:focus,
.styled-select:focus {
  outline: none;
  border-color: #1d4ed8;
  box-shadow:
    0 0 0 3px rgba(29, 78, 216, 0.14),
    inset 0 1px 2px rgba(15, 23, 42, 0.04);
}

.styled-select {
  min-height: 3rem;
  appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #475569 50%),
    linear-gradient(135deg, #475569 50%, transparent 50%);
  background-position:
    calc(100% - 1.15rem) calc(50% - 0.15rem),
    calc(100% - 0.85rem) calc(50% - 0.15rem);
  background-size: 0.42rem 0.42rem, 0.42rem 0.42rem;
  background-repeat: no-repeat;
  padding-right: 2.2rem;
}

.danger-button {
  padding: 0.8rem 1rem;
  border-radius: 0.85rem;
  border: 1px solid #fecaca;
  background: #fee2e2;
  color: #991b1b;
  cursor: pointer;
  font: inherit;
  font-weight: 600;
}

.danger-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-stats {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.25rem;
}

.meta-line {
  font-size: 0.9rem;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .grid,
  .summary,
  .preview-top,
  .modal-header,
  .add-layout {
    grid-template-columns: 1fr;
  }

  .modal-form {
    padding: 0.9rem;
  }

  .grid,
  .summary,
  .name-editor-header {
    grid-template-columns: 1fr;
  }

  .grid,
  .summary,
  .name-editor-header {
    display: grid;
  }

  .summary-actions {
    width: 100%;
    justify-content: space-between;
  }

  .name-editor-header {
    align-items: stretch;
  }

  .filters {
    grid-template-columns: 1fr;
  }

  .wagon-health-controls {
    grid-template-columns: 1fr;
  }

  .wagon-health-values {
    flex-direction: column;
  }

  .search-action-row {
    align-items: stretch;
    flex-direction: column;
  }

  .alert-summary-grid {
    grid-template-columns: 1fr;
  }

  .draft-summary-cell {
    min-width: 0;
  }
}

@media (max-width: 700px) {
  .page {
    padding: 1rem;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats,
  .summary-stats,
  .modal-stats,
  .two-columns {
    grid-template-columns: 1fr;
  }

  .summary,
  .summary-actions,
  .modal-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
