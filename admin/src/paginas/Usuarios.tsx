// ============================================================
// Usuarios (GP-085)
// Lista paginada con búsqueda por usuario o correo y filtros de rol y estado, y
// la ficha de la cuenta: sus datos, cuántas sesiones y comidas tiene (no cuáles:
// son datos de salud), desactivar, dar o quitar el rol de administrador, y el
// borrado a petición del titular, que pide escribir su nombre de usuario.
// La ficha se abre como panel encima de la tabla (GP-120); en el móvil, la tabla
// pasa a tarjetas y la ficha ocupa la pantalla.
// La API es la que protege: aquí solo se evita pedir lo que va a rechazar.
// ============================================================
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { admin, type Cuenta, type FichaCuenta } from '../api/admin';
import { Dialogo } from '../componentes/Dialogo';
import { Icono } from '../componentes/Icono';
import { Marco } from '../componentes/Marco';
import { Panel } from '../componentes/Panel';
import { AvisoFlotante, Buscador, EstadoLista, Filtro, Paginacion, POR_PAGINA, type Aviso } from '../componentes/Piezas';
import { useSesion } from '../sesion/Sesion';
import { cuenta as contar, fechaCorta, haceCuanto } from '../util/formato';
import { ANCHO, useMedia } from '../util/useMedia';
import { textoError, useCarga } from '../util/useCarga';

const TEXTO_ROL: Record<string, string> = { ADMIN: 'Admin', USER: 'Usuario', GUEST: 'Invitado' };

function EtiquetaRol({ rol }: { rol: string | null }) {
  return <span className={`etiqueta${rol === 'ADMIN' ? ' etiqueta--admin' : ''}`}>{TEXTO_ROL[rol ?? ''] ?? 'Sin rol'}</span>;
}

function EtiquetaEstado({ activo }: { activo: boolean }) {
  return <span className={`etiqueta ${activo ? 'etiqueta--ok' : 'etiqueta--peligro'}`}>{activo ? 'Activa' : 'Desactivada'}</span>;
}

function fecha(iso: string | null): string {
  return iso ? fechaCorta(new Date(iso)) : '—';
}

/** Diálogo de borrado a petición: nombre de usuario escrito a mano y el motivo. */
function DialogoBorrar({ cuenta, abierto, alCerrar, alBorrada }: {
  cuenta: Cuenta; abierto: boolean; alCerrar: () => void; alBorrada: () => void;
}) {
  const [confirmacion, setConfirmacion] = useState('');
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (abierto) { setConfirmacion(''); setMotivo(''); setError(null); setEnviando(false); }
  }, [abierto]);

  const coincide = confirmacion.trim() === cuenta.username;

  async function alEnviar(e: FormEvent) {
    e.preventDefault();
    if (!coincide || !motivo.trim()) return;
    setEnviando(true);
    setError(null);
    try {
      await admin.borrarCuenta(cuenta.id, confirmacion.trim(), motivo.trim());
      alBorrada();
    } catch (err) {
      setError(textoError(err));
      setEnviando(false);
    }
  }

  return (
    <Dialogo abierto={abierto} titulo={`Borrar @${cuenta.username} y sus datos`} alCerrar={alCerrar}>
      <form onSubmit={alEnviar} className="dialogo__cuerpo">
        <p className="nota"><Icono nombre="warning" tamano={20} />
          Se borran la cuenta, sus sesiones, sus comidas, sus medidas y todo lo suyo. No se puede deshacer.
          Solo si su titular lo ha pedido a privacidad@gymprofit.app.</p>
        <div className="campo">
          <label htmlFor="confirmacion" className="campo__etiqueta">
            Escribe <strong>{cuenta.username}</strong> para confirmar <span className="campo__obligatorio">(obligatorio)</span>
          </label>
          <input id="confirmacion" className="campo__control" autoComplete="off" spellCheck={false} aria-required="true"
                 value={confirmacion} onChange={(e) => setConfirmacion(e.target.value)} />
        </div>
        <div className="campo">
          <label htmlFor="motivo" className="campo__etiqueta">Motivo <span className="campo__obligatorio">(obligatorio)</span></label>
          <textarea id="motivo" className="campo__control" rows={2} maxLength={500} aria-required="true"
                    placeholder="Por ejemplo: correo a privacidad@ del 27 de septiembre"
                    value={motivo} onChange={(e) => setMotivo(e.target.value)} aria-describedby="ayuda-motivo" />
          <span id="ayuda-motivo" className="campo__ayuda">Queda en el registro de la API, con el id de la cuenta.</span>
        </div>
        {error && <p className="campo__error" role="alert">{error}</p>}
        <div className="dialogo__acciones">
          <button type="button" className="boton" onClick={alCerrar}>Cancelar</button>
          <button type="submit" className="boton boton--peligro" disabled={!coincide || !motivo.trim() || enviando}>
            <Icono nombre="delete_forever" tamano={20} />{enviando ? 'Borrando…' : 'Borrar para siempre'}
          </button>
        </div>
      </form>
    </Dialogo>
  );
}

function Ficha({ id, alCerrar, alCambiar, avisar }: {
  id: number; alCerrar: () => void; alCambiar: () => void; avisar: (a: Aviso) => void;
}) {
  const { usuario } = useSesion();
  const movil = useMedia(ANCHO.movil);
  const { datos, cargando, error, recargar } = useCarga<FichaCuenta>((s) => admin.cuenta(id, s), [id]);
  const [ocupado, setOcupado] = useState(false);
  const [dialogoRol, setDialogoRol] = useState(false);
  const [dialogoBorrar, setDialogoBorrar] = useState(false);

  async function hacer(accion: () => Promise<unknown>, textoOk: string) {
    setOcupado(true);
    try {
      await accion();
      avisar({ tipo: 'ok', texto: textoOk });
      recargar();
      alCambiar();
    } catch (err) {
      avisar({ tipo: 'error', texto: textoError(err) });
    } finally {
      setOcupado(false);
    }
  }

  const c = datos?.cuenta;
  const propia = !!c && c.username === usuario;
  const esAdmin = c?.rol === 'ADMIN';

  return (
    <div className="ficha">
      {movil && (
        <button type="button" className="boton boton--volver" onClick={alCerrar}>
          <Icono nombre="chevron_left" tamano={20} />Volver
        </button>
      )}
      <EstadoLista cargando={cargando && !datos} error={error} vacio={false} textoVacio="" alReintentar={recargar}>
        {c && datos && (
          <>
            <div className="ficha__cabeza">
              <span className="avatar avatar--grande" aria-hidden="true">{c.username.charAt(0).toUpperCase()}</span>
              <div className="ficha__nombre">
                <h2>@{c.username}</h2>
                <div className="ficha__etiquetas"><EtiquetaRol rol={c.rol} /><EtiquetaEstado activo={c.activo} /></div>
              </div>
              {!movil && (
                <button type="button" className="boton-icono" aria-label="Cerrar la ficha" onClick={alCerrar}>
                  <Icono nombre="close" />
                </button>
              )}
            </div>
            <dl className="ficha__datos">
              <dt>Correo</dt><dd>{c.email}</dd>
              <dt>Alta</dt><dd>{fecha(c.fechaRegistro)}</dd>
              <dt>Última actividad</dt><dd>{haceCuanto(c.ultimoAcceso)}</dd>
              <dt>Sesiones</dt><dd>{contar(datos.sesiones, 'registrada', 'registradas')}</dd>
              <dt>Comidas</dt><dd>{contar(datos.comidas, 'registrada', 'registradas')}</dd>
            </dl>
            <p className="nota"><Icono nombre="visibility_off" tamano={20} />
              Su peso, sus medidas, sus entrenamientos y sus comidas no se enseñan aquí: son datos de salud.</p>

            {propia ? (
              <p className="nota"><Icono nombre="verified_user" tamano={20} />
                Es tu cuenta. No puedes desactivarla, bajarte el rol ni borrarla desde aquí.</p>
            ) : (
              <div className="ficha__acciones">
                <button type="button" className="boton boton--ancho" disabled={ocupado}
                        onClick={() => hacer(() => admin.cambiarActivo(c.id), c.activo ? 'Cuenta desactivada.' : 'Cuenta activada.')}>
                  <Icono nombre={c.activo ? 'block' : 'check_circle'} tamano={20} />
                  {c.activo ? 'Desactivar la cuenta' : 'Activar la cuenta'}
                </button>
                <span className="campo__ayuda ficha__explica">
                  {c.activo
                    ? 'No podrá entrar y sus sesiones se cierran. Sus datos se quedan y se puede volver a activar.'
                    : 'Podrá volver a entrar con su contraseña de siempre.'}
                </span>
                <button type="button" className="boton boton--ancho" disabled={ocupado} onClick={() => setDialogoRol(true)}>
                  <Icono nombre={esAdmin ? 'remove_moderator' : 'shield_person'} tamano={20} />
                  {esAdmin ? 'Quitar el rol de administrador' : 'Dar rol de administrador'}
                </button>
              </div>
            )}

            {!propia && (
              <div className="ficha__peligro">
                <button type="button" className="boton boton--peligro boton--ancho" disabled={ocupado || esAdmin}
                        onClick={() => setDialogoBorrar(true)} aria-describedby="explica-borrar">
                  <Icono nombre="delete_forever" tamano={20} />Borrar la cuenta y sus datos
                </button>
                <span id="explica-borrar" className="campo__ayuda ficha__explica">
                  {esAdmin
                    ? 'Una cuenta de administración no se borra desde aquí: quítale antes el rol.'
                    : 'Solo si su titular lo pide a privacidad@gymprofit.app. Pide escribir su nombre de usuario para confirmar, y no se puede deshacer.'}
                </span>
              </div>
            )}

            <Dialogo abierto={dialogoRol} titulo={esAdmin ? `Quitar el rol de administrador a @${c.username}` : `Dar rol de administrador a @${c.username}`}
                     alCerrar={() => setDialogoRol(false)}>
              <p className="dialogo__texto">
                {esAdmin
                  ? 'Pasará a ser una cuenta normal: no podrá entrar aquí ni usar el panel de la app.'
                  : 'Podrá entrar aquí y en el panel de la app, ver todas las cuentas y cambiar el catálogo.'}
              </p>
              <div className="dialogo__acciones">
                <button type="button" className="boton" onClick={() => setDialogoRol(false)}>Cancelar</button>
                <button type="button" className="boton boton--principal" disabled={ocupado}
                        onClick={async () => {
                          setDialogoRol(false);
                          await hacer(() => admin.cambiarRol(c.id, esAdmin ? 'USER' : 'ADMIN'),
                            esAdmin ? 'Rol de administrador retirado.' : 'Rol de administrador dado.');
                        }}>
                  {esAdmin ? 'Quitar el rol' : 'Dar el rol'}
                </button>
              </div>
            </Dialogo>
            <DialogoBorrar cuenta={c} abierto={dialogoBorrar} alCerrar={() => setDialogoBorrar(false)}
                           alBorrada={() => {
                             setDialogoBorrar(false);
                             avisar({ tipo: 'ok', texto: `Cuenta @${c.username} borrada con todos sus datos.` });
                             alCambiar();
                             alCerrar();
                           }} />
          </>
        )}
      </EstadoLista>
    </div>
  );
}

/** En el móvil, una cuenta por tarjeta: usuario, correo y su estado. */
function TarjetasCuentas({ cuentas, elegida, alAbrir }: { cuentas: Cuenta[]; elegida: number | null; alAbrir: (c: Cuenta) => void }) {
  return (
    <ul className="tarjetas" aria-label="Cuentas, de la más nueva a la más antigua">
      {cuentas.map((c) => (
        <li key={c.id}>
          <button type="button" className="tarjeta-fila" data-fila={c.id} aria-current={elegida === c.id ? 'true' : undefined}
                  onClick={() => alAbrir(c)}>
            <span className="tarjeta-fila__texto">
              <span className="fila-boton__principal">@{c.username}</span>
              <span className="fila-boton__secundario">{c.email}</span>
            </span>
            <EtiquetaEstado activo={c.activo} />
          </button>
        </li>
      ))}
    </ul>
  );
}

export function Usuarios() {
  const [q, setQ] = useState('');
  const [rol, setRol] = useState('');
  const [estado, setEstado] = useState('');
  const [pagina, setPagina] = useState(0);
  const [elegida, setElegida] = useState<Cuenta | null>(null);
  const [aviso, setAviso] = useState<Aviso | null>(null);
  const movil = useMedia(ANCHO.movil);

  const lista = useCarga((s) => admin.cuentas({ q, rol, activo: estado, page: pagina, size: POR_PAGINA }, s),
    [q, rol, estado, pagina]);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  function filtrar(cambio: () => void) {
    cambio();
    setPagina(0);
  }

  const total = lista.datos?.totalElements ?? 0;
  const cuentas = lista.datos?.content ?? [];

  return (
    <Marco titulo="Usuarios" subtitulo={lista.datos ? contar(total, 'cuenta', 'cuentas') : undefined}>
      <section className="lista" aria-label="Cuentas">
        <div className="lista__filtros">
          <Buscador etiqueta="Buscar cuentas" marcador="Busca por usuario o correo" alBuscar={(t) => filtrar(() => setQ(t))} />
          <Filtro alto etiqueta="Rol" valor={rol} alCambiar={(v) => filtrar(() => setRol(v))}
                  opciones={[{ valor: '', texto: 'todos' }, { valor: 'USER', texto: 'usuario' }, { valor: 'ADMIN', texto: 'admin' },
                    { valor: 'GUEST', texto: 'invitado' }]} />
          <Filtro alto etiqueta="Estado" valor={estado} alCambiar={(v) => filtrar(() => setEstado(v))}
                  opciones={[{ valor: '', texto: 'todas' }, { valor: 'true', texto: 'activas' }, { valor: 'false', texto: 'desactivadas' }]} />
        </div>
        <div className={movil ? 'tarjetas-marco' : 'tarjeta tabla-marco'}>
          <EstadoLista cargando={lista.cargando} error={lista.error} vacio={total === 0}
                       textoVacio="Ninguna cuenta con esos filtros." alReintentar={lista.recargar}>
            {movil ? <TarjetasCuentas cuentas={cuentas} elegida={elegida?.id ?? null} alAbrir={setElegida} /> : (
              <table className="tabla tabla--usuarios">
                <caption className="solo-lector">Cuentas, de la más nueva a la más antigua</caption>
                <thead>
                  <tr><th scope="col">Cuenta</th><th scope="col">Alta</th><th scope="col">Última actividad</th>
                    <th scope="col">Rol</th><th scope="col">Estado</th></tr>
                </thead>
                <tbody>
                  {cuentas.map((c) => (
                    <tr key={c.id} data-elegida={elegida?.id === c.id} onClick={() => setElegida(c)}>
                      <td>
                        <button type="button" className="fila-boton" data-fila={c.id} aria-current={elegida?.id === c.id ? 'true' : undefined}
                                onClick={(e) => { e.stopPropagation(); setElegida(c); }}
                                aria-label={`@${c.username}, ${c.email}. Abrir la ficha`}>
                          <span className="fila-boton__principal">@{c.username}</span>
                          <span className="fila-boton__secundario">{c.email}</span>
                        </button>
                      </td>
                      <td>{fecha(c.fechaRegistro)}</td>
                      <td>{haceCuanto(c.ultimoAcceso)}</td>
                      <td><EtiquetaRol rol={c.rol} /></td>
                      <td><EtiquetaEstado activo={c.activo} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </EstadoLista>
        </div>
        {total > 0 && <Paginacion pagina={pagina} tamano={POR_PAGINA} total={total} alCambiar={setPagina} />}
      </section>
      {elegida !== null && (
        <Panel abierto etiqueta={`Cuenta @${elegida.username}`} alCerrar={() => setElegida(null)}
               volverA={() => document.querySelector<HTMLElement>(`[data-fila="${elegida.id}"]`)}>
          <Ficha key={elegida.id} id={elegida.id} alCerrar={() => setElegida(null)} alCambiar={lista.recargar} avisar={setAviso} />
        </Panel>
      )}
      <AvisoFlotante aviso={aviso} alCerrar={cerrarAviso} />
    </Marco>
  );
}
