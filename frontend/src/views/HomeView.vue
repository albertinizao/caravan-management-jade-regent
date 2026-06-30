<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import {
  createCaravan,
  deleteCaravan,
  getActiveCaravan,
  getCaravanStatistics,
  listCaravans,
  selectActiveCaravan,
} from "@/services/caravans";
import { useToast } from "@/composables/useToast";
import type { Caravan, CaravanStatistics } from "@/types/caravan";

const caravans = ref<Caravan[]>([]);
const activeCaravan = ref<Caravan | null>(null);
const loading = ref(true);
const submitting = ref(false);
const pendingAction = ref<string | null>(null);
const error = ref<string | null>(null);
const name = ref("");
const description = ref("");
const offense = ref(1);
const defense = ref(1);
const mobility = ref(1);
const morale = ref(1);
const createModalOpen = ref(false);
const caravanStatistics = ref<CaravanStatistics | null>(null);
const { showToast } = useToast();

const hiddenContributionStats = new Set([
  "offense",
  "defense",
  "security",
  "determination",
  "travelerCapacity",
  "cargoCapacity",
  "cargoLoad",
  "consumption",
  "beastCount",
  "wagonCount",
  "travelerCount",
]);

const selectedCaravan = computed(() => activeCaravan.value ?? caravans.value.find((caravan) => caravan.active) ?? null);
const allocatedPoints = computed(() => offense.value + defense.value + mobility.value + morale.value - 4);
const remainingPoints = computed(() => 3 - allocatedPoints.value);
const visibleContributions = computed(() =>
  caravanStatistics.value?.contributions.filter((item) => !hiddenContributionStats.has(item.statCode)) ?? [],
);

function isPending(action: string) {
  return pendingAction.value === action;
}

function openCreateModal() {
  name.value = "";
  description.value = "";
  offense.value = 1;
  defense.value = 1;
  mobility.value = 1;
  morale.value = 1;
  createModalOpen.value = true;
}

function closeCreateModal() {
  createModalOpen.value = false;
}

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
    const selectedId = activeResponse.caravan?.id ?? caravanList.find((caravan) => caravan.active)?.id ?? null;
    caravanStatistics.value = selectedId ? await getCaravanStatistics(selectedId) : null;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load caravans";
  } finally {
    loading.value = false;
    pendingAction.value = trackRefresh ? null : previousAction;
  }
}

async function handleCreate() {
  if (!name.value.trim()) {
    error.value = "Caravan name is required";
    return;
  }

  const caravanName = name.value.trim();
  submitting.value = true;
  pendingAction.value = "create";
  error.value = null;

  try {
    const created = await createCaravan({
      name: caravanName,
      description: description.value.trim() || undefined,
      offense: offense.value,
      defense: defense.value,
      mobility: mobility.value,
      morale: morale.value,
    });

    const active = await selectActiveCaravan({ caravanId: created.id });
    await refresh();
    activeCaravan.value = active.caravan;
    closeCreateModal();
    name.value = "";
    description.value = "";
    offense.value = 1;
    defense.value = 1;
    mobility.value = 1;
    morale.value = 1;
    showToast(`Caravana creada: ${caravanName}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to create caravan";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleSelect(caravanId: string) {
  submitting.value = true;
  pendingAction.value = `select:${caravanId}`;
  error.value = null;

  try {
    const active = await selectActiveCaravan({ caravanId });
    activeCaravan.value = active.caravan;
    await refresh();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to select caravan";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleDelete(caravan: Caravan) {
  const confirmed = window.confirm(
    `¿Seguro que quieres eliminar la caravana "${caravan.name}"? Esta acción no se puede deshacer.`
  );

  if (!confirmed) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `delete:${caravan.id}`;
  error.value = null;

  try {
    await deleteCaravan(caravan.id);
    await refresh();
    showToast(`Caravana eliminada: ${caravan.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to delete caravan";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

onMounted(refresh);
</script>

<template>
  <main class="page">
    <section class="shell">
      <header class="hero">
        <div>
          <p class="eyebrow">GestionCaravana</p>
          <h1>Caravana activa</h1>
          <p class="subtitle">
            Crea una campaña, selecciónala y trabaja siempre sobre la misma instancia.
          </p>
        </div>
        <div class="hero-actions">
          <button class="secondary-button" type="button" :disabled="loading || submitting" @click="openCreateModal">
            Crear caravana
          </button>
          <button class="ghost-button" type="button" :disabled="loading || submitting" @click="refresh">
            <span class="button-with-spinner">
              <span v-if="isPending('refresh')" class="button-spinner" aria-hidden="true"></span>
              <span>{{ isPending('refresh') ? "Refrescando…" : "Refrescar" }}</span>
            </span>
          </button>
        </div>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <section class="grid">
        <article class="card">
          <h2>Caravana seleccionada</h2>

          <div v-if="loading" class="muted">Cargando caravanas…</div>
          <div v-else-if="selectedCaravan" class="detail">
            <div class="title-row">
              <strong>{{ selectedCaravan.name }}</strong>
              <span class="pill" :class="{ active: selectedCaravan.active }">
                {{ selectedCaravan.active ? "Activa" : "Disponible" }}
              </span>
            </div>
            <p v-if="selectedCaravan.description" class="muted">
              {{ selectedCaravan.description }}
            </p>

            <dl class="stats">
              <div>
                <dt>Nivel</dt>
                <dd>{{ selectedCaravan.level }}</dd>
              </div>
              <div>
                <dt>Descontento</dt>
                <dd>{{ selectedCaravan.discontent }}</dd>
              </div>
              <div>
                <dt>Ofensiva</dt>
                <dd>{{ selectedCaravan.mainStats.offense }}</dd>
              </div>
              <div>
                <dt>Defensiva</dt>
                <dd>{{ selectedCaravan.mainStats.defense }}</dd>
              </div>
              <div>
                <dt>Movilidad</dt>
                <dd>{{ selectedCaravan.mainStats.mobility }}</dd>
              </div>
              <div>
                <dt>Moral</dt>
                <dd>{{ selectedCaravan.mainStats.morale }}</dd>
              </div>
              <div>
                <dt>Puntos libres</dt>
                <dd>{{ selectedCaravan.mainStats.unassignedPoints }}</dd>
              </div>
            </dl>

            <div v-if="caravanStatistics" class="stats-panel">
              <section>
                <h3>Estadísticas derivadas</h3>
                <dl class="stats stats-2">
                  <div>
                    <dt>Ataque</dt>
                    <dd>{{ caravanStatistics.derivedStats.attack }}</dd>
                  </div>
                  <div>
                    <dt>CA</dt>
                    <dd>{{ caravanStatistics.derivedStats.armorClass }}</dd>
                  </div>
                  <div>
                    <dt>Seguridad</dt>
                    <dd>{{ caravanStatistics.derivedStats.security }}</dd>
                  </div>
                  <div>
                    <dt>Determinación</dt>
                    <dd>{{ caravanStatistics.derivedStats.determination }}</dd>
                  </div>
                </dl>
              </section>

              <section>
                <h3>Otros atributos</h3>
                <dl class="stats stats-3">
                  <div>
                    <dt>Velocidad</dt>
                    <dd>{{ caravanStatistics.otherStats.speed }} mi/día</dd>
                  </div>
                  <div>
                    <dt>Viajeros</dt>
                    <dd>{{ caravanStatistics.otherStats.travelerCount }} / {{ caravanStatistics.otherStats.travelerCapacity }}</dd>
                  </div>
                  <div>
                    <dt>Carros</dt>
                    <dd>{{ caravanStatistics.otherStats.wagonCount }} / {{ caravanStatistics.otherStats.maxWagons }}</dd>
                  </div>
                  <div>
                    <dt>Cargamento</dt>
                    <dd>{{ caravanStatistics.otherStats.cargoLoad }} / {{ caravanStatistics.otherStats.cargoCapacity }}</dd>
                  </div>
                  <div>
                    <dt>Consumo</dt>
                    <dd>{{ caravanStatistics.otherStats.consumption }}</dd>
                  </div>
                  <div>
                    <dt>Bestias</dt>
                    <dd>{{ caravanStatistics.otherStats.beastCount }}</dd>
                  </div>
                </dl>
              </section>

              <section>
                <h3>Descontento</h3>
                <p :class="['warning-banner', { danger: caravanStatistics.discontent >= caravanStatistics.moraleThreshold }]">
                  Descontento: {{ caravanStatistics.discontent }} · Umbral de motín: {{ caravanStatistics.moraleThreshold }}
                </p>
                <p v-if="caravanStatistics.warnings.length > 0" class="muted">
                  {{ caravanStatistics.warnings.join(" ") }}
                </p>
              </section>

            <section>
              <h3>Bonificadores aplicados</h3>
              <ul class="contribution-list">
                  <li v-for="item in visibleContributions" :key="`${item.statCode}-${item.sourceType}-${item.sourceId}-${item.modifier}`">
                    <strong>{{ item.statCode }}</strong>
                    <span>{{ item.sourceName }} · {{ item.modifier }} · {{ item.reason }}</span>
                  </li>
                  <li v-if="visibleContributions.length === 0" class="muted contribution-empty">
                    No hay bonificadores relevantes para mostrar.
                  </li>
                </ul>
              </section>
            </div>

            <div class="sections">
              <section>
                <h3>Carros</h3>
                <p class="muted">{{ selectedCaravan.wagons.length }} elementos</p>
                <RouterLink class="section-link" to="/wagons">Abrir vista de carros</RouterLink>
              </section>
              <section>
                <h3>Viajeros</h3>
                <p class="muted">{{ selectedCaravan.travelers.length }} elementos</p>
                <RouterLink class="section-link" to="/travelers">Abrir vista de viajeros</RouterLink>
              </section>
              <section>
                <h3>Bestias</h3>
                <p class="muted">{{ selectedCaravan.beasts.length }} elementos</p>
                <RouterLink class="section-link" to="/beasts">Abrir vista de bestias</RouterLink>
              </section>
              <section>
                <h3>Dotes</h3>
                <p class="muted">{{ selectedCaravan.feats.length }} elementos</p>
              </section>
            </div>
          </div>

          <div v-else class="muted">
            Todavía no hay una caravana activa.
          </div>
        </article>
      </section>

      <article class="card">
        <h2>Instancias de caravana</h2>

        <div v-if="!loading && caravans.length === 0" class="muted">
          No hay caravanas creadas todavía.
        </div>

        <div class="list">
          <div
            v-for="caravan in caravans"
            :key="caravan.id"
            class="list-item"
            :class="{ selected: caravan.active }"
          >
            <div>
              <strong>{{ caravan.name }}</strong>
              <p v-if="caravan.description" class="muted">{{ caravan.description }}</p>
            </div>
            <div class="actions">
              <button class="secondary-button" type="button" :disabled="loading || submitting" @click.stop="handleSelect(caravan.id)">
                <span class="button-with-spinner">
                  <span v-if="isPending(`select:${caravan.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending(`select:${caravan.id}`) ? "Seleccionando…" : "Seleccionar" }}</span>
                </span>
              </button>
              <button class="danger-button" type="button" :disabled="loading || submitting" @click.stop="handleDelete(caravan)">
                <span class="button-with-spinner">
                  <span v-if="isPending(`delete:${caravan.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending(`delete:${caravan.id}`) ? "Eliminando…" : "Eliminar" }}</span>
                </span>
              </button>
              <span class="pill" :class="{ active: caravan.active }">
                {{ caravan.active ? "Activa" : "Disponible" }}
              </span>
            </div>
          </div>
        </div>
      </article>
    </section>

    <teleport to="body">
      <div v-if="createModalOpen" class="modal-backdrop" @click.self="closeCreateModal">
        <div class="modal modal-create">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Nueva caravana</p>
              <h2>Crear caravana</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCreateModal">Cerrar</button>
          </div>

          <form class="form" @submit.prevent="handleCreate">
            <label>
              <span>Nombre</span>
              <input v-model="name" type="text" placeholder="Campaña del norte" />
            </label>

            <label>
              <span>Descripción</span>
              <textarea v-model="description" rows="3" placeholder="Opcional"></textarea>
            </label>

            <div class="allocation-grid">
              <label>
                <span>Ofensiva</span>
                <input v-model.number="offense" type="number" min="1" max="10" />
              </label>
              <label>
                <span>Defensiva</span>
                <input v-model.number="defense" type="number" min="1" max="10" />
              </label>
              <label>
                <span>Movilidad</span>
                <input v-model.number="mobility" type="number" min="1" max="10" />
              </label>
              <label>
                <span>Moral</span>
                <input v-model.number="morale" type="number" min="1" max="10" />
              </label>
            </div>
            <p class="muted">
              Puntos asignados: {{ allocatedPoints }} / 3 · Puntos libres: {{ remainingPoints }}
            </p>

            <div class="modal-actions">
              <button class="secondary-button" type="button" @click="closeCreateModal">Cancelar</button>
              <button class="primary-button" type="submit" :disabled="loading || submitting || remainingPoints < 0" :aria-busy="isPending('create')">
                <span class="button-with-spinner">
                  <span v-if="isPending('create')" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending('create') ? "Creando…" : "Crear y seleccionar" }}</span>
                </span>
              </button>
            </div>
          </form>
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
  width: min(1100px, 100%);
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
p {
  margin: 0;
}

.subtitle,
.muted {
  color: #6b7280;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1.25rem;
}

.hero-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.card {
  padding: 1.25rem;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: white;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.form {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

label {
  display: grid;
  gap: 0.35rem;
}

input,
textarea {
  width: 100%;
  padding: 0.8rem 0.9rem;
  border: 1px solid #d1d5db;
  border-radius: 0.75rem;
  font: inherit;
}

.primary-button,
.ghost-button,
.secondary-button,
.danger-button,
.list-item {
  border-radius: 0.85rem;
  font: inherit;
}

.primary-button,
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
  background: white;
  border: 1px solid #cbd5e1;
  padding: 0.7rem 0.9rem;
  cursor: pointer;
}

.danger-button {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
  padding: 0.7rem 0.9rem;
  cursor: pointer;
}

.error {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  background: #fef2f2;
  color: #b91c1c;
}

.detail {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.pill {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  background: #e5e7eb;
  color: #374151;
  font-size: 0.875rem;
}

.pill.active {
  background: #dbeafe;
  color: #1d4ed8;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.stats.stats-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stats.stats-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  font-size: 1.15rem;
  font-weight: 700;
}

.sections {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.sections section {
  padding: 0.85rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.85rem;
  display: grid;
  gap: 0.35rem;
}

.section-link {
  color: #1d4ed8;
  text-decoration: none;
  font-weight: 600;
}

.list {
  display: grid;
  gap: 0.75rem;
  margin-top: 1rem;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.list-item.selected {
  border-color: #1d4ed8;
  background: #eff6ff;
}

.allocation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.allocation-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.stats-panel {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

.contribution-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.5rem;
}

.contribution-list li {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
}

.contribution-empty {
  list-style: none;
  text-align: center;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  padding: 1.5rem;
  z-index: 30;
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
  width: min(900px, 100%);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.warning-banner {
  padding: 0.85rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 12px;
}

.warning-banner.danger {
  border-color: rgba(220, 38, 38, 0.45);
  background: rgba(254, 226, 226, 0.9);
}

@media (max-width: 900px) {
  .grid,
  .stats,
  .sections {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
