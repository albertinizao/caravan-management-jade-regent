<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";

import { useToast } from "@/composables/useToast";
import { getActiveCaravan, listCaravans } from "@/services/caravans";
import { listCaravanWagons } from "@/services/wagons";
import {
  addCargoFromCatalog,
  addCustomCargo,
  deleteCaravanCargo,
  getCaravanCargo,
  listCaravanCargo,
  listCaravanCargoSummary,
  listCargoCatalog,
  updateCaravanCargoWagon,
} from "@/services/cargo";
import type { Caravan } from "@/types/caravan";
import type { CaravanCargo, CaravanCargoSummary, CargoCatalogItem } from "@/types/cargo";
import type { CaravanWagon } from "@/types/wagon";

const activeCaravan = ref<Caravan | null>(null);
const cargo = ref<CaravanCargo[]>([]);
const cargoSummary = ref<CaravanCargoSummary[]>([]);
const catalog = ref<CargoCatalogItem[]>([]);
const wagons = ref<CaravanWagon[]>([]);
const loading = ref(true);
const submitting = ref(false);
const pendingAction = ref<string | null>(null);
const error = ref<string | null>(null);
const search = ref("");
const sourceFilter = ref<"all" | "CATALOG" | "CUSTOM">("all");
const categoryFilter = ref("all");
const wagonFilter = ref("all");
const selectedCargo = ref<CaravanCargo | null>(null);
const selectedCargoError = ref<string | null>(null);
const selectedCargoWagonId = ref("");
const catalogModalOpen = ref(false);
const customModalOpen = ref(false);
const catalogModalError = ref<string | null>(null);
const customModalError = ref<string | null>(null);
const selectedCatalogCode = ref("");
const catalogQuantity = ref("1");
const catalogCargoUnits = ref("1");
const catalogWagonId = ref("");
const catalogOrigin = ref("");
const catalogSpecificCommodity = ref("");
const catalogDeity = ref("");
const catalogNotes = ref("");
const customDisplayName = ref("");
const customCategory = ref("");
const customQuantity = ref("1");
const customCargoUnits = ref("1");
const customWagonId = ref("");
const customOrigin = ref("");
const customSpecificCommodity = ref("");
const customDeity = ref("");
const customNotes = ref("");
const { showToast } = useToast();

function isPending(action: string) {
  return pendingAction.value === action;
}

const catalogByCode = computed(() =>
  Object.fromEntries(catalog.value.map((item) => [item.code, item] as const)),
);

const selectedCatalogItem = computed(() =>
  selectedCatalogCode.value ? catalogByCode.value[selectedCatalogCode.value] ?? null : null,
);

const visibleCargo = computed(() => {
  const query = search.value.trim().toLowerCase();
  return cargo.value
    .filter((item) => !query || item.displayName.toLowerCase().includes(query) || item.category.toLowerCase().includes(query))
    .filter((item) => sourceFilter.value === "all" || item.sourceType === sourceFilter.value)
    .filter((item) => categoryFilter.value === "all" || item.category === categoryFilter.value)
    .filter((item) => wagonFilter.value === "all" || item.wagonId === wagonFilter.value);
});

const categories = computed(() =>
  Array.from(new Set([...catalog.value.map((item) => item.category), ...cargo.value.map((item) => item.category)]))
    .sort(),
);

const catalogQuantityValue = computed(() => parsePositiveInteger(catalogQuantity.value, selectedCatalogItem.value?.defaultQuantity ?? 1));
const catalogCargoUnitsValue = computed(() => parsePositiveInteger(catalogCargoUnits.value, selectedCatalogItem.value?.defaultCargoUnits ?? 1));
const customQuantityValue = computed(() => parsePositiveInteger(customQuantity.value, 1));
const customCargoUnitsValue = computed(() => parsePositiveInteger(customCargoUnits.value, 1));
const selectedCargoLoad = computed(() => (selectedCargo.value ? totalCargoUnits(selectedCargo.value.quantity, selectedCargo.value.cargoUnits) : 0));
const catalogAvailableWagons = computed(() => filterWagonsForCatalog(selectedCatalogItem.value, catalogQuantityValue.value, catalogCargoUnitsValue.value));
const customAvailableWagons = computed(() => filterWagonsForCustomCargo(customQuantityValue.value, customCargoUnitsValue.value));
const selectedCargoAvailableWagons = computed(() => (selectedCargo.value ? filterWagonsForCargo(selectedCargo.value) : []));
const catalogQuantityMax = computed(() => maxQuantityForWagon(catalogWagonId.value, catalogCargoUnitsValue.value));
const customQuantityMax = computed(() => maxQuantityForWagon(customWagonId.value, customCargoUnitsValue.value));

async function refresh() {
  const previousAction = pendingAction.value;
  const trackRefresh = previousAction === null;

  if (trackRefresh) {
    pendingAction.value = "refresh";
  }

  loading.value = true;
  error.value = null;

  try {
    const [, activeResponse] = await Promise.all([listCaravans(), getActiveCaravan()]);
    activeCaravan.value = activeResponse.caravan;

    if (activeResponse.caravan) {
      const [cargoList, catalogList, wagonList, summaryList] = await Promise.all([
        listCaravanCargo(activeResponse.caravan.id),
        listCargoCatalog(activeResponse.caravan.id),
        listCaravanWagons(activeResponse.caravan.id),
        listCaravanCargoSummary(activeResponse.caravan.id),
      ]);

      cargo.value = cargoList;
      catalog.value = catalogList;
      wagons.value = wagonList;
      cargoSummary.value = summaryList;

      if (selectedCargo.value) {
        const refreshed = cargoList.find((item) => item.id === selectedCargo.value?.id);
        if (refreshed) {
          selectedCargo.value = refreshed;
          selectedCargoError.value = null;
          selectedCargoWagonId.value = refreshed.wagonId ?? "";
        }
      }

      if (selectedCatalogCode.value && !catalogList.some((item) => item.code === selectedCatalogCode.value)) {
        selectedCatalogCode.value = catalogList[0]?.code ?? "";
      }
    } else {
      cargo.value = [];
      cargoSummary.value = [];
      catalog.value = [];
      wagons.value = [];
      closeCatalogModal();
      closeCustomModal();
      closeCargoDetails();
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load cargo";
  } finally {
    loading.value = false;
    pendingAction.value = trackRefresh ? null : previousAction;
  }
}

function openCatalogModal() {
  if (!selectedCatalogCode.value) {
    selectedCatalogCode.value = catalog.value[0]?.code ?? "";
  }
  resetCatalogForm();
  catalogModalError.value = null;
  catalogModalOpen.value = true;
}

function closeCatalogModal() {
  catalogModalOpen.value = false;
  catalogModalError.value = null;
}

function resetCatalogForm() {
  const item = selectedCatalogItem.value;
  catalogQuantity.value = String(item?.defaultQuantity ?? 1);
  catalogCargoUnits.value = String(item?.defaultCargoUnits ?? 1);
  catalogWagonId.value = pickDefaultCatalogWagonId(item);
  catalogOrigin.value = "";
  catalogSpecificCommodity.value = "";
  catalogDeity.value = "";
  catalogNotes.value = "";
}

function openCustomModal() {
  resetCustomForm();
  customModalError.value = null;
  customModalOpen.value = true;
}

function closeCustomModal() {
  customModalOpen.value = false;
  customModalError.value = null;
}

function resetCustomForm() {
  customDisplayName.value = "";
  customCategory.value = "";
  customQuantity.value = "1";
  customCargoUnits.value = "1";
  customWagonId.value = pickDefaultCustomWagonId();
  customOrigin.value = "";
  customSpecificCommodity.value = "";
  customDeity.value = "";
  customNotes.value = "";
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

function getWagonRemainingCargoUnits(wagonId: string) {
  return cargoSummary.value.find((summary) => summary.wagonId === wagonId)?.remainingCargoUnits ?? 0;
}

function isWagonCompatibleWithCatalogItem(wagon: CaravanWagon, item: CargoCatalogItem | null) {
  if (!item) {
    return false;
  }

  if (item.allowedWagonCodes.length > 0) {
    return item.allowedWagonCodes.includes(wagon.wagonTypeCode);
  }

  return wagon.wagonTypeCode !== "carro-de-suministros"
    && wagon.wagonTypeCode !== "carro-de-mercancias-especificas";
}

function isWagonCompatibleForCustomCargo(wagon: CaravanWagon) {
  return wagon.wagonTypeCode !== "carro-de-suministros"
    && wagon.wagonTypeCode !== "carro-de-mercancias-especificas";
}

function canFitCargoLoad(wagon: CaravanWagon, requiredCargoUnits: number, currentWagonId?: string | null) {
  if (currentWagonId && wagon.id === currentWagonId) {
    return true;
  }

  return getWagonRemainingCargoUnits(wagon.id) >= requiredCargoUnits;
}

function maxQuantityForWagon(wagonId: string, cargoUnits: number) {
  if (!wagonId || cargoUnits < 1) {
    return 1;
  }

  return Math.max(1, Math.floor(getWagonRemainingCargoUnits(wagonId) / cargoUnits));
}

function filterWagonsForCatalog(item: CargoCatalogItem | null, quantity: number, cargoUnits: number) {
  if (!item) {
    return [];
  }

  const requiredCargoUnits = totalCargoUnits(quantity, cargoUnits);
  return wagons.value.filter((wagon) => isWagonCompatibleWithCatalogItem(wagon, item) && canFitCargoLoad(wagon, requiredCargoUnits));
}

function filterWagonsForCustomCargo(quantity: number, cargoUnits: number) {
  const requiredCargoUnits = totalCargoUnits(quantity, cargoUnits);
  return wagons.value.filter((wagon) => isWagonCompatibleForCustomCargo(wagon) && canFitCargoLoad(wagon, requiredCargoUnits));
}

function filterWagonsForCargo(entry: CaravanCargo) {
  const requiredCargoUnits = totalCargoUnits(entry.quantity, entry.cargoUnits);
  return wagons.value.filter((wagon) => isWagonCompatibleForCargo(entry, wagon, requiredCargoUnits));
}

function isWagonCompatibleForCargo(entry: CaravanCargo, wagon: CaravanWagon, requiredCargoUnits: number) {
  const catalogItem = entry.catalogCode ? catalogByCode.value[entry.catalogCode] ?? null : null;
  const compatibleByType =
    entry.sourceType === "CUSTOM"
      ? isWagonCompatibleForCustomCargo(wagon)
      : isWagonCompatibleWithCatalogItem(wagon, catalogItem);

  if (!compatibleByType) {
    return false;
  }

  if (entry.wagonId && wagon.id === entry.wagonId) {
    return true;
  }

  return getWagonRemainingCargoUnits(wagon.id) >= requiredCargoUnits;
}

function pickDefaultCatalogWagonId(item: CargoCatalogItem | null) {
  return filterWagonsForCatalog(item, catalogQuantityValue.value, catalogCargoUnitsValue.value)[0]?.id ?? "";
}

function pickDefaultCustomWagonId() {
  return filterWagonsForCustomCargo(customQuantityValue.value, customCargoUnitsValue.value)[0]?.id ?? "";
}

function syncCatalogWagonSelection() {
  if (!catalogModalOpen.value) {
    return;
  }

  const available = catalogAvailableWagons.value;
  if (available.length === 0) {
    catalogWagonId.value = "";
    return;
  }

  if (!available.some((wagon) => wagon.id === catalogWagonId.value)) {
    catalogWagonId.value = available[0].id;
  }
}

function syncCustomWagonSelection() {
  if (!customModalOpen.value) {
    return;
  }

  const available = customAvailableWagons.value;
  if (available.length === 0) {
    customWagonId.value = "";
    return;
  }

  if (!available.some((wagon) => wagon.id === customWagonId.value)) {
    customWagonId.value = available[0].id;
  }
}

function syncCargoWagonSelection() {
  if (!selectedCargo.value) {
    return;
  }

  const available = selectedCargoAvailableWagons.value;
  if (available.length === 0) {
    selectedCargoWagonId.value = selectedCargo.value.wagonId ?? "";
    return;
  }

  if (!available.some((wagon) => wagon.id === selectedCargoWagonId.value)) {
    selectedCargoWagonId.value = available[0].id;
  }
}

async function handleAddCatalogCargo() {
  if (!activeCaravan.value || !selectedCatalogItem.value) {
    return;
  }

  const wagonId = catalogWagonId.value.trim();
  if (!wagonId) {
    catalogModalError.value = "Debes seleccionar un carro";
    return;
  }

  const parsedQuantity = catalogQuantityValue.value;
  const parsedCargoUnits = catalogCargoUnitsValue.value;
  const selectedWagon = catalogAvailableWagons.value.find((wagon) => wagon.id === wagonId);
  if (!selectedWagon) {
    catalogModalError.value = "Solo se muestran carros válidos para esta carga";
    return;
  }

  if (totalCargoUnits(parsedQuantity, parsedCargoUnits) > getWagonRemainingCargoUnits(selectedWagon.id)) {
    catalogModalError.value = "La cantidad supera la capacidad disponible del carro";
    return;
  }

  submitting.value = true;
  pendingAction.value = "add-catalog";
  catalogModalError.value = null;

  try {
    const created = await addCargoFromCatalog(activeCaravan.value.id, {
      catalogCode: selectedCatalogItem.value.code,
      quantity: parsedQuantity,
      cargoUnits: parsedCargoUnits,
      wagonId,
      origin: catalogOrigin.value.trim() || null,
      specificCommodity: catalogSpecificCommodity.value.trim() || null,
      deity: catalogDeity.value.trim() || null,
      notes: catalogNotes.value.trim() || null,
    });
    closeCatalogModal();
    selectedCargo.value = created;
    selectedCargoWagonId.value = created.wagonId ?? "";
    await refresh();
    showToast(`Carga añadida: ${created.displayName}.`);
  } catch (cause) {
    catalogModalError.value = cause instanceof Error ? cause.message : "Failed to add cargo";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleAddCustomCargo() {
  if (!activeCaravan.value) {
    return;
  }

  const displayName = customDisplayName.value.trim();
  const category = customCategory.value.trim();
  const wagonId = customWagonId.value.trim();

  if (!displayName) {
    customModalError.value = "El nombre es obligatorio";
    return;
  }
  if (!category) {
    customModalError.value = "La categoría es obligatoria";
    return;
  }
  if (!wagonId) {
    customModalError.value = "Debes seleccionar un carro";
    return;
  }

  const parsedQuantity = customQuantityValue.value;
  const parsedCargoUnits = customCargoUnitsValue.value;
  const selectedWagon = customAvailableWagons.value.find((wagon) => wagon.id === wagonId);

  if (Number.isNaN(parsedQuantity) || parsedQuantity < 1) {
    customModalError.value = "La cantidad debe ser un número mayor o igual a 1";
    return;
  }
  if (Number.isNaN(parsedCargoUnits) || parsedCargoUnits < 1) {
    customModalError.value = "Las unidades de carga deben ser un número mayor o igual a 1";
    return;
  }
  if (!selectedWagon) {
    customModalError.value = "Solo se muestran carros válidos para esta carga";
    return;
  }
  if (totalCargoUnits(parsedQuantity, parsedCargoUnits) > getWagonRemainingCargoUnits(selectedWagon.id)) {
    customModalError.value = "La cantidad supera la capacidad disponible del carro";
    return;
  }

  submitting.value = true;
  pendingAction.value = "add-custom";
  customModalError.value = null;

  try {
    const created = await addCustomCargo(activeCaravan.value.id, {
      displayName,
      category,
      quantity: parsedQuantity,
      cargoUnits: parsedCargoUnits,
      wagonId,
      origin: customOrigin.value.trim() || null,
      specificCommodity: customSpecificCommodity.value.trim() || null,
      deity: customDeity.value.trim() || null,
      notes: customNotes.value.trim() || null,
    });
    closeCustomModal();
    selectedCargo.value = created;
    selectedCargoWagonId.value = created.wagonId ?? "";
    await refresh();
    showToast(`Carga añadida: ${created.displayName}.`);
  } catch (cause) {
    customModalError.value = cause instanceof Error ? cause.message : "Failed to add cargo";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function openCargoDetails(entry: CaravanCargo) {
  if (!activeCaravan.value) {
    return;
  }

  try {
    selectedCargo.value = await getCaravanCargo(activeCaravan.value.id, entry.id);
    syncCargoWagonSelection();
    selectedCargoError.value = null;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load cargo details";
  }
}

function closeCargoDetails() {
  selectedCargo.value = null;
  selectedCargoError.value = null;
  selectedCargoWagonId.value = "";
}

async function handleMoveCargo() {
  if (!activeCaravan.value || !selectedCargo.value) {
    return;
  }

  if (!selectedCargoWagonId.value) {
    selectedCargoError.value = "Debes seleccionar un carro";
    return;
  }

  const selectedWagon = selectedCargoAvailableWagons.value.find((wagon) => wagon.id === selectedCargoWagonId.value);
  if (!selectedWagon) {
    selectedCargoError.value = "Solo se muestran carros válidos para esta carga";
    return;
  }

  submitting.value = true;
  pendingAction.value = "move-cargo";
  selectedCargoError.value = null;

  try {
    const updated = await updateCaravanCargoWagon(activeCaravan.value.id, selectedCargo.value.id, {
      wagonId: selectedCargoWagonId.value,
    });
    selectedCargo.value = updated;
    await refresh();
    showToast(`Carga reasignada: ${updated.displayName}.`);
  } catch (cause) {
    selectedCargoError.value = cause instanceof Error ? cause.message : "Failed to move cargo";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleDeleteCargo() {
  if (!activeCaravan.value || !selectedCargo.value) {
    return;
  }

  const confirmed = window.confirm(`¿Eliminar "${selectedCargo.value.displayName}"? Esta acción no se puede deshacer.`);
  if (!confirmed) {
    return;
  }

  submitting.value = true;
  pendingAction.value = "delete-cargo";
  selectedCargoError.value = null;

  try {
    await deleteCaravanCargo(activeCaravan.value.id, selectedCargo.value.id);
    closeCargoDetails();
    await refresh();
    showToast("Carga eliminada.");
  } catch (cause) {
    selectedCargoError.value = cause instanceof Error ? cause.message : "Failed to delete cargo";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

function sourceLabel(sourceType: string) {
  return sourceType === "CATALOG" ? "Catálogo" : "Personalizada";
}

function formatCargoLoad(quantity: number, cargoUnits: number) {
  const total = totalCargoUnits(quantity, cargoUnits);
  return cargoUnits === 1 ? `${total} u.c.` : `${total} u.c. (${quantity} × ${cargoUnits})`;
}

watch(selectedCatalogCode, () => {
  if (catalogModalOpen.value) {
    resetCatalogForm();
  }
});

watch([catalogQuantity, catalogCargoUnits, catalogModalOpen, catalogAvailableWagons], () => {
  syncCatalogWagonSelection();
});

watch([customQuantity, customCargoUnits, customModalOpen, customAvailableWagons], () => {
  syncCustomWagonSelection();
});

watch([selectedCargo, selectedCargoAvailableWagons], () => {
  syncCargoWagonSelection();
});

onMounted(refresh);
</script>

<template>
  <main class="page">
    <div class="shell">
      <header class="hero">
        <div>
          <p class="eyebrow">Carga de caravana</p>
          <h1>Gestionar el cargamento</h1>
          <p class="subtitle">Catalogo, carga personalizada, asignación por carro y vista resumida por vehículo.</p>
        </div>

        <div class="hero-actions">
          <RouterLink class="secondary-button" to="/wagons">Carros</RouterLink>
          <button class="primary-button" type="button" :disabled="loading || submitting" @click="openCatalogModal">
            Añadir carga
          </button>
          <button class="secondary-button" type="button" :disabled="loading || submitting" @click="openCustomModal">
            Carga personalizada
          </button>
        </div>
      </header>

      <div v-if="error" class="error">{{ error }}</div>

      <section class="card">
        <div class="section-header">
          <div>
            <h2>Resumen por carro</h2>
            <p class="muted">Una vista rápida de qué transporta cada carro.</p>
          </div>
        </div>

        <div v-if="cargoSummary.length === 0" class="empty-state-inline">
          <strong>No hay carros con carga registrada.</strong>
          <p class="muted">Añade carga para empezar a distribuir el cargamento.</p>
        </div>

        <div v-else class="summary-grid">
          <article v-for="summary in cargoSummary" :key="summary.wagonId" class="summary-card" @click="wagonFilter = summary.wagonId">
            <strong>{{ summary.wagonName }}</strong>
            <p class="muted">{{ summary.cargoEntryCount }} entradas · {{ summary.usedCargoUnits }}/{{ summary.cargoCapacity }} u.c.</p>
            <div class="summary-bar">
              <div class="summary-bar-fill" :style="{ width: `${summary.cargoCapacity === 0 ? 0 : (summary.usedCargoUnits / summary.cargoCapacity) * 100}%` }"></div>
            </div>
            <small>{{ summary.remainingCargoUnits }} u.c. libres</small>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="section-header">
          <div>
            <h2>Listado de carga</h2>
            <p class="muted">Filtra por nombre, origen, categoría o carro.</p>
          </div>
          <div class="muted">{{ visibleCargo.length }} entradas</div>
        </div>

        <div class="filters">
          <label>
            <span>Buscar</span>
            <input v-model="search" type="search" placeholder="Nombre o categoría" />
          </label>
          <label>
            <span>Origen</span>
            <select v-model="sourceFilter">
              <option value="all">Todos</option>
              <option value="CATALOG">Catálogo</option>
              <option value="CUSTOM">Personalizada</option>
            </select>
          </label>
          <label>
            <span>Categoría</span>
            <select v-model="categoryFilter">
              <option value="all">Todas</option>
              <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
            </select>
          </label>
          <label>
            <span>Carro</span>
            <select v-model="wagonFilter">
              <option value="all">Todos</option>
              <option v-for="wagon in wagons" :key="wagon.id" :value="wagon.id">{{ wagon.name }}</option>
            </select>
          </label>
        </div>

        <div class="table-wrap">
          <table class="cargo-table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Origen</th>
                <th>Categoría</th>
                <th>Cantidad</th>
                <th>Carga total</th>
                <th>Carro</th>
                <th>Notas</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in visibleCargo" :key="entry.id" @click="openCargoDetails(entry)">
                <td>
                  <strong>{{ entry.displayName }}</strong>
                  <div class="muted">{{ entry.catalogName ?? entry.displayName }}</div>
                </td>
                <td>{{ sourceLabel(entry.sourceType) }}</td>
                <td>{{ entry.category }}</td>
                <td>{{ entry.quantity }}</td>
                <td>{{ formatCargoLoad(entry.quantity, entry.cargoUnits) }}</td>
                <td>{{ entry.wagonName ?? "Sin carro" }}</td>
                <td class="muted">{{ entry.notes ?? entry.origin ?? entry.specificCommodity ?? entry.deity ?? "—" }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="visibleCargo.length === 0" class="empty-state-inline">
          <strong>No se encontraron entradas.</strong>
          <p class="muted">Prueba con otros filtros o añade una nueva carga.</p>
        </div>
      </section>
    </div>

    <teleport to="body">
      <div v-if="catalogModalOpen" class="modal-backdrop" @click.self="closeCatalogModal">
        <div class="modal modal-catalog">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Añadir carga</p>
              <h2>Catálogo de cargamento</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCatalogModal">Cerrar</button>
          </div>

          <p v-if="catalogModalError" class="error">{{ catalogModalError }}</p>

          <div class="catalog-layout">
            <div class="catalog-list">
              <button
                v-for="item in catalog"
                :key="item.code"
                type="button"
                class="catalog-item"
                :class="{ selected: item.code === selectedCatalogCode }"
                @click="selectedCatalogCode = item.code"
              >
                <strong>{{ item.name }}</strong>
                <span class="muted">{{ item.category }}</span>
                <small>{{ item.priceExpression }} · {{ item.quantityLabel }}</small>
              </button>
            </div>

            <div v-if="selectedCatalogItem" class="catalog-preview">
              <div class="preview-header">
                <div>
                  <h3>{{ selectedCatalogItem.name }}</h3>
                  <p class="muted">{{ selectedCatalogItem.category }}</p>
                </div>
                <span class="assignment-badge">{{ selectedCatalogItem.priceExpression }}</span>
              </div>

              <section class="info-block">
                <h4>Beneficio</h4>
                <p>{{ selectedCatalogItem.benefitText }}</p>
              </section>

              <section class="info-block">
                <h4>Descripción</h4>
                <p>{{ selectedCatalogItem.description }}</p>
                <p v-if="selectedCatalogItem.notes" class="muted">{{ selectedCatalogItem.notes }}</p>
              </section>

              <div class="two-columns">
                <label>
                  <span>Cantidad</span>
                  <input v-model="catalogQuantity" type="number" min="1" :max="catalogQuantityMax" />
                </label>
                <label>
                  <span>Unidades de carga</span>
                  <input v-model="catalogCargoUnits" type="number" min="1" :disabled="!selectedCatalogItem.cargoUnitsEditable" />
                </label>
              </div>

              <label>
                <span>Carro</span>
                <select v-model="catalogWagonId">
                  <option value="">Selecciona un carro</option>
                  <option v-for="wagon in catalogAvailableWagons" :key="wagon.id" :value="wagon.id">{{ wagon.name }}</option>
                </select>
              </label>
              <p class="muted">Solo se muestran carros válidos y con capacidad suficiente para esta carga.</p>


              <label>
                <span>Notas</span>
                <textarea v-model="catalogNotes" rows="3" placeholder="Opcional"></textarea>
              </label>

              <div class="modal-actions">
                <button class="secondary-button" type="button" :disabled="loading || submitting" @click="closeCatalogModal">Cancelar</button>
                <button class="primary-button" type="button" :disabled="loading || submitting" :aria-busy="isPending('add-catalog')" @click="handleAddCatalogCargo">
                  <span class="button-with-spinner">
                    <span v-if="isPending('add-catalog')" class="button-spinner" aria-hidden="true"></span>
                    <span>Confirmar</span>
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="customModalOpen" class="modal-backdrop" @click.self="closeCustomModal">
        <div class="modal modal-custom">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Carga personalizada</p>
              <h2>Nuevo cargamento</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCustomModal">Cerrar</button>
          </div>

          <p v-if="customModalError" class="error">{{ customModalError }}</p>

          <div class="two-columns">
            <label>
              <span>Nombre</span>
              <input v-model="customDisplayName" type="text" placeholder="Nombre de la carga" />
            </label>
            <label>
              <span>Categoría</span>
              <input v-model="customCategory" type="text" placeholder="Por ejemplo, Botín" />
            </label>
          </div>

          <div class="two-columns">
            <label>
              <span>Cantidad</span>
              <input v-model="customQuantity" type="number" min="1" :max="customQuantityMax" />
            </label>
            <label>
              <span>Unidades de carga</span>
              <input v-model="customCargoUnits" type="number" min="1" />
            </label>
          </div>

          <label>
            <span>Carro</span>
            <select v-model="customWagonId">
              <option value="">Selecciona un carro</option>
              <option v-for="wagon in customAvailableWagons" :key="wagon.id" :value="wagon.id">{{ wagon.name }}</option>
            </select>
          </label>
          <p class="muted">Solo se muestran carros válidos y con capacidad suficiente para esta carga.</p>

          <div class="two-columns">
            <label>
              <span>Origen</span>
              <input v-model="customOrigin" type="text" placeholder="Opcional" />
            </label>
            <label>
              <span>Mercancía específica</span>
              <input v-model="customSpecificCommodity" type="text" placeholder="Opcional" />
            </label>
          </div>

          <div class="two-columns">
            <label>
              <span>Deidad</span>
              <input v-model="customDeity" type="text" placeholder="Opcional" />
            </label>
            <label>
              <span>Notas</span>
              <input v-model="customNotes" type="text" placeholder="Opcional" />
            </label>
          </div>

          <div class="modal-actions">
            <button class="secondary-button" type="button" :disabled="loading || submitting" @click="closeCustomModal">Cancelar</button>
            <button class="primary-button" type="button" :disabled="loading || submitting" :aria-busy="isPending('add-custom')" @click="handleAddCustomCargo">
              <span class="button-with-spinner">
                <span v-if="isPending('add-custom')" class="button-spinner" aria-hidden="true"></span>
                <span>Confirmar</span>
              </span>
            </button>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="selectedCargo" class="modal-backdrop" @click.self="closeCargoDetails">
        <div class="modal modal-detail">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Detalle de carga</p>
              <h2>{{ selectedCargo.displayName }}</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCargoDetails">Cerrar</button>
          </div>

          <p v-if="selectedCargoError" class="error">{{ selectedCargoError }}</p>

          <div class="detail-grid">
            <section class="info-block">
              <h3>Datos</h3>
              <dl class="stats">
                <div><dt>Origen</dt><dd>{{ sourceLabel(selectedCargo.sourceType) }}</dd></div>
                <div><dt>Categoría</dt><dd>{{ selectedCargo.category }}</dd></div>
                <div><dt>Cantidad</dt><dd>{{ selectedCargo.quantity }}</dd></div>
                <div><dt>U.c. total</dt><dd>{{ formatCargoLoad(selectedCargo.quantity, selectedCargo.cargoUnits) }}</dd></div>
                <div><dt>Carro</dt><dd>{{ selectedCargo.wagonName ?? "Sin carro" }}</dd></div>
                <div><dt>Precio</dt><dd>{{ selectedCargo.priceExpression ?? "—" }}</dd></div>
              </dl>
            </section>
          </div>

          <section class="info-block">
            <h3>Asignación a carro</h3>
            <div class="two-columns">
              <label>
                <span>Carro</span>
                <select v-model="selectedCargoWagonId">
                  <option value="">Selecciona un carro</option>
                  <option v-for="wagon in selectedCargoAvailableWagons" :key="wagon.id" :value="wagon.id">{{ wagon.name }}</option>
                </select>
              </label>
              <div class="detail-summary">
                <strong>{{ selectedCargo.wagonName ?? "Sin carro" }}</strong>
                <p class="muted">Puedes mover esta carga a otro carro si hay capacidad disponible.</p>
              </div>
            </div>

            <div class="modal-actions">
              <button class="secondary-button" type="button" :disabled="loading || submitting" :aria-busy="isPending('delete-cargo')" @click="handleDeleteCargo">
                <span class="button-with-spinner">
                  <span v-if="isPending('delete-cargo')" class="button-spinner" aria-hidden="true"></span>
                  <span>Eliminar</span>
                </span>
              </button>
              <button class="primary-button" type="button" :disabled="loading || submitting || !selectedCargoWagonId" :aria-busy="isPending('move-cargo')" @click="handleMoveCargo">
                <span class="button-with-spinner">
                  <span v-if="isPending('move-cargo')" class="button-spinner" aria-hidden="true"></span>
                  <span>Guardar cambios</span>
                </span>
              </button>
            </div>
          </section>
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
  width: min(1300px, 100%);
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
  flex-wrap: wrap;
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

.card,
.modal {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.card {
  padding: 1.25rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin-top: 1rem;
}

.summary-card {
  padding: 0.95rem;
  border-radius: 0.9rem;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  display: grid;
  gap: 0.35rem;
}

.summary-bar {
  width: 100%;
  height: 0.55rem;
  border-radius: 999px;
  overflow: hidden;
  background: #e5e7eb;
}

.summary-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1d4ed8, #60a5fa);
}

.filters {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 0.8fr 0.8fr;
  gap: 0.75rem;
  margin: 1rem 0;
}

.filters label,
.two-columns label,
.catalog-list,
.catalog-preview,
.detail-summary {
  display: grid;
  gap: 0.35rem;
}

.filters span,
.two-columns span {
  font-size: 0.85rem;
  color: #6b7280;
}

.filters input,
.filters select,
.two-columns input,
.two-columns select,
.catalog-preview input,
.catalog-preview select,
.catalog-preview textarea,
.modal input,
.modal select,
.modal textarea {
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

.cargo-table {
  width: 100%;
  border-collapse: collapse;
}

.cargo-table th,
.cargo-table td {
  padding: 0.8rem;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  vertical-align: top;
}

.cargo-table tbody tr {
  cursor: pointer;
}

.cargo-table tbody tr:hover {
  background: #f8fafc;
}

.empty-state-inline {
  display: grid;
  place-items: center;
  text-align: center;
  gap: 0.4rem;
  padding: 1rem 0;
}

.primary-button,
.secondary-button,
.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.85rem;
  font: inherit;
  padding: 0.8rem 1rem;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  text-decoration: none;
}

.primary-button {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: white;
}

.secondary-button,
.ghost-button {
  background: white;
  color: #1f2937;
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
  width: min(1200px, 100%);
  max-height: 90vh;
  overflow: auto;
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
}

.modal-catalog {
  width: min(1180px, 100%);
}

.modal-custom,
.modal-detail {
  width: min(980px, 100%);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.catalog-layout {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 1rem;
}

.catalog-list {
  max-height: 68vh;
  overflow: auto;
}

.catalog-item {
  display: grid;
  gap: 0.2rem;
  text-align: left;
  padding: 0.9rem;
  border-radius: 0.9rem;
  border: 1px solid #e5e7eb;
  background: white;
  cursor: pointer;
}

.catalog-item.selected {
  background: #eff6ff;
  border-color: #93c5fd;
}

.catalog-preview {
  gap: 0.9rem;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.info-block {
  padding: 0.95rem;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 0.9rem;
  display: grid;
  gap: 0.4rem;
}

.two-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.detail-grid {
  display: grid;
  gap: 1rem;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.stats div {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #fff;
  border: 1px solid #e5e7eb;
}

dt {
  font-size: 0.8rem;
  color: #6b7280;
}

dd {
  margin: 0.2rem 0 0;
  font-size: 0.98rem;
  font-weight: 700;
}

.assignment-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.65rem;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 1100px) {
  .summary-grid,
  .filters,
  .catalog-layout,
  .detail-grid,
  .two-columns,
  .stats {
    grid-template-columns: 1fr;
  }

  .hero,
  .modal-header,
  .section-header {
    flex-direction: column;
    align-items: flex-start;
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
