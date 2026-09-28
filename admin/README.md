# Web de administración — admin.gymprofit.app

Fase 1 de GP-085: Entrada, Resumen, Usuarios, Ejercicios y Alimentos. Plantillas llega en la fase 2.
Decisión de arquitectura: **DEC-035** en `documentacion/PRODUCT-DECISIONS.md`.

- **React + Vite + TypeScript**, React Router. Tests con Vitest y Testing Library. Nada más: ni librerías de componentes ni de gráficas (las barras del resumen son CSS).
- **Solo en español y solo en tema oscuro** (DEC-035). Se adapta a cualquier ancho (GP-120): barra lateral desde 1280 px, barra de iconos de 768 a 1279, y por debajo el título arriba, cuatro pestañas abajo y tarjetas en vez de tablas. Ejercicios y Alimentos enseñan lista y editor juntos desde 1440 px.
- **Producto y sistema visual** en `PRODUCT.md` y `DESIGN.md`, para Impeccable. `DESIGN.md` sale de `src/estilos/tokens.css`: si cambia un token, cambian los dos.
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
npx -y impeccable@4.0.0 detect src   # detector de Impeccable; el CI falla con cualquier hallazgo
```

La API local ya admite el origen `http://localhost:5173` (valor por defecto de `app.cors.allowed-origins`). Para entrar, la cuenta `admin` de la base local; **nunca la de producción** para probar.

Para añadir un icono: descargar el SVG de Material Symbols Rounded (`https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/<nombre>/materialsymbolsrounded/<nombre>_24px.svg`) a `src/iconos` y darlo de alta en `src/componentes/Icono.tsx`.

## Despliegue: Cloudflare Workers con archivos estáticos

`wrangler.jsonc` publica `dist/` como Worker **gymprofit-admin**, sin código propio: las rutas que no son un archivo devuelven `index.html` (modo SPA). Sin `workers.dev` ni URLs de previsualización; el dominio es `admin.gymprofit.app`. Las cabeceras de seguridad están en `public/_headers`: CSP que solo deja conectar con `https://api.gymprofit.app`, sin marcos (`frame-ancestors 'none'`), `nosniff` y `Referrer-Policy: no-referrer`.

Publicada el 2026-09-28. Desde entonces **cada push a `main` despliega** por Workers Builds. Lo que sigue es cómo se montó, en este orden, con lo que salió mal la primera vez.

### 1 · Primero, Cloudflare Access

Antes de que exista el Worker, para que `admin.gymprofit.app` no esté abierto ni un minuto.

1. Zero Trust → **Access controls → Applications → Create new application → Self-hosted and private**.
2. Dominio: `admin.gymprofit.app` (toda la ruta).
3. Política **«Administradores»**, acción *Allow*, con **Include → Emails**: los correos de los administradores, y nada más.
4. Duración de la sesión: **24 h**.
5. **El código por correo (One-time PIN) no viene activado en las cuentas nuevas**: se añade en Zero Trust → **Integraciones → Proveedores de identidad**. Sin él, Access no tiene con qué pedir la identidad.
6. Comprobar en una ventana privada que la página pide Access antes de enseñar Entrada.

### 2 · Después, el Worker

1. **Workers & Pages → Create application → Import a repository.** No el enlace de Pages: eso crea un proyecto de Pages, no un Worker.
   - Repositorio `ruubeenn13/GymProFit`, nombre **`gymprofit-admin`** (tiene que coincidir con `name` de `wrangler.jsonc`), rama de producción `main`.
   - **Si falla la conexión con GitHub**: desinstalar la app «Cloudflare Workers and Pages» en GitHub (Settings → Applications, en *Installed GitHub Apps* y en *Authorized GitHub Apps*) y volver a conectar, dándole acceso **solo a este repositorio**.
2. **El formulario de creación no tiene directorio raíz.** Se crea como salga y se corrige justo después, en el Worker → **Settings → Build**:
   - **Producción**: directorio raíz **`admin`**; compilación **`npm ci && npm run build`**; despliegue **`npx wrangler deploy`**.
   - **Previews Base**: vistas previas **apagadas**.
3. En el Worker → **Settings → Dominios**: `workers.dev` y *Preview URLs* **apagados**. `admin.gymprofit.app` lo da de alta el propio `wrangler deploy` por `routes` (`custom_domain: true`), porque la zona `gymprofit.app` está en la misma cuenta.

**Por qué ese orden importa.** El primer despliegue corrió en la raíz del repositorio: `wrangler` no encontró `wrangler.jsonc`, se inventó una configuración, **subió `admin/` entera con `node_modules`** y dejó **`workers.dev` público**, sin Access delante. Tras corregir el directorio raíz y apagar `workers.dev` a mano, el siguiente despliegue salió bien.

### Cómo se ve un despliegue bueno

En el registro de la compilación:

- `Read 21 files from … admin/dist` (21 el 28-09; si la web cambia, cambia el número. Lo que importa es que lea de `admin/dist`, no de la raíz).
- **Ningún aviso** sobre `workers_dev`.
- Entre los *triggers*, **`admin.gymprofit.app (custom domain)`**.

Si falta cualquiera de las tres, el directorio raíz o `wrangler.jsonc` no se están usando: parar y revisar el paso 2.

### Dar de alta a otro administrador

Hacen falta **los dos sitios**:

1. Su correo en la política «Administradores» de Access.
2. El rol **ADMIN** en su cuenta de GymProFit (en esta web, Usuarios → su ficha → cambiar rol).

Para quitarlo, en los dos sitios también: si solo se quita uno, el otro sigue concedido y basta con volver a darle el primero para que entre.

Access es la primera cerradura; la segunda es la cuenta ADMIN de GymProFit en Entrada, y la tercera, la que manda, la API, que exige ADMIN en cada ruta `/admin`.
