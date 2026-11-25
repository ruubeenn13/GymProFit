// ============================================================
// astro.mjs — lanza la orden de Astro con la telemetría apagada (DEC-069).
//
// Astro trae la telemetría encendida y el repositorio no manda datos a nadie sin
// pedirlo. ASTRO_TELEMETRY_DISABLED=1 delante de la orden en package.json no vale en
// Windows (cmd no entiende esa forma), y meter cross-env sería una dependencia más
// solo para eso: este lanzador pone la variable y pasa la orden tal cual.
// ============================================================
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// El binario que declara el paquete (su "bin"); no está en sus "exports".
const astro = fileURLToPath(new URL('../node_modules/astro/bin/astro.mjs', import.meta.url));
const hecho = spawnSync(process.execPath, [astro, ...process.argv.slice(2)], {
  stdio: 'inherit',
  env: { ...process.env, ASTRO_TELEMETRY_DISABLED: '1' },
});
process.exit(hecho.status ?? 1);
