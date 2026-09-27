// ============================================================
// useMedia — si la ventana cumple una media query, y se entera cuando cambia (GP-120)
// Para lo que no se resuelve solo con CSS: pintar tabla o tarjetas, o la lista y el
// editor juntos o por separado. Sin matchMedia (jsdom), contesta que no.
// ============================================================
import { useCallback, useSyncExternalStore } from 'react';

/** Anchos a partir de los que cambia el reparto de la web. */
export const ANCHO = {
  /** Por debajo, el móvil: pestañas abajo, tarjetas y todo a pantalla completa. */
  movil: '(max-width: 767px)',
  /** Desde aquí, lista y editor lado a lado en Ejercicios y Alimentos. */
  dosColumnas: '(min-width: 1440px)',
} as const;

/** @param consulta media query, p. ej. ANCHO.movil */
export function useMedia(consulta: string): boolean {
  const suscribir = useCallback((avisar: () => void) => {
    if (typeof window.matchMedia !== 'function') return () => {};
    const mq = window.matchMedia(consulta);
    mq.addEventListener('change', avisar);
    return () => mq.removeEventListener('change', avisar);
  }, [consulta]);
  return useSyncExternalStore(suscribir, () => typeof window.matchMedia === 'function' && window.matchMedia(consulta).matches);
}
