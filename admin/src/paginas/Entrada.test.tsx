// Entrada: una cuenta que no es ADMIN no pasa, con el mismo aviso que una
// contraseña equivocada; una ADMIN entra y va a donde iba.
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import { AVISO_ENTRADA, crearCliente } from '../api/cliente';
import { ProveedorSesion } from '../sesion/Sesion';
import { Entrada } from './Entrada';

afterEach(cleanup);

function responder(estado: number, cuerpo: unknown): typeof fetch {
  return (async (url: string | URL | Request) => {
    if (String(url).endsWith('/auth/logout')) return new Response('{}', { status: 200 });
    return new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } });
  }) as typeof fetch;
}

function pintar(fetchFalso: typeof fetch) {
  const cliente = crearCliente({ base: 'https://api.test/api', fetch: fetchFalso });
  render(
    <ProveedorSesion cliente={cliente}>
      <MemoryRouter initialEntries={['/entrar']}>
        <Routes>
          <Route path="/entrar" element={<Entrada />} />
          <Route path="/" element={<p>Panel de administración</p>} />
        </Routes>
      </MemoryRouter>
    </ProveedorSesion>,
  );
  fireEvent.change(screen.getByLabelText('Usuario'), { target: { value: 'ana' } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'una frase larga' } });
  fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
  return cliente;
}

describe('Entrada', () => {
  it('una cuenta USER no pasa de Entrada y ve el aviso de contraseña equivocada', async () => {
    const cliente = pintar(responder(200, { token: 't', refreshToken: 'r', username: 'ana', roles: ['USER'] }));
    expect(await screen.findByRole('alert')).toHaveProperty('textContent', AVISO_ENTRADA);
    expect(screen.queryByText('Panel de administración')).toBeNull();
    expect(cliente.haySesion()).toBe(false);
  });

  it('una contraseña equivocada da exactamente el mismo aviso', async () => {
    pintar(responder(401, { code: 401, message: 'Credenciales inválidas' }));
    expect(await screen.findByRole('alert')).toHaveProperty('textContent', AVISO_ENTRADA);
  });

  it('una cuenta ADMIN entra', async () => {
    const cliente = pintar(responder(200, { token: 't', refreshToken: 'r', username: 'admin', roles: ['ADMIN'] }));
    expect(await screen.findByText('Panel de administración')).toBeTruthy();
    expect(cliente.usuario()).toBe('admin');
  });

  it('sin usuario o sin contraseña, lo dice antes de llamar a la API', async () => {
    const cliente = crearCliente({ base: 'https://api.test/api', fetch: responder(500, {}) });
    render(
      <ProveedorSesion cliente={cliente}>
        <MemoryRouter initialEntries={['/entrar']}><Entrada /></MemoryRouter>
      </ProveedorSesion>,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Escribe el usuario y la contraseña.');
  });
});
