# Calendario de Golarion

## Cómputo de Absalom, festividades y cumpleaños de la campaña *El Regente de Jade*

Este documento reúne en una sola referencia:

1. el funcionamiento completo del **Cómputo de Absalom**;
2. los nombres españoles de meses y días de la semana;
3. las reglas para calcular cualquier fecha;
4. la correspondencia adoptada entre años AR y años terrestres;
5. los equinoccios, solsticios, lunas nuevas y lunas llenas entre **4712 y 4722 AR**;
6. las festividades fijas y móviles;
7. los cumpleaños establecidos en el canon interno de la campaña.

> **Convención principal de la campaña:** el **1 de Abadio de 4712 AR es Día de la Luna**, equivalente funcional al lunes.

> **Prioridad de canon:** los cumpleaños y decisiones cronológicas fijados por la campaña prevalecen sobre cualquier fuente secundaria. La correspondencia astronómica con la Tierra que se explica más adelante es también una convención interna del proyecto.

---

# 1. El Cómputo de Absalom

El **Cómputo de Absalom**, abreviado **AR** por *Absalom Reckoning*, es el sistema de datación más extendido en Avistan y Garund. Cuenta los años desde la fundación de Absalom por Aroden.

La era comienza el:

> **1 de Abadio del año 1 AR**

Las fechas se escriben normalmente como:

> **día de mes de año AR**

Ejemplo:

> **23 de Gozran de 4713 AR**

Un año ordinario tiene **365 días** y un año bisiesto tiene **366**. El calendario emplea:

- **12 meses**;
- semanas de **7 días**;
- **52 semanas completas**, más un día sobrante en los años ordinarios y dos en los bisiestos;
- días de **24 horas**;
- meses con la misma distribución de duraciones que el calendario gregoriano;
- un día adicional al final de Calistril cada ocho años.

## 1.1. Años bisiestos

Un año AR es bisiesto cuando su número es divisible entre ocho:

```text
año_bisiesto = (año_AR mod 8 = 0)
```

En un año bisiesto:

- Calistril tiene **29 días**;
- el resto de meses no cambia;
- el año siguiente comienza dos posiciones más adelante en el ciclo semanal, en lugar de una.

Entre 4712 y 4722 son bisiestos:

- **4712 AR**;
- **4720 AR**.

## 1.2. Consecuencia importante de la equivalencia terrestre

La regla bisiesta de Golarion **no es la misma** que la gregoriana. Por ejemplo:

- 4716 AR se corresponde astronómicamente con 2016 d. C.;
- 2016 fue bisiesto en la Tierra;
- **4716 AR no es bisiesto** en el Cómputo de Absalom.

Por tanto, la correspondencia con la Tierra se usa para trasladar **fenómenos astronómicos**, no para copiar la estructura semanal ni la regla bisiesta terrestre.

---

# 2. Los meses

| Mes en español | Nombre inglés | Días | Mes terrestre equivalente | Deidad asociada |
|---|---|---:|---|---|
| Abadio | Abadius | 31 | enero | Abadar |
| Calistril | Calistril | 28; 29 en año bisiesto | febrero | Calistria |
| Farasto | Pharast | 31 | marzo | Pharasma |
| Gozran | Gozran | 30 | abril | Gozreh |
| Desnio | Desnus | 31 | mayo | Desna |
| Sarenith | Sarenith | 30 | junio | Sarenrae |
| Erasto | Erastus | 31 | julio | Erastil |
| Arodio | Arodus | 31 | agosto | Aroden |
| Rova | Rova | 30 | septiembre | Rovagug |
| Lamashan | Lamashan | 31 | octubre | Lamashtu |
| Neth | Neth | 30 | noviembre | Nethys |
| Kuthona | Kuthona | 31 | diciembre | Zon-Kuthon |

Los nombres religiosos son denominaciones históricas del calendario civil. No implican que la población de una región venere necesariamente a la deidad que da nombre al mes.

---

# 3. Los días de la semana

En este documento se emplean siempre los nombres españoles. El nombre inglés se incluye solo como referencia.

| Nombre español | Nombre inglés | Equivalencia funcional | Abrev. | Asociación habitual |
|---|---|---|---|---|
| Día de la Luna | Moonday | lunes | Lun | Primer día del ciclo semanal; asociado a la Luna y a ritos nocturnos. |
| Día del Trabajo | Toilday | martes | Tra | Jornada ordinaria de labor, oficio y mantenimiento. |
| Día de la Fortuna | Wealday | miércoles | For | Día asociado a fortuna, bienestar y prosperidad. |
| Día del Juramento | Oathday | jueves | Jur | Día preferido para juramentos, contratos, tratados y ceremonias formales. |
| Día del Fuego | Fireday | viernes | Fue | Día asociado al fuego; habitual para mercados y celebraciones públicas. |
| Día de las Estrellas | Starday | sábado | Est | Día asociado a las estrellas, los viajes y la contemplación. |
| Día del Sol | Sunday | domingo | Sol | Día habitual de descanso, culto y reunión comunitaria. |

## 3.1. Orden semanal

El ciclo es siempre:

> Día de la Luna → Día del Trabajo → Día de la Fortuna → Día del Juramento → Día del Fuego → Día de las Estrellas → Día del Sol → Día de la Luna

## 3.2. Correcciones ortográficas de los nombres ingleses

Las formas inglesas correctas son:

- **Moonday**;
- **Toilday**;
- **Wealday**;
- **Oathday**;
- **Fireday**;
- **Starday**;
- **Sunday**.

Por tanto, *Toildayt* y *Fierday* se consideran erratas.

---

# 4. Cómo calcular el día de la semana

Se toma como ancla:

> **1 de Abadio de 4712 AR = Día de la Luna**

Para cualquier fecha posterior:

1. se cuentan los días completos transcurridos desde esa fecha;
2. se añade un día extra por cada año bisiesto atravesado;
3. se calcula el resto al dividir entre siete;
4. el resto indica la posición dentro del ciclo semanal.

En pseudocódigo:

```text
desplazamiento =
    días de los años completos desde 4712
  + días de los meses completos del año actual
  + (día - 1)

índice_semana = desplazamiento mod 7
```

Correspondencia de índices:

```text
0 = Día de la Luna
1 = Día del Trabajo
2 = Día de la Fortuna
3 = Día del Juramento
4 = Día del Fuego
5 = Día de las Estrellas
6 = Día del Sol
```

Esta regla permite calcular cualquier fecha sin consultar las tablas de este documento.

---

# 5. Correspondencia entre AR y el calendario terrestre

La campaña adopta la siguiente equivalencia:

```text
año terrestre d. C. = año AR - 2700
año AR = año terrestre d. C. + 2700
```

Ejemplos:

| Año AR | Año terrestre equivalente |
|---:|---:|
| 4712 AR | 2012 d. C. |
| 4715 AR | 2015 d. C. |
| 4720 AR | 2020 d. C. |
| 4722 AR | 2022 d. C. |

## 5.1. Qué se traslada desde la Tierra

Se copian del año terrestre equivalente:

- el **día y mes** de equinoccios y solsticios;
- la **hora UTC de referencia** de esos fenómenos;
- el día y la hora de lunas nuevas y lunas llenas.

## 5.2. Qué no se traslada

No se copia:

- el día de la semana terrestre;
- la regla bisiesta gregoriana;
- la numeración de semanas;
- fiestas civiles terrestres;
- zonas horarias terrestres como parte del canon de Golarion.

El día semanal de cada fenómeno se recalcula con el calendario de Golarion.

## 5.3. Hora de referencia

Las tablas astronómicas usan **UTC** como meridiano técnico de referencia. Esto permite fijar un único instante sin inventar zonas horarias para cada región de Golarion.

En mesa pueden darse dos usos:

- **Uso sencillo:** emplear el día indicado en la tabla para todo el mundo.
- **Uso astronómico estricto:** desplazar la fecha local si la región está lo bastante al este o al oeste como para que el instante caiga en el día anterior o posterior.

Para una campaña de viaje, el primer método suele ser más práctico.

---

# 6. Resumen de los años 4712–4722

| Año AR | Equivalente terrestre | Primer día del año | Bisiesto AR | Días |
|---:|---:|---|---|---:|
| 4712 AR | 2012 d. C. | Día de la Luna | Sí | 366 |
| 4713 AR | 2013 d. C. | Día de la Fortuna | No | 365 |
| 4714 AR | 2014 d. C. | Día del Juramento | No | 365 |
| 4715 AR | 2015 d. C. | Día del Fuego | No | 365 |
| 4716 AR | 2016 d. C. | Día de las Estrellas | No | 365 |
| 4717 AR | 2017 d. C. | Día del Sol | No | 365 |
| 4718 AR | 2018 d. C. | Día de la Luna | No | 365 |
| 4719 AR | 2019 d. C. | Día del Trabajo | No | 365 |
| 4720 AR | 2020 d. C. | Día de la Fortuna | Sí | 366 |
| 4721 AR | 2021 d. C. | Día del Fuego | No | 365 |
| 4722 AR | 2022 d. C. | Día de las Estrellas | No | 365 |

---

# 7. Día de inicio de cada mes

Leyenda:

- **Lun:** Día de la Luna
- **Tra:** Día del Trabajo
- **For:** Día de la Fortuna
- **Jur:** Día del Juramento
- **Fue:** Día del Fuego
- **Est:** Día de las Estrellas
- **Sol:** Día del Sol

| Año | Abadio | Calistril | Farasto | Gozran | Desnio | Sarenith | Erasto | Arodio | Rova | Lamashan | Neth | Kuthona |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 4712 | Lun | Jur | Fue | Lun | For | Est | Lun | Jur | Sol | Tra | Fue | Sol |
| 4713 | For | Est | Est | Tra | Jur | Sol | Tra | Fue | Lun | For | Est | Lun |
| 4714 | Jur | Sol | Sol | For | Fue | Lun | For | Est | Tra | Jur | Sol | Tra |
| 4715 | Fue | Lun | Lun | Jur | Est | Tra | Jur | Sol | For | Fue | Lun | For |
| 4716 | Est | Tra | Tra | Fue | Sol | For | Fue | Lun | Jur | Est | Tra | Jur |
| 4717 | Sol | For | For | Est | Lun | Jur | Est | Tra | Fue | Sol | For | Fue |
| 4718 | Lun | Jur | Jur | Sol | Tra | Fue | Sol | For | Est | Lun | Jur | Est |
| 4719 | Tra | Fue | Fue | Lun | For | Est | Lun | Jur | Sol | Tra | Fue | Sol |
| 4720 | For | Est | Sol | For | Fue | Lun | For | Est | Tra | Jur | Sol | Tra |
| 4721 | Fue | Lun | Lun | Jur | Est | Tra | Jur | Sol | For | Fue | Lun | For |
| 4722 | Est | Tra | Tra | Fue | Sol | For | Fue | Lun | Jur | Est | Tra | Jur |

Con esta tabla y la posición del día dentro del mes puede obtenerse rápidamente cualquier día semanal.

---

# 8. Eventos astronómicos entre 4712 y 4722

## 8.1. Equinoccios y solsticios

Las fechas y horas se trasladan directamente desde los años terrestres 2012–2022. El día de la semana mostrado es el correspondiente al Cómputo de Absalom.

| Año AR | Equinoccio de primavera | Solsticio de verano | Equinoccio de otoño | Solsticio de invierno |
|---:|---|---|---|---|
| 4712 | 20 Farasto · Día de la Fortuna · 05:14 UTC | 20 Sarenith · Día del Juramento · 23:09 UTC | 22 Rova · Día del Sol · 14:49 UTC | 21 Kuthona · Día de las Estrellas · 11:11 UTC |
| 4713 | 20 Farasto · Día del Juramento · 11:02 UTC | 21 Sarenith · Día de las Estrellas · 05:04 UTC | 22 Rova · Día de la Luna · 20:44 UTC | 21 Kuthona · Día del Sol · 17:11 UTC |
| 4714 | 20 Farasto · Día del Fuego · 16:57 UTC | 21 Sarenith · Día del Sol · 10:51 UTC | 23 Rova · Día de la Fortuna · 02:29 UTC | 21 Kuthona · Día de la Luna · 23:02 UTC |
| 4715 | 20 Farasto · Día de las Estrellas · 22:45 UTC | 21 Sarenith · Día de la Luna · 16:38 UTC | 23 Rova · Día del Juramento · 08:20 UTC | 22 Kuthona · Día de la Fortuna · 04:48 UTC |
| 4716 | 20 Farasto · Día del Sol · 04:30 UTC | 20 Sarenith · Día de la Luna · 22:34 UTC | 22 Rova · Día del Juramento · 14:21 UTC | 21 Kuthona · Día de la Fortuna · 10:44 UTC |
| 4717 | 20 Farasto · Día de la Luna · 10:28 UTC | 21 Sarenith · Día de la Fortuna · 04:24 UTC | 22 Rova · Día del Fuego · 22:02 UTC | 21 Kuthona · Día del Juramento · 16:28 UTC |
| 4718 | 20 Farasto · Día del Trabajo · 16:15 UTC | 21 Sarenith · Día del Juramento · 10:07 UTC | 23 Rova · Día del Sol · 01:54 UTC | 21 Kuthona · Día del Fuego · 22:22 UTC |
| 4719 | 20 Farasto · Día de la Fortuna · 21:58 UTC | 21 Sarenith · Día del Fuego · 15:54 UTC | 23 Rova · Día de la Luna · 07:50 UTC | 22 Kuthona · Día del Sol · 04:14 UTC |
| 4720 | 20 Farasto · Día del Fuego · 03:49 UTC | 20 Sarenith · Día de las Estrellas · 21:43 UTC | 22 Rova · Día del Trabajo · 13:30 UTC | 21 Kuthona · Día de la Luna · 10:02 UTC |
| 4721 | 20 Farasto · Día de las Estrellas · 09:37 UTC | 21 Sarenith · Día de la Luna · 03:32 UTC | 22 Rova · Día de la Fortuna · 19:21 UTC | 21 Kuthona · Día del Trabajo · 15:59 UTC |
| 4722 | 20 Farasto · Día del Sol · 15:33 UTC | 21 Sarenith · Día del Trabajo · 09:13 UTC | 23 Rova · Día del Fuego · 01:03 UTC | 21 Kuthona · Día de la Fortuna · 21:48 UTC |

### Celebraciones ligadas a estos fenómenos

- **Equinoccio de primavera:** *Firstbloom*, *Planting Week* y, según la región, *Days of Wrath*.
- **Solsticio de verano:** *Ritual of Stardust*, *Sunwrought Festival* y otros ritos solares o lunares.
- **Equinoccio de otoño:** *Harvest Feast*, *Swallowtail Festival* y fiestas de cosecha.
- **Solsticio de invierno:** *Crystalhue*, *Ritual of Stardust* y rituales de luz, memoria o resistencia.

> **No deben confundirse** las estaciones astronómicas con fiestas culturales fijas como *First Day of Summer* el 1 de Sarenith o *Last Day of Summer* el 30 de Arodio. Esas celebraciones no cambian de fecha aunque el solsticio caiga otro día.

## 8.2. Festividades móviles determinadas por la semana

### De Calistril a Erasto

| Año | Último Día del Juramento de Calistril — Batul al-Alim | Primer Día del Sol de Farasto — Golemwalk Parade | Último Día del Sol de Desnio — Goblin Flea Market y Breaching Festival | Último Día del Sol de Sarenith — Goblin Flea Market | Último Día del Sol de Erasto — Goblin Flea Market |
|---:|---|---|---|---|---|
| 4712 | 29 Calistril | 3 Farasto | 26 Desnio | 30 Sarenith | 28 Erasto |
| 4713 | 27 Calistril | 2 Farasto | 25 Desnio | 29 Sarenith | 27 Erasto |
| 4714 | 26 Calistril | 1 Farasto | 31 Desnio | 28 Sarenith | 26 Erasto |
| 4715 | 25 Calistril | 7 Farasto | 30 Desnio | 27 Sarenith | 25 Erasto |
| 4716 | 24 Calistril | 6 Farasto | 29 Desnio | 26 Sarenith | 31 Erasto |
| 4717 | 23 Calistril | 5 Farasto | 28 Desnio | 25 Sarenith | 30 Erasto |
| 4718 | 22 Calistril | 4 Farasto | 27 Desnio | 24 Sarenith | 29 Erasto |
| 4719 | 28 Calistril | 3 Farasto | 26 Desnio | 30 Sarenith | 28 Erasto |
| 4720 | 27 Calistril | 1 Farasto | 31 Desnio | 28 Sarenith | 26 Erasto |
| 4721 | 25 Calistril | 7 Farasto | 30 Desnio | 27 Sarenith | 25 Erasto |
| 4722 | 24 Calistril | 6 Farasto | 29 Desnio | 26 Sarenith | 31 Erasto |

### De Arodio a Kuthona

| Año | Último Día del Sol de Arodio — Silverglazer I | Primer Día de la Fortuna de Rova — Crabfest | Primer Día del Sol de Rova — Silverglazer II | Segundo Día del Juramento de Rova — Signing Day | Segundo Día de la Luna de Lamashan — Harvest Feast | Winter Week |
|---:|---|---|---|---|---|---|
| 4712 | 25 Arodio | 4 Rova | 1 Rova | 12 Rova | 14 Lamashan | 8–14 Kuthona |
| 4713 | 31 Arodio | 3 Rova | 7 Rova | 11 Rova | 13 Lamashan | 14–20 Kuthona |
| 4714 | 30 Arodio | 2 Rova | 6 Rova | 10 Rova | 12 Lamashan | 13–19 Kuthona |
| 4715 | 29 Arodio | 1 Rova | 5 Rova | 9 Rova | 11 Lamashan | 12–18 Kuthona |
| 4716 | 28 Arodio | 7 Rova | 4 Rova | 8 Rova | 10 Lamashan | 11–17 Kuthona |
| 4717 | 27 Arodio | 6 Rova | 3 Rova | 14 Rova | 9 Lamashan | 10–16 Kuthona |
| 4718 | 26 Arodio | 5 Rova | 2 Rova | 13 Rova | 8 Lamashan | 9–15 Kuthona |
| 4719 | 25 Arodio | 4 Rova | 1 Rova | 12 Rova | 14 Lamashan | 8–14 Kuthona |
| 4720 | 30 Arodio | 2 Rova | 6 Rova | 10 Rova | 12 Lamashan | 13–19 Kuthona |
| 4721 | 29 Arodio | 1 Rova | 5 Rova | 9 Rova | 11 Lamashan | 12–18 Kuthona |
| 4722 | 28 Arodio | 7 Rova | 4 Rova | 8 Rova | 10 Lamashan | 11–17 Kuthona |

## 8.3. Festividades lunares principales

Las horas lunares están calculadas con fórmulas astronómicas estándar y redondeadas al minuto. Son una referencia suficientemente precisa para campaña, no una efeméride científica de navegación.

### Inicio del año y final del invierno

| Año | Luna llena de Abadio — Longnight y Mooncall | Primera luna nueva — Eternal Kiss | Luna llena de Calistril — Lust Festival | Última luna nueva antes del equinoccio — Darkest Night |
|---:|---|---|---|---|
| 4712 | 9 Abadio · Día del Trabajo · 07:31 UTC | 23 Abadio · Día del Trabajo · 07:40 UTC | 7 Calistril · Día de la Fortuna · 21:55 UTC | 21 Calistril · Día de la Fortuna · 22:35 UTC |
| 4713 | 27 Abadio · Día de la Luna · 04:39 UTC | 11 Abadio · Día de las Estrellas · 19:44 UTC | 25 Calistril · Día del Trabajo · 20:27 UTC | 11 Farasto · Día del Trabajo · 19:52 UTC |
| 4714 | 16 Abadio · Día del Fuego · 04:53 UTC | 1 Abadio · Día del Juramento · 11:15 UTC | 14 Calistril · Día de las Estrellas · 23:54 UTC | 1 Farasto · Día del Sol · 08:00 UTC |
| 4715 | 5 Abadio · Día del Trabajo · 04:54 UTC | 20 Abadio · Día de la Fortuna · 13:14 UTC | 3 Calistril · Día de la Fortuna · 23:10 UTC | 20 Farasto · Día de las Estrellas · 09:37 UTC |
| 4716 | 24 Abadio · Día de la Luna · 01:46 UTC | 10 Abadio · Día de la Luna · 01:31 UTC | 22 Calistril · Día del Trabajo · 18:21 UTC | 9 Farasto · Día de la Fortuna · 01:55 UTC |
| 4717 | 12 Abadio · Día del Juramento · 11:35 UTC | 28 Abadio · Día de las Estrellas · 00:08 UTC | 11 Calistril · Día de las Estrellas · 00:34 UTC | 26 Calistril · Día del Sol · 14:59 UTC |
| 4718 | 2 Abadio · Día del Trabajo · 02:25 UTC | 17 Abadio · Día de la Fortuna · 02:18 UTC | — | 17 Farasto · Día de las Estrellas · 13:12 UTC |
| 4719 | 21 Abadio · Día de la Luna · 05:17 UTC | 6 Abadio · Día del Sol · 01:29 UTC | 19 Calistril · Día del Trabajo · 15:54 UTC | 6 Farasto · Día de la Fortuna · 16:05 UTC |
| 4720 | 10 Abadio · Día del Fuego · 19:22 UTC | 24 Abadio · Día del Fuego · 21:43 UTC | 9 Calistril · Día del Sol · 07:34 UTC | 23 Calistril · Día del Sol · 15:33 UTC |
| 4721 | 28 Abadio · Día del Juramento · 19:17 UTC | 13 Abadio · Día de la Fortuna · 05:01 UTC | 27 Calistril · Día de las Estrellas · 08:18 UTC | 13 Farasto · Día de las Estrellas · 10:22 UTC |
| 4722 | 17 Abadio · Día de la Luna · 23:49 UTC | 2 Abadio · Día del Sol · 18:34 UTC | 16 Calistril · Día de la Fortuna · 16:57 UTC | 2 Farasto · Día de la Fortuna · 17:35 UTC |

> En **4718 AR**, Calistril no contiene ninguna luna llena. Ese año no hay fecha natural para *Lust Festival* dentro de ese mes; cada culto local decidirá si omite la fiesta, la adelanta a la segunda luna llena de Abadio o la retrasa a la luna llena de Farasto.

### Resto del año

| Año | Primera luna llena de Desnio — Remembrance Moon | Primera luna llena de Lamashan — Admani Upastuti | Última luna nueva del año — Blightmother's Eve |
|---:|---|---|---|
| 4712 | 6 Desnio · Día de la Luna · 03:36 UTC | 29 Lamashan · Día del Trabajo · 19:50 UTC | 13 Kuthona · Día del Fuego · 08:42 UTC |
| 4713 | 25 Desnio · Día del Sol · 04:26 UTC | 18 Lamashan · Día de las Estrellas · 23:38 UTC | 3 Kuthona · Día de la Fortuna · 00:23 UTC |
| 4714 | 14 Desnio · Día del Juramento · 19:17 UTC | 8 Lamashan · Día del Juramento · 10:51 UTC | 22 Kuthona · Día del Trabajo · 01:36 UTC |
| 4715 | 4 Desnio · Día del Trabajo · 03:43 UTC | 27 Lamashan · Día de la Fortuna · 12:06 UTC | 11 Kuthona · Día de las Estrellas · 10:30 UTC |
| 4716 | 21 Desnio · Día de las Estrellas · 21:15 UTC | 16 Lamashan · Día del Sol · 04:24 UTC | 29 Kuthona · Día del Juramento · 06:54 UTC |
| 4717 | 10 Desnio · Día de la Fortuna · 21:43 UTC | 5 Lamashan · Día del Juramento · 18:41 UTC | 18 Kuthona · Día de la Luna · 06:31 UTC |
| 4718 | 29 Desnio · Día del Trabajo · 14:20 UTC | 24 Lamashan · Día de la Fortuna · 16:46 UTC | 7 Kuthona · Día del Fuego · 07:21 UTC |
| 4719 | 18 Desnio · Día de las Estrellas · 21:12 UTC | 13 Lamashan · Día del Sol · 21:09 UTC | 26 Kuthona · Día del Juramento · 05:14 UTC |
| 4720 | 7 Desnio · Día del Juramento · 10:46 UTC | 1 Lamashan · Día del Juramento · 21:06 UTC | 14 Kuthona · Día de la Luna · 16:17 UTC |
| 4721 | 26 Desnio · Día de la Fortuna · 11:15 UTC | 20 Lamashan · Día de la Fortuna · 14:57 UTC | 4 Kuthona · Día de las Estrellas · 07:44 UTC |
| 4722 | 16 Desnio · Día de la Luna · 04:15 UTC | 9 Lamashan · Día del Sol · 20:56 UTC | 23 Kuthona · Día del Fuego · 10:17 UTC |

### Mooncall

**Mooncall** no es una única fiesta anual: para los cultos de Camazotz, **cada luna llena** puede constituir una noche sagrada. La luna llena de Abadio aparece destacada porque coincide con *Longnight*.

## 8.4. Todas las lunas nuevas y llenas de 4712–4722

Horas en UTC de referencia. Dentro de cada celda, las fases aparecen en orden cronológico.

| Año AR | Lunas nuevas | Lunas llenas |
|---:|---|---|
| 4712 | 23 Abadio 07:40<br>21 Calistril 22:35<br>22 Farasto 14:38<br>21 Gozran 07:19<br>20 Desnio 23:48<br>19 Sarenith 15:03<br>19 Erasto 04:25<br>17 Arodio 15:55<br>16 Rova 02:11<br>15 Lamashan 12:03<br>13 Neth 22:09<br>13 Kuthona 08:42 | 9 Abadio 07:31<br>7 Calistril 21:55<br>8 Farasto 09:40<br>6 Gozran 19:19<br>6 Desnio 03:36<br>4 Sarenith 11:12<br>3 Erasto 18:52<br>2 Arodio 03:28<br>31 Arodio 13:59<br>30 Rova 03:19<br>29 Lamashan 19:50<br>28 Neth 14:47<br>28 Kuthona 10:22 |
| 4713 | 11 Abadio 19:44<br>10 Calistril 07:21<br>11 Farasto 19:52<br>10 Gozran 09:36<br>10 Desnio 00:29<br>8 Sarenith 15:57<br>8 Erasto 07:15<br>6 Arodio 21:51<br>5 Rova 11:37<br>5 Lamashan 00:35<br>3 Neth 12:51<br>3 Kuthona 00:23 | 27 Abadio 04:39<br>25 Calistril 20:27<br>27 Farasto 09:28<br>25 Gozran 19:58<br>25 Desnio 04:26<br>23 Sarenith 11:33<br>22 Erasto 18:16<br>21 Arodio 01:45<br>19 Rova 11:13<br>18 Lamashan 23:38<br>17 Neth 15:16<br>17 Kuthona 09:29 |
| 4714 | 1 Abadio 11:15<br>30 Abadio 21:39<br>1 Farasto 08:00<br>30 Farasto 18:45<br>29 Gozran 06:15<br>28 Desnio 18:41<br>27 Sarenith 08:09<br>26 Erasto 22:42<br>25 Arodio 14:13<br>24 Rova 06:14<br>23 Lamashan 21:57<br>22 Neth 12:33<br>22 Kuthona 01:36 | 16 Abadio 04:53<br>14 Calistril 23:54<br>16 Farasto 17:09<br>15 Gozran 07:43<br>14 Desnio 19:17<br>13 Sarenith 04:12<br>12 Erasto 11:25<br>10 Arodio 18:10<br>9 Rova 01:39<br>8 Lamashan 10:51<br>6 Neth 22:23<br>6 Kuthona 12:27 |
| 4715 | 20 Abadio 13:14<br>18 Calistril 23:48<br>20 Farasto 09:37<br>18 Gozran 18:57<br>18 Desnio 04:14<br>16 Sarenith 14:06<br>16 Erasto 01:25<br>14 Arodio 14:54<br>13 Rova 06:42<br>13 Lamashan 00:07<br>11 Neth 17:48<br>11 Kuthona 10:30 | 5 Abadio 04:54<br>3 Calistril 23:10<br>5 Farasto 18:06<br>4 Gozran 12:06<br>4 Desnio 03:43<br>2 Sarenith 16:20<br>2 Erasto 02:20<br>31 Erasto 10:44<br>29 Arodio 18:36<br>28 Rova 02:51<br>27 Lamashan 12:06<br>25 Neth 22:45<br>25 Kuthona 11:12 |
| 4716 | 10 Abadio 01:31<br>8 Calistril 14:40<br>9 Farasto 01:55<br>7 Gozran 11:24<br>6 Desnio 19:30<br>5 Sarenith 03:00<br>4 Erasto 11:02<br>2 Arodio 20:45<br>1 Rova 09:04<br>1 Lamashan 00:12<br>30 Lamashan 17:39<br>29 Neth 12:19<br>29 Kuthona 06:54 | 24 Abadio 01:46<br>22 Calistril 18:21<br>23 Farasto 12:02<br>22 Gozran 05:24<br>21 Desnio 21:15<br>20 Sarenith 11:03<br>19 Erasto 22:57<br>18 Arodio 09:27<br>16 Rova 19:06<br>16 Lamashan 04:24<br>14 Neth 13:53<br>14 Kuthona 00:06 |
| 4717 | 28 Abadio 00:08<br>26 Calistril 14:59<br>28 Farasto 02:58<br>26 Gozran 12:17<br>25 Desnio 19:45<br>24 Sarenith 02:31<br>23 Erasto 09:46<br>21 Arodio 18:31<br>20 Rova 05:31<br>19 Lamashan 19:13<br>18 Neth 11:43<br>18 Kuthona 06:31 | 12 Abadio 11:35<br>11 Calistril 00:34<br>12 Farasto 14:54<br>11 Gozran 06:09<br>10 Desnio 21:43<br>9 Sarenith 13:10<br>9 Erasto 04:07<br>7 Arodio 18:11<br>6 Rova 07:04<br>5 Lamashan 18:41<br>4 Neth 05:24<br>3 Kuthona 15:48 |
| 4718 | 17 Abadio 02:18<br>15 Calistril 21:06<br>17 Farasto 13:12<br>16 Gozran 01:58<br>15 Desnio 11:48<br>13 Sarenith 19:44<br>13 Erasto 02:49<br>11 Arodio 09:58<br>9 Rova 18:02<br>9 Lamashan 03:48<br>7 Neth 16:03<br>7 Kuthona 07:21 | 2 Abadio 02:25<br>31 Abadio 13:27<br>2 Farasto 00:52<br>31 Farasto 12:38<br>30 Gozran 00:59<br>29 Desnio 14:20<br>28 Sarenith 04:54<br>27 Erasto 20:21<br>26 Arodio 11:57<br>25 Rova 02:53<br>24 Lamashan 16:46<br>23 Neth 05:40<br>22 Kuthona 17:49 |
| 4719 | 6 Abadio 01:29<br>4 Calistril 21:04<br>6 Farasto 16:05<br>5 Gozran 08:51<br>4 Desnio 22:46<br>3 Sarenith 10:03<br>2 Erasto 19:17<br>1 Arodio 03:12<br>30 Arodio 10:38<br>28 Rova 18:27<br>28 Lamashan 03:39<br>26 Neth 15:06<br>26 Kuthona 05:14 | 21 Abadio 05:17<br>19 Calistril 15:54<br>21 Farasto 01:43<br>19 Gozran 11:13<br>18 Desnio 21:12<br>17 Sarenith 08:31<br>16 Erasto 21:39<br>15 Arodio 12:30<br>14 Rova 04:34<br>13 Lamashan 21:09<br>12 Neth 13:35<br>12 Kuthona 05:13 |
| 4720 | 24 Abadio 21:43<br>23 Calistril 15:33<br>24 Farasto 09:29<br>23 Gozran 02:26<br>22 Desnio 17:39<br>21 Sarenith 06:42<br>20 Erasto 17:33<br>19 Arodio 02:42<br>17 Rova 11:01<br>16 Lamashan 19:32<br>15 Neth 05:08<br>14 Kuthona 16:17 | 10 Abadio 19:22<br>9 Calistril 07:34<br>9 Farasto 17:48<br>8 Gozran 02:36<br>7 Desnio 10:46<br>5 Sarenith 19:13<br>5 Erasto 04:45<br>3 Arodio 15:59<br>2 Rova 05:23<br>1 Lamashan 21:06<br>31 Lamashan 14:50<br>30 Neth 09:30<br>30 Kuthona 03:29 |
| 4721 | 13 Abadio 05:01<br>11 Calistril 19:06<br>13 Farasto 10:22<br>12 Gozran 02:31<br>11 Desnio 19:00<br>10 Sarenith 10:53<br>10 Erasto 01:17<br>8 Arodio 13:51<br>7 Rova 00:52<br>6 Lamashan 11:06<br>4 Neth 21:15<br>4 Kuthona 07:44 | 28 Abadio 19:17<br>27 Calistril 08:18<br>28 Farasto 18:49<br>27 Gozran 03:32<br>26 Desnio 11:15<br>24 Sarenith 18:40<br>24 Erasto 02:38<br>22 Arodio 12:03<br>20 Rova 23:55<br>20 Lamashan 14:57<br>19 Neth 08:58<br>19 Kuthona 04:36 |
| 4722 | 2 Abadio 18:34<br>1 Calistril 05:47<br>2 Farasto 17:35<br>1 Gozran 06:25<br>30 Gozran 20:29<br>30 Desnio 11:31<br>29 Sarenith 02:53<br>28 Erasto 17:56<br>27 Arodio 08:18<br>25 Rova 21:55<br>25 Lamashan 10:49<br>23 Neth 22:58<br>23 Kuthona 10:17 | 17 Abadio 23:49<br>16 Calistril 16:57<br>18 Farasto 07:18<br>16 Gozran 18:56<br>16 Desnio 04:15<br>14 Sarenith 11:52<br>13 Erasto 18:38<br>12 Arodio 01:36<br>10 Rova 10:00<br>9 Lamashan 20:56<br>8 Neth 11:03<br>8 Kuthona 04:09 |

---

# 9. Tipos de festividad y reglas de interpretación

## 9.1. Fecha fija

Siempre cae el mismo día del mismo mes.

Ejemplo:

> *New Year*: 1 de Abadio.

## 9.2. Fecha semanal móvil

Depende de una posición dentro del mes.

Ejemplo:

> *Golemwalk Parade*: primer Día del Sol de Farasto.

Para calcularla se usa la tabla del inicio de los meses o la fórmula general del apartado 4.

## 9.3. Fecha lunar

Depende de una luna nueva o llena.

Ejemplo:

> *Darkest Night*: última luna nueva anterior al equinoccio de primavera.

Debe consultarse la tabla lunar del año correspondiente.

## 9.4. Fecha astronómica

Depende de un equinoccio o solsticio.

Ejemplo:

> *Crystalhue*: solsticio de invierno.

Debe consultarse la tabla de estaciones del año correspondiente.

## 9.5. Periodo festivo

Algunas celebraciones ocupan varios días o semanas.

Ejemplos:

- *Vernal Carpentry Court*: 1–15 de Farasto.
- *Wrights of Augustana*: 16–30 de Gozran.
- *Baptism of Ice*: 24–30 de Neth.
- *Winter Week*: segunda semana de Kuthona.

---

# 10. Almanaque anual

## Abadio — 31 días

Primer mes del año. En gran parte del hemisferio norte corresponde al corazón del invierno.

### 1 de Abadio

- **New Year** *(todo Golarion)* — Inicio del año civil. Es una jornada de balances, promesas, fiestas familiares y renovación de contratos.
- **Foundation Day** *(Absalom; Milani)* — Conmemora la fundación de Absalom. Combina orgullo cívico, ceremonias públicas y, entre los fieles de Milani, una lectura centrada en la libertad y la resistencia frente a la tiranía.
- **Pjallarane Day** *(Irrisen)* — Recuerda la breve rebelión de la reina Pjallarane contra Baba Yaga. Se preparan banquetes y se queman efigies de paja y alquitrán.
- 🎂 **Brindelvasthimir Quillopon Dranzibandor**

### 6 de Abadio

- **Vault Day** *(Abadar)* — Día dedicado a la banca, la contabilidad, los contratos y la prosperidad ordenada. Templos y casas comerciales revisan cuentas y formalizan acuerdos.

### 7 de Abadio

- **Founder's Day** *(Linvarre)* — Fiesta cívica local que celebra el origen de la comunidad, a sus fundadores y la continuidad de sus instituciones.

### Luna llena de Abadio — fecha móvil

- **Longnight** *(pueblos norteños; fecha lunar)* — Festival nocturno celebrado durante la luna llena de Abadio. Las comunidades mantienen hogueras, comen, cantan y permanecen despiertas hasta recibir juntas el amanecer.
- **Mooncall** *(Camazotz; fecha lunar)* — Rito de luna llena asociado a cacerías nocturnas, derramamiento de sangre y ofrendas al dios murciélago. Suele practicarse en secreto.

### 11 de Abadio

- **Ascension Day** *(Arazni)* — Jornada solemne dedicada a Arazni. Sus fieles recuerdan su transformación, su supervivencia y su negativa a seguir siendo definida por quienes la sometieron.

### 20 de Abadio

- **Ruby Prince's Birthday** *(Osirion)* — Celebración estatal del cumpleaños del Príncipe Rubí, con ceremonias oficiales, propaganda dinástica y actos públicos.

### 21 de Abadio

- 🎂 **Agricultor random 1**

### Primera luna nueva del año — fecha móvil

- **Eternal Kiss** *(Zon-Kuthon; fecha lunar)* — Se celebra en la primera luna nueva del año y culmina un prolongado rito kuthita de dolor. El sufrimiento de una víctima puede utilizarse como sacrificio y como medio de adivinación.

### 30 de Abadio

- 🎂 **Cholna de las Lluvias**

---

## Calistril — 28 días; 29 en año bisiesto

Último tramo del invierno en buena parte de Avistan.

### Durante todo Calistril

- **Benga** *(Minata)* — Ciclo de celebraciones comunitarias, visitas, comidas compartidas y renovación de pactos sociales.

### 1 de Calistril

- **Ritual of the Whip Sting** *(Calistria)* — Ceremonia en la que sacerdotes de Calistria median entre enemigos. La venganza se expresa públicamente y, una vez satisfecha, se espera que la disputa termine.

### 2 de Calistril

- **Merrymead** *(Cayden Cailean; Druma)* — Fiesta de bebida, camaradería y hospitalidad. En Druma se mezcla con banquetes, comercio y ostentación de riqueza.
- 🎂 **Frinn**
- 🎂 **Conduct random 11**

### 14 de Calistril

- **Feast of Vigor** *(Calistria)* — Celebración de la pasión, el deseo, el placer y la vitalidad. Es popular entre amantes, artistas y hedonistas.

### Luna llena de Calistril — fecha móvil

- **Lust Festival** *(Calistria)* — Celebración de deseo, placer, belleza y vínculos libremente elegidos. No todos los años existe una luna llena dentro de Calistril: en 4718 AR no la hay.

### 16 de Calistril

- **King Eodred II's Birthday** *(Korvosa)* — Fiesta monárquica de tono nostálgico, tradicionalmente acompañada de vino, música y espectáculos públicos.
- 🎂 **Uksahkka**

### 19 de Calistril

- **Treaty of Egorian / Loyalty Day** *(Cheliax; Asmodeus)* — Conmemora la firma del Tratado de Egorian el 19 de Calistril de 4640 AR, inicio de la Ascendencia Thrune. Se renuevan juramentos al Estado, a la Casa Thrune y al orden infernal.

### Último Día del Juramento de Calistril — fecha móvil

- **Batul al-Alim** *(Qadira)* — Conmemora el nacimiento de un popular poeta romántico qadiran. Es una jornada de poesía, cortejo, recitación pública y celebración cultural.

### Última luna nueva anterior al equinoccio de primavera — fecha móvil

- **Darkest Night** *(Tsukiyo; fecha lunar)* — Se celebra durante la última luna nueva anterior al equinoccio de primavera. Conmemora las tres noches que Tsukiyo pasó muerto y propone transformar la envidia en ayuda mutua.

### 26 de Calistril

- 🎂 **Ralvio Dorran**

### Último día de Calistril — 28 o 29

- **Tempest Day** *(Bloodcove)* — Marca el final de la estación seca y la calma anterior a las tormentas. En el puerto se brinda, se lanzan desafíos y se exageran historias marineras.

---

## Farasto — 31 días

Mes de transición hacia la primavera.

### 1–15 de Farasto

- **Vernal Carpentry Court** *(Andoran)* — Periodo de trabajos comunales, reparación de edificios y proyectos públicos. Refuerza el ideal andorano de cooperación cívica.

### Primer Día del Sol de Farasto — fecha móvil

- **Golemwalk Parade** *(Magnimar; fecha semanal en algunas fuentes)* — Desfile de gólems construidos por aficionados. Los diseños se exhiben y compiten por reconocimiento, contratos o apoyo de los artesanos de la ciudad.

### 5 de Farasto

- **Day of Bones** *(Pharasma)* — Jornada de recuerdo de los muertos. Se visitan tumbas, se limpian osarios y se honra a quienes han cruzado definitivamente al más allá.
- 🎂 **Freydis Manodura**

### 6 de Farasto

- **Sable Company Founding Day** *(Korvosa)* — Conmemora la fundación de la Compañía Sable con desfiles, ejercicios militares y demostraciones marciales.

### 7 de Farasto

- **Night of Tears** *(Solku)* — Vigilia solemne por los muertos de la Batalla de la Lluvia Roja. Predominan el silencio, el luto y la memoria colectiva.
- 🎂 **Leif Hagorsson**

### 12 de Farasto

- 🎂 **Conduct random 1**

### 13 de Farasto

- **Kaliashahrim** *(Qadira)* — Festividad cortesana y mercantil. Se celebran audiencias, se intercambian obsequios y se refuerzan vínculos comerciales y diplomáticos.

### 14 de Farasto

- 🎂 **Bevelek**

### Equinoccio de primavera — fecha astronómica móvil

- **Equinoccio vernal** *(fecha astronómica)* — El día y la noche alcanzan una duración semejante. Marca el comienzo simbólico de la primavera.
- **Days of Wrath** *(Asmodeus)* — Celebraciones de poder, dominio y orden, frecuentemente acompañadas de castigos públicos, competiciones o espectáculos violentos.
- **Firstbloom** *(Gozreh)* — Celebra el despertar de la naturaleza y las primeras señales visibles de la primavera.
- **Planting Week** *(Erastil)* — Inicio de la temporada agrícola. Familias y comunidades preparan la tierra y comparten el trabajo de la siembra.

### 25 de Farasto

- **Festival of Flowers** *(Kazutal)* — Fiesta de fertilidad y renovación, con danzas, desfiles, vestidos coloridos y abundantes decoraciones florales.

### 26 de Farasto

- **Conquest Day** *(Nex)* — Fiesta nacional en la que los ciudadanos renuevan su compromiso con Nex y su eterna rivalidad con Geb. Predominan los juramentos patrióticos, los discursos marciales y las exhibiciones de audacia.

### 27 de Farasto

- 🎂 **Conduct random 12**

---

## Gozran — 30 días

Primavera asentada, con lluvias, deshielos y reanudación de las rutas comerciales.

### 4 de Gozran

- 🎂 **Daichi Tomoko**

### 7 de Gozran

- **Currentseve** *(Gozreh)* — Jornada de respeto a mares, ríos y corrientes. Pescadores y marineros realizan ofrendas y piden rutas seguras.

### 15 de Gozran

- **Taxfest** *(Abadar)* — Día de impuestos, censos y contabilidad. Las obligaciones fiscales se presentan como una contribución necesaria al orden y a la prosperidad común.

### 16–30 de Gozran

- **Wrights of Augustana** *(Andoran; Brigh)* — Quincena dedicada a inventores, ingenieros y artesanos. Incluye exposiciones, concursos y presentación de nuevas máquinas.

### 18 de Gozran

- 🎂 **Caurrin Whesterwill**

### 19 de Gozran

- 🎂 **Kelda**

### 22 de Gozran

- 🎂 **Asmund, el quesero**

### 23 de Gozran

- 🎂 **Sandru Vhiski**

### 24 de Gozran

- **Find of the Cayhound** *(Cayden Cailean)* — Recuerda el día en que Cayden encontró a su mastín Thunder. Muchos fieles adoptan perros abandonados o ayudan a animales sin hogar.

### 27 de Gozran

- **Gala of Sails** *(Absalom)* — Fiesta primaveral de cometas y combates de cometas. El cielo de la ciudad se llena de velas de papel, colores y diseños competitivos.

### 30 de Gozran

- 🎂 **Lewis**

---

## Desnio — 31 días

Final de la primavera y preparación del verano.

### 2–3 de Desnio

- **Ascendance Night** *(Norgorber)* — Conmemora la ascensión del dios de los secretos. Sus ritos giran en torno a máscaras, identidades ocultas y conocimiento reservado.

### 3 de Desnio

- **Azvadeva Dejal** *(Gruhastha)* — Celebra la revelación de Azvadeva Pujila. Se regalan libros, se bendicen animales y se sirven banquetes vegetarianos.
- 🎂 **Vankor**
- 🎂 **Renji**

### Primera luna llena de Desnio — fecha móvil

- **Remembrance Moon** *(Iomedae; fecha lunar)* — En la primera luna llena de Desnio se honra a quienes murieron luchando contra el Tirano Susurrante durante la Cruzada Radiante.

### 9 de Desnio

- 🎂 **Eilvarr**

### 10 de Desnio

- 🎂 **Conduct random 3**

### 13 de Desnio

- **Old-Mage Day** *(Mwangi)* — Jornada de respeto al saber ancestral, a la enseñanza mágica y al legado del Viejo Mago Jatembe.

### 16 de Desnio

- **Suha-Suha** *(Minata)* — Fiesta dedicada al equilibrio comunitario, la reciprocidad y la reparación de tensiones dentro de la sociedad.

### 24 de Desnio

- 🎂 **Yrja Vientonegro**

### Último Día del Sol de Desnio — fecha móvil

- **Goblin Flea Market** *(Andoran; fecha semanal)* — Mercadillo caótico de objetos usados, curiosidades, piezas incompletas y mercancías de procedencia dudosa.
- **Breaching Festival** *(Korvosa; fecha semanal)* — Competición de la Acadamae en la que los participantes intentan superar defensas mágicas. Es prestigiosa, espectacular y potencialmente letal.

### 31 de Desnio

- **Angel Day** *(Magnimar)* — Conmemora la fundación de Magnimar y la huida de sus primeros pobladores de Korvosa. Son habituales las mascaradas angélicas y las efigies de diablos.
- **Apprentice Appreciation Day** *(Nethys)* — Reconoce el trabajo anual de los aprendices. Puede incluir intercambios de papeles, demostraciones o desafíos amistosos a sus maestros.

---

## Sarenith — 30 días

Comienzo del verano en el hemisferio norte.

### Semana previa al comienzo del verano

- **Blossom Days** *(Xopatl)* — Semana de renovación floral y memoria del renacimiento de la Ciudad de las Flores. Se decoran calles y hogares con pétalos y guirnaldas.

### 1 de Sarenith

- **First Day of Summer** *(Sarenrae)* — Inicio tradicional del verano. Celebra la luz, el calor, la curación y la posibilidad de redención.

### 3 de Sarenith

- **Day of Destiny** *(Korvosa)* — Recuerda la firma de la carta fundacional de Korvosa. Es una fiesta cívica muy asociada a la bebida popular.
- **Independence Day / Liberty Day** *(Andoran)* — Celebra la libertad política, la independencia y la oposición a la tiranía. Ambas denominaciones se utilizan para la misma tradición andorana en distintas fuentes.

### 5 de Sarenith

- 🎂 **Shalelu Andosana**

### 8 de Sarenith

- 🎂 **Ketaek Pasos Cortos**

### 10 de Sarenith

- **Burning Blades** *(Sarenrae)* — Ceremonias de luz, justicia y destreza marcial. Las armas y el fuego se presentan como instrumentos que deben usarse con disciplina y propósito.
- **Darkness Eternal** *(Asmodeus)* — Contrafestividad en la que los fieles de Asmodeus maldicen la abundancia de luz y rezan por noches más largas y un invierno temprano.

### 17 de Sarenith

- 🎂 **Inga Hagorsson**

### 18 de Sarenith

- 🎂 **Ramaso Panor**

### 21 de Sarenith

- **Talon Tag** *(Andoran)* — Competiciones juveniles de velocidad, agilidad y trabajo en equipo, inspiradas en los ideales heroicos andoranos.

### 22 de Sarenith

- **Riverwind Festival** *(Korvosa)* — Celebra la llegada de los vientos frescos que recorren el río. Se acompaña de bebida, música y reuniones al aire libre.

### Solsticio de verano — fecha astronómica móvil

- **Solsticio de verano** *(fecha astronómica)* — Día de máxima luz. Reúne numerosos ritos solares, lunares, agrícolas y adivinatorios.
- **Ritual of Stardust** *(Desna)* — Al anochecer se encienden grandes hogueras; cuando quedan brasas, los fieles arrojan arena mezclada con gemas estrelladas molidas y proclaman afectos, amistades y viajes futuros.
- **Sunwrought Festival** *(Sarenrae)* — Danzas, regalos, fuegos artificiales, cometas, mercados especiales y representaciones de la lucha de Sarenrae contra Rovagug.

### Último Día del Sol de Sarenith — fecha móvil

- **Goblin Flea Market** *(Andoran; fecha semanal)* — Nueva edición estacional del mercadillo de segunda mano y curiosidades.

### 27 de Sarenith

- 🎂 **Velani Korrast**

### 29 de Sarenith

- 🎂 **Conduct random 4**

---

## Erasto — 31 días

Pleno verano, con viajes más fáciles y actividad agrícola intensa.

### 3 de Erasto

- **Archerfeast / Archer's Day** *(Erastil)* — Día de comida, descanso y vida comunitaria, con competiciones de arco y lanzamiento de piedras, comercio de ganado, peticiones de matrimonio y un sacrificio compartido al anochecer.

### 10 de Erasto

- 🎂 **Hargor Hagorsson**

### 11 de Erasto

- 🎂 **Dominic Torettoson**

### 14 de Erasto

- **Founding Festival** *(Korvosa)* — Gran celebración de la fundación de la ciudad, con alcohol, fuegos artificiales, magia y espectáculos multitudinarios.

### 15–21 de Erasto

- **Kianidi Festival** *(Garund)* — Fiesta de cosecha, comunidad y abundancia, con comidas colectivas, música y danzas.

### 17 de Erasto

- **Burning Night** *(Razmiran)* — Espectáculo religioso y estatal del culto al Dios Viviente, marcado por hogueras, fervor público y demostraciones de obediencia.

### 22 de Erasto

- 🎂 **Ormund Gevren**

### Último Día del Sol de Erasto — fecha móvil

- **Goblin Flea Market** *(Andoran)* — Edición estival del mercadillo de segunda mano, piezas incompletas, curiosidades y objetos de procedencia dudosa.

### 29 de Erasto

- 🎂 **Jorgaan de la Roca Blanca**

---

## Arodio — 31 días

Último gran mes del verano.

### 1 de Arodio

- **Inheritor's Ascendance** *(Iomedae)* — Recuerda el ascenso de Iomedae como heredera espiritual de Aroden y campeona de la humanidad.

### 6 de Arodio

- **First Crusader Day / Crusader Memorial Day** *(Mendev)* — Homenaje a los primeros cruzados y a quienes murieron defendiendo Mendev frente a la Herida del Mundo.

### 7 de Arodio

- 🎂 **Agricultor random 2**

### 9 de Arodio

- **Day of Silenced Whispers** *(Ustalav)* — Jornada de recuerdo silencioso de tragedias, secretos y muertos que no pudieron contar su historia.

### 10 de Arodio

- **Founding Day** *(Ilsurian)* — Celebración del origen de la localidad y de su identidad cívica.

### 15 de Arodio

- 🎂 **Halvard Kaelvik**

### 16 de Arodio

- **Armasse** *(Aroden; Iomedae)* — Festival militar de entrenamiento, torneos, historia marcial y preparación de defensores. Tras la muerte de Aroden, gran parte de la tradición fue asumida por la iglesia de Iomedae.
- 🎂 **Ashka Cantoceniza**

### 18 de Arodio

- 🎂 **Conduct random 6**

### 23 de Arodio

- 🎂 **Siv Hagorsson**

### Último Día del Sol de Arodio — fecha móvil

- **Silverglazer Sunday I** *(Andoran; fecha semanal)* — Primera de dos jornadas andoranas vinculadas a mercados, artesanía de plata y orgullo cívico.

### 30 de Arodio

- **Last Day of Summer** *(Sarenrae)* — Despedida ceremonial del verano y preparación para los meses de cosecha y enfriamiento.

### 31 de Arodio

- **Leap Day** *(Asmodeus)* — Observancia religiosa asmodeana en la que los fieles dedican plegarias adicionales a su dios. **No es el día intercalar del calendario**: el día bisiesto civil se añade al final de Calistril.


---

## Rova — 30 días

Inicio del otoño y periodo de cosecha.

### Primer Día de la Fortuna de Rova — fecha móvil

- **Crabfest** *(Korvosa)* — Celebra el retorno estacional de los cangrejos a las aguas cercanas. La ciudad se llena de puestos de comida y banquetes populares.

### Primer Día del Sol de Rova — fecha móvil

- **Silverglazer Sunday II** *(Andoran; fecha semanal)* — Segunda jornada de la tradición andorana, con comercio, artesanía y celebraciones locales.

### 6 de Rova

- **Start of Classes** *(Acadamae, Arcanamirium, College of Mysteries y Clockwork Cathedral)* — Comienzo tradicional del curso en varias de las grandes instituciones arcanas del Mar Interior.

### 8 de Rova

- 🎂 **Shuo**

### 16–30 de Rova

- **Autumnal Carpentry Court** *(Andoran)* — Periodo de reparaciones, refuerzo de viviendas y preparación colectiva para el invierno.

### Segundo Día del Juramento de Rova — fecha móvil

- **Signing Day** *(naciones del Mar Interior; fecha semanal)* — Día especialmente favorable para tratados, contratos, alianzas y juramentos formales.

### 18 de Rova

- 🎂 **Hiriko**

### 19 de Rova

- **Day of the Inheritor** *(Iomedae)* — Conmemora la continuidad de la fe de Aroden en la iglesia de Iomedae y reafirma sus ideales de valor y justicia.

### 21 de Rova

- **Day of Bargained Ascension** *(Nivi Rhombodazzle)* — Recuerda la ascensión de Nivi mediante el ingenio y la negociación. Son habituales los juegos, apuestas y pactos cuidadosamente formulados.

### Equinoccio de otoño — fecha astronómica móvil

- **Equinoccio otoñal** *(fecha astronómica)* — El día y la noche vuelven a equilibrarse. Se asocia a la cosecha, las mariposas, el tránsito y el retorno progresivo de la oscuridad.
- **Harvest Feast** *(Erastil)* — Semana de cosecha, agradecimiento, recuento de reservas y cooperación rural.
- **Swallowtail Festival / Swallowtail Release** *(Desna)* — Los fieles liberan mariposas cola de golondrina y celebran con comida, canciones e historias. Que una mariposa se pose sobre alguien se considera un buen augurio.

### 26 de Rova

- **Feast of Szurpade** *(Irrisen)* — Parodia cruel de antiguas fiestas de cosecha anteriores al dominio de Baba Yaga. La abundancia oficial contrasta con la dureza de la vida cotidiana.

### 27 de Rova

- 🎂 **Perrin Holbrook**

### 29 de Rova

- **Day of Sundering** *(Ydersius)* — Los cultistas recuerdan la derrota y decapitación de Ydersius, renuevan sus votos y realizan sacrificios con la esperanza de restaurarlo.

---

## Lamashan — 31 días

Otoño avanzado, noches más largas y creciente presencia de ritos funerarios o sombríos.

### 1 de Lamashan

- **Mirror Poet's Farewell** *(Hei Feng)* — Jornada de meditación y duelo por quienes han sido descuidados, ofendidos o perdidos. También invita a valorar a los seres queridos mientras siguen presentes.

### Primera luna llena de Lamashan — fecha móvil

- **Admani Upastuti** *(Vudra; fecha lunar)* — Se celebra durante la primera luna llena de Lamashan y conmemora la fundación de Jalmeray. Se narran historias históricas, se realizan ceremonias públicas y se celebra la herencia vudrani.

### Segundo Día de la Luna de Lamashan — fecha móvil

- **Harvest Feast** *(Erastil y comunidades rurales)* — Segunda gran celebración de la cosecha. Las familias comparten alimentos, hacen balance de reservas y agradecen el trabajo comunitario.

### 6 de Lamashan

- **Ascendance Day** *(Iomedae)* — Recuerda la superación de la Prueba de la Piedra Estelar por Iomedae. Sus fieles cantan, renuevan amistades y practican el perdón.

### 9 de Lamashan

- 🎂 **Conduct random 8**
- 🎂 **Seya Numari**

### 15 de Lamashan

- **Kraken Carnival** *(Absalom)* — Festival otoñal de cometas y combates aéreos, con diseños inspirados en krákenes, monstruos marinos y navíos.

### 19–20 de Lamashan

- **Feast of the Survivors** *(Zon-Kuthon)* — Banquete de cosecha servido sobre mesas de huesos humanos. Los fieles lo interpretan como prueba de la protección de Zon-Kuthon a lo largo de los siglos.
- **Bastion Day** *(Solku)* — Celebración de dos días de la fundación de Solku y de la resistencia de la ciudad frente a sus enemigos.

### 23 de Lamashan

- 🎂 **Ameiko Kaijitsu**

### 27 de Lamashan

- **Jestercap** *(Andoran y otras regiones)* — Día de bufones, sátira, máscaras y bromas. Las jerarquías se relajan temporalmente y la crítica social se disfraza de comedia.

### 30 de Lamashan

- **Allbirth** *(Lamashtu)* — Fiesta de fertilidad monstruosa, nacimientos deformes y exaltación de aquello que las sociedades civilizadas rechazan. Sus ritos suelen ser oscuros y peligrosos.

### 31 de Lamashan

- 🎂 **Koya Mvashti**

---

## Neth — 30 días

Último mes completo del otoño.

### 1 de Neth

- 🎂 **Irgadriel**

### 5 de Neth

- **Independence Day** *(Galt)* — Celebra la ruptura con la monarquía y los ideales revolucionarios de Galt, aunque su significado varía según la facción política.
- 🎂 **Selanza Rovari**

### 7 de Neth

- **Seven Veils** *(Sivanah)* — Fiesta de máscaras, ilusiones y secretos. Cada velo simboliza una capa de la realidad que puede ocultar otra verdad.

### 8 de Neth

- **Abjurant Day** *(Nethys)* — Los vecinos cooperan para reforzar defensas, entrenar aliados en magia protectora y poner a prueba a posibles aprendices.
- **Festival of Making and Breaking** *(Nethys)* — Celebración adicional de la campaña sobre la doble naturaleza de la magia: creación y destrucción.

### 13 de Neth

- **Great Fire Remembrance** *(Korvosa)* — Recuerda el Gran Incendio. Desde el amanecer hasta el siguiente amanecer se evita encender llamas siempre que sea posible.
- 🎂 **Hayashi Tomoko**

### 14 de Neth

- **Even-Tongued Day** *(Cheliax)* — Fiesta de diplomacia, retórica y expansión chelia. En territorios perdidos puede tener un tono de duelo o resentimiento.

### 18 de Neth

- **Evoking Day** *(Nethys)* — Día de demostraciones de magia evocadora, competiciones arcanas y exhibición controlada de poder destructivo.

### 21 de Neth

- 🎂 **Virihane**

### 23 de Neth — fecha de la tabla general de festividades

- **Seven Veils** *(Sivanah)* — La tabla general de PathfinderWiki sitúa aquí esta fiesta de máscaras, ilusiones y secretos. **En el canon de esta campaña se conserva el 7 de Neth**; esta entrada deja constancia de la variante documental.

### 24–30 de Neth

- **Baptism of Ice** *(Irrisen)* — Semana de fertilidad en la que se exhibe a los niños nacidos durante el año. En las ciudades más crueles puede culminar en sacrificios o exposiciones rituales al frío.

### 28 de Neth

- **Transmutatum** *(Nethys)* — Jornada de reflexión y mejora personal. Muchos fieles comienzan proyectos de investigación, transformación mágica o fabricación de objetos.

### 30 de Neth

- 🎂 **Conduct random 9**

---

## Kuthona — 31 días

Comienzo del invierno y cierre del ciclo anual.

### 1 de Kuthona

- **First Day of Winter** — Inicio tradicional del invierno. Las comunidades revisan reservas, refugios, combustible y pactos de ayuda mutua.
- 🎂 **Erivo**

### 3 de Kuthona

- 🎂 **Ulf el Ulfen**

### 7 de Kuthona

- **Pseudodragon Festival** *(Korvosa)* — Celebra la presencia de pseudodragones en la ciudad con exhibiciones aéreas, juegos y pequeños homenajes a estas criaturas.

### Segunda semana de Kuthona — fecha móvil

- **Winter Week** *(Erastil y comunidades rurales)* — Semana que comienza el segundo Día del Sol de Kuthona. Se reparan hogares, se comparten reservas y se refuerzan los compromisos de protección mutua antes del invierno más duro.

### 11 de Kuthona

- **Ascension Day** *(Cayden Cailean)* — Conmemora la ascensión accidental de Cayden tras superar la Prueba de la Piedra Estelar. Abundan la bebida, los desafíos amistosos y los actos de libertad.

### 12 de Kuthona

- 🎂 **Sigmund «Ojos de Hielo»**

### Última luna nueva del año — fecha móvil

- **Blightmother's Eve** *(Gyronna; fecha lunar)* — En la última luna nueva del año, las seguidoras de Gyronna realizan sacrificios y ritos destinados a renovar la paciencia y la fuerza de la diosa.

### 15 de Kuthona

- **Winterbloom** *(Naderi)* — Aniversario de la ascensión de Naderi. Se celebra de forma contenida, con lecturas de tragedias románticas y recuerdos de amores imposibles.
- 🎂 **Conduct random 10**

### 17 de Kuthona

- 🎂 **O Chin Chin**

### 23 de Kuthona

- **Grand Day of Independence** *(Linvarre)* — Gran fiesta política y cívica de la independencia local.

### 30 de Kuthona

- **Night of the Pale** — Noche inquietante en la que, según la tradición, los espíritus de quienes murieron durante el año pueden regresar a los hogares que conocieron. Se cierran puertas, se encienden luces protectoras y se recuerda a los fallecidos con una mezcla de respeto y temor.

### Solsticio de invierno — fecha astronómica móvil

- **Solsticio de invierno** *(fecha astronómica)* — Noche más larga del año. Se celebran rituales de luz, sangre, memoria, protección y resistencia frente a la oscuridad.
- **Crystalhue** *(Shelyn)* — Fiesta de creación artística, amistad y cortejo. Son habituales las obras nuevas, las actuaciones, las propuestas matrimoniales y las celebraciones comunitarias.
- **Ritual of Stardust** *(Desna)* — Los fieles se reúnen junto a hogueras, arrojan polvo de gemas estrelladas sobre las brasas y formulan promesas de amor, amistad y futuros viajes.

### 31 de Kuthona

- **The Final Day** *(Groetus)* — Último día del año. Los fieles de Groetus rezan en silencio al caer el sol y contemplan el inevitable final de todas las cosas.

---
# 11. Índice rápido de cumpleaños

Esta tabla permite localizar los cumpleaños sin recorrer todo el almanaque.

| Mes | Día | Persona |
|---|---:|---|
| Abadio | 1 | Brindelvasthimir Quillopon Dranzibandor |
| Abadio | 21 | Agricultor random 1 |
| Abadio | 30 | Cholna de las Lluvias |
| Calistril | 2 | Frinn |
| Calistril | 2 | Conduct random 11 |
| Calistril | 16 | Uksahkka |
| Calistril | 26 | Ralvio Dorran |
| Farasto | 5 | Freydis Manodura |
| Farasto | 7 | Leif Hagorsson |
| Farasto | 12 | Conduct random 1 |
| Farasto | 14 | Bevelek |
| Farasto | 27 | Conduct random 12 |
| Gozran | 4 | Daichi Tomoko |
| Gozran | 18 | Caurrin Whesterwill |
| Gozran | 19 | Kelda |
| Gozran | 22 | Asmund, el quesero |
| Gozran | 23 | Sandru Vhiski |
| Gozran | 30 | Lewis |
| Desnio | 3 | Vankor |
| Desnio | 3 | Renji |
| Desnio | 9 | Eilvarr |
| Desnio | 10 | Conduct random 3 |
| Desnio | 24 | Yrja Vientonegro |
| Sarenith | 5 | Shalelu Andosana |
| Sarenith | 8 | Ketaek Pasos Cortos |
| Sarenith | 17 | Inga Hagorsson |
| Sarenith | 18 | Ramaso Panor |
| Sarenith | 27 | Velani Korrast |
| Sarenith | 29 | Conduct random 4 |
| Erasto | 10 | Hargor Hagorsson |
| Erasto | 11 | Dominic Torettoson |
| Erasto | 22 | Ormund Gevren |
| Erasto | 29 | Jorgaan de la Roca Blanca |
| Arodio | 7 | Agricultor random 2 |
| Arodio | 15 | Halvard Kaelvik |
| Arodio | 16 | Ashka Cantoceniza |
| Arodio | 18 | Conduct random 6 |
| Arodio | 23 | Siv Hagorsson |
| Rova | 8 | Shuo |
| Rova | 18 | Hiriko |
| Rova | 27 | Perrin Holbrook |
| Lamashan | 9 | Conduct random 8 |
| Lamashan | 9 | Seya Numari |
| Lamashan | 23 | Ameiko Kaijitsu |
| Lamashan | 31 | Koya Mvashti |
| Neth | 1 | Irgadriel |
| Neth | 5 | Selanza Rovari |
| Neth | 13 | Hayashi Tomoko |
| Neth | 21 | Virihane |
| Neth | 30 | Conduct random 9 |
| Kuthona | 1 | Erivo |
| Kuthona | 3 | Ulf el Ulfen |
| Kuthona | 12 | Sigmund «Ojos de Hielo» |
| Kuthona | 15 | Conduct random 10 |
| Kuthona | 17 | O Chin Chin |

---

# 12. Notas de continuidad

## 12.1. Cumpleaños

Las fechas de cumpleaños proceden del canon interno de la campaña. No se recalculan ni se desplazan por coincidir con una festividad móvil.

Los identificadores **Conduct random** y **Agricultor random** se mantienen literalmente hasta que esos PNJ reciban un nombre definitivo.

## 12.2. Desnio 31

**Angel Day** se sitúa el **31 de Desnio**, fecha compatible con la duración real del mes. *Apprentice Appreciation Day* se conserva el mismo día en este documento como decisión operativa de campaña.

## 12.3. Calistril no tiene 30 días

Calistril tiene 28 días, o 29 en año bisiesto. Por tanto:

- no existe un 30 de Calistril;
- *Tempest Day* se celebra el último día del mes;
- entre 4712 y 4722, ese último día es el 29 en 4712 y 4720, y el 28 en los demás años.

## 12.4. Fechas regionales

No todas las fiestas son universales. Una celebración puede ser:

- religiosa;
- nacional;
- urbana;
- propia de una etnia;
- clandestina;
- prácticamente desconocida fuera de su región.

La presencia de una fecha en el calendario no significa que toda la caravana la observe. Sirve para determinar quién podría recordarla, celebrarla, rechazarla o desconocerla.

---

## 12.5. Variantes documentales y decisiones de campaña

| Elemento | Variantes encontradas | Criterio de este documento |
|---|---|---|
| **Seven Veils** | La cronología aportada para la campaña la sitúa el 7 de Neth; la tabla general de festividades la coloca el 23 de Neth. | Se usa **7 de Neth** en juego y se conserva el 23 como referencia documental. |
| **Leap Day** | El calendario civil añade el día bisiesto al final de Calistril; también existe una observancia asmodeana llamada *Leap Day* el 31 de Arodio. | Se tratan como conceptos distintos: **día intercalar civil** y **fiesta religiosa fija**. |
| **Independence Day / Liberty Day** | Dos nombres para la festividad andorana de Sarenith. | Se muestran ambos como denominaciones de la misma celebración. |
| **Archerfeast / Archer's Day** | Variación de nombre para la fiesta de Erastil del 3 de Erasto. | Se muestran ambos; se prioriza *Archerfeast* como nombre de la tabla general. |
| **First Crusader Day / Crusader Memorial Day** | Variación de denominación en las recopilaciones. | Se muestran ambos como una misma conmemoración mendeviana. |
| **Night of the Pale** | La tabla general la coloca el 30 de Kuthona; algunas recopilaciones posteriores la asocian al cierre completo del año. | Se usa **30 de Kuthona**, por ser la fecha de la página tomada como base. |

## 12.6. Cobertura de las festividades

El almanaque incluye:

1. todas las festividades de la tabla general **Holidays and festivals**;
2. las festividades adicionales proporcionadas para esta campaña;
3. celebraciones lunares procedentes de recopilaciones ampliadas;
4. cumpleaños del canon interno;
5. fechas móviles calculadas para 4712–4722.

Cuando dos fuentes discrepan, la diferencia se señala en lugar de ocultarla.

# 13. Fuentes y criterio de autoridad

Fuentes de referencia:

- [Absalom Reckoning — PathfinderWiki](https://pathfinderwiki.com/wiki/Absalom_Reckoning)
- [Holidays and festivals — PathfinderWiki](https://pathfinderwiki.com/wiki/Holidays_and_festivals)
- tablas astronómicas terrestres para los años 2012–2022, usando UTC como referencia;
- canon interno de la campaña *El Regente de Jade*.

Orden de prioridad:

1. canon interno de la campaña;
2. material oficial de Pathfinder;
3. PathfinderWiki y otras fuentes secundarias;
4. convención astronómica terrestre adoptada por este proyecto.

---

# 14. Uso en mesa

El calendario puede emplearse para:

- registrar el avance de la caravana;
- calcular qué día de la semana es;
- decidir si hay mercados, cierres, ritos o celebraciones;
- recordar cumpleaños;
- introducir diferencias culturales;
- hacer visible el cambio de estación;
- anticipar lunas llenas relevantes para criaturas, cultos o magia;
- crear escenas cotidianas sin necesidad de combate.

Una festividad no necesita ocupar una sesión completa. Puede manifestarse mediante:

- una comida especial;
- una oración privada;
- una discusión religiosa;
- un regalo;
- una canción;
- un mercado temporal;
- una jornada de descanso;
- una obligación administrativa;
- un recuerdo doloroso;
- un cambio de ruta para llegar a tiempo a una celebración.

Así, el calendario forma parte de la vida de la caravana y no se limita a una lista decorativa de fechas.
