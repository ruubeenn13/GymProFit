# Apuntar un entreno y el objetivo del Perfil (GP-241, GP-242 y GP-250)

El propietario aprobó estos diseños el 6 de noviembre de 2026 en dos lienzos de claude.ai que no están en el repositorio: la opción B de «Apuntar un entreno hecho» y la 1 de «Perfil: cómo se ve tu objetivo» (lienzo «Entreno hecho y objetivo»), y la hoja para elegir la rutina al apuntar un entreno (lienzo «Rutinas: tiempo, crear y elegir»). Sus tableros están en `fuente/`, tal como salieron del lienzo: son `.dc.html` para leer como código. Manda esta especificación; los tableros, para medidas, colores y textos exactos. Solo están en oscuro y en español: el claro y el inglés salen de `app/GymProFit/DESIGN.md`. Lo que dibujan y la API no tiene no se inventa: va a «Huecos».

## Tableros

| Tablero | Qué enseña |
|---|---|
| `Entreno-B.dc.html` | «Apuntar entreno», la ficha del entreno |
| `Elegir.dc.html` | La hoja «¿Qué entrenaste?» |
| `Objetivo-1.dc.html` | El Perfil con la tarjeta del objetivo |

## Pantallas

| Superficie | Pieza de hoy |
|---|---|
| «Apuntar entreno», la del «+» | `activity_registrar_sesion.xml`, `RegistrarSesionActivity`, `RegistrarSesionViewModel` |
| «¿Qué entrenaste?» | Nueva, sobre `ElegirRutinaHoja` |
| «¿Qué rutina hoy?» | `ElegirRutinaHoja` (GP-232) |
| Perfil | `PerfilActivity` (`cardTusDatos`, `grupoDatos`, `bloquePlan`) |
| Resumen del entreno | `ResumenSesionActivity`, `ComparacionEntreno` |

## Especificación

1. **La cabecera de «Apuntar entreno»** («Log a workout»; fuera «Entrenamiento hecho»), en tarjeta protagonista (`gp_hero`):
   - la fecha en una píldora: Hoy de entrada; al tocarla, Hoy, Ayer u Otro día, con el calendario y sin días futuros;
   - la rutina en grande con «Cambiar», que abre la hoja del punto 5. Se abre con la que elige `HoyToca` y, debajo, por qué: «La que te toca en tu programa» o, sin programa, «La que más tiempo llevas sin hacer». Sin rutinas, «Entreno libre»;
   - tres cuadros que suman al marcar: la duración (de entrada, la de la rutina; libre, la del último entreno, o 45 min si no hay; se cambia con − y + de 5 en 5, de 5 a 300, sin teclado), los kilos de lo marcado y las series («5 de 21»: las marcadas de las previstas; libre, solo las marcadas). Son contadores y empiezan en 0. Una rutina sin ejercicios con peso, ni en la última vez ni en lo marcado, no lleva el cuadro de kilos, y los otros dos se reparten el ancho;
   - debajo, la última vez de esa rutina, en cascada (DEC-060): «La última vez moviste 8.410 kg.» o «La última vez hiciste 12 series.»; la primera vez, nada;
   - el récord en dorado en cuanto una serie marcada lo bate («Récord en press de banca: 72,5 kg × 8»; con más de uno, «2 récords: press de banca y remo con barra»), contado como en la sesión en vivo.
2. **Los ejercicios.** «Ejercicios» con «Todo como la última vez» («Same as last time»), que marca de una vez todas las series con los valores de la última vez; sin última vez, no sale. Cada ejercicio, con su pauta («3 series de 6 a 10 repeticiones») y las filas de la sesión en vivo (`item_envivo_serie`: serie, anterior, kg, rep. y marcar), con la última vez de pista (GP-014) y editables; la marcada que bate un récord, en dorado y con el trofeo. Los que no se han empezado, plegados, con «Sin empezar». Un entreno libre añade ejercicios del catálogo con `AnadirEjerciciosActivity` (`POST /sesiones/completa` los admite sin rutina).
3. **«¿Qué tal fue?»**: cinco caras con nombre, Fatal, Flojo, Normal, Bien y Genial (Awful, Poor, Okay, Good, Great), que valen de 1 a 5; sin tocar, NULL (GP-077). La marcada dice su nombre junto al título. La nota, plegada tras el botón del lápiz («Añadir una nota»).
4. **El pie fijo**, con lo que se va a guardar en una frase y «Guardar entreno» («Save workout»): «5 series apuntadas y 2.740 kg. Lo que no marques no se guarda.»; sin kilos, «12 series apuntadas. Lo que no marques no se guarda.»; sin nada marcado, «Sin series apuntadas: se guardan el día y la duración.». Se guarda solo lo marcado. Se mantienen `RegistrarSesionViewModel` con `SavedStateHandle`, la clave de idempotencia (GP-016 y GP-006) y `POST /sesiones/completa`, y la pantalla no enseña una cosa para guardar otra. Salir con algo marcado avisa (GP-098). Fuera `spRutina`, el `RatingBar`, la duración obligatoria y el hueco antes de guardar.
5. **La hoja «¿Qué entrenaste?»**: la de «¿Qué rutina hoy?» (`ElegirRutinaHoja`) con los títulos «De tu programa, <nombre del programa>» y «Tus rutinas», y «Entreno libre» («Sin rutina: añades tú los ejercicios») al final, tras una raya. Cada rutina, con sus zonas, «La última vez, …», sus barras pequeñas si la última vez tuvo kilos y el check en la elegida; para TalkBack, botones de opción. Se elige con un toque y se cierra; cambiar de rutina con series marcadas pregunta antes.
6. **Un solo orden para las dos hojas**, en una función pura: la que toca hoy; el resto del ciclo del programa, en el orden en que viene tras la de hoy; las tuyas, en el orden de «Mis rutinas». «¿Qué rutina hoy?» sigue sin títulos, como su tablero, y su punto 6 en `../2026-11-04-tarjetas/README.md` se corrige con este orden.
7. **El Perfil.** Tras la foto y el nombre, la tarjeta del objetivo: el icono del objetivo de TusDatos en su casilla, «Tu objetivo», el nombre en grande y «Cambiar», que abre TusDatos en el objetivo; al volver, el plan sale recalculado. Debajo, la frase de lo que supone, una raya, las kcal en grande («2.150 kcal al día») y la barra del reparto con los gramos de proteína, hidratos y grasa, en los colores de los macros de Nutrición y con los gramos de `ResultadoNutricional`, sin peticiones nuevas. Después, «Tus datos» en rejilla (peso, altura y edad; nivel y actividad) con «Editar». Fuera las píldoras de `grupoDatos` y `bg_chip_dato_destacado`; `bloquePlan` pasa a la tarjeta. La rejilla de objetivos de TusDatos (`gridObjetivo`) no cambia. Las frases, por objetivo de `CalculadoraNutricional`:
   - Perder grasa: «Comes un poco menos de lo que gastas y mucha proteína, para que lo que bajes sea grasa y no músculo.» («You eat a little less than you burn, with plenty of protein, so what you lose is fat, not muscle.») Con 17 años o menos el plan se queda en mantenimiento, y en vez de la frase va la nota de menores (`alta_plan_nota_menores`).
   - Ganar músculo: «Comes algo más de lo que gastas y mucha proteína, para que lo que subas sea sobre todo músculo.» («You eat a little more than you burn, with plenty of protein, so what you gain is mostly muscle.»)
   - Ganar fuerza: «Comes algo más de lo que gastas, con la proteína y la energía para entrenar pesado.» («You eat a little more than you burn, with the protein and energy to lift heavy.»)
   - Mantenerme: «Comes lo que gastas, con la proteína que necesitas para conservar tu músculo.» («You eat what you burn, with the protein you need to keep your muscle.»)

   TalkBack lee la tarjeta en frases: «Tu objetivo, perder grasa. Comes un poco menos… 2.150 kcal al día: 160 gramos de proteína, 210 de hidratos y 74 de grasa.»
8. **El resumen del entreno a 0 kg (GP-250).** Hoy dice «−100 % que la última vez»: `ComparacionEntreno.variacionDeSesion` compara 0 kg con la anterior. En cascada, como DEC-060: con kilos, como ahora; con 0 kg y series, la cifra grande son las series («12 series»), contadas como los kilos, sin la ficha de comparación ni la de series; sin series, el aviso de la cascada («Sin ejercicios apuntados» o «Sin series hechas») en el sitio de `tvSinKilos`. Va a DEC-060.
9. **En todas estas pantallas**: todo en frase y sin «·», en ES y EN; claro y oscuro; letra 1,3 sin cortes; contrastes medidos (4,5:1 en texto y 3:1 en gráficos); TalkBack con nombre en cada control, y la fecha y las caras como botones de opción; el movimiento con los tokens de DESIGN.md (las cifras cuentan con `Movimiento.contar`) y quieto con «Quitar animaciones». Ningún color nuevo sin pasar por DESIGN.md.

## Huecos

- «@dani.entrena, desde septiembre»: si la API no da la fecha de alta, solo el usuario.
