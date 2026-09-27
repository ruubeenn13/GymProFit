// Alimentos: el título del documento, la fila abierta anunciada con aria-current
// (no aria-selected, que no vale en una tabla), y el aviso antes de tirar lo
// escrito en el editor al elegir otra fila.
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Alimento } from '../api/admin';

const ALIMENTOS: Alimento[] = [
  { id: 1, nombre: 'Yogur natural', nombreEn: null, marca: null, categoria: 'Lácteos', barcode: null, calorias: 61,
    proteinas: 3.5, carbohidratos: 4.7, grasas: 3.3, fibra: 0, porcionGramos: 125, activo: true, origen: 'MANUAL' },
  { id: 2, nombre: 'Plátano', nombreEn: 'Banana', marca: null, categoria: 'Frutas', barcode: null, calorias: 89,
    proteinas: 1.1, carbohidratos: 22.8, grasas: 0.3, fibra: 2.6, porcionGramos: 120, activo: true, origen: 'MANUAL' },
];

vi.mock('../api/admin', () => ({
  admin: {
    resumenAlimentos: vi.fn(async () => ({ catalogo: 2, sinIngles: 1, categorias: ['Frutas', 'Lácteos'] })),
    alimentos: vi.fn(async () => ({ content: ALIMENTOS, page: 0, size: 8, totalElements: 2, totalPages: 1, last: true })),
    guardarAlimento: vi.fn(),
    salud: vi.fn(async () => ({ status: 'UP' })),
    info: vi.fn(async () => ({ commit: 'local' })),
  },
}));

const { Alimentos } = await import('./Alimentos');
const { ProveedorSesion } = await import('../sesion/Sesion');
const { ProveedorEstadoApi } = await import('../componentes/EstadoApi');

beforeEach(() => {
  render(
    <ProveedorSesion>
      <MemoryRouter>
        <ProveedorEstadoApi><Alimentos /></ProveedorEstadoApi>
      </MemoryRouter>
    </ProveedorSesion>,
  );
});
afterEach(cleanup);

const fila = (nombre: string) => screen.getByRole('button', { name: new RegExp(nombre) });

describe('Alimentos', () => {
  it('pone su título al documento', async () => {
    await screen.findByText('Yogur natural', { selector: 'h2' });
    expect(document.title).toBe('Alimentos · GymProFit Admin');
  });

  it('la fila abierta se anuncia con aria-current y ninguna fila usa aria-selected', async () => {
    await screen.findByText('Yogur natural', { selector: 'h2' });
    expect(fila('Yogur natural').getAttribute('aria-current')).toBe('true');
    expect(fila('Plátano').getAttribute('aria-current')).toBeNull();
    expect(document.querySelector('[aria-selected]')).toBeNull();
  });

  it('con cambios sin guardar, elegir otra fila pregunta; «Seguir editando» no pierde nada', async () => {
    await screen.findByText('Yogur natural', { selector: 'h2' });
    fireEvent.change(screen.getByLabelText('Nombre en inglés'), { target: { value: 'Plain yogurt' } });
    fireEvent.click(fila('Plátano'));

    expect(await screen.findByText('¿Descartar los cambios?')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Seguir editando' }));
    expect((screen.getByLabelText('Nombre en inglés') as HTMLInputElement).value).toBe('Plain yogurt');
    expect(screen.getByText('Yogur natural', { selector: 'h2' })).toBeTruthy();
  });

  it('«Descartar» abre la otra fila', async () => {
    await screen.findByText('Yogur natural', { selector: 'h2' });
    fireEvent.change(screen.getByLabelText('Nombre en inglés'), { target: { value: 'Plain yogurt' } });
    fireEvent.click(fila('Plátano'));
    fireEvent.click(await screen.findByRole('button', { name: 'Descartar' }));
    expect(await screen.findByText('Plátano', { selector: 'h2' })).toBeTruthy();
  });

  it('sin cambios, elegir otra fila no pregunta', async () => {
    await screen.findByText('Yogur natural', { selector: 'h2' });
    fireEvent.click(fila('Plátano'));
    expect(await screen.findByText('Plátano', { selector: 'h2' })).toBeTruthy();
    expect(screen.queryByText('¿Descartar los cambios?')).toBeNull();
  });
});
