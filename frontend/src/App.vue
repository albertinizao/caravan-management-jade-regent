<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <strong>GestionCaravana</strong>
        <p>Caravana activa, carga, dotes y calendario</p>
      </div>

      <button
        type="button"
        class="nav-toggle"
        :aria-expanded="isMenuOpen"
        aria-controls="primary-navigation"
        @click="isMenuOpen = !isMenuOpen"
      >
        <span class="nav-toggle__icon" aria-hidden="true">{{ isMenuOpen ? "✕" : "☰" }}</span>
        <span>{{ isMenuOpen ? "Cerrar" : "Menú" }}</span>
      </button>

      <nav id="primary-navigation" class="nav" :class="{ 'nav--open': isMenuOpen }">
        <RouterLink to="/" @click="isMenuOpen = false">Caravanas</RouterLink>
        <RouterLink to="/calendar" @click="isMenuOpen = false">Calendario</RouterLink>
        <RouterLink to="/travelers" @click="isMenuOpen = false">Viajeros</RouterLink>
        <RouterLink to="/wagons" @click="isMenuOpen = false">Carros</RouterLink>
        <RouterLink to="/cargo" @click="isMenuOpen = false">Carga</RouterLink>
        <RouterLink to="/beasts" @click="isMenuOpen = false">Bestias</RouterLink>
        <RouterLink to="/feats" @click="isMenuOpen = false">Dotes</RouterLink>
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
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useToast } from "@/composables/useToast";

const route = useRoute();
const isMenuOpen = ref(false);

const { toast } = useToast();

watch(
  () => route.fullPath,
  () => {
    isMenuOpen.value = false;
  },
);
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
  flex-wrap: wrap;
  gap: 0.85rem;
  padding: 1rem var(--page-gutter);
  border-bottom: 1px solid #e5e7eb;
  background: white;
  position: sticky;
  top: 0;
  z-index: 20;
}

.brand {
  flex: 1 1 18rem;
  min-width: 0;
}

.brand strong {
  display: block;
  font-size: 1.05rem;
}

.topbar p {
  margin: 0;
  color: #6b7280;
  font-size: 0.88rem;
}

.nav-toggle {
  display: none;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 0.85rem;
  background: #f9fafb;
  color: #111827;
  padding: 0.7rem 0.95rem;
  font: inherit;
  font-weight: 600;
  min-height: 2.75rem;
}

.nav-toggle__icon {
  font-size: 1.1rem;
  line-height: 1;
}

.nav {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex: 1 1 auto;
  min-width: 0;
  margin-left: auto;
}

.nav a {
  display: inline-flex;
  align-items: center;
  min-height: 2.75rem;
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

@media (max-width: 960px) {
  .topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .brand {
    flex: 0 1 auto;
  }

  .nav-toggle {
    display: inline-flex;
    align-self: flex-start;
  }

  .nav {
    display: none;
    width: 100%;
    flex-direction: column;
    gap: 0.5rem;
    padding-top: 0.25rem;
    justify-content: flex-start;
  }

  .nav.nav--open {
    display: flex;
  }

  .nav a {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .topbar {
    padding-block: 0.85rem;
  }

  .nav-toggle {
    width: 100%;
    justify-content: center;
  }

  .nav a {
    justify-content: center;
  }
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

@media (max-width: 960px) {
  .global-toast {
    left: 50%;
    right: auto;
    transform: translateX(-50%);
    bottom: max(1rem, env(safe-area-inset-bottom));
    max-width: min(420px, calc(100vw - 1.5rem));
  }
}

@media (max-width: 480px) {
  .global-toast {
    width: min(100%, calc(100vw - 1rem));
    max-width: calc(100vw - 1rem);
    padding: 0.85rem 0.9rem;
  }
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

.global-toast--warning {
  background: #fffbeb;
  border-color: #fbbf24;
  color: #92400e;
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
