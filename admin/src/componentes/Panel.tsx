// ============================================================
// Panel — la ficha de una cuenta, encima de la tabla (GP-120)
// Un <dialog> modal pegado a la derecha (a pantalla completa en el móvil): el foco
// entra en él al abrir, Esc o su botón lo cierran, y al cerrar vuelve a la fila de
// la que salió. Los diálogos que abre dentro (rol, borrado) cierran solo el suyo.
// ============================================================
import { useEffect, useRef, type KeyboardEvent, type ReactNode, type SyntheticEvent } from 'react';

/**
 * @param props.abierto   si se enseña
 * @param props.etiqueta  nombre accesible del panel, p. ej. «Cuenta @ana»
 * @param props.alCerrar  al pedir cerrarlo (Esc o su botón)
 * @param props.volverA   a quién devolver el foco al cerrar; si no, al que lo tenía al abrir
 */
export function Panel({ abierto, etiqueta, alCerrar, volverA, children }: {
  abierto: boolean; etiqueta: string; alCerrar: () => void; volverA?: () => HTMLElement | null; children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const origen = useRef<HTMLElement | null>(null);
  const volver = useRef(volverA);
  volver.current = volverA;

  useEffect(() => {
    const d = ref.current;
    if (!d || !abierto) return;
    origen.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    // jsdom no implementa showModal; en un navegador siempre existe.
    if (!d.open) {
      if (typeof d.showModal === 'function') d.showModal(); else d.setAttribute('open', '');
    }
    d.focus();
    return () => {
      if (d.open) {
        if (typeof d.close === 'function') d.close(); else d.removeAttribute('open');
      }
      const destino = volver.current?.() ?? origen.current;
      destino?.focus();
    };
  }, [abierto]);

  // Esc: el navegador ya lanza «cancel», pero un Esc dentro de un diálogo hijo es de él.
  function alTeclear(e: KeyboardEvent<HTMLDialogElement>) {
    if (e.key !== 'Escape' || (e.target as Element).closest('dialog') !== ref.current) return;
    e.preventDefault();
    alCerrar();
  }

  // Chrome agrupa los diálogos abiertos uno dentro de otro y un solo Esc lanza
  // «cancel» en los dos. Si hay un diálogo hijo abierto, ese Esc era para él.
  function alCancelar(e: SyntheticEvent<HTMLDialogElement>) {
    e.preventDefault();
    if (ref.current?.querySelector('dialog[open]')) return;
    alCerrar();
  }

  if (!abierto) return null;
  return (
    <dialog ref={ref} className="panel" aria-label={etiqueta} tabIndex={-1} onKeyDown={alTeclear}
            onCancel={alCancelar}>
      <div className="panel__cuerpo">{children}</div>
    </dialog>
  );
}
