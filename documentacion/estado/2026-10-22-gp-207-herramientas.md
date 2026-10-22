# GP-207 · Herramientas de Claude Code para la app — 2026-10-22

Sin código de producto, sin dependencias, sin cambios en Render. El propietario autorizó tocar la configuración de la máquina solo para esta lista (excepción a «Nada fuera de este repositorio»). Nada está commiteado todavía: ver *Qué se versiona*.

## Inventario de partida

- Plugins de usuario: caveman, code-review, commit-commands, context7, frontend-design, superpowers. Proyecto (apagados): document-skills, example-skills, frontend-slides. skill-creator apagado.
- Sincronizados de claude.ai: canva, website-design, audit-suite (plugins) y las `anthropic-skills:*` (academic-paper-drafter, built-in-browser, chrome-browser, code-documenter, code-review-skill, computer-use, cv-linkedin-optimizer, debug-assistant, deep-research, docs, docx, google-workspace, import-memory, learning-roadmap-generator, morning, pdf, pptx, presentaciones, professional-email-drafter, unit-test-generator, xlsx).
- MCP: playwright, github, context7 (suelto y de plugin), canva, uxkin y los conectores de claude.ai (CoinDesk pide autenticación).
- Skills de proyecto: design-taste-frontend, emil-design-eng, impeccable. Agentes de usuario: client-content, design-director, motion-polish, visual-qa. Hooks: ninguno.
- Skills globales de `npx skills`: 12 de emilkowalski/skills, impeccable y 13 de Leonxlnx/taste-skill.

## Quitado y apagado

- **Quitadas (previa confirmación con la lista exacta)**, 14 globales de `~/.agents` y `~/.claude/skills`: brandkit, industrial-brutalist-ui, gpt-taste, image-to-code, imagegen-frontend-mobile, imagegen-frontend-web, minimalist-ui, full-output-enforcement, redesign-existing-projects, high-end-visual-design, stitch-design-taste, design-taste-frontend-v1 (el repo Leonxlnx/taste-skill solo conserva ya `taste-skill` = design-taste-frontend), más write-swift y animate-expo. Quedan 12 en el lock global.
- **Apagado solo en este repo**, `.claude/settings.local.json`: `syncClaudeAiPlugins: false` (canva, website-design, audit-suite) y `syncClaudeAiSkills: false`. El segundo apaga **todas** las `anthropic-skills:*`, también las de código (code-review-skill, debug-assistant, unit-test-generator): la opción no distingue. Surte efecto al reiniciar.

## Instalado

| Qué | Dónde | Nota |
|---|---|---|
| Android CLI 1.0.16625370 | `%USERPROFILE%\AppData\AndroidCLI\android.exe`, en el PATH de usuario | Instalada desde cmd.exe con el instalador local de Google (leído antes de ejecutarlo). SDK y `--no-metrics` en `%USERPROFILE%\.androidrc` |
| allexp1/android-design | `.claude/skills/android-design` | `npx skills add`, entra en `skills-lock.json` |
| De android/skills: android-cli, play-policy-insights, r8-analyzer, anr-audit-and-fix, android-intent-security, android-permissions-security | `.claude/skills/` | Solo esas seis; ni el plugin ni testing-setup |
| pr-review-toolkit, claude-code-setup | usuario | Plugins del marketplace oficial |
| jdtls-lsp, typescript-lsp, render | proyecto (`.claude/settings.json`) | |
| typescript-language-server 6.0.2 + typescript | npm global | |
| JDT.LS 1.56.0 (redhat.java de Open VSX) | `~/.local/share/jdtls` | Más `lombok.jar` 1.18.48 copiado del `.m2` |
| `jdtls.cmd` | `~/.local/bin` (en el PATH de usuario) | Lanza JDT.LS con el JDK 21 (`~/.jdks/ms-21.0.10`), agente Lombok y datos en `~/.cache/jdtls-data/<carpeta>` |
| Render CLI 2.31.0 | `~/.local/bin/render.exe` | SHA-256 coincide con el digest de la release |
| `.claude/hooks/bloquear-git-add-masivo.js` | proyecto | Ver abajo |
| `.claude/agents/revisor-ui-android.md` | proyecto | Ver abajo |

## `.claude/settings.json`

- `attribution: {commit: "", pr: ""}`.
- Hook PreToolUse en Bash que bloquea `git add -A|.|--all` y `git commit -a|-am|--all`, con el motivo de CLAUDE.md. Quita lo entrecomillado antes de mirar los flags, así un mensaje con «-a» no lo dispara.
- `permissions.ask` para las 10 herramientas del MCP de Render que escriben (`trigger_deploy`, `create_web_service`, `create_static_site`, `create_cron_job`, `create_postgres`, `create_key_value`, `update_web_service`, `update_static_site`, `update_cron_job`, `update_environment_variables`). Los nombres salen del código del MCP (render-oss/render-mcp-server), no de una lista en ejecución: **falta contrastarlos con la lista real tras el reinicio** (`/mcp`).

## Pruebas y resultado

| Prueba | Resultado |
|---|---|
| Hook, 11 órdenes por script | `git add -A`, `.`, `--all`, `commit -a`, `-am` y `add a && add .` salen con 2 (bloqueo); `add a b`, `commit -m 'fix -a y'`, `add ./foo`, `status`, `commit --amend` pasan |
| Hook en vivo | `git add -n .` bloqueado con el motivo (se probó con `-n` por si el hook no estaba cargado) |
| `android layout --pretty` | OK en el emulador (instala un servidor de instrumentación en el dispositivo la primera vez) |
| `android screen capture --annotate -o` | OK, PNG de 186 KB |
| `android docs search` | OK tras bajar la base de conocimiento (19,6 MB, 5.319 entradas) |
| JDT.LS en `api/` | Abre `AuthController.java` con una línea rota añadida en memoria: devuelve **solo** ese error (`variableInexistente cannot be resolved`). Sin el agente Lombok daba 3 falsos positivos de getters; con él, ninguno |
| JDT.LS en `app/` | `MainActivity.java`: **ningún error falso de clases de Android**. Solo un aviso «not on the classpath of project app, only syntax errors are reported». Se queda, pero en `app/` solo vale para errores de sintaxis |
| Hook de render.yaml | Se ejecuta; la CLI pide espacio de trabajo y sesión (`render login` lo hace el propietario) |
| revisor-ui-android con Inicio | Ver abajo |

## Agente `revisor-ui-android`

Versionado con `!.claude/agents/` en `.gitignore`. Herramientas: Read, Glob, Grep, Bash. Procedimiento: abre la pantalla con `TrampolinActivity`, captura en las cuatro combinaciones (ES/claro/1,0 · EN/oscuro/1,0 · ES/oscuro/1,3 · EN/claro/1,3) con la Android CLI, mide con `MedidorAccesibilidad`, compara con el PNG, `DESIGN.md` y DEC-018/019/039, y devuelve fallos por gravedad con ruta de captura, sin código.

**No se pudo invocar por su nombre**: los agentes se cargan al arrancar Claude Code, así que `revisor-ui-android` no existía en esta sesión. Se probó el mismo fichero con un agente genérico que lo seguía al pie de la letra y sin Edit ni Write. Dos pasadas: la primera encontró cuatro fallos del procedimiento (sin sesión no se puede revisar; `android` no está en el PATH de Git Bash; `MainActivity` solo entra por el trampolín; `GymProFitPrefs.xml` no existe sin sesión); tras corregir el fichero, la segunda llegó al final. Falta que el propietario lo invoque tras reiniciar (el tope «no puede tocar ficheros» solo se verifica así).

Quedó anotado en el fichero lo que aprendió la segunda pasada: `theme_mode` solo existe en las prefs tras un cambio de tema (hay que insertarla); `force-stop` vacía el servicio de accesibilidad (reactivarlo tras cada reinicio); `logcat -c` antes de medir; el medidor no ve lo que queda bajo el pliegue (usar `android layout --pretty`, salida en cp1252).

Resultado de la revisión de Inicio (cuenta local vacía, así que el estado con datos **no se pudo comparar**):

- Alto: con letra 1,3, `tvHola` (30,1 → 23,2 sp equivalentes, no crece), `tvHoyTitulo`, `tvSemanaZonas` y `tvInicial` crecen entre 1,00× y 1,18×; el resto, 1,26–1,33×. Puede ser un tope deliberado de los títulos grandes; no confirmado. DEC-019.
- Medio: cadena EN rota, «The number is sets this week.» (ES: «El número es de series esta semana.»). DEC-020.
- Medio: «Cuerpo completo A» (contenido de BD) sin traducir en la pantalla principal en EN; DEC-020 ya lo reconoce como salvedad.
- Medio: la cuenta vacía enseña la tarjeta «Primeros pasos» y una tarjeta de saludo que el diseño no tiene; no es comparable sin datos.
- Bajo: el rótulo «PRUEBAS · LOCAL» (9 sp) tapa contenido al desplazar (solo debug). Las etiquetas «Nutrition» y «Progress» de la barra quedan a ~30 px con 1,3 y EN.
- Sin fallo: 12 zonas pulsables por combinación, todas ≥ 48 dp y con nombre accesible. Movimiento (DEC-039) no evaluado.
- Capturas y logs: `C:\Users\ruben\AppData\Local\Temp\revisor-ui\inicio\`.

## Propuestas de claude-code-setup (sin aplicar)

1. **Hook PreToolUse: no editar migraciones Flyway ya publicadas.** Bloquear Edit/Write sobre `db/migration/V*.sql` que ya estén en `git` (CLAUDE.md: «una migración publicada no se edita, se añade otra»). Mismo patrón para `src/generated/jooq`.
2. **Hook PreToolUse: no leer ni escribir `*firebase-adminsdk*.json`** de la raíz (está ignorado, pero un `Read` lo metería en el contexto).
3. **Subagente `revisor-ownership`**: dado un diff de `api/`, comprueba que toda ruta con id o id en el cuerpo tiene su test que extiende `AbstractOwnershipTest` y el criterio 403/aislamiento de DEC-014/DEC-027.
4. **Skill de usuario `/nueva-migracion <descripción>`**: crea `V<AAAAMMDDHHmm>__Descripcion.sql` con el sello de hora correcto, para no equivocarse en el nombre.
5. MCP y plugins: nada nuevo. context7, playwright, github y los plugins de revisión ya cubren lo que el repo usa.

## Lo que no funciona en Windows o queda pendiente

- **Android CLI**: no se instala desde PowerShell (hay que usar cmd.exe). `android emulator` está desactivado en Windows según su documentación; se usa el emulador del SDK. La telemetría solo se desactiva con `--no-metrics`; la dejé en `.androidrc`, pero **no he podido comprobar que ese fichero acepte la bandera** (solo documentan `--sdk`). Si no la acepta, hay que ponerla en cada orden o en un alias.
- **JDT.LS**: `download.eclipse.org/jdtls` está en «Temporary shutdown»; se sacó de la extensión de Open VSX. Las versiones 1.58 y 1.59 traen paquetes que exigen Java 25 y no arrancan en 21; la 1.56.0 sí (se descartó la nightly). Subir de versión exigirá JDK 25. Falta comprobar que Claude Code resuelve `jdtls` a `jdtls.cmd` sin shell (Node no suele hacerlo en Windows): si tras el reinicio no hay diagnósticos, hay que crear un `jdtls.exe` o ponerle ruta absoluta.
- **typescript-lsp**: igual con `typescript-language-server.cmd`. No lo probé sobre `admin/`.
- **Render MCP**: el OAuth lo autoriza el propietario y sus herramientas no aparecen hasta reiniciar. No se pudo listar ni leer logs del último despliegue. La CLI tampoco tiene sesión.
- **Nada se ve hasta reiniciar**: plugins nuevos, agente, skills de proyecto nuevas y `syncClaudeAi*`.
- El `.claude/settings.json` y `.claude/hooks/` quedan fuera de git (`.claude/*` ignorado); solo `skills/` y `agents/` se versionan.

## Qué se versiona

Para commitear: `.gitignore`, `skills-lock.json`, `.claude/skills/{android-cli,android-design,android-intent-security,android-permissions-security,anr-audit-and-fix,play-policy-insights,r8-analyzer}`, `.claude/agents/revisor-ui-android.md` y este informe. Tipos sugeridos: `build(android)` para skills, agente y lock; `docs(documentacion)` para el informe. Un push a `main` no despliega nada de esto.

## Dudas

1. ¿Subo la rama y la fusiono como el resto de lotes, o prefieres revisar las siete skills antes? Traen instrucciones con permisos completos de agente («Review skills before use»).
2. `syncClaudeAiSkills: false` apaga también las skills de código de claude.ai en este repo. ¿Aceptable?
