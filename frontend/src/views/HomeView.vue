<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import {
  createCaravan,
  deleteCaravan,
  getActiveCaravan,
  listCaravans,
  selectActiveCaravan,
} from "@/services/caravans";
import type { Caravan } from "@/types/caravan";

const caravans = ref<Caravan[]>([]);
const activeCaravan = ref<Caravan | null>(null);
const loading = ref(true);
const submitting = ref(false);
const error = ref<string | null>(null);
const name = ref("");
const description = ref("");

const selectedCaravan = computed(() => activeCaravan.value ?? caravans.value.find((caravan) => caravan.active) ?? null);

async function refresh() {
  loading.value = true;
  error.value = null;

  try {
    const [caravanList, activeResponse] = await Promise.all([listCaravans(), getActiveCaravan()]);
    caravans.value = caravanList;
    activeCaravan.value = activeResponse.caravan;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load caravans";
  } finally {
    loading.value = false;
  }
}

async function handleCreate() {
  if (!name.value.trim()) {
    error.value = "Caravan name is required";
    return;
  }

  submitting.value = true;
  error.value = null;

  try {
    const created = await createCaravan({
      name: name.value.trim(),
      description: description.value.trim() || undefined,
    });

    const active = await selectActiveCaravan({ caravanId: created.id });
    await refresh();
    activeCaravan.value = active.caravan;
    name.value = "";
    description.value = "";
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to create caravan";
  } finally {
    submitting.value = false;
  }
}

async function handleSelect(caravanId: string) {
  submitting.value = true;
  error.value = null;

  try {
    const active = await selectActiveCaravan({ caravanId });
    activeCaravan.value = active.caravan;
    await refresh();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to select caravan";
  } finally {
    submitting.value = false;
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
  error.value = null;

  try {
    await deleteCaravan(caravan.id);
    await refresh();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to delete caravan";
  } finally {
    submitting.value = false;
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
        <button class="ghost-button" type="button" @click="refresh">Refrescar</button>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <section class="grid">
        <article class="card">
          <h2>Nueva caravana</h2>
          <form class="form" @submit.prevent="handleCreate">
            <label>
              <span>Nombre</span>
              <input v-model="name" type="text" placeholder="Campaña del norte" />
            </label>

            <label>
              <span>Descripción</span>
              <textarea v-model="description" rows="3" placeholder="Opcional"></textarea>
            </label>

            <button class="primary-button" type="submit" :disabled="submitting">
              Crear y seleccionar
            </button>
          </form>
        </article>

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

            <div class="sections">
              <section>
                <h3>Carros</h3>
                <p class="muted">{{ selectedCaravan.wagons.length }} elementos</p>
              </section>
              <section>
                <h3>Viajeros</h3>
                <p class="muted">{{ selectedCaravan.travelers.length }} elementos</p>
              </section>
              <section>
                <h3>Bestias</h3>
                <p class="muted">{{ selectedCaravan.beasts.length }} elementos</p>
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
              <button class="secondary-button" type="button" @click.stop="handleSelect(caravan.id)">
                Seleccionar
              </button>
              <button class="danger-button" type="button" @click.stop="handleDelete(caravan)">
                Eliminar
              </button>
              <span class="pill" :class="{ active: caravan.active }">
                {{ caravan.active ? "Activa" : "Disponible" }}
              </span>
            </div>
          </div>
        </div>
      </article>
    </section>
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
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
