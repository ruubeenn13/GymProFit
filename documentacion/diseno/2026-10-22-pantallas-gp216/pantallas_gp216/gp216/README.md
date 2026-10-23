# Pantallas rehechas de GP-216 (lienzo «Firma visual GymProFit», 22-10-2026)

Tableros 13 a 32 del lienzo, aprobados por el propietario el 22-10 («lo veo todo bien»). Son la referencia visual de los lotes de GP-216. Los cimientos (espaciado, radios, tarjetas, filas, roles de texto) ya están en `DESIGN.md` de la app desde el paso 2 de GP-210 (v1.8.0): se usan esos tokens, no los valores escritos en el HTML.

## Cómo leer los tableros

- Son HTML del lienzo de diseño: `<sc-for>`, `<sc-if>` y `{{ … }}` son su plantilla; los datos y estados de ejemplo están en el `<script>` del final (`renderVals`). No se abren en el navegador tal cual: se leen como especificación (estructura, textos, colores, tamaños, estados).
- Ancho 390 px = 390 dp. Las cifras son de la cuenta demo «Dani» (`scripts/sembrar_demo.py`).
- `img/`: Profi (cara y pose «señala», juego oscuro), logo y fotos de free-exercise-db (las de `imagen_url` e `imagen_url_2` del catálogo; la demostración del ejercicio alterna las dos).
- Iconos: Material Symbols Rounded (los nombres son ligaduras dentro de `<span class="ms">`).

## Tableros

- `tableros/13-InicioPanel.html` — 13 · Inicio
- `tableros/14-Entrenar.html` — 14 · Entrenar
- `tableros/15-Nutricion.html` — 15 · Nutrición
- `tableros/16-Progreso.html` — 16 · Progreso
- `tableros/17-Perfil.html` — 17 · Perfil y ajustes
- `tableros/18-TusDatos.html` — 18 · Tus datos y objetivos
- `tableros/19-Notificaciones.html` — 19 · Notificaciones
- `tableros/20-Contrasena.html` — 20 · Contraseña (y correo)
- `tableros/21-Eliminar.html` — 21 · Eliminar cuenta
- `tableros/22-Tema.html` — 22 · Hojas de elegir: Tema
- `tableros/23-AcercaDe.html` — 23 · Acerca de
- `tableros/24-ComidaVacia.html` — 24 · Comida vacía: Profi te propone
- `tableros/25-AnadirAlimento.html` — 25 · Añadir alimento: varios de una vez
- `tableros/26-FichaAlimento.html` — 26 · Ficha: cuánto y cómo encaja
- `tableros/27-Escaner.html` — 27 · Escáner: encontrado
- `tableros/28-ComidaDetalle.html` — 28 · Comida apuntada
- `tableros/29-Biblioteca.html` — 29 · Biblioteca de ejercicios
- `tableros/30-DetalleEjercicio.html` — 30 · Ejercicio: tu progreso y cómo se hace
- `tableros/31-DetallePrograma.html` — 31 · Detalle de programa
- `tableros/32-DetalleRutina.html` — 32 · Detalle de rutina

## Notas del lienzo (decisiones de diseño)

**Inicio (13).** Inicio aprobado el 22-10. De arriba abajo: avatar arriba a la derecha para Perfil y ajustes · Profi te dice una frase con datos tuyos · «Hoy toca» en el naranja de la marca, con las zonas del día en el cuerpo y los ejercicios en carrusel (a por cuánto vas hoy) · «Tu día»: calorías, proteína y agua en tarjetas que se deslizan, y el botón de la comida que toca · «Tus músculos esta semana» en el cuerpo, con series por zona · «Tu progreso»: kilos por semana y el último récord · «Tus últimos entrenos» en carrusel. Los iconos de ejercicio y el cuerpo son provisionales: ilustraciones de ejercicio y mapa muscular de frente y espalda.

**Perfil y ajustes (17–23).** Perfil y ajustes con las filas reales de la app (22), en 6 grupos y un solo patrón de fila. Los 5 datos del cuerpo y objetivos pasan a una tarjeta-resumen con tu plan; dentro, «Tus datos» cambia desplegables por botones +/− y opciones a la vista, y te enseña cómo queda tu plan antes de guardar. Notificaciones: si falta el permiso, Profi te lo pide y las filas quedan apagadas. Contraseña: requisitos que se marcan solos al escribir; Correo, igual con tu correo actual arriba. Eliminar: 4 líneas de qué pierdes en vez de 11 viñetas, el resto plegado, y casilla para habilitar el botón. Tema, Idioma, Vaso y Descanso: la misma hoja inferior. Nuevo: «Ver el tour de nuevo» (GP-215). Quitado: «Foto de perfil» como fila (es la cámara del avatar).

**Apuntar comida (24–28).** El flujo de apuntar, de vacío a hecho. Comida vacía: Profi propone copiar la de ayer y 3 ideas para la proteína que te falta. Añadir alimento: el título dice a qué comida vas y se cambia tocándolo (sin la ✕ ni la etiqueta aparte); «Lo que te encaja» arriba; el «+» añade al momento, como hoy, y abajo sale una barra con lo añadido en esta visita, lo que suma y la proteína que falta, y «Listo» para volver (aprobado el 22-10). Ficha: la cantidad sube arriba con −/+ y atajos (½, 1, 2 filetes o 100/150/200 g); las cifras y «Cómo encaja en tu día» cambian al momento; la tabla completa, plegada. Escáner: al leer el código sube una hoja con el alimento y «Añadir», sin cambiar de pantalla. Comida apuntada: el resumen en el naranja de la marca, un comentario de lo bien que va, iconos por tipo de alimento.

**Antes de entrenar (29–32).** Biblioteca: chips de grupo muscular con icono, que filtran; «Los que más haces» con tu marca; filas compactas con el nivel por color (verde, ámbar, rojo) en vez de 2 líneas de descripción y 2 chips. Ejercicio: arriba la ilustración (hoy es un bloque blanco, lo más luminoso de la pantalla); tu mejor marca en dorado con el máximo estimado; dos pestañas: «Tu progreso» (gráfica por periodo y últimas veces) y «Cómo se hace» (pasos numerados, de tú). Programa: tu semana con cada rutina en su día, descripción corta con «Leer más», rutinas plegables. Rutina: cabecera en el naranja de la marca con series por zona y la última vez; cada ejercicio con su pauta y tu última marca (dorado si fue récord). Los iconos son provisionales hasta tener ilustraciones.

## Decisiones del propietario y de este chat que afectan a la implementación

- Entrenar, Nutrición y Progreso llevan el avatar arriba a la derecha (abre Perfil y ajustes); Progreso pierde el engranaje y sus cuatro pestañas: es una sola pantalla y cada «Ver todo» abre la lista completa.
- Ajustes y Editar perfil se juntan en «Perfil y ajustes»; «Tus datos y objetivos» es su subpantalla, con −/+ y el plan antes de guardar (usa la fórmula real de la app, no la del tablero).
- Añadir alimento: el «+» sigue añadiendo al momento (`AnadirRapido`); abajo, barra con lo añadido en esta visita, kcal, proteína que falta y «Listo». Nada queda sin guardar.
- Tema, Idioma, Vaso y Descanso: una sola hoja inferior.
- «Ver el tour de nuevo» (GP-215) aparece cuando exista el tour.
- Iconos de ejercicio y el mapa del cuerpo son provisionales.
