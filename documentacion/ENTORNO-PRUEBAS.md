# Entorno de pruebas

Cómo trabajar en la app sin que nada llegue a producción. Complementa a
[ENTORNO-DESARROLLO.md](ENTORNO-DESARROLLO.md), que explica cómo arrancar cada
pieza; este documento explica **por qué no se puede tocar producción sin querer**.

Verificado en emulador el 18 de septiembre de 2026.

---

## Resumen

| | Pruebas (build `debug`) | Producción (build `release`) |
|---|---|---|
| API | `http://10.0.2.2:8080/api/` (tu PC) | `https://api.gymprofit.app/api/` |
| Base de datos | MariaDB local, puerto **3308** | Aiven MySQL |
| Crashlytics y Analytics | **apagados** | activos |
| Distintivo en pantalla | `PRUEBAS · LOCAL` arriba | ninguno |
| Usuario | `prueba` / `Prueba1234.` | cuentas reales |

Hay tres cuentas locales, cada una para una cosa:

| Cuenta | Contraseña | Para qué |
|---|---|---|
| `prueba` | `Prueba1234.` | Recorrido normal, **con datos**: sesiones, comidas con alimentos, mediciones y rutinas propias |
| `vacia` | `Vacia1234.` | **Estados vacíos**: no tiene ni un dato, y es donde se ven los huecos que el caso feliz tapa |
| `admin` | `AdminDev1234.` | Panel de administración |

El punto final de las contraseñas no es un capricho: `adb shell input text` no
teclea `!`, así que una contraseña con admiración rompe cualquier prueba en
emulador.

Todo lo que crees, edites o borres en el build de pruebas se queda en la base de
datos local. La de producción no se toca en ningún paso.

---

## Las tres capas de aislamiento

No basta con "acordarse de no apuntar a producción". Hay tres barreras y hacen
falta las tres, porque cada una cubre un fallo distinto.

### 1. No compila

`app/build.gradle` valida `BASE_URL` antes de generar nada. Si en
`local.properties` hay una URL que no sea de la red local (emulador, localhost o
LAN privada), el build **falla** con un mensaje explícito en vez de producir un
APK que escribiría en Render.

Comprobado: poniendo la URL de Render en `local.properties`, Gradle se detiene con

```
BASE_URL de desarrollo apunta fuera de la red local: 'https://gymprofit-api.onrender.com/api/'.
El build de desarrollo no puede escribir en producción.
```

La URL de producción vive **solo** en `buildTypes.release`, que sobrescribe ese
valor. El APK de release sigue saliendo como siempre.

### 2. No arranca

`GymProFitApp.verificarEntornoAislado()` vuelve a comprobarlo al iniciar el
proceso. Cubre el caso de un APK de desarrollo compilado antes de existir la
guarda anterior, o en otra máquina: en lugar de escribir en producción, la app
se cierra con el motivo. En release no hace nada.

### 3. Se ve

Cada pantalla del build de pruebas lleva un distintivo rojo `PRUEBAS · LOCAL`
centrado en la parte superior. Se dibuja sobre la ventana desde la clase
`Application`, así que aparece en las 42 pantallas sin tocar ningún layout, y no
intercepta toques.

**Regla: si el distintivo no está, estás mirando producción.** Vale para
capturas, vídeos y pruebas.

### Extra: telemetría apagada

`app/src/debug/AndroidManifest.xml` desactiva Crashlytics y Analytics en el build
de pruebas. Los fallos provocados probando no ensucian la consola de producción
ni se mezclan con los de usuarios reales.

---

## Uso diario

```bash
./scripts/entorno-pruebas.sh           # comprueba el aislamiento y arranca la API local
./scripts/entorno-pruebas.sh estado    # solo comprueba, no arranca nada
./scripts/entorno-pruebas.sh parar     # para la API local
```

El script se niega a seguir si detecta que la app apunta fuera de la red local o
si no encuentra la base de datos en el 3308.

Compilar e instalar en el emulador:

```bash
cd app/GymProFit
JAVA_HOME="/c/Program Files/Android/Android Studio1/jbr" ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Volver al punto de partida

```bash
./scripts/entorno-pruebas.sh parar       # la restauración exige la API parada
./scripts/reset-datos-pruebas.sh         # devuelve la base local al estado base
```

Deshace todo lo creado probando: usuarios, rutinas, sesiones, comidas y
mediciones. El catálogo (873 ejercicios con imágenes y los alimentos) forma parte
del estado base y se conserva, así que no hay que reimportar nada.

Para fijar un nuevo punto de partida (por ejemplo tras preparar datos de demo):

```bash
./scripts/reset-datos-pruebas.sh --guardar
```

El volcado vive en `api/gymprofit-api/sql/snapshots/dev_baseline.sql` y está
excluido del repositorio: son datos, no código.

---

## Qué contiene la base local

Contado el 21 de septiembre de 2026:

| Tabla | Filas |
|---|---|
| ejercicios | 1527 (873 activos, con imágenes) |
| usuarios | 5 |
| alimentos | 12 |
| rutinas | 8 (6 predefinidas del catálogo + 2 de `prueba`) |
| sesiones de entrenamiento | 6 |
| mediciones corporales | 5 |
| comidas | 4 |
| logros | 6 |

El catálogo —ejercicios, alimentos y las rutinas predefinidas— es lo que hace que
no haya que sembrar nada para probar una pantalla de lista. Lo demás es poco a
propósito: los datos de usuario se crean y se borran constantemente al probar.

**El recuento anterior, del 18 de septiembre, decía 18 usuarios y unas 11 filas
por tabla de usuario.** Ya no es así, y no por un borrado accidental: son las
altas y bajas de las pruebas de esos días, incluida la verificación del borrado
de cuenta de GP-008, que por definición vacía todo lo de un usuario. Si el número
importa para algo, **cuéntalo, no lo leas aquí**: esta tabla envejece en cuanto
alguien prueba algo.

`prueba` **ya no sirve para recorrer el alta desde cero**: tiene historial
sembrado. Para el alta y el onboarding, crea una cuenta nueva desde la app; para
los estados vacíos, usa `vacia`.

En la base local queda además `gp008_test` (id 5078), sobrante de una sesión de
pruebas del 19 de septiembre que se cortó a medias. No molesta y no se ha tocado.

---

## Móvil físico en lugar de emulador

Añade la IP del PC en la misma WiFi a dos sitios:

1. `local.properties` → `BASE_URL=http://192.168.x.x:8080/api/` (la guarda de
   Gradle acepta rangos privados).
2. `app/src/main/res/xml/network_security_config.xml` → un `<domain>` con esa IP,
   porque por defecto solo se permite HTTP en claro hacia el emulador y localhost.

---

## Lo que este entorno todavía no aísla

- **Notificaciones push.** Los tokens se registran contra la API local, pero el
  envío sale del mismo proyecto de Firebase que producción. No afecta a datos de
  usuario; conviene saberlo antes de probar envíos masivos.
- **Open Food Facts.** La búsqueda de alimentos consulta el servicio público en
  vivo también en pruebas. Es solo lectura: no se escribe nada fuera.
