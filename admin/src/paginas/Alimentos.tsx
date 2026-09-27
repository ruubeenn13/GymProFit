// ============================================================
// Alimentos (GP-085)
// Solo el catálogo: los alimentos que crea cada persona son su dieta y la API no
// los devuelve aquí. Editor con aviso si las calorías no cuadran con los macros
// (util/calorias.ts). Se guarda con PATCH /alimentos/{id}, la ruta de siempre.
// ============================================================
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { admin, type Alimento, type AlimentoCambios } from '../api/admin';
import { Icono } from '../componentes/Icono';
import { Marco } from '../componentes/Marco';
import { AvisoFlotante, Buscador, DialogoDescartar, EstadoLista, Filtro, Interruptor, Paginacion, useAvisoAlSalir, type Aviso } from '../componentes/Piezas';
import { comprobarCalorias } from '../util/calorias';
import { decimal1, entero, leerNumero } from '../util/formato';
import { textoError, useCarga } from '../util/useCarga';

const TAMANO = 8;

const ORIGEN: Record<Alimento['origen'], string> = {
  OPEN_FOOD_FACTS: 'Importado de Open Food Facts al escanear su código',
  MANUAL: 'Añadido a mano al catálogo',
};

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

function Editor({ alimento, categorias, alGuardado, alSiguiente, avisar, alCambiarSucio }: {
  alimento: Alimento; categorias: string[]; alCambiarSucio: (sucio: boolean) => void;
  alGuardado: () => void; alSiguiente: (actual: number) => Promise<void>; avisar: (a: Aviso) => void;
}) {
  const [f, setF] = useState<Formulario>(() => aFormulario(alimento));
  const [errores, setErrores] = useState<Partial<Record<keyof Formulario, string>>>({});
  const [guardando, setGuardando] = useState(false);

  useEffect(() => { setF(aFormulario(alimento)); setErrores({}); }, [alimento]);

  const sucio = JSON.stringify(f) !== JSON.stringify(aFormulario(alimento));
  useAvisoAlSalir(sucio);
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
      if (siguiente) await alSiguiente(alimento.id);
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
    <form className="tarjeta editor" onSubmit={alEnviar} aria-label={`Editar ${alimento.nombre}`} noValidate>
      <div className="editor__cabeza">
        <div>
          <h2>{alimento.nombre}</h2>
          <span className="editor__origen">{ORIGEN[alimento.origen]}</span>
        </div>
        {sinIngles ? <span className="etiqueta etiqueta--aviso">Falta el inglés</span> : null}
      </div>

      <div className="rejilla rejilla--2">
        <div className="campo">
          <label htmlFor="al-es" className="campo__etiqueta">Nombre en español</label>
          <input id="al-es" className="campo__control" value={f.nombre} maxLength={100} onChange={(e) => cambiar('nombre', e.target.value)}
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
              <label htmlFor={`al-${clave}`} className="campo__etiqueta campo__etiqueta--suave">{etiqueta}</label>
              <input id={`al-${clave}`} className="campo__control" inputMode="decimal" value={f[clave] as string}
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
        <button type="button" className="boton boton--principal" disabled={guardando} onClick={() => void guardar(true)}>
          Guardar y siguiente sin inglés<Icono nombre="arrow_forward" tamano={20} />
        </button>
      </div>
    </form>
  );
}

export function Alimentos() {
  const [q, setQ] = useState('');
  const [sinIngles, setSinIngles] = useState(false);
  const [categoria, setCategoria] = useState('');
  const [origen, setOrigen] = useState('');
  const [pagina, setPagina] = useState(0);
  const [elegido, setElegido] = useState<Alimento | null>(null);
  const [sucio, setSucio] = useState(false);
  const [pendiente, setPendiente] = useState<Alimento | null>(null);

  // Elegir otra fila con cambios sin guardar pregunta antes.
  function elegir(a: Alimento) {
    if (a.id === elegido?.id) return;
    if (sucio) setPendiente(a); else setElegido(a);
  }
  const [aviso, setAviso] = useState<Aviso | null>(null);

  const resumen = useCarga((s) => admin.resumenAlimentos(s), []);
  const lista = useCarga((s) => admin.alimentos({ q, categoria, sinIngles, origen, page: pagina, size: TAMANO }, s),
    [q, categoria, sinIngles, origen, pagina]);
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
    setElegido((actual) => (actual ? lista.datos!.content.find((a) => a.id === actual.id) ?? actual : lista.datos!.content[0] ?? null));
  }, [lista.datos]);

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
      setElegido(otro ?? null);
      setAviso({ tipo: 'ok', texto: otro ? 'Guardado. Vamos con el siguiente sin inglés.' : 'Guardado. No queda ninguno sin inglés.' });
    } catch (err) {
      setAviso({ tipo: 'error', texto: `Guardado, pero no se ha podido buscar el siguiente: ${textoError(err)}` });
    }
  }

  const total = lista.datos?.totalElements ?? 0;
  const r = resumen.datos;

  return (
    <Marco titulo="Alimentos" subtitulo={r ? `${entero(r.catalogo)} en el catálogo · ${entero(r.sinIngles)} sin nombre en inglés` : undefined}>
      <div className="dos-columnas dos-columnas--alimentos">
        <section className="lista" aria-label="Lista de alimentos">
          <Buscador etiqueta="Buscar alimentos" marcador="Busca por nombre o por código de barras" alBuscar={(t) => filtrar(() => setQ(t))} />
          <div className="lista__filtros lista__filtros--envuelve">
            <button type="button" className="chip" aria-pressed={sinIngles} onClick={() => filtrar(() => setSinIngles((v) => !v))}>
              {sinIngles && <Icono nombre="check" tamano={18} />}Sin nombre en inglés{r ? ` · ${entero(r.sinIngles)}` : ''}
            </button>
            <Filtro etiqueta="Categoría" valor={categoria} alCambiar={(v) => filtrar(() => setCategoria(v))}
                    opciones={[{ valor: '', texto: 'todas' }, ...(r?.categorias ?? []).map((c) => ({ valor: c, texto: c.toLowerCase() }))]} />
            <Filtro etiqueta="Origen" valor={origen} alCambiar={(v) => filtrar(() => setOrigen(v))}
                    opciones={[{ valor: '', texto: 'todos' }, { valor: 'OPEN_FOOD_FACTS', texto: 'Open Food Facts' }, { valor: 'MANUAL', texto: 'a mano' }]} />
          </div>
          <div className="tarjeta tabla-marco">
            <EstadoLista cargando={lista.cargando} error={lista.error} vacio={total === 0}
                         textoVacio="Ningún alimento del catálogo con estos filtros." alReintentar={lista.recargar}>
              <table className="tabla tabla--alimentos">
                <caption className="solo-lector">Alimentos del catálogo, por nombre; valores por 100 g</caption>
                <thead>
                  <tr><th scope="col">Alimento</th><th scope="col" className="num">kcal</th><th scope="col" className="num">Prot.</th>
                    <th scope="col" className="num">Carbos</th><th scope="col" className="num">Grasas</th></tr>
                </thead>
                <tbody>
                  {lista.datos?.content.map((a) => (
                    <tr key={a.id} data-elegida={elegido?.id === a.id} onClick={() => elegir(a)}>
                      <td>
                        <button type="button" className="fila-boton" aria-current={elegido?.id === a.id ? 'true' : undefined} onClick={(ev) => { ev.stopPropagation(); elegir(a); }}>
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
            </EstadoLista>
          </div>
          {total > 0 && <Paginacion pagina={pagina} tamano={TAMANO} total={total} alCambiar={setPagina} />}
          <span className="lista__nota">Valores por 100 g. Los alimentos que crea cada persona son suyos y no salen aquí.</span>
        </section>
        {elegido
          ? <Editor key={elegido.id} alimento={elegido} categorias={r?.categorias ?? []} alGuardado={alGuardado}
                    alSiguiente={siguiente} avisar={setAviso} alCambiarSucio={setSucio} />
          : <div className="tarjeta editor editor--vacio"><p>Elige un alimento de la lista para editarlo.</p></div>}
      </div>
      <DialogoDescartar abierto={pendiente !== null} alSeguir={() => setPendiente(null)}
                        alDescartar={() => { setSucio(false); setElegido(pendiente); setPendiente(null); }} />
      <AvisoFlotante aviso={aviso} alCerrar={cerrarAviso} />
    </Marco>
  );
}
