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
- **Remate (28-11, tarde), tras revisar el propietario las capturas**: cabecera con letra grande, campos de cristal en claro, la lista se desvanece antes del canto del resumen y del descanso, y minimizar y «Terminar» en cristal estático. Tercera pasada del revisor, sin altos ni medios. **Los fotogramas siguen sin medir** (otra vez con batería). Todo en la sección «Remate», al final.

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

---

## Remate (28-11-2026, tarde)

Lo que pidió el propietario tras revisar las capturas del lote. La PR #149 sigue **en borrador y sin fusionar**: antes se revisan las capturas nuevas.

### Commits

| Hash | Qué |
|---|---|
| `1fc571fa` | las cuatro correcciones de la Sesión en vivo, con sus tests y DESIGN.md |
| `bdef9a0f` | la regla de `python -` en `CLAUDE.md` |

Después va el commit de documentación que trae esta sección (DEC-070, la referencia de diseño y el CHANGELOG).

### Qué se ha hecho

1. **Cabecera con letra grande.** A letra 2,0, «Cuerpo completo C» y su programa «Cuerpo completo» ocupaban cuatro líneas. Ahora (`ui/widget/TituloSesion`, la regla de GP-288): el título, como mucho en dos líneas y sin partir palabras; desde letra 1,5 el programa solo se ve si no repite el título y cabe entero en una línea. TalkBack lo lee siempre: el grupo es una sola parada, «Cuerpo completo C, Cuerpo completo». «Terminar» no se corta: cede el título. Medido por el revisor en API 36: a letra 2,0 la fila pasa a 71 dp con el título en dos líneas y «Terminar» entero (147 × 49 dp). En API 24 a letra 2,0, «Torso A» queda en una línea (antes, tres con su programa).
   - **Salió al probarlo**: en un móvil de 360 dp a letra 2,0 el título se quedaba en «Cuerpo…» (tres palabras no caben en dos líneas si solo encoge hasta que cabe la palabra más ancha). El título encoge ahora lo justo para caber entero en sus dos líneas, nunca por debajo de su tamaño a letra 1,0, antes de recortar (`TextoSinPartir.encogerParaCaber`, solo para este título).
2. **Campos de kg y reps en claro.** Eran un surco (tinta al 10 %) que sobre la tarjeta clara se leía como una caja gris opaca. Son cristal estático con el relleno de las tarjetas (blanco 62 % → 34 % → 48 %) y su canto de tinta (`Cristal`, tipos `campo` y `campo_foco`). Lo escrito, 13,85:1 en el peor punto (16,94:1 en captura); la pista, 6,46:1 (7,90:1); el canto, 3,10:1 contra la tarjeta y 3,22:1 contra lo de dentro. La serie en curso y el foco, como antes: lente, acento y canto de 2 dp (6,56:1 en captura). **El oscuro no cambia**: los tokens nuevos apuntan allí al surco de siempre; comparado con la captura anterior, el relleno es el mismo (`#111111`) y el canto difiere 1–7 niveles por el suavizado.
3. **Scroll bajo el resumen.** Un velo (`ui/cristal/CantoVivo`, pintado por `BordeSistema` con el propio fondo ambiental) desvanece la lista antes del canto: opaco desde un cuarto de radio por dentro del cristal hasta 2 dp por fuera, y una rampa de 22 dp hacia la lista con una curva suave. Por dentro del cristal la lista sigue pasando desenfocada. **Igual en los tres niveles** (en el 3 el cristal es opaco y el velo es lo único que suaviza el corte); comprobado en captura en API 36, 32 y 24. Medido por el revisor en API 36, en 10–12 posiciones por combinación: lo que pasa queda a 1,07:1 o menos hasta 14 px bajo el canto y no llega a 4,5:1 hasta 35–40 px; en reposo la primera tarjeta, a 24 dp, no queda desvanecida; sin raya ni banda en los márgenes (salto máximo entre píxeles vecinos, 6 de 765).
   - Dos vueltas antes de darlo por bueno: un velo que acababa en seco por dentro cortaba en una raya la sombra de la tarjeta en los márgenes de los lados (ahora también entra poco a poco por dentro); y con rampa de 14 dp y la curva de los bordes del sistema el texto llegaba al canto a medio ver (ahora 22 dp y curva suave, y la lista deja 8 dp más bajo el resumen para que en reposo no toque la primera tarjeta).
   - **También sobre el descanso**, que es el mismo caso por arriba: la lista se desvanece antes del canto de arriba del panel.
4. **Minimizar y «Terminar», de cristal vivo a cristal estático** (`BotonCristal`): bajo ellos no pasa nada. Son botones de verdad (ya no vistas a las que se les decía a TalkBack que lo eran) y se hunden a 0,94 sin cambiar de forma (medido: «Terminar» 261 → 247 px). Texto de «Terminar» 4,81 · 9,18:1 en el peor punto (7,32 · 10,04 en captura). La regla, en DEC-070: **un elemento flotante bajo el que nunca pasa contenido es cristal estático**.
5. **`CLAUDE.md`**: nunca `python -` con la entrada vacía. Al acabar no queda ningún `python` vivo (comprobado a las 17:43).

### Tests

**Nuevos, todos vistos en rojo:**

| Test | Rojo |
|---|---|
| `CabeceraSesionTest` (instrumentado, 5 casos) | con la regla quitada: `es a 2.0 en 411 dp, «Cuerpo completo C» / «Cuerpo completo»: el programa a la vista expected:<false> but was:<true>` (y a 1,5, y con un programa que no cabe). Y antes de que el título encogiera: `es a 2.0 en 360 dp …: el título va recortado expected:<Cuerpo[ completo C]> but was:<Cuerpo[…]>` |
| `TituloSesionTest` (unitario, 4) | con la cuenta quitada: `repite el título` y `el_programa_repite_el_titulo_si_sus_palabras_ya_estan_en_el` |
| `CantoVivoTest` (unitario, 5) | `expected:<448> but was:<480>`; sin la entrada por dentro, `tres cuartos de radio de entrada expected:<47> but was:<0>`; con la curva de antes, `a media rampa, medio velo: 60`; sin el sitio de la lista, `lo que falta expected:<21> but was:<0>` |
| `SesionEnVivoCristalTest`, 2 casos nuevos | `btnMinimizar es cristal estático (BotonCristal), no …CristalVivo` y `no existe veloResumen` |
| `ContrasteCristalTest`, 1 caso nuevo y 5 pares más | `no existe el color gp_cristal_campo_alto en el tema claro` (y en el oscuro) |

**Batería.**
- Unitarios: 1.456; fallan solo los 4 `*WebTest` locales de siempre.
- Instrumentados: **56 de 56 en API 36, API 32 y API 24**, con el código final (los 51 de antes y los 5 de `CabeceraSesionTest`).
- Lint sin errores. **CI en verde en `bdef9a0f`** (`android-build-test`, `api-build-test`, `app-release-r8`, `secretos`); el commit de documentación, ver la PR.

### Revisor de UI (tercera pasada de la pantalla)

API 36, las seis combinaciones de siempre y cuatro estados en cada una. **Sin altos ni medios.** Los cuatro puntos del remate, comprobados con medida (arriba). Tres bajos, sin tocar (a «Dudas»):

- El canto de «Terminar», en claro, sobre el brillo naranja: 1,25–1,29:1 contra el fondo (minimizar, 1,46:1). Es el canto del botón secundario de DEC-070, la misma duda 6 de arriba; su texto pasa de sobra.
- El canto del campo de kg en la fila dorada, en claro: 2,95–3,03:1 contra el dorado de fuera (3,30:1 contra lo de dentro), medido en una fila de píxeles. El color del canto no lo ha tocado el remate.
- A letra 1,5 el título queda «Cuerpo completo / C», con la «C» sola. Cumple la regla.

Y una observación que ya estaba en el lote: las filas que quedan bajo la cabecera siguen en el árbol de accesibilidad (sin TalkBack real no se sabe si el foco puede caer en una fila tapada).

Sin comprobar por el revisor: los niveles 2 y 3 (los he mirado yo en captura en API 32 y 24), TalkBack real, el aviso del fin del descanso con su velo, y el velo con el estado del guardado como último cristal.

### Fotogramas lentos: SIN MEDIR, otra vez

No se cumplió la primera condición cuando tocaba medir. La cronología:

| Hora (28-11) | Estado |
|---|---|
| 14:00, al empezar | con batería (`BatteryStatus` 1, 77 %); un lighthouse y su Chrome de otra sesión, vivos desde el 26-11; ningún `python -` |
| 15:30 | enchufado (`BatteryStatus` 2, 61 %); los restos de la otra sesión, ahí. Pregunté al propietario y autorizó cerrarlos (abajo) |
| 17:29, tras unas dos horas sin actividad de esta sesión | **otra vez con batería** (`BatteryStatus` 1, 85 %) |
| 17:43, con el código final, los instrumentados y las capturas hechos | con batería, 79 % y bajando. **No se mide** |

`main` (`76abc4f1`) sigue compilado desde cero de la sesión anterior y el guion es el mismo (`medir.py`): en cuanto el portátil esté enchufado y nadie lo use (unos 90 minutos para los tres emuladores), se mide `main` → rama en API 36, 32 y 24, en la Sesión en vivo y en Inicio. Arrastra también la medida de Inicio de los botones (`76abc4f1`).

### Procesos de otra sesión que cerré, y por qué

Para medir, el encargo pide que no haya restos de otra sesión de Claude (lighthouse, Chrome de pruebas). Los había: una pasada de lighthouse con su Chrome sin ventana, arrancada el 26-11 a las 10:31 por otra sesión (rama `un-dia-fase-4`) y colgada desde entonces (16 ms de CPU en 10 s, 83 MB). **No eran míos: pregunté antes de tocarlos y el propietario respondió «Mátalos y mide».** Los cerré el 28-11 a las 15:34 con `Stop-Process -Force`, comprobando antes que cada PID era de antes del 27-11 y que era de lighthouse o hijo de su Chrome (14 de 14):

| PID | Proceso | Qué era |
|---|---|---|
| 35680, 19188 | `bash.exe` | `npx -y lighthouse@13.6.0 http://127.0.0.1:8302/beta …` |
| 22792 | `node.exe` | `npx-cli.js` de esa orden |
| 21288 | `cmd.exe` | `lighthouse http://127.0.0.1:8302/beta --quiet --output=json …` |
| 12592 | `node.exe` | `lighthouse/cli/index.js` |
| 33512 | `chrome.exe` | el Chrome sin ventana que lanzó lighthouse |
| 32460, 33156, 15156, 22452, 31424, 35508, 17824, 3520 | `chrome.exe` | sus hijos (crashpad, GPU, red, almacenamiento, trazas y tres de página) |

**Lo que pasó después, que no esperaba.** Esos procesos no estaban huérfanos del todo: colgaban de un guion de aquella sesión, `lh.sh` (PID 2140, `sh.exe`, vivo desde el 26-11 a las 10:23, en el scratchpad de la otra sesión), que es un bucle de 80 pasadas de lighthouse (8 páginas × 2 versiones × 5) y llevaba dos días parado en la que cerré. Al cerrarla, el bucle siguió: lanzó las pasadas que le quedaban entre las 15:34 y las 17:30. **No lo toqué** (la autorización era para los 14 de arriba). Lo leí para saber qué hacía: salta los resultados que ya existen, sus dos servidores locales (8301 y 8302) ya no escuchaban y su carpeta de salida (`Desktop\un-dia-fase-4\lighthouse`) ya no existe, así que cada pasada falló sin escribir nada, y el guion **acabó solo** poco después de las 17:30 (a las 17:29 iba por la sexta página de ocho; en la comprobación siguiente ya no estaba). A las 17:43 no quedaba ningún proceso suyo, ningún Chrome y ningún `python`. No ha tocado el repositorio (rama y ficheros, comprobados). Lo que sí: si aquella sesión esperaba esos resultados, no los tiene.

### Capturas

En `Desktop\GymProFit\2026-11-28-gp285-lote-2\remate\`, una carpeta por API. Nombre: `<tema>_es_letra<1.0|2.0>_<estado>.png`.

- Estados: `cabecera` (la lista arriba del todo), `serie-en-curso`, `scroll-bajo-el-resumen` y `descanso`.
- `API36` (16): claro y oscuro, letra 1,0 y 2,0, en español.
- `API24` (4): claro a letra 1,0, con los campos (y los otros tres estados).
- `API32` (4), de más: claro a letra 1,0, para ver el velo en el nivel 2.

Con el build final (`1fc571fa`). Cuentas locales y API local. En las de API 36 la sesión va por la serie 4 o 5 de 13 (para abrir el descanso se marca una serie).

### Decisiones tomadas

1. **El velo, y no un desenfoque progresivo**: desvanecer hacia el fondo no cuesta nada por fotograma y vale igual en los tres niveles; un segundo desenfoque junto al canto habría sido un efecto nuevo de la GPU sin poder medirlo hoy.
2. **El velo también sobre el descanso**, aunque la captura señalada era la del resumen: la regla es de todo cristal vivo, y el corte se veía igual en el canto de arriba del panel.
3. **La lista deja 8 dp más bajo el resumen** (24 en vez de 16 hasta la primera tarjeta): es lo que necesita una rampa que desvanezca de verdad sin tocar la tarjeta en reposo.
4. **«No cabe» es no caber en una línea**: desde letra 1,5 el programa ocupa una línea o ninguna. Y «repite el título» es que sus palabras ya estén, seguidas, en él (sin contar mayúsculas ni tildes).
5. **TalkBack lee rutina y programa en una sola parada**, a cualquier tamaño de letra (antes eran dos).
6. **«Terminar» conserva su color** (`gp_vivo_accion`: el acento en oscuro, como en la maqueta; tinta en claro), ahora medido sobre cristal estático.
7. **El título encoge para caber entero** antes de recortar; solo este título, no los demás `TextoSinPartir`.

### Dudas

1. **El canto del botón secundario** («Terminar» y minimizar en claro sobre el brillo naranja, 1,25–1,46:1): es la duda 6 de arriba, ahora con dos botones más. Cuando eran cristal vivo llevaban sombra y se despegaban más del fondo.
2. **El canto del campo sobre la fila dorada en claro**, 2,95:1 por fuera en captura: el token (`gp_cristal_barra_tenue`) es el de los aros y las barras apagadas; oscurecerlo es decisión de sistema, no de esta pantalla.
3. **La «C» sola en la segunda línea a letra 1,5.**
4. **Las filas bajo la cabecera en el árbol de accesibilidad**: pide una prueba con TalkBack real.
5. **En Android 7 a letra 2,0 la lista se abre algo desplazada** (el sistema da el foco al primer campo y lo trae a la vista). Ya pasaba en el lote; no lo he tocado.

### Pendiente

1. **Revisar las capturas del remate** y decidir la fusión.
2. **Medir los fotogramas** (lo de arriba).
3. Las dudas.
