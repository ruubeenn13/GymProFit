# GP-285 · Liquid Glass, lote 2: la Sesión en vivo con el descanso (28-11-2026)

Rama `gp-285-lote-2`, PR #149 **en borrador, sin fusionar**: primero se revisan las capturas.

## Resumen

- **La Sesión en vivo y su panel de descanso están en Liquid Glass**, en claro y en oscuro y en los tres niveles del cristal (API 36, 32 y 24), siguiendo `sesion-en-vivo.dc.html` y el README de la referencia.
- **La lógica no se ha tocado**: sesión, descanso, alarmas, notificación, Live Update, `LogicaDescanso` y guardado siguen igual. Los momentos de DEC-039 cambian de dibujo, no de duración ni de curva.
- **Un solo principal en el descanso: «Saltar».**
- **GP-286 cerrado**: los rótulos de las columnas de una serie no se parten con letra grande; `FilaSerieRotulosTest` sin `@Ignore` y en verde a letra 1,0, 1,3, 1,5 y 2,0, en ES y EN.
- **`CamposSerie` borrado**: no tenía ningún uso.
- **Revisor de UI**: dos pasadas; la segunda, sin altos ni medios.
- **Fotogramas lentos: SIN MEDIR.** El portátil iba con batería al llegar el momento de medir, y esa es la primera condición. No hay cifra ni se da nada por bueno (ver «Fotogramas lentos»).

## Commits

| Hash | Qué |
|---|---|
| `91f11fe1` | se borra `CamposSerie` (y el id y el comentario que solo existían por ella) |
| `39c6c5a0` | la Sesión en vivo y el descanso en Liquid Glass, y GP-286 |
| `3cabf206` | lo que salió al probar en Android 7: el teclado al abrir, «¡Récord!» partido y la lista bajo el título |
| `d38e8754` | lo que encontró la primera pasada del revisor de UI |
| `5fb9a1c0` | DESIGN.md, DEC-070, DEC-039, la referencia de diseño y el CHANGELOG |
| `b9ff00e3` | el nombre de un ejercicio en dos líneas, sin una línea en blanco en medio |
| `bcdd6365` | el único bajo nuevo de la segunda pasada del revisor: el borde de abajo sube en el mismo fotograma que el panel |

Después va el commit de documentación que trae este informe.

## Qué se ha hecho

**Estructura.** Un `LienzoCristal` a pantalla completa con el fondo ambiental de la sesión (naranja, **dorado** y azul: `Ambiente.sesion`), la lista y los bordes del sistema. Encima, de cristal vivo: minimizar y «Terminar», el resumen (tiempo, volumen, series y la barra de la sesión) y, abajo, el descanso. La pantalla coloca sus márgenes del sistema, como la navegación principal (`VentanaCristal`, que ahora comparten las dos).

**La tarjeta de cada ejercicio** es cristal estático. La lista es plana —cabecera, una fila por serie y pie son elementos sueltos, porque cada serie se quita deslizándola—, y el cristal no se puede partir en trozos (su relleno es un degradado de esquina a esquina: se verían las juntas). La pinta la lista de una pieza, por detrás (`TarjetaEjercicioCristal`), con el mismo `Cristal` y la misma sombra que las tarjetas de Inicio.

**Filas.** La serie en curso lleva la «lente»; la de un récord, el tinte dorado del diseño con «¡Récord!» donde iba la marca anterior y el trofeo en su botón. Los campos son un surco con borde. Sin hacer, el botón de marcar es un aro vacío; hecha, el círculo relleno con su check.

**Descanso.** Panel de cristal vivo a 12 dp de los lados y 22 del hueco del sistema: «Descanso», la cuenta atrás, «−15 s», «+15 s», «Saltar», la barra que se vacía (antes, un aro) y «Siguiente: …». Con letra grande los botones bajan a su propia fila.

**Componentes reutilizados, sin duplicar ninguno**: `Cristal`, `CristalVivo`, `LienzoCristal`, `FondoAmbiental`, `BordeSistema`, `BarraProgresoCristal`, `BotonMarca`, `BotonCristal`, `FilaBotones`, `Hundir` y `TextoSinPartir`. Lo nuevo es lo que no existía: `TarjetaEjercicioCristal` (la tarjeta de una lista plana), `FilaRotulos` (rótulos de columnas de distinto ancho, al mismo tamaño), `CampoQueEncoge` (un campo que no corta su cifra) y `VentanaCristal` (las barras transparentes, sacadas de `MainActivity`).

## El principal del descanso

Antes, «−15 s», «+15 s» y «Saltar» llevaban los tres el estilo del principal, y «Terminar», arriba, otro. Queda **«Saltar»** (`BotonMarca`), porque es el que avanza el entreno: lleva a la serie siguiente. «−15 s» y «+15 s» ajustan la espera y son secundarios (`BotonCristal`); «Terminar» también, como en la maqueta. «Reintentar», en la tarjeta del guardado, es principal pero no convive con el descanso: guardar lo borra. Lo vigila `SesionEnVivoBotonesTest`.

## Donde la maqueta cede a una regla

Con los valores de la maqueta, `ContrasteCristalTest` falla (visto en rojo). Manda la regla, como en el lote 1; la tabla completa está en la referencia de diseño («Lote 2»):

| Maqueta | Medido | Implementado |
|---|---|---|
| Brillo naranja al 50 % y dorado al 28 % | «Tu récord» sobre una tarjeta que pasa por el dorado, 3,70:1; el secundario, 3,44:1 con el naranja | naranja al 26 % y dorado al 18 % |
| Dorado sobre la fila dorada | `#E8B84B` da 3,88:1 sobre el tinte sin ningún brillo detrás; 2,83:1 en el peor punto | el tinte sobre un surco negro al 25 % y el texto en `#F0DDA8`; en claro, sin surco y en `#6B4F12` |
| Número y kilos de la serie en curso en `#FFB27A` | 3,95:1 sobre la lente | `#FFC9A3` (4,69:1) |
| Kilos y repeticiones sin caja | son campos | un surco con borde a 3:1 |

**El récord en dorado, medido** (peor punto, oscuro · claro): «Tu récord: …» 4,60 · 4,53:1; «¡Récord!» y su número en la fila dorada 4,80 · 4,76:1. En captura, el revisor midió 7,76 · 5,82 y 7,13 · 5,52.

**El título y la lista.** La cabecera es la de la maqueta: dos piezas de cristal vivo y el título suelto. Suelto, con la lista pasando por detrás, no se leería: la lista se desvanece antes (el borde de arriba es macizo hasta donde empieza el resumen), como el contenido de Inicio bajo la barra de estado. Detrás del resumen y del descanso sí pasa, desenfocada.

## Tests

**Nuevos, todos vistos en rojo:**

| Test | Rojo |
|---|---|
| `SesionEnVivoBotonesTest` (unitario) | `principales del descanso expected:<[btnDescansoSaltar]> but was:<[btnDescansoMenos, btnDescansoMas, btnDescansoSaltar]>` y `…fuera de la tarjeta del guardado… but was:<[btnTerminar]>` |
| `FilaSerieRotulosTest` (instrumentado; sin `@Ignore`, con 1,5 y la Sesión en vivo) | `item_apuntar_ejercicio, es a 1.5: «SERIE» mide 131.0 px y su columna es de 126`; a 2,0, 163 px. Y con el tamaño común: `«Anterior» va a otro tamaño que el primero expected:<52.0> but was:<54.0>` |
| `ContrasteCristalTest`, dos tests de la sesión (unitario) | con los valores de la maqueta: 23 pares por debajo del mínimo (p. ej. `récord en dorado 2.13:1`, `serie en curso, acento 2.68:1`) |
| `CristalPintadoTest` (instrumentado) | `oscuro, bg_cristal: rojo 15, hay algo oscuro debajo del cristal`: un fallo mío (ver «Lo que salió mal») |
| `DescansoSinCorteTest` (instrumentado) | con los botones a 18 sp y 24 dp de relleno: `es a 1.5, «−15 s»: su línea no lleva el texto entero` |
| `FilaSerieSinPartirTest` (instrumentado) | `es a 1.3, récord: «¡Récord!» se reparte en varias líneas` y `es a 1.3: «1022.5» se parte en varias líneas` |
| `SesionEnVivoCristalTest` (unitario) | sin `stateHidden`: `el teclado no sale solo al abrir la pantalla: adjustResize` |

**Tocados porque leen código que se ha movido** (misma intención): `CimientosTest`, `BordesSistemaTest` y `CristalTest`.

**Batería.**
- Unitarios: 1.444; fallan solo los 4 `*WebTest` locales de siempre (por `web/dist` y `web/node_modules` sin versionar).
- Instrumentados: **51 de 51 en API 36, API 32 y API 24** (los 40 de antes y 11 nuevos: 2 casos más de `FilaSerieRotulosTest`, 2 de `CristalPintadoTest`, 4 de `DescansoSinCorteTest` y 3 de `FilaSerieSinPartirTest`).
- Lint sin errores. CI en verde (`android-build-test`, `api-build-test`, `app-release-r8`) en `5fb9a1c0`; los commits siguientes, ver la PR.

## Revisor de UI

**Primera pasada (API 36): 2 altos, 3 medios y 4 bajos**, casi todos solo con letra 2,0. Arreglados en `3cabf206` y `d38e8754`:

| | Qué | Arreglo |
|---|---|---|
| Alto | «¡Récord!» partido a media palabra | `TextoSinPartir` |
| Alto | los campos cortaban «12–15» en «12–1» y partían «102.5» | `CampoQueEncoge` |
| Medio | texto de la lista en el hueco entre el título y el resumen | el borde de arriba, macizo hasta el resumen |
| Medio | texto de la lista en los 22 dp bajo el descanso | el borde de abajo, macizo hasta el pie del panel |
| Medio | TalkBack no decía cuál era la serie del récord ni la siguiente | «Serie 1, récord», «Serie 2, la siguiente» |
| Bajo | el resumen era una parada de foco sin nombre | ya no lo es |
| Bajo | la marca del aviso repartida en dos líneas | espacios duros |
| Bajo | el aro, a 2 dp del campo | 6 dp |
| Bajo | los kilos propuestos de la serie en curso, sin acento | en el acento |

**Segunda pasada (API 36, las mismas seis combinaciones y estados): sin altos ni medios.** Los nueve de arriba, arreglados y medidos uno por uno. El emulador se cayó a media pasada (murió su `system_server`, con el reloj del invitado en bucle: del emulador, no de la app); se reinició y la pasada se terminó en el mismo sitio. Quedaron dos bajos:

- **Nuevo, arreglado en `bcdd6365`:** con «Quitar animaciones», al abrirse el descanso el borde de abajo llegaba un fotograma tarde y durante 18 ms se leía la fila de debajo del panel. Comprobado a mano después del arreglo: vídeo en API 36 con las animaciones a 0 (claro, EN, letra 1,3), 19 fotogramas; en los 5 en que ya está el panel, la franja de debajo (2218–2270 px) es la misma desde el primero (desviación de luminancia 17,1 en los cinco, que es el degradado del fondo; antes del panel, 15,8 con la lista), y en el primer fotograma con panel no hay texto.
- **De decisión, sin tocar:** el contorno de «−15 s» y «+15 s» contra el panel da 1,45–1,57:1 en claro y 1,8–2,3:1 en oscuro. Es el `BotonCristal` de DEC-070, con el valor que ya tiene documentado sobre una tarjeta (1,48:1); su texto pasa de sobra (10,4:1 o más). Ver «Dudas».

Sin comprobar por el revisor: TalkBack real, de oído (nombres, estados y paradas se miraron por nodo); la animación del récord (momento 25), que exige desmarcar la serie del récord; y los niveles 2 y 3, que he mirado yo en las capturas de API 32 y 24.

## Fotogramas lentos

**Sin medir.** La condición primera es el portátil enchufado, y no se cumplía:

- Al empezar la sesión (27-11, 23:20): batería, 47 % y descargando.
- A media mañana del 28-11 estuvo enchufado un rato (de 39 % a 100 %), mientras yo aún estaba implementando y el revisor de UI trabajaba en el emulador: no era momento de medir (otra carga en el equipo).
- Al terminar el revisor y las capturas (28-11, hacia las 13:00): otra vez con batería (`PowerOnline=False`, 85 % y bajando, plan «Equilibrado»).

Lo demás que pedía comprobar el encargo:

- **Otra sesión de Claude:** la app de escritorio de Claude está abierta desde el 26-11, y cuelgan de otra sesión un `lighthouse` (`node`, de `un-dia-fase-4`) con su Chrome sin ventana (9 procesos), arrancados el 26-11 a las 10:31. Medidos 10 s: 0 % de CPU. No los he tocado: no son míos.
- **`python -` colgado:** ninguno (los dos que lancé por error los paré en el momento).
- **`java.exe`:** el PID 42816 ya no existe. Los que hay son míos: la API local (3972, con su `mvnw` 14832), un daemon de Gradle (10044) y el servidor de lenguaje Java de esta sesión de Claude Code (8472, `jdtls`). Ninguno es de Android Studio. No he cerrado ninguno.

**Queda listo para medir** en cuanto el portátil esté enchufado y el equipo quieto:

- `main` (`76abc4f1`) compilado desde cero en un worktree aparte; la rama se compila igual desde su último commit.
- El guion (`medir.py`, en el scratchpad de la sesión; el protocolo es el de los lotes 1 y 1c): por emulador recién arrancado con `-gpu host` y 3 GB, tres rondas (una de calentamiento y dos que cuentan) con las dos variantes intercaladas; en cada una, instalar, `cmd package compile -m speed -f`, y el guion fijo de deslizamientos (cuatro veces tres hacia arriba y tres hacia abajo, del 71 % al 29 % del alto, 300 ms y 450 ms de pausa) en Inicio y en la Sesión en vivo, con `dumpsys gfxinfo`. En Android 8.1 o menos cierra el teclado que `main` saca al abrir la sesión, para medir lo mismo en las dos.
- La tanda vale solo si `main` sale estable en sus dos rondas. Presupuesto: 2 % o menos en las dos pantallas, en API 36, 32 y 24.

**Sigue pendiente también la medida de Inicio de los botones** (PR #148, fusión `76abc4f1`), que este lote tenía que hacer.

## Capturas

En `Desktop\GymProFit\2026-11-28-gp285-lote-2\`, una carpeta por API (`API36`, `API32`, `API24`). Nombre: `<tema>_<idioma>_letra<1.0|2.0>_<estado>.png`.

- Estados: `serie-en-curso-y-record` (la lista arriba: la fila dorada y la serie con la lente, como en la maqueta), `descanso` (lo mismo con el panel abierto) y `scroll` (la lista bajo la cabecera y el resumen).
- API 36 (24): claro y oscuro, letra 1,0 y 2,0, ES y EN. API 32 (12) y API 24 (12): lo mismo, en ES. Todas, con el último build.
- API 36, además, en `API36\pulsado-y-sin-animaciones\` (7): el descanso con «Saltar» pulsado y con «Quitar animaciones». **Estas son las que tomó el revisor de UI en su segunda pasada** (letra 1,3; oscuro en ES y claro en EN; build `b9ff00e3`, que para esto es igual que el último): las mías, a letra 1,0 en los dos temas, no llegaron a hacerse. Cuando estaban en cola, el equipo se quedó sin memoria y Claude Code paró los dos emuladores y la API local; no los he vuelto a arrancar por mi cuenta.

Cuentas locales (`prueba` en API 36, `dani.entrena` en API 32 y 24); la API, la local.

En las capturas de «descanso» en inglés de API 36 la sesión va una serie más adelantada («3 of 13» y «Next: Dumbbell Bench Press»): el descanso se abre marcando la serie en curso, y esa tanda fue la segunda.

## Lo que salió mal

- **Un pincel que nacía negro.** Al añadir el surco de la fila dorada a `Cristal`, su pincel nacía negro opaco y se pintaba debajo de **todos** los cristales, también los de Inicio. En oscuro apenas se notaba; lo vi al abrir el tema claro. No llegó a ningún commit. `CristalPintadoTest` pinta cada cristal sobre rojo puro y lo vigila desde ahora.
- **`python -` con la entrada vacía, dos veces.** Es justo lo que la nota de memoria avisa (se queda en bucle gastando un núcleo). Las dos veces lo paré en el momento y comprobé que no quedaba ningún `python` vivo.
- **Un cierre de `zygote64` en el emulador de Android 7**, una vez, al crear el proceso de la app durante las capturas (fallo nativo en `nativeForkAndSpecialize`, antes de que corra código de la app). No se repitió en las pasadas siguientes.

## Decisiones tomadas

1. **«Saltar» es el principal del descanso** (arriba).
2. **El título va suelto y la lista se desvanece antes de él**, en vez de meterlo en una cápsula de cristal: es lo que dibuja la maqueta.
3. **La tarjeta de cada ejercicio la pinta la lista por detrás** (una decoración), para no tocar cómo se quita una serie.
4. **El aro del descanso pasa a ser la barra de la maqueta.**
5. **«¡Récord!» sustituye a la marca anterior en la fila dorada**, como en la maqueta; la marca de antes sigue en el aviso de encima y en lo que lee TalkBack.
6. **El botón de una serie sin hacer es un aro vacío** (antes llevaba un check tenue dentro), como en la maqueta.
7. **`stateHidden`**: en Android 8.1 o menos la pantalla sacaba el teclado al abrirse. Ya pasaba en `main` (comprobado con su APK en API 24). No es del rediseño, pero salía en todas las capturas de Android 7.
8. **GP-286 también en Apuntar entreno**, que es la pantalla que miraba el test: solo sus cuatro rótulos.

## Dudas

1. **Las hojas inferiores de la pantalla** (terminar sesión, cronómetro, permiso de alarmas) y el menú ⋮ siguen opacas. No estaban en el encargo; van con el lote de las hojas.
2. **El tema claro de la sesión no tiene maqueta**: el fondo (naranja 55 %, dorado 45 %, azul 36 %) es mío, por analogía con el de Inicio.
3. **En el nivel 3 (Android 11 o menos) el resumen y el descanso son opacos**, como la barra de Inicio: ahí «la lista pasa por detrás» no se ve.
4. **El nombre de un ejercicio en dos líneas dejaba una línea en blanco entre ellas** (el estilo `Display` trae el interlineado de su tamaño grande). Arreglado aquí; el mismo estilo se usa igual en otras pantallas (Apuntar entreno), sin tocar.
5. **La carpeta `Desktop\GymProFit\2026-11-27-gp285-botones\` no se ha podido borrar**: el modo automático denegó el borrado. Son 31 capturas, sin APK. El `CLAUDE.md` no pide copiarlas a Drive (eso es para lo publicado).

6. **El contorno de «−15 s» y «+15 s»** sobre el panel del descanso (1,45–1,57:1 en claro): es el del botón de cristal de DEC-070. Si a un botón secundario se le pide el 3:1 de un control, hay que decidirlo para todos (también «Cambiar» en Inicio), no aquí.
7. **Dos piezas de la cabecera son cristal vivo sin nada que desenfocar**: minimizar y «Terminar» flotan sobre la franja donde la lista ya se ha desvanecido. Se ven como en la maqueta y responden a los tres niveles, pero el desenfoque ahí no tiene contenido detrás. La alternativa (toda la cabecera en una cápsula de cristal con la lista pasando por detrás) se aparta de la maqueta.

## Pendiente

1. **Revisar las capturas** y decidir la fusión (la PR #149 está en borrador a propósito).
2. **Medir los fotogramas lentos** (`main` → rama, API 36, 32 y 24, Sesión en vivo e Inicio) con el portátil enchufado y el equipo quieto.
3. **Borrar a mano** la carpeta de las capturas de los botones del 27-11 (duda 5): no se me dejó.
4. Las dudas de arriba.
