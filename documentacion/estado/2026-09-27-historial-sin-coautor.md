# Historial sin coautor — 2026-09-27

Cuatro commits de `main` llevaban la línea «Co-Authored-By: Claude…», y por eso GitHub enseñaba a
Claude como colaborador del repositorio: `3e7554e`, `0f2e9ee`, `8557b23` y `85c90f5` (hashes
viejos, del 21-09). Decisión del propietario, como excepción a lo que dice `CLAUDE.md` de no
reescribir `main`: se reescribió el historial desde el primero de ellos para quitar solo esas
cuatro líneas (y la línea en blanco que las separaba del cuerpo).

**Cómo:** `git filter-branch --msg-filter` sobre `main` y las etiquetas `v1.0.0`, `v1.0.1`,
`v1.0.2`, `v1.1.0` y `v1.1.1`, desde `3e7554e`. Subido con `--force-with-lease` (`main`) y las
cinco etiquetas. La firma de Claude en commits y PR queda desactivada en la configuración de Claude
Code del propietario, para que no vuelva a colarse.

**Comprobado antes de subir:**
- Ningún commit de `main` con ese coautor.
- 123 commits reescritos; `main` sigue teniendo 457, los mismos que antes.
- `git diff` entre la copia y `main`, vacío; autores, fechas y árboles de los 457, idénticos; los
  mensajes solo pierden la línea del coautor y su línea en blanco. Las cinco etiquetas, con el mismo
  árbol y el mismo mensaje.

**Copias locales** (solo en la máquina del propietario, unos días): la rama
`respaldo/antes-sin-coautor` y las etiquetas `respaldo-v1.0.0` … `respaldo-v1.1.1`.

**Hashes en la documentación:** los `.md` del repositorio (CHANGELOG, CLAUDE.md, README y
`documentacion/`) se pasaron al historial nuevo con este mapa, solo como palabra entera y
conservando la longitud citada. **Fuera del repositorio** (los mensajes de commit ya escritos, las
notas externas, `/actuator/info` hasta el próximo despliegue, que da `b71c46d`) siguen los hashes
viejos: se traducen con esta tabla.

## Mapa

Emparejado por árbol y fecha de autor de cada commit, no por el orden del log. Los commits
anteriores a `3e7554e` no cambian.

| Viejo | Nuevo | Fecha | Commit |
|---|---|---|---|
| `b71c46d` | `3465225` | 2026-09-27 | docs(documentacion): 1539375 ya está desplegado; lo que queda para el propietario |
| `f53a855` | `37be0d2` | 2026-09-27 | docs(changelog,documentacion): web de administración, fase 1.1, y su informe |
| `d294f83` | `bb1edfb` | 2026-09-27 | ci(admin,api): la web de administración en el CI y la API solo cuando cambia algo fuera de admin/ (GP-121) |
| `8db994e` | `238c32c` | 2026-09-27 | docs(decisiones): la web de administración, solo en tema oscuro (DEC-035) |
| `eae139a` | `2e191de` | 2026-09-27 | feat(admin): la web de administración se adapta a cualquier pantalla y aprovecha el espacio (GP-120) |
| `da8a595` | `c1a1fea` | 2026-09-27 | docs(admin): PRODUCT.md y DESIGN.md de la web de administración (GP-120) |
| `e7905c8` | `4c1e85d` | 2026-09-27 | fix(api): el equipamiento de los ejercicios importados (GP-122) |
| `1539375` | `e07adba` | 2026-09-27 | docs(changelog,documentacion): lote 1.1.1 y su informe |
| `76ddb16` | `3710cc4` | 2026-09-27 | build(android): versión 1.1.1 (10101) |
| `e16f8ae` | `4ab55ba` | 2026-09-27 | fix(admin): accesibilidad, errores y carga de la web tras la auditoría (GP-085) |
| `214801b` | `af8ac28` | 2026-09-27 | fix(api,android): el borrado de cuenta, al día con el cambio de contraseña (GP-119) |
| `98a799f` | `16a3ed3` | 2026-09-27 | fix(android): el engranaje de Progreso sin el anillo naranja (GP-118) |
| `12d489e` | `5773f66` | 2026-09-27 | fix(android): el «+» de la barra se ve entero (GP-118) |
| `76a63fb` | `2fd6f21` | 2026-09-27 | docs(changelog,decisiones,documentacion): web de administración, fase 1 (GP-085, DEC-035) |
| `1894997` | `921a05a` | 2026-09-27 | fix(admin): la cabecera deja de decir «API en marcha» en cuanto la API no contesta (GP-085) |
| `c9ae39f` | `15a2bcb` | 2026-09-27 | feat(admin): web de administración en admin.gymprofit.app, fase 1 (GP-085) |
| `557b5bb` | `78ad1ac` | 2026-09-27 | fix(api): alta y último acceso de /admin/cuentas salen con su zona (GP-085) |
| `83f70f6` | `80e8f61` | 2026-09-27 | fix(api): la opción de equipamiento lleva «valor», no «codigo» (GP-085) |
| `e34bae9` | `4f841ae` | 2026-09-27 | feat(api): GET /admin/resumen con la actividad de la semana en hora de Madrid (GP-085) |
| `aad81cb` | `f27eef3` | 2026-09-27 | feat(api): catálogo de alimentos en /admin/alimentos (GP-085) |
| `beeaa90` | `799031e` | 2026-09-27 | feat(api): ejercicios en /admin/ejercicios, con equipamiento y nombre revisado (GP-085) |
| `05db066` | `7d73cb0` | 2026-09-27 | feat(api): cuentas en /admin/cuentas, con último acceso y borrado a petición (GP-085) |
| `102462c` | `47ffcec` | 2026-09-27 | fix(api): CORS de producción solo para admin.gymprofit.app (GP-085) |
| `e6f0bb7` | `4bbffb4` | 2026-09-27 | fix(api): errores en español por defecto y en inglés con Accept-Language (GP-109) |
| `c264986` | `e8e6b7e` | 2026-09-27 | docs(documentacion): detalle de GP-106, ya desplegado |
| `4641c9b` | `0557583` | 2026-09-27 | docs(changelog,documentacion): la cuenta del bot en gymprofit.app |
| `8ea5d02` | `f8790e7` | 2026-09-27 | fix(api): la cuenta del bot también usa el dominio del producto (GP-106) |
| `3759b8a` | `d3d89a0` | 2026-09-27 | docs(documentacion): detalle de GP-115, ya desplegado |
| `7dabb6c` | `a588391` | 2026-09-27 | docs(changelog,documentacion): lote 1.1.0 y su informe |
| `94dcdce` | `44abc71` | 2026-09-27 | Merge gp-105-estructura: estructura nueva con la barra B (GP-105) |
| `d5260d1` | `f7dd373` | 2026-09-27 | fix(android): el aviso de descartar de los diálogos también en Android 13-15 (GP-108) |
| `eebe1a1` | `9840d7c` | 2026-09-27 | build(android): versión 1.1.0 |
| `fde28c4` | `6ec1944` | 2026-09-27 | fix(android): los botones de Hoy toca sin rutinas no se cortan (GP-105) |
| `3941df1` | `e056709` | 2026-09-27 | test(android): el medidor de accesibilidad vuelca nombre, estado, rol y selección |
| `52c4151` | `ec66b19` | 2026-09-27 | fix(android): el foco no entra en las pestañas que no se ven (GP-105) |
| `1e86639` | `907d0a9` | 2026-09-27 | feat(android): salir de más formularios con algo escrito pregunta antes (GP-108) |
| `4aba76a` | `1378c2f` | 2026-09-27 | fix(android): no se enseñan los logros que dependen de objetivos personales (GP-114) |
| `6b97b0b` | `1ca8de8` | 2026-09-27 | fix(android): el historial dice el nombre de la plantilla (GP-113) |
| `dc4a19e` | `90142da` | 2026-09-27 | feat(android,documentacion): estructura nueva con la barra B (GP-105) |
| `de43067` | `b31ceb9` | 2026-09-27 | docs(decisiones): DEC-034, la app recorta los espacios de los extremos de la contraseña |
| `0506042` | `1ad3ce6` | 2026-09-27 | docs(changelog): lote 1.1.0, partes A y B |
| `12c14c1` | `d000a01` | 2026-09-27 | fix(android): una sesión perdida abre Login una sola vez (GP-107) |
| `04fab69` | `8549146` | 2026-09-27 | feat(api): series por zona de la semana natural en /volumen-muscular |
| `3a2af47` | `5785be5` | 2026-09-27 | fix(api): cambiar la contraseña con la actual mal responde 403, no 401 |
| `93b7a6a` | `9918fbd` | 2026-09-27 | fix(api): logs fuera del repositorio y DTO sin secretos en su toString (GP-115) |
| `da08ac6` | `6e59d38` | 2026-09-27 | fix(api): las cuentas semilla usan el dominio del producto (GP-106) |
| `b4572ac` | `af8c163` | 2026-09-27 | docs(documentacion): Render ya despliega solo con el CI en verde (GP-094) |
| `44c1320` | `29fbe8b` | 2026-09-27 | docs(documentacion): Render no aplica el disparador de render.yaml (GP-094) |
| `5773c60` | `ab9dd3f` | 2026-09-27 | docs(changelog,documentacion): lote 1.0.2 y entrega |
| `b10fffe` | `5e68d97` | 2026-09-26 | build(android): versión 1.0.2 |
| `02828d7` | `9f2f65b` | 2026-09-26 | docs(decisiones): DEC-034, contraseñas según NIST con mínimo de 8 |
| `004294d` | `5754389` | 2026-09-26 | feat(android): la app pide la contraseña nueva como la API de GP-101 |
| `60c1454` | `0ff29e8` | 2026-09-26 | feat(api): contraseñas nuevas según NIST SP 800-63B-4, con mínimo de 8 (GP-101) |
| `936a6ca` | `1be26ef` | 2026-09-26 | fix(android): el diálogo de gramos no enseña texto de ejemplo a fuego (GP-026) |
| `198be5c` | `5c13d6d` | 2026-09-26 | fix(android): la notificación de logros cuenta con plurales (GP-025) |
| `b46e1fb` | `4cf03ce` | 2026-09-26 | fix(android): ningún fallo de red mudo (GP-017) |
| `7be6832` | `f5fe55d` | 2026-09-26 | fix(android): un 429 pide esperar, y dice cuánto (GP-096) |
| `d65e178` | `95fc71f` | 2026-09-26 | fix(android): la sesión sobrevive a que Android mate la app en segundo plano (GP-091) |
| `0d64129` | `828314c` | 2026-09-26 | feat(android): salir de un formulario con algo apuntado pregunta antes (GP-098) |
| `442b542` | `66851b0` | 2026-09-26 | fix(android): atrás vuelve a Inicio en vez de salir de la app (GP-097) |
| `db85482` | `6385c59` | 2026-09-26 | build(render): Render despliega solo con el CI en verde (GP-094) |
| `b9ed20c` | `bdd2b1a` | 2026-09-26 | fix(api): lo que no es un fallo del servidor deja de responder 500 (GP-075) |
| `e58580a` | `3f1406e` | 2026-09-26 | feat(api): /actuator/info dice qué commit corre, sin iniciar sesión (GP-100) |
| `ffad996` | `9bd6217` | 2026-09-25 | docs(changelog,documentacion): arreglo del registro (GP-095) y entrega 1.0.1 |
| `0feba89` | `6149a28` | 2026-09-25 | fix(android): recuperar contraseña usa la misma política que el registro |
| `4774f26` | `4a43ff7` | 2026-09-25 | build(android): versión 1.0.1 |
| `ed4d4e9` | `be824c1` | 2026-09-25 | fix(android): el registro valida como la API y dice qué falla |
| `80ce95d` | `6337b9b` | 2026-09-25 | feat(api): el registro dice si el usuario o el correo ya están en uso |
| `5afb69f` | `b62e8ec` | 2026-09-25 | docs(changelog): esquema de versión de la app |
| `6885367` | `e751f23` | 2026-09-25 | build(android): versión MAYOR.MENOR.PARCHE y versionCode derivado de ella |
| `e4b2822` | `f88f295` | 2026-09-25 | docs(changelog,documentacion): informe final del bloque de diseño 2c |
| `6921083` | `9cb0d94` | 2026-09-25 | fix(android): la letra baja a una escala más contenida y regular |
| `c21b9a7` | `cadf265` | 2026-09-25 | feat(api,android): los récords salen de las series de las sesiones (GP-088) |
| `61b1afb` | `2f6fd89` | 2026-09-25 | fix(android): los filtros de rol de Gestión de usuarios dicen el rol, no el código (GP-071) |
| `ed9237c` | `46c2036` | 2026-09-25 | fix(android): el campo de los buscadores de administración mide 48 dp (GP-062) |
| `996cfd0` | `5a7a4d9` | 2026-09-25 | fix(android): los pulsables sin medida escrita también llegan a 48 dp (GP-062) |
| `ce1d3a6` | `22444c8` | 2026-09-25 | fix(android): «Carbohidratos» ya no se parte a media palabra con la letra grande (GP-089) |
| `7b6f29b` | `0022ca7` | 2026-09-25 | fix(android): las etiquetas en mayúsculas también van en condensada (GP-089) |
| `11b1348` | `fa4c369` | 2026-09-25 | fix(android): ni códigos de la API ni unidades a fuego en pantalla (GP-071) |
| `1a48873` | `25941da` | 2026-09-25 | fix(api): borrar una sesión con ejercicios ya no da 500 |
| `eadb19f` | `914ee6b` | 2026-09-25 | feat(android): una sola cabecera y 48 dp en todo lo pulsable (GP-062) |
| `55db41a` | `4fa9501` | 2026-09-25 | docs(changelog,reglas): cada commit lleva solo los ficheros de su tarea |
| `ea84b4c` | `9ca4d28` | 2026-09-25 | fix(api): restaura los ficheros de progreso por ejercicio que borró 3de9436 |
| `603db08` | `b053338` | 2026-09-24 | docs(changelog,documentacion): estado del bloque de diseño 2c al cortar la sesión |
| `3de9436` | `6accede` | 2026-09-24 | feat(android,web): una familia tipográfica, dos anchos, y nada por debajo de 13 sp (GP-089) |
| `9ca07d3` | `a4d5955` | 2026-09-24 | docs(reglas): ningún fallo de seguridad descrito antes de estar desplegado |
| `716d79e` | `b36e059` | 2026-09-24 | docs(changelog,documentacion): informe del lote de seguridad e inventario de rutas con id |
| `018babb` | `7e93af5` | 2026-09-24 | fix(api): cobertura IDOR de todas las rutas con id y cinco fugas que había detrás (GP-048) |
| `cd1ff75` | `66d983e` | 2026-09-24 | fix(api): la carrera del guardado idempotente de la sesión ya no da 500 (GP-076) |
| `8833279` | `9dc7f1b` | 2026-09-24 | fix(api,android): una cuenta desactivada deja de entrar y el correo se cambia con contraseña (GP-083) |
| `f2f9cd5` | `0dc1f46` | 2026-09-24 | docs(changelog,documentacion): hash de GP-081 y cierre con la versión A del icono |
| `7ac8e8a` | `6f88abc` | 2026-09-24 | feat(android): icono adaptativo con el logo real y logo en las notificaciones |
| `f73535b` | `a13a25b` | 2026-09-24 | docs(android): el README de la app dice cómo son hoy el tema, los iconos y las notificaciones |
| `99a1fa5` | `e0032d4` | 2026-09-24 | docs(changelog,documentacion): informe y hashes del bloque de diseño 2b |
| `d936bf1` | `c52778b` | 2026-09-24 | feat(android): pantalla de licencias de terceros |
| `3ef0e23` | `14dd544` | 2026-09-24 | feat(android): un solo sistema de iconos en toda la app |
| `3c528ea` | `fc761d5` | 2026-09-24 | fix(android): el logro conseguido va en dorado, no en naranja |
| `170c7f2` | `834a4ac` | 2026-09-24 | fix(android): el tema oscuro deja de tener otra tipografía |
| `ea7773c` | `b470aaa` | 2026-09-24 | docs(changelog,documentacion): informe y hashes del bloque de diseño 2a |
| `6ea3b3f` | `e27bc5b` | 2026-09-24 | fix(android): la paleta pasa AA en los dos temas y el texto deja de llevar alpha |
| `ee4f0ff` | `139f547` | 2026-09-24 | docs(api): el ejemplo de /sesiones/completa usa el campo que la API pide |
| `cba7663` | `36cd260` | 2026-09-24 | feat(api,android): un logro bloqueado parece bloqueado y dice cuánto falta |
| `343e1b0` | `3f6e4ed` | 2026-09-24 | fix(android): el medidor de nivel deja de parecer el icono de cobertura |
| `4616b2c` | `5565f6e` | 2026-09-24 | fix(android): la valoración de la sesión arranca sin valorar |
| `61bac5c` | `8780d9b` | 2026-09-23 | docs(api,documentacion): el endpoint nuevo en el README y el despliegue sin confirmar |
| `7da8aed` | `9c2a8d9` | 2026-09-23 | docs(changelog): hashes del bloque de guardado de sesión |
| `721d256` | `4fe0ce5` | 2026-09-23 | docs(decisiones): DEC-033 dice lo que el código hace con 403 y 404 |
| `f38800e` | `78fdd8c` | 2026-09-23 | fix(android): el guardado de la sesión deja de mentir |
| `7442a53` | `ecc8955` | 2026-09-23 | feat(api,db): guardar la sesión entera o nada, y sin duplicar al reintentar |
| `c0c0238` | `d521865` | 2026-09-23 | feat(api,db): la valoración de la sesión pasa a ser un campo |
| `bf59504` | `95d6430` | 2026-09-23 | docs(changelog): hash del rótulo de sugerencias vacío |
| `1614baa` | `d661a27` | 2026-09-23 | fix(android): el rótulo de sugerencias no aparece sin sugerencias debajo |
| `c44ce17` | `99bd99d` | 2026-09-23 | docs(changelog): hashes de los dos cambios de contrato |
| `24e4437` | `bf0c55b` | 2026-09-23 | feat(api,android): fuera las calorías estimadas de entrenamiento |
| `6666120` | `3bc0131` | 2026-09-23 | fix(api,android): una colección vacía es 200 con [], no 404 |
| `31af654` | `90acb65` | 2026-09-23 | docs(changelog): hashes del primer bloque de diseño |
| `40c686b` | `78f9a8e` | 2026-09-23 | feat(android): un icono vectorial por sitio donde había un emoji |
| `5e0dee3` | `6cae9f1` | 2026-09-23 | fix(android): las rutinas predefinidas se ofrecen en el estado vacío, no como propias |
| `e1560cd` | `41a1345` | 2026-09-23 | refactor(android): el hueco bajo la barra flotante sale de las medidas de la barra |
| `85c90f5` | `2e6c89c` | 2026-09-21 | docs(changelog): hash de la puesta al día de decisiones y entorno de pruebas |
| `8557b23` | `553e4ff` | 2026-09-21 | docs(decisiones,documentacion): poner al día lo que la auditoría de diseño dejó desfasado |
| `0f2e9ee` | `fe7b4fb` | 2026-09-21 | docs(changelog): hash de la auditoría de diseño |
| `3e7554e` | `cb43073` | 2026-09-21 | docs(documentacion): auditoría de diseño medible de la app, pantalla por pantalla |
