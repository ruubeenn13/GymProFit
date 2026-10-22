# GP-209 · PRODUCT.md y DESIGN.md de la app Android — 2026-10-22

Solo documentación: ni código ni recursos, ningún token inventado. Documenta lo que existe; no rediseña nada. Va después de GP-207 (herramientas).

## Qué se ha hecho

| Fichero | Qué es |
|---|---|
| `app/GymProFit/PRODUCT.md` | Formato de impeccable (`init`), plataforma `android`. Reutiliza usuarios y posicionamiento de `web/PRODUCT.md` y añade lo propio de la app |
| `app/GymProFit/DESIGN.md` | Formato oficial de DESIGN.md (frontmatter de tokens + las secciones canónicas) más una sección «Motion» y otra «Huecos» |
| `CLAUDE.md` | «Antes de tocar código», punto 4 nuevo: todo trabajo de UI en `app/` lee `app/GymProFit/DESIGN.md` y lo deja al día si cambia un token o un patrón |
| `.claude/agents/revisor-ui-android.md` | Ahora compara también con `app/GymProFit/DESIGN.md` y `PRODUCT.md` y lo cita en cada fallo |
| Vault | `app_PRODUCT.md` y `app_DESIGN.md` en `claude/gymprofit-app/`, con su línea en `MEMORY.md` (solo cambian `name` y `description` del frontmatter) |

## Preguntas hechas (tres, de una vez) y respuestas

1. **Profi:** ¿qué papel y tono tiene dentro de la app? → *Guía que habla poco*: una frase corta en el alta, las pantallas vacías, los logros y «Tu semana»; mismo tuteo, sin épica; nunca bloquea ni sustituye un dato.
2. **Norte visual de DESIGN.md:** → *«El cuaderno del atleta»*.
3. **Usuarios:** → reutilizar los de `web/PRODUCT.md` y añadir el momento de uso: el móvil entre series, a menudo con una mano, de principiante a avanzado.

Lo demás salió del repositorio (DEC-018/019/020/039, `res/values`, `Movimiento.java`, los diseños aprobados y `web/PRODUCT.md`).

## Cómo se comprobó que cada token se rastrea

- **Colores:** un script compara los 47 colores del frontmatter con `values/colors.xml` y `values-night/colors.xml` por nombre de token y tema: 47 de 47 coinciden.
- **Tipografía:** los 12 tamaños coinciden con los `text_*` de `values/dimens.xml`.
- **Movimiento:** las 44 duraciones citadas coinciden con las constantes de `Movimiento.java`; el resto (6 de escalón o retraso) se dan como «+N» en el texto.
- Cada sección da el fichero del que sale el valor. Las referencias a líneas concretas se quitaron: se desfasan.
- **Linter:** impeccable no trae linter de DESIGN.md (su `detect` es de código web). Se pasó el oficial, `@google/design.md lint` 0.4.0 (instalado en una carpeta temporal, fuera del repo): **0 errores, 32 avisos, 1 info**. Los avisos son tokens «huérfanos» (un tema que ningún componente referencia: los componentes apuntan al oscuro) y no se tocan. Para pasarlo hubo que:
  - dar el frontmatter en `px` (el linter solo admite px, em y rem; aquí 1 px = 1 dp = 1 sp, como los lienzos; el texto sigue en dp y sp);
  - llamar `primary` al naranja oscuro, que el linter exige;
  - poner el círculo como `9999px` (el 50 % de Android no se puede escribir).
- `revisor-ui-android` aplicado a Inicio: ver abajo.

## Huecos (lo que DESIGN.md nombra y no arregla)

1. **Vacío, error y carga sin sistema (GP-064):** existe `VistaEstado` y la usan 10 layouts, pero hay 8 pantallas con su vacío a mano; la carga es un `ProgressBar` o un diálogo modal, sin esqueletos; 84 toasts frente a 5 Snackbar.
2. **Animaciones a mano sin sistema:** `GuardaPlanActivity`, `SplashActivity`, `SelectorPildora`, `SemanaEjemplo`, `PasoPlanFragment`, `MainActivity`, `BarraNavegacion` y el deslizamiento entre pantallas.
3. **Ni Material motion ni estilos Expressive** (0 usos de `MaterialSharedAxis`, `ContainerTransform` o `FadeThrough`; sin Dynamic Color).
4. **Profi, todavía fuera de la app:** ningún recurso suyo en `res/`.
5. Sin escala de espaciado ni de radios declarada (13 radios y más de 10 márgenes en uso).
6. Elevación incoherente: `Card` 1 dp, `Card.Estructura` 0 dp y 21 tarjetas a 0 a mano.
7. Tabla de DEC-039 incompleta: «doce momentos» pero llega al 23; faltan el 13 y el 14, y `RESPIRA` está en código y test, no en la tabla.
8. Alias de color heredados sin retirar.
9. Contenido de base de datos sin traducir (DEC-020, 752 de 873 ejercicios).
10. Títulos grandes de Inicio que no crecen con la letra 1,3 (medido en GP-207; puede ser un tope deliberado).
11. 23 `textSize` en `sp` escritos a mano en layouts.
12. Bordes discontinuos (tarjeta del fundador y otros dos) sin documentar.
13. Inicio con errores de carga: mensaje repetido, nutrición sin reintento, tarjeta pulsable sin nombre accesible.

Los puntos 7 y 10 a 13 no estaban en la lista que pedía el encargo: salieron al contrastar DEC-039 con el código y al revisar Inicio, y se anotan porque son incoherencias reales.

## Revisor aplicado a Inicio

Probado con un agente genérico que sigue `revisor-ui-android.md` (el agente por su nombre no existe hasta reiniciar Claude Code). **Cita DESIGN.md**: en cada fallo pone la sección que rompe («Huecos» 1 y 10, «Typography > The No-Raw-sp Rule», «Layout > Zonas pulsables», «Do's and Don'ts»). Dice que le sirvió, y que le faltaban los estados de error de Inicio, la tarjeta del fundador y el tope de letra de los títulos: los tres están ya en «Huecos» (11 a 13).

**Limitación:** con la cuenta local vacía, Inicio solo enseña estados de error (la API responde 200 con «0-byte body» en `rutinas/activas`, `sesiones/usuario` y `programas/seguido`; sin confirmar si es la API local o el cliente; el `health` de la API está UP), así que **no se pudo comparar contra el diseño con datos**. Antes de dar Inicio por revisado hace falta una cuenta con datos (`scripts/sembrar_demo.py`). Lo que sí midió:

- Alto: el estado de error repite el mismo mensaje en dos tarjetas y deja la de nutrición sin cifra, sin reintento y con los puntos de macros sin texto (DEC-019).
- Medio: `cardNutricionHoy` es pulsable (abre Nutrición) y sale sin nombre accesible (379×149 dp).
- Medio: con letra 1,3 no crecen `tvHola`, `tvHoyTitulo`, `tvSemanaZonas`, `tvInicial`, el «R» del fundador ni `btnHoyPrincipal`.
- Bajo: `fragment_inicio.xml` fija 20 sp a mano; borde discontinuo en la tarjeta del fundador (no está en DESIGN.md ni en el diseño); el rótulo «PRUEBAS · LOCAL» (solo debug) baja de 13 sp.
- Sin fallo: 48 dp en avatar, botón principal, «Registrar comida», Escribir/Ocultar y barra inferior, con nombre accesible; nada cortado en EN ni con 1,3.
- Capturas y logs en `%TEMP%\revisor-ui\Inicio\`.

## Dudas

1. DEC-039 pide que `Movimiento` y su tabla cambien a la vez; la tabla no recoge `RESPIRA`. ¿Se añade a DEC-039 en un lote aparte? Aquí no se tocó.
2. Las tarjetas con `cardElevation="0dp"` a mano contradicen el 1 dp de `Widget.GymProFit.Card`. ¿Es deliberado (borde en vez de sombra) o se unifica? DESIGN.md recoge la regla «borde, no sombra» como la vigente.
3. «Tu semana»: en Inicio hoy la tarjeta se titula «ESTA SEMANA». DESIGN.md/PRODUCT.md usan «Tu semana» solo al citar la decisión de Profi del 22-10; si es otra pantalla, hay que decir cuál.
4. El `.claude/agents/revisor-ui-android.md` sigue sin poder invocarse por su nombre hasta reiniciar Claude Code; la prueba se hizo con un agente genérico que sigue el fichero.

## Resolución de las dudas (22-10, lote GP-211/212)

- **DEC-039**: la tabla recoge ya los momentos 13 y 14 (existen en `documentacion/diseno/2026-10-01-nutricion/fuente/Movimiento.dc.html`: «Las cifras cuentan» y «El anillo se llena», 1200 ms; el código los resuelve con `CIFRAS`, sin constante propia) y `RESPIRA` como fila «— · Respira» (no es un momento del lienzo, existe solo en `Movimiento.java`). `MovimientoTest` ahora acepta filas «— · …» y ya no tiene excepciones; en verde. El título «doce momentos» no se tocó.
- **Elevación**: decidida «borde, no sombra»; `Widget.GymProFit.Card` pasa a 0 dp con borde en el paso 2 de GP-210. Hasta entonces, hueco decidido y pendiente en `DESIGN.md`.
- **«Tu semana»**: pantalla nueva desde una tarjeta de los lunes en Inicio (GP-210, paso 6). «ESTA SEMANA» (GP-090) no cambia y Profi no entra en ella. Recogido en `DESIGN.md`.
