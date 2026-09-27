// ============================================================
// Dialogo — ventana modal sobre <dialog> (GP-085)
// El navegador ya atrapa el foco, lo devuelve al cerrar y cierra con Escape.
// ============================================================
import { useEffect, useId, useRef, type ReactNode } from 'react';

/**
 * @param props.abierto   si se enseña
 * @param props.titulo    título (h2), que da nombre al diálogo
 * @param props.alCerrar  al cerrar con Escape o con un botón de cancelar
 */
export function Dialogo({ abierto, titulo, alCerrar, children }: {
  abierto: boolean; titulo: string; alCerrar: () => void; children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  // Un id por diálogo: la ficha de una cuenta tiene dos, y un id repetido le da a
  // uno el título del otro.
  const idTitulo = useId();

  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (abierto && !d.open) {
      // jsdom no implementa showModal; en un navegador siempre existe.
      if (typeof d.showModal === 'function') d.showModal(); else d.setAttribute('open', '');
    }
    if (!abierto && d.open) {
      if (typeof d.close === 'function') d.close(); else d.removeAttribute('open');
    }
  }, [abierto]);

  return (
    <dialog ref={ref} className="dialogo" aria-labelledby={idTitulo}
            onCancel={(e) => { e.preventDefault(); alCerrar(); }}>
      {abierto && (
        <div className="dialogo__cuerpo">
          <h2 id={idTitulo} className="dialogo__titulo">{titulo}</h2>
          {children}
        </div>
      )}
    </dialog>
  );
}
