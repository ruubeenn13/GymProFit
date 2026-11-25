# «Un día», fase 1, segunda vuelta (25-11-2026)

Rama `un-dia-fase-1` (PR #139, borrador, sin fusionar). Sustituye la prueba de `/un-dia` de la
primera vuelta, que salió vacía y quieta. Manda la «Política de impacto» de `web/PLAN-UN-DIA.md`.

## Commits

| Hash | Qué |
|---|---|
| `9e122c08` | fix(web): cargas creíbles en la demo de «Un día» |
| `a1eb6462` | feat(web): «Un día» con la política de impacto |
| `6cbc5f4c` | docs(web): política de impacto de «Un día» y presupuesto de DEC-069 |

## Datos de la demo

- **42,5 kg en tres ejercicios.** `scripts/sembrar_demo.py`, `entrenar()`: la carga salía de
  `KILOS[tipo]` (BASICO 60, EXTRA 20) más `(ejercicioId % 5) * 5` más el escalón semanal.
  Elevaciones laterales (id 1524), curl (904) y extensión de tríceps (1684) dan resto 4:
  20 + 20 + 2,5. El mismo cálculo ponía 85 kg en las dominadas y kilos en la plancha. Ahora hay una
  carga y un escalón por ejercicio del programa (intermedio de 77 kg), el peso corporal va sin
  kilos y la plancha por segundos. «Hoy toca»: 80, 70, 47,5, 60, 10, 32,5 y 25 kg.
- **`--hoy AAAA-MM-DD`**: la demo se resembró como lunes 23-11 (hoy es miércoles); el script
  lleva la fecha de alta a ese día en la BD local. Solo MariaDB y API locales (GP-282).
- **El récord** sigue saliendo de `LogicaRecordVivo.supera`: 82,5 × 8 sobre 80 × 10
  (`UnDiaWebTest.elRecordCaeEnLaTerceraSerieConLaReglaDeLaApp`).
- **«Espalda, Hombros y Brazos», sin pecho.** No es la demo: es la plantilla más la regla de la
  app. La plantilla `GIM-TORSO-A` (`api/gymprofit-api/src/main/resources/db/semillas/catalogo-plantillas-v1.json`)
  tiene un ejercicio de pecho (3 series) y dos de espalda, hombros y brazos (6 series cada zona).
  `ZonasDelDia.de` (`app/.../utils/ZonasDelDia.java`) ordena las zonas por series e
  `InicioHoyDetalle.zonasListas` (`app/.../ui/inicio/InicioHoyDetalle.java`) se queda con las tres
  primeras: el pecho cae aunque sea el primer ejercicio. No se toca aquí.

## Tests

- Nuevos, vistos en rojo contra la prueba anterior y el JSON de `HEAD` (8 en rojo):
  `UnDiaImpactoWebTest` (7: ancho del reloj, columnas de cuentakilómetros, lamas sin degradados
  en JS, récord y frases en el HTML sin JS, cursor del sistema en enlaces/botones/campos, 12–20
  chispas mirando el equipo, SplitText/DrawSVG fuera del módulo inicial) y
  `UnDiaWebTest.lasCargasDeHoyTocaSonCreibles`.
- Cambiados: el presupuesto diferido de `UnDiaWebTest` (80/90 KB) y `ignoreMobileResize`, que
  ahora se busca en `motor.js`; la prohibición de `pin:` pasa a mirar todos los módulos.
- `testDebugUnitTest` completo: 1376 tests, 0 fallos.

## Medidas (servidor local, `servir-web.py` con brotli)

- **JS sobre `dist`, gzip -9**: inicial 1.945 B; diferido móvil 55,4 KB (motor con GSAP,
  ScrollTrigger, SplitText y DrawSVG 50,0 KB + escenas); escritorio 61,5 KB (+ Lenis 6,1 KB y el
  cursor 1,0 KB).
- **Lighthouse 13.6.0 móvil, mediana de 5**: rendimiento 100 (100 ×5), accesibilidad 100 (×5),
  LCP 762 ms, FCP 762 ms, CLS 0, TBT 0 ms, Speed Index 1.642 ms, 160 KB. JSON en el escritorio,
  `un-dia-fase-1/vuelta-2/lighthouse`.
- **LCP**: el subtítulo, no el titular. Con el titular troceado por caracteres cada letra es su
  propio bloque y ninguno es candidato. Ver dudas.
- **Fotogramas, Chromium de escritorio con CPU ×4** (orientativo; manda el Xiaomi): ~60 fps de
  mediana a 393 y a 1440, 7 % de fotogramas de más de 20 ms; con la página ya montada, ninguna
  tarea larga al desplazar en móvil y una de 55 ms en escritorio. Si se baja nada más cargar, el
  montaje de Torso A (202 ms con ×4, unos 50 ms sin freno) cae en pleno scroll.
- **Movimiento reducido**: 0 trozos de movimiento descargados a 360, 768 y 1440.

Capturas en `un-dia-fase-1/vuelta-2/capturas` (360, 768 y 1440: entrada, mitad de la transición,
serie, check, descanso, récord; y la página entera con movimiento reducido).

## Pendiente del propietario

- La prueba en el Xiaomi 17T (Chrome, TikTok, Instagram) con `?debug=motion`. No se encendió el
  servidor de red: la wifi del equipo («Xiaomi 17T 20») está como red pública.

## Respuestas del propietario y cierre (25-11-2026)

-` `3d14b102 perf(web): el montaje de «Un día» en tareas cortas
-` `01ec81f1 docs(web): el brief de impeccable de la portada se retira en la fase 4
-` `846e0ce0 fix(web): frases del descanso y de la serie 2 de «Un día»
-` `66557eda test(web): el titular de «Un día» se lee entero y una sola vez
-` `4c8310fb docs(documentacion): informe de la segunda vuelta de «Un día»
-` `6cbc5f4c docs(web): política de impacto de «Un día» y presupuesto de DEC-069

- **LCP**: se queda en el subtítulo; el titular sigue por letras. `UnDiaImpactoWebTest`
  comprueba que el lector lee el h1 entero y una sola vez, y que todo lo troceado va oculto con su
  texto entero al lado. Vistos en rojo quitando el `aria-hidden` de las letras en `dist`.
- **Frases**: la 4 pasa a «¿Necesitas más? +15 s.» y la 5 a «Igualar no es récord.».
- **Montaje en tareas cortas.** Perfil: 393 × 760, CPU ×4, caché vacía, bajando nada más cargar,
  tres pasadas, tareas largas desde la navegación.
  - Antes: la mayor, 238, 261 y 248 ms (el montaje de Torso A); 5 tareas de más de 50 ms por
    pasada. Atribución por traza: montaje 231 ms, GSAP y plugins evaluados de una vez 94 ms,
    fotograma de GSAP 70 ms, más el primer pintado.
  - Después: ninguna tarea de montaje pasa de 50 ms. Por pasada quedan 212 ms, ninguna y 57 ms,
    todas del primer pintado de la carga. Cambios: GSAP pieza a pieza (`cargar.js`), cada escena
    cede el hilo entre trozos (`ceder()`), Torso A se monta en tiempo libre antes del viewport y
    sin el `refresh` que pintaba la sesión entera (84–127 ms).
  - El primer layout no es montaje y no baja de 50 ms con ×4 en ninguna página: `/un-dia`
    180–221 ms, la portada actual 113–123, `/privacidad` 83–101, una página vacía 13–16.
- **CHANGELOG** al fusionar; capturas de la portada y brief de impeccable, fuera en la fase 4.
- Tests: `testDebugUnitTest` completo, 1378, 0 fallos.
