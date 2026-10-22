# Glosario del catálogo de ejercicios (GP-217)

Criterio para escribir los nombres y las descripciones en español de los 873 ejercicios del catálogo (`api/gymprofit-api/src/main/resources/catalogo/ejercicios-es.csv`). El español es el de un gimnasio de España: se queda en inglés lo que allí se dice en inglés y se traduce lo demás. **Ninguna traducción es automática**: está escrita a mano, ejercicio a ejercicio, contra el texto en inglés de la base de datos (`nombre_en`, `descripcion_en`), que no se toca.

## 1. Se quedan en inglés

Lo que un monitor dice así en un gimnasio español. Dentro de un nombre en español van tal cual, sin cursiva ni comillas.

| Término | Ejemplo |
|---|---|
| hip thrust | Hip thrust con barra |
| face pull | Face pull en polea |
| curl | Curl de bíceps con barra; curl martillo; curl predicador |
| press | Press de banca; press militar; press Arnold |
| pullover | Pullover con mancuerna |
| kettlebell | Swing con kettlebell; sentadilla goblet con kettlebell |
| crunch | Crunch en polea; crunch inverso |
| goblet | Sentadilla goblet |
| good morning | Good morning con barra |
| burpee, jumping jack | tal cual |
| Smith (máquina Smith) | Press de banca en máquina Smith |
| landmine | Press landmine |
| bosu, TRX, sled, prowler | tal cual (el material se llama así) |
| clean, snatch, jerk | **se traducen** con el término de halterofilia (ver 2) salvo en `power clean`, que sale como «cargada de potencia» |

## 2. Traducciones fijas

### Material (va al final del nombre: «… con barra», «… en polea»)

| Inglés | Español |
|---|---|
| barbell | con barra |
| dumbbell(s) | con mancuernas (una sola: «con mancuerna») |
| EZ-bar | con barra Z |
| cable | en polea |
| machine, leverage | en máquina |
| Smith machine | en máquina Smith |
| bands | con banda elástica |
| chains | con cadenas |
| bodyweight | (se omite: es el ejercicio sin material) |
| exercise ball, stability ball, physioball | con fitball |
| medicine ball | con balón medicinal |
| foam roller (SMR) | liberación miofascial con rodillo |
| plate | con disco |
| rope | con cuerda |
| V-bar | con agarre en V |
| trap bar | con barra hexagonal |
| suspended, straps | en suspensión |
| sandbag, atlas stone, tire, log, keg, yoke, rickshaw | saco de arena, piedra de atlas, neumático, tronco, barril, yugo, rickshaw |

### Movimientos

| Inglés | Español |
|---|---|
| squat | sentadilla |
| front squat | sentadilla frontal |
| deadlift | peso muerto |
| Romanian deadlift | peso muerto rumano |
| stiff-legged deadlift | peso muerto con piernas rígidas |
| sumo deadlift | peso muerto sumo |
| lunge | zancada |
| split squat | sentadilla dividida |
| step-up | subida al banco (o al cajón) |
| row | remo |
| upright row | remo al mentón |
| pulldown, lat pulldown | jalón (jalón al pecho) |
| pull-up, pullup | dominada |
| chin-up | dominada con agarre supino |
| push-up, pushup | flexión |
| dip | fondos |
| fly, flye | aperturas |
| reverse fly | aperturas inversas |
| raise (lateral, front, rear delt) | elevaciones laterales, frontales, para deltoides posterior |
| calf raise | elevación de talones |
| shrug | encogimientos |
| extension (triceps) | extensión de tríceps |
| leg extension | extensión de piernas (cuádriceps) |
| leg curl | curl femoral |
| skull crusher, skullcrusher | press francés |
| pushdown (triceps) | extensión de tríceps en polea |
| kickback | patada (de glúteo, de tríceps) |
| glute bridge | puente de glúteos |
| hip raise, hip lift | elevación de cadera |
| back extension, hyperextension | extensión lumbar |
| sit-up | abdominal |
| leg raise | elevación de piernas |
| plank | plancha |
| side bend | flexión lateral |
| twist | giro |
| Russian twist | giro ruso |
| wood chop | leñador |
| woodchop | leñador |
| mountain climbers | escaladores |
| bear crawl | gateo de oso |
| carry, farmer's walk | paseo del granjero |
| clean | cargada |
| hang clean | cargada desde colgado |
| snatch | arrancada |
| jerk | envión |
| clean and jerk | dos tiempos (cargada y envión) |
| thruster | thruster |
| jump, hop, bound | salto |
| box jump | salto al cajón |
| sprint | sprint |
| stretch | estiramiento |
| circles | círculos |
| rotation (internal, external) | rotación interna, externa |

### Posición y agarre

| Inglés | Español |
|---|---|
| incline, decline | inclinado, declinado |
| flat | plano (se omite) |
| seated | sentado |
| standing | de pie |
| lying | tumbado |
| prone, supine | boca abajo, boca arriba |
| kneeling | de rodillas |
| bent-over | con el torso inclinado |
| one-arm, single-arm | a un brazo |
| one-leg, single-leg | a una pierna |
| alternating, alternate | alterno |
| close-grip, narrow | agarre cerrado |
| wide-grip, wide | agarre ancho |
| medium grip | agarre medio (se omite si no distingue) |
| reverse grip, underhand | agarre supino |
| overhand, pronated | agarre prono |
| neutral grip, hammer grip | agarre neutro |
| behind the neck / back | tras nuca / a la espalda |
| overhead | por encima de la cabeza |

### Músculos y zonas

| Inglés | Español |
|---|---|
| biceps, triceps | bíceps, tríceps |
| deltoids, shoulders | deltoides, hombros |
| lats, latissimus dorsi | dorsales |
| traps | trapecios |
| chest, pecs | pecho, pectorales |
| abs, abdominals | abdominales |
| obliques | oblicuos |
| glutes | glúteos |
| quads, quadriceps | cuádriceps |
| hamstrings | femorales (isquiotibiales en las descripciones) |
| calves | gemelos |
| forearms | antebrazos |
| lower back | zona lumbar |
| hip flexors | flexores de la cadera |
| adductors, abductors | aductores, abductores |
| neck | cuello |

## 3. Orden del nombre

**Movimiento + variante + material.** Lo que identifica al ejercicio va primero; el material, al final.

- «Press de banca inclinado con mancuernas» (no «Mancuernas press de banca inclinado»).
- «Remo a un brazo con mancuerna»; «Curl de bíceps alterno con mancuernas».
- «Extensión de tríceps tumbado con barra Z»; «Elevaciones laterales sentado con mancuernas».
- El agarre y la posición son variantes y van antes del material: «Jalón al pecho con agarre cerrado», «Peso muerto sumo con bandas».
- Sin mayúsculas innecesarias: solo la primera letra y los nombres propios («Arnold», «Smith», «Zottman»).
- Sin números ni paréntesis salvo que distingan de verdad dos ejercicios («Cargada desde colgado, por debajo de las rodillas»).
- Un nombre = un ejercicio: **no hay dos nombres iguales** en el catálogo (lo comprueba un test).
- Máximo 100 caracteres, que es lo que admite la columna.
- Los 62 ejercicios de las plantillas y los programas (`catalogo-plantillas-v1.json`) **conservan el nombre que el propietario aprobó** al sembrarlos («Press de banca con barra», «Press militar de pie, con barra»…); no se rehacen.

## 4. Descripciones

- **De tú, en imperativo**: «túmbate», «agarra», «mantén», «baja», «exhala». Nunca «usted» ni formas como «recuéstese», «asegúrese», «colóquese».
- **De 3 a 6 pasos numerados** (`1. …`, uno por línea), con frases cortas. Si el original solo da dos ideas, son dos pasos: no se rellena.
- **Sin inventar técnica**: todo lo que se dice está en el texto en inglés. Se puede unir o partir pasos, pero no añadir consejos, series, repeticiones ni advertencias que el original no traiga.
- Orden natural: posición de partida, movimiento, vuelta a la posición, repetición. Una respiración solo si el original la menciona.
- Medidas: las pulgadas y las libras del original pasan a centímetros y kilos redondeados («12 pulgadas» → «unos 30 cm»).
- Español de gimnasio: «mancuernas», «barra», «polea», «fitball», «banco», «rack», «agarre», «series y repeticiones».
- Nada en inglés dentro de la descripción salvo los términos de la sección 1 (hip thrust, curl, press…).
- Sin saludos, sin «¡», sin emojis, sin «ten cuidado» que el original no diga.

## 5. Lo que no cambia

La versión en inglés (`nombre_en`, `descripcion_en`, `equipo_necesario_en`, `musculo_primario_en`), las imágenes, el equipamiento, el grupo muscular y la dificultad no se tocan. Un test lo comprueba contra los valores de antes.

## 6. Nombres que coinciden con el inglés

Los únicos tres ejercicios cuyo nombre en español es el mismo que el inglés, porque así se dice en un gimnasio español. Un test (`CatalogoEsTest`) lee esta lista: cualquier otro nombre igual al inglés lo hace fallar.

- `Dead_Bug` — «Dead bug»
- `Muscle_Up` — «Muscle up»
- `Superman` — «Superman»

## 7. Descripciones que quedan vacías

Siete ejercicios no tienen texto original que traducir (o el original no es una descripción: «Mira el vídeo…», un enlace, «Halterofilia olímpica»). No se inventa técnica: su descripción queda vacía en español, como estaba en inglés. `Hip_Circles_prone`, `Iron_Cross`, `One-Arm_Kettlebell_Swings`, `Power_Clean`, `Quad_Stretch`, `Romanian_Deadlift` (esta sí sale en plantillas) y `Side_Jackknife`.
