// ============================================================
// fotogramas.js — cómo cuenta el panel ?debug=motion los fotogramas (PLAN-UN-DIA.md §4).
//
// Sin DOM: depurar.js le pasa cada evento de scroll y cada requestAnimationFrame. Así se
// puede probar fuera del navegador (FotogramasWebTest, con node).
//
// Un intervalo entre dos rAF solo cuenta si hubo scroll en los dos fotogramas que lo cierran.
// El navegador despacha el evento de scroll una vez por fotograma, antes de los rAF, cuando
// la posición ha cambiado; así que eso es «se estaba desplazando de verdad». Queda fuera:
// · la cola de reposo tras parar (antes contaban 250 ms de fotogramas quietos);
// · el primer fotograma de un gesto nuevo, que mide la pausa entre gestos y no un tirón;
// · lo que pase con la pestaña oculta, y el primer fotograma al volver.
// Un tirón en pleno scroll sí cuenta: sus dos fotogramas llevan scroll. Salvo si pasa de 1 s:
// con la página visible el rAF no para, así que más de 1 s sin ninguno es el navegador en pausa
// (otra app encima, el WebView suspendido). Esas pausas se cuentan aparte.
// ============================================================

const PAUSA = 1000;
const FRECUENCIAS = [30, 48, 60, 72, 90, 120, 144, 165];

/** Contador de fotogramas mientras se desplaza. */
export function contador() {
  const intervalos = [];
  let pausas = 0;
  let huboScroll = false;
  let previoScroll = false;
  let previo = 0;
  return {
    /** Un evento de scroll: marca el fotograma en curso. */
    scroll() { huboScroll = true; },
    /** @param {number} t  la marca del requestAnimationFrame · @param {boolean} oculto */
    cuadro(t, oculto = false) {
      const ahora = huboScroll && !oculto;
      if (previo && ahora && previoScroll && t - previo > PAUSA) pausas++;
      else if (previo && ahora && previoScroll) {
        intervalos.push(t - previo);
        if (intervalos.length > 20000) intervalos.splice(0, 10000);
      }
      previo = oculto ? 0 : t;
      previoScroll = ahora;
      huboScroll = false;
    },
    resumen() { return { ...resumir(intervalos), pausas }; },
  };
}

/**
 * Mediana y percentil 5 en fps, porcentaje de fotogramas de más de 20 ms y la frecuencia a la
 * que el navegador entrega fotogramas al desplazar: el percentil 10 de los intervalos (los
 * tirones solo alargan), redondeado a la frecuencia de pantalla más cercana.
 */
export function resumir(intervalos) {
  const ord = [...intervalos].sort((a, b) => a - b);
  const pc = (p) => (ord.length ? ord[Math.min(ord.length - 1, Math.floor(p * ord.length))] : 0);
  const fps = (ms) => (ms ? 1000 / ms : 0);
  const rapido = fps(pc(0.1));
  return {
    n: ord.length,
    mediana: fps(pc(0.5)),
    p5: fps(pc(0.95)),
    lentos: ord.length ? (100 * ord.filter((x) => x > 20).length) / ord.length : 0,
    hz: rapido ? FRECUENCIAS.reduce((a, b) => (Math.abs(b - rapido) < Math.abs(a - rapido) ? b : a)) : 0,
  };
}
