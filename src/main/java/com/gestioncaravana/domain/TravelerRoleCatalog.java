package com.gestioncaravana.domain;

import java.util.List;
import java.util.Optional;

public final class TravelerRoleCatalog {

  public static final String PASSENGER_CODE = "pasajero";
  public static final String CARRETERO_CODE = "carretero";

  private static final List<TravelerRoleCatalogItem> ITEMS = List.of(
      new TravelerRoleCatalogItem("adivino", "Adivino", "Consejero espiritual y guía de la caravana.", "Ser capaz de lanzar conjuros de adivinación.", false),
      new TravelerRoleCatalogItem("agricultor", "Agricultor", "Cultiva comida para la caravana.", "1 rango en profesión (jardinero o agricultor).", false, TravelerRoleHelperBenefitMode.PERIODIC, 4),
      new TravelerRoleCatalogItem("animador-itinerante", "Animador itinerante", "Consigue dinero entreteniendo en asentamientos.", "1 rango en interpretar.", false),
      new TravelerRoleCatalogItem("artesano", "Artesano", "Trabaja en piezas de artesanía.", "1 rango de artesanía.", false),
      new TravelerRoleCatalogItem("artillero", "Artillero", "Maneja máquinas de asedio.", "Competencia con armas de asedio.", false),
      new TravelerRoleCatalogItem("batidor", "Batidor", "Explora o caza lejos de la caravana.", "1 rango en supervivencia.", false),
      new TravelerRoleCatalogItem("boticario", "Boticario", "Produce artículos curativos y apoya el descanso.", "1 rango en saber (naturaleza) o artesanía (alquimia).", false),
      new TravelerRoleCatalogItem("carrero", "Carrero", "Realiza reparaciones sobre la marcha.", "1 rango en artesanía (carpintería) o profesión (ingeniero o carrero).", false),
      new TravelerRoleCatalogItem(CARRETERO_CODE, "Carretero", "Controla los carros durante el viaje.", "1 rango en profesión (carretero) o trato con animales.", false),
      new TravelerRoleCatalogItem("cocinero", "Cocinero", "Mejora el rendimiento de los suministros.", "1 rango en una profesión culinaria relacionada.", false, TravelerRoleHelperBenefitMode.DAILY, null),
      new TravelerRoleCatalogItem("comediante", "Comediante", "Mejora la determinación de la caravana.", "1 rango en interpretar.", false),
      new TravelerRoleCatalogItem("comerciante", "Comerciante", "Facilita transacciones comerciales.", "1 rango en Diplomacia, engañar o Profesión (mercader).", false),
      new TravelerRoleCatalogItem("cronista", "Cronista", "Registra la historia de la caravana.", "1 rango en Saber (historia), Interpretar (escritura) o Profesión (escriba).", false),
      new TravelerRoleCatalogItem("diplomatico", "Diplomático", "Mejora las transacciones en asentamientos.", "1 rango en Tasación.", false),
      new TravelerRoleCatalogItem("domador", "Domador", "Entrena animales y apoya pruebas de determinación.", "1 rango en Trato con animales.", false),
      new TravelerRoleCatalogItem("encargado-de-suministros", "Encargado de suministros", "Aumenta la capacidad de transporte.", "1 rango en Saber (ingeniería).", false),
      new TravelerRoleCatalogItem("esclavo", "Esclavo", "Trabaja forzadamente dentro de la caravana.", "Debe ser comprado o forzado a unirse.", false),
      new TravelerRoleCatalogItem("esclavista", "Esclavista", "Evita que los esclavos huyan.", "1 rango en intimidar o percepción.", false),
      new TravelerRoleCatalogItem("explorador-nocturno", "Explorador nocturno", "Explora con ventaja durante la noche.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("guarda", "Guarda", "Defiende a la caravana.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("guia", "Guía", "Ayuda a la caravana a orientarse.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("heroe", "Héroe", "Aporta liderazgo y valor.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("instructor", "Instructor", "Enseña a otros viajeros.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("lanzador-de-conjuros", "Lanzador de conjuros", "Realiza magia útil para la caravana.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("lenador", "Leñador", "Aporta recursos y trabajo físico.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("lider-de-la-caravana", "Líder de la caravana", "Representa y coordina la caravana.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("meteorologo", "Meteorólogo", "Predice el clima y ayuda a planificar el viaje.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("nevero", "Nevero", "Consigue hielo para la caravana.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem(PASSENGER_CODE, "Pasajero", "No aporta beneficios estadísticos.", "Ninguno.", false),
      new TravelerRoleCatalogItem("prisionero", "Prisionero", "Viajero retenido por la caravana.", "Ninguno.", false),
      new TravelerRoleCatalogItem("profesor", "Profesor", "Enseña habilidades a otros viajeros.", "5 rangos en la habilidad a enseñar.", false),
      new TravelerRoleCatalogItem("sanador", "Sanador", "Mejora la recuperación de los heridos.", "Requisitos definidos por la mesa.", false),
      new TravelerRoleCatalogItem("sirviente", "Sirviente", "Sirve a otro viajero concreto.", "Debe elegirse el viajero al que sirve.", true));

  private TravelerRoleCatalog() {}

  public static List<TravelerRoleCatalogItem> all() {
    return ITEMS;
  }

  public static Optional<TravelerRoleCatalogItem> findByCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    return ITEMS.stream().filter(item -> item.code().equals(code)).findFirst();
  }

  public static boolean requiresTargetTraveler(String code) {
    return findByCode(code).map(TravelerRoleCatalogItem::requiresTargetTraveler).orElse(false);
  }
}
