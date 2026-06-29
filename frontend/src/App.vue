<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <strong>GestionCaravana</strong>
        <p>Caravana activa y carros</p>
      </div>

      <nav class="nav">
        <RouterLink to="/">Caravanas</RouterLink>
        <RouterLink to="/wagons">Carros</RouterLink>
      </nav>
    </header>

    <RouterView />
  </div>
</template>

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
</style>
