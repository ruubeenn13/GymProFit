# Diseño de gymprofit.app (GP-205)

Dirección del rediseño de toda la web. La identidad no cambia: fondo `#0B0C0E`, el naranja de la app `#FF6A00`, Barlow y Barlow Condensed servidas desde el dominio y los logos de `img/`. Lo que cambia es el mundo en que se ponen, la composición y el movimiento. Reglas técnicas: DEC-053 y `README.md`.

## Concepto: el dorsal

Apuntarse a la beta es **inscribirse**. La portada es un dorsal prendido sobre una camiseta técnica oscura:

- **La tela** es el fondo: el `#0B0C0E` de siempre con un punto muy fino y algo de grano, como el tejido de una camiseta de entrenar vista de cerca. No es un degradado ni un «fondo oscuro con brillo»: es una superficie.
- **El dorsal** es el único panel claro de toda la página: blanco roto con la fibra del Tyvek, cuatro ojales de imperdible en las esquinas, una banda naranja arriba con `GYMPROFIT · BETA` y `ANDROID`, y abajo un troquel punteado con la tira desprendible, donde va «Entra en la beta». El titular ocupa el sitio del número, a esa escala.
- **El móvil** con la captura real va al lado, como lo que llevas en la mano. Unas líneas-guía finas señalan datos que de verdad están en la captura («Hoy toca», el descanso que corre, lo que queda de calorías).

Por qué funciona aquí: un dorsal es lo que te dan cuando te apuntas a algo serio, y lo reconoce cualquiera que haya corrido una popular. Da a la página un objeto propio que no es una pantalla de móvil sobre un degradado, y una sola superficie clara que gana a todo lo demás.

Lo que la página **no** es (lo que pidió el propietario): plantilla SaaS (hero centrado, tres tarjetas iguales, degradados), movimiento de feria, ni estética de gimnasio agresiva (negro y neón, tono «no pain no gain»).

## Ritmo de la portada

Una sola gramática, con pasajes densos y pasajes quietos alternados:

1. **Dorsal** (alto, ocupa la primera pantalla): titular, una línea de subtítulo, la tira con la acción y el móvil.
2. **«Entrenas en serio. Comes a ojo.»** (quieto, tipográfico): el dorsal se suelta y sube; la frase llena el ancho sobre la tela.
3. **Entreno, nutrición y progreso** (denso): el móvil queda fijo y cambia de pantalla al llegar a cada pilar; el texto de cada uno pasa a su lado. Sustituye al zigzag de tres bloques iguales de GP-203.
4. **Cómo se entra en la beta** (quieto): los tres pasos como una tira de dorsal troquelada en tres partes.
5. **Preguntas** (lectura): lista sobria.
6. **Cierre**: la acción una última vez y el pie legal.

## Sistema de movimiento

Mejora progresiva (DEC-053): sin JavaScript, todo está en su estado final y la página funciona entera. Con `prefers-reduced-motion: reduce` no se carga GSAP ni Lenis y no se mueve nada; tampoco hay scroll suave.

**Curvas** (de `emil-design-eng`; las de CSS son blandas):

| Token | Curva | Uso |
|---|---|---|
| `--salida` | `cubic-bezier(0.23, 1, 0.32, 1)` | todo lo que entra |
| `--recorrido` | `cubic-bezier(0.77, 0, 0.175, 1)` | lo que se desplaza en pantalla (el cambio de pantalla del móvil) |
| `--tela` | `cubic-bezier(0.32, 0.72, 0, 1)` | el dorsal al asentarse |

Nunca `ease-in` en una entrada, nunca rebote, nunca desde `scale(0)`.

**Duraciones**:

| Qué | Duración |
|---|---|
| Pulsar un botón (`scale(0.97)`) | 140 ms |
| Hover (solo con `hover: hover` y puntero fino) | 160 ms |
| Una línea del titular | 650 ms, escalonadas 70 ms |
| El dorsal al asentarse | 700 ms |
| Cambio de pantalla del móvil | 420 ms (o atado al scroll, con `scrub`) |

La entrada del hero es **CSS**, no JavaScript: empieza en el primer pintado y corre fuera del hilo principal aunque el navegador esté cargando lo demás. GSAP solo hace lo que CSS no puede: lo que va atado al scroll.

**Coreografía del scroll** (GSAP + ScrollTrigger, Lenis para el scroll suave):

- Del dorsal al problema: el dorsal pasa de -2° a 0° y sube más despacio que la página (0,85×), como si se soltara del pecho; la frase del problema entra por líneas.
- Los pilares: el móvil queda fijado (`pin`, `start: "top top"`) y su pantalla cambia de inicio a entreno, nutrición y progreso cuando el texto de cada pilar llega al centro. El cambio es un corte de cortina vertical (`clip-path`), no un fundido de dos imágenes encima.
- Las líneas-guía se dibujan cuando su pantalla está a la vista (`stroke-dashoffset`).
- Una cosa se mueve cada vez. Lo que se lee no se mueve mientras se lee.

**Qué no se anima**: los formularios, los enlaces legales, el pie, las preguntas al abrirse (solo el signo) y cualquier cosa que se repita muchas veces.

## Tipografía a escala

Una familia, dos anchos, como la app. Escala fluida con `clamp()` y topes en px (`TitularesWebTest`).

| Papel | Letra | Tamaño |
|---|---|---|
| Número del dorsal (titular) | Barlow Condensed 700, mayúsculas, interlínea 0,86 | llena el panel línea a línea (`--k` y `cqi`), 44–168 px |
| Frase de sección | Barlow Condensed 700, mayúsculas | 44–200 px, llena el ancho |
| Pilar | Barlow Condensed 700 | 44–96 px |
| Lema | Barlow 400 | 20–24 px |
| Texto | Barlow 400 | 17–19 px, 65 caracteres como mucho |
| Etiqueta (banda del dorsal, líneas-guía) | Barlow Condensed 600, mayúsculas, +0,14 em | 13–15 px |

Etiquetas pequeñas en mayúsculas solo en la banda y en las líneas-guía: nada de «antetítulos» encima de cada sección.

## Texturas y profundidad

- **Grano** sobre una capa fija que no recibe el puntero (`position: fixed`, nunca sobre lo que se desplaza), con un ruido SVG en línea al 5 %: sin peticiones.
- **Punto de la tela**: un patrón repetido de 3 px casi invisible; da superficie sin dibujar nada.
- **Luz**: una sola, cálida y alta a la izquierda, como la de un gimnasio, muy tenue sobre la tela.
- **Profundidad** con tres planos: tela, dorsal (sombra corta y teñida, no negra pura) y móvil (sombra larga). Nada de cristal ni brillos de neón.
- Una sola escala de radios: el dorsal con esquinas de 14 px (el Tyvek troquelado), los botones en píldora, el móvil con su canto.

## Referencias

| Referencia | Qué tomo | Qué no |
|---|---|---|
| Páginas de producto del Apple Watch y del iPhone (apple.com) | El dispositivo fijado mientras su pantalla cambia al ritmo del scroll, y que solo se mueva una cosa cada vez | El volumen de vídeo y la escala de producción |
| Strava (strava.com) | El naranja como señal y no como fondo, y la prueba con datos reales de la app en vez de ilustraciones | La fotografía de estilo de vida |
| WHOOP (whoop.com) | Fondo oscuro con tipografía condensada grande y el producto como protagonista; la seriedad sin épica | El tono de rendimiento extremo |
| Los dorsales de las carreras populares (Behobia, Maratón de Valencia) | El objeto: Tyvek, banda de patrocinio, imperdibles, troquel y tira desprendible | El número: no hay número que dar a nadie, así que ese sitio lo ocupa el titular |

## Lo que se valoró y no entra

- **Cifras de la app que cuentan**: las únicas cifras disponibles son las de la cuenta demo (878 kcal, 120 kg × 5…). Animarlas las convierte en afirmaciones del producto y la web no inventa cifras. Las líneas-guía señalan esos datos dentro de la captura, donde se leen como lo que son: una pantalla de ejemplo.
- **SplitText**: su licencia actual lo permite (todo GSAP es gratis desde 2025, comprobado el 21-10-2026 en gsap.com/standard-license), pero los titulares ya van partidos por líneas en el HTML para el ajuste al ancho; no hace falta pagar 3,6 KB por partirlos otra vez.

## Presupuesto

GSAP 3.15.0 + ScrollTrigger 3.15.0 + Lenis 1.3.26 + el script propio ≤ 60 KB comprimidos; LCP < 2,5 s en 4G móvil; CLS < 0,05. Ver DEC-053.
