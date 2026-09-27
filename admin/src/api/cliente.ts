// ============================================================
// Cliente de la API para la web de administración (GP-085)
//
// Los tokens viven SOLO en memoria: ni localStorage ni sessionStorage. Al recargar
// la página se pierden y se vuelve a Entrada; es el precio de que un script ajeno
// no tenga dónde leerlos.
//
// Ante un 401: una renovación con /auth/refresh y un solo reintento. Si la
// renovación falla, o el reintento vuelve a dar 401, la sesión se da por perdida:
// se borran los tokens y se avisa (la app vuelve a Entrada). Varias peticiones
// que caen a la vez comparten la misma renovación.
//
// La web no decide quién es administrador: lo decide la API en cada ruta /admin.
// Lo que hace aquí es no dejar pasar de Entrada a una cuenta que no lo es, con el
// mismo aviso que una contraseña equivocada.
// ============================================================

/** Error de la API, con el estado HTTP y el mensaje que devuelve. */
export class ApiError extends Error {
  readonly estado: number;
  readonly causa?: string;

  constructor(estado: number, mensaje: string, causa?: string) {
    super(mensaje);
    this.name = 'ApiError';
    this.estado = estado;
    this.causa = causa;
  }
}

/** Mismo texto para contraseña equivocada, cuenta que no es ADMIN o desactivada. */
export const AVISO_ENTRADA = 'Usuario o contraseña incorrectos.';

interface Tokens {
  acceso: string;
  renovacion: string;
  usuario: string;
}

interface RespuestaToken {
  token: string;
  refreshToken: string;
  username: string;
  roles: string[];
}

/** Opciones del cliente. fetch se inyecta en los tests. */
export interface OpcionesCliente {
  base: string;
  fetch?: typeof fetch;
  /** Se llama cuando la sesión se pierde y no se puede renovar. */
  alPerderSesion?: () => void;
}

/** Opciones de una petición. cuerpo se envía como JSON. */
export interface OpcionesPeticion {
  metodo?: string;
  cuerpo?: unknown;
  senal?: AbortSignal;
}

export type Cliente = ReturnType<typeof crearCliente>;

/**
 * Crea un cliente con su propio almacén de tokens en memoria.
 *
 * @param opciones URL base de la API (con /api), fetch y aviso de sesión perdida
 */
export function crearCliente(opciones: OpcionesCliente) {
  const base = opciones.base.replace(/\/$/, '');
  const hacerFetch: typeof fetch = opciones.fetch ?? ((...args) => fetch(...args));
  let tokens: Tokens | null = null;
  let renovando: Promise<boolean> | null = null;
  let alPerderSesion = opciones.alPerderSesion;

  function enviar(ruta: string, op: OpcionesPeticion, conToken: boolean): Promise<Response> {
    const cabeceras: Record<string, string> = { Accept: 'application/json' };
    if (op.cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
    if (conToken && tokens) cabeceras.Authorization = `Bearer ${tokens.acceso}`;
    return hacerFetch(base + ruta, {
      method: op.metodo ?? 'GET',
      headers: cabeceras,
      body: op.cuerpo === undefined ? undefined : JSON.stringify(op.cuerpo),
      signal: op.senal,
    });
  }

  async function errorDe(r: Response): Promise<ApiError> {
    let mensaje = `Error ${r.status}`;
    let causa: string | undefined;
    try {
      const cuerpo = await r.json();
      if (cuerpo && typeof cuerpo.message === 'string') mensaje = cuerpo.message;
      if (cuerpo && typeof cuerpo.cause === 'string') causa = cuerpo.cause;
    } catch {
      // Sin cuerpo JSON (un 502 del proveedor, por ejemplo): queda el estado.
    }
    return new ApiError(r.status, mensaje, causa);
  }

  function renovar(): Promise<boolean> {
    if (!renovando) {
      renovando = (async () => {
        const actual = tokens;
        if (!actual) return false;
        try {
          const r = await enviar('/auth/refresh', { metodo: 'POST', cuerpo: { refreshToken: actual.renovacion } }, false);
          if (!r.ok) return false;
          const datos = (await r.json()) as RespuestaToken;
          tokens = { acceso: datos.token, renovacion: datos.refreshToken, usuario: datos.username };
          return true;
        } catch {
          // Sin red en la renovación: la sesión no se puede recuperar desde aquí.
          return false;
        }
      })().finally(() => {
        renovando = null;
      });
    }
    return renovando;
  }

  function perderSesion(): never {
    tokens = null;
    alPerderSesion?.();
    throw new ApiError(401, 'La sesión ha caducado. Vuelve a entrar.');
  }

  /**
   * Petición autenticada. Devuelve el JSON, o undefined en un 204.
   *
   * @param ruta ruta bajo la base, con la barra inicial
   */
  async function pedir<T>(ruta: string, op: OpcionesPeticion = {}): Promise<T> {
    if (!tokens) perderSesion();
    let r = await enviar(ruta, op, true);
    if (r.status === 401) {
      if (!(await renovar())) perderSesion();
      r = await enviar(ruta, op, true);
      if (r.status === 401) perderSesion();
    }
    if (!r.ok) throw await errorDe(r);
    if (r.status === 204) return undefined as T;
    return (await r.json()) as T;
  }

  /**
   * Entra con usuario y contraseña. Solo deja entrar a una cuenta ADMIN; cualquier
   * otra recibe el mismo aviso que una contraseña equivocada, y su refresh token se
   * revoca en el acto para no dejarlo vivo.
   */
  async function entrar(usuario: string, contrasena: string): Promise<void> {
    const r = await enviar('/auth/login', { metodo: 'POST', cuerpo: { username: usuario, password: contrasena } }, false);
    if (r.status === 429) {
      const espera = r.headers.get('Retry-After');
      throw new ApiError(429, espera
        ? `Demasiados intentos. Prueba otra vez dentro de ${espera} segundos.`
        : 'Demasiados intentos. Prueba otra vez dentro de un momento.');
    }
    if (r.status === 400 || r.status === 401 || r.status === 403) throw new ApiError(r.status, AVISO_ENTRADA);
    if (!r.ok) throw await errorDe(r);

    const datos = (await r.json()) as RespuestaToken;
    if (!datos.roles?.includes('ADMIN')) {
      // La cuenta existe y la contraseña vale, pero no es de administración. Se revoca
      // lo que acaba de emitir la API; si falla, el refresh caduca solo.
      void enviar('/auth/logout', { metodo: 'POST', cuerpo: { refreshToken: datos.refreshToken } }, false)
        .catch(() => undefined);
      throw new ApiError(401, AVISO_ENTRADA);
    }
    tokens = { acceso: datos.token, renovacion: datos.refreshToken, usuario: datos.username };
  }

  /** Sale: revoca el refresh token en la API y borra los tokens de memoria. */
  async function salir(): Promise<void> {
    const actual = tokens;
    tokens = null;
    if (!actual) return;
    try {
      await enviar('/auth/logout', { metodo: 'POST', cuerpo: { refreshToken: actual.renovacion } }, false);
    } catch {
      // Ya no hay tokens en memoria; si la API no contesta, el refresh caduca solo.
    }
  }

  /** Petición pública, sin token (el estado de la API). */
  async function pedirPublico<T>(ruta: string, senal?: AbortSignal): Promise<T> {
    const r = await enviar(ruta, { senal }, false);
    if (!r.ok) throw await errorDe(r);
    return (await r.json()) as T;
  }

  return {
    pedir,
    pedirPublico,
    entrar,
    salir,
    haySesion: () => tokens !== null,
    usuario: () => tokens?.usuario ?? null,
    alPerderSesion: (f: (() => void) | undefined) => {
      alPerderSesion = f;
    },
    base,
  };
}

/** Cliente de la web, contra la API de VITE_API_URL. */
export const api = crearCliente({ base: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api' });
