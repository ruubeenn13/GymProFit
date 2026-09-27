// Aviso flotante: el de éxito se va solo; el de error se queda hasta cerrarlo
// (un error que desaparece antes de leerlo no se ha dicho, WCAG 2.2.1).
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AvisoFlotante, type Aviso } from './Piezas';

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

function pintar(aviso: Aviso) {
  const alCerrar = vi.fn();
  render(<AvisoFlotante aviso={aviso} alCerrar={alCerrar} />);
  return alCerrar;
}

describe('AvisoFlotante', () => {
  it('el de éxito se cierra solo a los 5 s', () => {
    vi.useFakeTimers();
    const alCerrar = pintar({ tipo: 'ok', texto: 'Guardado.' });
    act(() => { vi.advanceTimersByTime(5000); });
    expect(alCerrar).toHaveBeenCalledTimes(1);
  });

  it('el de error no se cierra solo', () => {
    vi.useFakeTimers();
    const alCerrar = pintar({ tipo: 'error', texto: 'No se ha podido guardar.' });
    act(() => { vi.advanceTimersByTime(60_000); });
    expect(alCerrar).not.toHaveBeenCalled();
    expect(screen.getByRole('alert').textContent).toContain('No se ha podido guardar.');
  });

  it('la acción del aviso se ejecuta y lo cierra', () => {
    const reintentar = vi.fn();
    const alCerrar = pintar({ tipo: 'error', texto: 'Falló', accion: { texto: 'Reintentar', alPulsar: reintentar } });
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(reintentar).toHaveBeenCalledTimes(1);
    expect(alCerrar).toHaveBeenCalledTimes(1);
  });
});
