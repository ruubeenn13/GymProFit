// Estado de la API en el pie de la barra lateral: si una petición se queda sin
// respuesta, lo comprueba en el acto y deja de decir «API en marcha»; y «Comprobar
// otra vez» vuelve a preguntar.
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EVENTO_SIN_RESPUESTA } from '../util/useCarga';
import { LineaApi, ProveedorEstadoApi } from './EstadoApi';

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('estado de la API', () => {
  it('pasa a «sin respuesta» en cuanto una lista no llega a la API', async () => {
    let caida = false;
    vi.stubGlobal('fetch', vi.fn(async (url: string) => {
      if (caida) throw new TypeError('Failed to fetch');
      const cuerpo = String(url).endsWith('/actuator/health') ? { status: 'UP' } : { commit: 'b10fffe1234' };
      return new Response(JSON.stringify(cuerpo), { status: 200 });
    }));

    render(<ProveedorEstadoApi><LineaApi /></ProveedorEstadoApi>);
    expect(await screen.findByText(/API en marcha/)).toBeTruthy();
    expect(screen.getByRole('link').getAttribute('href')).toBe('https://github.com/ruubeenn13/GymProFit/commit/b10fffe1234');

    caida = true;
    act(() => { window.dispatchEvent(new Event(EVENTO_SIN_RESPUESTA)); });
    expect(await screen.findByText(/API sin respuesta/)).toBeTruthy();

    caida = false;
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar otra vez' }));
    expect(await screen.findByText(/API en marcha/)).toBeTruthy();
  });
});
