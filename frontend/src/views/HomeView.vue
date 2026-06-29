<template>
  <main class="page">
    <section class="card">
      <p class="eyebrow">GestionCaravana</p>
      <h1>Hexagonal backend + Vue frontend skeleton</h1>
      <p>
        This is the starting point for the project structure, not the business logic.
      </p>

      <ul>
        <li>Backend: Java 25 + Spring Boot</li>
        <li>Frontend: Node 22 + Vue 3 + Vite</li>
        <li>Boundaries: domain, application, adapters</li>
      </ul>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2rem;
}

.card {
  width: min(42rem, 100%);
  padding: 2rem;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: white;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.eyebrow {
  margin: 0 0 0.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6b7280;
}

h1 {
  margin: 0 0 1rem;
  font-size: clamp(2rem, 5vw, 3rem);
  line-height: 1.1;
}

p {
  margin: 0 0 1rem;
}

ul {
  margin: 0;
  padding-left: 1.25rem;
}
</style>
