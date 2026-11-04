# Medir el arranque (GP-223)

Detector de huecos y atenuaciones de la G en el traspaso del splash, el tiempo hasta
`MainActivity` y la traza de Perfetto. Todo por `adb` contra el dispositivo conectado.
Necesitan `adb`, `ffmpeg` y Python con `numpy` y `Pillow`. Las grabaciones y las trazas
**no van al repositorio**: se dejan en el escritorio.

| Script | Qué hace |
|---|---|
| `graba.sh <nombre> <escala_anim> <segundos> [extras de am start]` | Un arranque en frío desde el lanzador grabado con `screenrecord`, y sus fotogramas analizados con `fotogramas.py`. |
| `serie.sh <prefijo> <n> <segundos> [extras]` | N arranques con `graba.sh`, esperando antes de cada uno a que la carga baje de 2,5; por cada uno, si saltó el respaldo (log `Arranque`), la pantalla final, `oro.py` y `caja.py`. |
| `graba_traza.sh <prefijo> <max> [extras]` | Arranques con traza de Perfetto (`traza.cfg`) y grabación a la vez, hasta el primero con hueco. |
| `fotogramas.py <video> <carpeta>` | Todos los fotogramas (sin remuestrear en el tiempo, a 270 px de ancho) con su hora, luminancia y destellos claros sobre la pantalla oscura. |
| `oro.py <carpeta>` | La fracción de píxeles de la G por fotograma; hueco o atenuación = por debajo del 85 % de la G en reposo entre que entra y que sale. |
| `caja.py <video>` | Lo mismo a resolución completa, y cuánto se mueve la caja de la G (borde, ancho y alto en px). |
| `brillo.py <carpeta>` | Los tramos en que se ve el brillo sobre la G. |
| `mide.py <componente> <n> <etiqueta>` | El tiempo del toque al primer fotograma de `MainActivity` (logcat `START u0` → `Displayed`), n veces, con la mediana. |
| `ui.py ids · tap · esperar · escribir · top · entrar` | Ayudante de `uiautomator` para tocar y esperar vistas por id o texto. |

## Uso

```sh
export MSYS_NO_PATHCONV=1                  # en Git Bash de Windows
export GP_SALIDA=~/Desktop/gp223-arranque  # adónde van vídeos, fotogramas y trazas
mkdir -p "$GP_SALIDA"
sh scripts/arranque/serie.sh ini 12 13                          # hacia Inicio, 13 s
sh scripts/arranque/serie.sh lenta 3 13 --el depuracion_lento_ms 5000   # solo debug
python scripts/arranque/mide.py com.gymprofit.app/es.pmdm.gymprofit.ui.activities.MainActivity 7 final
```

Antes de medir, la carga del dispositivo por debajo de 2,5 (`adb shell cat /proc/loadavg`):
recién arrancado el emulador pasa de 20 y los tiempos no valen. Hacia Inicio, grabar 13 s:
con 8 la salida a veces no entra en el vídeo.

## En un móvil real

- `oro.py` y `brillo.py` buscan la G en una caja calculada para 1080 px de ancho; con otra
  densidad, `GP_DENSIDAD` (px por dp: 2,625 a 420 dpi; `adb shell wm density` / 160).
  `caja.py` usa un cuadrado de 2 × `GP_CAJA` px (320 por defecto): súbelo o bájalo en
  proporción al ancho de la pantalla.
- `screenrecord` graba a la resolución de la pantalla; no funciona en el emulador de API 24.
- Las secciones de la app en la traza (`gp:traspaso.*`, `gp:arranque.*`) se han visto con
  la build debug en el emulador; con el release en un móvil real no está comprobado que
  salgan (puede hacer falta que la app sea `profileable`).
- `--el depuracion_lento_ms N` solo existe en debug.
