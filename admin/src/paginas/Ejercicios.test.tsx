// Ejercicios: 25 por página, el nombre en español marcado como obligatorio, y el
// reparto por ancho: por debajo de 1440 px, la lista y el editor uno cada vez.
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Ejercicio, EjercicioDetalle } from '../api/admin';
import { simularAnchura } from '../pruebas/anchura';
import { pintarPantalla } from '../pruebas/pintar';

const EJERCICIOS: Ejercicio[] = [
  { id: 1, nombre: 'Air Bike', nombreEn: 'Air Bike', grupoMuscular: 'ABDOMEN', equipamiento: 'PESO_CORPORAL',
    dificultad: 'PRINCIPIANTE', activo: true, nombreRevisado: false },
  { id: 2, nombre: 'Sentadilla', nombreEn: 'Squat', grupoMuscular: 'PIERNAS', equipamiento: 'BARRA',
    dificultad: 'INTERMEDIO', activo: true, nombreRevisado: true },
];

const detalle = (e: Ejercicio): EjercicioDetalle => ({
  ...e, descripcion: null, descripcionEn: null, instrucciones: null, instruccionesEn: null, musculoPrimario: null,
  musculoPrimarioEn: null, imagenUrl: null, imagenUrl2: null, origen: 'MANUAL', equipoNecesario: null, rutinas: 0,
});

vi.mock('../api/admin', () => ({
  admin: {
    resumenEjercicios: vi.fn(async () => ({ activos: 2, sinRevisar: 1, equipamientos: [] })),
    ejercicios: vi.fn(async () => ({ content: EJERCICIOS, page: 0, size: 25, totalElements: 2, totalPages: 1, last: true })),
    ejercicio: vi.fn(async (id: number) => detalle(EJERCICIOS.find((e) => e.id === id)!)),
    salud: vi.fn(async () => ({ status: 'UP' })),
    info: vi.fn(async () => ({ commit: 'local' })),
  },
}));

const { admin } = await import('../api/admin');
const { Ejercicios } = await import('./Ejercicios');

afterEach(cleanup);

describe('Ejercicios', () => {
  it('desde 1440 px abre el primero al lado de la lista y pide 25 por página', async () => {
    simularAnchura(1440);
    pintarPantalla('/ejercicios', <Ejercicios />);
    expect(await screen.findByRole('heading', { level: 2, name: 'Air Bike' })).toBeTruthy();
    expect(screen.getByRole('button', { name: /Sentadilla/ })).toBeTruthy();
    expect(vi.mocked(admin.ejercicios)).toHaveBeenCalledWith(expect.objectContaining({ size: 25 }), expect.anything());
  });

  it('el nombre en español es obligatorio y lo dice', async () => {
    simularAnchura(1440);
    pintarPantalla('/ejercicios', <Ejercicios />);
    await screen.findByRole('heading', { level: 2, name: 'Air Bike' });
    const nombre = screen.getByLabelText(/Nombre en español/);
    expect(nombre.getAttribute('aria-required')).toBe('true');
    expect((nombre as HTMLInputElement).labels?.[0].textContent).toMatch(/obligatorio/);
  });

  it('por debajo de 1440 px el editor ocupa la pantalla y «Volver a la lista» devuelve el foco a la fila', async () => {
    simularAnchura(768);
    pintarPantalla('/ejercicios', <Ejercicios />);
    fireEvent.click(await screen.findByRole('button', { name: /Sentadilla/ }));
    expect(await screen.findByRole('heading', { level: 2, name: 'Sentadilla' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: /Air Bike/ })).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Volver a la lista' }));
    await waitFor(() => expect(document.activeElement).toBe(screen.getByRole('button', { name: /Sentadilla/ })));
  });
});
