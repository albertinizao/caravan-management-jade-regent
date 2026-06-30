<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <strong>GestionCaravana</strong>
        <p>Caravana activa y carga</p>
      </div>

      <nav class="nav">
        <RouterLink to="/">Caravanas</RouterLink>
        <RouterLink to="/travelers">Viajeros</RouterLink>
        <RouterLink to="/wagons">Carros</RouterLink>
        <RouterLink to="/cargo">Carga</RouterLink>
        <RouterLink to="/beasts">Bestias</RouterLink>
      </nav>
    </header>

    <RouterView />

    <teleport to="body">
      <Transition name="toast" mode="out-in">
        <div
          v-if="toast"
          :key="toast.id"
          class="global-toast"
          :class="`global-toast--${toast.variant}`"
          role="status"
          aria-live="polite"
        >
          {{ toast.message }}
        </div>
      </Transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { useToast } from "@/composables/useToast";

const { toast } = useToast();
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f8fafc;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  background: white;
}

.topbar p {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.nav {
  display: flex;
  gap: 0.75rem;
}

.nav a {
  color: #1f2937;
  text-decoration: none;
  padding: 0.55rem 0.85rem;
  border-radius: 0.75rem;
  background: #f3f4f6;
}

.nav a.router-link-active {
  background: #dbeafe;
  color: #1d4ed8;
}

.global-toast {
  position: fixed;
  right: 1.25rem;
  bottom: 1.25rem;
  z-index: 1000;
  max-width: min(420px, calc(100vw - 2.5rem));
  padding: 0.95rem 1rem;
  border-radius: 0.95rem;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
  border: 1px solid transparent;
  font-weight: 600;
}

.global-toast--success {
  background: #ecfdf5;
  border-color: #86efac;
  color: #166534;
}

.global-toast--error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}

.global-toast--info {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
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
  transform: translateY(0.4rem) scale(0.98);
}
</style>
