// ============================================================
// motor.js — lo que comparten las escenas de «Un día» (PLAN-UN-DIA.md, «Política de impacto»).
//
// GSAP con ScrollTrigger, SplitText y DrawSVG, registrados una vez; las curvas de la página y
// de la app; el reloj gigante como cuentakilómetros; y dónde está cada cosa del móvil, para
// que las piezas que se escapan salgan de su sitio exacto. Lo pide inicio.js con import()
// (nunca va en el módulo inicial) y lo importan escena-dia.js y escena-entreno.js.
// ============================================================
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { SplitText } from 'gsap/SplitText';
import { DrawSVGPlugin } from 'gsap/DrawSVGPlugin';

gsap.registerPlugin(ScrollTrigger, SplitText, DrawSVGPlugin);
// La barra del navegador de TikTok e Instagram cambia el alto al desplazar: sin esto, cada
// cambio recalcularía todas las posiciones a mitad de gesto.
ScrollTrigger.config({ ignoreMobileResize: true });

export { gsap, ScrollTrigger, SplitText };

// Curvas. Página (DISENO.md): SALIDA para lo que entra, RECORRIDO para lo que cruza. App
// (Movimiento.java): ENFATIZADA y ESTANDAR. Ninguna rebota.
export const SALIDA = bezier(0.23, 1, 0.32, 1);
export const RECORRIDO = bezier(0.77, 0, 0.175, 1);
export const ENFATIZADA = bezier(0.05, 0.7, 0.1, 1);
export const ESTANDAR = bezier(0.2, 0, 0, 1);

let conectado = false;
/** Lenis (solo con ratón) mueve el scroll: ScrollTrigger tiene que enterarse de cada paso. */
export function conectarLenis(lenis) {
  if (!lenis || conectado) return;
  conectado = true;
  lenis.on('scroll', ScrollTrigger.update);
  gsap.ticker.add((t) => lenis.raf(t * 1000));
  gsap.ticker.lagSmoothing(0);
}

/** Escritorio: ≥ 1200 px, con su propia composición (texto a un lado, móvil al otro). */
export const esEscritorio = () => innerWidth >= 1200;

// ── El reloj gigante, como cuentakilómetros ───────────────────────────────
// Cuatro ruedas por cara (las dos caras, llena y contorno, ruedan juntas). Cada rueda va a su
// posición con transform: la de las unidades, continua; las demás, quietas hasta que la de su
// derecha pasa del 9 al 0 (o del 5 al 0), y entonces ruedan con ella, como las de verdad.
const BASES = [3, 10, 6, 10];
const UNIDAD = [600, 60, 10, 1]; // minutos que vale un paso de cada rueda

/**
 * @param {HTMLElement} reloj  el .reloj-dia
 * @returns {(minutos: number, conUnidades?: boolean) => void}  pone el reloj en esos minutos
 *   del día. Sin conUnidades la rueda de los minutos sueltos se queda en 0 y la de las decenas
 *   lleva el giro (de 7:00 a 19:00 pasan 72 vueltas cortas, no 720 ilegibles).
 */
export function cuentakilometros(reloj) {
  const ruedas = [0, 1, 2, 3].map((i) => [...reloj.querySelectorAll(`[data-col="${i}"] .odo-tira`)]);
  const ultima = [NaN, NaN, NaN, NaN];
  return (minutos, conUnidades = false) => {
    const pos = [0, 0, 0, 0];
    const baja = conUnidades ? 3 : 2;
    for (let i = baja; i >= 0; i--) {
      const valor = minutos / UNIDAD[i];
      if (i === baja) { pos[i] = valor % BASES[i]; continue; }
      // Rueda con la de su derecha solo mientras esa pasa por su última cifra.
      pos[i] = (Math.floor(valor) % BASES[i]) + Math.max(0, pos[i + 1] - (BASES[i + 1] - 1));
    }
    for (let i = 0; i < 4; i++) {
      const p = Math.round(pos[i] * 1000) / 1000;
      if (p === ultima[i]) continue;
      ultima[i] = p;
      for (const t of ruedas[i]) t.style.transform = `translate3d(0, ${-p}em, 0)`;
    }
  };
}

// ── Dónde está cada cosa del móvil ────────────────────────────────────────
// El móvil es un marco de 414 × 868 con la pantalla de 390 × 844 a 12 px del canto, centrado
// en el escenario, desplazado por su pose (translate del CSS + x, y de GSAP) y escalado por
// --ps. Una pieza de la pantalla se sitúa por sus offset (que no ven transform) más el
// desplazamiento con que la lista de la sesión deja ver la fila que toca.

/** La escala del móvil, tal como la resuelve el CSS (--ps). */
export function escalaMovil(escala) {
  return parseFloat(getComputedStyle(escala).scale) || 1;
}

/** El translate del CSS (la pose de la entrada), en px. */
export function poseCss(el) {
  const [x = '0', y = '0'] = getComputedStyle(el).translate.split(' ');
  return { x: parseFloat(x) || 0, y: parseFloat(y) || 0 };
}

/**
 * El rectángulo que ocupa una pieza de la pantalla en el escenario cuando el móvil está en la
 * pose { x, y, s } (x e y: el centro del móvil respecto al centro del escenario).
 */
export function rectEnEscenario(pieza, pantalla, pose, desplazamiento = 0) {
  let l = 0, t = 0;
  for (let n = pieza; n && n !== pantalla; n = n.offsetParent) { l += n.offsetLeft; t += n.offsetTop; }
  const cx = innerWidth / 2 + pose.x, cy = innerHeight / 2 + pose.y;
  return {
    left: cx + (12 + l - 207) * pose.s,
    top: cy + (12 + t + desplazamiento - 434) * pose.s,
    width: pieza.offsetWidth * pose.s,
    height: pieza.offsetHeight * pose.s,
  };
}

/**
 * Dónde queda el centro del móvil en la sesión, respecto al centro del escenario: en el centro
 * en escritorio; en vertical, un 14 % más abajo, en el hueco que dejan las frases.
 */
export const centroSesion = () => ({ x: 0, y: esEscritorio() ? 0 : innerHeight * 0.14 });

/** La x, y de GSAP que llevan el móvil de su pose de la entrada (el translate del CSS) a la sesión. */
export function hastaSesion(pos) {
  const css = poseCss(pos), c = centroSesion();
  return { x: c.x - css.x, y: c.y - css.y };
}

/** El rectángulo de una pieza grande en su sitio final, sin el transform que la encoge. */
export function rectFinal(el, escenario) {
  const antes = el.style.transform;
  el.style.transform = 'none';
  const r = el.getBoundingClientRect(), e = escenario.getBoundingClientRect();
  el.style.transform = antes;
  return { left: r.left - e.left, top: r.top - e.top, width: r.width, height: r.height };
}

/** El transform que pone una pieza grande (origen arriba a la izquierda) encima de otro rect. */
export function encima(final, sobre) {
  const k = sobre.width / final.width;
  return { x: sobre.left - final.left, y: sobre.top - final.top, scale: k };
}

/**
 * Una curva cubic-bezier(x1, y1, x2, y2) como función de 0 a 1, para GSAP (que no lee
 * cubic-bezier() en texto sin CustomEase). Newton sobre x(t) y, si no converge, bisección.
 */
export function bezier(x1, y1, x2, y2) {
  const cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx;
  const cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by;
  const x = (t) => ((ax * t + bx) * t + cx) * t;
  const y = (t) => ((ay * t + by) * t + cy) * t;
  const dx = (t) => (3 * ax * t + 2 * bx) * t + cx;
  return (p) => {
    if (p <= 0 || p >= 1) return p;
    let t = p;
    for (let i = 0; i < 8; i++) {
      const e = x(t) - p, d = dx(t);
      if (Math.abs(e) < 1e-5) return y(t);
      if (Math.abs(d) < 1e-6) break;
      t -= e / d;
    }
    let lo = 0, hi = 1; t = p;
    while (hi - lo > 1e-5) { if (x(t) < p) lo = t; else hi = t; t = (lo + hi) / 2; }
    return y(t);
  };
}
