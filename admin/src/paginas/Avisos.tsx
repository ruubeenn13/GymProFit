// ============================================================
// Avisos (lote 1.6.1)
// Lo que la gente reporta de un alimento desde la app: un aviso por alimento y motivo,
// los más repetidos primero, sin saber quién (la API no lo guarda). Al elegir uno se
// abre el alimento con el editor de siempre (el de Alimentos) y «Resuelto» lo quita de
// la lista. Un producto que nadie ha elegido aún no está en el catálogo y no se edita:
// se reconoce por su código y se resuelve igual.
// Como en Alimentos: lista y editor juntos desde 1440 px; por debajo, uno cada vez.
// ============================================================
import { useCallback, useEffect, useState } from 'react';
import { admin, type AvisoAlimento, type MotivoAviso } from '../api/admin';
import { Icono } from '../componentes/Icono';
import { Marco } from '../componentes/Marco';
import { AvisoFlotante, DialogoDescartar, EstadoLista, Paginacion, POR_PAGINA, useBloqueoCambios, type Aviso } from '../componentes/Piezas';
import { cuenta, haceCuanto } from '../util/formato';
import { ANCHO, useMedia } from '../util/useMedia';
import { textoError, useCarga } from '../util/useCarga';
import { Editor } from './Alimentos';

/** Los motivos, como los ve quien reporta en la app. */
export const MOTIVOS: Record<MotivoAviso, string> = {
  VALORES: 'Los valores no cuadran con la etiqueta',
  NOMBRE: 'El nombre o la marca',
  RACION: 'La ración o el envase',
  REPETIDO: 'Está repetido',
  OTRO: 'Otra cosa',
};

function titulo(a: AvisoAlimento): string {
  return a.alimento?.nombre ?? a.nombre ?? `Código ${a.barcode}`;
}

function Detalle({ aviso, categorias, alResuelto, alGuardado, avisar, alCambiarSucio, enfocar }: {
  aviso: AvisoAlimento; categorias: string[]; enfocar: boolean;
  alResuelto: (a: AvisoAlimento) => Promise<void>; alGuardado: () => void;
  avisar: (a: Aviso) => void; alCambiarSucio: (sucio: boolean) => void;
}) {
  const [resolviendo, setResolviendo] = useState(false);

  async function resolver() {
    setResolviendo(true);
    try {
      await alResuelto(aviso);
    } finally {
      setResolviendo(false);
    }
  }

  return (
    <div className="aviso-detalle">
      <div className="aviso-detalle__cabeza">
        <p className="aviso-detalle__motivo">
          <Icono nombre="warning" tamano={20} className="nota__icono--aviso" />
          <span>{MOTIVOS[aviso.motivo]} · {cuenta(aviso.veces, 'vez', 'veces')}</span>
        </p>
        <button type="button" className="boton boton--principal" disabled={resolviendo} onClick={() => void resolver()}>
          <Icono nombre="check" tamano={20} />Resuelto
        </button>
      </div>
      {aviso.alimento ? (
        <Editor key={aviso.alimento.id} alimento={aviso.alimento} categorias={categorias} alGuardado={alGuardado}
                avisar={avisar} alCambiarSucio={alCambiarSucio} enfocar={enfocar} />
      ) : (
        <div className="editor editor--vacio">
          <h2 tabIndex={-1}>{titulo(aviso)}</h2>
          <p>
            Producto de Open Food Facts que nadie ha elegido todavía: no está en el catálogo y no se puede editar aquí.
            {aviso.barcode && <> Código <span className="campo__control--mono">{aviso.barcode}</span>.</>}
            {aviso.marca && <> Marca: {aviso.marca}.</>} Se corrige en Open Food Facts y llega con la importación semanal.
          </p>
        </div>
      )}
    </div>
  );
}

export function Avisos() {
  const [pagina, setPagina] = useState(0);
  const [elegido, setElegido] = useState<AvisoAlimento | null>(null);
  const [sucio, setSucio] = useState(false);
  const [pendiente, setPendiente] = useState<{ aviso: AvisoAlimento | null } | null>(null);
  const [enfocar, setEnfocar] = useState(false);
  const [aviso, setAviso] = useState<Aviso | null>(null);
  const dos = useMedia(ANCHO.dosColumnas);
  const movil = useMedia(ANCHO.movil);
  const avisoSalir = useBloqueoCambios(sucio);

  const resumen = useCarga((s) => admin.resumenAlimentos(s), []);
  const lista = useCarga((s) => admin.avisos({ page: pagina, size: POR_PAGINA }, s), [pagina]);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    if (!lista.datos) return;
    setElegido((actual) => (actual
      ? lista.datos!.content.find((a) => a.id === actual.id) ?? (dos ? lista.datos!.content[0] ?? null : null)
      : dos ? lista.datos!.content[0] ?? null : null));
  }, [lista.datos, dos]);

  function abrir(a: AvisoAlimento | null) {
    setEnfocar(!dos);
    setElegido(a);
  }

  function elegir(a: AvisoAlimento | null) {
    if ((a?.id ?? null) === (elegido?.id ?? null)) return;
    if (sucio) setPendiente({ aviso: a }); else abrir(a);
  }

  async function resolver(a: AvisoAlimento) {
    try {
      await admin.resolverAviso(a.id);
      setSucio(false);
      setAviso({ tipo: 'ok', texto: `Resuelto: ${titulo(a)}.` });
      if (!dos) setElegido(null);
      lista.recargar();
    } catch (err) {
      setAviso({ tipo: 'error', texto: textoError(err) });
    }
  }

  const total = lista.datos?.totalElements ?? 0;
  const avisos = lista.datos?.content ?? [];
  const verLista = dos || elegido === null;

  return (
    <Marco titulo="Avisos" subtitulo={lista.datos ? `${cuenta(total, 'aviso pendiente', 'avisos pendientes')} de alimentos` : undefined}>
      <div className={`reparto${dos ? ' reparto--dos' : ''}`}>
        {verLista && (
          <section className="lista" aria-label="Avisos pendientes">
            <div className={movil ? 'tarjetas-marco' : 'tarjeta tabla-marco'}>
              <EstadoLista cargando={lista.cargando} error={lista.error} vacio={total === 0}
                           textoVacio="No hay avisos pendientes." alReintentar={lista.recargar}>
                <ul className="tarjetas" aria-label="Avisos pendientes, los más repetidos primero">
                  {avisos.map((a) => (
                    <li key={a.id}>
                      <button type="button" className="tarjeta-fila" data-fila={a.id}
                              aria-current={elegido?.id === a.id ? 'true' : undefined} onClick={() => elegir(a)}>
                        <span className="tarjeta-fila__texto">
                          <span className="fila-boton__principal">{titulo(a)}</span>
                          <span className="fila-boton__secundario">{MOTIVOS[a.motivo]} · {haceCuanto(a.actualizado)}</span>
                        </span>
                        <span className="etiqueta">{cuenta(a.veces, 'vez', 'veces')}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </EstadoLista>
            </div>
            {total > 0 && <Paginacion pagina={pagina} tamano={POR_PAGINA} total={total} alCambiar={setPagina} />}
            <span className="lista__nota">Los avisos no guardan quién los envió.</span>
          </section>
        )}
        {elegido ? (
          <div className="reparto__editor">
            {!dos && (
              <button type="button" className="boton boton--volver" onClick={() => elegir(null)}>
                <Icono nombre="chevron_left" tamano={20} />Volver a la lista
              </button>
            )}
            <Detalle key={elegido.id} aviso={elegido} categorias={resumen.datos?.categorias ?? []} enfocar={enfocar}
                     alResuelto={resolver} alGuardado={lista.recargar} avisar={setAviso} alCambiarSucio={setSucio} />
          </div>
        ) : dos && total > 0 && <div className="editor editor--vacio"><p>Elige un aviso de la lista.</p></div>}
      </div>
      <DialogoDescartar abierto={pendiente !== null} alSeguir={() => setPendiente(null)}
                        alDescartar={() => { setSucio(false); abrir(pendiente!.aviso); setPendiente(null); }} />
      {avisoSalir}
      <AvisoFlotante aviso={aviso} alCerrar={cerrarAviso} />
    </Marco>
  );
}
