// ============================================================
// Estado de la API: si responde (/actuator/health) y qué commit corre
// (/actuator/info), con enlace al commit en GitHub (GP-085). Se comprueba al
// abrir, cada minuto y en cuanto una petición se queda sin respuesta; lo comparten
// el pie de la barra lateral (o el menú de cuenta en el móvil) y la tarjeta Sistema
// del Resumen.
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

/**
 * Una línea: «● API en marcha · abc1234», con el enlace al commit y el botón de
 * «Comprobar otra vez». Va en el pie de la barra lateral y en el menú del móvil;
 * en la barra de iconos se reduce al punto y al botón, y el texto sale al pasar.
 */
export function LineaApi() {
  const e = useEstadoApi();
  const estado = e.enMarcha === null ? 'Comprobando la API…' : e.enMarcha ? 'API en marcha' : 'API sin respuesta';
  const clase = e.enMarcha ? 'linea-api--ok' : e.enMarcha === false ? 'linea-api--mal' : '';
  return (
    <div className={`linea-api ${clase}`}>
      <Icono nombre="circle_relleno" tamano={16} className="linea-api__punto" />
      <span className="linea-api__texto" role="status" aria-live="polite">
        {estado}
        {' · '}
        {e.enlaceCommit ? (
          <a href={e.enlaceCommit} target="_blank" rel="noreferrer noopener"
             aria-label={`Commit ${e.commit} en ${e.entorno === 'Producción' ? 'producción' : 'local'}. Ver en GitHub (se abre en otra pestaña)`}>
            {e.commit}
          </a>
        ) : (
          <span>{e.entorno === 'Producción' ? 'sin commit' : 'local'}</span>
        )}
      </span>
      <button type="button" className="boton-icono linea-api__comprobar" aria-label="Comprobar otra vez"
              title="Comprobar otra vez" onClick={e.comprobar}>
        <Icono nombre="refresh" tamano={18} />
      </button>
    </div>
  );
}
