// ============================================================
// Resumen (GP-085)
// Cifras, altas por semana, sesiones por día y lo que pide trabajo en el
// catálogo, de GET /admin/resumen; y el estado del sistema. Las barras son CSS:
// sin librería de gráficas. La fila de Plantillas es de la fase 2 y no sale.
// ============================================================
import { Link } from 'react-router-dom';
import { admin, type Resumen as DatosResumen } from '../api/admin';
import { useEstadoApi } from '../componentes/EstadoApi';
import { Icono, type NombreIcono } from '../componentes/Icono';
import { Marco } from '../componentes/Marco';
import { EstadoLista } from '../componentes/Piezas';
import { cuenta, deCalendario, diaMes, entero, fechaLarga, haceMinutos } from '../util/formato';
import { useCarga } from '../util/useCarga';

function Cifra({ titulo, valor, detalle, positivo }: { titulo: string; valor: number; detalle: string; positivo?: boolean }) {
  return (
    <div className="tarjeta cifra">
      <span className="cifra__titulo">{titulo}</span>
      <span className="cifra__valor">{entero(valor)}</span>
      <span className={`cifra__detalle${positivo ? ' cifra__detalle--ok' : ''}`}>{detalle}</span>
    </div>
  );
}

// Altura de una barra en px, proporcional al máximo; un valor > 0 nunca se queda en 0.
function alto(valor: number, maximo: number, techo: number): number {
  if (maximo === 0 || valor === 0) return 0;
  return Math.max(4, Math.round((valor / maximo) * techo));
}

function AltasPorSemana({ semanas }: { semanas: DatosResumen['altasPorSemana'] }) {
  const maximo = Math.max(...semanas.map((s) => s.altas));
  const descripcion = `Altas por semana, de la más antigua a esta: ${semanas.map((s) => s.altas).join(', ')}`;
  return (
    <div className="tarjeta grafica">
      <div className="grafica__cabeza">
        <h2>Altas por semana</h2>
        <span>últimas {semanas.length}</span>
      </div>
      <div className="grafica__barras grafica__barras--altas" role="img" aria-label={descripcion}>
        {semanas.map((s, i) => {
          const actual = i === semanas.length - 1;
          return (
            <div key={s.lunes} className="grafica__columna">
              <span className={`grafica__valor${actual ? ' grafica__valor--actual' : ''}`}>{s.altas}</span>
              <div className={`grafica__barra grafica__barra--alta${actual ? ' grafica__barra--actual' : ''}`}
                   style={{ height: alto(s.altas, maximo, 135) }} />
            </div>
          );
        })}
      </div>
      <div className="grafica__ejes grafica__ejes--altas" aria-hidden="true">
        {semanas.map((s, i) => (
          <span key={s.lunes} className={i === semanas.length - 1 ? 'grafica__eje--actual' : ''}>
            {diaMes(deCalendario(s.lunes))}
          </span>
        ))}
      </div>
    </div>
  );
}

function SesionesPorDia({ dias }: { dias: DatosResumen['sesionesPorDia'] }) {
  const maximo = Math.max(...dias.map((d) => d.sesiones));
  const hoy = dias[dias.length - 1];
  const descripcion = `Sesiones por día en los últimos ${dias.length} días, de ${entero(Math.min(...dias.map((d) => d.sesiones)))} a `
    + `${entero(maximo)}; hoy, ${entero(hoy.sesiones)}`;
  // Tres marcas en el eje, como en el diseño: el primer día, el del medio y hoy.
  const medio = dias[Math.floor(dias.length / 2)];
  return (
    <div className="tarjeta grafica">
      <div className="grafica__cabeza">
        <h2>Sesiones por día</h2>
        <span>últimos {dias.length} días</span>
      </div>
      <div className="grafica__barras grafica__barras--dias" role="img" aria-label={descripcion}>
        {dias.map((d, i) => (
          <div key={d.fecha} title={`${diaMes(deCalendario(d.fecha))}: ${d.sesiones}`}
               className={`grafica__barra grafica__barra--sesion${i === dias.length - 1 ? ' grafica__barra--hoy' : ''}`}
               style={{ height: alto(d.sesiones, maximo, 168) }} />
        ))}
      </div>
      <div className="grafica__ejes grafica__ejes--dias" aria-hidden="true">
        <span>{diaMes(deCalendario(dias[0].fecha))}</span>
        <span>{diaMes(deCalendario(medio.fecha))}</span>
        <span className="grafica__eje--actual">Hoy, {entero(hoy.sesiones)}</span>
      </div>
    </div>
  );
}

function Pendiente({ icono, titulo, detalle, a, accion }: { icono: NombreIcono; titulo: string; detalle: string; a: string; accion: string }) {
  return (
    <li className="pendiente">
      <Icono nombre={icono} className="pendiente__icono" />
      <div className="pendiente__texto">
        <span className="pendiente__titulo">{titulo}</span>
        <span className="pendiente__detalle">{detalle}</span>
      </div>
      <Link to={a} className="enlace-accion" aria-label={`${accion}: ${titulo}`}>{accion}</Link>
    </li>
  );
}

function Sistema() {
  const e = useEstadoApi();
  return (
    <div className="tarjeta sistema">
      <h2>Sistema</h2>
      <dl>
        <div><dt>API</dt>
          <dd className={e.enMarcha ? 'sistema__ok' : e.enMarcha === false ? 'sistema__mal' : ''}>
            {e.enMarcha === null ? 'Comprobando…' : e.enMarcha ? 'En marcha' : 'Sin respuesta'}
          </dd></div>
        <div><dt>{e.entorno === 'Producción' ? 'Commit en producción' : 'Commit en local'}</dt>
          <dd className="sistema__commit">{e.commit ?? '—'}</dd></div>
        <div><dt>Comprobado</dt><dd>{e.comprobado ? haceMinutos(e.comprobado) : '—'}</dd></div>
      </dl>
      {e.enlaceCommit ? (
        <a className="enlace-accion" href={e.enlaceCommit} target="_blank" rel="noreferrer noopener">
          Ver el commit en GitHub<Icono nombre="open_in_new" tamano={18} />
          <span className="solo-lector"> (se abre en otra pestaña)</span>
        </a>
      ) : (
        <button type="button" className="enlace-accion" onClick={e.comprobar}>
          <Icono nombre="refresh" tamano={18} />Comprobar otra vez
        </button>
      )}
    </div>
  );
}

export function Resumen() {
  const { datos, cargando, error, recargar } = useCarga((s) => admin.resumen(s), []);
  const subtitulo = datos ? fechaLarga(deCalendario(datos.hoy)) : undefined;

  return (
    <Marco titulo="Resumen" subtitulo={subtitulo}>
      <div className="resumen">
        <EstadoLista cargando={cargando && !datos} error={error} vacio={false} textoVacio="" alReintentar={recargar}>
          {datos && (
            <>
              <section aria-label="Cifras" className="resumen__cifras">
                <Cifra titulo="Cuentas" valor={datos.cuentas.total} positivo={datos.cuentas.altasSemana > 0}
                       detalle={datos.cuentas.altasSemana > 0 ? `+${entero(datos.cuentas.altasSemana)} esta semana` : 'ninguna alta esta semana'} />
                <Cifra titulo="Entrenaron esta semana" valor={datos.entrenaronSemana}
                       detalle={`de ${cuenta(datos.cuentas.activas, 'cuenta activa', 'cuentas activas')}`} />
                <Cifra titulo="Sesiones esta semana" valor={datos.sesiones.semana}
                       detalle={`${entero(datos.sesiones.hoy)} hoy · ${entero(datos.sesiones.total)} en total`} />
                <Cifra titulo="Apuntaron comida hoy" valor={datos.comidaHoy}
                       detalle={datos.comidaHoy === 1 ? 'cuenta' : 'cuentas distintas'} />
              </section>
              <section className="resumen__graficas" aria-label="Actividad">
                <AltasPorSemana semanas={datos.altasPorSemana} />
                <SesionesPorDia dias={datos.sesionesPorDia} />
              </section>
              <section className="resumen__abajo">
                <div className="tarjeta pendientes">
                  <h2>Lo que pide trabajo en el catálogo</h2>
                  <ul>
                    <Pendiente icono="translate" a="/ejercicios"
                               titulo={`${cuenta(datos.catalogo.ejerciciosSinRevisar, 'ejercicio sin revisar', 'ejercicios sin revisar')} en español`}
                               detalle={`de ${entero(datos.catalogo.ejerciciosActivos)} activos: su nombre en español es el mismo que en inglés`}
                               accion="Revisar" />
                    <Pendiente icono="nutrition" a="/alimentos"
                               titulo={`${cuenta(datos.catalogo.alimentosSinIngles, 'alimento', 'alimentos')} sin nombre en inglés`}
                               detalle={`de ${entero(datos.catalogo.alimentosCatalogo)} en el catálogo`}
                               accion="Revisar" />
                  </ul>
                </div>
                <Sistema />
              </section>
            </>
          )}
        </EstadoLista>
      </div>
    </Marco>
  );
}
