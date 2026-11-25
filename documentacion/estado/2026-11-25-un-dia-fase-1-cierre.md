# «Un día», fase 1: medida del cierre (25-11-2026)

Rama `un-dia-fase-1` (PR #139, borrador, sin fusionar). Responde a los datos del Xiaomi 17T del
propietario: Chrome 158, 417 × 834 @3,25; pasada 1, 59,9 fps de mediana, p5 30,0, 20,8 % de 664
fotogramas > 20 ms; pasada 2, 59,5, p5 24,0, 48,4 % de 192; CLS 0,010–0,019 (PC 0,027). A la vista
no se notaban tirones. TikTok no abrió la URL http de la red local.

## Commits

| Hash | Qué |
|---|---|
| `94f8c371` | fix(web): el panel de «Un día» solo cuenta fotogramas con scroll |
| `f118031a` | feat(web): modo de control del panel de «Un día» |
| `12bd68a8` | fix(web): «Un día» sin desplazamientos de layout |
| `9c9358b6` | test(web): medir-un-dia.py, layout por escena y CLS de «Un día» |
| `7bf229cb` | docs(web,decisiones): criterio de fotogramas con control y vista previa de Pages |

## 1. El panel medía mal

- **Fallos** (`FotogramasWebTest`, 7 tests, 5 vistos en rojo contra la lógica de antes extraída tal
  cual a `fotogramas.js`): contaba los rAF hasta 250 ms después del último scroll, así que entraban
  la cola de reposo tras parar (44 intervalos donde había 30), el primer fotograma de cada gesto
  (mide la pausa entre gestos: un hueco de 400 ms salía como fotograma lento) y el primero al volver
  de una pestaña oculta; y no decía a qué frecuencia iba la pantalla.
- **Ahora**: un intervalo cuenta solo si hubo scroll en los dos fotogramas que lo cierran; un hueco
  de más de 1 s sin rAF es el navegador en pausa y va aparte («pausas»); el panel da la frecuencia
  (`rAF a 60 Hz` / `120 Hz`). Un bloqueo de menos de 1 s en pleno scroll sigue siendo un tirón.
- **Modo de control**, `?debug=motion&control`: la página quieta (el estado de movimiento reducido),
  sin GSAP ni Lenis; se cargan solo `inicio` y el panel (2 ficheros, 4,1 KB, comprobado).
- **Comprobación de punta a punta** en Chromium (417 × 834, scroll en cada rAF, seis gestos de 1,2 s):
  normal ×1 0 % > 20 ms; normal ×4 2,8 % de 251, p5 59,5; control ×1 y ×4, 0 %. Ojo: la rueda de
  headless desplaza un fotograma sí y otro no y entonces no cuenta nada (n = 0): en el móvil el dedo
  y la inercia mueven la página en cada fotograma. Si el panel da un «de N» pequeño, el gesto fue
  demasiado lento.
- Los números del Xiaomi de antes no sirven para el criterio: hay que repetirlos con el panel nuevo.

## 2. Layout por escena

`python scripts/medir-un-dia.py --url http://127.0.0.1:8207` (393 × 760 @2,75, CPU ×4, caché vacía,
sin JS, 5 pasadas; el primer «Layout» del marco de la página, no el de about:blank):

| Página | Primer layout (mediana) | Añade | Pasadas | Primer recálculo de estilos | Layout total hasta load + 1,5 s |
|---|---|---|---|---|---|
| vacía (cabecera y pie) | 19 ms |  | 21, 19, 19, 18, 15 | 8 ms | 19 ms |
| + luz | 18 ms | -1 ms | 20, 22, 18, 18, 16 | 10 ms | 19 ms |
| + reloj gigante | 35 ms | +17 ms | 38, 31, 35, 34, 35 | 13 ms | 36 ms |
| + escenario (móvil y fugas) | 165 ms | +130 ms | 165, 173, 169, 159, 163 | 27 ms | 203 ms |
| + entrada | 206 ms | +41 ms | 220, 217, 184, 206, 201 | 34 ms | 284 ms |
| + transición | 203 ms | -3 ms | 243, 198, 203, 197, 204 | 36 ms | 279 ms |
| + entreno | 229 ms | +26 ms | 340, 229, 225, 226, 246 | 38 ms | 308 ms |
| + cierre | 247 ms | +18 ms | 249, 241, 249, 247, 239 | 37 ms | 328 ms |
| completa + content-visibility: auto en las escenas | 203 ms | -44 ms frente a la completa | 242, 187, 201, 203, 211 | 35 ms | 324 ms |
| completa + content-visibility: hidden en las capas ocultas | 154 ms | -93 ms frente a la completa | 155, 144, 167, 149, 154 | 25 ms | 200 ms |
| completa + las dos | 96 ms | -151 ms frente a la completa | 96, 96, 100, 89, 90 | 22 ms | 183 ms |

- El móvil de la escena se lleva casi todo (+130 ms): cuatro capas apiladas (arranque, Hoy toca,
  sesión, descanso) y las fugas, aunque al cargar solo se ve la de arranque. Quitar una capa sola
  apenas mueve la cifra; contenerlas todas, sí.
- **Contención para la fase 3**: `content-visibility: hidden` en las capas del móvil que no se ven
  y en las fugas (el movimiento las destapa antes de enseñarlas; van con `position: absolute` e
  `inset: 0`, así que no cambia ninguna caja) y `content-visibility: auto` con
  `contain-intrinsic-size` en las escenas fuera de pantalla. Las dos juntas: **96 ms**, bajo el tope
  de 130. CLS con la de escenas: 0,00000 a 417 y a 1920. No se ha aplicado aún: la de las capas
  obliga a que las escenas quiten el `hidden` antes de medir nada dentro.

## 3. CLS

Medido nodo a nodo con el mismo script (carga, entrada y la página entera a saltos):

- **Cuenta atrás del descanso** (lo más, ~0,012): cambiaba de ancho cada segundo. Tenía
  `tabular-nums`, pero el minificador escribe `font-variant-numeric` delante del atajo `font`, que la
  anula. Igual en la cifra del récord. Las dos reglas pasan a propiedades sueltas.
- **Aviso del récord** (~0,005 en móvil, 0,0018 al montar en escritorio): GSAP lo plegaba con
  `height`, márgenes y rellenos y empujaba las filas. Ahora ocupa siempre su sitio, las filas suben
  con `transform` y el fondo de la tarjeta (un `::before`) se acorta por abajo con `--pliegue`.
- **Cabecera de la sesión**: volumen, tiempo y series con cifras tabulares; el volumen reserva el
  ancho de su cifra final.
- **Después**: 0,00000 a 417 × 834 y a 1920 × 1080. Quedan entradas de 0,00000x (las cifras de kg y
  reps que se escriben centradas, 1 px del cambio de fuente en escritorio).
- `UnDiaClsWebTest` (3 tests, los 3 vistos en rojo contra el commit anterior al arreglo).

## 4. Vista previa de Pages

Preparada, sin subir: `web/README.md`, «Vista previa para el móvil». `wrangler` no está instalado;
se usa `npx wrangler@4.168.0` (flags comprobados con `--help`, sin iniciar sesión). La documentación
de Cloudflare confirma `--branch`, el alias `<rama>.<proyecto>.pages.dev` y el `X-Robots-Tag: noindex`
de toda vista previa; no dice con todas las letras cuándo un despliegue es de producción, así que el
paso 2 comprueba antes que `un-dia-prueba` no es la rama de producción. `https://gymprofit.app/un-dia`
da 404 hoy.

## Tests

- Nuevos: `FotogramasWebTest` (7) y `UnDiaClsWebTest` (3). Todos los `*WebTest`: 78, 0 fallos.

## Pendiente del propietario

- Repetir la medida en el Xiaomi (pasos en el resumen) en normal y control, y en TikTok e Instagram
  por la vista previa.
