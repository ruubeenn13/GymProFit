# GymProFit — Registro de decisiones de producto y arquitectura

Decisiones tomadas, con su porqué. Sirve para no volver a discutir lo ya discutido, y para saber **qué evidencia haría cambiar de idea** — una decisión sin condición de revisión acaba convertida en dogma.

**Reglas del archivo**

- Los IDs son estables y no se reutilizan. Una decisión que deja de valer se marca `Sustituida por DEC-xxx`; no se borra.
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
**Estado:** Aceptada · **Fecha:** 2026-09-18 · **Deuda conocida**

**Contexto.** El producto calculaba las calorías de una sesión como `calorías × series × repeticiones`. Es un número sin fundamento fisiológico: no depende de la carga, ni del peso corporal, ni del descanso, ni de nada real.

**Decisión.** No se muestra ni se almacena ninguna estimación de calorías quemadas en entrenamiento. Si algún día se recupera, será con un modelo defendible y presentada como estimación, no como dato.

**Consecuencias.** Un número inventado con apariencia de dato es peor que no tener número: el usuario decide sobre él, y en nutrición puede llevarle a comer de más.

**No afecta a** las calorías de **nutrición**, que proceden de los alimentos registrados y son reales.

**Contradicción actual.** El cálculo sigue vivo en `RegistrarSesionActivity` y el valor se persiste y se muestra en varias pantallas. Es deuda pendiente de retirar, no una excepción concedida.

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

## Pendientes de decidir

Se registran aquí para que no se decidan por omisión.

### DEC-023 · Identificador de la aplicación
**Estado:** PENDIENTE · **Bloquea:** cualquier subida a Play Console, incluida la interna

El `applicationId` es `es.pmdm.gymprofit`, donde PMDM es el módulo académico del ciclo. **Tras la primera publicación no se puede cambiar nunca**: queda en la URL de la ficha y en cada instalación. Alternativa propuesta: `com.gymprofit.app`. Cambiarlo obliga a recrear la app en Firebase. Hay que decidirlo de forma consciente y anotar aquí el resultado, se cambie o no.

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
