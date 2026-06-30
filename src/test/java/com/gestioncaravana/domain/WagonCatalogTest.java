package com.gestioncaravana.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WagonCatalogTest {

  @Test
  void exposesTheExpectedCatalogEntries() {
    assertThat(WagonCatalog.all())
        .extracting(WagonType::code)
        .contains(
            "carro-cubierto",
            "carro-de-viajeros",
            "carro-de-esclavista",
            "carro-de-prisioneros",
            "carro-zoologico",
            "carruaje-comodo",
            "carruaje-familiar",
            "carruaje-real",
            "trineo-de-pasajeros",
            "trineo-de-carga",
            "carro-biblioteca",
            "carro-descubierto",
            "carro-de-mercancias",
            "carro-de-mercancias-especificas",
            "carro-de-suministros",
            "carro-museo",
            "carro-arcano",
            "carro-de-adivino",
            "carro-con-taller",
            "carro-escuela",
            "carro-huerto",
            "carro-medico",
            "carro-vacio");
  }
}
