# Web de administración — admin.gymprofit.app

Fase 1 de GP-085: Entrada, Resumen, Usuarios, Ejercicios y Alimentos. Plantillas llega en la fase 2.
Decisión de arquitectura: **DEC-035** en `documentacion/PRODUCT-DECISIONS.md`.

- **React + Vite + TypeScript**, React Router. Tests con Vitest y Testing Library. Nada más: ni librerías de componentes ni de gráficas (las barras del resumen son CSS).
- **Solo en español** y pensada para escritorio (1280 px o más); por debajo se apila.
- **Toda escritura pasa por la API con rol ADMIN.** La web nunca toca la base de datos y no lleva ningún secreto: solo la URL de la API, en `.env.development` y `.env.production`. El repositorio es público.
- **Tokens solo en memoria.** Ni `localStorage` ni `sessionStorage`: al recargar se vuelve a entrar. Ante un 401, una renovación con `/auth/refresh` y un solo reintento; si falla, a Entrada.
- **Fuentes e iconos servidos desde la propia web.** Barlow y Barlow Condensed en `public/fuentes` (las mismas que `web/`, más Barlow 700, OFL). Los iconos son Material Symbols Rounded en SVG, en `src/iconos`, solo los que se usan. Nada de Google Fonts ni de CDN.

## Desarrollo

```bash
cd admin
npm ci
npm run dev      # http://localhost:5173, contra la API local (http://localhost:8080/api)
npm test         # Vitest
npm run build    # dist/
```

La API local ya admite el origen `http://localhost:5173` (valor por defecto de `app.cors.allowed-origins`). Para entrar, la cuenta `admin` de la base local; **nunca la de producción** para probar.

Para añadir un icono: descargar el SVG de Material Symbols Rounded (`https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/<nombre>/materialsymbolsrounded/<nombre>_24px.svg`) a `src/iconos` y darlo de alta en `src/componentes/Icono.tsx`.

## Despliegue: Cloudflare Workers con archivos estáticos

`wrangler.jsonc` publica `dist/` como Worker **gymprofit-admin**, sin código propio: las rutas que no son un archivo devuelven `index.html` (modo SPA). Sin `workers.dev` ni URLs de previsualización; el dominio es `admin.gymprofit.app`. Las cabeceras de seguridad están en `public/_headers`: CSP que solo deja conectar con `https://api.gymprofit.app`, sin marcos (`frame-ancestors 'none'`), `nosniff` y `Referrer-Policy: no-referrer`.

Un push **no** despliega nada hasta que el repositorio esté conectado (paso 1). Antes de conectarlo, la API tiene que estar desplegada con la fase 1 (CORS para `https://admin.gymprofit.app` y las rutas `/admin/...`).

### Lo que tiene que hacer el propietario en Cloudflare

1. **Conectar el repositorio en Workers Builds.** Workers y Pages → Crear → *Import a repository* → `ruubeenn13/GymProFit`.
   - Nombre del proyecto: `gymprofit-admin` (tiene que coincidir con `name` de `wrangler.jsonc`).
   - Rama de producción: `main`.
   - **Directorio raíz: `admin`**.
   - Comando de compilación: `npm ci && npm run build`.
   - Comando de despliegue: `npx wrangler deploy`.
   - Desactivar las compilaciones de ramas que no son `main` (*non-production branch builds*): no se quieren versiones de previsualización.
2. **El dominio.** Lo da de alta el propio `wrangler deploy` por `routes` (`custom_domain: true`), porque la zona `gymprofit.app` está en la misma cuenta. Comprobar en el Worker → *Settings* → *Domains & Routes* que aparece `admin.gymprofit.app` y que `workers.dev` y *Preview URLs* están desactivados.
3. **Cloudflare Access delante de `admin.gymprofit.app`, solo para tu correo.** Zero Trust → Access → Applications → *Add an application* → *Self-hosted*.
   - Dominio: `admin.gymprofit.app` (toda la ruta).
   - Proveedor de identidad: Google (o el código de un solo uso por correo).
   - Política *Allow* con la regla *Emails* igual a tu correo, y nada más.
   - Duración de la sesión: la que prefieras (24 h es razonable).
   - Comprobar en una ventana privada que la página pide Access antes de enseñar Entrada.

Access es la primera cerradura; la segunda es la cuenta ADMIN de GymProFit en Entrada, y la tercera, la que manda, la API, que exige ADMIN en cada ruta `/admin`.
