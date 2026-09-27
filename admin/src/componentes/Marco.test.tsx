// Marco: la barra lateral solo enseña lo que existe; la API y el commit viven en su
// pie, en una línea, y no en la cabecera; los enlaces tienen nombre aunque la barra
// se quede en iconos; y en el móvil, la cuenta y la línea de la API van en un menú.
import { cleanup, fireEvent, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { simularAnchura } from '../pruebas/anchura';
import { pintarPantalla } from '../pruebas/pintar';
import { Marco } from './Marco';

vi.mock('../api/admin', () => ({
  admin: {
    salud: vi.fn(async () => ({ status: 'UP' })),
    info: vi.fn(async () => ({ commit: 'b10fffe1234abcd' })),
  },
}));

afterEach(cleanup);

function pintar() {
  simularAnchura(1440);
  pintarPantalla('/usuarios', <Marco titulo="Usuarios" subtitulo="15 cuentas"><p>contenido</p></Marco>);
}

describe('Marco', () => {
  it('no anuncia lo que aún no existe', () => {
    pintar();
    expect(screen.queryByText('Más adelante')).toBeNull();
    expect(screen.queryByText('Importaciones')).toBeNull();
  });

  it('la API y el commit salen en el pie de la barra lateral, no en la cabecera', async () => {
    pintar();
    const lateral = screen.getByRole('complementary', { name: 'Barra lateral' });
    const commit = await within(lateral).findByRole('link', { name: /b10fffe/ });
    expect(commit.getAttribute('href')).toBe('https://github.com/ruubeenn13/GymProFit/commit/b10fffe1234abcd');
    expect(within(lateral).getByRole('button', { name: 'Comprobar otra vez' })).toBeTruthy();
    expect(within(lateral).getByText(/API en marcha/)).toBeTruthy();

    const cabecera = screen.getByRole('heading', { level: 1, name: 'Usuarios' }).closest('header')!;
    expect(cabecera.textContent).not.toMatch(/API|commit/);
    expect(cabecera.textContent).toContain('15 cuentas');
  });

  it('cada sección de la barra lateral tiene su nombre accesible', () => {
    pintar();
    const nav = screen.getByRole('navigation', { name: 'Secciones' });
    for (const nombre of ['Resumen', 'Usuarios', 'Ejercicios', 'Alimentos']) {
      expect(within(nav).getByRole('link', { name: nombre })).toBeTruthy();
    }
  });

  it('en el móvil, cuatro pestañas abajo y un menú con la cuenta, la API y cerrar sesión', async () => {
    pintar();
    const pestanas = screen.getByRole('navigation', { name: 'Pestañas' });
    expect(within(pestanas).getAllByRole('link').map((a) => a.textContent)).toEqual(['Resumen', 'Usuarios', 'Ejercicios', 'Alimentos']);

    fireEvent.click(screen.getByRole('button', { name: 'Cuenta y estado de la API' }));
    const menu = await screen.findByRole('dialog', { name: 'Cuenta' });
    expect(within(menu).getByRole('button', { name: 'Cerrar sesión' })).toBeTruthy();
    expect(within(menu).getByRole('button', { name: 'Comprobar otra vez' })).toBeTruthy();
    expect(await within(menu).findByText(/API en marcha/)).toBeTruthy();
  });
});
