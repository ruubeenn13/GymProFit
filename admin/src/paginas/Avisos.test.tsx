// Avisos (lote 1.6.1): el título, los avisos con su motivo y sus veces, el editor de
// siempre al elegir uno del catálogo, «Resuelto» que lo quita, el producto sin
// materializar que no se edita, y la lista vacía.
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AvisoAlimento } from '../api/admin';
import { simularAnchura } from '../pruebas/anchura';
import { pintarPantalla } from '../pruebas/pintar';

const YOGUR: AvisoAlimento = {
  id: 7, alimentoId: 1, barcode: null, nombre: 'Yogur natural', marca: null, fuente: null, motivo: 'VALORES', veces: 3,
  creado: '2026-10-02T09:00:00', actualizado: '2026-10-02T10:00:00',
  alimento: { id: 1, nombre: 'Yogur natural', nombreEn: null, marca: null, categoria: 'Lácteos', barcode: null, calorias: 61,
    proteinas: 3.5, carbohidratos: 4.7, grasas: 3.3, fibra: 0, porcionGramos: 125, activo: true, origen: 'MANUAL',
    fuente: null, revisado: false },
};
const GALLETAS: AvisoAlimento = {
  id: 8, alimentoId: null, barcode: '8400000730013', nombre: 'Galletas María', marca: 'Marca', fuente: 'OFF', motivo: 'REPETIDO',
  veces: 1, creado: '2026-10-01T09:00:00', actualizado: '2026-10-01T09:00:00', alimento: null,
};

let pendientes: AvisoAlimento[] = [YOGUR, GALLETAS];

vi.mock('../api/admin', () => ({
  admin: {
    resumenAlimentos: vi.fn(async () => ({ catalogo: 2, sinIngles: 1, categorias: ['Lácteos'] })),
    avisos: vi.fn(async () => ({ content: pendientes, page: 0, size: 25, totalElements: pendientes.length, totalPages: 1, last: true })),
    resolverAviso: vi.fn(async (id: number) => { pendientes = pendientes.filter((a) => a.id !== id); }),
    guardarAlimento: vi.fn(),
    salud: vi.fn(async () => ({ status: 'UP' })),
    info: vi.fn(async () => ({ commit: 'local' })),
  },
}));

const { admin } = await import('../api/admin');
const { Avisos } = await import('./Avisos');

afterEach(cleanup);

describe('Avisos desde 1440 px', () => {
  beforeEach(() => {
    pendientes = [YOGUR, GALLETAS];
    vi.mocked(admin.resolverAviso).mockClear();
    simularAnchura(1440);
    pintarPantalla('/avisos', <Avisos />);
  });

  it('pone su título y dice cuántos hay', async () => {
    await screen.findByRole('heading', { level: 2, name: 'Yogur natural' });
    expect(document.title).toBe('Avisos · GymProFit Admin');
    expect(screen.getByText('2 avisos pendientes de alimentos')).toBeTruthy();
  });

  it('cada aviso dice su motivo y sus veces, y el primero se abre con el editor de siempre', async () => {
    await screen.findByRole('heading', { level: 2, name: 'Yogur natural' });
    expect(screen.getByText(/Los valores no cuadran con la etiqueta · 3 veces/)).toBeTruthy();
    expect(screen.getByLabelText(/Nombre en español/)).toBeTruthy();
    // Sin «siguiente sin inglés»: aquí no pinta nada.
    expect(screen.queryByRole('button', { name: /siguiente sin inglés/ })).toBeNull();
  });

  it('«Resuelto» lo quita de la lista', async () => {
    await screen.findByRole('heading', { level: 2, name: 'Yogur natural' });
    fireEvent.click(screen.getByRole('button', { name: 'Resuelto' }));
    await waitFor(() => expect(vi.mocked(admin.resolverAviso)).toHaveBeenCalledWith(7));
    await screen.findByText('1 aviso pendiente de alimentos');
    expect(screen.queryByRole('button', { name: /^Yogur natural/ })).toBeNull();
  });

  it('un producto sin materializar no se edita, pero se reconoce y se resuelve', async () => {
    await screen.findByRole('heading', { level: 2, name: 'Yogur natural' });
    fireEvent.click(screen.getByRole('button', { name: /Galletas María/ }));
    await screen.findByRole('heading', { level: 2, name: 'Galletas María' });
    expect(screen.getByText('8400000730013')).toBeTruthy();
    expect(screen.queryByLabelText(/Nombre en español/)).toBeNull();
    expect(screen.getByRole('button', { name: 'Resuelto' })).toBeTruthy();
  });
});

describe('Avisos sin avisos', () => {
  it('lo dice', async () => {
    pendientes = [];
    simularAnchura(1440);
    pintarPantalla('/avisos', <Avisos />);
    expect(await screen.findByText('No hay avisos pendientes.')).toBeTruthy();
  });
});
