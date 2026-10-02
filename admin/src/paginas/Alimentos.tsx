// ============================================================
// Alimentos (GP-085)
// Solo el catálogo: los alimentos que crea cada persona son su dieta y la API no
// los devuelve aquí. Editor con aviso si las calorías no cuadran con los macros
// (util/calorias.ts). Se guarda con PATCH /alimentos/{id}, la ruta de siempre.
// Lista y editor lado a lado solo desde 1440 px; por debajo, el editor ocupa la
// pantalla, con «Volver a la lista» (GP-120).
// ============================================================
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { admin, type Alimento, type AlimentoCambios } from '../api/admin';
import { Icono } from '../componentes/Icono';
import { Marco } from '../componentes/Marco';
import { AvisoFlotante, Buscador, DialogoDescartar, EstadoLista, Filtro, Interruptor, Paginacion, POR_PAGINA, useBloqueoCambios, type Aviso } from '../componentes/Piezas';
import { comprobarCalorias } from '../util/calorias';
import { decimal1, entero, leerNumero } from '../util/formato';
import { ANCHO, useMedia } from '../util/useMedia';
import { textoError, useCarga } from '../util/useCarga';


// De dónde sale cada alimento (GP-127). Sin fuente, se mira el origen de antes.
function procedencia(a: Alimento): string {
  switch (a.fuente) {
    case 'CIQUAL': return 'Básico de Ciqual (ANSES), revisado';
    case 'USDA': return 'Básico de USDA FoodData Central, revisado';
    case 'OFF': return 'Producto de Open Food Facts, sin revisar';
    default: return a.origen === 'OPEN_FOOD_FACTS' ? 'Importado de Open Food Facts al escanear su código'
      : 'Añadido a mano al catálogo';
  }
}

const FUENTES = [
  { valor: '', texto: 'todas' },
  { valor: 'CIQUAL', texto: 'Ciqual' },
  { valor: 'USDA', texto: 'USDA' },
  { valor: 'OFF', texto: 'Open Food Facts' },
  { valor: 'MANUAL', texto: 'a mano' },
];

interface Formulario {
  nombre: string; nombreEn: string; marca: string; categoria: string; barcode: string;
  calorias: string; proteinas: string; carbohidratos: string; grasas: string; fibra: string; porcionGramos: string;
  activo: boolean;
}

const texto = (n: number | null) => (n === null || n === undefined ? '' : decimal1(n));

function aFormulario(a: Alimento): Formulario {
  return {
    nombre: a.nombre, nombreEn: a.nombreEn ?? '', marca: a.marca ?? '', categoria: a.categoria ?? '', barcode: a.barcode ?? '',
    calorias: String(a.calorias ?? ''), proteinas: texto(a.proteinas), carbohidratos: texto(a.carbohidratos),
    grasas: texto(a.grasas), fibra: texto(a.fibra), porcionGramos: a.porcionGramos === null ? '' : String(a.porcionGramos),
    activo: a.activo,
  };
}

const NUMEROS: { clave: keyof Formulario; etiqueta: string; entero?: boolean }[] = [
  { clave: 'calorias', etiqueta: 'kcal', entero: true },
  { clave: 'proteinas', etiqueta: 'Proteínas (g)' },
  { clave: 'carbohidratos', etiqueta: 'Carbos (g)' },
  { clave: 'grasas', etiqueta: 'Grasas (g)' },
  { clave: 'fibra', etiqueta: 'Fibra (g)' },
];

/** Errores de los números: vacío en kcal, texto que no es número, negativos o decimales en kcal. */
function erroresNumeros(f: Formulario): Partial<Record<keyof Formulario, string>> {
  const errores: Partial<Record<keyof Formulario, string>> = {};
  for (const { clave, entero: soloEntero } of [...NUMEROS, { clave: 'porcionGramos' as const, etiqueta: '', entero: true }]) {
    const n = leerNumero(f[clave] as string);
    if (n === null) { if (clave === 'calorias') errores[clave] = 'Hacen falta las calorías.'; continue; }
    if (Number.isNaN(n)) errores[clave] = 'Tiene que ser un número.';
    else if (n < 0) errores[clave] = 'No puede ser negativo.';
    else if (soloEntero && !Number.isInteger(n)) errores[clave] = 'Sin decimales.';
  }
  return errores;
}

function AvisoCalorias({ f }: { f: Formulario }) {
  const num = (t: string) => { const n = leerNumero(t); return n === null || Number.isNaN(n) ? null : n; };
  const [p, c, g] = [num(f.proteinas), num(f.carbohidratos), num(f.grasas)];
  const r = comprobarCalorias(num(f.calorias), p, c, g);
  if (!r) return null;
  const cuenta = `${decimal1(p ?? 0)} × 4 + ${decimal1(c ?? 0)} × 4 + ${decimal1(g ?? 0)} × 9 = ${decimal1(r.calculadas)} kcal`;
  return (
    <p className={`nota ${r.seAleja ? 'nota--aviso' : ''}`} role="status" aria-live="polite">
      <Icono nombre={r.seAleja ? 'warning' : 'check_circle'} tamano={20} className={r.seAleja ? 'nota__icono--aviso' : 'nota__icono--ok'} />
      {r.seAleja
        ? `No cuadra: ${cuenta}, frente a las ${entero(num(f.calorias)!)} declaradas. Revisa si hay una errata.`
        : `Cuadra: ${cuenta}, frente a las ${entero(num(f.calorias)!)} declaradas.`}
    </p>
  );
}

/**
 * El editor de un alimento del catálogo. También lo usa Avisos (lote 1.6.1), sin
 * «siguiente sin inglés».
 *
 * @param props.alSiguiente si llega, el botón «Guardar y siguiente sin inglés».
 */
export function Editor({ alimento, categorias, alGuardado, alSiguiente, avisar, alCambiarSucio, enfocar }: {
  alimento: Alimento; categorias: string[]; alCambiarSucio: (sucio: boolean) => void;
  /** Poner el foco en el título al abrirlo: a pantalla completa o al pasar al siguiente. */
  enfocar: boolean;
  alGuardado: () => void; alSiguiente?: (actual: number) => Promise<void>; avisar: (a: Aviso) => void;
}) {
  const [f, setF] = useState<Formulario>(() => aFormulario(alimento));
  const [errores, setErrores] = useState<Partial<Record<keyof Formulario, string>>>({});
  const [guardando, setGuardando] = useState(false);
  const titulo = useRef<HTMLHeadingElement>(null);

  useEffect(() => { setF(aFormulario(alimento)); setErrores({}); }, [alimento]);
  useEffect(() => { if (enfocar) titulo.current?.focus(); }, [enfocar]);

  const sucio = JSON.stringify(f) !== JSON.stringify(aFormulario(alimento));
  useEffect(() => { alCambiarSucio(sucio); }, [sucio, alCambiarSucio]);
  // Al cerrarse el editor (otra fila, «siguiente»), sus cambios dejan de contar.
  useEffect(() => () => alCambiarSucio(false), [alCambiarSucio]);

  const cambiar = <K extends keyof Formulario>(clave: K, valor: Formulario[K]) => setF((x) => ({ ...x, [clave]: valor }));

  async function guardar(siguiente: boolean) {
    const e = erroresNumeros(f);
    if (!f.nombre.trim()) e.nombre = 'El nombre en español no puede quedar vacío.';
    setErrores(e);
    if (Object.keys(e).length) return;

    const cambios: AlimentoCambios = {
      nombre: f.nombre.trim(), nombreEn: f.nombreEn.trim(), marca: f.marca.trim(), barcode: f.barcode.trim(), activo: f.activo,
      calorias: leerNumero(f.calorias)!,
    };
    if (f.categoria) cambios.categoria = f.categoria;
    for (const clave of ['proteinas', 'carbohidratos', 'grasas', 'fibra', 'porcionGramos'] as const) {
      const n = leerNumero(f[clave]);
      if (n !== null) cambios[clave] = n;
    }
    setGuardando(true);
    try {
      await admin.guardarAlimento(alimento.id, cambios);
      alGuardado();
      if (siguiente && alSiguiente) await alSiguiente(alimento.id);
      else avisar({ tipo: 'ok', texto: 'Guardado.' });
    } catch (err) {
      avisar({ tipo: 'error', texto: textoError(err) });
    } finally {
      setGuardando(false);
    }
  }

  function alEnviar(ev: FormEvent) {
    ev.preventDefault();
    void guardar(false);
  }

  const opcionesCategoria = Array.from(new Set([...categorias, ...(alimento.categoria ? [alimento.categoria] : [])]));
  const sinIngles = !f.nombreEn.trim();

  return (
    <form className="editor" onSubmit={alEnviar} aria-label={`Editar ${alimento.nombre}`} noValidate>
      <div className="editor__cabeza">
        <div>
          <h2 ref={titulo} tabIndex={-1}>{alimento.nombre}</h2>
          <span className="editor__origen">{procedencia(alimento)}</span>
        </div>
        {sinIngles ? <span className="etiqueta etiqueta--aviso">Falta el inglés</span> : null}
      </div>

      <div className="rejilla rejilla--2">
        <div className="campo">
          <label htmlFor="al-es" className="campo__etiqueta">
            Nombre en español <span className="campo__obligatorio">(obligatorio)</span>
          </label>
          <input id="al-es" className="campo__control" aria-required="true" value={f.nombre} maxLength={100} onChange={(e) => cambiar('nombre', e.target.value)}
                 aria-invalid={!!errores.nombre} aria-describedby={errores.nombre ? 'al-es-error' : undefined} />
          {errores.nombre && <span id="al-es-error" className="campo__error">{errores.nombre}</span>}
        </div>
        <div className="campo">
          <label htmlFor="al-en" className="campo__etiqueta">Nombre en inglés</label>
          <input id="al-en" className={`campo__control${sinIngles ? ' campo__control--pendiente' : ''}`} maxLength={100}
                 placeholder="Falta el nombre en inglés" value={f.nombreEn} onChange={(e) => cambiar('nombreEn', e.target.value)} />
        </div>
      </div>

      <div className="rejilla rejilla--3">
        <div className="campo">
          <label htmlFor="al-marca" className="campo__etiqueta">Marca</label>
          <input id="al-marca" className="campo__control" placeholder="Sin marca" maxLength={100} value={f.marca}
                 onChange={(e) => cambiar('marca', e.target.value)} />
        </div>
        <div className="campo">
          <label htmlFor="al-cat" className="campo__etiqueta">Categoría</label>
          <select id="al-cat" className="campo__control" value={f.categoria} onChange={(e) => cambiar('categoria', e.target.value)}>
            {!f.categoria && <option value="">Sin categoría</option>}
            {opcionesCategoria.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <div className="campo">
          <label htmlFor="al-cod" className="campo__etiqueta">Código de barras</label>
          <input id="al-cod" className="campo__control campo__control--mono" inputMode="numeric" maxLength={32}
                 placeholder="Sin código" value={f.barcode} onChange={(e) => cambiar('barcode', e.target.value)} />
        </div>
      </div>

      <fieldset className="grupo-numeros">
        <legend className="campo__etiqueta">Por cada 100 g</legend>
        <div className="rejilla rejilla--5">
          {NUMEROS.map(({ clave, etiqueta }) => (
            <div className="campo" key={clave}>
              <label htmlFor={`al-${clave}`} className="campo__etiqueta campo__etiqueta--suave">
                {etiqueta}{clave === 'calorias' && <> <span className="campo__obligatorio">(obligatorio)</span></>}
              </label>
              <input id={`al-${clave}`} className="campo__control" inputMode="decimal" value={f[clave] as string}
                     aria-required={clave === 'calorias' ? true : undefined}
                     onChange={(e) => cambiar(clave, e.target.value as never)} aria-invalid={!!errores[clave]}
                     aria-describedby={errores[clave] ? `al-${clave}-error` : undefined} />
              {errores[clave] && <span id={`al-${clave}-error`} className="campo__error">{errores[clave]}</span>}
            </div>
          ))}
        </div>
      </fieldset>

      <AvisoCalorias f={f} />

      <div className="editor__fila">
        <div className="campo campo--estrecho">
          <label htmlFor="al-porc" className="campo__etiqueta">Ración habitual (g)</label>
          <input id="al-porc" className="campo__control" inputMode="numeric" value={f.porcionGramos}
                 onChange={(e) => cambiar('porcionGramos', e.target.value)} aria-invalid={!!errores.porcionGramos}
                 aria-describedby={errores.porcionGramos ? 'al-porc-error' : undefined} />
          {errores.porcionGramos && <span id="al-porc-error" className="campo__error">{errores.porcionGramos}</span>}
        </div>
        <div className="editor__visible editor__visible--abajo">
          <Interruptor texto="Visible en la app" activo={f.activo} alCambiar={(v) => cambiar('activo', v)} />
        </div>
      </div>

      <div className="editor__pie">
        <button type="submit" className="boton" disabled={guardando}>Guardar</button>
        {alSiguiente && (
          <button type="button" className="boton boton--principal" disabled={guardando} onClick={() => void guardar(true)}>
            Guardar y siguiente sin inglés<Icono nombre="arrow_forward" tamano={20} />
          </button>
        )}
      </div>
    </form>
  );
}

export function Alimentos() {
  const [q, setQ] = useState('');
  const [sinIngles, setSinIngles] = useState(false);
  const [categoria, setCategoria] = useState('');
  const [fuente, setFuente] = useState('');
  const [pagina, setPagina] = useState(0);
  const [elegido, setElegido] = useState<Alimento | null>(null);
  const [sucio, setSucio] = useState(false);
  // A dónde se iba cuando saltó el aviso de cambios: otra fila, o la lista (null).
  const [pendiente, setPendiente] = useState<{ alimento: Alimento | null } | null>(null);
  const [enfocar, setEnfocar] = useState(false);
  const [volverA, setVolverA] = useState<number | null>(null);
  const [aviso, setAviso] = useState<Aviso | null>(null);
  const dos = useMedia(ANCHO.dosColumnas);
  const movil = useMedia(ANCHO.movil);
  const avisoSalir = useBloqueoCambios(sucio);

  function abrir(a: Alimento | null) {
    if (a === null && elegido !== null) setVolverA(elegido.id);
    setEnfocar(!dos);
    setElegido(a);
  }

  // Elegir otra fila, o volver a la lista, con cambios sin guardar pregunta antes.
  function elegir(a: Alimento | null) {
    if ((a?.id ?? null) === (elegido?.id ?? null)) return;
    if (sucio) setPendiente({ alimento: a }); else abrir(a);
  }

  const resumen = useCarga((s) => admin.resumenAlimentos(s), []);
  const lista = useCarga((s) => admin.alimentos({ q, categoria, sinIngles, fuente, page: pagina, size: POR_PAGINA }, s),
    [q, categoria, sinIngles, fuente, pagina]);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    // Sin el resumen faltan la cabecera y las opciones de un filtro: se dice, con reintento.
    if (resumen.error) {
      setAviso({ tipo: 'error', texto: `No se ha podido cargar el resumen: ${resumen.error}`,
        accion: { texto: 'Reintentar', alPulsar: resumen.recargar } });
    }
  }, [resumen.error, resumen.recargar]);

  useEffect(() => {
    if (!lista.datos) return;
    // El elegido se refresca con lo que devuelve la lista (tras guardar, trae lo nuevo).
    // Con lista y editor juntos, sin elegido, el primero; por debajo de 1440 px se
    // empieza por la lista.
    setElegido((actual) => (actual
      ? lista.datos!.content.find((a) => a.id === actual.id) ?? actual
      : dos ? lista.datos!.content[0] ?? null : null));
  }, [lista.datos, dos]);

  useEffect(() => {
    // Al volver a la lista, el foco vuelve a la fila de la que se salió.
    if (volverA === null || elegido !== null) return;
    document.querySelector<HTMLElement>(`[data-fila="${volverA}"]`)?.focus();
    setVolverA(null);
  }, [volverA, elegido]);

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
      const pendientes = await admin.alimentos({ sinIngles: true, page: 0, size: 100 });
      const otro = pendientes.content.find((a) => a.id !== actual);
      if (otro) {
        setEnfocar(true);
        setElegido(otro);
      }
      setAviso({ tipo: 'ok', texto: otro ? 'Guardado. Vamos con el siguiente sin inglés.' : 'Guardado. No queda ninguno sin inglés.' });
    } catch (err) {
      setAviso({ tipo: 'error', texto: `Guardado, pero no se ha podido buscar el siguiente: ${textoError(err)}` });
    }
  }

  const total = lista.datos?.totalElements ?? 0;
  const r = resumen.datos;
  const alimentos = lista.datos?.content ?? [];
  const verLista = dos || elegido === null;

  return (
    <Marco titulo="Alimentos" subtitulo={r ? `${entero(r.catalogo)} en el catálogo · ${entero(r.sinIngles)} sin nombre en inglés`
      + (r.productos ? ` · ${entero(r.productos)} productos de España para elegir` : '') : undefined}>
      <div className={`reparto${dos ? ' reparto--dos' : ''}`}>
        {verLista && (
          <section className="lista" aria-label="Lista de alimentos">
            <div className="lista__filtros lista__filtros--envuelve">
              <Buscador etiqueta="Buscar alimentos" marcador="Busca por nombre o por código de barras" alBuscar={(t) => filtrar(() => setQ(t))} />
              <button type="button" className="chip" aria-pressed={sinIngles} onClick={() => filtrar(() => setSinIngles((v) => !v))}>
                {sinIngles && <Icono nombre="check" tamano={18} />}Sin nombre en inglés{r ? ` · ${entero(r.sinIngles)}` : ''}
              </button>
              <Filtro etiqueta="Categoría" valor={categoria} alCambiar={(v) => filtrar(() => setCategoria(v))}
                      opciones={[{ valor: '', texto: 'todas' }, ...(r?.categorias ?? []).map((c) => ({ valor: c, texto: c.toLowerCase() }))]} />
              <Filtro etiqueta="Fuente" valor={fuente} alCambiar={(v) => filtrar(() => setFuente(v))}
                      opciones={FUENTES.map((f) => {
                        const n = f.valor ? r?.porFuente?.[f.valor as 'CIQUAL'] : undefined;
                        return { valor: f.valor, texto: n === undefined ? f.texto : `${f.texto} · ${entero(n)}` };
                      })} />
            </div>
            <div className={movil ? 'tarjetas-marco' : 'tarjeta tabla-marco'}>
              <EstadoLista cargando={lista.cargando} error={lista.error} vacio={total === 0}
                           textoVacio="Ningún alimento del catálogo con estos filtros." alReintentar={lista.recargar}>
                {movil ? (
                  <ul className="tarjetas" aria-label="Alimentos del catálogo, por nombre">
                    {alimentos.map((a) => (
                      <li key={a.id}>
                        <button type="button" className="tarjeta-fila" data-fila={a.id} onClick={() => elegir(a)}>
                          <span className="tarjeta-fila__texto">
                            <span className="fila-boton__principal">{a.nombre}</span>
                            {a.nombreEn && <span className="fila-boton__secundario">{a.nombreEn}</span>}
                          </span>
                          {!a.nombreEn && <span className="etiqueta etiqueta--aviso">Falta el inglés</span>}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <table className="tabla tabla--alimentos">
                    <caption className="solo-lector">Alimentos del catálogo, por nombre; valores por 100 g</caption>
                    <thead>
                      <tr><th scope="col">Alimento</th><th scope="col" className="num">kcal</th><th scope="col" className="num">Prot.</th>
                        <th scope="col" className="num">Carbos</th><th scope="col" className="num">Grasas</th></tr>
                    </thead>
                    <tbody>
                      {alimentos.map((a) => (
                        <tr key={a.id} data-elegida={elegido?.id === a.id} onClick={() => elegir(a)}>
                          <td>
                            <button type="button" className="fila-boton" data-fila={a.id} aria-current={elegido?.id === a.id ? 'true' : undefined}
                                    onClick={(ev) => { ev.stopPropagation(); elegir(a); }}>
                              <span className="fila-boton__principal">{a.nombre}</span>
                              {a.nombreEn
                                ? <span className="fila-boton__secundario">{a.nombreEn}</span>
                                : <span className="fila-boton__secundario fila-boton__secundario--aviso">Falta el inglés</span>}
                            </button>
                          </td>
                          <td className="num">{entero(a.calorias)}</td>
                          <td className="num">{decimal1(a.proteinas)}</td>
                          <td className="num">{decimal1(a.carbohidratos)}</td>
                          <td className="num">{decimal1(a.grasas)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </EstadoLista>
            </div>
            {total > 0 && <Paginacion pagina={pagina} tamano={POR_PAGINA} total={total} alCambiar={setPagina} />}
            <span className="lista__nota">Valores por 100 g. Los alimentos que crea cada persona son suyos y no salen aquí.</span>
            {movil && <Link className="enlace-accion" to="/avisos">Ver los avisos de la gente</Link>}
          </section>
        )}
        {elegido ? (
          <div className="reparto__editor">
            {!dos && (
              <button type="button" className="boton boton--volver" onClick={() => elegir(null)}>
                <Icono nombre="chevron_left" tamano={20} />Volver a la lista
              </button>
            )}
            <Editor key={elegido.id} alimento={elegido} categorias={r?.categorias ?? []} alGuardado={alGuardado}
                    alSiguiente={siguiente} avisar={setAviso} alCambiarSucio={setSucio} enfocar={enfocar} />
          </div>
        ) : dos && <div className="editor editor--vacio"><p>Elige un alimento de la lista para editarlo.</p></div>}
      </div>
      <DialogoDescartar abierto={pendiente !== null} alSeguir={() => setPendiente(null)}
                        texto={pendiente?.alimento === null ? 'Hay cambios sin guardar en el editor. Si vuelves a la lista, se pierden.' : undefined}
                        alDescartar={() => { setSucio(false); abrir(pendiente!.alimento); setPendiente(null); }} />
      {avisoSalir}
      <AvisoFlotante aviso={aviso} alCerrar={cerrarAviso} />
    </Marco>
  );
}
