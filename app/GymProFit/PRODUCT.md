# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Gente que entrena en serio en el gimnasio (o en casa con mancuernas o peso corporal), la misma que `web/PRODUCT.md` describe en la web: llega desde un vídeo de TikTok o Instagram y entra por la beta de Google Play. En la app el momento de uso es otro: el móvil en la mano **entre series, en el gimnasio, a menudo con una sola mano**, y de nuevo en la cocina o en el súper para apuntar y buscar comida. El rango va de principiante a avanzado (DEC-002): la profundidad existe, pero no se impone.

## Product Purpose

GymProFit junta en una sola app Android el entreno, la nutrición y el progreso: apunta las series, las comidas y el peso, y dice qué toca hoy. Se organiza alrededor de un ciclo, ENTRENAR → REGISTRAR → ENTENDER → PROGRESAR → VOLVER (DEC-003), y una función que no mejora ninguno de esos pasos tiene prioridad baja. Éxito: que quien entra apunte su primera sesión y su primera comida, y vuelva mañana.

## Positioning

Entreno y dieta en la misma app, con el detalle de una app de entreno (lo de la última vez en cada serie, el descanso que avisa con la pantalla apagada) y el de una de nutrición (buscar o escanear, lo que queda hoy, copiar la comida de ayer). «Entrenas en serio. Comes a ojo.» (el lema es el de `web/PRODUCT.md`). Una cifra que enseña la app es una cifra real: el entrenamiento no usa calorías estimadas como métrica (DEC-004); las de nutrición, sí.

## Operating Context

- Beta cerrada de Google Play, solo Android y gratis. **No hay app publicada**: no se ha subido nada a Play, ni a la pista interna; hay builds repartidas fuera de Play a probadores.
- Una app, cuatro pestañas (Inicio, Entrenar, Nutrición, Progreso) y un «+» central de acciones rápidas, más Ajustes (GP-105). Español e inglés siempre (DEC-020).
- Vistas XML en Java con Material Components 1.14.0; `minSdk` 24, `targetSdk` 36. No se migra a Compose en bloque (DEC-010).
- Desarrollo contra una API local aislada de producción (DEC-017). La API vive en Render.
- La variante `debug` enseña un rótulo «PRUEBAS · LOCAL» que no existe en release.

## Capabilities and Constraints

- Funciones que existen hoy: alta guiada en capítulos con plan inicial, sesión en vivo con descanso y «Anterior», programas y plantillas, récords, nutrición con buscador propio, escáner de códigos, favoritos, copia de comida, platos propios, agua, diario y día completo, avisos por tipo, foto de perfil, borrado de cuenta.
- Terminado no es compilar (DEC-022): funciona, trae sus tests, es accesible, está en ES y EN y se ve bien en claro y oscuro.
- No se añade IA hasta que los datos sean fiables (DEC-009). Free + Pro sin bloquear el núcleo (DEC-008).
- El movimiento es **nativo**: animadores, interpoladores y vectores del sistema. Ni Lottie ni librerías de animación (decidido el 22-10-2026; DEC-039 ya excluía dependencias nuevas).
- Contrato de API estable: hay builds fuera de Play que no se actualizan solas.

## Brand Commitments

- Nombre GYMPROFIT; oscuro por defecto con tema claro completo; naranja de marca como acento único en los dos temas; dorado reservado a récords y logros (DEC-018). Barlow y Barlow Condensed; iconos Material Symbols Rounded.
- Tono directo, en tuteo, sin épica de gimnasio.
- Sin emojis como iconos ni en la interfaz.
- El propietario no quiere: estética gaming, exceso de efectos, aspecto de plantilla genérica (DEC-018, y lo mismo que la web).
- **Profi** (la mascota narradora de la serie de vídeos; su material vive en el repositorio de marketing, fuera de este) entra en la app **solo en el alta, en las pantallas vacías, en los logros y en «Tu semana»**. Es una **guía que habla poco**: una frase corta en esos momentos, mismo tuteo y sin épica, y nunca bloquea ni sustituye un dato. Todavía no está en la app (decidido el 22-10-2026).

## Evidence on Hand

- Diseños aprobados en `documentacion/diseno/`: estructura de cuatro pestañas y Ajustes (27-09), alta nueva con su movimiento (30-09), nutrición nueva (01-10 y 02-10) y administración (la web).
- Cuatro capturas reales con la cuenta demo local en `web/img/capturas/`.
- Decisiones vigentes en `documentacion/PRODUCT-DECISIONS.md` (sobre todo DEC-018, DEC-019, DEC-020 y DEC-039).
- No hay: cifras de usuarios, testimonios, valoraciones ni prensa. No se inventan. El contenido de la base de datos (ejercicios, programas) está sin traducir al inglés en su mayor parte (DEC-020).

## Product Principles

1. Simple por fuera, potente por dentro: la profundidad aparece cuando se pide, no antes (DEC-002).
2. Lo que se apunta tiene que ser fácil de apuntar entre series: pocas pulsaciones, zonas pulsables grandes, nada que espere a una animación (DEC-039).
3. Las cifras son reales o no se enseñan; nada de estimaciones disfrazadas de dato (DEC-004).
4. Lo que se ve debe poder medirse: contrastes, tamaños y zonas pulsables se comprueban, no se suponen (DEC-019).
5. Una sola fuente de verdad para el diseño: los tokens de `res/values` y este sistema; cada pantalla nueva parte de ellos.

## Accessibility & Inclusion

Nombre accesible, zona pulsable de al menos 48 dp y estado comunicado en todo elemento interactivo; nada depende solo del color; las vistas dibujadas a mano nacen con su accesibilidad (DEC-019). Contraste medido contra WCAG (4,5:1 en texto, 3:1 en bordes y gráficos). La letra sigue la escala del sistema, con topes puntuales hasta 1,3. «Quitar animaciones» deja todo en su estado final; las vibraciones se quedan porque no son movimiento. Español e inglés con plurales (DEC-020).
