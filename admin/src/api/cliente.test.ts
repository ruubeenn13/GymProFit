// Cliente de la API: renovación ante un 401, un solo reintento, vuelta a Entrada
// cuando no se puede, y el rechazo de una cuenta que no es ADMIN.
import { describe, expect, it, vi } from 'vitest';
import { AVISO_ENTRADA, ApiError, crearCliente } from './cliente';

type Respuesta = { estado: number; cuerpo?: unknown; cabeceras?: Record<string, string> };

/** fetch falso: responde por orden y apunta cada petición. */
function fetchFalso(respuestas: Respuesta[]) {
  const peticiones: { url: string; metodo: string; auth: string | null; cuerpo: unknown }[] = [];
  const fn = vi.fn(async (url: string | URL | Request, init?: RequestInit) => {
    const cabeceras = (init?.headers ?? {}) as Record<string, string>;
    peticiones.push({
      url: String(url),
      metodo: init?.method ?? 'GET',
      auth: cabeceras.Authorization ?? null,
      cuerpo: init?.body ? JSON.parse(String(init.body)) : undefined,
    });
    const r = respuestas.shift();
    if (!r) throw new Error('petición de más: ' + String(url));
    return new Response(r.cuerpo === undefined ? null : JSON.stringify(r.cuerpo), {
      status: r.estado,
      headers: { 'Content-Type': 'application/json', ...(r.cabeceras ?? {}) },
    });
  });
  return { fn: fn as unknown as typeof fetch, peticiones };
}

const tokenAdmin = (n: number) => ({
  estado: 200,
  cuerpo: { token: `acceso-${n}`, refreshToken: `renovacion-${n}`, username: 'admin', roles: ['ADMIN'] },
});

async function clienteConSesion(respuestas: Respuesta[], alPerderSesion = vi.fn()) {
  const falso = fetchFalso([tokenAdmin(1), ...respuestas]);
  const cliente = crearCliente({ base: 'https://api.test/api', fetch: falso.fn, alPerderSesion });
  await cliente.entrar('admin', 'una frase larga');
  return { cliente, peticiones: falso.peticiones, alPerderSesion };
}

describe('entrar', () => {
  it('una cuenta ADMIN entra y las peticiones llevan su token', async () => {
    const { cliente, peticiones } = await clienteConSesion([{ estado: 200, cuerpo: { ok: 1 } }]);
    expect(cliente.haySesion()).toBe(true);
    expect(cliente.usuario()).toBe('admin');
    await cliente.pedir('/admin/resumen');
    expect(peticiones[1].auth).toBe('Bearer acceso-1');
  });

  it('una cuenta que no es ADMIN recibe el mismo aviso que una contraseña equivocada', async () => {
    const usuario = fetchFalso([
      { estado: 200, cuerpo: { token: 'a', refreshToken: 'r-user', username: 'ana', roles: ['USER'] } },
      { estado: 200, cuerpo: {} },
    ]);
    const cliente = crearCliente({ base: 'https://api.test/api', fetch: usuario.fn });
    const noAdmin = await cliente.entrar('ana', 'bien').catch((e) => e);

    const equivocada = crearCliente({
      base: 'https://api.test/api',
      fetch: fetchFalso([{ estado: 401, cuerpo: { code: 401, message: 'Credenciales inválidas' } }]).fn,
    });
    const mal = await equivocada.entrar('admin', 'mal').catch((e) => e);

    expect(noAdmin).toBeInstanceOf(ApiError);
    expect(noAdmin.message).toBe(AVISO_ENTRADA);
    expect(mal.message).toBe(AVISO_ENTRADA);
    expect(cliente.haySesion()).toBe(false);
    // Y el refresh que acaba de emitir la API para esa cuenta se revoca.
    await vi.waitFor(() => expect(usuario.peticiones[1]).toMatchObject({
      url: 'https://api.test/api/auth/logout', cuerpo: { refreshToken: 'r-user' },
    }));
  });

  it('un invitado tampoco entra', async () => {
    const cliente = crearCliente({
      base: 'https://api.test/api',
      fetch: fetchFalso([
        { estado: 200, cuerpo: { token: 'a', refreshToken: 'r', username: 'guest', roles: ['GUEST'] } },
        { estado: 200, cuerpo: {} },
      ]).fn,
    });
    await expect(cliente.entrar('guest', 'x')).rejects.toThrow(AVISO_ENTRADA);
  });

  it('un 429 dice cuánto esperar', async () => {
    const cliente = crearCliente({
      base: 'https://api.test/api',
      fetch: fetchFalso([{ estado: 429, cuerpo: {}, cabeceras: { 'Retry-After': '60' } }]).fn,
    });
    await expect(cliente.entrar('admin', 'x')).rejects.toThrow('60 segundos');
  });
});

describe('renovación ante un 401', () => {
  it('renueva con /auth/refresh y reintenta una vez con el token nuevo', async () => {
    const { cliente, peticiones, alPerderSesion } = await clienteConSesion([
      { estado: 401, cuerpo: {} },
      tokenAdmin(2),
      { estado: 200, cuerpo: { total: 42 } },
    ]);
    await expect(cliente.pedir('/admin/resumen')).resolves.toEqual({ total: 42 });
    expect(peticiones.map((p) => p.url.replace('https://api.test/api', ''))).toEqual([
      '/auth/login', '/admin/resumen', '/auth/refresh', '/admin/resumen',
    ]);
    expect(peticiones[2].cuerpo).toEqual({ refreshToken: 'renovacion-1' });
    expect(peticiones[3].auth).toBe('Bearer acceso-2');
    expect(alPerderSesion).not.toHaveBeenCalled();
  });

  it('si la renovación falla, borra la sesión y vuelve a Entrada', async () => {
    const { cliente, peticiones, alPerderSesion } = await clienteConSesion([
      { estado: 401, cuerpo: {} },
      { estado: 401, cuerpo: {} },
    ]);
    await expect(cliente.pedir('/admin/resumen')).rejects.toMatchObject({ estado: 401 });
    expect(alPerderSesion).toHaveBeenCalledTimes(1);
    expect(cliente.haySesion()).toBe(false);
    expect(peticiones).toHaveLength(3);
  });

  it('solo un reintento: si el token nuevo también da 401, vuelve a Entrada', async () => {
    const { cliente, peticiones, alPerderSesion } = await clienteConSesion([
      { estado: 401, cuerpo: {} },
      tokenAdmin(2),
      { estado: 401, cuerpo: {} },
    ]);
    await expect(cliente.pedir('/admin/resumen')).rejects.toMatchObject({ estado: 401 });
    expect(peticiones).toHaveLength(4);
    expect(alPerderSesion).toHaveBeenCalledTimes(1);
    expect(cliente.haySesion()).toBe(false);
  });

  it('dos peticiones que caen a la vez comparten una sola renovación', async () => {
    const { cliente, peticiones } = await clienteConSesion([
      { estado: 401, cuerpo: {} },
      { estado: 401, cuerpo: {} },
      tokenAdmin(2),
      { estado: 200, cuerpo: { a: 1 } },
      { estado: 200, cuerpo: { b: 2 } },
    ]);
    await Promise.all([cliente.pedir('/uno'), cliente.pedir('/dos')]);
    expect(peticiones.filter((p) => p.url.endsWith('/auth/refresh'))).toHaveLength(1);
  });

  it('un error que no es 401 llega con su mensaje y no toca la sesión', async () => {
    const { cliente, alPerderSesion } = await clienteConSesion([
      { estado: 409, cuerpo: { code: 409, message: 'No puedes borrar tu propia cuenta' } },
    ]);
    await expect(cliente.pedir('/admin/cuentas/1', { metodo: 'DELETE' }))
      .rejects.toMatchObject({ estado: 409, message: 'No puedes borrar tu propia cuenta' });
    expect(alPerderSesion).not.toHaveBeenCalled();
    expect(cliente.haySesion()).toBe(true);
  });
});

describe('salir', () => {
  it('revoca el refresh en la API y olvida los tokens', async () => {
    const { cliente, peticiones } = await clienteConSesion([{ estado: 200, cuerpo: {} }]);
    await cliente.salir();
    expect(peticiones[1]).toMatchObject({ url: 'https://api.test/api/auth/logout', cuerpo: { refreshToken: 'renovacion-1' } });
    expect(cliente.haySesion()).toBe(false);
  });

  it('los tokens no se guardan en el navegador', async () => {
    await clienteConSesion([]);
    expect(window.localStorage.length).toBe(0);
    expect(window.sessionStorage.length).toBe(0);
  });
});
