// ============================================================
// useCarga — pide datos a la API con carga, error y reintento (GP-085)
// Cancela la petición anterior si cambian las dependencias o se desmonta, para
// que una respuesta vieja no pise a la nueva.
// ============================================================
import { useCallback, useEffect, useState, type DependencyList } from 'react';
import { ApiError } from '../api/cliente';

export interface Carga<T> {
  datos: T | null;
  cargando: boolean;
  error: string | null;
  recargar: () => void;
}

/** Texto de un error para enseñarlo tal cual. */
export function textoError(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  return 'No se ha podido contactar con la API. Comprueba la conexión.';
}

/**
 * @param pedir función que hace la petición con la señal de cancelación
 * @param deps  cuándo volver a pedir
 */
export function useCarga<T>(pedir: (senal: AbortSignal) => Promise<T>, deps: DependencyList): Carga<T> {
  const [datos, setDatos] = useState<T | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [vuelta, setVuelta] = useState(0);

  useEffect(() => {
    const control = new AbortController();
    setCargando(true);
    setError(null);
    pedir(control.signal)
      .then((d) => {
        if (!control.signal.aborted) setDatos(d);
      })
      .catch((e) => {
        // Una petición cancelada no es un error: la sustituye otra.
        if (!control.signal.aborted) setError(textoError(e));
      })
      .finally(() => {
        if (!control.signal.aborted) setCargando(false);
      });
    return () => control.abort();
    // pedir se recrea en cada render; las dependencias reales las da quien llama.
  }, [...deps, vuelta]);

  const recargar = useCallback(() => setVuelta((v) => v + 1), []);
  return { datos, cargando, error, recargar };
}
