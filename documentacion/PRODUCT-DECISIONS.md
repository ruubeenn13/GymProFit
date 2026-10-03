# GymProFit — Registro de decisiones de producto y arquitectura

Decisiones tomadas, con su porqué. Sirve para no volver a discutir lo ya discutido, y para saber **qué evidencia haría cambiar de idea** — una decisión sin condición de revisión acaba convertida en dogma.

**Reglas del archivo**

- Los IDs son estables y no se reutilizan. Una decisión que deja de valer se marca `Sustituida por DEC-xxx`; no se borra.
- Una entrada sale de **Pendientes de decidir** en el mismo momento en que se resuelve, y pasa al bloque de decisiones con su ID intacto. El encabezado de una sección es una afirmación: si ahí debajo queda algo ya resuelto, el archivo miente a quien lo escanea sin abrir cada entrada, que es justo como se lee esta sección.
- Cada entrada dice qué la invalidaría. Si nadie sabe contestar a eso, probablemente no era una decisión, era una preferencia.
- Aquí van decisiones **duraderas**. Las tareas están en el backlog, que vive fuera de este repositorio y es su única fuente de verdad.
- Si el código contradice una decisión de aquí, manda la decisión y el código es deuda. Las contradicciones conocidas están anotadas.

---

## Producto

### DEC-001 · GymProFit es un producto comercial
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** El proyecto nació como TFG de 2º DAM y el TFG ya terminó. Seguir tratándolo como trabajo académico justificaba atajos que un producto real no admite.

**Decisión.** GymProFit se desarrolla como producto comercial destinado a usuarios reales, con vocación de mantenerse durante años.

**Consecuencias.** Seguridad, privacidad, rendimiento, accesibilidad, observabilidad y preparación para Google Play dejan de ser extras y pasan a ser parte del trabajo. El material público del repositorio no debe presentarlo como ejercicio académico.

**Qué la invalidaría.** Nada previsible. Si se abandonara la idea de publicar, casi todo lo demás de este archivo dejaría de aplicar.

---

### DEC-002 · Simple por fuera, potente por dentro
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** El público objetivo va de principiantes a usuarios avanzados. Servir a los dos con la misma interfaz es el problema de diseño central del producto.

**Decisión.** La profundidad existe pero no se impone. Un principiante no se encuentra de golpe con toda la complejidad disponible; un usuario avanzado puede llegar a ella sin que eso complique la experiencia básica.

**Consecuencias.** Revelación progresiva como patrón por defecto. Una función nueva que obligue a todos a entenderla para usar lo básico está mal planteada, aunque sea buena.

**Qué la invalidaría.** Datos de uso que muestren que el producto solo retiene a un extremo del espectro y que intentar servir a los dos perjudica a ambos.

---

### DEC-003 · El core loop decide las prioridades
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** Una app todo-en-uno tiende a crecer por acumulación de funciones inconexas.

**Decisión.** El producto se organiza alrededor de ENTRENAR → REGISTRAR → ENTENDER → PROGRESAR → VOLVER. Toda función nueva se evalúa por lo que aporta a ese ciclo.

**Consecuencias.** Una función que no mejora ninguno de esos pasos tiene prioridad baja por definición, por buena que parezca aislada. Justifica decir que no.

**Qué la invalidaría.** Que el uso real muestre que el valor del producto está en otra parte —por ejemplo, que la gente lo use sobre todo para nutrición y el entrenamiento sea accesorio.

---

### DEC-004 · El entrenamiento no usa calorías estimadas como métrica
**Estado:** Aceptada · **Fecha:** 2026-09-18 · **Aplicada el 2026-09-23 (GP-010)**

**Contexto.** El producto calculaba las calorías de una sesión como `calorías × series × repeticiones`. Es un número sin fundamento fisiológico: no depende de la carga, ni del peso corporal, ni del descanso, ni de nada real.

**Decisión.** No se muestra ni se almacena ninguna estimación de calorías quemadas en entrenamiento. Si algún día se recupera, será con un modelo defendible y presentada como estimación, no como dato.

**Consecuencias.** Un número inventado con apariencia de dato es peor que no tener número: el usuario decide sobre él, y en nutrición puede llevarle a comer de más.

**No afecta a** las calorías de **nutrición**, que proceden de los alimentos registrados y son reales.

**Contradicción resuelta el 2026-09-23 (GP-010).** Hasta esa fecha el cálculo seguía vivo, el valor se persistía y se mostraba en varias pantallas. La auditoría de diseño del 21 de septiembre de 2026 ([AUDITORIA-DISENO-2026-09.md](AUDITORIA-DISENO-2026-09.md), P-13, P-19 y P-40) inventarió **dónde exactamente**, porque este apartado solo nombraba `RegistrarSesionActivity` y eso llevaba a creer que el problema estaba en un sitio. Lo que apareció fueron **tres grupos que se retiraban por separado**, y el tercero era el que nadie había mirado:

- `RegistrarSesionActivity` — el cálculo que originó la decisión.
- Resumen de sesión y Home — mostraban el valor persistido.
- **Lista de rutinas, detalle de rutina y ficha de ejercicio** — mostraban un valor que **no** venía de la sesión: lo calculaba la API al vuelo en un `@Formula` de Hibernate sobre `Rutina`, con `SUM(series × repeticiones × calorias_quemadas)`, que es **literalmente la fórmula del contexto de arriba**. Era una violación viva de esta decisión, en el servidor, sin inventariar.

Los números delataban solos la falta de fundamento: una rutina de movilidad de 35 minutos anunciaba ~2385 kcal, más que la ingesta diaria completa del usuario.

**Qué se retiró.** En la API: el `@Formula`, el campo de calorías de los DTO de rutina, el de la relación rutina-ejercicio, la exposición de `ejercicios.calorias_quemadas` (incluidos el endpoint de búsqueda por rango de calorías y el filtro `caloriasMax`), la escritura de `sesiones_entrenamiento.calorias_quemadas` —parámetro de `PUT /sesiones/{id}/completar` incluido—, `totalCaloriasQuemadas` de las estadísticas, las kcal del resumen semanal por notificación, y la tabla de kcal por grupo muscular que rellenaba el catálogo al importarlo. En Android: las siete pantallas del inventario.

**Las columnas NO se borran todavía.** `ejercicios.calorias_quemadas` y `sesiones_entrenamiento.calorias_quemadas` siguen en la base de datos: dejan de leerse y de escribirse, y se retiran en una migración posterior. Separar las dos cosas permite volver atrás sin perder el histórico mientras el cambio se asienta, y es lo que [DEC-025](#dec-025--destino-del-histórico-de-calorías-de-entrenamiento) tiene pendiente de decidir.

**Qué se puso en su lugar.** En la tarjeta de rutina, **nada**: una rutina es una plantilla, no tiene pesos, y el volumen ahí no significa nada; inventar otro número para rellenar el hueco habría repetido el error con otra cara. En el resumen de sesión, los kilos movidos ya son el número grande (GP-011). En Home se **quitó la columna**: el volumen de la semana no se podía poner porque la app agrega la semana en el cliente desde la lista de sesiones, y ahí no viene el peso movido.

**Lo que impide que vuelva.** `SinCaloriasEntrenamientoTest` afirma sobre el **JSON** —no sobre las clases Java— que ninguna de esas rutas devuelve el campo, y de paso que las calorías de nutrición siguen ahí. Un comentario no impide una reintroducción; un test que falla, sí.

**Qué la invalidaría.** Un modelo de gasto energético con respaldo, alimentado por datos que el producto realmente tenga (carga, tiempo bajo tensión, peso corporal, y frecuencia cardíaca si algún día llega por Health Connect).

---

### DEC-005 · El registro es por serie real
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** Antes se guardaba un único peso por ejercicio. Quien hacía 4×8 subiendo carga tenía que elegir un número y falsear el resto.

**Decisión.** Cada serie se registra con su peso, sus repeticiones reales y su marca de completada. Los datos derivados se calculan desde ahí, nunca al revés.

**Consecuencias.** Sin dato por serie no hay progresión de carga, ni récords, ni volumen. Es la base sobre la que se apoya todo lo demás del apartado de progreso. Las repeticiones se precargan desde la rutina como sugerencia editable: fallar la última serie es información, no un error que corregir.

**Qué la invalidaría.** Nada previsible. Es un requisito del dominio.

---

### DEC-006 · El volumen es la métrica de carga
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** Hacía falta una métrica honesta que sustituyera a la de DEC-004 y respondiera a "¿estoy mejorando?".

**Decisión.** El volumen, Σ(peso × repeticiones) sobre las series completadas, es la métrica principal de carga. Se calcula en el servidor desde las series reales.

**Consecuencias.** Una sola métrica bien explicada, no un muro de gráficas. Cuando no hay pesos registrados no se enseña un cero: se enseña un estado vacío honesto.

**Qué la invalidaría.** Que el uso muestre que los usuarios no la entienden ni la usan, y que otra métrica —tonelaje por grupo muscular, series efectivas, intensidad relativa— responda mejor a la misma pregunta.

---

### DEC-007 · Las referencias se estudian, no se copian
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** Hevy, Fitia y Symmetry resuelven bien partes del problema y son referencias legítimas.

**Decisión.** Se estudian sus flujos, sus soluciones y sus modelos de negocio. No se replican pantallas ni identidad visual. La oportunidad de GymProFit es unir experiencias que hoy están separadas, no ser una versión peor de una de ellas.

**Consecuencias.** "Hevy lo hace así" es un argumento para entender un problema, nunca una justificación por sí solo.

**Qué la invalidaría.** Nada. Copiar no deja de ser mala idea con el tiempo.

---

### DEC-008 · Free + Pro, sin bloquear el núcleo
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** El producto es gratuito hoy y debe poder monetizarse sin rehacerse.

**Decisión.** Modelo Free + Pro. La versión gratuita conserva valor real: entrenar, registrar y ver el progreso básico no se bloquean nunca. Lo que puede ser Pro es la profundidad —análisis avanzado, histórico largo, planificación, informes, integraciones.

**Consecuencias.** Primero se crea el valor, después se monetiza. Un paywall sobre el core loop mata la adopción antes de que haya nada que vender.

**Qué la invalidaría.** Que los costes de infraestructura hagan inviable el plan gratuito. Se ajustarían los límites, no se bloquearía el núcleo.

---

### DEC-009 · No se añade IA hasta que los datos sean fiables
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** Hay presión ambiental por añadir IA a cualquier producto.

**Decisión.** No se incorporan funciones de IA o personalización automática hasta que el registro sea fiable y las métricas honestas. La IA se apoyará en datos reales; no inventará métricas ni generará certezas médicas.

**Consecuencias.** Con datos que se pierden (ver backlog) y métricas inventadas (DEC-004), una capa de IA amplificaría el error y lo haría más creíble.

**Qué la invalidaría.** Tener histórico limpio y suficiente, y un problema concreto del core loop que la IA resuelva mejor que una regla simple.

---

## Arquitectura

### DEC-010 · No se migra a Compose en bloque
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** La app está en vistas XML con Java. Migrar a Compose es la recomendación por defecto del ecosistema.

**Decisión.** No se reescribe la app para modernizarla. Compose puede entrar de forma incremental en pantallas nuevas donde aporte valor medible.

**Consecuencias.** El problema real no es el framework: es no tener una fuente de verdad para el estado. Eso se arregla con ViewModel y repositorio en el camino crítico, sin cambiar de tecnología de vistas.

**Qué la invalidaría.** Que mantener las vistas XML empiece a costar de forma demostrable más que migrar, o que una función necesaria sea inviable sin Compose.

---

### DEC-011 · Monolito modular, no microservicios
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** "Escalar" se asocia por reflejo a repartir el backend en servicios.

**Decisión.** La API sigue siendo un monolito bien estructurado. Un servicio se separa solo cuando exista una razón medida: un cuello de botella real o una necesidad de despliegue independiente.

**Consecuencias.** La escalabilidad se ataca antes por donde duele de verdad —paginación, índices, consultas— que por topología de despliegue.

**Qué la invalidaría.** Un cuello de botella medido que no se resuelva dentro del monolito, o un equipo lo bastante grande como para que el despliegue conjunto sea el freno.

---

### DEC-012 · Las capacidades se consultan en un solo sitio
**Estado:** Aceptada · **Fecha:** 2026-09-18 · **Sin implementar todavía, a propósito**

**Contexto.** Consecuencia directa de DEC-008. La forma habitual de equivocarse es repartir `if premium` por todo el código.

**Decisión.** Cuando llegue la primera función Pro, el acceso se resolverá con una capa central de capacidades y entitlements. La app pregunta "¿puede el usuario hacer X?", no "¿es premium?".

**Consecuencias.** Añadir un nivel nuevo, una promoción o un periodo de prueba no obliga a tocar pantallas. **Esta capa debe existir antes de la primera función Pro, no después.**

**Qué la invalidaría.** Que se descarte definitivamente monetizar.

---

### DEC-013 · La identidad sale siempre del token
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** La API expone muchas rutas que reciben un `usuarioId` o un id de recurso.

**Decisión.** El usuario autenticado se deriva del token, nunca de lo que envía el cliente. Al crear, el `usuarioId` se sobrescribe con el del token; al leer o modificar, se comprueba la propiedad. Un USER accede solo a lo suyo; un ADMIN, a todo, de forma explícita.

**Consecuencias.** Es el patrón ya construido en `SecurityUtils` y aplicado en la mayoría de servicios. Los huecos que quedan son deuda, no excepciones.

**Qué la invalidaría.** Que se añadan funciones sociales con contenido compartido a propósito. Eso se diseñaría como excepción explícita y consentida, no relajando la regla.

---

### DEC-014 · Toda ruta con id de recurso nace con su test de acceso
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Contexto.** Los fallos de autorización encontrados en la auditoría estaban exactamente en las rutas que la suite de tests no cubría. No fue casualidad.

**Decisión.** Una ruta que reciba un id de recurso no se da por terminada sin un test que compruebe que un usuario ajeno recibe 403. Los tests de controlador con el servicio simulado no cuentan: hay que levantar el contexto para que la comprobación de propiedad se ejecute de verdad.

**Consecuencias.** Es la regla que impide que la clase de fallo vuelva. `AbstractOwnershipTest` ya da la base.

**Qué la invalidaría.** Nada.

---

### DEC-015 · El rol nunca viene del cliente
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Decisión.** El registro público crea siempre USER. Los cambios de rol se hacen solo desde el panel de administración. Un administrador no puede aplicarse a sí mismo operaciones destructivas del panel —desactivarse o bajarse de rol— porque es el único daño irreversible desde la app.

**Consecuencias.** La barrera vive en el servidor. La del cliente es ayuda visual, nunca la protección.

**Qué la invalidaría.** Nada.

---

### DEC-016 · Los secretos no entran en el repositorio, y producción no tiene valores por defecto
**Estado:** Aceptada · **Fecha:** 2026-09-18 · **Deuda conocida**

**Contexto.** El perfil de producción ya sigue 12-factor para la base de datos y el JWT. Pero una comodidad de desarrollo que también se aplica en producción es una vulnerabilidad, no una comodidad.

**Decisión.** Ninguna credencial en el repositorio. En el perfil `prod`, toda variable sensible es obligatoria y **sin valor por defecto**: si falta, la aplicación no arranca. Un fallback silencioso en producción está prohibido.

**Consecuencias.** Arrancar y fallar es preferible a arrancar y degradarse sin que nadie se entere.

**Contradicciones actuales.** Ninguna en el código. Las dos que había —usuarios semilla creados en producción con contraseña versionada, y `spring.mail.host` con valor por defecto vacío en `prod`, que hacía que la recuperación de contraseña escribiera el código en el log en vez de enviarlo— se cerraron el 2026-09-19 (`f1a4841`). La contraseña del admin sale de `ADMIN_PASSWORD` y sin ella no se crea ninguna cuenta ADMIN; las variables de correo (`BREVO_API_KEY` y `MAIL_FROM`) van sin default y su ausencia impide arrancar; y el código de recuperación **no se escribe en el log en ningún perfil** —fuera de producción se entrega en `target/mail-outbox`—. Queda una contradicción **fuera del código**: las credenciales del seed antiguo siguen vivas en la base de datos de producción y hay que rotarlas a mano, porque definir `ADMIN_PASSWORD` no cambia la contraseña de un usuario que ya existe.

**Qué la invalidaría.** Nada.

---

### DEC-017 · Un build de desarrollo no puede apuntar a producción
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Decisión.** El build de desarrollo solo acepta URLs de red local y **falla la compilación** si se le da otra cosa. La URL de producción vive únicamente en el build de release.

**Consecuencias.** Ya implementado en `app/build.gradle`. Impide escribir en los datos reales de usuarios desde un emulador sin que nadie se dé cuenta.

**Qué la invalidaría.** Nada. Si hiciera falta un entorno de pruebas remoto, se añadiría como entorno propio, no relajando la guarda.

---

## Calidad y diseño

### DEC-018 · Oscuro por defecto, naranja de marca, dorado para lo conseguido
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Decisión.** Tema oscuro por defecto con tema claro completo. Naranja como acento único en ambos temas, ajustado para cumplir contraste. Dorado reservado a récords y logros. Se evita la estética gaming, el exceso de efectos y los emojis usados como iconos.

**Consecuencias.** El acento no cambia entre temas: la identidad es la misma a cualquier hora. El dorado significa algo precisamente porque no se usa para nada más.

**Qué la invalidaría.** Un rediseño de marca deliberado.

---

### DEC-019 · La accesibilidad es parte de terminado
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Decisión.** Todo elemento interactivo tiene nombre accesible, área táctil suficiente y comunica su estado. Nada depende solo del color. **Toda vista dibujada a mano nace con su accesibilidad**: es donde se ha fallado dos veces.

**Consecuencias.** Una pantalla sin accesibilidad no está terminada, aunque funcione. Play lo señala en el informe previo al lanzamiento.

**Qué la invalidaría.** Nada.

---

### DEC-020 · ES y EN siempre, sin texto a fuego
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Decisión.** Toda cadena visible va en recursos, en español e inglés. Los contadores usan plurales. Números, fechas y unidades respetan el idioma.

**Consecuencias.** La paridad está al 100% y mantenerla es más barato que recuperarla.

**Alcance, con una salvedad medida.** La paridad del 100% es la de los **recursos**: 569 cadenas traducibles en `values/` y otras tantas en `values-en/`, sin ninguna suelta. Lo que **no** cumple es el **contenido de la base de datos**, que también es texto visible y que esta decisión no distinguía: la auditoría de diseño del 21 de septiembre de 2026 ([AUDITORIA-DISENO-2026-09.md](AUDITORIA-DISENO-2026-09.md), T-10) contó **752 de 873 ejercicios activos (86%) con `nombre` idéntico a `nombre_en`** y 479 descripciones igual. Con la app en español, la pestaña más poblada se lee en inglés. Los logros sí están traducidos.

Queda anotado aquí, y no solo en la auditoría, porque «la paridad está al 100%» leído a secas daba por cerrado algo que no lo está.

**Qué la invalidaría.** Nada. Añadir idiomas amplía la regla, no la cambia.

---

### DEC-021 · No se optimiza sin medir
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Decisión.** Ningún cambio de rendimiento se acomete por intuición. Primero se mide, se registra el número de partida y se demuestra la mejora. Un índice que nadie usa también cuesta.

**Consecuencias.** Obliga a tener observabilidad antes que optimización, y evita refactors que no arreglan nada.

**Qué la invalidaría.** Nada.

---

### DEC-022 · Compilar no es terminar
**Estado:** Aceptada · **Fecha:** 2026-09-18

**Decisión.** Una tarea está terminada cuando funciona, no rompe lo existente, cubre carga/error/vacío/éxito, contempla los casos límite, tiene tests, es accesible, está en ES y EN, se ve bien en claro y oscuro, y no deja deuda innecesaria.

**Consecuencias.** "Los tests existentes siguen en verde" no significa que lo nuevo esté probado. Código nuevo sin tests nuevos no está terminado.

**Qué la invalidaría.** Nada.

---

### DEC-026 · Los correos son claros, de tablas y sin imágenes
**Estado:** Aceptada · **Fecha:** 2026-09-19

**Decisión.** Todos los correos salen de una misma plantilla (`PlantillaCorreo`), con una envoltura común —barra de acento, cabecera con la marca, contenido, franja de cierre y pie— y tres formas de contenido: **acción** (un protagonista único: un código o un botón), **aviso** (solo texto, nada que pulsar) y **datos** (número grande, etiqueta y comparación). Dos variantes de pie: **transaccional**, sin enlace de baja, y **comercial**, con él. La dirección visual es «A3 · profundidad por capas»: fondo `#E4DCD2`, tarjeta blanca de 520 px dentro de 600 px, acento `#B83E00` y profundidad por cambios de tono y líneas de 1 px.

**Son claros aunque la app sea dark-first.** El correo no es la app. La app pinta su propio fondo y el usuario ha elegido su tema dentro de ella; un correo se abre dentro de la bandeja de otro, sobre el fondo que ese cliente decida, y ni Gmail ni Outlook dan forma fiable de saber cuál es —el `prefers-color-scheme` de un correo lo aplica una minoría de clientes, y varios de los demás *invierten* los colores por su cuenta—. Un correo diseñado en oscuro se ve bien donde se probó y sale con texto gris sobre gris donde no. El claro es el único fondo que se comporta igual en todas partes, y además es lo que espera un correo de seguridad.

**Se maquetan con tablas.** Outlook de escritorio no usa un motor web: renderiza con el de Word. No entiende `flex` ni `grid`, ignora `border-radius` y `box-shadow`, y se salta buena parte del posicionamiento. Por eso todo va en tablas anidadas, con estilos en línea (el bloque `<style>` lo descartan varios clientes), los anchos también en el atributo `width`, y *ghost tables* entre comentarios `[if mso]` para fijar los 600/520 px que Outlook no saca de `max-width`. El diseño está pensado para esa pérdida: la profundidad viene de los tonos y de las líneas de 1 px, que sobreviven; las sombras están declaradas porque suman donde se entienden, pero nada depende de ellas.

**Sin imágenes y sin fuentes cargadas.** Gmail pasa las imágenes remotas por su proxy y muchos clientes las bloquean hasta que el usuario acepta, así que un diseño que dependa de ellas llega roto justo la primera vez, que es la que importa en un correo de recuperación. Las fuentes web solo las aplica un puñado de clientes. La marca es un cuadrado de color más texto, y el hueco del cuadrado queda reservado al logotipo real: cuando exista, entra ahí sin mover nada. Tipografía del sistema y monoespaciada del sistema para el código, que es lo único donde importa que las cifras midan lo mismo.

**Sin motor de plantillas.** Nada de Thymeleaf ni equivalentes: dependencia nueva y más arranque en una API que ya tarda 185 s en levantar en Render, para seis correos que no cambian en caliente. Bloques de texto de Java.

**El dorado `#CBA135` queda reservado.** No se usa todavía. Es el color de los correos de celebración —récords, rachas—, y si se gasta antes en un correo corriente deja de significar nada cuando haga falta.

**Consecuencias.** Un correo nuevo aporta solo su bloque central y elige forma y pie; la envoltura no se copia. La variante de pie es una decisión consciente y no estética: el resumen semanal y los avisos de récord son marketing a efectos legales aunque hablen de sentadillas, y llevan baja obligatoria; recuperación, verificación, borrado de cuenta y avisos de seguridad no la llevan, porque darse de baja de ellos equivale a quedarse sin cuenta. Todo valor que venga del usuario se escapa antes de entrar en la plantilla: sus párrafos son fragmentos de HTML, no texto plano.

**Qué la invalidaría.** Que Outlook de escritorio deje de usar el motor de Word, lo que permitiría maquetar con CSS moderno y quitar las *ghost tables*. Que el soporte de `prefers-color-scheme` en correo deje de ser minoritario y predecible, lo que reabriría la variante oscura. Un rediseño de marca que exija un logotipo en imagen aceptando que llegue bloqueado, o llevar los correos a una herramienta externa de plantillas, que haría sobrar esta clase entera.

---

### DEC-027 · Un id de catálogo no es un id de recurso: ahí el criterio no es 403
**Estado:** Aceptada · **Fecha:** 2026-09-19

**Contexto.** Al cerrar las IDOR abiertas, el backlog pedía «403 en las cinco rutas». En tres se cumple. En las otras dos —`GET /ejercicios-realizados/ejercicio/{ejercicioId}` y `GET /ejercicios-realizados/count/ejercicio/{ejercicioId}`— **es imposible por construcción**, y perseguirlo habría llevado a inventarse un 403 falso o a dar por buena una ruta que seguía filtrando.

**Decisión.** El criterio de aceptación depende de **qué es el id que viaja en la ruta**, y hay dos casos que no se defienden igual:

- **Id de un recurso con dueño** (una sesión, una comida, un usuario). Existe un «recurso de otro» que pedir, así que la respuesta correcta es **403** y es lo que fija el test, tal y como manda DEC-014.
- **Id del catálogo público** (un ejercicio, un alimento del catálogo, una categoría). Ese id es el mismo para todo el mundo y no pertenece a nadie: **no hay un id ajeno que rechazar**. El fallo posible no es de permisos sino de **alcance** —la consulta devolvía las filas de todos los usuarios—, y el criterio correcto es que **el atacante no vea nada del otro**: lista vacía o `count: 0`. *(Cuando se escribió esto, el controlador traducía la lista vacía a 404 y los tests lo afirmaban así; desde **DEC-033** esa lista vacía es un **200 con `[]`** y es sobre el cuerpo sobre lo que se afirma el aislamiento. El criterio no cambia, cambia la forma de comprobarlo.)* Un 403 en esa ruta sería incorrecto además de inútil, porque negaría a un usuario legítimo su propio histórico.

**El criterio «403 en las cinco rutas» del backlog estaba mal formulado.** Confundía la forma de la ruta con la forma del fallo. Queda anotado aquí para que no se reintroduzca al redactar el siguiente lote: antes de escribir el criterio hay que mirar si el id tiene dueño.

**Un agregado también es información ajena.** Un contador o un `exists` no devuelven ni una fila del otro y aun así lo delatan: iterando ids de sesión se dibuja el historial de entrenamiento de cualquiera, y preguntando ejercicio a ejercicio se reconstruye una sesión entera a base de síes y noes. Se protegen igual que un listado.

**Consecuencias.** DEC-014 no se relaja: sigue exigiendo test a toda ruta con id. Lo que esto añade es que el test comprueba **403 o aislamiento**, según el id. La vista global se conserva para ADMIN donde ya estaba declarada.

**Qué la invalidaría.** Que el catálogo de ejercicios o de alimentos dejara de ser público y pasara a tener dueño, lo que convertiría esas rutas en el primer caso.

---

### DEC-028 · La enumeración de cuentas por el registro es una limitación aceptada, no un descuido
**Estado:** Aceptada · **Fecha:** 2026-09-19 · **Se revisa con:** GP-045 (verificación de correo en el registro)

**Contexto.** `POST /auth/forgot-password` se diseñó para no revelar qué cuentas existen: el cuerpo de la respuesta es el mismo exista la cuenta o no. Al escribir sus tests apareció que esa propiedad no se sostiene por dos sitios distintos.

El primero es el propio endpoint: cuando la cuenta no existe —o está desactivada, o no tiene correo— el servicio vuelve pronto y no paga el hash del código ni el encolado del correo. El cuerpo es idéntico, el **coste no**, y esa diferencia se mide desde fuera sin herramientas especiales.

El segundo es más simple y más grave, y hace al primero irrelevante: `POST /auth/register` responde en texto plano **«el username 'X' ya está en uso»** y **«el email 'X' ya está en uso»** (`AuthService.java:90` y `:94`). Quien quiera saber si una dirección está registrada no necesita cronometrar nada: la pregunta ya tiene respuesta directa, y además es la respuesta *correcta* para un formulario de alta.

**Decisión.** La enumeración de cuentas queda **abierta y anotada**, no disimulada. En concreto:

- El **cuerpo uniforme** de `/auth/forgot-password` se mantiene. Es barato y evita el caso obvio.
- **No se añade trabajo caro artificial** en la rama sin cuenta para igualar tiempos. Compraría una propiedad que el registro regala igualmente, y el precio se paga en CPU en **cada intento fallido**, que en una instancia de 512 MB es justo lo que no sobra. Un hash de relleno es además un blanco cómodo: basta con repetir la petición para ocupar el servidor.
- Los **javadoc dicen lo que el código hace**: el cuerpo es uniforme, el coste no, y la enumeración está abierta por el registro de todas formas. Una promesa de seguridad escrita en un comentario y no cumplida por el código es peor que no escribirla, porque el siguiente que lea el comentario dará el problema por resuelto.
- Lo que sí contiene el daño es el **rate limit estricto** (DEC del filtro de autenticación): `/auth/register`, `/auth/forgot-password` y `/auth/reset-password` están en el cupo bajo por IP, así que enumerar es posible pero lento.

**El username seguramente se quede como está.** Es un identificador **público** —se muestra en la aplicación— y el formulario de alta tiene que poder decir que está cogido para que el usuario elija otro. Ahí no hay secreto que proteger, así que decirlo no es una fuga.

**El correo es el caso que sí hay que cerrar**, porque una dirección sí es un dato personal y saber que está registrada en una aplicación de gimnasio dice algo de su dueño.

**Qué la invalidaría — y cuándo toca mirarlo.** El momento es **GP-045, la verificación de correo en el registro**: hasta entonces no hay con qué sustituir la respuesta síncrona, porque el alta tiene que resolverse en la misma petición. Con verificación por correo, el registro puede responder siempre igual —«te hemos mandado un correo»— y el conflicto de dirección se resuelve dentro del mensaje, que solo lee quien controla el buzón; ahí esta decisión se sustituye. También la invalidarían: que la aplicación pase a tratar la pertenencia como dato sensible por normativa o por un requisito de un tercero, que aparezca un incidente real de enumeración en producción, o que el username deje de ser visible para otros usuarios, que reabriría el caso del username junto al del correo.

---

### DEC-029 · El host de la API es `api.gymprofit.app`, no el del proveedor
**Estado:** Aceptada · **Fecha:** 2026-09-19

**Contexto.** El `BASE_URL` del build de release **se compila dentro del APK** como constante de `BuildConfig`. No es una preferencia que se lea al arrancar ni algo que se pueda cambiar desde el servidor: **cada instalación se queda para siempre con el host que tuviera el día que se instaló**, y la única forma de moverlo es publicar una versión nueva y esperar a que todo el mundo la instale. Mientras ahí figurara `gymprofit-api.onrender.com`, el nombre del proveedor estaba grabado en cada teléfono.

**Decisión.** El host público de la API es **`api.gymprofit.app`**, un nombre del producto, y es el que va en `buildTypes.release` y en el keep-alive. El servicio de `onrender.com` **no se desactiva**: es el destino del CNAME y es también lo que siguen llamando las builds ya repartidas.

**Se hace antes de publicar a propósito.** Es el momento en que sale gratis. Después de la primera publicación, cada host mal elegido se arrastra tanto como el `applicationId` de DEC-023, solo que sin aviso: la app vieja sigue funcionando hasta el día en que el host desaparece.

**El keep-alive también apunta ahí, y no por simetría.** Lo que hay que vigilar es el camino por el que entran los usuarios —su DNS y su certificado, no solo el proceso del otro extremo—. Un keep-alive contra un host que ya nadie usa puede estar en verde mientras el real está caído.

**Consecuencias.** Mudarse de proveedor pasa a ser mover un CNAME, con las instalaciones existentes sin enterarse. A cambio, el dominio hay que renovarlo y su DNS pasa a ser parte de la infraestructura: si caduca o se configura mal, la app no llega a la API aunque el servidor esté perfecto. El certificado lo emite y renueva Render para el dominio personalizado.

**Qué la invalidaría.** Que se dejara de usar Render y el CNAME tuviera que apuntar a otro sitio — que es **precisamente el caso que esta decisión hace indoloro**, así que no la invalida, la justifica. Sí la invalidarían: perder el control del dominio `gymprofit.app`, o que apareciera una razón para servir la API desde un nombre distinto del de la marca. Si algún día hiciera falta cambiar de host de verdad, el sitio donde se decide sigue siendo este archivo, no un `build.gradle`.

---

### DEC-030 · El `applicationId` es `com.gymprofit.app`
**Estado:** Aceptada · **Fecha:** 2026-09-19 · **Resuelve:** DEC-023

**Contexto.** El `applicationId` era `es.pmdm.gymprofit`, donde PMDM es el nombre de un módulo del ciclo formativo en el que nació el proyecto. **Tras la primera publicación en Play ya no se puede cambiar nunca**: queda en la URL de la ficha, en cada instalación y en la identidad con la que Play reconoce las actualizaciones. Cambiarlo después no es difícil, es **imposible**: sería una app distinta, sin sus usuarios, sin sus reseñas y sin su historial.

**Decisión.** El `applicationId` es **`com.gymprofit.app`**. Coherente con el paquete de la API (`com.gymprofit.api`) y con el dominio del producto (`gymprofit.app`), y sin rastro del contexto académico en el que empezó (DEC-001).

**Los paquetes Java se quedan en `es.pmdm.gymprofit`, y es a propósito.** Desde AGP 7 el `namespace` —la raíz de los paquetes y de la clase `R`— y el `applicationId` son ajustes **independientes**: el primero es una decisión de organización del código y no sale del repositorio; el segundo es la identidad pública de la app. Lo que se ve desde fuera —la ficha de Play, el directorio de datos del dispositivo, la autoridad del `FileProvider`, los proveedores de Firebase— sale del `applicationId` y ya se ha movido solo, porque el manifest usa `${applicationId}` y no cadenas escritas a mano. Renombrar los 121 ficheros Java sería **cosmético**, tocaría todos los imports y las reglas de ProGuard, y el riesgo se paga entero en release, que es donde R8 reescribe nombres. Si algún día se hace, es una tarea aparte y opcional.

**Consecuencias.** El `google-services.json` pasa a declarar **dos clientes**, el nuevo y el viejo, para que Firebase siga reconociendo las builds ya repartidas mientras existan; el fichero está en `.gitignore` y no viaja en el repositorio. El único sitio del código que afirmaba el identificador antiguo era el test instrumentado de ejemplo, que comprobaba `getPackageName()` —que devuelve el `applicationId`, no el paquete Java— y se ha puesto al día.

**Qué la invalidaría.** **Nada, una vez publicada la app** — y ese es justo el motivo de decidirlo ahora y no más tarde. Mientras no se haya subido nada a Play, incluida la pista interna, todavía se puede cambiar de idea sin coste; después, esta entrada deja de ser una decisión revisable y pasa a ser un hecho. Lo que sí puede cambiar sin tocar esto es el **namespace** de los paquetes Java, que es independiente.

---

### DEC-023 · Identificador de la aplicación
**Estado:** RESUELTA por DEC-030 · **Fecha de cierre:** 2026-09-19

Pedía decidir de forma consciente si el `applicationId` `es.pmdm.gymprofit` —donde PMDM es el módulo académico del ciclo— se cambiaba antes de publicar, porque **tras la primera publicación no se puede cambiar nunca**. Ya no bloquea la subida a Play: **DEC-030** fija `com.gymprofit.app` y deja escrito por qué los paquetes Java se quedan como están.

---

### DEC-031 · Borrar la cuenta borra de verdad, y borra ya
**Estado:** Aceptada · **Fecha:** 2026-09-19

**Contexto.** `gymprofit.app/eliminar-cuenta` promete por escrito que el borrado es irreversible, y esa página es una de las dos URL que Google Play exige enlazar en la ficha. Lo que la API hiciera tenía que coincidir con lo prometido, no parecerse.

**Decisión.** `DELETE /usuarios/me` borra la cuenta y todos sus datos **en el acto**: sin periodo de gracia, sin copia anonimizada y sin baja lógica. Exige la **contraseña actual** aunque la petición ya venga autenticada, porque un token robado —o un móvil desbloqueado un minuto— no debe bastar para vaciar una cuenta. El usuario sale del token (DEC-013) y la ruta va en el cupo estricto del rate limit.

**El borrado se escribe, no se hereda de las cascadas.** Las `ON DELETE` del esquema siguen ahí, pero nada de lo que importa depende de ellas: una cascada no se lee, no se prueba con nombre propio y no avisa cuando alguien añade una tabla. El servicio borra en orden explícito y deja en el log cuántas filas se lleva cada paso.

**Los alimentos personalizados se borran, no se despublican.** En `alimentos`, `usuario_id` NULL significa *catálogo público*, así que el `ON DELETE SET NULL` de esa tabla **no borraba: publicaba** la dieta privada de quien se iba. Era un fallo de privacidad, no una decisión.

**Las filas de terceros se tocan, y se anota.** Una comida de otro usuario que referencie un alimento del que se va pierde esa línea, y sus totales se recalculan; una sesión de otro que apunte a una rutina del que se va **se desvincula, no se borra**. Ambas cosas solo pueden existir por IDOR de escritura ya cerradas, quedan registradas en el log, y **no se notifica** a esos usuarios: explicarles el cambio les diría que otra persona ha borrado su cuenta. Entre el derecho de supresión prometido y una fila fabricada abusando de un fallo, gana la supresión.

**Desvincular la sesión ajena no la deja a medias: la deja en un estado que el modelo ya contempla.** `sesiones_entrenamiento.rutina_id` es `INT NULL` desde la migración inicial y la entidad lo documenta como *«opcional, puede ser entrenamiento libre»*. Una sesión sin rutina **no es un huérfano**, es un entrenamiento libre, que es un concepto del producto desde el primer día. Esto no es «la opción menos destructiva de las malas»: es la única que deja la fila ajena en un estado válido, y por eso no hay nada que revisar aquí más adelante.

**Qué la invalidaría.** Una obligación legal o contable de conservar algo —hoy no la hay: no se factura nada—, o que apareciera un caso real de borrado por error lo bastante frecuente como para que un periodo de gracia compense romper la promesa de irreversibilidad. Si algún día se monetiza, esto se revisa junto con DEC-028 y el resto de lo que cambia al ser comerciante.

---

### DEC-032 · El catálogo de alimentos lo escribe ADMIN; el escáner es la excepción
**Estado:** Aceptada · **Fecha:** 2026-09-19

**Contexto.** En `alimentos`, un `usuario_id` NULL marca la fila como catálogo público, que ve todo el mundo —incluido GUEST, cuyo token se obtiene sin credenciales—. `POST /alimentos` estaba abierto a cualquier USER, tomaba el `usuarioId` del cuerpo y, si se omitía, creaba la fila sin dueño. Con el registro abierto, eso era **escritura efectivamente anónima en el catálogo compartido**. En una aplicación de nutrición el daño no es de privacidad: son macros falsos que alguien se cree y se come.

**Decisión.** El propietario de un alimento sale **siempre del token** (DEC-013) y el `usuarioId` del cuerpo se ignora. Un USER crea alimentos **suyos**; crear catálogo —filas sin dueño— es cosa de **ADMIN**. Lo mismo vale para modificar, desactivar y borrar: el catálogo solo lo toca un ADMIN, y un alimento con dueño, su dueño. Cambiarle los macros a un alimento del catálogo se los cambia a todo el mundo, así que no basta con cerrar la creación.

**La excepción es el escáner.** `POST /alimentos/importar` materializa un producto de **Open Food Facts** por código de barras y sí crea una fila **sin dueño**, porque es catálogo real y no la comida de nadie, y lo dispara un usuario normal al escanear. No pasa por la ruta de creación: construye la entidad desde el producto externo. Es la diferencia entre *escribir en el catálogo* y *traerse un producto que ya existe*. *(Desde el lote 1.6.0 el producto sale primero de `productos_off`, la copia semanal de Open Food Facts (DEC-041), y solo si no está se lee de Open Food Facts; `GET /alimentos/codigo/{codigo}` materializa por el mismo camino. La regla no cambia: es catálogo y lo dispara un usuario normal.)*

**Qué la invalidaría.** Querer un catálogo **colaborativo**, con alimentos propuestos por usuarios y visibles para el resto. Eso no reabre esta decisión sin más: pediría moderación, autoría visible y forma de corregir, que es un sistema, no un permiso. También la invalidaría dejar de tener catálogo propio y apoyarse solo en Open Food Facts.

---

### DEC-033 · Una colección vacía es 200 con `[]`; el 404 es del recurso padre
**Estado:** Aceptada · **Fecha:** 2026-09-23

**Contexto.** La API respondía `404` cuando una lista salía vacía. No era un endpoint suelto: el patrón `if (lista.isEmpty()) throw new NotFoundEntityException(...)` aparecía **58 veces en 13 controladores**, o sea que era *la* convención. La consecuencia es que **el cliente no podía distinguir «no tienes datos» de «ha fallado algo»**, porque las dos cosas llegaban con el mismo código. Cada pantalla de la app acabó con su apaño para compensarlo, y `UiFeedback` silenciaba el 404 **en toda la aplicación** para que el estado vacío no saliera como error de red. El precio de ese silencio es que un 404 de verdad —un id borrado, una ruta mal construida— tampoco se veía: la pantalla se quedaba vacía sin decir nada.

Se vio al cerrar GP-060: la pestaña de rutinas pedía las predefinidas **antes** que las propias precisamente porque las propias «fallaban» cuando el usuario no tenía ninguna. Un defecto de producto —seis rutinas ajenas bajo el rótulo «Mis rutinas»— que nacía de un código HTTP.

**Decisión.** Dos reglas, y se aplican **endpoint por endpoint**, no en bloque:

- **Una colección que existe y está vacía responde `200` con `[]`.** Vacío es un resultado, no un error.
- **`404` es que el recurso PADRE de la ruta no existe**, y se comprueba **explícitamente** —`existsById` o cargando la entidad—, nunca deduciéndolo de que la lista salga vacía.

La diferencia no es cosmética: `GET /sesiones/rutina/{id}` daba `404` igual si la rutina no existía que si existía sin sesiones, y **son dos respuestas distintas**. El precedente ya estaba escrito en el propio código: `LogroService.findByUsuarioId` comprueba propiedad, luego existencia, y devuelve la lista aunque venga vacía. Esto generaliza eso.

**El orden entre 403 y 404 depende de la forma de la ruta, y no es una elección libre.** Hay dos casos y el código hace uno en cada uno:

- **Rutas que cuelgan del usuario** (`/sesiones/usuario/{id}`, `/comidas/usuario/{id}`…). El dueño del id **es el id**: se compara contra el del token y se decide **sin tocar la base de datos**. Ahí la propiedad va primero y a quien no es dueño se le responde `403` sin decirle de paso si ese usuario existe.
- **Rutas que cuelgan de un recurso con dueño** (`/rutinas-ejercicios/rutina/{id}/ordenados`, `/sesiones/rutina/{id}`…). Aquí **no se puede comprobar la propiedad sin cargar el recurso**, porque el dueño está dentro de él. Un id inexistente da `404` —no hay nada que cargar— y uno ajeno da `403`. Es lo que hace `RutinaEjercicioService.findByRutinaIdOrdenado` —carga, `checkRutinaReadAccess`, lista— y es lo que afirman sus propios tests de `/ordenados`, uno por código.

**Se deja así a sabiendas.** La diferencia entre `403` y `404` en el segundo caso sí revela si un id existe. El coste de taparlo sería responder `404` también al recurso ajeno, y el beneficio es **despreciable**: los ids son **secuenciales y autonuméricos**, así que quien quiera saber cuántas rutinas hay en la base no necesita sondear —le basta con crear una suya y mirar el número—. Se ganaría ocultar la existencia de ids sueltos a cambio de mentirle a quien se equivoca de id, que es el caso común. **El código no se toca.**

*(La primera redacción de esta decisión decía «403 antes que 404» a secas, generalizando el primer caso al segundo. Era falso sobre el código y contradecía los tests que la propia decisión hizo escribir.)*

DEC-027 y DEC-014 no se relajan; lo único que cambia es **cómo se afirma el aislamiento en los tests**: donde el id es de catálogo público, el test pasa de exigir `404` a exigir `200` con lista vacía, que es lo que siempre quiso decir.

**En el cliente, el 404 vuelve a ser un error.** Se retira el silencio de `UiFeedback` y los apaños de las nueve pantallas que trataban el 404 como «sin datos». Dejarlos habría sido peor que antes: con la lista vacía llegando ya como `200`, seguir callando el 404 solo escondría errores de verdad.

**Consecuencias.** Es un **cambio de contrato** y toca a las builds repartidas fuera de Play, que no se actualizan solas. Se asume a sabiendas y ahora, antes de publicar: en cuanto la app esté en la tienda, esta convención queda congelada. Una build vieja contra la API nueva deja de ver 404 donde los esperaba —y como los trataba como estado vacío, el resultado es el mismo o mejor.

**Qué la invalidaría.** Nada razonable. Lo único que la reabriría es que apareciera un endpoint donde la lista vacía signifique de verdad «esto no existe», y entonces el problema es el diseño de ese endpoint, no la convención.

---

### DEC-034 · Contraseñas según NIST SP 800-63B-4, con mínimo de 8
**Estado:** Aceptada · **Fecha:** 2026-09-26 · **Sustituye:** la regla de mezcla de GP-095

**Contexto.** El registro pedía de 8 a 100 caracteres con minúscula, mayúscula, dígito y símbolo. Es la regla que NIST retiró: la guía SP 800-63B-4 (versión final, agosto de 2025) dice que las reglas de composición no mejoran las contraseñas que la gente elige de verdad —producen `Password1!`— y sí aumentan la fricción y las que se apuntan en un papel. Lo que pide a cambio es longitud y una lista de bloqueo. Además, el máximo de 100 mentía: BCrypt solo admite 72 bytes, Spring Security rechaza más, y eso salía como el 400 genérico «Parámetro con valor inválido».

**Decisión.** Al poner una contraseña nueva —alta, recuperar y cambiar—:

- **Mínimo 8 caracteres**, contados como caracteres (code points) y no como unidades UTF-16: un emoji cuenta uno.
- **Máximo 72 bytes en UTF-8**, que es lo que admite BCrypt. Con letras latinas son 72 caracteres; con eñes, menos. El error dice «demasiado larga», nunca el genérico.
- **Sin reglas de composición.** Ni mayúsculas, ni números, ni símbolos obligatorios. Se admite cualquier carácter imprimible, espacios y Unicode incluidos. El texto de ayuda empuja hacia lo que sí sirve: «Una frase corta es más segura que una palabra».
- **Lista de bloqueo dentro de la API**, sin servicios externos: las contraseñas de 8 o más caracteres de las 100 000 más frecuentes de la lista de 10 millones de Mark Burnett (dominio público), tomada de SecLists (MIT); 38 451 entradas en `seguridad/contrasenas-comunes.txt`, con la fuente en la cabecera. Más la que sea o contenga el nombre del servicio, el nombre de usuario o la parte del correo antes de la «@» (esta desde el lote 1.5.0, porque el alta nueva no pide usuario y se entra con el correo); el usuario y el correo cuentan desde 3 caracteres. Sin distinguir mayúsculas. El rechazo es el mismo 400 con un código en `cause` —`PASSWORD_COMUN` o `PASSWORD_CONTIENE_NOMBRE`— y la app lo enseña en el campo.
- **Las cuentas que ya existen siguen entrando con su contraseña.** La regla no se comprueba al entrar, ni en la API ni en la app. **Nada de cambios periódicos**: NIST los desaconseja por lo mismo que la composición.
- **La app recorta los espacios del principio y del final de la contraseña**, y lo hace igual al crearla, al entrar y al cambiarla (también al recuperarla). El motivo es el teclado del móvil: el autocompletado y el pegado añaden un espacio al final sin que se vea, y una contraseña guardada con ese espacio no vuelve a entrar si al día siguiente no se añade. Lo que importa es que las tres pantallas hagan lo mismo: si una recortara y otra no, la misma contraseña valdría en una y fallaría en otra. Los espacios de dentro sí cuentan, y una frase corta sigue siendo una buena contraseña. La API no recorta: recibe lo que la app manda.
  ~~*Al escribir esto (2026-09-27), la confirmación del borrado de cuenta manda la contraseña sin recortar. Es deuda anotada, no parte de la regla.*~~ **Saldada en GP-119 (1.1.1):** el borrado de cuenta también recorta, y con la contraseña equivocada la API responde 403 con `PASSWORD_ACTUAL_INCORRECTA` en `cause`, como al cambiarla. La del cambio de correo, que tenía el mismo defecto, recorta desde que pasó a Ajustes (GP-105).

**La desviación: el mínimo es 8, no 15.** NIST pide 15 caracteres cuando la contraseña es el único factor de autenticación, que es el caso aquí. El propietario elige **8 por la fricción en el alta**: es una aplicación de gimnasio que se instala en el vestuario, y cada pantalla de más en el registro se paga en altas que no se completan. Es una decisión consciente, no un descuido, y lo que la compensa en parte es la lista de bloqueo, que quita justo las contraseñas de 8 a 14 caracteres que se prueban primero, y el rate limit estricto de las rutas de autenticación.

**Consecuencias.** La 1.0.1 trata cualquier 400 del registro como «usuario o correo en uso»: con la lista de bloqueo, rechazaría contraseñas que ella da por buenas y lo explicaría mal. Por eso la parte de API se despliega a la vez que se reparte la 1.0.2, no antes. La lista ocupa unos 350 KB en el JAR y unos pocos MB de memoria al arrancar.

**Qué la invalidaría.** Un segundo factor —entonces 8 es lo que pide NIST y la desviación desaparece—, o que aparezcan cuentas tomadas por contraseñas adivinadas, que obligaría a subir el mínimo hacia 15. Si la lista se queda corta, se cambia el fichero por uno mayor; la regla no cambia.

---

### DEC-035 · La administración es una web aparte, detrás de dos cerraduras, que nunca toca la base
**Estado:** Aceptada · **Fecha:** 2026-09-27 · **Tarea:** GP-085 (fase 1)

**Contexto.** El panel de administración vivía dentro de la app Android. Sirve para activar o desactivar una cuenta, pero no para el trabajo que de verdad pide el producto: 752 de 873 ejercicios activos se llamaban igual en español que en inglés (DEC-020), y traducir un catálogo así en la pantalla de un móvil no se hace. Hacía falta una herramienta de escritorio. Lo que no podía pasar es que esa herramienta abriera una segunda puerta a los datos.

**Decisión.**

- **Dónde vive.** Es una web propia, en `admin/` del monorepo (React, Vite y TypeScript), servida en **`admin.gymprofit.app`** como Cloudflare Worker de solo archivos estáticos (`gymprofit-admin`), sin `workers.dev` ni URLs de previsualización. Solo en español: la usa el propietario. Y solo en tema oscuro, el de la app: una herramienta de una persona no necesita mantener dos paletas, y cada contraste se comprueba una vez (añadido con GP-120).
- **Dos cerraduras.** La primera es **Cloudflare Access** delante del dominio, que solo deja pasar el correo del propietario: sin ella no se descarga ni el HTML. La segunda es **una cuenta con rol ADMIN** de GymProFit en Entrada; cualquier otra cuenta recibe el mismo aviso que una contraseña equivocada. Y detrás de las dos sigue la que manda: **la API exige ADMIN en cada ruta `/admin`**, venga la petición de la web o de cualquier otro sitio. La web no decide nada de seguridad; como mucho, evita pedir lo que la API va a rechazar.
- **Tokens solo en memoria.** Ni `localStorage` ni `sessionStorage`: un script que llegara a ejecutarse en la página no encontraría dónde leerlos después, y al cerrar la pestaña no queda nada. El precio es volver a entrar al recargar, que en una herramienta de uso ocasional es barato. Ante un 401, una renovación y un solo reintento; si falla, a Entrada.
- **Nunca toca la base.** Toda lectura y toda escritura pasan por la API con el token de la cuenta ADMIN. La web no lleva credenciales de base de datos, ni claves, ni ningún secreto: el repositorio es público y lo único que se compila dentro es la URL de la API.
- **Sin datos de salud.** Ninguna respuesta de `/admin` lleva peso, medidas, entrenamientos ni comidas; de una cuenta se ve cuántas sesiones y comidas tiene, no cuáles. Las rutas de la web son nuevas y con DTO propios: las que usa el panel de la app (que sí devuelven peso y altura) no cambian de forma, y ese panel sale en la fase 2.
- **CORS y cabeceras.** Producción solo admite el origen `https://admin.gymprofit.app` (escrito en `application-prod.properties`, fijado por test). La web lleva una CSP que solo deja conectar con `https://api.gymprofit.app` y prohíbe los marcos.

**Consecuencias.** La superficie nueva es un sitio estático: no hay servidor propio que mantener ni base que proteger en otro sitio. Añadir una pantalla de administración es añadir rutas a la API (con su test de 403 para USER e invitado, DEC-014) y una página a la web. El panel de la app queda como duplicado hasta que se retire en la fase 2.

**Qué la invalidaría.** Que haga falta más de un administrador con permisos distintos: entonces Access con un solo correo no basta y el rol ADMIN único se queda corto, y habría que diseñar roles (lo que DEC-012 anticipa para las capacidades). Que la web necesite hacer algo que la API no expone —la tentación de «leerlo directamente de la base»—: se añade a la API, nunca se abre la base a la web. Que Cloudflare deje de ser el proveedor del dominio: la web es estática y se muda a cualquier sitio que sirva archivos y cabeceras, pero Access habría que sustituirlo por otra cerradura equivalente antes de publicar.

---

### DEC-036 · Se entra con el correo o con el usuario, y el usuario lo propone la API
**Estado:** Aceptada · **Fecha:** 2026-09-30 · **Tarea:** GP-103 (fase 1, lote 1.5.0)

**Contexto.** El alta nueva es un cuestionario sin cuenta, «Tu plan» con el programa recomendado y la cuenta al final («Guarda tu plan»). Pedir ahí un nombre de usuario es una pregunta más en el peor momento, y la mayoría de la gente no recuerda qué usuario eligió, pero sí su correo.

**Decisión.**

- **Entrar.** `POST /auth/login` acepta en el mismo campo `username` el usuario o el correo; el campo no cambia de nombre para que las builds ya repartidas sigan entrando. Con «@» se busca primero por correo, **sin distinguir mayúsculas**, y si no hay cuenta con ese correo, por usuario, por si alguna antigua lleva «@» en el nombre. Un correo que no existe da el mismo 401 que una contraseña equivocada. El token sale con el `username` de la cuenta.
- **El usuario, opcional en el alta.** Si no llega, la API lo propone con el **nombre** de la persona: minúsculas, sin tildes, solo letras, números, punto y guion bajo, de 3 a 30 caracteres (sin nada aprovechable, «usuario»). Si se queda corto o ya existe, se le añade el primer número que lo deja libre («ana», «ana2»). **Del correo, solo cuando el alta no trae nombre**, con las mismas reglas y la parte antes de la «@» (desde la 1.5.1, GP-147; en la 1.5.0 salía siempre del correo). La respuesta del alta lo devuelve.
- **Ningún usuario nuevo lleva «@»**, se cree la cuenta por donde se cree (alta y `POST /usuarios`): 400 con `USERNAME_NO_VALIDO`. El login decide por la «@» si lo escrito es un correo, y un usuario con «@» podría llamarse como el correo de otra cuenta.
- **La contraseña** tampoco puede contener la parte del correo antes de la «@» (DEC-034), con el mismo mínimo de 3 caracteres que el usuario: es lo primero que probaría quien conoce la dirección.

**Consecuencias.** Un usuario propuesto con el correo es su parte local, y quien lo vea sabe media dirección; por eso, desde la 1.5.1, sale del nombre siempre que lo hay, y el alta de la app lo manda siempre. Hoy el usuario solo lo ven la propia cuenta y la administración (comprobado en las rutas de la API; el bot de Discord está fuera de este repositorio y no se ha mirado). Dos altas simultáneas con la misma base pueden recibir la misma propuesta; la restricción única de la base impide el duplicado, y la que choca al guardar prueba el número siguiente (GP-146, desde la 1.5.1; antes recibía un 500). Por eso el alta no va en una transacción envolvente: cada intento de guardar es la suya, y se rinde al quinto choque seguido.

**Qué la invalidaría.** Que el usuario pase a verse entre cuentas (un ranking, un perfil público): entonces el propuesto no puede salir del correo y hay que pedirlo o generarlo de otra forma. Que haga falta cambiar el usuario después del alta: hoy no se puede, y un propuesto que no gusta se queda. Con GP-045 (verificación de correo) el correo pasa a ser el identificador fuerte y esta decisión no cambia, pero DEC-028 sí.

---

### DEC-037 · Avisos por tipo, con comidas apagadas de serie
**Estado:** Aceptada · **Fecha:** 2026-09-30 · **Tarea:** GP-112 (lote 1.5.0)

**Contexto.** Los recordatorios que genera el servidor eran todo o nada. Los de comidas son cinco al día y solo sirven a quien registra lo que come; quien no lo hace solo podía callarlos apagando todas las notificaciones, también las que sí le servían. Y el recordatorio genérico de entrenar de las 18:00 saltaba los días de descanso de un programa.

**Decisión.**

- Tres interruptores por cuenta: **entrenar** (el de inactividad), **comidas** (los cinco) y **progreso** (resumen semanal, logro próximo, medición mensual y objetivo por vencer). Están en `UsuarioDTO` y se cambian por el PATCH.
- **Valores de serie: entrenar sí, comidas no, progreso sí**, también para las cuentas que ya existían (los pone el `DEFAULT` de la migración). Comidas va apagado porque cinco avisos diarios sin haberlos pedido son la forma más rápida de que alguien apague todas las notificaciones de la app.
- **Se retira el recordatorio de las 18:00.** No sabía si ese día tocaba descanso; el de inactividad ya cubre a quien lleva tres días sin entrenar.
- Un recordatorio nuevo va en uno de los tres tipos, o trae su propio interruptor.

**Consecuencias.** Quien recibía los de comidas deja de recibirlos al desplegar, y la 1.4.0 no tiene dónde encenderlos: llegan con la 1.5.1. Las notificaciones que crea la propia app (sesión en curso, descanso) no dependen de estos interruptores.

**Qué la invalidaría.** Que los datos muestren que quien registra comidas deja de hacerlo sin los avisos: entonces comidas se enciende de serie para quien haya registrado alguna. Que se añada un recordatorio de entrenar que sepa del programa (qué día toca): volvería a caber uno a una hora fija, dentro de «entrenar».

---

### DEC-038 · La edad mínima, 14, la comprueba también la API
**Estado:** Aceptada · **Fecha:** 2026-09-30 · **Tarea:** lote 1.5.0

**Contexto.** La política de privacidad dice que GymProFit no es para menores de 14, pero la API aceptaba cualquier edad y la app dejaba poner desde 10. Una regla que solo está en un texto legal no protege a nadie.

**Decisión.** El alta y el PATCH del perfil rechazan una edad menor de 14 con un 400 que dice el mínimo en el idioma de la petición y lleva `EDAD_MINIMA` en `cause`. La comprobación está en el servicio y no en el DTO, para que 0 o una edad negativa den el mismo 400 claro. Sin edad no se comprueba nada: sigue siendo opcional, y no se pide para usar la app.

**Consecuencias.** La 1.4.0 deja poner de 10 a 13 en el onboarding, y Editar perfil no comprueba la edad; con una menor de 14 el guardado del perfil falla con un 400 que esa versión no sabe explicar. La 1.5.1 tiene que poner el mínimo en sus formularios y enseñar el mensaje. Las cuentas que ya tienen guardada una edad menor no se tocan: la regla se aplica al escribirla.

**Qué la invalidaría.** Que la política cambie el mínimo (por país, o a 16 por el RGPD en algún mercado): se cambia el número en `ReglasPerfil` y en la política a la vez. Que se decida verificar la edad de verdad: esto solo rechaza lo que se declara.

---

### DEC-039 · El movimiento: doce momentos, cuatro curvas y nada que esperar
**Estado:** Aceptada · **Fecha:** 2026-10-01 · **Tarea:** GP-104 (lote 1.5.1)

**Contexto.** La app se movía a trozos: cada pantalla elegía su duración y su curva (260 ms aquí, 600 allá, un `DecelerateInterpolator` suelto), y nadie sabía qué tenía que pasar con «Quitar animaciones». El alta nueva se aprobó con su movimiento dibujado al milisegundo (`documentacion/diseno/2026-09-30-alta/fuente/Movimiento.dc.html`), y hacía falta un sitio donde vivieran esos números para que la siguiente pantalla no se los inventara.

**Decisión.** El movimiento sale de `Movimiento` (`utils/Movimiento.java`) y de nada más: las pantallas piden un momento, no escriben milisegundos ni curvas.

Cuatro curvas: **enfatizada** `cubic-bezier(.05,.7,.1,1)` para lo que entra; **estándar** `(.2,0,0,1)` para lo que cambia de sitio o de tamaño; **rebote** `(.34,1.56,.64,1)` para lo que salta; **temblor** `(.36,.07,.19,.97)` para el error. Y lineal para lo que gira.

| Momento | Qué pasa | Duración y curva | Vibración |
|---|---|---|---|
| 1 · Elegir | Se hunde un 3 % al tocar (`TOQUE`), fondo y borde se rellenan (`RELLENO`), el check salta (`CHECK`) y el icono gira y rebota (`ICONO`) | 120 estándar · 150 · 260 rebote · 420 rebote | Ligera |
| 2 · «Siguiente» se enciende | La primera vez que se responde, se llena de naranja y crece de 0,94 a 1 (`ENCIENDE`) | 380 rebote | — |
| 3 · Entrar en pantalla | Título, texto y opciones suben 16 dp, uno tras otro (`ENTRA`, `ENTRA_ESCALON`) | 350 enfatizada, cada 40 | — |
| 4 · Abrir capítulo | El icono del capítulo salta (`CAPITULO`) y su aro late dos veces (`LATIDO`) | 450 rebote · 900 ×2 | Media |
| 5 · Barra de progreso | El tramo se llena (`BARRA`) y, al cerrar un capítulo, destella (`DESTELLO`) | 500 estándar · 700 | — |
| 6 · Selector que se desliza | La píldora viaja a la opción (`PILDORA`) y la semana de ejemplo se enciende día a día (`DIA`, `DIA_ESCALON`) | 320 rebote · 280 rebote, cada 35 | Ligera |
| 7 · Cifras que cuentan | Las calorías cuentan mientras el anillo se dibuja por macros (`CIFRAS`), y al final un halo (`HALO`) | 1200 con 1 − (1 − t)³ · 900 | — |
| 8 · El programa llega | La tarjeta sube (`CARTA`), las rutinas entran de una en una (`FILA`, `FILA_ESCALON`) y un brillo la cruza una vez (`BRILLO`) | 450 enfatizada · 300, cada 100 · 900 | — |
| 9 · Cuenta creada | El botón se hace círculo (`CIRCULO`), gira mientras responde la API (`GIRO`) y acaba en un check que se traza (`TRAZO`) | 300 estándar · 700 lineal · 420 | Éxito |
| 10 · Error | El campo tiembla (`TEMBLOR`) y el mensaje aparece debajo (`MENSAJE`) | 380 temblor · 250 | Error |
| 11 · Aviso de ejemplo | Baja como una notificación de verdad y su icono zumba (`AVISO`, en bucle) | 5000 | — |
| 12 · Récord | El trofeo salta (`TROFEO`), suelta seis chispas doradas (`CHISPAS`) y un brillo cruza la fila (`BRILLO`). En la bienvenida y en el resumen de cada sesión | 450 rebote · 700 · 900 | Éxito |
| 15 · El «+» se vuelve ✓ | Al tocar el «+» de una fila, el botón se hunde a 0,94 (`HUNDE`), vuelve mientras su fondo pasa a verde y el «+» se va (`VERDE`), y el check salta con rebote (`CHECK`). Tocar el ✓ lo deshace con los mismos tiempos | 70 estándar · 120 estándar · 260 rebote | Ligera |
| 16 · La lista entra en cascada | Las filas de Añadir y de los resultados suben 16 dp y aparecen, una tras otra (`CASCADA`, `CASCADA_ESCALON`). Solo la primera vez: al volver, ya están | 350 enfatizada, cada 40 | — |
| 17 · Código leído | Mientras busca, una línea barre el marco (`BARRIDO`). Al leer, las esquinas pasan a verde y el marco encaja (`ENCAJA`), y la hoja del producto sube (`HOJA`) | 2400 de ida y vuelta · 200 estándar · 450 enfatizada | Éxito |
| 18 · La copia de ayer | Con la ✓ o la ✗, la tarjeta «¿Copiar la de ayer?» se pliega: encoge a 0,96 y se apaga (`PLIEGA`). Con la ✓, en la pantalla de una comida, sus alimentos caen 10 dp y aparecen uno detrás de otro (`CAE`, `CAE_ESCALON`) | 180 estándar · 360 enfatizada, cada 120 | Ligera al copiar |
| 19 · El corazón late | Al marcar un favorito, el corazón se rellena de rojo y late: crece a 1,3, baja a 0,95 y vuelve (`CORAZON`). Al quitarlo, se vacía sin latido. Aceptar la propuesta de favorito mete el alimento en la lista: baja 12 dp, aparece con el fondo naranja suave y el fondo se apaga (`RESALTA`) | 450 rebote · 1400 enfatizada | Ligera |
| 20 · Borrar y deshacer | La fila sigue al dedo y, al soltarla, se va de lado (`SALE`); la comida se recoloca (`RECOLOCA`) y sube el aviso con «Deshacer», el de Material, que ya entra en lo que pide el lienzo. «Deshacer» devuelve la fila a su sitio con el mismo `RECOLOCA`. El anillo, los porcentajes y la barra del día pasan a su valor nuevo con `BARRA` | 380 estándar · 250 estándar · 500 estándar | — |

La nutrición nueva (lienzo del 01-10, `documentacion/diseno/2026-10-01-nutricion/fuente/Movimiento.dc.html`) sigue la numeración del 13 al 22. Cada momento entra en esta tabla con el lote que lo construye: el 16 y el 17, con el 1.6.1 (2026-10-02); las barras de la ficha se llenan con `BARRA`, el del momento 5. El 15 y el 19, con el 1.6.3, salen del mismo lienzo: el «+» se hunde del 30 al 33 % de un bucle de 2,4 s (72 ms, aquí 70), vuelve y se pone verde del 33 al 38 % (120) y el check salta en 260; el corazón late del 31 al 49 % (432 ms), que el tablero de la ficha redondea a 450; el resaltado de un favorito nuevo es la animación `nueva` del tablero 8, de 1400. La barra de «Hecho» sube con `ENTRA`. El 20, con el 1.6.2, sale del lienzo del 02-10 (`documentacion/diseno/2026-10-02-nutricion/fuente/Movimiento.dc.html`): allí la fila tarda el 12 % de un bucle de 3,2 s (384 ms, aquí 380) y la comida se recoloca en 250. En el mismo lote, las filas de una comida entran con `CASCADA`, la hoja «¿A qué comida?» sube con `HOJA` y su check salta con `CHECK`. El 18, con el 1.6.4 (2026-10-03), del mismo tablero: la tarjeta se pliega del 16 al 22 % de un bucle de 3 s (180 ms; el lienzo no da curva, y va con la estándar, la de lo que cambia de tamaño) y cada alimento cae del 20 al 32 % (360 ms, enfatizada), el segundo 120 ms después. Lo que el lienzo enseña en el diario —los alimentos que caen en su tarjeta y las cifras del día que cuentan— llega con el diario nuevo de la 1.6.7: en la 1.6.4 la tarjeta se pliega y el diario se recarga.

Las reglas:

- **Con «Quitar animaciones», todo en su estado final.** Casi todo lo hace Android: con la escala de duración a 0, un animador salta a su final. Por eso se anima siempre *hacia* el estado final, nunca desde él. Lo que va en bucle (la demostración de la bienvenida, el aviso de ejemplo) pregunta antes a `Movimiento.quieto()` y se queda en su último fotograma.
- **Las vibraciones se quedan**, porque no son movimiento. Van por `performHapticFeedback`, que respeta el ajuste de vibración al tocar del sistema.
- **Nada espera a una animación.** Ningún botón se apaga mientras algo se mueve, y lo que entra en pantalla se puede tocar desde el primer fotograma.
- **Sin dependencias nuevas**: animadores, interpoladores, vectores y la capa superpuesta de las vistas.

**Consecuencias.** Un momento nuevo se añade aquí y en `Movimiento` a la vez; `MovimientoTest` lee esta tabla y el código y falla si los números no coinciden. Las animaciones de antes del alta (el deslizamiento entre pantallas, la entrada de la bienvenida vieja) siguen con lo suyo hasta que se toquen.

**Qué la invalidaría.** Que se mida que el movimiento cuesta fotogramas en los móviles baratos para los que se diseña (DEC-021): se recorta el momento que lo cause, no el sistema. Un rediseño de marca que cambie el carácter del movimiento.

---

### DEC-040 · Los alimentos se buscan en casa: básicos propios, productos de España e índice en memoria
**Estado:** Aceptada · **Fecha:** 2026-10-01 · **Tareas:** GP-127, GP-162, GP-164 (lote 1.6.0)

**Contexto.** `GET /alimentos/buscar` preguntaba a Open Food Facts en cada pulsación (la app busca a los 400 ms) y, sin texto, por sus «populares». Open Food Facts limita a 10 búsquedas por minuto y por IP, pide no usarlo para buscar mientras se escribe, y todos los usuarios salen por la IP de Render: con unos pocos usuarios a la vez, la búsqueda se caía para todos. Y era flojo en lo básico: «pollo» no daba una pechuga de pollo genérica, sino productos con marca.

**Decisión.**

- **Tres fuentes, todas en nuestra base.** Los **básicos** (unos 450 genéricos, `datos/basicos/`): Ciqual 2025 de ANSES, con Licence Ouverte / Etalab 2.0, que permite el uso comercial citando la fuente, como base; USDA FoodData Central (SR Legacy, dominio público) para lo que falta y para el peso de las raciones. **BEDCA no**: pide autorización expresa de AESAN para uso comercial. Los **productos de España**, de la exportación de Open Food Facts (ODbL), en `productos_off` (DEC-041). Y **lo de cada usuario**.
- **El alimento dice de dónde sale.** `fuente` (CIQUAL, USDA, OFF o nada si se hizo a mano), `codigo_origen`, `revisado` (los básicos sí, los productos no) y sus raciones en `alimento_raciones`, cada peso con su fuente. Los valores siguen siendo por 100 g (`calcularCalorias` divide entre 100).
- **Tres grupos, en este orden**, y cada resultado lleva su `grupo`: **TUYO** (tus alimentos y lo que has apuntado en los últimos 60 días), **BASICO** y **PRODUCTO** (los de `productos_off` y los ya materializados). Un producto sin materializar va con `id` nulo y su código, como iban los de Open Food Facts, así que la 1.5.3 los importa al elegirlos sin cambiar nada. Sin texto: lo tuyo por uso reciente y una lista fija de básicos habituales (`busqueda/habituales.txt`). Lo de otro usuario nunca sale (DEC-027).
- **Qué casa con qué** (`Normalizador`, `IndiceTexto`): sin tildes ni mayúsculas; singular y plural llevados a la misma forma; sin «de», «con», «la», «of»…, así que da igual el orden; el último término, por prefijo (se busca mientras se escribe); una errata en palabras de 5 letras o más; unos pocos sinónimos versionados (`busqueda/sinonimos.txt`). Se busca en ES, en EN y en la marca. Delante, los que casan con todos los términos exactos y cuyo nombre empieza por el primero; dentro de los básicos, los habituales; dentro de los productos, los más escaneados.
- **Ninguna búsqueda por texto sale de casa.** El cliente de Open Food Facts ya no tiene `buscar` ni `browsePopulares`; solo queda la lectura de un código suelto, con cupo (GP-160).

**El motor: un índice invertido en memoria, no la base.** Medido en local con los 198 224 productos reales:

| | En la base (`LIKE '%…%'`) | En memoria |
|---|---|---|
| Consulta | 95–123 ms por consulta en MariaDB local (barrido completo), sin tildes, plurales ni erratas | 24 ms de mediana y 30 de peor caso por petición entera (16 ms por encima de `/actuator/health`), con todo el normalizado |
| Memoria de la API | nada | 12,5 MB de heap tras GC (68,4 → 80,9 MB), para 198 224 productos y 37 777 términos distintos |
| Construir | — | 1,3 s, en segundo plano al arrancar |

El `LIKE` con comodín delante no usa índice: barre la tabla entera en cada pulsación, y eso en un ordenador de sobremesa ya se come casi todo el presupuesto de 150 ms; en el MySQL compartido de Aiven, más. Hacer en SQL lo que hace el normalizador (plurales, erratas) obligaría a columnas o tablas de n-gramas propias, que con 200 000 productos multiplican el disco, que es justo lo escaso (1 GB entre todo). El parser ngram de MySQL no existe en MariaDB, y lo que solo existe en uno no se usa. El índice en memoria ocupa unos 12,5 MB de heap y no cuesta disco; lo que limita no es el heap sino los 512 MB del contenedor entero, y su presupuesto está en DEC-046 (corregido el 2026-10-03: decía que cabía «con holgura en los ~300 MB de heap de Render», mirando solo el heap).

**En producción, con la tabla llena** (Render gratis, 0,1 de CPU; rondas de 20 búsquedas distintas contra `/actuator/health`): **en caliente, la mediana queda 134 ms por encima de health** (321 frente a 187 ms; peor caso, 511), dentro de los 150. **Recién desplegada, no**: entre 310 y 317 ms por encima, por el JIT y la caché de planes de Hibernate en frío con tan poca CPU; se estabiliza tras unas 60 búsquedas. El índice no es lo que cuesta: una búsqueda sin resultados tarda lo que health, y en local cada búsqueda gasta unos 4 ms de CPU, la mitad en Hibernate. El detalle y lo que queda por probar, en el informe del lote 1.6.0.

**Consecuencias.** La API tarda unos segundos más en tener el grupo de productos tras arrancar: el índice se construye en segundo plano y, mientras, la búsqueda sigue con TUYO y BASICO. Tras cada importación se reconstruye cuando lleva 30 s sin llegar ningún lote. El catálogo (básicos y materializados, unos pocos miles) se reconstruye en la primera búsqueda después de cualquier cambio. Lo de cada usuario se lee de la base en cada búsqueda, porque es poco y cambia a cada comida apuntada.

**El código de barras de un alimento propio y el de un producto conviven** (para el «Créalo» de la 1.6.1). `uq_alimentos_barcode`, único en toda la tabla, pasa a ser único **por dueño**: `(barcode, usuario_id)`. En el catálogo, la unicidad de los productos la da `(fuente, codigo_origen)`. Así, si escaneas un código que no existe y creas tu alimento con ese código, es tuyo: a ti te sale primero al escanearlo; a nadie más le sale nunca. Si más adelante ese producto llega a `productos_off`, a los demás les sale el producto y a ti el tuyo, sin choque. MySQL no permite una columna generada sobre `usuario_id` mientras su clave ajena borre con SET NULL, por eso no se hizo con `IFNULL(usuario_id, 0)`.

**Qué la invalidaría.** Que el índice no quepa: con más de un millón de productos o con el plan de Render más pequeño, medir otra vez y pasar a n-gramas en la base o a un servicio de búsqueda. Que Open Food Facts cambie la licencia de la exportación. Que los usuarios pidan un catálogo colaborativo (DEC-032).

---

### DEC-041 · Los productos de Open Food Facts llegan cada semana por la API, con una clave propia
**Estado:** Aceptada · **Fecha:** 2026-10-01 · **Tarea:** GP-164 (lote 1.6.0)

**Contexto.** Para buscar productos sin preguntar a Open Food Facts hay que tenerlos. La exportación completa pesa 1,3 GB comprimida y tiene 4,5 millones de productos; la API vive en 512 MB y Aiven da 1 GB de disco para todo.

**Decisión.**

- **La importación no corre dentro de la API.** Un workflow de GitHub Actions (`importar-productos.yml`), los lunes y a mano, descarga la exportación, filtra con `datos/productos/filtrar_off.py` y manda los productos en lotes de 1000 a `POST /importacion/productos`. La API solo recibe lotes: nada descarga, nada toca la base por otro camino (DEC-035).
- **Qué entra**: vendido en España, con código, nombre y los cuatro valores por 100 g, sin cifras imposibles (nada por encima de 100 g por cada 100 g, kcal que cuadran con los macros), y sin duplicados (mismo nombre normalizado, marca y kcal: el más escaneado). La API vuelve a comprobarlo en cada lote.
- **Presupuesto: 200 MB** para `productos_off` y sus índices; si no caben, los más escaneados. Medido: 198 224 productos en 19,5 MB de datos y 9,5 MB de índices, lo mismo en MariaDB local que en el MySQL de Aiven; caben todos, sin `--max`. En el disco de Aiven, la primera importación ocupó unos 60 MB (del ~31 % al 37,2 % de 1 GB): la tabla más registros y espacio de InnoDB.
- **La ruta pide una clave propia, no ADMIN.** Cabecera `X-Clave-Importacion`, comparada en tiempo constante y antes de leer el cuerpo. Una cuenta ADMIN guardada en un secreto de GitHub daría a quien la robara todo el panel (desactivar cuentas, cambiar el catálogo); la clave solo deja escribir en `productos_off`. Sin ella, 403 para todos, ADMIN incluido. En producción es `IMPORTACION_CLAVE`, sin valor por defecto (DEC-016) y de 32 caracteres o más: si falta, la API no arranca. El mismo valor va en el secreto `IMPORTACION_CLAVE` del repositorio.
- **Reimportar no toca lo materializado.** Un producto elegido se copia a `alimentos` (DEC-032) y las comidas apuntan a esa copia; la importación actualiza `productos_off`, nunca `alimentos`.
- **ODbL.** Los datos de Open Food Facts no se modifican; lo normalizado vive solo en la memoria de la API. Derivan de Open Food Facts `productos_off` y las filas de `alimentos` con `fuente = 'OFF'`: si se mejoran, se ofrecen de vuelta.

**Consecuencias.** Un producto que se quite de Open Food Facts sigue en `productos_off` hasta que se limpie a mano: la importación añade y actualiza, no borra. Un producto nuevo tarda como mucho una semana en salir en la búsqueda, salvo que alguien lo escanee (GP-160), que lo trae al momento.

**Qué la invalidaría.** Que los productos de España dejen de caber en el presupuesto aun quedándose con los más escaneados. Que haga falta más de una fuente de productos: entonces la clave se queda corta y conviene una cuenta de servicio con su rol (DEC-012).

### DEC-042 · Añadir en un viaje, la ración que se guarda, el envase como ración y los avisos sin quién
**Estado:** Aceptada · **Fecha:** 2026-10-02 · **Tarea:** GP-162, GP-160 y GP-167 (lote 1.6.1)

**Contexto.** La 1.6.1 trae a la app los tableros 4 a 7 del lienzo de la nutrición: Añadir, Buscar, el escáner y la ficha. Para que la ficha añada con una ración, el escáner apunte el envase y se pueda reportar un dato malo, la API necesitaba cuatro cosas que no tenía, y cada una obligaba a elegir.

**Decisión.**

- **Añadir es una ruta, no cuatro peticiones.** `POST /comidas/anadir` recibe el día, el tipo de comida, el alimento (por id o por código, que se materializa) y la cantidad (en gramos o en raciones), y encuentra o crea la comida del usuario del token (DEC-013). Todo en una transacción: un código que no existe no deja una comida vacía. **Si el alimento ya está en esa comida, se suma** en una sola línea (misma ración, más raciones; si no, en gramos) en vez del 400 de duplicado de `POST /alimentos-comida`, que no cambia para las builds repartidas.
- **La ración elegida se guarda, pero mandan los gramos.** `alimentos_comida` gana `racion_id` y `raciones`, opcionales; las calorías y los macros salen de los gramos, como siempre. La ración solo dice cómo enseñarlo («2 × rebanada (56 g)»). Cambiar los gramos a mano la quita. Un producto que aún no está en el catálogo no tiene ids de ración: se elige por su **posición**, que es la misma al materializarlo.
- **El envase es una ración de un producto, solo si se lee sin dudas y pesa 500 g o menos;** va primero salvo que el producto declare una ración más pequeña (corregido el 2026-10-03, DEC-045: decía que era siempre la primera). Una cantidad con su unidad, o un multipack de unidades iguales (que da «1 unidad»); el volumen cuenta 1 ml = 1 g, como hace Open Food Facts con sus valores. Lo demás (cifras sueltas, piezas, onzas, neto y escurrido) no se lee. **El tope de 500 g es una decisión de producto**: el escáner propone la primera ración, y un kilo de arroz o un litro de leche no se comen de una vez. De 198 224 productos, 55 413 traen un envase legible y 42 252 de ellos de hasta 500 g, más 4 957 multipacks.
- **Los avisos no guardan quién.** Reportar un alimento guarda el alimento (o el código, si no está materializado), un motivo de una lista cerrada y las veces, sin usuario y sin texto libre: no es un dato personal y no entra en la política de privacidad ni en el borrado de la cuenta. El freno por cuenta (el mismo aviso cuenta una vez al día; como mucho diez por hora) vive en memoria y **no responde distinto**: quien se pasa no lo nota, y no hay nada que tantear. Lo tuyo no se reporta (400): lo editas tú, y el administrador no ve los alimentos de nadie (DEC-027).
- **El cupo de Open Food Facts también es por cuenta** (GP-167): 3 lecturas nuevas por minuto y 30 al día, además de las 10 por minuto de toda la API. Una cuenta no puede dejar sin escáner a las demás.
- **El escáner lee en el móvil con el modelo de Google Play Services**, no con el que va dentro del APK. El de dentro lleva una librería nativa de 3 a 5 MB por arquitectura, y un APK que se reparte fuera de Play las lleva todas: el release pasaba de 6,7 a 28,8 MB. Con Play Services queda en 7,5 MB, y el modelo se descarga la primera vez (instalada desde Play, con la app).

**Consecuencias.** Los productos ya materializados antes de la 1.6.1 no tienen la ración del envase: la ganan si se vuelven a materializar, no antes. Un móvil sin Google Play Services no puede leer códigos con la cámara, pero sí escribirlos. Los avisos de una cuenta que se pasa del freno se pierden, y es lo que se quiere.

**Qué la invalidaría.** Que los avisos necesiten respuesta a quien los envió (entonces hace falta el usuario, y entra en la política). Que la app se publique solo por Play con App Bundle: entonces el modelo dentro de la app cuesta 3 a 5 MB por móvil y no 22, y se puede reconsiderar.


### DEC-043 · El plural es un dato, la ración propia es de una lista, quitar no pregunta y la categoría es una clave
**Estado:** Aceptada · **Fecha:** 2026-10-02 · **Tarea:** GP-162 (decisiones 15, 16 y 17 del lienzo), GP-172, GP-174, GP-175 y GP-176 (lote 1.6.2)

**Contexto.** El 02-10 el propietario vio la 1.6.1 en su móvil y cambió tres tableros (`documentacion/diseno/2026-10-02-nutricion/`): elegir la comida con una etiqueta y una hoja, una comida con más vida y crear un alimento como en la etiqueta. Construirlos obligaba a decidir cuatro cosas que el lienzo no dice.

**Decisión.**

- **El plural de una ración es un dato, no una regla de la app.** La API manda la unidad de cada ración en singular y en plural, en el idioma de la petición (`unidad`, `unidadPlural`; en la línea de una comida, `racionUnidad` y `racionUnidadPlural`), sacadas de un diccionario versionado junto a los de la búsqueda (`busqueda/unidades_racion.txt`) que cubre todo nombre que empieza por «1 ». Así «flan» da «flanes» y «flans», «mango» da «mangos» y «mangoes», y la app solo elige: una, singular; si no, plural («1,5 rebanadas»). Un nombre sin «1 » («Media taza») no tiene unidad y se escribe como en la 1.6.1, «2 × media taza (120 g)»; una API anterior, igual. `nombre` y `racionNombre` no cambian: los usan las builds repartidas.
- **La ración de un alimento propio sale de una lista cerrada**: unidad, ración, envase o rebanada, con sus gramos (más de 0 y hasta 2000), y una sola por ahora, aunque el campo sea una lista para no cambiar el contrato cuando sean más. Con texto libre la app no sabría ponerla en plural ni marcarla al editar (`RacionDTO.clave`). Cambiarla reescribe su misma fila, para que las líneas de comidas que la usan no la pierdan; quitarla deja esas líneas en gramos. En un alimento del catálogo, 400: sus raciones están revisadas.
- **Quitar un alimento de una comida no pregunta.** La fila se va, los números pasan a su valor nuevo y un aviso con «Deshacer» la devuelve sin llamar a la API. El borrado se manda cuando el aviso se va —por tiempo, porque se quita otro o porque se sale de la pantalla, también a otra app—, y al salir hacia el diario se espera la respuesta, para que el diario no cuente lo quitado. Si falla, la fila vuelve a su sitio y se dice. El aviso dura **5 s**, no los 2,75 de Material: no daba para leer el nombre y llegar al botón; Material lo alarga además a lo que pida el sistema por accesibilidad, nunca menos. Dice «Quitado: Arroz blanco» y no «Arroz blanco quitado», que en español obliga a concordar con el género del alimento.
- **La categoría viaja como clave canónica, en español** («Carnes y aves», `AlimentoController.CATEGORIAS`), y la app la traduce y le pone su icono (`Categorias`). En la línea de una comida (GP-176) la API no la traduce. En el alimento sí lo hacía y lo sigue haciendo: `categoria` sale en inglés en los básicos que tienen traducción (`AlimentoMapper`), porque las builds repartidas la enseñan tal cual. Por eso, desde la 1.6.3, `AlimentoDTO` lleva además `categoriaClave`, la canónica sin traducir, y la app usa esa (corregido el 2026-10-02: esta línea decía que la API no la traducía «en ningún sitio»). Lo que la app manda al crear o editar es siempre la clave: si mandara el nombre traducido, un alimento creado en inglés quedaría en una categoría que no existe. Sin elegir, el alimento se guarda como «Otro».
- **Las cifras de crear un alimento se escriben por 100 g o por la ración, y se guardan por 100 g.** La app hace la cuenta (`EtiquetaAlimento`) y conserva el valor exacto al cambiar de base, para que ir y volver no cambie lo que se guarda. Solo la energía es obligatoria, y 0 vale; un macro vacío es 0 y la fibra vacía no se sabe. Más de **900 kcal en 100 g** no se guarda: es más que la grasa pura, y casi siempre son kJ. Cada macro va de 0 a 100 g por 100 g, y grasas, hidratos, fibra y proteínas suman como mucho **102 g**: las etiquetas redondean cada cifra y la suma puede pasarse un poco. El aviso de las calorías compara la energía con 4·P + 4·C + 9·G y **cuadra** si se separa 20 kcal o un 15 % como mucho, lo que sea mayor; no impide guardar, porque la fibra y el alcohol cuentan aparte.

**Consecuencias.** Un nombre de ración nuevo que empiece por «1 » necesita su línea en el diccionario, en los dos idiomas; `UnidadesRacionTest` lo exige para todas las de la semilla. Lo quitado y no confirmado se pierde si el sistema mata la app con el aviso a la vista: queda en la comida, que es el lado seguro. Una etiqueta con la energía en kJ de verdad (más de 900) hay que pasarla a kcal a mano.

**Qué la invalidaría.** Un idioma con más de dos formas de plural (entonces la API tendría que mandar las formas de CLDR, no dos). Que el propietario quiera raciones propias libres o varias por alimento: la lista de `ClaveRacion` y el «una sola» son los que se cambiarían, no el contrato.


### DEC-044 · El «+» añade lo último sin preguntar, añadir no cierra Añadir, los favoritos se proponen una vez y una línea va por raciones solo si cuadra
**Estado:** Aceptada · **Fecha:** 2026-10-02 · **Tarea:** GP-162 (decisiones 1, 2 y 6 del lienzo), GP-177, GP-178, GP-179 y GP-181 (lote 1.6.3)

**Contexto.** Apuntar lo de siempre costaba cuatro idas y vueltas por alimento: abrir la ficha, elegir la cantidad, volver al diario y entrar otra vez en Añadir. El lienzo del 02-10 lo resuelve con un «+» en cada fila, una barra de «Hecho» y los favoritos, y construirlo obligaba a decidir qué añade el «+», qué pasa si se toca dos veces, cuándo se propone un favorito y qué es una línea cuya ración ya no pesa lo mismo.

**Decisión.**

- **El «+» añade sin preguntar la última cantidad que apuntaste de ese alimento**: la de su línea más reciente, por la fecha de la comida y, a igualdad, la última creada (`ultima` en lo tuyo de la búsqueda y en los favoritos). Si nunca lo apuntaste, lo que propondría la ficha: la primera ración por una, o 100 g. El «+» y la ficha nunca proponen cosas distintas. Para un producto, la primera ración es su ración declarada si es más pequeña que el envase y, si no, el envase (DEC-045; hasta la 1.6.4 era siempre el envase, y unas galletas de 400 g se añadían enteras).
- **Cambia al tocar; la API va detrás, de una en una por alimento.** El «+» se vuelve ✓ al momento (momento 15). Un segundo toque mientras el primero viaja no sale a la API hasta que vuelva la respuesta, y entonces se hace lo que falte para que la API quede como dice la fila: nunca dos líneas, nunca un borrado perdido. Si añadir falla, la fila vuelve a «+»; si quitar falla, a ✓; las dos cosas se dicen.
- **Quitar es exacto.** `POST /comidas/anadir` devuelve `anterior`: null si la línea es nueva, o la cantidad que tenía (gramos, ración y cuántas) si se sumó a una que ya estaba. Quitar borra la línea nueva o la deja como estaba con el PATCH de siempre.
- **Añadir ya no cierra Añadir.** Lo añadido con el «+», desde la ficha, desde la hoja del escáner o tras crear un alimento se junta en la barra de abajo («2 añadidos a la merienda · +260 kcal»; si va a dos comidas, sin nombrar ninguna). «Hecho», cerrar y atrás hacen lo mismo: volver, esperando a lo que aún viaje y diciéndolo si tarda más de 300 ms. El aviso «Añadido a la cena» sale solo si todo fue a una comida y no es la de origen.
- **Favoritos.** De una cuenta y de un alimento que esa cuenta ve, en `/favoritos`: marcar y quitar repetibles, por id o, un producto que aún no está en el catálogo, por su código (se materializa). Ordenados por las comidas en que aparecen en los últimos 60 días, hoy incluido; a igualdad, el usado más reciente y luego el nombre. El id ajeno es un 403 que no dice si existe; el invitado no tiene favoritos, como no añade. Son datos de nutrición: la política de privacidad los nombra y el borrado de la cuenta se los lleva.
- **La propuesta sale una vez por alimento.** Como mucho uno que no es favorito y aparece en 4 comidas o más de los últimos 14 días (hoy y los 13 anteriores); si hay varios, el más usado. Lo que alguna vez fue favorito, aunque se quitara, y lo rechazado no se vuelven a proponer nunca (`propuestas_favorito`): ya lo conoces, y una propuesta que vuelve es un aviso que molesta.
- **Una línea va por raciones solo mientras sus gramos sean los de su ración por cuántas**, con medio gramo de margen (GP-177). Si alguien cambia el peso de su ración, la línea sale en gramos, sin nada de la ración, y todas las builds la enseñan y la editan así. Se decide al leer: no se reescribe ningún dato, y si la ración vuelve a pesar lo que pesaba, la línea vuelve a salir con ella. La misma regla vale para `ultima` y para sumar al añadir.
- **El «+» mide 44 dp, como el tablero, dentro de una zona de toque de 48**: la regla de las zonas pulsables (`CabeceraTest`) no baja de 48.

**Consecuencias.** `SentenciasBusquedaTest` no cambia: la última línea y los favoritos de lo tuyo van en la misma consulta. Un producto añadido con el «+» se añade con su primera ración (su ración declarada o, si no la tiene más pequeña, el envase); quien quiera otra cantidad abre la ficha. Lo añadido y aún sin respuesta se pierde si el sistema mata la app en ese momento: la fila decía ✓ y la línea no existe, que es el lado seguro.

**Qué la invalidaría.** Que los usuarios esperen que el «+» pregunte la cantidad (entonces la ficha vuelve a ser el único camino). Que las propuestas se queden cortas o molesten: el 4 en 14 días y el «una vez» son los números que se tocarían.


### DEC-045 · Copiar una comida es añadir cada alimento, «lo que sueles» se cuenta en días, «¿Copiar la de ayer?» sale donde la comida está vacía y la ración de un producto va delante de su envase
**Estado:** Aceptada · **Fecha:** 2026-10-03 · **Tarea:** GP-162 (comidas recientes, «¿Copiar la de ayer?» y «Lo que sueles» del lienzo), GP-182, GP-183, GP-184 y GP-185 (lote 1.6.4)

**Contexto.** Con el «+» de la 1.6.3 se apunta un alimento en un toque, pero una merienda de cuatro cosas siguen siendo cuatro toques, y casi siempre es la de ayer. El lienzo del 02-10 lo resuelve con tres atajos —«Comidas recientes» en Añadir (tablero 4), «¿Copiar la de ayer?» en el diario y en una comida vacía (tableros 1 y 2b) y «Lo que sueles merendar» (2b)— y no dice qué es reciente, qué es soler ni qué pasa con lo que ya estaba. Además, al revisar la 1.6.3, el «+» de unas galletas añadió el paquete de 400 g (GP-182).

**Decisión.**

- **Las comidas recientes son tres como mucho, de los 14 días que acaban en el día de destino** (`GET /comidas/recientes`, con el día y el tipo de la comida a la que se va a añadir). Ese día cuenta («Almuerzo de hoy»), los posteriores no. Nunca la de destino (ese día y ese tipo) ni una sin nada que copiar. Primero, la más reciente del mismo tipo anterior a ese día —«la de ayer», si la hubo—; después, las más recientes de cualquier tipo: por día y, en el mismo día, la más tardía primero (cena, merienda, comida, almuerzo, desayuno). La ventana cuenta desde el día de destino y no desde hoy: al apuntar un día pasado, lo reciente es lo de alrededor, y la API no tiene que adivinar qué día es en el móvil. Cada una lleva sus líneas como las de una comida, pero **solo las que se pueden copiar**, y las kcal de esas: lo que se enseña es lo que se copia. Tres consultas, sean cuantas sean.
- **Copiar es añadir cada alimento con la regla de siempre** (`POST /comidas/copiar`): con su ración si sigue pesando lo mismo (GP-177) y si no en gramos, a la comida de destino del token, que se encuentra o se crea; si el alimento ya está, se suma. Lo desactivado o lo que la cuenta ya no ve se salta; si no queda nada, 400 y no se crea una comida vacía. La comida de origen de otra cuenta y la que no existe dan **el mismo 403**, como los favoritos, y tampoco el ADMIN copia la de otro: el destino es siempre del token. Copiar una comida sobre sí misma (mismo día y tipo), 400. La respuesta trae la comida y, por cada alimento copiado, su línea y `anterior`, como añadir: **no hay «Deshacer» de la copia**, cada alimento se quita como cualquier otro.
- **«Lo que sueles» se cuenta en días, no en líneas** (`GET /comidas/habituales`): hasta tres alimentos apuntados en ese tipo de comida en dos días distintos o más de los últimos 60, hoy incluido; del más repetido al menos y, a igualdad, el apuntado más recientemente. Dos líneas el mismo día no son costumbre. Su `ultima` es la última cantidad **en ese tipo de comida**, que es lo que añade su «+».
- **«¿Copiar la de ayer?» sale cuando la comida está vacía y la misma comida del día anterior tuvo algo que copiar.** En el diario, solo en la comida que toca por la hora y solo si el día es hoy, dentro de su tarjeta como en el tablero 1: «2 alimentos · 203 kcal», la ✓, la ✗ y debajo cada alimento con su cantidad y sus kcal, para saber qué se copia antes de copiarlo. En la pantalla de una comida vacía, sea el día que sea («el día anterior» es el de esa comida). Vacía quiere decir sin kcal, como «Sin registrar». La ✗ la pliega y no vuelve **ese día para esa comida**, ni en el diario ni en su pantalla: se guarda en el móvil, por cuenta, y se recuerdan los últimos 60 descartes. Si la petición falla, la tarjeta no sale.
- **En Añadir, lo copiado cuenta como lo añadido.** «Copiar» pasa a «Copiada», con su ✓, al tocarlo, y ya no se toca; la API va detrás y, si falla, vuelve a «Copiar» y se dice. En la barra de «Hecho» cuenta la cantidad y las kcal copiadas, no las de la línea ya sumada, y las filas de esos alimentos quedan en ✓ con su quitar exacto. Si un alimento copiado coincide con uno que se acaba de añadir con el «+» a esa comida, es la misma línea: una fila, la barra cuenta los dos, y su ✓ la deja como antes del primero (el `anterior` más antiguo: la línea solo crece al sumar, así que es el que no existía o el de menos gramos). «Hecho» espera también a una copia en vuelo. El nombre de cada una se dice frente al día de hoy del móvil: «de hoy», «de ayer», el día de la semana de hace 2 a 6 días y, si no, la fecha.
- **Cada fila de Añadir es un alimento en una comida** (GP-185): lo añadido a la merienda no deja en ✓ la fila cuando la etiqueta pasa a la cena, y se puede añadir también ahí; al volver a la merienda, vuelve a ✓. La barra cuenta lo de todas. **Tocar una fila en ✓ cuyo «+» aún viaja espera a la línea** y abre la ficha con ella (GP-183); si añadir falla, no se abre nada. **En Favoritos, con la propuesta y ningún favorito, debajo sale el texto de la pestaña vacía** (GP-184); aceptar la propuesta mete el favorito en la lista al momento (momento 19) y el texto se va; si la API falla, vuelve como estaba.
- **La ración que declara un producto va delante de su envase si es más pequeña** (GP-182). Corrige DEC-042 y DEC-044, que decían que la primera ración de un producto era siempre el envase. Unas galletas de 400 g con ración de 30 proponen 30 g en la ficha, la hoja del escáner y el «+», y el envase sigue de segunda opción. En un multipack la unidad sigue primero, porque ya es lo que se come de una vez; sin ración declarada, o si no es más pequeña, como antes. El tope de 500 g no cambia. Los productos ya materializados se reordenan con una migración que solo cambia el orden: los ids de sus raciones se quedan, porque hay líneas que los usan.
- **El momento 18 entra en DEC-039 con lo que se puede enseñar en este lote**: la tarjeta se pliega y, en la pantalla de una comida, lo copiado cae en la lista. Los alimentos que caen en la tarjeta del diario y las cifras del día que cuentan llegan con el diario nuevo de la 1.6.7.

**Consecuencias.** Una comida con solo cosas sin kcal (el agua, un café solo) cuenta como vacía y puede proponer la de ayer. Un descarte se pierde con los datos de la app, y entonces la tarjeta vuelve a salir ese día. Un producto que alguien ya apuntó con el envase sigue apuntado así (manda la línea, no el orden), pero su próximo «+» propone la ración, salvo que su última cantidad fuera el envase, que es lo que se repite (DEC-044). Copiar no avisa de lo que se ha saltado: lo que se salta no se enseñaba en la lista.

**Qué la invalidaría.** Que se copie más a menudo una comida que no es la de ayer (entonces la de ayer no tendría por qué ir primera). Que «lo que sueles» proponga cosas que ya no se comen: los 60 días y los dos días son los números que se tocarían. Que los usuarios quieran deshacer una copia de un toque: entonces hace falta quitar varias líneas en una petición, que hoy no existe.


### DEC-046 · La memoria de la API tiene presupuesto, la foto de perfil pesa como mucho 1 MB y Spring Data REST no está
**Estado:** Aceptada · **Fecha:** 2026-10-03 · **Tarea:** GP-186, GP-187 y GP-188 (lote 1.6.5)

**Contexto.** El 03-10 Render mató el contenedor de la API por pasarse de memoria, y cada caída eran unos 4 minutos sin API. El Dockerfile solo decía `-XX:MaxRAMPercentage=60`: el heap podía llegar a 297 MB, y fuera del heap ya había unos 160 MB nada más arrancar (DEC-040 miraba solo el heap). Producción vivía al 92–93 % de sus 512 MB, y el plan gratis de Render no enseña la memoria ni deja nada en el log al matar.

**Decisión.**

- **La memoria, a la vista.** Una línea `Memoria:` en el log al arrancar y cada 10 minutos, con lo que mira Render (uso y límite del cgroup, v1 o v2, y cuánto es memoria anónima y cuánto caché), el heap usado, reservado y máximo, metaspace, code cache, memoria directa e hilos; en WARN si el contenedor pasa del 90 %, una vez por subida (se rearma al bajar del 85 %).
- **Cada parte de la memoria con su tope**, de modo que todas al máximo, más lo que no tiene tope, dejen al menos 40 MB libres de los 512. Medido con los 198 224 productos en Docker a 512 MB y 0,1 de CPU, con NMT, en cinco momentos (recién arrancada, construyendo el índice, tras recorrer la app con jOOQ, reconstruyendo el índice tras una importación y subiendo fotos):

  | Parte | Tope | Medido |
  |---|---|---|
  | Heap (Serial, el que elige la JVM con 1 CPU) | 160 MB | 74 MB vivos en reposo; como mucho 108 tras un GC completo, al reconstruir el índice con el anterior vivo |
  | Metaspace (con el espacio de clases) | 150 MB | 125 tras recorrer la app entera |
  | Code cache (solo C1) | 24 MB | 16 |
  | Memoria directa | 16 MB | 0 |
  | Pilas de hilos | 512 KB cada una | 32–35 hilos; Tomcat con 20 como mucho y 5 conexiones a la base |
  | Sin tope: symbol, GC y resto | — | ~55 + ~10 MB |
  | Sin tope: fuera de lo que cuenta la JVM (malloc, con `MALLOC_ARENA_MAX=2`) | — | ~30 MB |

  Con todo al máximo, unos 445 MB. Medido de verdad, el pico pasa de 501 MB (y muerte por falta de memoria al reconstruir el índice y al subir fotos) a 438 MB, y el contenedor recién arrancado de 412 a 335. El arranque, en el mismo Docker a 0,1 de CPU, de 1228 a 207 s.
- **El heap no se recorta más:** el índice se construye después de que el health dé verde, y con `-XX:+ExitOnOutOfMemoryError` un heap corto sería un bucle de reinicios. Con 160 MB queda medio heap libre sobre lo vivo.
- **Solo C1** (`-XX:TieredStopAtLevel=1`): la mitad de code cache, sin la memoria del compilador C2, y el arranque más corto. En el mismo Docker a 0,1 de CPU, frente a C2 con todo lo demás igual: arranque de 207 s frente a 347, búsqueda 222 ms por encima de health frente a 416 (rondas de 20 tras 20 de calentamiento, como en GP-168), y 35 MB menos de pico. Falta medirlo en producción, que es lo que manda.
- **La foto de perfil, como mucho 1 MB**, en todos los perfiles y en un solo sitio (`spring.servlet.multipart`). Por encima, 413 que dice que es la foto y cuánto admite, también cuando lo para el multipart de Tomcat (que se traga hasta 10 MB del resto para que el 413 llegue). La app la manda reducida: 512 × 512, JPEG a calidad 85, sin metadatos, decenas de KB. Guardarla no lee la anterior y servirla lee los bytes con JDBC: una consulta de Spring Data que devuelve `byte[]` los convierte uno a uno, y una foto de 1 MB costaba 375 MB de memoria.
- **Spring Data REST no está en el classpath** (corregido el 2026-10-03; antes decía que ningún repositorio se publicaba por omisión, con `detection-strategy=annotated` y `exported = false` en cada uno). La API se sirve solo con controladores propios, cada uno con sus reglas de acceso; con la librería, cada repositorio era una ruta posible que no pasaba por ningún servicio, y había que acordarse de anotar cada uno nuevo. Sin ella no hay nada que anotar ni que olvidar. Los errores los resuelve el resolvedor de Spring MVC con la negociación de `WebConfig` y siguen saliendo en JSON sin `Accept` (GP-075). `SinSpringDataRestTest` falla si la librería vuelve. Se quitó por seguridad, no por memoria: en el mismo Docker, 9 librerías menos en el jar, metaspace 103 MB frente a 105 y la misma memoria anónima recién arrancada (333 frente a 328, dentro del ruido).

**Los dos fallos que arregló el lote** (contados el 2026-10-03, con los arreglos ya en producción y confirmados por el propietario):

- **S22 · los favoritos de cualquiera, con un token de invitado.** `IFavoritoRepository` era el único repositorio sin `exported = false`, y Spring Data REST lo publicaba solo en `/api/favoritoes`, fuera de cualquier controlador. Como la seguridad pedía solo estar autenticado, también valía un token de invitado: con él se leían, se creaban y se borraban los favoritos de cualquier cuenta. Confirmado en local antes del arreglo (lectura 200, alta 201, borrado 200). En producción ya daba 404 antes de desplegar el arreglo, por la variable de entorno que el propietario había puesto en Render. Se cerró primero con `detection-strategy=annotated` y la anotación (`8cc2d32`), y del todo quitando la librería (`a223c20`).
- **S23 · cualquier cuenta podía tumbar la API subiendo fotos.** La foto de perfil se aceptaba hasta 5 MB sin reducir, y guardarla la copiaba varias veces en memoria (la instantánea de Hibernate y la carga de la anterior). Con producción ya al 92–93 % de sus 512 MB, unas pocas subidas seguidas pasaban del límite y Render mataba el contenedor: unos 4 minutos sin API cada vez. Medido en local: el contenedor muere por falta de memoria al subir fotos de 5 MB. Se cerró con el tope de 1 MB, guardar sin cargar la anterior y servir con JDBC (`10a50cc`, `5f1be94`), y con el presupuesto de memoria (`d2bf7d2`). Al medirlo apareció un tercer camino, de este mismo lote y arreglado antes de fusionar: la consulta de Spring Data que devolvía `byte[]` convertía cada byte en un objeto, unos 375 MB por cada foto de 1 MB.

**Consecuencias.** Las builds repartidas antes de la 1.6.5 mandan la foto sin reducir: con una de más de 1 MB reciben el 413 y su aviso. Si la app crece en clases (una librería grande, más consultas), el metaspace se acerca a su tope: la línea `Memoria:` lo enseña antes de que pase.

**Qué la invalidaría.** Que la línea `Memoria:` de producción pase del 85 % de forma sostenida: entonces se mide qué parte creció y se recorta esa, o se pasa a un plan con más memoria. Que la búsqueda en caliente en producción quede por encima de los 150 ms sobre health con C1 y no con C2: entonces vuelve C2 con su code cache. Que la app necesite subir otras imágenes más grandes que la foto.

---

## Pendientes de decidir

Se registran aquí para que no se decidan por omisión.

### DEC-024 · Proveedor de correo transaccional
**Estado:** PENDIENTE DE CONFIRMAR

La recuperación de contraseña usa **Brevo por su API HTTP**, no por SMTP. El SMTP se probó y **no es viable**: Render bloquea los puertos 25, 465 y 587 en los servicios del plan gratuito, así que `JavaMailSender` no podía entregar nada, y el `MailHealthIndicator` que Actuator registraba al detectar el starter de correo tumbó producción entera abriendo una conexión SMTP en cada health check (`4b35d6e`, `5e2c21a`). Eso no es una preferencia revisable mientras el hosting sea el plan gratuito de Render: **el transporte tiene que ir por HTTPS**.

Lo que sí sigue **pendiente de confirmar como decisión**: el proveedor (Brevo frente a alternativas), los límites del plan gratuito y el coste al crecer, y el remitente con dominio propio en lugar del compartido. El borrado de cuenta se apoyará en la misma infraestructura.

**Ya resuelto de lo que estaba abierto —qué pasa cuando el envío falla—**: la excepción no se propaga al endpoint, porque `POST /auth/forgot-password` responde lo mismo exista la cuenta o no y dejarla escapar lo convertiría en un detector de cuentas; pero el fallo **sí deja rastro**, con el código de estado y el cuerpo del error en el log, y el `messageId` de Brevo cuando el envío sale. El código de seis dígitos no aparece en el log por ningún camino.

### DEC-025 · Destino del histórico de calorías de entrenamiento
**Estado:** PENDIENTE · **Depende de:** DEC-004

Hay datos de calorías estimadas ya almacenados en producción. Al retirar la métrica hay que decidir si la columna se conserva sin mostrarse durante una versión o se elimina de inmediato. Propuesta: conservarla primero, eliminarla en una migración posterior, para no perder información de forma irreversible antes de confirmar que nada depende de ella.

---

*Formato ADR. Añadir una entrada cuesta cinco minutos; reconstruir dentro de un año por qué se decidió algo cuesta mucho más.*
