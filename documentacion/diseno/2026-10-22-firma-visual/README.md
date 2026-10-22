# Firma visual — el lienzo del 22-10-2026 (GP-210)

Aprobado por el propietario el 22-10, con Profi animado.

- `fuente/*.dc.html`: el lienzo, tablero a tablero, con los valores exactos (px = dp, colores, textos y `@keyframes`). Se lee como especificación; no se ejecuta. Las marcas `{{ … }}` y el bloque `data-dc-script` son del editor del lienzo.
- `fuente/canvas.json`: el índice, con el título y la posición de cada tablero.
- Las imágenes van como `/_blob/<id>`; la tabla de abajo dice qué fichero es cada una. Las capas de Profi están en el kit `profi_kit_gp210.zip` (llega con el paso 5).

## Tableros

| N.º | Fichero | Qué es |
|---|---|---|
| 1 | `Main` | Sesión en vivo: marcar la serie 3, récord en mitad de la serie y descanso |
| 2 | `Descanso` | El descanso con el móvil bloqueado (Live Update y notificación normal) |
| 3 | `Resumen` | El resumen del entreno; sustituye al tablero 11 de `2026-10-02-nutricion` |
| 4 | `Tarjeta` | La tarjeta 9:16 para compartir |
| 5 | `Semana` | «Tu semana», los lunes |
| 6 | `Alta` | «Todo listo» con Profi |
| 7 | `Vacio` | Un vacío: el historial |
| 8 | `Logro` | Logro conseguido |
| 9 | `Transiciones` | Container transform, eje X, fundido y atrás predictivo |
| 10 | `Expresivo` | La pasada Expressive |
| — | `Momentos` | Los momentos 24 a 42 para DEC-039 |
| — | `Decisiones` | Las once decisiones y los seis pasos |

## Imágenes

| `/_blob/…` | Fichero |
|---|---|
| 344f18165d89a9c9ba306e836b415172 | el logo de la variante oscura (`drawable-night/logo.png`) |
| 849a992f…, 87a6d089…, c3bc89ec…, 4e80685e… | `profi_saluda_base`, `_mano`, `_puno`, `_ojos` |
| c2fb95a1…, 73e1d167…, 2afb4f7c… | `profi_senala_base`, `_mano`, `_ojos` |
| d4850d96…, 595cf8f7… | `profi_pulgar_base`, `_puno` |
| 9e44323d…, 820bd031…, 41c70481… | `profi_hombros_base`, `_mano_izq`, `_mano_der` |
| 760ea880… | `profi_salta_base` |
| 23fd159b…, f806c497… | `profi_cara_sonrisa_base`, `_ojos` |

Todas las de Profi, en su variante `noche` (el lienzo es oscuro).

## Lo que el lienzo no dice

- **Resumen, «Para recuperar» (tablero 3):** lleva lo del tablero 11 de nutrición: las sugerencias con su «+» que se apuntan en la comida que toca, no un enlace. Solo sale si faltan 20 g de proteína o más, y no cambia los objetivos del día (decisión 12 de nutrición).
- **Resumen, «Frente a la última vez»:** kilos por ejercicio y % frente a la sesión anterior del mismo ejercicio. Las marcas concretas ya salen en los récords. En la primera vez de un ejercicio: «Primera vez: es tu punto de partida» (como `envivo_primera_vez`), sin porcentaje.
- **Resumen, el logro:** sin Profi hasta el paso 5; mientras, el medallón dorado.
- **Profi:** solo en el alta, los vacíos, los logros y «Tu semana»; capas claro y oscuro; momentos 30 y 39 a 42.
