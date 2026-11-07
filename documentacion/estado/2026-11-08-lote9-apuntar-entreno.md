# Lote 9 · apuntar un entreno hecho, el objetivo del Perfil y el resumen a 0 kg (GP-241, GP-242 y GP-250)

07 y 08-11-2026. Solo app; la API no cambia (`POST /sesiones/completa` ya admitía ejercicios sin rutina). Rama `gp-241-lote9`.

## Encargo

Los puntos 1 a 9 de `documentacion/diseno/2026-11-06-entreno-y-objetivo/README.md`: «Apuntar entreno» rehecho (cabecera protagonista con fecha, rutina y cuadros que suman, filas de la sesión en vivo, caras, nota y pie), la hoja «¿Qué entrenaste?», un solo orden para las dos hojas de rutina, la tarjeta del objetivo del Perfil y el resumen del entreno a 0 kg en la cascada de DEC-060. Además, una línea en `ENTORNO-PRUEBAS.md`.

## Commits

| Hash | Qué |
|---|---|
| `72def92` | Tableros y especificación |
| `29e5f2c` | App: Apuntar entreno, sus hojas, el Perfil y el resumen, con las pasadas 1 y 2 del revisor y lo que el formulario viejo dejaba sin uso |
| `2015929` | DEC-060: la cascada también en el resumen |
| `f72c2a3` | `ENTORNO-PRUEBAS.md`: las claves locales se quedan por decisión del propietario |
| `2b4c774` | App: pasada 3 del revisor (el círculo del récord anima y la lista no salta). Su mensaje dice «pasadas 1 a 3»: las 1 y 2 van en `29e5f2c`; corregirlo exigía reescribir la rama publicada y no se hizo |

## Cómo se hizo

- **El borrador es una `SesionEnCurso`**, el modelo de la sesión en vivo (ahora `Serializable`), en el `SavedStateHandle`: marcar, «Anterior», las pistas, el récord y el cuerpo del POST son `LogicaSesion` y `LogicaRecordVivo`, no una copia. Se mantienen la clave de idempotencia (GP-016, GP-006) y el aviso al salir (GP-098), ahora solo con algo marcado, la nota o una cara.
- **La rutina de entrada** la elige `HoyToca` con las activas, el programa que sigue y las sesiones (las mismas tres peticiones que Inicio). Duración de entrada: la de la rutina; las de un programa no la traen y se usa la del programa; libre, la del último entreno; si no, 45.
- **Lógica pura nueva**: `OrdenRutinas`, `CabeceraEntreno`, `Caras`, `ObjetivoPerfil` (frase, icono, reparto y columnas) y `ComparacionEntreno.cabecera`.
- **Tokens nuevos** (DESIGN.md): `gp_hero_superficie`, `gp_hero_oro`, `gp_hero_oro_fondo` y `capa_hero`, con sus contrastes.
- **Borrado con permiso del propietario**: `EjercicioPesoAdapter`, `item_ejercicio_peso`, `item_serie`, `item_desplegable`, `marca_elegida`, `AvisoDescartar.sesionConDatos` y 20 cadenas del formulario viejo.

## Tests

1209 unitarios en verde y lint sin errores. Tests nuevos, todos vistos en rojo por mutación (19 mutaciones, cada una tumbó su test):
- **Orden de las hojas** (`OrdenRutinasTest`, 8): con programa y la de hoy en mitad del ciclo, con programa empezando por hoy, una rutina repetida en el ciclo, un día borrado, sin programa, la propia de hoy delante del programa y sin ninguna que toque.
- **Sumas de la cabecera** (`CabeceraEntrenoTest`, 11): con kilos, a 0 kg (sin cuadro de kilos), entreno libre, el pie en sus tres casos, la última vez en cascada y el récord (uno por ejercicio, igualar no cuenta, tipos que no compiten, varios con la coma de serie en inglés).
- **Caras** (`CarasTest`, 4): de 1 a 5 con su nombre en ES y EN, NULL sin tocar y tocar la marcada la quita.
- **Frase del objetivo** (`ObjetivoPerfilTest`, 8): los cuatro objetivos en ES y EN, la nota de menores con la calculadora de verdad, el reparto y las columnas al 30 %.
- **Cascada del resumen** (`ResumenCascadaTest`, 4): con kilos, a 0 kg con series (sin «−100 %» ni ficha de series), sin ejercicios y sin series.
- `RegistrarSesionViewModelTest` rehecho (32): fecha, la que toca hoy con y sin programa, libre, matar el proceso y restaurar, cambiar de rutina, fallos, marcar, duración, «Todo como la última vez», caras, pliegue, la clave, el doble toque y el cuerpo con solo lo marcado.

## Revisor de UI

Cuatro combinaciones (ES oscuro 1,0; EN claro 1,0; ES claro 1,3; EN oscuro 1,3) en las cinco superficies, tres pasadas.
- **Pasada 1**: 2 altos («0 de 0» tras cambiar de rutina; «Ejercicios» partido a 1,3), 4 medios y bajos. Arreglados.
- **Pasada 2**: 4 medios (título descentrado, columnas del Perfil, sin marcar solo por el color, nombres sin traducir) y bajos. Arreglados salvo el de los nombres (abajo).
- **Pasada 3**: 2 medios al marcar el récord. Arreglados y medidos a mano: la fila tocada no se mueve (0 px antes y después, 1080×2400) y el círculo dorado crece en tres fotogramas a `animator_duration_scale` 5, como el verde.

## Producción

Con la release (contra `api.gymprofit.app`, commit `633ef35`) y la cuenta nueva `prueba241bc6117`, que sigue «Cuerpo completo» de gimnasio:
- La rutina que toca, «Cuerpo completo A» con «La que te toca en tu programa» y 50 min: dos series con kilos; girar y matar el proceso a medias no perdieron nada; **dos toques seguidos en «Guardar entreno» dejaron una sola sesión** (56).
- Entreno libre con dos ejercicios del catálogo (57).
- Peso corporal: «Cuerpo completo A» otra vez a 0 kg, cuya última vez tuvo 980 kg: el resumen dice «HAS HECHO 2 series en esta sesión», sin «−100 %» ni ficha de comparación (58).
- Cambiar el objetivo a «Perder grasa» recalcula el plan del Perfil: de 3.114 kcal y 140/445/86 g a 2.166 kcal y 156/250/60 g.
- Cuenta borrada: `DELETE /usuarios/me` 200 y login 401; su clave, borrada después.

## Decisiones tomadas

- El botón de marcar sigue el patrón de la sesión en vivo (verde hecha), no el naranja del tablero, y sin marcar es un aro: el tablero no lo distinguía más que por el color.
- Los ejercicios de un entreno libre entran con 3 series vacías, como en la sesión en vivo; el primero abierto y los demás plegados.
- «Todo como la última vez» rellena y marca solo las series sin marcar que tuvieron su igual; cuando no queda ninguna, su sitio se guarda para que la lista no salte.
- La cabecera del Perfil (foto y nombre) se queda como estaba: el README no pide el formato del tablero.
- Con el teclado abierto, el pie fijo se aparta.

## Fuera del lote, para el propietario

- **Nombres de ejercicio sin traducir en inglés** en las rutinas propias y en «Frente a la última vez» del resumen («Press de banca» junto a «Bench press» en la misma pantalla), y nombres de comida de «Para recuperar» que salen unas veces en español: vienen así de la API (DEC-020).
- El botón de marcar de la **sesión en vivo** solo distingue hecha de sin hacer por el color (en Apuntar ya es un aro).
- «·» en Añadir ejercicios y en el plan fijo de Tus datos; el borde de «Para recuperar» en claro; la tarjeta dorada de primeras marcas sin récord; el borde de las opciones de `HojaElegir` en claro (1,2:1).
- El plan guardado de la cuenta local `prueba` cambió al volver a guardar su objetivo en las pruebas (proteína de 135 a 128 g, DEC-054).
