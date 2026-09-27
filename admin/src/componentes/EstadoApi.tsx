// ============================================================
// Estado de la API: si responde (/actuator/health) y qué commit corre
// (/actuator/info), con enlace al commit en GitHub (GP-085). Se comprueba al
// abrir, cada minuto y en cuanto una petición se queda sin respuesta; lo comparten
// la cabecera y la tarjeta Sistema del Resumen.
// ============================================================
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { admin } from '../api/admin';
import { api } from '../api/cliente';
import { EVENTO_SIN_RESPUESTA } from '../util/useCarga';
import { Icono } from './Icono';

const REPOSITORIO = 'https://github.com/ruubeenn13/GymProFit';
const CADA_MS = 60_000;

export interface Estado {
  /** null mientras se comprueba por primera vez. */
  enMarcha: boolean | null;
  commit: string | null;
  comprobado: Date | null;
  /** Producción o local, según la API a la que apunta la web. */
  entorno: 'Producción' | 'Local';
  enlaceCommit: string | null;
  comprobar: () => void;
}

const ContextoEstado = createContext<Estado | null>(null);

export function ProveedorEstadoApi({ children }: { children: ReactNode }) {
  const [enMarcha, setEnMarcha] = useState<boolean | null>(null);
  const [commit, setCommit] = useState<string | null>(null);
  const [comprobado, setComprobado] = useState<Date | null>(null);
  const [vuelta, setVuelta] = useState(0);

  const comprobar = useCallback(() => setVuelta((v) => v + 1), []);

  useEffect(() => {
    const control = new AbortController();
    (async () => {
      try {
        const salud = await admin.salud(control.signal);
        setEnMarcha(salud.status === 'UP');
      } catch {
        // Sin respuesta: la API está caída o no se llega a ella. Es lo que se enseña.
        if (!control.signal.aborted) setEnMarcha(false);
      }
      try {
        const info = await admin.info(control.signal);
        setCommit(info.commit ?? null);
      } catch {
        // Sin info no hay commit que enseñar; la salud ya dice si responde.
        if (!control.signal.aborted) setCommit(null);
      }
      if (!control.signal.aborted) setComprobado(new Date());
    })();
    // Con la pestaña oculta no se pregunta: nadie lo ve. Al volver, se comprueba ya.
    const reloj = window.setInterval(() => { if (!document.hidden) comprobar(); }, CADA_MS);
    const alVolver = () => { if (!document.hidden) comprobar(); };
    window.addEventListener(EVENTO_SIN_RESPUESTA, comprobar);
    document.addEventListener('visibilitychange', alVolver);
    return () => {
      control.abort();
      window.clearInterval(reloj);
      window.removeEventListener(EVENTO_SIN_RESPUESTA, comprobar);
      document.removeEventListener('visibilitychange', alVolver);
    };
  }, [vuelta, comprobar]);

  const valor = useMemo<Estado>(() => {
    const esCommit = commit !== null && /^[0-9a-f]{7,40}$/i.test(commit);
    return {
      enMarcha,
      // En local, info dice "local": no es un commit y no se enseña como tal.
      commit: esCommit ? commit!.slice(0, 7) : null,
      comprobado,
      entorno: api.base.includes('api.gymprofit.app') ? 'Producción' : 'Local',
      enlaceCommit: esCommit ? `${REPOSITORIO}/commit/${commit}` : null,
      comprobar,
    };
  }, [enMarcha, commit, comprobado, comprobar]);

  return <ContextoEstado.Provider value={valor}>{children}</ContextoEstado.Provider>;
}

export function useEstadoApi(): Estado {
  const v = useContext(ContextoEstado);
  if (!v) throw new Error('useEstadoApi fuera de ProveedorEstadoApi');
  return v;
}

/** Las dos píldoras de la cabecera: «API en marcha» y «Producción · commit abc1234». */
export function PildorasEstado() {
  const e = useEstadoApi();
  const texto = e.enMarcha === null ? 'Comprobando la API…' : e.enMarcha ? 'API en marcha' : 'API sin respuesta';
  const commit = e.commit ? `${e.entorno} · commit ${e.commit}` : `${e.entorno} · sin commit`;
  return (
    <div className="pildoras" role="status" aria-live="polite">
      <span className={`pildora ${e.enMarcha ? 'pildora--ok' : e.enMarcha === false ? 'pildora--mal' : ''}`}>
        <Icono nombre="circle_relleno" tamano={16} />
        {texto}
      </span>
      {e.enlaceCommit ? (
        <a className="pildora pildora--borde" href={e.enlaceCommit} target="_blank" rel="noreferrer noopener"
           aria-label={`${commit}. Ver el commit en GitHub (se abre en otra pestaña)`}>
          {commit}
        </a>
      ) : (
        <span className="pildora pildora--borde">{commit}</span>
      )}
    </div>
  );
}
