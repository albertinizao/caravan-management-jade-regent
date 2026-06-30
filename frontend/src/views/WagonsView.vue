<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { useToast } from "@/composables/useToast";
import { getActiveCaravan, listCaravans } from "@/services/caravans";
import { listCaravanBeasts, updateCaravanBeastAssignment, clearCaravanBeastAssignment } from "@/services/beasts";
import {
  addCaravanWagon,
  addCaravanWagonImprovement,
  deleteCaravanWagon,
  deleteCaravanWagonImprovement,
  getCaravanWagon,
  listCaravanWagons,
  listWagonCatalog,
  listWagonImprovementCatalog,
} from "@/services/wagons";
import { listCaravanTravelers, updateCaravanTravelerWagon } from "@/services/travelers";
import type { Caravan } from "@/types/caravan";
import type { CaravanBeast } from "@/types/beast";
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
const loading = ref(true);
const submitting = ref(false);
const pendingAction = ref<string | null>(null);
const error = ref<string | null>(null);
const addModalOpen = ref(false);
const selectedCatalogCode = ref<string | null>(null);
const selectedWagon = ref<CaravanWagon | null>(null);
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
const addTypeFilter = ref<"all" | "viajeros" | "mercancias" | "especiales">("all");
const addSearch = ref("");
const improvementSearch = ref("");
const wagonsTypeFilter = ref<"all" | "viajeros" | "mercancias" | "especiales">("all");
const wagonsSearch = ref("");
const draftMediumSlotsPerLargeBeast = 4;
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
const selectedCatalogNeedsOverride = computed(() =>
  selectedCatalogItem.value ? isLimitExceeded(selectedCatalogItem.value) : false
);
const visibleCatalogItems = computed(() =>
  catalog.value.filter((item) => matchesTypeFilter(item.category, addTypeFilter.value))
    .filter((item) => matchesSearch(item.name, addSearch.value))
);
const visibleWagons = computed(() =>
  wagons.value.filter((item) => matchesTypeFilter(item.category, wagonsTypeFilter.value))
    .filter((item) => matchesSearch(item.name, wagonsSearch.value))
);

const selectedWagonTravelers = computed(() =>
  selectedWagon.value ? travelers.value.filter((traveler) => traveler.wagonId === selectedWagon.value?.id) : []
);

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

const unassignedTravelers = computed(() => travelers.value.filter((traveler) => !traveler.wagonId));

const unassignedBeasts = computed(() => beasts.value.filter((beast) => beast.assignmentType === "NONE"));

const selectedWagonOccupancy = computed(() => selectedWagonTravelers.value.length + selectedWagonTravelerBeasts.value.length);
const selectedWagonCapacityRemaining = computed(() =>
  selectedWagon.value ? Math.max(0, selectedWagon.value.travelerCapacity - selectedWagonOccupancy.value) : 0
);
const selectedWagonDraftRequirement = computed(() =>
  selectedWagon.value ? parseDraftRequirement(selectedWagon.value.propulsion) : null,
);
const availableTravelersForSelectedWagon = computed(() =>
  selectedWagonCapacityRemaining.value > 0 ? unassignedTravelers.value : []
);
const availableTravelerBeastsForSelectedWagon = computed(() =>
  selectedWagonCapacityRemaining.value > 0 ? unassignedBeasts.value : []
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
    return selectedWagonCapacityRemaining.value === 0
      ? "Este carro ya no tiene plazas libres para viajeros."
      : `Se mostrarán solo viajeros sin asignar. Quedan ${selectedWagonCapacityRemaining.value} plazas libres en este carro.`;
  }

  if (assignmentModalMode.value === "beast-traveler") {
    return selectedWagonCapacityRemaining.value === 0
      ? "Este carro ya no tiene plazas libres para bestias viajeras."
      : `Se mostrarán solo bestias sin asignar. Quedan ${selectedWagonCapacityRemaining.value} plazas libres en este carro.`;
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
      const [catalogResponse, wagonsResponse, travelerResponse, beastResponse] = await Promise.all([
        listWagonCatalog(activeResponse.caravan.id),
        listCaravanWagons(activeResponse.caravan.id),
        listCaravanTravelers(activeResponse.caravan.id),
        listCaravanBeasts(activeResponse.caravan.id),
      ]);

      catalog.value = catalogResponse;
      wagons.value = wagonsResponse;
      travelers.value = travelerResponse;
      beasts.value = beastResponse;

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
        } else {
          closeModal();
        }
      }
    } else {
      catalog.value = [];
      wagons.value = [];
      travelers.value = [];
      beasts.value = [];
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

  submitting.value = true;
  pendingAction.value = "add-wagon";
  error.value = null;

  try {
    await addCaravanWagon(activeCaravan.value.id, { wagonTypeCode: selectedCatalogItem.value.code });
    await refresh();
    addModalOpen.value = false;
    selectedCatalogCode.value = selectedCatalogItem.value.code;
    showToast(`Carro añadido: ${selectedCatalogItem.value.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to add wagon";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function openAddModal() {
  selectedCatalogCode.value = null;
  addModalOpen.value = true;
}

function selectCatalogItem(code: string) {
  selectedCatalogCode.value = code;
}

async function openWagonDetails(wagon: CaravanWagon) {
  if (!activeCaravan.value) {
    return;
  }

  try {
    selectedWagon.value = await getCaravanWagon(activeCaravan.value.id, wagon.id);
    improvementModalOpen.value = false;
    selectedImprovementCode.value = null;
    improvementSearch.value = "";
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load wagon details";
  }
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

  if (selectedWagonCapacityRemaining.value <= 0) {
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

  if (assignmentType === "TRAVELER" && selectedWagonCapacityRemaining.value <= 0) {
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

        <section class="card">
          <div class="section-header">
            <div>
              <h2>Carros de la caravana</h2>
              <p class="muted">Haz clic en un carro para ver todos sus detalles</p>
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
              <thead>
                <tr>
                  <th>Carro</th>
                  <th>Categoría</th>
                  <th>PG</th>
                  <th>Dureza</th>
                  <th>Viajeros</th>
                  <th>Carga</th>
                  <th>Consumo</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="wagon in visibleWagons"
                  :key="wagon.id"
                  tabindex="0"
                  role="button"
                  @click="openWagonDetails(wagon)"
                  @keyup.enter="openWagonDetails(wagon)"
                >
                  <td>
                    <strong>{{ wagon.name }}</strong>
                    <p class="muted">{{ wagon.specialBenefit }}</p>
                  </td>
                  <td>{{ wagon.category }}</td>
                  <td>{{ wagon.hitPoints }}</td>
                  <td>{{ wagon.hardness }}</td>
                  <td>{{ wagon.travelerCapacity }}</td>
                  <td>{{ wagon.cargoCapacity }}</td>
                  <td>{{ wagon.consumption }}</td>
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
                :class="{ disabled: submitting || !selectedCatalogItem || selectedCatalogNeedsOverride, loading: submitting }"
                type="button"
                :disabled="loading || submitting || !selectedCatalogItem || selectedCatalogNeedsOverride"
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
              <h2>{{ selectedWagon.name }}</h2>
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

          <dl class="stats modal-stats">
            <div><dt>Tipo</dt><dd>{{ selectedWagon.wagonTypeCode }}</dd></div>
            <div><dt>Coste</dt><dd>{{ selectedWagon.cost }} po</dd></div>
            <div><dt>PG</dt><dd>{{ selectedWagon.hitPoints }}</dd></div>
            <div><dt>Dureza</dt><dd>{{ selectedWagon.hardness }}</dd></div>
            <div><dt>Propulsión</dt><dd>{{ selectedWagon.propulsion }}</dd></div>
            <div><dt>Viajeros</dt><dd>{{ selectedWagon.travelerCapacity }}</dd></div>
            <div><dt>Carga</dt><dd>{{ selectedWagon.cargoCapacity }}</dd></div>
            <div><dt>Límite</dt><dd>{{ selectedWagon.limit }}</dd></div>
            <div><dt>Consumo</dt><dd>{{ selectedWagon.consumption }}</dd></div>
          </dl>

          <section class="info-block">
            <div class="section-header">
              <div>
                <h3>Tripulación asignada</h3>
                <p class="muted">
                  {{ selectedWagonOccupancy }}/{{ selectedWagon.travelerCapacity }} plazas ocupadas entre viajeros y bestias viajeras.
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
              No hay bestias sin asignar.
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

.filters {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 0.75rem;
  margin: 1rem 0;
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
}

.wagon-table th,
.wagon-table td {
  padding: 0.8rem;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  vertical-align: top;
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

  .grid,
  .summary {
    display: grid;
  }

  .stats,
  .summary-stats,
  .modal-stats {
    grid-template-columns: 1fr 1fr;
  }

  .summary-actions {
    width: 100%;
    justify-content: space-between;
  }

  .filters {
    grid-template-columns: 1fr;
  }

  .search-action-row {
    align-items: stretch;
    flex-direction: column;
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
  .modal-stats {
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
