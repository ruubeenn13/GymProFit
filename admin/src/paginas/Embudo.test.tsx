// Embudo (GP-019): los pasos del alta con su porcentaje sobre la bienvenida y lo que se
// pierde en cada uno, el filtro por semana, la semana sin pasos, y el núcleo por semana
// de alta con la marca de «en curso». Nada de nadie: solo números.
import { cleanup, fireEvent, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Embudo as DatosEmbudo, PasoAlta } from '../api/admin';
import { simularAnchura } from '../pruebas/anchura';
import { pintarPantalla } from '../pruebas/pintar';

const PASOS: PasoAlta[] = ['BIENVENIDA', 'OBJETIVO', 'NIVEL', 'SOBRE_TI', 'DONDE', 'DIAS', 'PLAN', 'GUARDA', 'CUENTA', 'AVISOS'];
const semana = (lunes: string, totales: number[]) =>
  ({ lunes, pasos: PASOS.map((paso, i) => ({ paso, total: totales[i] ?? 0 })) });

const DATOS: DatosEmbudo = {
  hoy: '2026-11-12',
  embudo: [
    semana('2026-11-03', [10, 8, 8, 6, 6, 5, 5, 4, 3, 3]),
    semana('2026-11-10', [10, 9, 8, 7, 7, 7, 6, 6, 5, 4]),
  ],
  nucleo: [
    { lunes: '2026-11-03', cuentas: 4, entrenan: 2, comen: 1, vuelven: 3, completa: false },
    { lunes: '2026-11-10', cuentas: 0, entrenan: 0, comen: 0, vuelven: 0, completa: false },
  ],
};

vi.mock('../api/admin', () => ({
  admin: {
    embudo: vi.fn(async () => DATOS),
    salud: vi.fn(async () => ({ status: 'UP' })),
    info: vi.fn(async () => ({ commit: 'local' })),
  },
}));

const { Embudo, porcentaje, sumarPasos } = await import('./Embudo');

afterEach(cleanup);

describe('sumarPasos y porcentaje', () => {
  it('suma las semanas, o solo la elegida, con los pasos en su orden', () => {
    expect(sumarPasos(DATOS.embudo, '').map((p) => p.total)).toEqual([20, 17, 16, 13, 13, 12, 11, 10, 8, 7]);
    expect(sumarPasos(DATOS.embudo, '2026-11-03')[8]).toEqual({ paso: 'CUENTA', total: 3 });
  });

  it('no divide entre cero', () => {
    expect(porcentaje(3, 0)).toBe('—');
    expect(porcentaje(8, 20)).toBe('40 %');
  });
});

describe('Embudo desde 1440 px', () => {
  beforeEach(() => {
    simularAnchura(1440);
    pintarPantalla('/embudo', <Embudo />);
  });

  it('pone su título y los diez pasos con su porcentaje sobre la bienvenida', async () => {
    const lista = await screen.findByRole('list');
    expect(document.title).toBe('Embudo · GymProFit Admin');
    const pasos = within(lista).getAllByRole('listitem');
    expect(pasos).toHaveLength(10);
    expect(pasos[0].textContent).toContain('Bienvenida');
    expect(pasos[0].textContent).toContain('100 %');
    expect(pasos[8].textContent).toContain('Todo listo (cuenta creada)');
    expect(pasos[8].textContent).toContain('40 %');
    // De «Guarda tu plan» (10) a «Todo listo» (8) se van 2.
    expect(pasos[8].textContent).toContain('−2');
  });

  it('el filtro enseña una sola semana', async () => {
    await screen.findByRole('list');
    fireEvent.change(screen.getByLabelText('Semana:'), { target: { value: '2026-11-03' } });
    const pasos = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(pasos[8].textContent).toContain('30 %');
  });

  it('el núcleo va de la semana más reciente a la más antigua, con su porcentaje', async () => {
    const tabla = await screen.findByRole('table');
    const filas = within(tabla).getAllByRole('row');
    expect(filas[1].textContent).toContain('semana del 10 nov');
    expect(filas[1].textContent).toContain('en curso');
    expect(filas[2].textContent).toContain('2 · 50 %');
    expect(filas[2].textContent).toContain('3 · 75 %');
    // Una semana sin cuentas no da porcentaje.
    expect(filas[1].textContent).toContain('0 · —');
  });
});

describe('Embudo sin pasos esa semana', () => {
  it('lo dice en vez de pintar barras a cero', async () => {
    vi.mocked((await import('../api/admin')).admin.embudo).mockResolvedValueOnce({
      ...DATOS, embudo: [semana('2026-11-10', [])],
    });
    simularAnchura(1440);
    pintarPantalla('/embudo', <Embudo />);
    expect(await screen.findByText('Ningún paso contado en estas semanas.')).toBeTruthy();
  });
});

describe('Embudo en el móvil', () => {
  it('el núcleo va en tarjetas, sin tabla', async () => {
    simularAnchura(390);
    pintarPantalla('/embudo', <Embudo />);
    expect(await screen.findByText('4 cuentas nuevas')).toBeTruthy();
    expect(screen.queryByRole('table')).toBeNull();
  });
});
