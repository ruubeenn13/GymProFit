// ============================================================
// Piezas pequeñas compartidas por las pantallas (GP-085, GP-120):
// estado de lista, paginación, filtro, interruptor, buscador, aviso flotante y el
// aviso de cambios sin guardar.
// ============================================================
import { useEffect, useId, useState, type ReactNode } from 'react';
import { useBlocker } from 'react-router-dom';
import { entero } from '../util/formato';
import { Dialogo } from './Dialogo';
import { Icono } from './Icono';

/** Carga, error con reintento, o vacío. Si no toca ninguno, pinta los hijos. */
export function EstadoLista({ cargando, error, vacio, textoVacio, alReintentar, children }: {
  cargando: boolean;
  error: string | null;
  vacio: boolean;
  textoVacio: string;
  alReintentar: () => void;
  children: ReactNode;
}) {
  if (error) {
    return (
      <div className="estado-lista estado-lista--error" role="alert">
        <Icono nombre="error" />
        <span className="estado-lista__titulo">No se ha podido cargar</span>
        <span>{error}</span>
        <button type="button" className="boton" onClick={alReintentar}>
          <Icono nombre="refresh" tamano={20} />Reintentar
        </button>
      </div>
    );
  }
  if (cargando) {
    return (
      <div className="estado-lista" role="status" aria-live="polite">
        <div className="cargando-barra" />
        <span>Cargando…</span>
      </div>
    );
  }
  if (vacio) {
    return (
      <div className="estado-lista" role="status">
        <span className="estado-lista__titulo">Nada por aquí</span>
        <span>{textoVacio}</span>
      </div>
    );
  }
  return <>{children}</>;
}

/** Filas por página en las tres listas. */
export const POR_PAGINA = 25;

/** «1–25 de 42» y las flechas de página. */
export function Paginacion({ pagina, tamano, total, alCambiar }: {
  pagina: number; tamano: number; total: number; alCambiar: (p: number) => void;
}) {
  const desde = total === 0 ? 0 : pagina * tamano + 1;
  const hasta = Math.min(total, (pagina + 1) * tamano);
  const ultima = Math.max(0, Math.ceil(total / tamano) - 1);
  return (
    <div className="pie-lista">
      <span aria-live="polite">{`${entero(desde)}–${entero(hasta)} de ${entero(total)}`}</span>
      <div className="pie-lista__botones">
        <button type="button" className="boton-icono boton-icono--borde" aria-label="Página anterior"
                disabled={pagina === 0} onClick={() => alCambiar(pagina - 1)}>
          <Icono nombre="chevron_left" />
        </button>
        <button type="button" className="boton-icono boton-icono--borde" aria-label="Página siguiente"
                disabled={pagina >= ultima} onClick={() => alCambiar(pagina + 1)}>
          <Icono nombre="chevron_right" />
        </button>
      </div>
    </div>
  );
}

/** Un select con aspecto de píldora: «Rol: todos ▾». */
export function Filtro({ etiqueta, valor, opciones, alCambiar, alto }: {
  etiqueta: string;
  valor: string;
  opciones: { valor: string; texto: string }[];
  alCambiar: (v: string) => void;
  alto?: boolean;
}) {
  const id = useId();
  return (
    <div className={`filtro${alto ? ' filtro--alto' : ''}`}>
      <label htmlFor={id} className="filtro__etiqueta">{etiqueta}:</label>
      <select id={id} value={valor} onChange={(e) => alCambiar(e.target.value)}>
        {opciones.map((o) => <option key={o.valor} value={o.valor}>{o.texto}</option>)}
      </select>
      <Icono nombre="expand_more" tamano={18} />
    </div>
  );
}

/** Interruptor accesible (role="switch") con su texto. */
export function Interruptor({ texto, activo, alCambiar }: { texto: string; activo: boolean; alCambiar: (v: boolean) => void }) {
  const id = useId();
  return (
    <div className="interruptor">
      <span id={id}>{texto}</span>
      <button type="button" role="switch" aria-checked={activo} aria-labelledby={id}
              className="interruptor__boton" onClick={() => alCambiar(!activo)}>
        <span className="interruptor__pista"><span className="interruptor__bola" /></span>
      </button>
    </div>
  );
}

/**
 * Buscador con espera: avisa del texto cuando se deja de escribir 300 ms.
 */
export function Buscador({ etiqueta, marcador, alBuscar }: { etiqueta: string; marcador: string; alBuscar: (q: string) => void }) {
  const [texto, setTexto] = useState('');
  useEffect(() => {
    const t = window.setTimeout(() => alBuscar(texto.trim()), 300);
    return () => window.clearTimeout(t);
    // alBuscar cambia en cada render de quien lo usa; solo importa el texto.
  }, [texto]);
  return (
    <label className="buscador">
      <Icono nombre="search" tamano={20} />
      <input type="search" aria-label={etiqueta} placeholder={marcador} value={texto}
             onChange={(e) => setTexto(e.target.value)} />
    </label>
  );
}

export interface Aviso {
  tipo: 'ok' | 'error';
  texto: string;
  /** Acción opcional en el propio aviso (por ejemplo, reintentar). */
  accion?: { texto: string; alPulsar: () => void };
}

/**
 * Aviso flotante. El de éxito se va solo a los 5 s; el de error se queda hasta que
 * se cierra: un error que desaparece antes de leerlo no se ha dicho (WCAG 2.2.1).
 */
export function AvisoFlotante({ aviso, alCerrar }: { aviso: Aviso | null; alCerrar: () => void }) {
  useEffect(() => {
    if (!aviso || aviso.tipo === 'error') return;
    const t = window.setTimeout(alCerrar, 5000);
    return () => window.clearTimeout(t);
  }, [aviso, alCerrar]);
  if (!aviso) return null;
  return (
    <div className={`aviso-flotante aviso-flotante--${aviso.tipo}`} role={aviso.tipo === 'error' ? 'alert' : 'status'}>
      <Icono nombre={aviso.tipo === 'error' ? 'error' : 'check_circle'} tamano={20} />
      <span>{aviso.texto}</span>
      {aviso.accion && (
        <button type="button" className="enlace-accion" onClick={() => { aviso.accion!.alPulsar(); alCerrar(); }}>
          {aviso.accion.texto}
        </button>
      )}
      <button type="button" className="boton-icono" aria-label="Cerrar el aviso" onClick={alCerrar}>
        <Icono nombre="close" tamano={20} />
      </button>
    </div>
  );
}

/**
 * Con cambios sin guardar, el navegador pregunta antes de cerrar o recargar la
 * pestaña: los tokens viven en memoria, así que recargar además cierra la sesión.
 *
 * @param sucio si hay algo escrito que no se ha guardado
 */
export function useAvisoAlSalir(sucio: boolean) {
  useEffect(() => {
    if (!sucio) return;
    const alSalir = (e: BeforeUnloadEvent) => { e.preventDefault(); };
    window.addEventListener('beforeunload', alSalir);
    return () => window.removeEventListener('beforeunload', alSalir);
  }, [sucio]);
}

/**
 * Pregunta antes de tirar lo escrito en un editor.
 *
 * @param props.texto qué pasa si se descarta; por defecto, al abrir otro
 */
export function DialogoDescartar({ abierto, alDescartar, alSeguir, texto }: {
  abierto: boolean; alDescartar: () => void; alSeguir: () => void; texto?: string;
}) {
  return (
    <Dialogo abierto={abierto} titulo="¿Descartar los cambios?" alCerrar={alSeguir}>
      <p className="dialogo__texto">{texto ?? 'Hay cambios sin guardar en el editor. Si abres otro, se pierden.'}</p>
      <div className="dialogo__acciones">
        <button type="button" className="boton" onClick={alSeguir}>Seguir editando</button>
        <button type="button" className="boton boton--peligro" onClick={alDescartar}>Descartar</button>
      </div>
    </Dialogo>
  );
}

/**
 * Con cambios sin guardar, salir de la pantalla por la barra lateral, por las
 * pestañas o por cualquier enlace pregunta antes, con el mismo diálogo que al elegir
 * otra fila. Cerrar o recargar la pestaña lo pregunta el navegador (useAvisoAlSalir).
 *
 * @param sucio si hay algo escrito que no se ha guardado
 * @return el diálogo, para pintarlo en la pantalla
 */
export function useBloqueoCambios(sucio: boolean): ReactNode {
  useAvisoAlSalir(sucio);
  const bloqueo = useBlocker(({ currentLocation, nextLocation }) => sucio && currentLocation.pathname !== nextLocation.pathname);
  return (
    <DialogoDescartar abierto={bloqueo.state === 'blocked'} texto="Hay cambios sin guardar en el editor. Si sales, se pierden."
                      alSeguir={() => bloqueo.reset?.()} alDescartar={() => bloqueo.proceed?.()} />
  );
}
