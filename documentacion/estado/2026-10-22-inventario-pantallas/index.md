# Inventario de pantallas de la app (GP-216)

Foto del estado real de la app el 22-10-2026 (versión 1.6.9), para rediseñarla con criterio. **Solo lectura**: no se ha cambiado código de producto (salvo el arreglo del vacío de «Tu semana», que este inventario encontró, en su propio commit).

- **Cuenta**: `@dani.entrena`, sembrada por `scripts/sembrar_demo.py` (programa «Torso y pierna», 3 semanas de historial); entorno local de pruebas, sin datos de nadie.
- **Dispositivo**: emulador Pixel 8 (1080 × 2400), API 36, build `debug` (lleva el distintivo «PRUEBAS · LOCAL» arriba).
- **Variantes**: toda pantalla en **ES · oscuro · letra 1,0**. Inicio, Entrenar, Nutrición, Progreso (Récords), Ajustes y Editar perfil, también en **ES · claro · letra 1,0** y **ES · oscuro · letra 1,3**; el resumen del entreno y «Tu semana» también en claro. Estados **vacío** y **error** donde se podían provocar sin tocar código (sin red: `svc wifi/data disable`; vacío: semanas sin datos, comidas sin apuntar, búsquedas sin resultado).
- **Capturas**: `capturas/<pantalla>__<idioma>_<tema>_<letra>.png`, reducidas a 540 × 1200 y a 96 colores para que pesen poco (la carpeta entera, unos 4 MB).
- Las observaciones son lo que se ve en la captura o en el layout: cuántos bloques hay, de qué tamaño, qué se repite. No son propuestas.
- Lo que el `DESIGN.md` ya llama «Huecos» (sin escala de espaciado ni de radios, elevación incoherente, tamaños a mano) se **mide** abajo, al final.

## Las pantallas

46 entradas (algunas agrupan varias pantallas o hojas de un mismo flujo). Capturas: 97.

| Pantalla | Clase | Cómo se llega | Capturas | Observaciones |
|---|---|---|---|---|
| Splash | `SplashActivity` | Al abrir la app; decide a Bienvenida, Login o Main. | [es oscuro 1,0](capturas/splash__es_oscuro_1-0.png) | · Un solo elemento centrado (logo, nombre y rueda de carga): no hay jerarquía que medir.<br>· No lleva ningún texto de carga: lo único que se mueve es la rueda bajo el nombre. |
| Bienvenida | `BienvenidaActivity` | Splash sin sesión; Cerrar sesión; sesión de invitado. | [es oscuro 1,0](capturas/bienvenida__es_oscuro_1-0.png) | · La demostración animada ocupa algo más de la mitad de la pantalla; el titular y su texto, el cuarto inferior.<br>· Dos acciones: «Empezar» (naranja) y «Ya tengo cuenta» (solo texto, sin contorno): 1 primaria y 1 terciaria.<br>· Selector de idioma («ES») como única pieza arriba, aislada del resto. |
| Alta, pasos 1 a 4 y plan | `AltaActivity (PasoOpciones, PasoSobreTi, PasoDias, PasoPlan)` | Bienvenida › Empezar. | [alta-paso-1 es oscuro 1,0](capturas/alta-paso-1__es_oscuro_1-0.png)<br>[alta-paso-2 es oscuro 1,0](capturas/alta-paso-2__es_oscuro_1-0.png)<br>[alta-paso-3 es oscuro 1,0](capturas/alta-paso-3__es_oscuro_1-0.png)<br>[alta-paso-3-completo es oscuro 1,0](capturas/alta-paso-3-completo__es_oscuro_1-0.png)<br>[alta-paso-4 es oscuro 1,0](capturas/alta-paso-4__es_oscuro_1-0.png)<br>[alta-paso-5 es oscuro 1,0](capturas/alta-paso-5__es_oscuro_1-0.png)<br>[alta-paso-6-plan es oscuro 1,0](capturas/alta-paso-6-plan__es_oscuro_1-0.png) | · Entre el texto del paso y las opciones hay un hueco vacío de unos 120 dp (pasos 1, 2, 3 y 4): el contenido se alinea abajo, no junto al título.<br>· Cuatro tarjetas de opción apiladas con el mismo peso en el paso 1 («Perder grasa», «Ganar músculo», «Mantenerme», «Ganar fuerza»).<br>· «Siguiente» deshabilitado (gris) hasta elegir; en «Sobre ti» hay además «Prefiero no decirlo» como segunda salida. |
| Alta: hojas de opción y de número | `AltaActivity (HojasAlta)` | Alta, paso «Sobre ti», tocando Sexo, Edad, Altura, Peso o Actividad. | [alta-dialogo-opciones es oscuro 1,0](capturas/alta-dialogo-opciones__es_oscuro_1-0.png)<br>[alta-dialogo-numero es oscuro 1,0](capturas/alta-dialogo-numero__es_oscuro_1-0.png)<br>[alta-dialogo-actividad es oscuro 1,0](capturas/alta-dialogo-actividad__es_oscuro_1-0.png) | · Hoja inferior con título, las opciones como tarjetas de borde (círculo de radio a la derecha) y «Listo» a todo el ancho.<br>· La hoja de número pone el selector sin contexto de unidad (solo «29 / 30 / 31»); la unidad va en la fila de debajo. |
| Guarda tu plan | `GuardaPlanActivity` | Alta, paso final › Guarda tu plan. | [es oscuro 1,0](capturas/guarda-plan__es_oscuro_1-0.png) | · Tres campos y un botón «Crear cuenta» con la letra de condiciones, muy pequeña, debajo.<br>· Cada campo lleva su línea de apoyo («Así te saludamos…», «Mínimo 8 caracteres…»): 2 de los 3. |
| Avisos | `AvisosActivity` | Tras Guarda tu plan o el primer login. | [es oscuro 1,0](capturas/avisos__es_oscuro_1-0.png) | · Tarjeta de ejemplo del aviso (arriba), titular, tres filas con interruptor y dos botones: 5 bloques en una pantalla.<br>· «Activar avisos» (primario) y «Ahora no» (texto): igual patrón que la Bienvenida. |
| Entrar | `LoginActivity` | Bienvenida › Ya tengo cuenta; Guarda tu plan › Entrar. | [es oscuro 1,0](capturas/login__es_oscuro_1-0.png) | · Dos campos y un enlace en la mitad superior; el botón «Entrar» queda anclado abajo y el medio de la pantalla, vacío.<br>· El error de credenciales no se pudo ver: con usuario y clave erróneos el campo se vacía y no aparece ningún mensaje en la captura (ver «No se pudo capturar»). |
| Recuperar contraseña | `RecuperarPasswordActivity` | Entrar › ¿Has olvidado tu contraseña? | [es oscuro 1,0](capturas/recuperar-password__es_oscuro_1-0.png) | · Un campo y un botón; más de la mitad de la pantalla está vacía. |
| Inicio | `MainActivity › InicioFragment` | Tab de MainActivity 1 de 4. | [inicio es claro 1,0](capturas/inicio__es_claro_1-0.png)<br>[inicio es oscuro 1,0](capturas/inicio__es_oscuro_1-0.png)<br>[inicio es oscuro 1,3](capturas/inicio__es_oscuro_1-3.png)<br>[inicio-error es oscuro 1,0](capturas/inicio-error__es_oscuro_1-0.png) | · 4 tarjetas seguidas en la primera pantalla (Hoy toca, Esta semana, Último récord, Nutrición de hoy) sin títulos de sección entre ellas.<br>· El único título grande es «Hola, Dani» (30 sp); el rótulo de cada tarjeta es de 13 sp.<br>· Sin red, «Hoy toca» y «Esta semana» muestran cada uno su propio error y la tarjeta de nutrición queda con la barra vacía y tres puntos (sin texto). |
| Entrenar | `MainActivity › EntrenarFragment` | Tab de MainActivity 2 de 4. | [entrenar es claro 1,0](capturas/entrenar__es_claro_1-0.png)<br>[entrenar es oscuro 1,0](capturas/entrenar__es_oscuro_1-0.png)<br>[entrenar es oscuro 1,3](capturas/entrenar__es_oscuro_1-3.png)<br>[entrenar-error es oscuro 1,0](capturas/entrenar-error__es_oscuro_1-0.png) | · La tarjeta del programa trae título, barra de 5 pasos, «Hoy toca», 6 ejercicios con su dosis y el botón: 5 niveles de información en una tarjeta.<br>· «Después» repite el mismo patrón de tarjeta con «Empezar» de contorno: 2 más antes de «Mis rutinas».<br>· Sin red salen a la vez el error del programa y el de las rutinas, y la biblioteca sigue enseñándose. |
| Nutrición | `MainActivity › NutricionFragment` | Tab de MainActivity 3 de 4. | [nutricion es claro 1,0](capturas/nutricion__es_claro_1-0.png)<br>[nutricion es oscuro 1,0](capturas/nutricion__es_oscuro_1-0.png)<br>[nutricion es oscuro 1,3](capturas/nutricion__es_oscuro_1-3.png)<br>[nutricion-error es oscuro 1,0](capturas/nutricion-error__es_oscuro_1-0.png) | · Tira de 7 días, píldora de entreno, tarjeta del día (anillo + 3 barras de macros) y tarjeta de agua: 3 bloques con 3 estilos de título distintos.<br>· La tarjeta del día lleva 5 líneas de texto bajo «Proteínas» (objetivo, mínimo, base de cálculo, «Te faltan») antes de carbohidratos.<br>· Sin red el anillo queda vacío y sin cifras y no sale ningún mensaje ni botón de reintento. |
| Progreso: Récords | `MainActivity › ProgresoFragment › RecordsFragment` | Tab de MainActivity 4 de 4 (sección «Récords»). | [progreso-records es claro 1,0](capturas/progreso-records__es_claro_1-0.png)<br>[progreso-records es oscuro 1,0](capturas/progreso-records__es_oscuro_1-0.png)<br>[progreso-records es oscuro 1,3](capturas/progreso-records__es_oscuro_1-3.png)<br>[progreso-error es oscuro 1,0](capturas/progreso-error__es_oscuro_1-0.png) | · Cabecera de perfil + 3 cifras + 4 secciones antes del primer dato: el primer récord queda por debajo de la mitad de la pantalla.<br>· Cada récord es una fila de 60 dp con trofeo, nombre, «hace 2 días · antes…» y marca: 4 elementos por fila.<br>· Sin red las cifras pasan a «—» y el cuerpo es un icono, un texto y «Reintentar». |
| Progreso: Logros | `ProgresoFragment › LogrosFragment` | Progreso › Logros. | [es oscuro 1,0](capturas/progreso-logros__es_oscuro_1-0.png) | · Tarjetas con icono, nombre, descripción y fecha; las que están en curso añaden una barra y «13 de 30».<br>· El dorado se usa en las dos conseguidas y el borde de la barra en las que no: 2 acentos en una lista. |
| Progreso: Medidas | `ProgresoFragment › MedidasFragment` | Progreso › Medidas. | [es oscuro 1,0](capturas/progreso-medidas__es_oscuro_1-0.png) | · Una gráfica con 3 selectores de serie y 3 de rango (6 chips) dentro de la misma tarjeta. |
| Progreso: Historial | `ProgresoFragment › HistorialFragment` | Progreso › Historial. | [es oscuro 1,0](capturas/progreso-historial__es_oscuro_1-0.png) | · Tarjetas de sesión con fecha, rutina, papelera y duración: la papelera de cada tarjeta compite con el toque que abre el resumen. |
| Ajustes | `AjustesActivity` | Progreso › engranaje. | [ajustes es claro 1,0](capturas/ajustes__es_claro_1-0.png)<br>[ajustes es oscuro 1,0](capturas/ajustes__es_oscuro_1-0.png)<br>[ajustes es oscuro 1,3](capturas/ajustes__es_oscuro_1-3.png)<br>[dialogo-tema es oscuro 1,0](capturas/dialogo-tema__es_oscuro_1-0.png)<br>[dialogo-idioma es oscuro 1,0](capturas/dialogo-idioma__es_oscuro_1-0.png)<br>[dialogo-vaso es oscuro 1,0](capturas/dialogo-vaso__es_oscuro_1-0.png)<br>[dialogo-descanso-sin-pauta es oscuro 1,0](capturas/dialogo-descanso-sin-pauta__es_oscuro_1-0.png)<br>[dialogo-cerrar-sesion es oscuro 1,0](capturas/dialogo-cerrar-sesion__es_oscuro_1-0.png) | · Seis grupos con rótulo (Cuenta, Tus datos, Entrenamiento, Preferencias, Ayuda, Legal) y 22 filas con id.<br>· Las filas mezclan 4 patrones: valor a la derecha, interruptor, botón («Activar») y enlace externo.<br>· Los diálogos de Tema, Idioma, Vaso y Descanso usan 2 estilos distintos de lista (radio y filas con icono). |
| Editar perfil | `EditarPerfilActivity` | Ajustes › Peso, altura y edad / Sexo / Objetivo / Nivel. | [es claro 1,0](capturas/editar-perfil__es_claro_1-0.png)<br>[es oscuro 1,0](capturas/editar-perfil__es_oscuro_1-0.png)<br>[es oscuro 1,3](capturas/editar-perfil__es_oscuro_1-3.png) | · Seis campos y cuatro selectores en una sola columna sin separar por grupos.<br>· «Guardar cambios» queda cortado por el borde inferior en la captura (la lista pasa por debajo del botón). |
| Cambiar contraseña | `CambiarPasswordActivity` | Ajustes › Contraseña. | [es oscuro 1,0](capturas/cambiar-password__es_oscuro_1-0.png) | · Dos campos y un botón: más de la mitad inferior está vacía. |
| Cambiar correo | `CambiarCorreoActivity` | Ajustes › Correo. | [es oscuro 1,0](capturas/cambiar-correo__es_oscuro_1-0.png) | · Dos campos y un botón; el apoyo «Para cambiar el correo hace falta tu contraseña» va en letra pequeña bajo el segundo. |
| Eliminar cuenta | `EliminarCuentaActivity` | Ajustes › Eliminar cuenta. | [es oscuro 1,0](capturas/eliminar-cuenta__es_oscuro_1-0.png) | · Un aviso rojo, dos tarjetas con 7 y 4 viñetas y el campo de contraseña: más de una pantalla de texto antes de la acción. |
| Notificaciones | `NotificacionesActivity` | Ajustes › Notificaciones. | [es oscuro 1,0](capturas/notificaciones__es_oscuro_1-0.png) | · Tarjeta de aviso, rótulo, 4 filas con interruptor y un enlace a Ajustes de Android: 3 patrones distintos en 6 elementos. |
| Acerca de | `AcercaDeActivity` | Ajustes › Acerca de GymProFit. | [es oscuro 1,0](capturas/acerca-de__es_oscuro_1-0.png) | · Logo, nombre, versión y lema; debajo, dos tarjetas de texto largo (descripción y lista de funcionalidades). |
| Licencias de terceros | `LicenciasActivity` | Ajustes › Licencias de terceros. | [es oscuro 1,0](capturas/licencias__es_oscuro_1-0.png) | · Rótulo, párrafo y 3 filas de bibliotecas, luego «Datos» y más filas: las filas de bibliotecas y las de datos comparten estilo con el texto de apoyo, casi del mismo tamaño. |
| Texto de una licencia | `LicenciaActivity` | Licencias › una fila. | [es oscuro 1,0](capturas/licencia-texto__es_oscuro_1-0.png) | · Texto plano largo, sin formato, de lado a lado de la pantalla. |
| Biblioteca de ejercicios | `EjerciciosActivity` | Entrenar › Biblioteca (buscador o grupo). | [ejercicios es oscuro 1,0](capturas/ejercicios__es_oscuro_1-0.png)<br>[ejercicios-sin-resultados es oscuro 1,0](capturas/ejercicios-sin-resultados__es_oscuro_1-0.png)<br>[ejercicios-error es oscuro 1,0](capturas/ejercicios-error__es_oscuro_1-0.png) | · Buscador, 5 chips de grupo y tarjetas con foto, nombre, descripción en 2 líneas y 2 chips: 4 elementos por tarjeta.<br>· Sin resultados y sin red el vacío es un texto de una línea («No hay nada aún») sin acción ni icono; el error no lleva botón. |
| Detalle de ejercicio | `DetalleEjercicioActivity` | Biblioteca o rutina › un ejercicio. | [es oscuro 1,0](capturas/detalle-ejercicio__es_oscuro_1-0.png) | · La demostración (imagen vacía) es un bloque blanco a todo el ancho sobre fondo oscuro: lo más luminoso de la pantalla.<br>· Músculo y Nivel en una tarjeta con rayas partidas y la gráfica de progresión debajo. |
| Programas | `ProgramasActivity` | Entrenar › Programa; Inicio › Elige tu programa. | [programas es oscuro 1,0](capturas/programas__es_oscuro_1-0.png)<br>[programas-error es oscuro 1,0](capturas/programas-error__es_oscuro_1-0.png) | · Dos filas de chips (dónde, días) y 5 tarjetas de programa; «Lo sigues» es una etiqueta pequeña dentro de la tarjeta.<br>· Sin red desaparecen las tarjetas y queda un texto con «Reintentar» pero los filtros siguen activos. |
| Detalle de programa | `ProgramaDetalleActivity` | Programas › un programa. | [programa-detalle es oscuro 1,0](capturas/programa-detalle__es_oscuro_1-0.png)<br>[hoja-seguir-programa es oscuro 1,0](capturas/hoja-seguir-programa__es_oscuro_1-0.png)<br>[hoja-menu-programa es oscuro 1,0](capturas/hoja-menu-programa__es_oscuro_1-0.png) | · Título, párrafo de 9 líneas, ciclo numerado y rutinas desplegables antes del botón fijo «Seguir este programa».<br>· La hoja de «¿Cuánto tiempo tienes por sesión?» repite las duraciones del programa en dos sitios (chips y lista) sin diferencia visible. |
| Detalle de rutina | `DetalleRutinaActivity` | Entrenar › una rutina. | [es oscuro 1,0](capturas/detalle-rutina__es_oscuro_1-0.png) | · Cabecera con nombre, nivel y duración (sin descripción) y 7 tarjetas de ejercicio con 3 chips cada una. |
| Editar rutina | `EditarRutinaActivity` | Detalle de rutina › menú › Editar. | [es oscuro 1,0](capturas/editar-rutina__es_oscuro_1-0.png) | · 4 campos, un grupo de chips y la lista de ejercicios con una ✕ por tarjeta; «Guardar cambios» fijo abajo. |
| Crear rutina | `CrearRutinaActivity` | Entrenar › + / Inicio › Crea tu primera rutina. | [es oscuro 1,0](capturas/crear-rutina__es_oscuro_1-0.png) | · Tres campos, un selector de nivel y un botón «Siguiente»; casi la mitad inferior está vacía. |
| Añadir ejercicios | `AnadirEjerciciosActivity` | Crear/Editar rutina; sesión en vivo › Añadir ejercicio. | [es oscuro 1,0](capturas/anadir-ejercicios__es_oscuro_1-0.png) | · Buscador, 4 chips de nivel y tarjetas de ejercicio con foto, descripción y 2 chips; «Continuar (0)» fijo abajo. |
| Resumen de rutina nueva | `ResumenCrearRutinaActivity` | Añadir ejercicios › Continuar. | [es oscuro 1,0](capturas/resumen-crear-rutina__es_oscuro_1-0.png) | · Con 0 ejercicios la pantalla enseña «Ejercicios (0)» y más de la mitad de la pantalla vacía y sin mensaje. |
| Entrenamiento hecho (apuntar) | `RegistrarSesionActivity` | Acciones rápidas › Apuntar un entrenamiento hecho. | [es oscuro 1,0](capturas/registrar-sesion__es_oscuro_1-0.png) | · Cinco campos y una valoración de estrellas en una columna; el rótulo «Valoración (opcional)» y «Sin valorar» repiten el mismo estado. |
| Sesión en vivo | `SesionEnVivoActivity` | Entrenar o Inicio › Empezar; Acciones rápidas. | [sesion-en-vivo es oscuro 1,0](capturas/sesion-en-vivo__es_oscuro_1-0.png)<br>[hoja-terminar-sesion es oscuro 1,0](capturas/hoja-terminar-sesion__es_oscuro_1-0.png)<br>[dialogo-descartar-sesion es oscuro 1,0](capturas/dialogo-descartar-sesion__es_oscuro_1-0.png)<br>[hoja-elegir-rutina es oscuro 1,0](capturas/hoja-elegir-rutina__es_oscuro_1-0.png) | · Cabecera naranja «Terminar», 3 cifras de resumen y tarjetas de ejercicio con 3 columnas de datos (anterior, kg, reps) y un círculo por serie.<br>· La hoja de terminar apila 4 bloques (duración, series, «Seguir entrenando», «Descartar») sobre la sesión atenuada.<br>· «Descartar la sesión» aparece en 3 sitios (hoja, diálogo y botón de texto) con rojo, naranja o texto. |
| Resumen del entreno | `ResumenSesionActivity` | Fin de la sesión; Historial; Inicio › Hecho hoy. | [resumen-sesion-hoy es claro 1,0](capturas/resumen-sesion-hoy__es_claro_1-0.png)<br>[resumen-sesion-hoy es oscuro 1,0](capturas/resumen-sesion-hoy__es_oscuro_1-0.png)<br>[resumen-sesion-anterior es oscuro 1,0](capturas/resumen-sesion-anterior__es_oscuro_1-0.png)<br>[resumen-sesion-error es oscuro 1,0](capturas/resumen-sesion-error__es_oscuro_1-0.png) | · Cifra de 72 sp, 3 fichas, tarjeta «Frente a la última vez» (hasta 7 filas) y «Para recuperar»: 4 bloques tras la cifra, en el orden del lienzo.<br>· Con letra 1,3 las fichas saltan de línea y quedan alineadas a la izquierda.<br>· Sin red, el cuerpo es el texto de error y «Reintentar»; la cabecera se mantiene. |
| Tu semana de nutrición | `SemanaNutricionActivity` | Nutrición › gráfica de la cabecera; tarjeta de los lunes. | [semana-nutricion es claro 1,0](capturas/semana-nutricion__es_claro_1-0.png)<br>[semana-nutricion es oscuro 1,0](capturas/semana-nutricion__es_oscuro_1-0.png)<br>[semana-nutricion-vacia es oscuro 1,0](capturas/semana-nutricion-vacia__es_oscuro_1-0.png)<br>[semana-nutricion-error es oscuro 1,0](capturas/semana-nutricion-error__es_oscuro_1-0.png) | · 5 tarjetas con rótulo de 13 sp; dos gráficas de barras (proteína 120 dp, calorías 80 dp) con su raya de objetivo.<br>· Con 0 días conseguidos las cifras van en el color de texto, no en dorado ni en verde.<br>· Vacío y error se enseñan bajo el selector de semana, con su texto y «Reintentar». |
| Estadísticas de nutrición | `EstadisticasNutricionActivity` | Tu semana › Ver semanas y meses. | [estadisticas-nutricion es oscuro 1,0](capturas/estadisticas-nutricion__es_oscuro_1-0.png)<br>[estadisticas-nutricion-error es oscuro 1,0](capturas/estadisticas-nutricion-error__es_oscuro_1-0.png) | · Dos chips, selector de periodo, 4 cifras en tarjetas de 2 × 2 y 2 gráficas en la primera pantalla.<br>· Sin datos cada gráfica pone su propio «Aún no has registrado comidas» (el mismo texto 2 veces). |
| Comida | `ComidaActivity` | Nutrición › una comida (fila o «+»). | [comida es oscuro 1,0](capturas/comida__es_oscuro_1-0.png)<br>[comida-vacia es oscuro 1,0](capturas/comida-vacia__es_oscuro_1-0.png)<br>[comida-error es oscuro 1,0](capturas/comida-error__es_oscuro_1-0.png) | · Tarjeta de resumen con anillo y 3 macros, barra de kcal y la lista de 5 alimentos con 3 datos cada uno; «Añadir alimento» y escáner fijos abajo.<br>· La comida vacía añade una tarjeta «¿Copiar la de ayer?» y 3 sugerencias: 3 bloques con 3 estilos de tarjeta.<br>· Sin red solo queda un texto con «Reintentar» y los botones de abajo siguen activos. |
| Añadir alimento | `AnadirAlimentoActivity` | Comida › Añadir alimento; Inicio › Registrar comida. | [anadir-alimento es oscuro 1,0](capturas/anadir-alimento__es_oscuro_1-0.png)<br>[anadir-alimento-favoritos es oscuro 1,0](capturas/anadir-alimento-favoritos__es_oscuro_1-0.png)<br>[anadir-alimento-mis-platos es oscuro 1,0](capturas/anadir-alimento-mis-platos__es_oscuro_1-0.png)<br>[anadir-alimento-buscando es oscuro 1,0](capturas/anadir-alimento-buscando__es_oscuro_1-0.png)<br>[anadir-alimento-error es oscuro 1,0](capturas/anadir-alimento-error__es_oscuro_1-0.png)<br>[hoja-elegir-comida es oscuro 1,0](capturas/hoja-elegir-comida__es_oscuro_1-0.png) | · Cabecera con ✕, título y etiqueta de comida (3 piezas), buscador, escáner y 3 pestañas antes de la primera fila.<br>· «Mis platos» apila «Lo que te encaja» (3 tarjetas con barra), «Mis platos» y «Alimentos que has usado»: 3 secciones con 3 estilos de rótulo.<br>· La ✕ de la cabecera y la flecha de las demás pantallas son dos formas de «volver» distintas. |
| Ficha de alimento | `FichaAlimentoActivity` | Añadir alimento › una fila. | [es oscuro 1,0](capturas/ficha-alimento__es_oscuro_1-0.png) | · Icono, nombre, 4 cifras de macros, barra de reparto, «Cómo encaja en tu día», selector de tabla y de cantidad: 6 bloques antes del botón fijo. |
| Crear alimento | `CrearAlimentoActivity` | Añadir alimento › crear. | [es oscuro 1,0](capturas/crear-alimento__es_oscuro_1-0.png) | · Nombre, marca, categorías (chips en fila desplazable), 6 campos de etiqueta y dos botones; la fila de categorías se corta por el borde derecho. |
| Crear plato | `CrearPlatoActivity` | Mis platos › Crear un plato. | [es oscuro 1,0](capturas/crear-plato__es_oscuro_1-0.png) | · Nombre, ingredientes, raciones, peso y «El plato entero» (4 cifras): 4 bloques con 3 estilos de cabecera. |
| Escáner | `EscanerActivity` | Añadir alimento › escáner. | [es oscuro 1,0](capturas/escaner__es_oscuro_1-0.png) | · Cámara a pantalla completa con aviso arriba, marco al centro y «Escribir el código» abajo: 3 elementos sobre la imagen. |
| Hoja de acciones rápidas | `MainActivity (hoja del «+»)` | El «+» de la barra inferior. | [es oscuro 1,0](capturas/hoja-acciones-rapidas__es_oscuro_1-0.png) | · 4 filas de 60 dp con icono, título y subtítulo, sobre la pantalla atenuada.<br>· El «+» central se gira a ✕ y queda encima de la hoja. |
| Calendario de Nutrición | `NutricionFragment (diálogo de fecha)` | Nutrición › calendario de la cabecera. | [es oscuro 1,0](capturas/dialogo-calendario__es_oscuro_1-0.png) | · Selector de fecha de Material en el tema oscuro, sin personalizar los colores de la app más allá del día elegido. |

## Lo que no se pudo capturar, y por qué

| Pantalla | Motivo |
|---|---|
| AdminActivity, AdminUsuariosActivity, AdminRutinasActivity, AdminEjerciciosActivity, AdminAlimentosActivity, EditarRutinaAdminActivity, EditarEjercicioAdminActivity | Solo las abre una cuenta ADMIN y la cuenta sembrada es USER. No se ha usado la cuenta admin local para no mezclar cuentas en las capturas. |
| MainActivity | Es el contenedor de las cuatro pestañas y su barra inferior: está en Inicio, Entrenar, Nutrición y Progreso. |
| Estado de error de Entrar | Con usuario y clave erróneos el campo se vacía y la captura no enseña ningún mensaje: no se ha podido ver cómo avisa. |
| Diálogos de cantidad, de duración y de editar alimento/ejercicio/rutina, y el diálogo de «Ya tienes una sesión en curso» | Cada uno exige un estado concreto (editar una línea, ajustar una duración) que no se puede provocar sin escribir en la cuenta sembrada o sin tocar código; el de «sesión en curso» sí salió en la prueba pero no se capturó. |
| Estados vacío de Historial, Récords y Medidas | La cuenta sembrada tiene datos en los tres; para ver su vacío haría falta otra cuenta. |
| Foto de perfil, permisos del sistema y selector de archivos | Son pantallas del sistema, no de la app. |

## Medidas de layouts, drawables y estilos (GP-216)

Generado por `scripts/medir-layouts.py` sobre `res/` (layout, drawable, values, menu, xml, animator).
«a mano» = un literal escrito en el XML; «dimens.xml» = referencia `@dimen/…`; «tema» = `?attr/…`.

### Resumen

| Propiedad | Valores distintos | Usos | De `dimens.xml` | A mano | Del tema / otros |
|---|---|---|---|---|---|
| padding | 24 (23 a mano) | 952 | 1 | 951 | 0 |
| margin | 27 (27 a mano) | 907 | 0 | 907 | 0 |
| cornerRadius | 20 (20 a mano) | 370 | 0 | 370 | 0 |
| elevation | 5 (5 a mano) | 28 | 0 | 28 | 0 |
| textSize | 24 (10 a mano) | 601 | 577 | 24 | 0 |

### padding

| Valor | Origen | Veces | Dónde (primeros usos) |
|---|---|---|---|
| `0dp` | a mano | 3 | `layout/activity_anadir_alimento.xml:124`, `layout/activity_eliminar_cuenta.xml:245`, `layout/view_copiar_ayer_diario.xml:100` |
| `2dp` | a mano | 4 | `layout/item_ejercicio_pauta.xml:49`, `layout/item_ejercicio_pauta.xml:50`, `layout/item_programa.xml:44`, `layout/item_programa.xml:45` |
| `3dp` | a mano | 1 | `layout/activity_crear_alimento.xml:257` |
| `4dp` | a mano | 171 | `layout/activity_acerca_de.xml:263`, `layout/activity_acerca_de.xml:293`, `layout/activity_acerca_de.xml:326`, `layout/activity_acerca_de.xml:358` … (+167) |
| `6dp` | a mano | 26 | `layout/activity_ajustes.xml:71`, `layout/activity_ajustes.xml:72`, `layout/activity_ajustes.xml:73`, `layout/activity_ajustes.xml:96` … (+22) |
| `7dp` | a mano | 1 | `layout/item_envivo_serie.xml:26` |
| `8dp` | a mano | 126 | `layout/activity_admin_alimentos.xml:28`, `layout/activity_admin_alimentos.xml:29`, `layout/activity_admin_alimentos.xml:30`, `layout/activity_admin_ejercicios.xml:28` … (+122) |
| `10dp` | a mano | 27 | `layout/activity_bienvenida.xml:42`, `layout/activity_crear_alimento.xml:237`, `layout/activity_crear_alimento.xml:449`, `layout/chart_marker.xml:11` … (+23) |
| `11dp` | a mano | 1 | `layout/item_envivo_serie.xml:25` |
| `12dp` | a mano | 111 | `layout/activity_admin_alimentos.xml:51`, `layout/activity_admin_alimentos.xml:52`, `layout/activity_admin_alimentos.xml:100`, `layout/activity_admin_ejercicios.xml:40` … (+107) |
| `13dp` | a mano | 1 | `layout/view_barra_navegacion.xml:48` |
| `14dp` | a mano | 57 | `layout/activity_anadir_alimento.xml:230`, `layout/activity_avisos.xml:45`, `layout/activity_avisos.xml:46`, `layout/activity_bienvenida.xml:27` … (+53) |
| `16dp` | a mano | 284 | `layout/activity_acerca_de.xml:31`, `layout/activity_admin.xml:31`, `layout/activity_admin.xml:61`, `layout/activity_admin.xml:98` … (+280) |
| `17dp` | a mano | 2 | `layout/view_descanso.xml:28`, `layout/view_descanso.xml:30` |
| `18dp` | a mano | 8 | `layout/activity_comida.xml:89`, `layout/activity_detalle_rutina.xml:44`, `layout/activity_escaner.xml:158`, `layout/item_rutina.xml:24` … (+4) |
| `20dp` | a mano | 60 | `layout/activity_acerca_de.xml:85`, `layout/activity_acerca_de.xml:229`, `layout/activity_avisos.xml:100`, `layout/activity_avisos.xml:101` … (+56) |
| `24dp` | a mano | 50 | `layout/activity_acerca_de.xml:39`, `layout/activity_ajustes.xml:35`, `layout/activity_anadir_alimento.xml:212`, `layout/activity_comida.xml:411` … (+46) |
| `26dp` | a mano | 2 | `layout/fragment_nutricion.xml:205`, `layout/fragment_nutricion.xml:206` |
| `28dp` | a mano | 4 | `layout/activity_acerca_de.xml:40`, `layout/activity_anadir_alimento.xml:266`, `layout/activity_anadir_alimento.xml:267`, `layout/dialog_loading.xml:16` |
| `32dp` | a mano | 9 | `layout/activity_guarda_plan.xml:349`, `layout/activity_guarda_plan.xml:350`, `layout/activity_semana_nutricion.xml:43`, `layout/dialog_loading.xml:15` … (+5) |
| `44dp` | a mano | 1 | `values/themes.xml:317` |
| `56dp` | a mano | 1 | `layout/activity_anadir_alimento.xml:68` |
| `80dp` | a mano | 1 | `layout/fragment_medidas.xml:77` |
| `@dimen/envivo_hueco_descanso` = 160dp | dimens.xml | 1 | `layout/activity_sesion_en_vivo.xml:221` |

### margin

| Valor | Origen | Veces | Dónde (primeros usos) |
|---|---|---|---|
| `-16dp` | a mano | 2 | `layout/activity_crear_alimento.xml:205`, `layout/activity_crear_alimento.xml:206` |
| `-10dp` | a mano | 1 | `layout/activity_crear_alimento.xml:120` |
| `-2dp` | a mano | 1 | `layout/view_barra_navegacion.xml:45` |
| `-1dp` | a mano | 3 | `layout/view_descanso.xml:21`, `layout/view_descanso.xml:22`, `layout/view_descanso.xml:23` |
| `0dp` | a mano | 6 | `layout/activity_comida.xml:163`, `layout/activity_eliminar_cuenta.xml:128`, `layout/activity_eliminar_cuenta.xml:176`, `layout/activity_eliminar_cuenta.xml:218` … (+2) |
| `1dp` | a mano | 5 | `layout/item_envivo_serie.xml:20`, `layout/item_envivo_serie.xml:21`, `layout/item_primer_paso.xml:42`, `layout/item_record.xml:71` … (+1) |
| `2dp` | a mano | 66 | `layout/activity_comida.xml:132`, `layout/activity_crear_alimento.xml:207`, `layout/activity_crear_alimento.xml:277`, `layout/activity_eliminar_cuenta.xml:50` … (+62) |
| `3dp` | a mano | 2 | `layout/view_demo_bienvenida.xml:54`, `values/themes.xml:432` |
| `4dp` | a mano | 57 | `layout/activity_acerca_de.xml:60`, `layout/activity_acerca_de.xml:68`, `layout/activity_admin_alimentos.xml:42`, `layout/activity_anadir_alimento.xml:46` … (+53) |
| `5dp` | a mano | 3 | `layout/fragment_paso_plan.xml:173`, `layout/item_dia_semana.xml:43`, `layout/item_dia_semana.xml:50` |
| `6dp` | a mano | 85 | `layout/activity_acerca_de.xml:240`, `layout/activity_acerca_de.xml:294`, `layout/activity_acerca_de.xml:327`, `layout/activity_admin.xml:52` … (+81) |
| `7dp` | a mano | 1 | `layout/view_macro_plan.xml:29` |
| `8dp` | a mano | 158 | `layout/activity_acerca_de.xml:132`, `layout/activity_acerca_de.xml:147`, `layout/activity_acerca_de.xml:162`, `layout/activity_acerca_de.xml:177` … (+154) |
| `9dp` | a mano | 1 | `values/themes.xml:459` |
| `10dp` | a mano | 80 | `layout/activity_acerca_de.xml:112`, `layout/activity_acerca_de.xml:124`, `layout/activity_acerca_de.xml:136`, `layout/activity_acerca_de.xml:151` … (+76) |
| `12dp` | a mano | 162 | `layout/activity_acerca_de.xml:96`, `layout/activity_acerca_de.xml:97`, `layout/activity_acerca_de.xml:250`, `layout/activity_admin.xml:39` … (+158) |
| `14dp` | a mano | 83 | `layout/activity_acerca_de.xml:111`, `layout/activity_acerca_de.xml:249`, `layout/activity_alta.xml:24`, `layout/activity_comida.xml:343` … (+79) |
| `16dp` | a mano | 133 | `layout/activity_acerca_de.xml:79`, `layout/activity_acerca_de.xml:223`, `layout/activity_admin.xml:328`, `layout/activity_admin.xml:371` … (+129) |
| `18dp` | a mano | 5 | `layout/activity_guarda_plan.xml:375`, `layout/activity_guarda_plan.xml:395`, `layout/activity_semana_nutricion.xml:430`, `layout/dialog_loading.xml:31` … (+1) |
| `20dp` | a mano | 12 | `layout/activity_acerca_de.xml:52`, `layout/activity_eliminar_cuenta.xml:342`, `layout/activity_estadisticas_nutricion.xml:182`, `layout/activity_guarda_plan.xml:87` … (+8) |
| `22dp` | a mano | 1 | `layout/activity_ajustes.xml:164` |
| `24dp` | a mano | 32 | `layout/activity_admin.xml:215`, `layout/activity_avisos.xml:99`, `layout/activity_cambiar_correo.xml:91`, `layout/activity_cambiar_password.xml:86` … (+28) |
| `28dp` | a mano | 1 | `layout/activity_avisos.xml:35` |
| `32dp` | a mano | 2 | `layout/activity_crear_rutina.xml:152`, `layout/activity_splash.xml:41` |
| `36dp` | a mano | 1 | `layout/activity_escaner.xml:81` |
| `40dp` | a mano | 1 | `layout/activity_escaner.xml:101` |
| `54dp` | a mano | 3 | `layout/activity_detalle_ejercicio.xml:166`, `layout/activity_detalle_ejercicio.xml:207`, `layout/activity_detalle_ejercicio.xml:217` |

### cornerRadius

| Valor | Origen | Veces | Dónde (primeros usos) |
|---|---|---|---|
| `0dp` | a mano | 24 | `drawable/bg_grupo_media.xml:7`, `drawable/bg_grupo_media.xml:7`, `drawable/bg_grupo_media.xml:7`, `drawable/bg_grupo_media.xml:7` … (+20) |
| `2dp` | a mano | 5 | `drawable/bg_asa_hoja.xml:5`, `drawable/bg_demo_barra.xml:3`, `drawable/bg_demo_pista.xml:3`, `drawable/bg_marca_entreno.xml:5` … (+1) |
| `3dp` | a mano | 2 | `layout/fragment_inicio.xml:186`, `layout/item_logro.xml:91` |
| `4dp` | a mano | 8 | `drawable/bg_pista_barra.xml:5`, `drawable/bg_relleno_barra.xml:5`, `drawable/progress_bar_calorias.xml:16`, `drawable/progress_bar_calorias.xml:25` … (+4) |
| `5dp` | a mano | 2 | `drawable/bg_barra_redonda.xml:9`, `layout/activity_semana_nutricion.xml:388` |
| `6dp` | a mano | 1 | `layout/view_para_recuperar.xml:112` |
| `8dp` | a mano | 12 | `layout/item_serie.xml:52`, `layout/item_serie.xml:53`, `layout/item_serie.xml:54`, `layout/item_serie.xml:55` … (+8) |
| `10dp` | a mano | 3 | `drawable/bg_demo_chip.xml:3`, `drawable/bg_envivo_campo.xml:9`, `drawable/bg_envivo_campo.xml:15` |
| `12dp` | a mano | 177 | `drawable/bg_campo_cifra.xml:7`, `drawable/bg_campo_cifra.xml:14`, `drawable/bg_chip_zona_hecha.xml:5`, `drawable/bg_chip_zona_pendiente.xml:5` … (+173) |
| `13dp` | a mano | 1 | `drawable/bg_accion_tile.xml:5` |
| `14dp` | a mano | 27 | `drawable/bg_copiar_ayer.xml:6`, `drawable/bg_icono_categoria.xml:5`, `layout/activity_guarda_plan.xml:108`, `layout/activity_guarda_plan.xml:109` … (+23) |
| `16dp` | a mano | 27 | `drawable/bg_aviso_salud.xml:6`, `drawable/bg_campo_texto.xml:6`, `drawable/bg_campo_texto.xml:13`, `drawable/bg_cifra_progreso.xml:5` … (+23) |
| `17dp` | a mano | 2 | `drawable/bg_conmutador_lado.xml:6`, `drawable/bg_conmutador_lado.xml:12` |
| `18dp` | a mano | 4 | `drawable/bg_etiqueta_comida.xml:8`, `drawable/bg_grupo_ajustes.xml:5`, `drawable/bg_nueva_rutina.xml:7`, `values/themes.xml:236` |
| `19dp` | a mano | 8 | `drawable/bg_grupo_primera.xml:21`, `drawable/bg_grupo_primera.xml:21`, `drawable/bg_grupo_solo.xml:21`, `drawable/bg_grupo_solo.xml:21` … (+4) |
| `20dp` | a mano | 34 | `drawable/bg_boton_discontinuo.xml:7`, `drawable/bg_boton_discontinuo.xml:13`, `drawable/bg_conmutador.xml:5`, `drawable/bg_demo_tarjeta.xml:3` … (+30) |
| `22dp` | a mano | 4 | `drawable/bg_pildora_entreno.xml:6`, `layout/activity_admin.xml:53`, `layout/fragment_nutricion.xml:425`, `values/themes.xml:364` |
| `24dp` | a mano | 17 | `drawable/bg_campo_buscar.xml:7`, `drawable/bg_hoja_escaner.xml:9`, `drawable/bg_hoja_escaner.xml:9`, `drawable/bg_hoja_escaner.xml:15` … (+13) |
| `26dp` | a mano | 11 | `layout/activity_anadir_alimento.xml:90`, `layout/activity_anadir_alimento.xml:91`, `layout/activity_anadir_alimento.xml:92`, `layout/activity_anadir_alimento.xml:93` … (+7) |
| `50%` | a mano | 1 | `values/estilos_estructura.xml:200` |

### elevation

| Valor | Origen | Veces | Dónde (primeros usos) |
|---|---|---|---|
| `0dp` | a mano | 23 | `layout/activity_detalle_ejercicio.xml:38`, `layout/activity_detalle_ejercicio.xml:73`, `layout/activity_detalle_ejercicio.xml:120`, `layout/activity_detalle_ejercicio.xml:267` … (+19) |
| `1dp` | a mano | 1 | `values/themes.xml:237` |
| `4dp` | a mano | 1 | `layout/toast_custom.xml:18` |
| `8dp` | a mano | 2 | `layout/activity_anadir_alimento.xml:227`, `layout/view_demo_bienvenida.xml:152` |
| `12dp` | a mano | 1 | `values/themes.xml:441` |

### textSize

| Valor | Origen | Veces | Dónde (primeros usos) |
|---|---|---|---|
| `20sp` | a mano | 2 | `layout/item_rutina_plan.xml:47`, `layout/view_demo_bienvenida.xml:125` |
| `22sp` | a mano | 1 | `layout/view_demo_bienvenida.xml:180` |
| `24sp` | a mano | 3 | `layout/activity_anadir_alimento.xml:50`, `layout/activity_crear_plato.xml:317`, `layout/dialog_elegir_comida.xml:36` |
| `26sp` | a mano | 2 | `layout/activity_comida.xml:312`, `layout/activity_crear_plato.xml:546` |
| `28sp` | a mano | 5 | `layout/activity_comida.xml:51`, `layout/activity_crear_alimento.xml:45`, `layout/activity_crear_plato.xml:52`, `layout/fragment_paso_plan.xml:296` … (+1) |
| `30sp` | a mano | 1 | `layout/activity_comida.xml:127` |
| `34sp` | a mano | 1 | `values/estilos_alta.xml:27` |
| `36sp` | a mano | 2 | `layout/activity_guarda_plan.xml:380`, `layout/fragment_nutricion.xml:222` |
| `38sp` | a mano | 6 | `layout/activity_avisos.xml:103`, `layout/activity_bienvenida.xml:70`, `layout/activity_guarda_plan.xml:72`, `layout/activity_login.xml:43` … (+2) |
| `40sp` | a mano | 1 | `layout/fragment_nutricion.xml:261` |
| `@dimen/envivo_descanso_numero` = 46sp | dimens.xml | 1 | `layout/view_descanso.xml:75` |
| `@dimen/text_body` = 15sp | dimens.xml | 171 | `layout/activity_acerca_de.xml:62`, `layout/activity_acerca_de.xml:70`, `layout/activity_acerca_de.xml:104`, `layout/activity_acerca_de.xml:278` … (+167) |
| `@dimen/text_caption` = 14sp | dimens.xml | 120 | `layout/activity_acerca_de.xml:120`, `layout/activity_acerca_de.xml:137`, `layout/activity_acerca_de.xml:152`, `layout/activity_acerca_de.xml:167` … (+116) |
| `@dimen/text_display` = 26sp | dimens.xml | 15 | `layout/activity_bienvenida.xml:36`, `layout/activity_estadisticas_nutricion.xml:118`, `layout/activity_estadisticas_nutricion.xml:133`, `layout/activity_estadisticas_nutricion.xml:154` … (+11) |
| `@dimen/text_display_large` = 30sp | dimens.xml | 10 | `layout/activity_admin.xml:72`, `layout/activity_admin.xml:109`, `layout/activity_admin.xml:155`, `layout/activity_admin.xml:192` … (+6) |
| `@dimen/text_eyebrow` = 13sp | dimens.xml | 62 | `layout/activity_ajustes.xml:75`, `layout/activity_ajustes.xml:100`, `layout/activity_ajustes.xml:187`, `layout/activity_avisos.xml:74` … (+58) |
| `@dimen/text_fila` = 16sp | dimens.xml | 56 | `layout/activity_anadir_alimento.xml:247`, `layout/activity_avisos.xml:83`, `layout/activity_crear_alimento.xml:99`, `layout/activity_crear_alimento.xml:439` … (+52) |
| `@dimen/text_headline` = 22sp | dimens.xml | 16 | `layout/activity_detalle_ejercicio.xml:46`, `layout/activity_escaner.xml:62`, `layout/activity_ficha_alimento.xml:497`, `layout/activity_sesion_en_vivo.xml:74` … (+12) |
| `@dimen/text_label` = 14sp | dimens.xml | 54 | `layout/activity_admin.xml:80`, `layout/activity_admin.xml:117`, `layout/activity_admin.xml:163`, `layout/activity_admin.xml:200` … (+50) |
| `@dimen/text_number` = 44sp | dimens.xml | 3 | `layout/activity_semana_nutricion.xml:136`, `values/themes.xml:111`, `values/themes.xml:196` |
| `@dimen/text_number_gigante` = 72sp | dimens.xml | 1 | `layout/activity_resumen_sesion.xml:80` |
| `@dimen/text_number_hero` = 56sp | dimens.xml | 1 | `layout/dialog_cronometro.xml:32` |
| `@dimen/text_subtitle` = 17sp | dimens.xml | 53 | `layout/activity_admin.xml:330`, `layout/activity_admin.xml:373`, `layout/activity_admin.xml:416`, `layout/activity_admin.xml:459` … (+49) |
| `@dimen/text_title` = 19sp | dimens.xml | 14 | `layout/activity_estadisticas_nutricion.xml:89`, `layout/dialog_custom.xml:31`, `layout/dialog_editar_alimento.xml:31`, `layout/dialog_idioma.xml:21` … (+10) |

### Lo que no sale de `dimens.xml`

Los valores literales («a mano») de arriba, de más a menos usados, por propiedad:

| Propiedad | Valor | Veces |
|---|---|---|
| cornerRadius | `12dp` | 177 |
| cornerRadius | `20dp` | 34 |
| cornerRadius | `16dp` | 27 |
| cornerRadius | `14dp` | 27 |
| cornerRadius | `0dp` | 24 |
| cornerRadius | `24dp` | 17 |
| cornerRadius | `8dp` | 12 |
| cornerRadius | `26dp` | 11 |
| cornerRadius | `19dp` | 8 |
| cornerRadius | `4dp` | 8 |
| cornerRadius | `2dp` | 5 |
| cornerRadius | `18dp` | 4 |
| cornerRadius | `22dp` | 4 |
| cornerRadius | `10dp` | 3 |
| cornerRadius | `5dp` | 2 |
| cornerRadius | `17dp` | 2 |
| cornerRadius | `3dp` | 2 |
| cornerRadius | `13dp` | 1 |
| cornerRadius | `6dp` | 1 |
| cornerRadius | `50%` | 1 |
| elevation | `0dp` | 23 |
| elevation | `8dp` | 2 |
| elevation | `4dp` | 1 |
| elevation | `1dp` | 1 |
| elevation | `12dp` | 1 |
| margin | `12dp` | 162 |
| margin | `8dp` | 158 |
| margin | `16dp` | 133 |
| margin | `6dp` | 85 |
| margin | `14dp` | 83 |
| margin | `10dp` | 80 |
| margin | `2dp` | 66 |
| margin | `4dp` | 57 |
| margin | `24dp` | 32 |
| margin | `20dp` | 12 |
| margin | `0dp` | 6 |
| margin | `18dp` | 5 |
| margin | `1dp` | 5 |
| margin | `54dp` | 3 |
| margin | `5dp` | 3 |
| margin | `-1dp` | 3 |
| margin | `-16dp` | 2 |
| margin | `32dp` | 2 |
| margin | `3dp` | 2 |
| margin | `22dp` | 1 |
| margin | `28dp` | 1 |
| margin | `-10dp` | 1 |
| margin | `36dp` | 1 |
| margin | `40dp` | 1 |
| margin | `-2dp` | 1 |
| margin | `7dp` | 1 |
| margin | `9dp` | 1 |
| padding | `16dp` | 284 |
| padding | `4dp` | 171 |
| padding | `8dp` | 126 |
| padding | `12dp` | 111 |
| padding | `20dp` | 60 |
| padding | `14dp` | 57 |
| padding | `24dp` | 50 |
| padding | `10dp` | 27 |
| padding | `6dp` | 26 |
| padding | `32dp` | 9 |
| padding | `18dp` | 8 |
| padding | `28dp` | 4 |
| padding | `2dp` | 4 |
| padding | `0dp` | 3 |
| padding | `26dp` | 2 |
| padding | `17dp` | 2 |
| padding | `56dp` | 1 |
| padding | `3dp` | 1 |
| padding | `80dp` | 1 |
| padding | `11dp` | 1 |
| padding | `7dp` | 1 |
| padding | `13dp` | 1 |
| padding | `44dp` | 1 |
| textSize | `38sp` | 6 |
| textSize | `28sp` | 5 |
| textSize | `24sp` | 3 |
| textSize | `26sp` | 2 |
| textSize | `36sp` | 2 |
| textSize | `20sp` | 2 |
| textSize | `30sp` | 1 |
| textSize | `40sp` | 1 |
| textSize | `22sp` | 1 |
| textSize | `34sp` | 1 |

`dimens.xml` define: `envivo_col_serie` 40dp, `envivo_descanso_numero` 46sp, `envivo_hueco_descanso` 160dp, `text_body` 15sp, `text_caption` 14sp, `text_display` 26sp, `text_display_large` 30sp, `text_eyebrow` 13sp, `text_fila` 16sp, `text_headline` 22sp, `text_label` 14sp, `text_number` 44sp, `text_number_gigante` 72sp, `text_number_hero` 56sp, `text_subtitle` 17sp, `text_title` 19sp.

