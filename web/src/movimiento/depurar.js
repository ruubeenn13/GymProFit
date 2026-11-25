// ============================================================
// depurar.js — el panel ?debug=motion (PLAN-UN-DIA.md §4). Solo se descarga con esa URL.
//
// Mide en el propio móvil, también en el navegador de TikTok y de Instagram, donde no hay
// DevTools: fotogramas mientras se desplaza (mediana, percentil 5 y los de más de 20 ms),
// tareas largas (todas y las que caen durante el scroll), LCP, CLS, INP aproximado, la
// escena y la hora activas, los bytes de JS transferidos, el movimiento reducido y el
// navegador. «Copiar» deja el texto en el portapapeles. No envía nada a ningún sitio.
//
// Qué fotogramas cuentan lo decide fotogramas.js: solo los que tienen scroll a los dos lados.
// Con ?debug=motion&control la página sale quieta (como con movimiento reducido, sin GSAP ni
// Lenis): el mismo panel mide entonces solo el scroll del documento, para comparar.
// ============================================================
import { contador } from './fotogramas.js';

/**
 * @param {{escena: string, hora: string, progreso?: number}} estado  el de inicio.js
 * @param {boolean} control  ?debug=motion&control: la página quieta
 */
export function iniciar(estado, control = false) {
  const m = { largas: 0, largaMax: 0, largasScroll: 0, lcp: 0, cls: 0, inp: 0 };
  const fotogramas = contador();
  let ultimoScroll = -1e9;
  addEventListener('scroll', () => { ultimoScroll = performance.now(); fotogramas.scroll(); }, { passive: true });

  // Fotogramas: el intervalo entre dos requestAnimationFrame (fotogramas.js decide cuáles).
  const cuadro = (t) => {
    fotogramas.cuadro(t, document.hidden);
    requestAnimationFrame(cuadro);
  };
  requestAnimationFrame(cuadro);

  const observar = (tipo, fn, extra = {}) => {
    try { new PerformanceObserver((l) => l.getEntries().forEach(fn)).observe({ type: tipo, buffered: true, ...extra }); return true; } catch { return false; }
  };
  const hayLargas = observar('longtask', (e) => {
    m.largas++; m.largaMax = Math.max(m.largaMax, e.duration);
    if (e.startTime - ultimoScroll < 250) m.largasScroll++;
  });
  observar('largest-contentful-paint', (e) => { m.lcp = e.startTime; });
  observar('layout-shift', (e) => { if (!e.hadRecentInput) m.cls += e.value; });
  const hayInp = observar('event', (e) => { if (e.interactionId) m.inp = Math.max(m.inp, e.duration); }, { durationThreshold: 16 });

  const caja = document.createElement('div');
  caja.setAttribute('role', 'region');
  caja.setAttribute('aria-label', 'Panel de medida');
  caja.style.cssText = 'position:fixed;right:8px;bottom:8px;z-index:99;max-width:min(300px,calc(100vw - 16px));' +
    'padding:10px 12px;border-radius:12px;background:rgba(11,12,14,.92);border:1px solid #4A505B;' +
    'font:11px/1.4 ui-monospace,Menlo,Consolas,monospace;color:#F2F2F3;white-space:pre-wrap';
  const texto = document.createElement('div');
  const boton = document.createElement('button');
  boton.type = 'button';
  boton.textContent = 'Copiar';
  boton.style.cssText = 'margin-top:8px;min-height:40px;padding:0 16px;border-radius:20px;border:0;background:#FF6A00;color:#1A0E00;font:600 14px sans-serif';
  // Plegado por defecto (una línea): así no tapa la escena mientras se mide.
  const plegar = document.createElement('button');
  plegar.type = 'button';
  plegar.style.cssText = boton.style.cssText + ';margin-left:8px;background:#252A31;color:#F2F2F3';
  let abierto = false;
  plegar.addEventListener('click', () => { abierto = !abierto; pintar(); });
  const botones = document.createElement('div');
  botones.append(boton, plegar);
  caja.append(texto, botones);
  document.body.append(caja);

  const informe = () => {
    const f = fotogramas.resumen();
    const fps = (v) => (f.n ? v.toFixed(1) : '—');
    const js = performance.getEntriesByType('resource').filter((r) => /\.m?js(\?|$)/.test(r.name));
    const kb = (b) => (b / 1024).toFixed(1);
    const transferido = js.reduce((a, r) => a + (r.transferSize || 0), 0);
    const ua = navigator.userAgent;
    const chrome = ua.match(/Chrome\/(\d+)/);
    const nav = /musical_ly|Bytedance|TikTok|trill/i.test(ua) ? 'TikTok' : /Instagram/i.test(ua) ? 'Instagram'
      : /FBAN|FBAV/i.test(ua) ? 'Facebook' : chrome ? 'Chrome ' + chrome[1] : 'otro';
    const webview = /; wv\)/.test(ua) ? ' (WebView)' : '';
    return [
      `${control ? 'CONTROL · ' : ''}fps (al desplazar): mediana ${fps(f.mediana)} · p5 ${fps(f.p5)}`,
      `fotogramas > 20 ms: ${f.n ? f.lentos.toFixed(1) : '—'} % de ${f.n} · rAF a ${f.hz || '—'} Hz · pausas ${f.pausas}`,
      `modo: ${control ? 'control (sin GSAP ni Lenis, página quieta)' : 'normal'}`,
      hayLargas ? `tareas largas: ${m.largas} (máx ${Math.round(m.largaMax)} ms) · al desplazar: ${m.largasScroll}`
        : 'tareas largas: no disponible',
      `LCP ${Math.round(m.lcp)} ms · CLS ${m.cls.toFixed(3)} · INP ~${hayInp ? Math.round(m.inp) + ' ms' : 'no disponible'}`,
      `escena: ${estado.escena}${estado.progreso != null ? ' ' + estado.progreso + ' %' : ''} · hora ${estado.hora}`,
      `JS: ${js.length} ficheros, ${kb(transferido)} KB transferidos`,
      `movimiento reducido: ${matchMedia('(prefers-reduced-motion: reduce)').matches ? 'sí' : 'no'}`,
      `pantalla: ${innerWidth}×${innerHeight} @${devicePixelRatio}`,
      `navegador: ${nav}${webview}`,
      `ua: ${ua.slice(0, 160)}`,
    ].join('\n');
  };
  const pintar = () => {
    const todo = informe();
    texto.textContent = abierto ? todo : todo.split('\n')[0];
    plegar.textContent = abierto ? 'Menos' : 'Más';
  };
  setInterval(pintar, 1000);
  pintar();

  boton.addEventListener('click', () => {
    const t = informe();
    const hecho = () => { boton.textContent = 'Copiado'; setTimeout(() => { boton.textContent = 'Copiar'; }, 1500); };
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(t).then(hecho, () => aMano(t, hecho));
    } else aMano(t, hecho);
  });
}

// Sin contexto seguro (la IP de la red local va por http) no hay API del portapapeles.
function aMano(t, hecho) {
  const area = document.createElement('textarea');
  area.value = t;
  area.setAttribute('readonly', '');
  area.style.cssText = 'position:fixed;opacity:0';
  document.body.append(area);
  area.select();
  try { if (document.execCommand('copy')) hecho(); } finally { area.remove(); }
}
