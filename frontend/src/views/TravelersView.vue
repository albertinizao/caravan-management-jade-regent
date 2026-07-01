<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";

import { useToast } from "@/composables/useToast";
import { getActiveCaravan, listCaravans } from "@/services/caravans";
import { listCaravanWagons } from "@/services/wagons";
import {
  addCaravanTraveler,
  deleteCaravanTraveler,
  getCaravanTraveler,
  listCaravanTravelers,
  listTravelerRoleCatalog,
  updateCaravanTraveler,
  updateCaravanTravelerRole,
  updateCaravanTravelerWagon,
} from "@/services/travelers";
import type { Caravan } from "@/types/caravan";
import type { CaravanTraveler, TravelerRoleCatalogItem } from "@/types/traveler";
import type { CaravanWagon } from "@/types/wagon";

const caravans = ref<Caravan[]>([]);
const activeCaravan = ref<Caravan | null>(null);
const travelers = ref<CaravanTraveler[]>([]);
const wagons = ref<CaravanWagon[]>([]);
const roleCatalog = ref<TravelerRoleCatalogItem[]>([]);
const loading = ref(true);
const submitting = ref(false);
const pendingAction = ref<string | null>(null);
const error = ref<string | null>(null);
const search = ref("");
const roleFilter = ref("all");
const wagonFilter = ref("all");
const selectedTraveler = ref<CaravanTraveler | null>(null);
const travelerMode = ref<"view" | "edit">("view");
const selectedModalError = ref<string | null>(null);
const selectedFullName = ref("");
const selectedDescription = ref("");
const selectedWagonId = ref("");
const selectedAvailableRoleCodes = ref<string[]>([]);
const selectedRoleCodes = ref<string[]>([]);
const selectedMaxActiveRoleCount = ref(1);
const selectedRoleTargetTravelerId = ref("");
const selectedSalary = ref("");
const selectedContractConditions = ref("");
const selectedConsumption = ref("1");
const createModalOpen = ref(false);
const createModalError = ref<string | null>(null);
const createFullName = ref("");
const createDescription = ref("");
const createAvailableRoleCodes = ref<string[]>([]);
const createActiveRoleCodes = ref<string[]>([]);
const createMaxActiveRoleCount = ref(1);
const createSalary = ref("");
const createContractConditions = ref("");
const createConsumption = ref("1");
const createServedTravelerId = ref("");
const { showToast } = useToast();
const gpFormatter = new Intl.NumberFormat("es-ES", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

function isPending(action: string) {
  return pendingAction.value === action;
}

const roleCatalogByCode = computed(() =>
  Object.fromEntries(roleCatalog.value.map((item) => [item.code, item] as const)),
);

const selectedPrimaryRoleCode = computed(() => selectedRoleCodes.value[0] ?? "");
const selectedTravelerRole = computed(() => roleCatalogByCode.value[selectedPrimaryRoleCode.value] ?? null);
const createPrimaryRoleCode = computed(() => createActiveRoleCodes.value[0] ?? "");
const createActiveRole = computed(() => roleCatalogByCode.value[createPrimaryRoleCode.value] ?? null);
const selectedTargetRole = computed(
  () => selectedRoleCodes.value.map((code) => roleCatalogByCode.value[code]).find((role) => role?.requiresTargetTraveler) ?? null,
);
const createTargetRole = computed(
  () => createActiveRoleCodes.value.map((code) => roleCatalogByCode.value[code]).find((role) => role?.requiresTargetTraveler) ?? null,
);

function syncSelectedTravelerDraft(traveler: CaravanTraveler) {
  selectedFullName.value = traveler.fullName;
  selectedDescription.value = traveler.description ?? "";
  selectedWagonId.value = traveler.wagonId ?? "";
  selectedAvailableRoleCodes.value = [...traveler.availableRoleCodes];
  selectedRoleCodes.value = [...traveler.activeRoleCodes];
  selectedMaxActiveRoleCount.value = traveler.maxActiveRoleCount;
  selectedRoleTargetTravelerId.value = traveler.servedTravelerId ?? "";
  selectedSalary.value = traveler.salary === null ? "" : String(traveler.salary);
  selectedContractConditions.value = traveler.contractConditions ?? "";
  selectedConsumption.value = String(traveler.consumption);
}

const visibleTravelers = computed(() => {
  const query = search.value.trim().toLowerCase();
  return travelers.value
    .filter((traveler) => (query ? traveler.fullName.toLowerCase().includes(query) : true))
    .filter((traveler) =>
      roleFilter.value === "all" || traveler.activeRoleCodes.includes(roleFilter.value) || traveler.activeRoleCode === roleFilter.value,
    )
    .filter((traveler) => wagonFilter.value === "all" || traveler.wagonId === wagonFilter.value);
});

const travelersWithoutWagon = computed(() => travelers.value.filter((traveler) => !traveler.wagonId));

const createRoleOptions = computed(() =>
  roleCatalog.value.filter((role) => createAvailableRoleCodes.value.includes(role.code)),
);
const selectedRoleOptions = computed(() =>
  roleCatalog.value.filter((role) => selectedAvailableRoleCodes.value.includes(role.code)),
);

const createTargetTravelerOptions = computed(() => travelers.value);
const selectedTargetTravelerOptions = computed(() =>
  travelers.value.filter((traveler) => traveler.id !== selectedTraveler.value?.id),
);

const roleFilterOptions = computed(() => roleCatalog.value);

const passengerRoleCode = "pasajero";

watch(createAvailableRoleCodes, () => {
  if (!createAvailableRoleCodes.value.includes(passengerRoleCode)) {
    createAvailableRoleCodes.value = [...createAvailableRoleCodes.value, passengerRoleCode];
  }
  createActiveRoleCodes.value = createActiveRoleCodes.value.filter((code) =>
    createAvailableRoleCodes.value.includes(code),
  );
  if (createMaxActiveRoleCount.value < createActiveRoleCodes.value.length) {
    createMaxActiveRoleCount.value = createActiveRoleCodes.value.length;
  }
}, { deep: true });

watch(createActiveRoleCodes, () => {
  if (!createTargetRole.value?.requiresTargetTraveler) {
    createServedTravelerId.value = "";
  }
}, { deep: true });

watch(selectedRoleCodes, () => {
  if (!selectedTargetRole.value?.requiresTargetTraveler) {
    selectedRoleTargetTravelerId.value = "";
  }
}, { deep: true });

watch(selectedAvailableRoleCodes, () => {
  if (!selectedAvailableRoleCodes.value.includes(passengerRoleCode)) {
    selectedAvailableRoleCodes.value = [...selectedAvailableRoleCodes.value, passengerRoleCode];
  }
  selectedRoleCodes.value = selectedRoleCodes.value.filter((code) =>
    selectedAvailableRoleCodes.value.includes(code),
  );
  if (selectedMaxActiveRoleCount.value < selectedRoleCodes.value.length) {
    selectedMaxActiveRoleCount.value = selectedRoleCodes.value.length;
  }
}, { deep: true });

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
      const [travelerList, wagonList, roleList] = await Promise.all([
        listCaravanTravelers(activeResponse.caravan.id),
        listCaravanWagons(activeResponse.caravan.id),
        listTravelerRoleCatalog(activeResponse.caravan.id),
      ]);
      travelers.value = travelerList;
      wagons.value = wagonList;
      roleCatalog.value = roleList;

      if (selectedTraveler.value) {
        const refreshed = travelerList.find((traveler) => traveler.id === selectedTraveler.value?.id);
        if (refreshed) {
          selectedTraveler.value = refreshed;
          selectedModalError.value = null;
          syncSelectedTravelerDraft(refreshed);
        }
      }
    } else {
      travelers.value = [];
      wagons.value = [];
      roleCatalog.value = [];
      selectedTraveler.value = null;
      travelerMode.value = "view";
      selectedModalError.value = null;
      selectedFullName.value = "";
      selectedDescription.value = "";
      selectedWagonId.value = "";
      selectedAvailableRoleCodes.value = [];
      selectedRoleCodes.value = [];
      selectedMaxActiveRoleCount.value = 1;
      selectedRoleTargetTravelerId.value = "";
      selectedSalary.value = "";
      selectedContractConditions.value = "";
      selectedConsumption.value = "1";
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load travelers";
  } finally {
    loading.value = false;
    pendingAction.value = trackRefresh ? null : previousAction;
  }
}

function openCreateModal() {
  createModalOpen.value = true;
  createModalError.value = null;
  createFullName.value = "";
  createDescription.value = "";
  createAvailableRoleCodes.value = [passengerRoleCode];
  createActiveRoleCodes.value = [];
  createMaxActiveRoleCount.value = 1;
  createSalary.value = "";
  createContractConditions.value = "";
  createConsumption.value = "1";
  createServedTravelerId.value = "";
}

function closeCreateModal() {
  createModalOpen.value = false;
  createModalError.value = null;
}

async function handleCreateTraveler() {
  if (!activeCaravan.value) {
    return;
  }

  const travelerName = createFullName.value.trim();
  if (!createFullName.value.trim()) {
    error.value = "El nombre completo es obligatorio";
    return;
  }

  if (!createAvailableRoleCodes.value.includes(passengerRoleCode)) {
    createAvailableRoleCodes.value = [...createAvailableRoleCodes.value, passengerRoleCode];
  }

  if (createAvailableRoleCodes.value.length === 0) {
    createModalError.value = "Debes seleccionar al menos un rol";
    return;
  }

  if (createActiveRoleCodes.value.some((code) => !createAvailableRoleCodes.value.includes(code))) {
    createModalError.value = "Los roles activos deben estar dentro de los roles disponibles";
    return;
  }

  if (createActiveRoleCodes.value.length > createMaxActiveRoleCount.value) {
    createModalError.value = "Los roles activos no pueden superar el máximo permitido";
    return;
  }

  if (createTargetRole.value?.requiresTargetTraveler && !createServedTravelerId.value) {
    createModalError.value = "Debes seleccionar el viajero al que sirve";
    return;
  }

  submitting.value = true;
  pendingAction.value = "create";
  error.value = null;

  try {
    const salaryText = String(createSalary.value).trim().replace(",", ".");
    const consumptionText = String(createConsumption.value).trim();
    const parsedSalary = salaryText ? Number(salaryText) : null;
    const parsedConsumption = consumptionText ? Number(consumptionText) : 1;

    if (parsedSalary !== null && Number.isNaN(parsedSalary)) {
      throw new Error("El sueldo debe ser un número válido");
    }
    if (parsedSalary !== null && !/^\d+(\.\d{1,2})?$/.test(salaryText)) {
      throw new Error("El sueldo debe tener como máximo 2 decimales");
    }
    if (Number.isNaN(parsedConsumption) || parsedConsumption < 1) {
      throw new Error("El consumo debe ser un número mayor o igual a 1");
    }

    await addCaravanTraveler(activeCaravan.value.id, {
      fullName: travelerName,
      description: createDescription.value.trim() || undefined,
      availableRoleCodes: [passengerRoleCode, ...createAvailableRoleCodes.value.filter((code) => code !== passengerRoleCode)],
      activeRoleCodes: createActiveRoleCodes.value.length === 0
        ? [passengerRoleCode]
        : createActiveRoleCodes.value,
      activeRoleCode: createPrimaryRoleCode.value || passengerRoleCode,
      maxActiveRoleCount: createMaxActiveRoleCount.value,
      salary: parsedSalary,
      contractConditions: createContractConditions.value.trim() || null,
      consumption: parsedConsumption,
      servedTravelerId: createServedTravelerId.value || null,
    });
    closeCreateModal();
    await refresh();
    showToast(`Viajero añadido: ${travelerName}.`);
  } catch (cause) {
    createModalError.value = cause instanceof Error ? cause.message : "Failed to create traveler";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function openTraveler(traveler: CaravanTraveler) {
  if (!activeCaravan.value) {
    return;
  }

  try {
    const detailed = await getCaravanTraveler(activeCaravan.value.id, traveler.id);
    selectedTraveler.value = detailed;
    travelerMode.value = "view";
    selectedModalError.value = null;
    syncSelectedTravelerDraft(detailed);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load traveler details";
  }
}

function closeTraveler() {
  selectedTraveler.value = null;
  travelerMode.value = "view";
  selectedModalError.value = null;
  selectedFullName.value = "";
  selectedDescription.value = "";
  selectedWagonId.value = "";
  selectedAvailableRoleCodes.value = [];
  selectedRoleCodes.value = [];
  selectedMaxActiveRoleCount.value = 1;
  selectedRoleTargetTravelerId.value = "";
  selectedSalary.value = "";
  selectedContractConditions.value = "";
  selectedConsumption.value = "1";
}

function enterEditMode() {
  if (!selectedTraveler.value) {
    return;
  }
  travelerMode.value = "edit";
  selectedModalError.value = null;
  syncSelectedTravelerDraft(selectedTraveler.value);
}

function cancelEditMode() {
  if (!selectedTraveler.value) {
    return;
  }
  travelerMode.value = "view";
  selectedModalError.value = null;
  syncSelectedTravelerDraft(selectedTraveler.value);
}

async function handleAssignWagon() {
  if (!activeCaravan.value || !selectedTraveler.value) {
    return;
  }

  if (!selectedWagonId.value) {
    selectedModalError.value = "Debes seleccionar un carro";
    return;
  }

  submitting.value = true;
  pendingAction.value = "assign-wagon";
  error.value = null;

  try {
    const updated = await updateCaravanTravelerWagon(activeCaravan.value.id, selectedTraveler.value.id, {
      wagonId: selectedWagonId.value,
    });
    selectedTraveler.value = updated;
    await refresh();
    showToast(`Viajero asignado al carro: ${updated.fullName}.`);
  } catch (cause) {
    selectedModalError.value = cause instanceof Error ? cause.message : "Failed to update traveler wagon";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleDeleteTraveler() {
  if (!activeCaravan.value || !selectedTraveler.value) {
    return;
  }

  const confirmed = window.confirm(
    `¿Seguro que quieres eliminar a ${selectedTraveler.value.fullName}? Esta acción no se puede deshacer.`,
  );
  if (!confirmed) {
    return;
  }

  submitting.value = true;
  pendingAction.value = "delete";
  selectedModalError.value = null;

  try {
    await deleteCaravanTraveler(activeCaravan.value.id, selectedTraveler.value.id);
    const deletedName = selectedTraveler.value.fullName;
    closeTraveler();
    await refresh();
    showToast(`Viajero eliminado: ${deletedName}.`);
  } catch (cause) {
    selectedModalError.value = cause instanceof Error ? cause.message : "Failed to delete traveler";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleSaveTravelerChanges() {
  if (!activeCaravan.value || !selectedTraveler.value) {
    return;
  }

  if (!selectedFullName.value.trim()) {
    selectedModalError.value = "El nombre completo es obligatorio";
    return;
  }

  if (selectedRoleCodes.value.length === 0) {
    selectedRoleTargetTravelerId.value = "";
  }

  const targetRole = selectedTargetRole.value;
  if (targetRole?.requiresTargetTraveler && !selectedRoleTargetTravelerId.value) {
    selectedModalError.value = "Debes seleccionar el viajero al que sirve";
    return;
  }

  const salaryText = String(selectedSalary.value).trim().replace(",", ".");
  const consumptionText = String(selectedConsumption.value).trim();
  const parsedSalary = salaryText ? Number(salaryText) : null;
  const parsedConsumption = consumptionText ? Number(consumptionText) : selectedTraveler.value.consumption;

  if (parsedSalary !== null && Number.isNaN(parsedSalary)) {
    selectedModalError.value = "El sueldo debe ser un número válido";
    return;
  }
  if (parsedSalary !== null && !/^\d+(\.\d{1,2})?$/.test(salaryText)) {
    selectedModalError.value = "El sueldo debe tener como máximo 2 decimales";
    return;
  }
  if (Number.isNaN(parsedConsumption) || parsedConsumption < 1) {
    selectedModalError.value = "El consumo debe ser un número mayor o igual a 1";
    return;
  }

  submitting.value = true;
  pendingAction.value = "save";
  selectedModalError.value = null;

  try {
    const updated = await updateCaravanTraveler(activeCaravan.value.id, selectedTraveler.value.id, {
      fullName: selectedFullName.value.trim(),
      description: selectedDescription.value.trim() || null,
      availableRoleCodes: selectedAvailableRoleCodes.value,
      activeRoleCodes: selectedRoleCodes.value.length === 0
        ? [passengerRoleCode]
        : selectedRoleCodes.value,
      activeRoleCode: selectedRoleCodes.value.length === 0 ? passengerRoleCode : selectedPrimaryRoleCode.value,
      maxActiveRoleCount: selectedMaxActiveRoleCount.value,
      wagonId: selectedWagonId.value || null,
      salary: parsedSalary,
      contractConditions: selectedContractConditions.value.trim() || null,
      consumption: parsedConsumption,
      servedTravelerId: targetRole?.requiresTargetTraveler ? selectedRoleTargetTravelerId.value : null,
    });
    selectedTraveler.value = updated;
    syncSelectedTravelerDraft(updated);
    travelerMode.value = "view";
    await refresh();
    showToast(`Viajero actualizado: ${updated.fullName}.`);
  } catch (cause) {
    selectedModalError.value = cause instanceof Error ? cause.message : "Failed to update traveler";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleUpdateRole() {
  if (!activeCaravan.value || !selectedTraveler.value) {
    return;
  }

  if (selectedRoleCodes.value.length === 0) {
    selectedRoleTargetTravelerId.value = "";
  }

  const targetRole = selectedTargetRole.value;
  if (targetRole?.requiresTargetTraveler && !selectedRoleTargetTravelerId.value) {
    selectedModalError.value = "Debes seleccionar el viajero al que sirve";
    return;
  }

  submitting.value = true;
  pendingAction.value = "update-role";
  error.value = null;

  try {
    const updated = await updateCaravanTravelerRole(activeCaravan.value.id, selectedTraveler.value.id, {
      activeRoleCodes: selectedRoleCodes.value.length === 0
        ? [passengerRoleCode]
        : selectedRoleCodes.value,
      activeRoleCode: selectedRoleCodes.value.length === 0 ? passengerRoleCode : selectedPrimaryRoleCode.value,
      maxActiveRoleCount: selectedMaxActiveRoleCount.value,
      servedTravelerId: targetRole?.requiresTargetTraveler ? selectedRoleTargetTravelerId.value : null,
    });
    selectedTraveler.value = updated;
    await refresh();
  } catch (cause) {
    selectedModalError.value = cause instanceof Error ? cause.message : "Failed to update traveler role";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleRoleSelection(roleCode: string) {
  if (!selectedTraveler.value) {
    return;
  }

  const currentlySelected = selectedRoleCodes.value.includes(roleCode);
  if (currentlySelected) {
    selectedRoleCodes.value = selectedRoleCodes.value.filter((code) => code !== roleCode);
    selectedModalError.value = null;
    if (selectedRoleTargetTravelerId.value && !roleCatalogByCode.value[roleCode]?.requiresTargetTraveler) {
      selectedRoleTargetTravelerId.value = "";
    }
    return;
  }

  if (selectedRoleCodes.value.length >= selectedMaxActiveRoleCount.value) {
    selectedModalError.value = "Aumenta el máximo de roles simultáneos antes de activar otro rol";
    return;
  }

  selectedRoleCodes.value = [...selectedRoleCodes.value, roleCode];
  selectedModalError.value = null;
}

function increaseRoleLimit() {
  selectedMaxActiveRoleCount.value += 1;
  selectedModalError.value = null;
}

function decreaseRoleLimit() {
  if (selectedMaxActiveRoleCount.value <= 1) {
    return;
  }
  if (selectedRoleCodes.value.length >= selectedMaxActiveRoleCount.value) {
    window.alert("No puedes reducir el máximo mientras tengas ya seleccionados ese número de roles.");
    return;
  }
  selectedMaxActiveRoleCount.value -= 1;
  selectedModalError.value = null;
}

function toggleSelectedAvailableRole(roleCode: string) {
  if (travelerMode.value !== "edit") {
    return;
  }
  if (roleCode === passengerRoleCode) {
    selectedAvailableRoleCodes.value = selectedAvailableRoleCodes.value.includes(passengerRoleCode)
      ? selectedAvailableRoleCodes.value
      : [...selectedAvailableRoleCodes.value, passengerRoleCode];
    return;
  }
  const currentlySelected = selectedAvailableRoleCodes.value.includes(roleCode);
  if (currentlySelected) {
    selectedAvailableRoleCodes.value = selectedAvailableRoleCodes.value.filter((code) => code !== roleCode);
    selectedRoleCodes.value = selectedRoleCodes.value.filter((code) => code !== roleCode);
    selectedModalError.value = null;
    return;
  }

  selectedAvailableRoleCodes.value = [...selectedAvailableRoleCodes.value, roleCode];
  selectedModalError.value = null;
}

function toggleCreateRole(roleCode: string) {
  const currentlySelected = createActiveRoleCodes.value.includes(roleCode);
  if (currentlySelected) {
    createActiveRoleCodes.value = createActiveRoleCodes.value.filter((code) => code !== roleCode);
    createModalError.value = null;
    return;
  }

  if (createActiveRoleCodes.value.length >= createMaxActiveRoleCount.value) {
    createModalError.value = "Aumenta el máximo de roles simultáneos antes de activar otro rol";
    return;
  }

  createActiveRoleCodes.value = [...createActiveRoleCodes.value, roleCode];
  createModalError.value = null;
}

function toggleCreateAvailableRole(roleCode: string) {
  if (roleCode === passengerRoleCode) {
    createAvailableRoleCodes.value = createAvailableRoleCodes.value.includes(passengerRoleCode)
      ? createAvailableRoleCodes.value
      : [...createAvailableRoleCodes.value, passengerRoleCode];
    return;
  }
  const currentlySelected = createAvailableRoleCodes.value.includes(roleCode);
  if (currentlySelected) {
    createAvailableRoleCodes.value = createAvailableRoleCodes.value.filter((code) => code !== roleCode);
    createActiveRoleCodes.value = createActiveRoleCodes.value.filter((code) => code !== roleCode);
    return;
  }

  createAvailableRoleCodes.value = [...createAvailableRoleCodes.value, roleCode];
}

function increaseCreateRoleLimit() {
  createMaxActiveRoleCount.value += 1;
  createModalError.value = null;
}

function decreaseCreateRoleLimit() {
  if (createMaxActiveRoleCount.value <= 1) {
    return;
  }
  if (createActiveRoleCodes.value.length >= createMaxActiveRoleCount.value) {
    window.alert("No puedes reducir el máximo mientras tengas ya seleccionados ese número de roles.");
    return;
  }
  createMaxActiveRoleCount.value -= 1;
  createModalError.value = null;
}

function roleName(code: string) {
  return roleCatalogByCode.value[code]?.name ?? code;
}

function roleSummary(roleCodes: string[]) {
  if (!roleCodes.length) {
    return "Sin roles activos";
  }
  return roleCodes.map((code) => roleName(code)).join(", ");
}

function travelerAssignmentLabel(traveler: CaravanTraveler): string {
  return traveler.wagonName ?? "Sin carro";
}

function roleCountLabel(activeCount: number, maxCount: number) {
  return `${activeCount} / ${maxCount}`;
}

function wagonName(wagonId: string | null) {
  if (!wagonId) {
    return "Sin carro";
  }
  return wagons.value.find((wagon) => wagon.id === wagonId)?.name ?? "Sin carro";
}

function travelerName(travelerId: string | null) {
  if (!travelerId) {
    return "";
  }
  return travelers.value.find((traveler) => traveler.id === travelerId)?.fullName ?? "";
}

onMounted(refresh);
</script>

<template>
  <main class="page">
    <section class="shell">
      <header class="hero">
        <div>
          <p class="eyebrow">Caravana activa</p>
          <h1>Viajeros de la caravana</h1>
          <p class="subtitle">
            Consulta el roster, filtra por rol o carro y abre el detalle completo de cada viajero.
          </p>
        </div>

        <div class="hero-actions">
          <button class="ghost-button" type="button" :disabled="loading || submitting" @click="refresh">
            <span class="button-with-spinner">
              <span v-if="isPending('refresh')" class="button-spinner" aria-hidden="true"></span>
              <span>{{ isPending('refresh') ? "Refrescando…" : "Refrescar" }}</span>
            </span>
          </button>
          <button class="primary-button" type="button" :disabled="!activeCaravan || loading || submitting" @click="openCreateModal">
            Añadir
          </button>
        </div>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <section v-if="!activeCaravan" class="card empty-state">
        <h2>No hay caravana activa</h2>
        <p class="muted">Debes seleccionar una caravana antes de gestionar viajeros.</p>
        <RouterLink class="primary-link" to="/">Ir a caravanas</RouterLink>
      </section>

      <template v-else>
        <section class="card">
          <div class="section-header">
            <div>
              <h2>Listado de viajeros</h2>
              <p class="muted">El listado se actualiza al cambiar filtros o al guardar modificaciones.</p>
            </div>
          </div>

          <div v-if="travelersWithoutWagon.length > 0" class="warning-banner" role="status" aria-live="polite">
            <strong>{{ travelersWithoutWagon.length }} viajero{{ travelersWithoutWagon.length === 1 ? "" : "s" }} sin carro asignado.</strong>
            <p>
              Revísalos cuanto antes: cuanto más tiempo pasen sin asignación, más fácil es perder visibilidad sobre el reparto real de la caravana.
            </p>
          </div>

          <div class="filters">
            <label>
              <span>Buscador</span>
              <input v-model="search" type="search" placeholder="Buscar por nombre" />
            </label>

            <label>
              <span>Rol activo</span>
              <select v-model="roleFilter">
                <option value="all">Todos</option>
                <option v-for="role in roleFilterOptions" :key="role.code" :value="role.code">
                  {{ role.name }}
                </option>
              </select>
            </label>

            <label>
              <span>Carro</span>
              <select v-model="wagonFilter">
                <option value="all">Todos</option>
                <option v-for="wagon in wagons" :key="wagon.id" :value="wagon.id">
                  {{ wagon.name }}
                </option>
              </select>
            </label>
          </div>

          <div v-if="loading" class="muted">Cargando viajeros…</div>
          <div v-else-if="visibleTravelers.length === 0" class="empty-state-inline">
            <p class="muted">No hay viajeros que coincidan con el filtro actual.</p>
          </div>

          <div v-else class="table-wrap">
            <table class="travelers-table">
              <thead>
                <tr>
                  <th>Viajero</th>
                  <th>Rol activo</th>
                  <th>Carro</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="traveler in visibleTravelers"
                  :key="traveler.id"
                  tabindex="0"
                  role="button"
                  :class="{ 'is-unassigned': !traveler.wagonId }"
                  @click="openTraveler(traveler)"
                  @keyup.enter="openTraveler(traveler)"
                >
                  <td>
                    <strong>{{ traveler.fullName }}</strong>
                    <p class="muted">{{ traveler.description ?? "Sin descripción" }}</p>
                  </td>
                  <td>{{ roleSummary(traveler.activeRoleCodes) }}</td>
                  <td>
                    <span class="assignment-badge" :class="{ 'assignment-badge--warning': !traveler.wagonId }">
                      {{ travelerAssignmentLabel(traveler) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </section>

    <teleport to="body">
      <div v-if="selectedTraveler" class="modal-backdrop" @click.self="closeTraveler">
        <div class="modal">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Detalle del viajero</p>
              <template v-if="travelerMode === 'view'">
                <h2>{{ selectedTraveler.fullName }}</h2>
              </template>
              <template v-else>
                <label class="modal-title-field">
                  <span>Nombre completo</span>
                  <input v-model="selectedFullName" type="text" />
                </label>
              </template>
            </div>
            <div class="modal-header-actions">
              <button
                v-if="travelerMode === 'view'"
                class="danger-button"
                type="button"
                :disabled="loading || submitting"
                :aria-busy="isPending('delete')"
                @click="handleDeleteTraveler"
              >
                <span class="button-with-spinner">
                  <span v-if="isPending('delete')" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending('delete') ? "Eliminando…" : "Eliminar" }}</span>
                </span>
              </button>
              <button
                class="ghost-button"
                type="button"
                  :disabled="loading || submitting"
                  @click="travelerMode === 'view' ? enterEditMode() : cancelEditMode()"
                >
                  {{ travelerMode === 'view' ? "Editar" : "Cancelar edición" }}
                </button>
              <button class="ghost-button" type="button" @click="closeTraveler">Cerrar</button>
            </div>
          </div>

          <p v-if="selectedModalError" class="error">{{ selectedModalError }}</p>

          <template v-if="travelerMode === 'view'">
            <section class="info-block">
              <h3>Descripción</h3>
              <p>{{ selectedTraveler.description ?? "Sin descripción" }}</p>
            </section>

            <section class="info-block">
              <h3>Datos actuales</h3>
              <dl class="stats">
                <div>
                  <dt>Roles activos</dt>
                  <dd>{{ roleSummary(selectedTraveler.activeRoleCodes) }}</dd>
                </div>
                <div>
                  <dt>Roles posibles</dt>
                  <dd>{{ roleSummary(selectedTraveler.availableRoleCodes) }}</dd>
                </div>
                <div><dt>Carro</dt><dd>{{ selectedTraveler.wagonName ?? "Sin carro" }}</dd></div>
                <div><dt>Consumo</dt><dd>{{ selectedTraveler.consumption }}</dd></div>
              </dl>
              <p v-if="selectedTraveler.servedTravelerName" class="muted">
                Sirve a <strong>{{ selectedTraveler.servedTravelerName }}</strong>
              </p>
            </section>

            <section class="info-block">
              <h3>Contrato</h3>
              <p v-if="selectedTraveler.salary !== null || selectedTraveler.contractConditions">
                <strong>Sueldo:</strong>
                {{ selectedTraveler.salary !== null ? `${gpFormatter.format(selectedTraveler.salary)} gp` : "—" }}
              </p>
              <p v-if="selectedTraveler.contractConditions">
                <strong>Condiciones:</strong> {{ selectedTraveler.contractConditions }}
              </p>
              <p v-else class="muted">Este viajero no tiene contrato registrado.</p>
            </section>
          </template>

          <template v-else>
            <div class="create-form">
              <label>
                <span>Nombre completo</span>
                <input v-model="selectedFullName" type="text" placeholder="Nombre y apellidos" />
              </label>

              <label>
                <span>Descripción</span>
                <textarea v-model="selectedDescription" rows="3" placeholder="Opcional"></textarea>
              </label>

              <section class="info-block">
                <div class="section-header">
                  <div>
                    <h3>Roles disponibles</h3>
                    <p class="muted">Selecciona todos los roles que este viajero puede llegar a ejercer.</p>
                  </div>
                </div>

                <div class="role-toggle-grid">
                  <button
                    v-for="role in roleCatalog"
                    :key="role.code"
                    type="button"
                    class="role-toggle"
                    :class="{ 'is-active': selectedAvailableRoleCodes.includes(role.code), 'is-primary': role.code === passengerRoleCode }"
                    :aria-pressed="selectedAvailableRoleCodes.includes(role.code)"
                    :disabled="role.code === passengerRoleCode"
                    @click="toggleSelectedAvailableRole(role.code)"
                  >
                    <span class="role-toggle-name">{{ role.name }}</span>
                    <span class="role-toggle-state">
                      {{ selectedAvailableRoleCodes.includes(role.code) ? "Disponible" : "No disponible" }}
                    </span>
                    <small>{{ role.requirements }}</small>
                  </button>
                </div>
              </section>

              <section class="info-block">
                <div class="section-header">
                  <div>
                    <h3>Roles activos</h3>
                    <p class="muted">Activa los roles que este viajero está ejerciendo ahora mismo.</p>
                  </div>
                  <div class="role-limit-controls">
                    <button class="ghost-button" type="button" :disabled="selectedMaxActiveRoleCount <= 1" @click="decreaseRoleLimit">
                      -
                    </button>
                    <strong>{{ roleCountLabel(selectedRoleCodes.length, selectedMaxActiveRoleCount) }}</strong>
                    <button class="ghost-button" type="button" :disabled="loading || submitting" @click="increaseRoleLimit">
                      +
                    </button>
                  </div>
                </div>

                <div class="role-toggle-grid">
                  <button
                    v-for="role in selectedRoleOptions"
                    :key="role.code"
                    type="button"
                    class="role-toggle"
                    :class="{ 'is-active': selectedRoleCodes.includes(role.code), 'is-primary': role.code === selectedPrimaryRoleCode }"
                    :aria-pressed="selectedRoleCodes.includes(role.code)"
                    :disabled="loading || submitting"
                    @click="handleRoleSelection(role.code)"
                  >
                    <span class="role-toggle-name">{{ role.name }}</span>
                    <span class="role-toggle-state">{{ selectedRoleCodes.includes(role.code) ? "En ejercicio" : "Disponible" }}</span>
                    <small>{{ role.requirements }}</small>
                  </button>
                </div>
                <div v-if="selectedTargetRole?.requiresTargetTraveler" class="editor-row">
                  <label>
                    <span>Viajero al que sirve</span>
                    <select v-model="selectedRoleTargetTravelerId" :disabled="loading || submitting">
                      <option value="">Selecciona un viajero</option>
                      <option v-for="traveler in selectedTargetTravelerOptions" :key="traveler.id" :value="traveler.id">
                        {{ traveler.fullName }}
                      </option>
                    </select>
                  </label>
                </div>
              </section>

              <div class="two-columns">
                <label>
                  <span>Carro</span>
                  <select v-model="selectedWagonId" :disabled="loading || submitting">
                    <option value="">Selecciona un carro</option>
                    <option v-for="wagon in wagons" :key="wagon.id" :value="wagon.id">
                      {{ wagon.name }}
                    </option>
                  </select>
                </label>

                <label>
                  <span>Sueldo</span>
                  <input
                    v-model="selectedSalary"
                    type="number"
                    min="0"
                    step="0.01"
                    inputmode="decimal"
                    placeholder="Opcional"
                  />
                </label>
              </div>

              <div class="two-columns">
                <label>
                  <span>Consumo</span>
                  <input v-model="selectedConsumption" type="number" min="1" />
                </label>

                <div class="role-limit-summary">
                  <span>Roles simultáneos máximos</span>
                  <strong>{{ selectedMaxActiveRoleCount }}</strong>
                </div>
              </div>

              <label>
                <span>Condiciones del contrato</span>
                <textarea v-model="selectedContractConditions" rows="3" placeholder="Opcional"></textarea>
              </label>

              <div class="modal-actions">
                <button class="secondary-button" type="button" :disabled="loading || submitting" @click="cancelEditMode">Cancelar</button>
                <button class="primary-button" type="button" :disabled="loading || submitting" :aria-busy="isPending('save')" @click="handleSaveTravelerChanges">
                  <span class="button-with-spinner">
                    <span v-if="isPending('save')" class="button-spinner" aria-hidden="true"></span>
                    <span>{{ isPending('save') ? "Guardando…" : "Guardar cambios" }}</span>
                  </span>
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="createModalOpen" class="modal-backdrop" @click.self="closeCreateModal">
        <div class="modal modal-create">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Añadir viajero</p>
              <h2>Nuevo viajero</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCreateModal">Cerrar</button>
          </div>

          <div class="create-form">
            <p v-if="createModalError" class="error">{{ createModalError }}</p>

            <label>
              <span>Nombre completo</span>
              <input v-model="createFullName" type="text" placeholder="Nombre y apellidos" />
            </label>

            <label>
              <span>Descripción</span>
              <textarea v-model="createDescription" rows="3" placeholder="Opcional"></textarea>
            </label>

            <section class="info-block">
              <div class="section-header">
                <div>
                  <h3>Roles disponibles</h3>
                  <p class="muted">Selecciona todos los roles que este viajero puede llegar a ejercer.</p>
                </div>
              </div>

              <div class="role-toggle-grid">
                <button
                  v-for="role in roleCatalog"
                  :key="role.code"
                  type="button"
                  class="role-toggle"
                  :class="{ 'is-active': createAvailableRoleCodes.includes(role.code), 'is-primary': role.code === passengerRoleCode }"
                  :aria-pressed="createAvailableRoleCodes.includes(role.code)"
                  :disabled="role.code === passengerRoleCode"
                  @click="toggleCreateAvailableRole(role.code)"
                >
                  <span class="role-toggle-name">{{ role.name }}</span>
                  <span class="role-toggle-state">
                    {{ createAvailableRoleCodes.includes(role.code) ? "Disponible" : "No disponible" }}
                  </span>
                  <small>{{ role.requirements }}</small>
                </button>
              </div>
            </section>

            <section class="info-block">
              <div class="section-header">
                <div>
                  <h3>Roles en ejercicio</h3>
                  <p class="muted">Activa los roles que este viajero está ejerciendo ahora mismo.</p>
                </div>
                <div class="role-limit-controls">
                  <button class="ghost-button" type="button" :disabled="createMaxActiveRoleCount <= 1" @click="decreaseCreateRoleLimit">
                    -
                  </button>
                  <strong>{{ roleCountLabel(createActiveRoleCodes.length, createMaxActiveRoleCount) }}</strong>
                  <button class="ghost-button" type="button" @click="increaseCreateRoleLimit">+</button>
                </div>
              </div>

              <div class="role-toggle-grid">
                <button
                  v-for="role in createRoleOptions"
                  :key="role.code"
                  type="button"
                  class="role-toggle"
                  :class="{ 'is-active': createActiveRoleCodes.includes(role.code), 'is-primary': role.code === createPrimaryRoleCode }"
                  :aria-pressed="createActiveRoleCodes.includes(role.code)"
                  @click="toggleCreateRole(role.code)"
                >
                  <span class="role-toggle-name">{{ role.name }}</span>
                  <span class="role-toggle-state">{{ createActiveRoleCodes.includes(role.code) ? "En ejercicio" : "Disponible" }}</span>
                  <small>{{ role.requirements }}</small>
                </button>
              </div>
            </section>

            <div class="two-columns">
              <label v-if="createTargetRole?.requiresTargetTraveler">
                <span>Viajero al que sirve</span>
                <select v-model="createServedTravelerId">
                  <option value="">Selecciona un viajero</option>
                  <option v-for="traveler in createTargetTravelerOptions" :key="traveler.id" :value="traveler.id">
                    {{ traveler.fullName }}
                  </option>
                </select>
              </label>

              <div class="role-limit-summary">
                <span>Roles simultáneos máximos</span>
                <strong>{{ createMaxActiveRoleCount }}</strong>
              </div>
            </div>

            <div class="two-columns">
              <label>
                <span>Sueldo</span>
                <input
                  v-model="createSalary"
                  type="number"
                  min="0"
                  step="0.01"
                  inputmode="decimal"
                  placeholder="Opcional"
                />
              </label>

              <label>
                <span>Consumo</span>
                <input v-model="createConsumption" type="number" min="1" />
              </label>
            </div>

            <label>
              <span>Condiciones del contrato</span>
              <textarea v-model="createContractConditions" rows="3" placeholder="Opcional"></textarea>
            </label>

            <div class="modal-actions">
              <button class="secondary-button" type="button" @click="closeCreateModal">Cancelar</button>
              <button class="primary-button" type="button" :disabled="loading || submitting" :aria-busy="isPending('create')" @click="handleCreateTraveler">
                <span class="button-with-spinner">
                  <span v-if="isPending('create')" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending('create') ? "Creando…" : "Confirmar" }}</span>
                </span>
              </button>
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
  width: min(1200px, 100%);
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

.hero-actions {
  display: flex;
  gap: 0.75rem;
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1rem;
}

.filters {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr 0.9fr;
  gap: 0.75rem;
  margin: 1rem 0;
}

.warning-banner {
  display: grid;
  gap: 0.25rem;
  padding: 0.95rem 1rem;
  margin-top: 1rem;
  border-radius: 0.9rem;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  color: #92400e;
  box-shadow: 0 10px 24px rgba(245, 158, 11, 0.18);
}

.warning-banner strong {
  font-size: 1rem;
}

.warning-banner p {
  color: inherit;
}

.filters label,
.create-form label,
.editor-row label,
.two-columns label {
  display: grid;
  gap: 0.35rem;
}

.filters span,
.create-form span,
.editor-row span,
.two-columns span {
  font-size: 0.85rem;
  color: #6b7280;
}

.filters select,
.filters input,
.create-form select,
.create-form input,
.create-form textarea,
.editor-row select,
.editor-row input,
.two-columns select,
.two-columns input {
  width: 100%;
  padding: 0.75rem 0.9rem;
  border-radius: 0.8rem;
  border: 1px solid #d1d5db;
  font: inherit;
  background: white;
}

.table-wrap {
  overflow: auto;
  margin-top: 1rem;
}

.travelers-table {
  width: 100%;
  border-collapse: collapse;
}

.travelers-table th,
.travelers-table td {
  padding: 0.8rem;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  vertical-align: top;
}

.travelers-table tbody tr {
  cursor: pointer;
}

.travelers-table tbody tr.is-unassigned {
  background: #fff7ed;
}

.travelers-table tbody tr:hover {
  background: #f8fafc;
}

.travelers-table tbody tr.is-unassigned:hover {
  background: #ffedd5;
}

.assignment-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.65rem;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
  line-height: 1.2;
}

.assignment-badge--warning {
  background: #fee2e2;
  color: #b91c1c;
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
  width: min(1000px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: white;
  border-radius: 1.2rem;
  padding: 1.25rem;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
  display: grid;
  gap: 1rem;
}

.modal-create {
  width: min(1080px, 100%);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.modal-header-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.modal-title-field {
  display: grid;
  gap: 0.35rem;
  min-width: min(28rem, 100%);
}

.modal-title-field span {
  font-size: 0.85rem;
  color: #6b7280;
}

.modal-title-field input {
  width: 100%;
  padding: 0.75rem 0.9rem;
  border-radius: 0.8rem;
  border: 1px solid #d1d5db;
  font: inherit;
  background: white;
}

.detail-grid,
.editor-grid,
.two-columns {
  display: grid;
  gap: 0.85rem;
}

.detail-grid {
  grid-template-columns: 1fr 1fr;
}

.editor-grid {
  grid-template-columns: 1fr 1fr;
}

.info-block {
  padding: 0.9rem;
  border-radius: 0.85rem;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  display: grid;
  gap: 0.6rem;
}

.stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.stats div {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #fff;
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

.editor-row {
  display: grid;
  gap: 0.85rem;
}

.role-limit {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.role-limit-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
}

.role-limit-summary {
  display: grid;
  gap: 0.35rem;
  align-content: start;
}

.role-limit-summary strong,
.role-limit-controls strong {
  font-size: 1.05rem;
}

.create-form {
  display: grid;
  gap: 1rem;
}

.role-toggle-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.6rem;
}

.role-toggle {
  display: grid;
  gap: 0.25rem;
  padding: 0.85rem 0.9rem;
  border-radius: 0.9rem;
  border: 1px solid #dbe3ef;
  background: white;
  text-align: left;
  cursor: pointer;
  min-height: 78px;
}

.role-toggle:hover {
  border-color: #b6c8ff;
  box-shadow: 0 8px 18px rgba(29, 78, 216, 0.08);
}

.role-toggle.is-active {
  background: #dbeafe;
  border-color: #93c5fd;
}

.role-toggle.is-primary {
  outline: 2px solid #1d4ed8;
  outline-offset: 1px;
}

.role-toggle-name {
  font-weight: 700;
  color: #1f2937;
}

.role-toggle-state {
  font-size: 0.8rem;
  color: #6b7280;
}

.role-toggle small {
  color: #6b7280;
  line-height: 1.3;
}

.role-checkbox-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
}

.role-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  padding: 0.65rem 0.75rem;
  border-radius: 0.8rem;
  border: 1px solid #dbe3ef;
  background: white;
  min-height: 64px;
}

.role-checkbox input {
  margin-top: 0.15rem;
  flex: 0 0 auto;
}

.role-checkbox span {
  display: grid;
  gap: 0.08rem;
}

.role-checkbox strong {
  display: block;
  font-size: 0.95rem;
}

.role-checkbox small {
  display: block;
  color: #6b7280;
  line-height: 1.25;
  font-size: 0.8rem;
}

.two-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 1000px) {
  .filters,
  .detail-grid,
  .editor-grid,
  .two-columns {
    grid-template-columns: 1fr;
  }

  .role-checkbox-grid,
  .role-toggle-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .role-limit {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero,
  .modal-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .filters,
  .detail-grid,
  .editor-grid,
  .two-columns,
  .role-checkbox-grid,
  .role-toggle-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .page {
    padding: 1rem;
  }

  .hero-actions,
  .modal-actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
