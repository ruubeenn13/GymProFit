// ============================================================
// Astro para gymprofit.app (DEC-069, propuesta; web/PLAN-UN-DIA.md §3).
//
// · Salida estática, una página por fichero: src/pages/privacidad.astro sale como
//   privacidad.html, que es lo que Pages sirve en /privacidad. Con el formato por
//   defecto saldría privacidad/index.html y Pages redirigiría a /privacidad/, una URL
//   que Google Play tiene declarada. RutasWebTest lo fija sobre dist.
// · Sin barra final en ninguna ruta.
// · Sin prefetch, sin ClientRouter, sin la barra de herramientas de desarrollo.
// · Las páginas de antes viven tal cual en public/ hasta que pasen a .astro.
// ============================================================
import { defineConfig } from 'astro/config';

export default defineConfig({
  site: 'https://gymprofit.app',
  output: 'static',
  trailingSlash: 'never',
  build: {
    format: 'file',
    // Las hojas pequeñas van dentro del HTML; las grandes, en /_astro/ con su hash.
    inlineStylesheets: 'auto',
  },
  prefetch: false,
  devToolbar: { enabled: false },
  compressHTML: true,
});
