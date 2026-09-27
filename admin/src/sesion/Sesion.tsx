// ============================================================
// Sesión de la web: quién ha entrado, y entrar y salir (GP-085)
// El estado vive en memoria, como los tokens del cliente. Si el cliente pierde la
// sesión (401 sin renovación posible), aquí se entera y la app vuelve a Entrada.
// ============================================================
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, type Cliente } from '../api/cliente';

interface ValorSesion {
  usuario: string | null;
  /** true si la sesión se perdió sola (caducó), para decirlo en Entrada. */
  caducada: boolean;
  entrar: (usuario: string, contrasena: string) => Promise<void>;
  salir: () => Promise<void>;
}

const ContextoSesion = createContext<ValorSesion | null>(null);

/**
 * @param props.cliente cliente de la API; el de la web por defecto (los tests pasan otro)
 */
export function ProveedorSesion({ children, cliente = api }: { children: ReactNode; cliente?: Cliente }) {
  const [usuario, setUsuario] = useState<string | null>(cliente.usuario());
  const [caducada, setCaducada] = useState(false);

  useEffect(() => {
    cliente.alPerderSesion(() => {
      setUsuario(null);
      setCaducada(true);
    });
    return () => cliente.alPerderSesion(undefined);
  }, [cliente]);

  const entrar = useCallback(async (u: string, c: string) => {
    await cliente.entrar(u, c);
    setCaducada(false);
    setUsuario(cliente.usuario());
  }, [cliente]);

  const salir = useCallback(async () => {
    await cliente.salir();
    setCaducada(false);
    setUsuario(null);
  }, [cliente]);

  const valor = useMemo(() => ({ usuario, caducada, entrar, salir }), [usuario, caducada, entrar, salir]);
  return <ContextoSesion.Provider value={valor}>{children}</ContextoSesion.Provider>;
}

/** La sesión actual. Solo dentro de ProveedorSesion. */
export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion);
  if (!valor) throw new Error('useSesion fuera de ProveedorSesion');
  return valor;
}
