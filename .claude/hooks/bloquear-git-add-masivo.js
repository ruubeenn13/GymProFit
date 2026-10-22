#!/usr/bin/env node
// PreToolUse (Bash): bloquea `git add -A|.|--all` y `git commit -a`.
// Regla de CLAUDE.md: cada commit lleva solo los ficheros de su tarea, añadidos uno a uno.
let entrada = '';
process.stdin.on('data', (d) => (entrada += d));
process.stdin.on('end', () => {
  let cmd = '';
  try { cmd = JSON.parse(entrada).tool_input?.command ?? ''; } catch { process.exit(0); }
  // Se quitan las cadenas entrecomilladas para no confundir un mensaje de commit con flags.
  const limpio = cmd.replace(/"(?:[^"\\]|\\.)*"|'[^']*'/g, '""');
  const segmentos = limpio.split(/&&|\|\||;|\||\n/);
  for (const seg of segmentos) {
    const t = seg.trim().split(/\s+/);
    const i = t.indexOf('git');
    if (i < 0) continue;
    const sub = t.slice(i + 1).filter((x) => x !== '--no-pager');
    const args = sub.slice(1);
    const masivoAdd = sub[0] === 'add' && args.some((a) => a === '-A' || a === '--all' || a === '.');
    const masivoCommit = sub[0] === 'commit' && args.some((a) => a === '--all' || /^-[a-zA-Z]*a[a-zA-Z]*$/.test(a));
    if (masivoAdd || masivoCommit) {
      console.error('Bloqueado: nunca `git add -A`, `git add .`, `git add --all` ni `git commit -a` (CLAUDE.md, «Commits»). ' +
        'Cada commit lleva solo los ficheros de su tarea: añade las rutas una a una y revisa `git diff --cached --stat` antes de commitear.');
      process.exit(2);
    }
  }
  process.exit(0);
});
