# scripts

| Script | Para qué |
|---|---|
| `comprobar-limite-ip.sh` | Comprueba desde fuera que el límite de peticiones del login (o, con `RUTA=embudo`, el de `/alta/pasos`) no se salta cambiando `X-Forwarded-For` (S28, DEC-062). Hay que repetirlo tras cualquier cambio de infraestructura o de plan en Render. Gasta el cupo de tu IP: **espera 61 s después de la ráfaga antes de cualquier login o alta de prueba** (GP-264). |
| `entorno-pruebas.sh` | Comprueba el aislamiento y la BD local, rellena las fotos de los ejercicios que falten (GP-248) y arranca la API local (`estado` solo comprueba, `parar`). Ver `documentacion/ENTORNO-PRUEBAS.md`. |
| `reset-datos-pruebas.sh` | Restaura los datos de pruebas al estado base. Exige la API parada. |
| `sembrar_demo.py` | Crea la cuenta demo de las capturas, `@dani.entrena`, con tres semanas de datos. Solo en local. |
| `versionar-hojas-web.py` | Pone en las páginas de `web/` la versión de sus hojas. Ver `web/README.md`. |
| `sesion-arranque/` | Pruebas de la renovación de sesión y del arranque (GP-237, GP-235, GP-234). `proxy-refresh.py` se pone en el 8080 delante de la API local (en el 8081, con el access de 60 s) y decide qué contesta `/auth/refresh`; `tanda.sh <modo>` cuenta las llamadas al volver a Inicio y en la siguiente acción; `matar_y_volver.sh` cuenta cuántas veces se crea cada pantalla al volver tras matar el proceso. Cómo arrancar cada pieza, en la cabecera de cada script. Al acabar, se paran el proxy y la API para dejar libre el 8080. |
| `extraer-musculos.py`, `generar-icono-app.py` | Utilidades sueltas de la app; cada una explica su uso en la cabecera. |

## La cuenta demo y las capturas de la web

Las capturas de `gymprofit.app` (GP-203) son de la app real, con una cuenta de
demostración en el entorno **local** de pruebas: ni producción ni datos de nadie.

### Regenerar la cuenta demo

1. Arrancar la API local: `sh scripts/entorno-pruebas.sh` (BD MariaDB del puerto 3308).
2. Si la cuenta ya existe, borrarla antes: desde la app (Ajustes → Eliminar cuenta) o
   con `DELETE /usuarios/me` en la API local. El script para si el alta falla.
3. Sembrar, desde la raíz del repositorio:

   ```bash
   GYMPROFIT_DEMO_PASSWORD=<la que elijas> GYMPROFIT_DB_PASSWORD=<la de la BD local> \
     python scripts/sembrar_demo.py
   ```

   - La contraseña de la demo no puede contener «gymprofit» ni el nombre de usuario.
     Ninguna de las dos tiene valor fijo en el código.
   - El script **se niega a arrancar** si `GYMPROFIT_API_URL` no es de `localhost` o
     `127.0.0.1`.
   - `MARIADB_CLI` apunta al cliente si no está en el PATH (en Windows,
     `C:/Program Files/MariaDB 11.8/bin/mariadb.exe`).

Deja tres semanas que acaban hoy:

- Sigue el programa «Torso y pierna» (`GIM-TP`, 60 min) y 12 entrenos con sus rutinas:
  lunes torso A, martes pierna A, jueves torso B y viernes pierna B, con los pesos subiendo
  cada semana. Así hay «Hoy toca» con rutina, resumen con la comparación frente a la sesión
  anterior de la misma rutina (GP-210) y semana de nutrición con datos.
- Comidas españolas de cada día. Hoy, solo el desayuno y la comida.
- El agua de cada día.
- 8 pesos, de 78,6 a 76,9 kg. La API fecha cada medición al guardarla, así que el
  script la lleva después a su día con el cliente de MariaDB.

### Las fotos de los ejercicios, como en producción (GP-248)

En producción, cada ejercicio trae dos fotos de free-exercise-db, en
`https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/<fed_id>/0.jpg`
y `/1.jpg`. Es la regla del import (`WgerImportService.mapearFed`), y todos los
ejercicios activos tienen `fed_id`. Sin esas URL, la Biblioteca, «Los que más haces»
y la ficha de la rutina sacan la silueta de reserva y las capturas ya no se parecen a
producción.

No hay ningún paso a mano:

- `entorno-pruebas.sh`, al arrancar, rellena desde `fed_id` las `imagen_url` e
  `imagen_url_2` que falten y dice cuántas ha puesto. Después comprueba que todos los
  ejercicios de las plantillas tienen foto, y si alguno sigue sin ella (eso es que no
  tiene `fed_id`) no arranca la API. `estado` hace la comprobación sin escribir nada.
- `sembrar_demo.py` no crea la cuenta si algún ejercicio del programa no tiene foto.

No se relanza el import (`POST /admin/catalogo/importar-ejercicios`) porque pisaría los
nombres en español revisados a mano (GP-217). Tampoco se restaura el volcado base: es
anterior a los programas.

Comprobado el 13-11-2026: las 50 URL (25 ejercicios, dos fotos cada uno) de las
rutinas de `GIM-TP` contestan 200, y en la BD local las 873 filas con `fed_id` siguen
la regla. Para repetirlo sin tocar producción basta con un `curl -I` a cada URL: la
API de producción no las enseña sin token, y `POST /auth/guest` ya escribe un
refresh token.

«Hoy toca», en Inicio, no lleva fotos: su carrusel pinta el icono de la zona de cada
ejercicio, así que esa captura sale igual con fotos o sin ellas.


### Hacer las capturas

1. Emulador `Pixel_8_Capturas_34` (Pixel 8, API 34, 1080 × 2400). No el móvil propio.
2. **El distintivo «PRUEBAS · LOCAL»** sale en todas las pantallas de `debug`. Para las
   capturas se añade **en local y sin commitear** una variante sin él:
   - en `app/build.gradle`, dentro de `buildTypes`:
     `verificacion { initWith debug; debuggable false; matchingFallbacks = ['debug'] }`;
   - un `src/verificacion/AndroidManifest.xml` con Crashlytics y Analytics apagados.

   Después, `sh ./gradlew :app:assembleVerificacion` e instalar ese APK. Al terminar, se
   quitan las dos cosas y `git status` tiene que salir limpio.
3. Oscuro, español y barra de estado de demostración:

   ```bash
   adb shell cmd uimode night yes
   adb shell cmd locale set-app-locales com.gymprofit.app --locales es-ES
   adb shell settings put global sysui_demo_allowed 1
   adb shell am broadcast -a com.android.systemui.demo -e command enter
   adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
   adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
   adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
   adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
   ```

4. Entrar con `dani.entrena` y saltar los avisos con «Ahora no». Capturar a tamaño
   nativo con `adb exec-out screencap -p > <nombre>.png`, en
   `C:\Users\ruben\Desktop\capturas-web`:
   - `inicio.png`: Inicio, con «Hoy toca Torso A».
   - `entreno.png`: Torso A empezado, la primera serie marcada y el descanso corriendo.
     Después, descartar la sesión (al final de la lista, «Descartar la sesión»).
   - `nutricion.png`: Nutrición, el día de hoy.
   - `progreso.png`: Progreso → Medidas, con la gráfica del peso.
5. Revisar cada una: sin teclado, sin errores, sin pantallas vacías y sin la barra de
   «Sesión en curso».
6. Al terminar, salir del modo demo:
   `adb shell am broadcast -a com.android.systemui.demo -e command exit` y
   `adb shell settings put global sysui_demo_allowed 0`.
