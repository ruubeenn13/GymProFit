# scripts

| Script | Para qué |
|---|---|
| `entorno-pruebas.sh` | Comprueba el aislamiento y la BD local y arranca la API local (`estado`, `parar`). Ver `documentacion/ENTORNO-PRUEBAS.md`. |
| `reset-datos-pruebas.sh` | Restaura los datos de pruebas al estado base. Exige la API parada. |
| `sembrar_demo.py` | Crea la cuenta demo de las capturas, `@dani.entrena`, con tres semanas de datos. Solo en local. |
| `versionar-hojas-web.py` | Pone en las páginas de `web/` la versión de sus hojas. Ver `web/README.md`. |
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
