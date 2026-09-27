// Usuarios: 25 por página; la ficha como panel encima de la tabla, que se cierra con
// Esc o con la × y devuelve el foco a la fila; y en el móvil, tarjetas en vez de tabla.
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Cuenta } from '../api/admin';
import { simularAnchura } from '../pruebas/anchura';
import { pintarPantalla } from '../pruebas/pintar';

const CUENTAS: Cuenta[] = [
  { id: 7, username: 'ana', email: 'ana@test.local', rol: 'USER', activo: true, fechaRegistro: '2026-09-20T10:00:00Z', ultimoAcceso: null },
  { id: 8, username: 'luis', email: 'luis@test.local', rol: 'USER', activo: false, fechaRegistro: '2026-09-21T10:00:00Z', ultimoAcceso: null },
];

vi.mock('../api/admin', () => ({
  admin: {
    cuentas: vi.fn(async () => ({ content: CUENTAS, page: 0, size: 25, totalElements: 2, totalPages: 1, last: true })),
    cuenta: vi.fn(async (id: number) => ({ cuenta: CUENTAS.find((c) => c.id === id), sesiones: 3, comidas: 0 })),
    salud: vi.fn(async () => ({ status: 'UP' })),
    info: vi.fn(async () => ({ commit: 'local' })),
  },
}));

const { admin } = await import('../api/admin');
const { Usuarios } = await import('./Usuarios');

afterEach(cleanup);

const fila = (nombre: string) => screen.getByRole('button', { name: new RegExp(`@${nombre}`) });

describe('Usuarios en escritorio', () => {
  it('pide 25 por página', async () => {
    simularAnchura(1440);
    pintarPantalla('/usuarios', <Usuarios />);
    await screen.findByRole('table');
    expect(vi.mocked(admin.cuentas)).toHaveBeenCalledWith(expect.objectContaining({ size: 25 }), expect.anything());
  });

  it('la ficha se abre como panel con el foco dentro, y Esc la cierra devolviendo el foco a la fila', async () => {
    simularAnchura(1440);
    pintarPantalla('/usuarios', <Usuarios />);
    await screen.findByRole('table');
    fila('ana').focus();
    fireEvent.click(fila('ana'));

    const panel = await screen.findByRole('dialog', { name: 'Cuenta @ana' });
    await within(panel).findByText('ana@test.local');
    await waitFor(() => expect(panel.contains(document.activeElement)).toBe(true));

    fireEvent.keyDown(panel, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Cuenta @ana' })).toBeNull());
    expect(document.activeElement).toBe(fila('ana'));
  });

  it('un Esc con un diálogo abierto dentro de la ficha cierra el diálogo, no la ficha', async () => {
    // Chrome agrupa los dos diálogos y lanza «cancel» en los dos con un solo Esc.
    simularAnchura(1440);
    pintarPantalla('/usuarios', <Usuarios />);
    await screen.findByRole('table');
    fireEvent.click(fila('ana'));
    const panel = await screen.findByRole('dialog', { name: 'Cuenta @ana' });
    fireEvent.click(await within(panel).findByRole('button', { name: 'Dar rol de administrador' }));
    await within(panel).findByRole('dialog', { name: /Dar rol de administrador/ });

    fireEvent(panel, new Event('cancel', { cancelable: true }));
    expect(screen.getByRole('dialog', { name: 'Cuenta @ana' })).toBeTruthy();
  });

  it('la × también cierra la ficha y devuelve el foco a la fila', async () => {
    simularAnchura(1440);
    pintarPantalla('/usuarios', <Usuarios />);
    await screen.findByRole('table');
    fireEvent.click(fila('luis'));
    const panel = await screen.findByRole('dialog', { name: 'Cuenta @luis' });
    fireEvent.click(await within(panel).findByRole('button', { name: 'Cerrar la ficha' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Cuenta @luis' })).toBeNull());
    expect(document.activeElement).toBe(fila('luis'));
  });
});

describe('Usuarios en el móvil', () => {
  it('las cuentas son tarjetas con usuario, correo y estado, sin tabla', async () => {
    simularAnchura(390);
    pintarPantalla('/usuarios', <Usuarios />);
    const tarjeta = await screen.findByRole('button', { name: /@luis/ });
    expect(screen.queryByRole('table')).toBeNull();
    expect(tarjeta.textContent).toContain('luis@test.local');
    expect(tarjeta.textContent).toContain('Desactivada');
  });

  it('la ficha se cierra con «Volver»', async () => {
    simularAnchura(390);
    pintarPantalla('/usuarios', <Usuarios />);
    fireEvent.click(await screen.findByRole('button', { name: /@ana/ }));
    const panel = await screen.findByRole('dialog', { name: 'Cuenta @ana' });
    fireEvent.click(within(panel).getByRole('button', { name: 'Volver' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });
});
