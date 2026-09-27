// ============================================================
// Ejercicios (GP-085)
// Sobre todo para traducir el catálogo: lista con búsqueda en los dos idiomas,
// «sin revisar», grupo y equipamiento; y el editor, con «Guardar y siguiente sin
// revisar» y la marca «se dice igual en español». Un nombre en español distinto
// del inglés cuenta como revisado al guardar (lo decide la API).
// ============================================================
import { useCallback, useEffect, useId, useState, type FormEvent, type ReactNode } from 'react';
import { admin, type EjercicioDetalle, type EjercicioGuardar, type ResumenEjercicios } from '../api/admin';
import { Icono } from '../componentes/Icono';
import { Marco } from '../componentes/Marco';
import { AvisoFlotante, Buscador, EstadoLista, Filtro, Interruptor, Paginacion, type Aviso } from '../componentes/Piezas';
import { cuenta, entero } from '../util/formato';
import { textoError, useCarga } from '../util/useCarga';
import { DIFICULTADES, GRUPOS, ORIGEN_EJERCICIO } from './etiquetas';

const TAMANO = 8;

type Formulario = Omit<EjercicioGuardar, 'nombreEn' | 'descripcion' | 'descripcionEn' | 'instrucciones' | 'instruccionesEn'
  | 'musculoPrimario' | 'musculoPrimarioEn'> & {
  nombreEn: string; descripcion: string; descripcionEn: string; instrucciones: string; instruccionesEn: string;
  musculoPrimario: string; musculoPrimarioEn: string;
};

function aFormulario(e: EjercicioDetalle): Formulario {
  return {
    nombre: e.nombre, nombreEn: e.nombreEn ?? '', descripcion: e.descripcion ?? '', descripcionEn: e.descripcionEn ?? '',
    instrucciones: e.instrucciones ?? '', instruccionesEn: e.instruccionesEn ?? '', grupoMuscular: e.grupoMuscular,
    musculoPrimario: e.musculoPrimario ?? '', musculoPrimarioEn: e.musculoPrimarioEn ?? '', equipamiento: e.equipamiento,
    dificultad: e.dificultad, activo: e.activo, nombreRevisado: e.nombreRevisado,
  };
}

const nulo = (t: string) => (t.trim() === '' ? null : t.trim());

function Desplegable({ titulo, estado, children }: { titulo: string; estado?: string; children: ReactNode }) {
  const [abierto, setAbierto] = useState(false);
  const id = useId();
  return (
    <div className="desplegable">
      <button type="button" className="desplegable__boton" aria-expanded={abierto} aria-controls={id}
              onClick={() => setAbierto((a) => !a)}>
        <span className="desplegable__titulo"><Icono nombre="format_list_numbered" tamano={20} />{titulo}</span>
        <span className="desplegable__estado">{estado}<Icono nombre="expand_more" tamano={20} /></span>
      </button>
      <div id={id} hidden={!abierto} className="desplegable__cuerpo">{children}</div>
    </div>
  );
}

function faltaEspanol(es: string, en: string): string | undefined {
  if (!es.trim() && en.trim()) return 'falta el español';
  if (es.trim() && !en.trim()) return 'falta el inglés';
  if (!es.trim() && !en.trim()) return 'vacías';
  return undefined;
}

function Editor({ id, resumen, alGuardado, alSiguiente, avisar }: {
  id: number;
  resumen: ResumenEjercicios | null;
  alGuardado: () => void;
  alSiguiente: (actual: number) => Promise<void>;
  avisar: (a: Aviso) => void;
}) {
  const { datos, cargando, error, recargar } = useCarga<EjercicioDetalle>((s) => admin.ejercicio(id, s), [id]);
  const [f, setF] = useState<Formulario | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [errorNombre, setErrorNombre] = useState<string | null>(null);

  useEffect(() => {
    if (datos) setF(aFormulario(datos));
  }, [datos]);

  const cambiar = <K extends keyof Formulario>(clave: K, valor: Formulario[K]) => setF((x) => (x ? { ...x, [clave]: valor } : x));

  async function guardar(siguiente: boolean) {
    if (!f) return;
    if (!f.nombre.trim()) {
      setErrorNombre('El nombre en español no puede quedar vacío.');
      return;
    }
    setErrorNombre(null);
    setGuardando(true);
    try {
      const guardado = await admin.guardarEjercicio(id, {
        ...f, nombre: f.nombre.trim(), nombreEn: nulo(f.nombreEn), descripcion: nulo(f.descripcion),
        descripcionEn: nulo(f.descripcionEn), instrucciones: nulo(f.instrucciones), instruccionesEn: nulo(f.instruccionesEn),
        musculoPrimario: nulo(f.musculoPrimario), musculoPrimarioEn: nulo(f.musculoPrimarioEn),
      });
      alGuardado();
      if (siguiente) {
        await alSiguiente(id);
      } else {
        setF(aFormulario(guardado));
        avisar({ tipo: 'ok', texto: guardado.nombreRevisado ? 'Guardado. Queda revisado.' : 'Guardado.' });
      }
    } catch (err) {
      avisar({ tipo: 'error', texto: textoError(err) });
    } finally {
      setGuardando(false);
    }
  }

  function alEnviar(e: FormEvent) {
    e.preventDefault();
    void guardar(false);
  }

  const mismoNombre = !!f && f.nombre.trim().toLowerCase() === f.nombreEn.trim().toLowerCase();
  // Lo que quedará al guardar: revisado si se marca o si los nombres ya son distintos.
  const quedaraRevisado = !!f && (f.nombreRevisado || !mismoNombre);

  return (
    <form className="tarjeta editor" onSubmit={alEnviar} aria-label={datos ? `Editar ${datos.nombre}` : 'Editor de ejercicio'} noValidate>
      <EstadoLista cargando={cargando && !datos} error={error} vacio={false} textoVacio="" alReintentar={recargar}>
        {datos && f && (
          <>
            <div className="editor__cabeza">
              <div>
                <h2>{datos.nombre}</h2>
                <span className="editor__origen">
                  {ORIGEN_EJERCICIO[datos.origen]} · {datos.rutinas === 0 ? 'no está en ninguna rutina' : `se usa en ${cuenta(datos.rutinas, 'rutina', 'rutinas')}`}
                </span>
              </div>
              {quedaraRevisado
                ? <span className="etiqueta etiqueta--ok">Revisado</span>
                : <span className="etiqueta etiqueta--aviso">Sin revisar</span>}
            </div>

            <div className="rejilla rejilla--2">
              <div className="campo">
                <label htmlFor="ej-nombre" className="campo__etiqueta">Nombre en español</label>
                <input id="ej-nombre" className={`campo__control${!quedaraRevisado ? ' campo__control--pendiente' : ''}`}
                       value={f.nombre} maxLength={100} onChange={(e) => cambiar('nombre', e.target.value)}
                       aria-invalid={!!errorNombre} aria-describedby={errorNombre ? 'ej-nombre-error' : undefined} />
                {errorNombre && <span id="ej-nombre-error" className="campo__error">{errorNombre}</span>}
              </div>
              <div className="campo">
                <label htmlFor="ej-nombre-en" className="campo__etiqueta">Nombre en inglés</label>
                <input id="ej-nombre-en" className="campo__control" value={f.nombreEn} maxLength={100}
                       onChange={(e) => cambiar('nombreEn', e.target.value)} />
              </div>
            </div>

            <label className="casilla">
              <input type="checkbox" checked={f.nombreRevisado && mismoNombre} disabled={!mismoNombre}
                     onChange={(e) => cambiar('nombreRevisado', e.target.checked)} />
              <span>En el gimnasio se dice igual en español (como «hip thrust»): se queda así y cuenta como revisado</span>
            </label>

            <div className="rejilla rejilla--2">
              <div className="campo">
                <label htmlFor="ej-desc" className="campo__etiqueta">Descripción en español</label>
                <textarea id="ej-desc" className="campo__control" rows={3} placeholder="Falta la descripción en español"
                          value={f.descripcion} onChange={(e) => cambiar('descripcion', e.target.value)} />
              </div>
              <div className="campo">
                <label htmlFor="ej-desc-en" className="campo__etiqueta">Descripción en inglés</label>
                <textarea id="ej-desc-en" className="campo__control" rows={3} placeholder="Falta la descripción en inglés"
                          value={f.descripcionEn} onChange={(e) => cambiar('descripcionEn', e.target.value)} />
              </div>
            </div>

            <Desplegable titulo="Instrucciones paso a paso" estado={faltaEspanol(f.instrucciones, f.instruccionesEn)}>
              <div className="rejilla rejilla--2">
                <div className="campo">
                  <label htmlFor="ej-ins" className="campo__etiqueta">Instrucciones en español</label>
                  <textarea id="ej-ins" className="campo__control" rows={6} value={f.instrucciones}
                            onChange={(e) => cambiar('instrucciones', e.target.value)} />
                </div>
                <div className="campo">
                  <label htmlFor="ej-ins-en" className="campo__etiqueta">Instrucciones en inglés</label>
                  <textarea id="ej-ins-en" className="campo__control" rows={6} value={f.instruccionesEn}
                            onChange={(e) => cambiar('instruccionesEn', e.target.value)} />
                </div>
              </div>
              <div className="rejilla rejilla--2">
                <div className="campo">
                  <label htmlFor="ej-musc" className="campo__etiqueta">Músculo principal en español</label>
                  <input id="ej-musc" className="campo__control" maxLength={60} value={f.musculoPrimario}
                         onChange={(e) => cambiar('musculoPrimario', e.target.value)} />
                </div>
                <div className="campo">
                  <label htmlFor="ej-musc-en" className="campo__etiqueta">Músculo principal en inglés</label>
                  <input id="ej-musc-en" className="campo__control" maxLength={60} value={f.musculoPrimarioEn}
                         onChange={(e) => cambiar('musculoPrimarioEn', e.target.value)} />
                </div>
              </div>
            </Desplegable>

            <div className="rejilla rejilla--3">
              <div className="campo">
                <label htmlFor="ej-grupo" className="campo__etiqueta">Grupo muscular</label>
                <select id="ej-grupo" className="campo__control" value={f.grupoMuscular} onChange={(e) => cambiar('grupoMuscular', e.target.value)}>
                  {Object.entries(GRUPOS).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                </select>
              </div>
              <div className="campo">
                <label htmlFor="ej-equipo" className="campo__etiqueta">Equipamiento</label>
                <select id="ej-equipo" className="campo__control" value={f.equipamiento} onChange={(e) => cambiar('equipamiento', e.target.value)}
                        aria-describedby={datos.equipoNecesario ? 'ej-equipo-origen' : undefined}>
                  {(resumen?.equipamientos ?? [{ valor: f.equipamiento, etiqueta: f.equipamiento, etiquetaEn: '' }])
                    .map((o) => <option key={o.valor} value={o.valor}>{o.etiqueta}</option>)}
                </select>
                {datos.equipoNecesario && <span id="ej-equipo-origen" className="campo__ayuda">Importado como «{datos.equipoNecesario}»</span>}
              </div>
              <div className="campo">
                <label htmlFor="ej-dif" className="campo__etiqueta">Dificultad</label>
                <select id="ej-dif" className="campo__control" value={f.dificultad} onChange={(e) => cambiar('dificultad', e.target.value)}>
                  {Object.entries(DIFICULTADES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                </select>
              </div>
            </div>

            <div className="editor__fila">
              {[datos.imagenUrl, datos.imagenUrl2].map((url, i) => (
                url
                  ? <img key={i} className="miniatura" src={url} alt={`Imagen ${i + 1} de ${datos.nombre}`} loading="lazy" />
                  : <span key={i} className="miniatura miniatura--vacia" role="img" aria-label={`Sin imagen ${i + 1}`}>
                      <Icono nombre="image" /></span>
              ))}
              <div className="editor__visible">
                <Interruptor texto="Visible en la app" activo={f.activo} alCambiar={(v) => cambiar('activo', v)} />
              </div>
            </div>

            <div className="editor__pie">
              <button type="submit" className="boton" disabled={guardando}>Guardar</button>
              <button type="button" className="boton boton--principal" disabled={guardando} onClick={() => void guardar(true)}>
                Guardar y siguiente sin revisar<Icono nombre="arrow_forward" tamano={20} />
              </button>
            </div>
          </>
        )}
      </EstadoLista>
    </form>
  );
}

export function Ejercicios() {
  const [q, setQ] = useState('');
  const [sinRevisar, setSinRevisar] = useState(true);
  const [grupo, setGrupo] = useState('');
  const [equipamiento, setEquipamiento] = useState('');
  const [pagina, setPagina] = useState(0);
  const [elegido, setElegido] = useState<number | null>(null);
  const [aviso, setAviso] = useState<Aviso | null>(null);

  const resumen = useCarga((s) => admin.resumenEjercicios(s), []);
  const lista = useCarga((s) => admin.ejercicios({ q, grupo, equipamiento, sinRevisar, page: pagina, size: TAMANO }, s),
    [q, grupo, equipamiento, sinRevisar, pagina]);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    // Al abrir la pantalla, el primero de la lista queda elegido: es por donde se empieza.
    if (elegido === null && lista.datos?.content.length) setElegido(lista.datos.content[0].id);
  }, [lista.datos, elegido]);

  function filtrar(cambio: () => void) {
    cambio();
    setPagina(0);
  }

  function alGuardado() {
    lista.recargar();
    resumen.recargar();
  }

  async function siguiente(actual: number) {
    try {
      const pendientes = await admin.ejercicios({ sinRevisar: true, page: 0, size: 100 });
      const otro = pendientes.content.find((e) => e.id !== actual);
      if (otro) {
        setElegido(otro.id);
        setAviso({ tipo: 'ok', texto: 'Guardado. Vamos con el siguiente sin revisar.' });
      } else {
        setAviso({ tipo: 'ok', texto: 'Guardado. No queda ninguno sin revisar.' });
      }
    } catch (err) {
      setAviso({ tipo: 'error', texto: `Guardado, pero no se ha podido buscar el siguiente: ${textoError(err)}` });
    }
  }

  const total = lista.datos?.totalElements ?? 0;
  const r = resumen.datos;

  return (
    <Marco titulo="Ejercicios" subtitulo={r ? `${entero(r.activos)} activos · ${entero(r.sinRevisar)} sin revisar` : undefined}>
      <div className="dos-columnas dos-columnas--ejercicios">
        <section className="lista" aria-label="Lista de ejercicios">
          <Buscador etiqueta="Buscar ejercicios" marcador="Busca por nombre, en español o en inglés" alBuscar={(t) => filtrar(() => setQ(t))} />
          <div className="lista__filtros lista__filtros--envuelve">
            <button type="button" className="chip" aria-pressed={sinRevisar} onClick={() => filtrar(() => setSinRevisar((v) => !v))}>
              {sinRevisar && <Icono nombre="check" tamano={18} />}Sin revisar{r ? ` · ${entero(r.sinRevisar)}` : ''}
            </button>
            <Filtro etiqueta="Grupo" valor={grupo} alCambiar={(v) => filtrar(() => setGrupo(v))}
                    opciones={[{ valor: '', texto: 'todos' }, ...Object.entries(GRUPOS).map(([v, t]) => ({ valor: v, texto: t.toLowerCase() }))]} />
            <Filtro etiqueta="Equipamiento" valor={equipamiento} alCambiar={(v) => filtrar(() => setEquipamiento(v))}
                    opciones={[{ valor: '', texto: 'todo' }, ...(r?.equipamientos ?? []).map((o) => ({ valor: o.valor, texto: o.etiqueta.toLowerCase() }))]} />
          </div>
          <div className="tarjeta tabla-marco">
            <EstadoLista cargando={lista.cargando} error={lista.error} vacio={total === 0}
                         textoVacio={sinRevisar ? 'No queda ningún ejercicio sin revisar con estos filtros.' : 'Ningún ejercicio con estos filtros.'}
                         alReintentar={lista.recargar}>
              <table className="tabla tabla--ejercicios">
                <caption className="solo-lector">{sinRevisar ? 'Ejercicios sin revisar' : 'Ejercicios'}, por nombre</caption>
                <thead><tr><th scope="col">Nombre · español / inglés</th><th scope="col">Grupo</th><th scope="col">Equipamiento</th></tr></thead>
                <tbody>
                  {lista.datos?.content.map((e) => (
                    <tr key={e.id} aria-selected={elegido === e.id} onClick={() => setElegido(e.id)}>
                      <td>
                        <button type="button" className="fila-boton" onClick={(ev) => { ev.stopPropagation(); setElegido(e.id); }}>
                          <span className="fila-boton__principal">{e.nombre}</span>
                          <span className="fila-boton__secundario">{e.nombreEn ?? 'Sin nombre en inglés'}</span>
                        </button>
                      </td>
                      <td>{GRUPOS[e.grupoMuscular] ?? e.grupoMuscular}</td>
                      <td>{r?.equipamientos.find((o) => o.valor === e.equipamiento)?.etiqueta ?? e.equipamiento}
                        {!e.activo && <span className="solo-lector"> (oculto en la app)</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </EstadoLista>
          </div>
          {total > 0 && <Paginacion pagina={pagina} tamano={TAMANO} total={total} alCambiar={setPagina} />}
        </section>
        {elegido !== null
          ? <Editor key={elegido} id={elegido} resumen={r} alGuardado={alGuardado} alSiguiente={siguiente} avisar={setAviso} />
          : <div className="tarjeta editor editor--vacio"><p>Elige un ejercicio de la lista para editarlo.</p></div>}
      </div>
      <AvisoFlotante aviso={aviso} alCerrar={cerrarAviso} />
    </Marco>
  );
}
