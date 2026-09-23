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

**La excepción es el escáner.** `POST /alimentos/importar` materializa un producto de **Open Food Facts** por código de barras y sí crea una fila **sin dueño**, porque es catálogo real y no la comida de nadie, y lo dispara un usuario normal al escanear. No pasa por la ruta de creación: construye la entidad desde el producto externo. Es la diferencia entre *escribir en el catálogo* y *traerse un producto que ya existe*.

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

**El orden importa: 403 antes que 404.** La comprobación de existencia va **después** de la de propiedad, para que a quien no es dueño se le responda `403` sin decirle de paso si el id existe. DEC-027 y DEC-014 no se relajan; lo único que cambia es **cómo se afirma el aislamiento en los tests**: donde el id es de catálogo público, el test pasa de exigir `404` a exigir `200` con lista vacía, que es lo que siempre quiso decir.

**En el cliente, el 404 vuelve a ser un error.** Se retira el silencio de `UiFeedback` y los apaños de las nueve pantallas que trataban el 404 como «sin datos». Dejarlos habría sido peor que antes: con la lista vacía llegando ya como `200`, seguir callando el 404 solo escondría errores de verdad.

**Consecuencias.** Es un **cambio de contrato** y toca a las builds repartidas fuera de Play, que no se actualizan solas. Se asume a sabiendas y ahora, antes de publicar: en cuanto la app esté en la tienda, esta convención queda congelada. Una build vieja contra la API nueva deja de ver 404 donde los esperaba —y como los trataba como estado vacío, el resultado es el mismo o mejor.

**Qué la invalidaría.** Nada razonable. Lo único que la reabriría es que apareciera un endpoint donde la lista vacía signifique de verdad «esto no existe», y entonces el problema es el diseño de ese endpoint, no la convención.

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
