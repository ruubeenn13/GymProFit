// Alimentos: el título del documento, la fila abierta anunciada con aria-current
// (no aria-selected, que no vale en una tabla), el aviso antes de tirar lo escrito
// (al elegir otra fila y al salir por un enlace), 25 por página, y cómo se reparte
// según el ancho: lista y editor juntos desde 1440 px; por debajo, uno cada vez.
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Alimento } from '../api/admin';
import { simularAnchura } from '../pruebas/anchura';
import { pintarPantalla } from '../pruebas/pintar';

const ALIMENTOS: Alimento[] = [
  { id: 1, nombre: 'Yogur natural', nombreEn: null, marca: null, categoria: 'Lácteos', barcode: null, calorias: 61,
    proteinas: 3.5, carbohidratos: 4.7, grasas: 3.3, fibra: 0, porcionGramos: 125, activo: true, origen: 'MANUAL' },
  { id: 2, nombre: 'Plátano', nombreEn: 'Banana', marca: null, categoria: 'Frutas', barcode: null, calorias: 89,
    proteinas: 1.1, carbohidratos: 22.8, grasas: 0.3, fibra: 2.6, porcionGramos: 120, activo: true, origen: 'MANUAL' },
];

vi.mock('../api/admin', () => ({
  admin: {
    resumenAlimentos: vi.fn(async () => ({ catalogo: 2, sinIngles: 1, categorias: ['Frutas', 'Lácteos'] })),
    alimentos: vi.fn(async () => ({ content: ALIMENTOS, page: 0, size: 25, totalElements: 2, totalPages: 1, last: true })),
    guardarAlimento: vi.fn(),
    salud: vi.fn(async () => ({ status: 'UP' })),
    info: vi.fn(async () => ({ commit: 'local' })),
  },
}));

const { admin } = await import('../api/admin');
const { Alimentos } = await import('./Alimentos');

afterEach(cleanup);

const fila = (nombre: string) => screen.getByRole('button', { name: new RegExp(nombre) });
const editor = (nombre: string) => screen.findByRole('heading', { level: 2, name: nombre });

describe('Alimentos desde 1440 px: lista y editor juntos', () => {
  beforeEach(() => {
    simularAnchura(1440);
    pintarPantalla('/alimentos', <Alimentos />);
  });

  it('pone su título al documento', async () => {
    await editor('Yogur natural');
    expect(document.title).toBe('Alimentos · GymProFit Admin');
  });

  it('pide 25 por página', async () => {
    await editor('Yogur natural');
    expect(vi.mocked(admin.alimentos)).toHaveBeenCalledWith(expect.objectContaining({ size: 25 }), expect.anything());
  });

  it('la fila abierta se anuncia con aria-current y ninguna fila usa aria-selected', async () => {
    await editor('Yogur natural');
    expect(fila('Yogur natural').getAttribute('aria-current')).toBe('true');
    expect(fila('Plátano').getAttribute('aria-current')).toBeNull();
    expect(document.querySelector('[aria-selected]')).toBeNull();
  });

  it('los obligatorios lo dicen en la etiqueta y con aria-required', async () => {
    await editor('Yogur natural');
    for (const campo of [screen.getByLabelText(/Nombre en español/), screen.getByLabelText(/^kcal/)]) {
      expect(campo.getAttribute('aria-required')).toBe('true');
      expect((campo as HTMLInputElement).labels?.[0].textContent).toMatch(/obligatorio/);
    }
    expect(screen.getByLabelText('Nombre en inglés').getAttribute('aria-required')).toBeNull();
  });

  it('con cambios sin guardar, elegir otra fila pregunta; «Seguir editando» no pierde nada', async () => {
    await editor('Yogur natural');
    fireEvent.change(screen.getByLabelText('Nombre en inglés'), { target: { value: 'Plain yogurt' } });
    fireEvent.click(fila('Plátano'));

    expect(await screen.findByText('¿Descartar los cambios?')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Seguir editando' }));
    expect((screen.getByLabelText('Nombre en inglés') as HTMLInputElement).value).toBe('Plain yogurt');
    expect(await editor('Yogur natural')).toBeTruthy();
  });

  it('«Descartar» abre la otra fila', async () => {
    await editor('Yogur natural');
    fireEvent.change(screen.getByLabelText('Nombre en inglés'), { target: { value: 'Plain yogurt' } });
    fireEvent.click(fila('Plátano'));
    fireEvent.click(await screen.findByRole('button', { name: 'Descartar' }));
    expect(await editor('Plátano')).toBeTruthy();
  });

  it('sin cambios, elegir otra fila no pregunta', async () => {
    await editor('Yogur natural');
    fireEvent.click(fila('Plátano'));
    expect(await editor('Plátano')).toBeTruthy();
    expect(screen.queryByText('¿Descartar los cambios?')).toBeNull();
  });

  it('con cambios sin guardar, salir por la barra lateral también pregunta', async () => {
    await editor('Yogur natural');
    fireEvent.change(screen.getByLabelText('Nombre en inglés'), { target: { value: 'Plain yogurt' } });
    fireEvent.click(screen.getAllByRole('link', { name: 'Resumen' })[0]);

    expect(await screen.findByText('¿Descartar los cambios?')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Seguir editando' }));
    expect((screen.getByLabelText('Nombre en inglés') as HTMLInputElement).value).toBe('Plain yogurt');
    expect(screen.queryByText('Pantalla de inicio')).toBeNull();

    fireEvent.click(screen.getAllByRole('link', { name: 'Resumen' })[0]);
    fireEvent.click(await screen.findByRole('button', { name: 'Descartar' }));
    expect(await screen.findByText('Pantalla de inicio')).toBeTruthy();
  });
});

describe('Alimentos por debajo de 1440 px: la lista, y el editor a toda la pantalla', () => {
  beforeEach(() => {
    simularAnchura(1024);
    pintarPantalla('/alimentos', <Alimentos />);
  });

  it('empieza por la lista, sin editor abierto', async () => {
    await screen.findByRole('button', { name: /Plátano/ });
    expect(screen.queryByRole('heading', { level: 2 })).toBeNull();
  });

  it('al pulsar una fila se abre el editor con «Volver a la lista», que devuelve el foco a la fila', async () => {
    fireEvent.click(await screen.findByRole('button', { name: /Plátano/ }));
    expect(await editor('Plátano')).toBeTruthy();
    expect(screen.queryByRole('button', { name: /Yogur natural/ })).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Volver a la lista' }));
    await waitFor(() => expect(document.activeElement).toBe(fila('Plátano')));
    expect(screen.queryByRole('heading', { level: 2 })).toBeNull();
  });
});
