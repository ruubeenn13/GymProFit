// ============================================================
// Embudo (GP-019)
// En qué paso del alta se va la gente y qué hacen las cuentas nuevas en sus primeros
// 7 días, de GET /admin/embudo. Solo recuentos: aquí no sale nadie (DEC-064). Las
// barras son CSS, como en Resumen.
// ============================================================
import { useState } from 'react';
import { admin, type Embudo as DatosEmbudo, type PasoAlta } from '../api/admin';
import { Marco } from '../componentes/Marco';
import { EstadoLista, Filtro } from '../componentes/Piezas';
import { cuenta, deCalendario, diaMes, entero } from '../util/formato';
import { ANCHO, useMedia } from '../util/useMedia';
import { useCarga } from '../util/useCarga';

/** El nombre de cada pantalla del alta, como se ve en la app. */
export const NOMBRE_PASO: Record<PasoAlta, string> = {
  BIENVENIDA: 'Bienvenida',
  OBJETIVO: 'Tu objetivo',
  NIVEL: 'Tu nivel',
  SOBRE_TI: 'Sobre ti',
  DONDE: 'Dónde entrenas',
  DIAS: 'Días y tiempo',
  PLAN: 'Tu plan',
  GUARDA: 'Guarda tu plan',
  CUENTA: 'Todo listo (cuenta creada)',
  AVISOS: '¿Te avisamos?',
};

/** Porcentaje entero de {@code parte} sobre {@code todo}, o «—» si no hay de dónde. */
export function porcentaje(parte: number, todo: number): string {
  return todo > 0 ? `${Math.round((parte / todo) * 100)} %` : '—';
}

/**
 * Las llegadas a cada paso, sumadas en las semanas elegidas: todas ('') o la de ese lunes.
 */
export function sumarPasos(embudo: DatosEmbudo['embudo'], lunes: string): { paso: PasoAlta; total: number }[] {
  const semanas = lunes ? embudo.filter((s) => s.lunes === lunes) : embudo;
  const totales = new Map<PasoAlta, number>();
  for (const s of semanas) for (const p of s.pasos) totales.set(p.paso, (totales.get(p.paso) ?? 0) + p.total);
  return (embudo[0]?.pasos ?? []).map((p) => ({ paso: p.paso, total: totales.get(p.paso) ?? 0 }));
}

function semanaDe(lunes: string): string {
  return `semana del ${diaMes(deCalendario(lunes))}`;
}

function Pasos({ datos }: { datos: DatosEmbudo }) {
  const [lunes, setLunes] = useState('');
  const pasos = sumarPasos(datos.embudo, lunes);
  const primero = pasos[0]?.total ?? 0;
  const maximo = Math.max(1, ...pasos.map((p) => p.total));
  const opciones = [{ valor: '', texto: `las ${datos.embudo.length} semanas` },
    ...[...datos.embudo].reverse().map((s) => ({ valor: s.lunes, texto: semanaDe(s.lunes) }))];
  return (
    <section className="tarjeta embudo" aria-labelledby="titulo-embudo">
      <div className="grafica__cabeza">
        <h2 id="titulo-embudo">Pasos del alta</h2>
        <Filtro etiqueta="Semana" valor={lunes} opciones={opciones} alCambiar={setLunes} />
      </div>
      <p className="embudo__nota">
        Cuántas veces se llegó a cada pantalla, una por intento. El porcentaje es sobre la bienvenida.
      </p>
      {primero === 0 && pasos.every((p) => p.total === 0) ? (
        <p className="embudo__vacio">Ningún paso contado en {lunes ? `la ${semanaDe(lunes)}` : 'estas semanas'}.</p>
      ) : (
        <ol className="embudo__pasos">
          {pasos.map((p, i) => {
            const anterior = i > 0 ? pasos[i - 1].total : p.total;
            const seVan = Math.max(0, anterior - p.total);
            return (
              <li key={p.paso} className="embudo__paso">
                <span className="embudo__nombre">{NOMBRE_PASO[p.paso]}</span>
                <span className="embudo__barra-marco" aria-hidden="true">
                  <span className={`embudo__barra${p.paso === 'CUENTA' ? ' embudo__barra--cuenta' : ''}`}
                        style={{ width: `${(p.total / maximo) * 100}%` }} />
                </span>
                <span className="embudo__total">{entero(p.total)}</span>
                <span className="embudo__por">{porcentaje(p.total, primero)}</span>
                <span className="embudo__caida">{i > 0 && seVan > 0 ? `−${entero(seVan)}` : ''}</span>
              </li>
            );
          })}
        </ol>
      )}
    </section>
  );
}

function Nucleo({ semanas }: { semanas: DatosEmbudo['nucleo'] }) {
  const movil = useMedia(ANCHO.movil);
  const filas = [...semanas].reverse();
  const cifra = (n: number, de: number) => `${entero(n)} · ${porcentaje(n, de)}`;
  return (
    <section className="tarjeta nucleo" aria-labelledby="titulo-nucleo">
      <div className="grafica__cabeza">
        <h2 id="titulo-nucleo">Las cuentas nuevas en sus primeros 7 días</h2>
        <span>por semana de alta</span>
      </div>
      <p className="embudo__nota">
        «Vuelven» es apuntar un entreno, una comida, agua o el peso otro día que el del alta. Una semana
        «en curso» aún tiene cuentas a las que no les han pasado sus 7 días.
      </p>
      {movil ? (
        <ul className="tarjetas">
          {filas.map((s) => (
            <li key={s.lunes} className="tarjeta nucleo__tarjeta">
              <span className="fila-boton__principal">
                {semanaDe(s.lunes)}{s.completa ? '' : ' · en curso'}
              </span>
              <span>{cuenta(s.cuentas, 'cuenta nueva', 'cuentas nuevas')}</span>
              <span>Entrenan {cifra(s.entrenan, s.cuentas)} · Comen {cifra(s.comen, s.cuentas)}</span>
              <span>Vuelven {cifra(s.vuelven, s.cuentas)}</span>
            </li>
          ))}
        </ul>
      ) : (
        <table className="tabla tabla--nucleo">
          <caption className="solo-lector">Cuentas nuevas por semana de alta, de la más reciente a la más antigua</caption>
          <thead>
            <tr><th scope="col">Semana</th><th scope="col" className="num">Cuentas</th>
              <th scope="col" className="num">Entrenan</th><th scope="col" className="num">Comen</th>
              <th scope="col" className="num">Vuelven</th></tr>
          </thead>
          <tbody>
            {filas.map((s) => (
              <tr key={s.lunes}>
                <th scope="row">{semanaDe(s.lunes)}{s.completa ? '' : <span className="nucleo__curso"> · en curso</span>}</th>
                <td className="num">{entero(s.cuentas)}</td>
                <td className="num">{cifra(s.entrenan, s.cuentas)}</td>
                <td className="num">{cifra(s.comen, s.cuentas)}</td>
                <td className="num">{cifra(s.vuelven, s.cuentas)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

export function Embudo() {
  const { datos, cargando, error, recargar } = useCarga((s) => admin.embudo(s), []);
  return (
    <Marco titulo="Embudo" subtitulo="sin datos de nadie">
      <div className="resumen">
        <EstadoLista cargando={cargando && !datos} error={error} vacio={false} textoVacio="" alReintentar={recargar}>
          {datos && (
            <>
              <Pasos datos={datos} />
              <Nucleo semanas={datos.nucleo} />
            </>
          )}
        </EstadoLista>
      </div>
    </Marco>
  );
}
