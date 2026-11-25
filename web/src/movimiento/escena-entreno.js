// ============================================================
// escena-entreno.js — las 19:00 y el récord de las 19:40 (PLAN-UN-DIA.md §1).
//
// Llega con import() cuando la escena está a un viewport (inicio.js). El HTML ya trae el
// estado final; aquí se pone cada pieza en su estado inicial y el scroll la lleva al final.
//
// Dos tipos de movimiento, como dice el plan:
// · Lo que es tiempo (el aro y la cuenta atrás del descanso, la luz) va atado al scroll.
// · Lo que en la app es un toque (Empezar, escribir, marcar, +15 s, el récord) se dispara al
//   cruzar su punto, dura lo que dura en la app (Movimiento.java) y se deshace al volver.
//
// Una sola ScrollTrigger recorre la escena (sticky en CSS, sin pin) y reparte los pasos
// por el progreso. Solo transform y opacity, salvo el fondo de la fila del récord y la
// altura del aviso, que cambian una vez.
// ============================================================
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);
// La barra del navegador de TikTok e Instagram cambia el alto al desplazar: sin esto,
// cada cambio recalcularía todas las posiciones a mitad de gesto.
ScrollTrigger.config({ ignoreMobileResize: true });

// Movimiento.java: ENFATIZADA y ESTANDAR (REBOTE no se usa en la web). GSAP no lee
// cubic-bezier() en texto sin CustomEase; con esta función de 20 líneas no hace falta.
const ENFATIZADA = bezier(0.05, 0.7, 0.1, 1);
const ESTANDAR = bezier(0.2, 0, 0, 1);
const s = (ms) => ms / 1000;

/**
 * Prepara la escena y engancha sus pasos al scroll.
 * @param {HTMLElement} escena  la <section data-escena="entreno">
 * @param {{ponerHora: Function, oro: Function, estado: object, lenis: object|null}} api
 */
export function iniciar(escena, api) {
  if (api.lenis) {
    api.lenis.on('scroll', ScrollTrigger.update);
    gsap.ticker.add((t) => api.lenis.raf(t * 1000));
    gsap.ticker.lagSmoothing(0);
  }
  const q = (sel) => escena.querySelector(sel);
  const qa = (sel) => [...escena.querySelectorAll(sel)];

  const capaHoy = q('.capa-hoy');
  const capaSesion = q('.capa-sesion');
  const capaDescanso = q('.capa-descanso');
  const panel = q('[data-pieza="descanso"]');
  const fin = q('[data-pieza="fin"]');
  const pasos = q('.pasos');
  const series = qa('[data-pieza="serie"]');
  const tuRecord = q('[data-pieza="tu-record"] span');
  const aviso = q('[data-pieza="aviso-record"]');
  const trofeo = q('[data-pieza="trofeo"]');
  const aro = q('[data-pieza="aro"]');
  const cuenta = q('[data-cifra="cuenta"]');
  const cifra = {
    tiempo: q('[data-cifra="tiempo"]'),
    volumen: q('[data-cifra="volumen"]'),
    series: q('[data-cifra="series"]'),
  };
  const barra = q('.cab-barra-hecho');
  const total = Number(q('.cab-de').textContent.replace(/\D/g, ''));
  const luzTarde = document.querySelector('.luz-tarde');
  const luzOro = document.querySelector('.luz-oro');
  const luzAmanecer = document.querySelector('.luz-amanecer');

  // Lo que dice el HTML de cada serie (su estado final), para volver a él.
  const fila = series.map((f) => {
    const valores = [...f.querySelectorAll('.s-valor')];
    return {
      el: f, valores, final: valores.map((v) => v.textContent),
      pistas: [...f.querySelectorAll('.s-pista')], marca: f.querySelector('.s-marca'),
      record: f.classList.contains('serie-record'),
    };
  });
  // Las cifras de la cabecera tras cada serie salen del HTML (dia-ejemplo.json), no de aquí.
  const datosSeries = JSON.parse(escena.dataset.series || '[]');
  const segundos = Number(cuenta.dataset.segundos);
  const perimetro = parseFloat(getComputedStyle(aro).getPropertyValue('--perimetro'));
  const recordFinal = tuRecord.textContent;

  // ── Estado inicial ──────────────────────────────────────────────────────
  gsap.set(capaHoy, { autoAlpha: 1 });
  gsap.set(capaSesion, { y: 40 });
  gsap.set(capaDescanso, { autoAlpha: 1 });
  gsap.set(panel, { yPercent: 110 });
  gsap.set(fin, { autoAlpha: 0, y: -12 });
  fila.forEach((f, i) => {
    f.valores.forEach((v) => { v.textContent = ''; });
    f.pistas.forEach((p) => { p.style.visibility = 'visible'; });
    f.marca.classList.add(i === 0 ? 'toca' : 'pendiente');
    f.el.classList.remove('serie-record');
  });
  tuRecord.textContent = tuRecord.dataset.antes;
  gsap.set(aviso, { height: 0, autoAlpha: 0, marginTop: 0, paddingTop: 0, paddingBottom: 0, borderWidth: 0 });
  ponerCabecera(-1);
  gsap.set(aro, { strokeDashoffset: 0 });

  function ponerCabecera(n) {
    const d = datosSeries[n];
    cifra.tiempo.textContent = d ? d.tiempo : '0:00';
    cifra.volumen.textContent = d ? d.volumen : '0';
    cifra.series.textContent = String(n + 1);
    gsap.to(barra, { scaleX: (n + 1) / total, duration: s(500), ease: ESTANDAR });
    if (n >= 0) {
      // CIFRA: cae 6 px y pasa de 0,3 a 1, 250 ms.
      gsap.fromTo([cifra.tiempo, cifra.volumen, cifra.series], { y: -6, opacity: 0.3 },
        { y: 0, opacity: 1, duration: s(250), ease: ESTANDAR, overwrite: true });
    }
  }

  function pulsar(el) {
    gsap.fromTo(el, { scale: 1 }, { scale: 0.97, duration: s(140), ease: ESTANDAR, yoyo: true, repeat: 1 });
  }

  // RELLENO: la pista deja sitio al número de hoy, cifra a cifra, 150 ms cada una.
  function escribir(n, campo) {
    const f = fila[n], v = f.valores[campo], texto = f.final[campo];
    gsap.killTweensOf(v);
    f.pistas[campo].style.visibility = 'hidden';
    const t = { i: 0 };
    gsap.to(t, {
      i: texto.length, duration: s(150) * texto.length, ease: 'none', roundProps: 'i',
      onUpdate: () => { v.textContent = texto.slice(0, t.i); },
    });
  }
  function borrar(n, campo) {
    const f = fila[n];
    f.valores[campo].textContent = '';
    f.pistas[campo].style.visibility = 'visible';
  }

  // CHECK: el botón se rellena (260 ms) y la cabecera suma.
  function marcar(n, si) {
    const f = fila[n];
    f.marca.classList.remove('pendiente');
    f.marca.classList.toggle('toca', !si);
    f.marca.classList.toggle('hecha', si);
    if (fila[n + 1]) {
      fila[n + 1].marca.classList.toggle('toca', si);
      fila[n + 1].marca.classList.toggle('pendiente', !si);
    }
    if (si) gsap.fromTo(f.marca.firstElementChild, { scale: 0.4 }, { scale: 1, duration: s(260), ease: ENFATIZADA });
    ponerCabecera(si ? n : n - 1);
  }

  // Como la lista de la app al marcar: se desplaza lo justo para que la fila n se vea entera.
  const ejercicio = q('[data-pieza="ejercicio"]');
  function verFila(n) {
    const actual = gsap.getProperty(ejercicio, 'y');
    const fondo = fila[n].el.getBoundingClientRect().bottom - actual;
    const limite = capaSesion.getBoundingClientRect().bottom - 16;
    gsap.to(ejercicio, { y: Math.min(0, limite - fondo), duration: s(350), ease: ESTANDAR, overwrite: 'auto' });
  }

  function activo(i) {
    pasos.setAttribute('data-activo', '');
    pasos.querySelectorAll('li').forEach((li, j) => li.classList.toggle('activo', j === i));
  }

  // El récord (momento 25): la fila a oro (ORO, 900 ms), el aviso entra (350 ms desde
  // −8 px) con el trofeo de 0,5 a 1 (TROFEO, 450 ms), «Tu récord» pasa a «de hoy», el reloj
  // de la cabecera se tiñe de oro y la luz funde al dorado.
  function record(si) {
    const f = fila.find((x) => x.record);
    f.el.classList.toggle('serie-record', si);
    tuRecord.textContent = si ? recordFinal : tuRecord.dataset.antes;
    api.oro(si);
    gsap.to(luzOro, { opacity: si ? 1 : 0, duration: s(900), ease: ESTANDAR, overwrite: true });
    if (si) {
      gsap.to(aviso, { height: 'auto', autoAlpha: 1, marginTop: 10, paddingTop: 12, paddingBottom: 12, borderWidth: 1,
        duration: s(350), ease: ENFATIZADA, overwrite: true });
      gsap.fromTo(trofeo, { scale: 0.5 }, { scale: 1, duration: s(450), ease: ENFATIZADA });
      gsap.fromTo(f.el, { backgroundColor: '#3A2E12' }, { backgroundColor: '#2A2414', duration: s(675), delay: s(225), ease: ESTANDAR });
    } else {
      gsap.to(aviso, { height: 0, autoAlpha: 0, marginTop: 0, paddingTop: 0, paddingBottom: 0, borderWidth: 0,
        duration: s(250), ease: ESTANDAR, overwrite: true });
      gsap.set(f.el, { clearProps: 'backgroundColor' });
    }
  }

  // ── Los pasos, por el progreso de la escena (0 a 1) ─────────────────────
  // en: dónde se dispara; hacer/deshacer: lo que pasa al cruzarlo hacia abajo o hacia arriba.
  const PASOS = [
    { en: 0.0, hacer: () => activo(0), deshacer: () => {} },
    { en: 0.06, hacer: () => pulsar(q('[data-pieza="empezar"]')), deshacer: () => {} },
    { en: 0.09,
      hacer: () => { activo(1); gsap.to(capaHoy, { autoAlpha: 0, duration: s(250), ease: ESTANDAR });
        gsap.to(capaSesion, { y: 0, duration: s(350), ease: ENFATIZADA }); },
      deshacer: () => { activo(0); gsap.to(capaHoy, { autoAlpha: 1, duration: s(250), ease: ESTANDAR });
        gsap.to(capaSesion, { y: 40, duration: s(250), ease: ESTANDAR }); } },
    { en: 0.13, hacer: () => escribir(0, 0), deshacer: () => borrar(0, 0) },
    { en: 0.15, hacer: () => escribir(0, 1), deshacer: () => borrar(0, 1) },
    { en: 0.18, hacer: () => marcar(0, true), deshacer: () => marcar(0, false) },
    // HOJA: el descanso sube 450 ms con ENFATIZADA; baja en 250 ms con ESTANDAR.
    { en: 0.21, hacer: () => { activo(2); gsap.to(panel, { yPercent: 0, duration: s(450), ease: ENFATIZADA }); },
      deshacer: () => { activo(1); gsap.to(panel, { yPercent: 110, duration: s(250), ease: ESTANDAR }); } },
    { en: 0.35, hacer: () => pulsar(q('[data-pieza="mas15"]')), deshacer: () => {} },
    { en: 0.48,
      hacer: () => { gsap.to(panel, { yPercent: 110, duration: s(250), ease: ESTANDAR });
        gsap.to(fin, { autoAlpha: 1, y: 0, duration: s(250), ease: ESTANDAR }); },
      deshacer: () => { gsap.to(panel, { yPercent: 0, duration: s(250), ease: ESTANDAR });
        gsap.to(fin, { autoAlpha: 0, y: -12, duration: s(250), ease: ESTANDAR }); } },
    { en: 0.53, hacer: () => gsap.to(fin, { autoAlpha: 0, y: 20, duration: s(250), ease: ESTANDAR }),
      deshacer: () => gsap.to(fin, { autoAlpha: 1, y: 0, duration: s(250), ease: ESTANDAR }) },
    { en: 0.56, hacer: () => { activo(3); verFila(1); escribir(1, 0); },
      deshacer: () => { activo(2); verFila(0); borrar(1, 0); } },
    { en: 0.58, hacer: () => escribir(1, 1), deshacer: () => borrar(1, 1) },
    { en: 0.61, hacer: () => marcar(1, true), deshacer: () => marcar(1, false) },
    { en: 0.68, hacer: () => { activo(4); api.ponerHora(pasos.querySelector('[data-hora]').dataset.hora); },
      deshacer: () => { activo(3); api.ponerHora(escena.dataset.hora); } },
    { en: 0.71, hacer: () => { verFila(2); escribir(2, 0); }, deshacer: () => { verFila(1); borrar(2, 0); } },
    { en: 0.74, hacer: () => escribir(2, 1), deshacer: () => borrar(2, 1) },
    { en: 0.78, hacer: () => { marcar(2, true); record(true); gsap.delayedCall(s(380), () => verFila(2)); },
      deshacer: () => { record(false); marcar(2, false); gsap.delayedCall(s(280), () => verFila(2)); } },
    { en: 0.93, hacer: () => api.oro(false), deshacer: () => api.oro(true) },
  ];
  let hechos = 0;

  // El descanso, atado al scroll: de 0,22 a 0,48 pasan 2:00 más los 15 s de «+15 s», en lineal.
  const DESDE = 0.22, HASTA = 0.48, MAS15 = 0.35;
  // Los segundos del botón, de su rótulo («+15 s», de strings.xml vía dia-ejemplo.json).
  const extraBoton = Number(q('[data-pieza="mas15"]').textContent.replace(/\D/g, ''));
  let ultimoTexto = '';
  function descanso(p) {
    const t = Math.min(1, Math.max(0, (p - DESDE) / (HASTA - DESDE)));
    const extra = p >= MAS15 ? extraBoton : 0;
    const quedan = Math.max(0, Math.round(segundos - t * (segundos + extraBoton) + extra));
    const texto = `${Math.floor(quedan / 60)}:${String(quedan % 60).padStart(2, '0')}`;
    if (texto !== ultimoTexto) { cuenta.textContent = texto; ultimoTexto = texto; }
    gsap.set(aro, { strokeDashoffset: perimetro * (1 - Math.min(1, quedan / segundos)) });
  }

  function actualizar(p) {
    while (hechos < PASOS.length && PASOS[hechos].en <= p) PASOS[hechos++].hacer();
    while (hechos > 0 && PASOS[hechos - 1].en > p) PASOS[--hechos].deshacer();
    descanso(p);
    api.estado.escena = 'entreno';
    api.estado.progreso = Math.round(p * 100);
  }

  ScrollTrigger.create({
    trigger: q('.entreno-pista'), start: 'top top', end: 'bottom bottom',
    onUpdate: (self) => actualizar(self.progress),
    onRefresh: (self) => actualizar(self.progress),
  });

  // La luz: del amanecer a la de la tarde mientras la escena entra en pantalla.
  gsap.timeline({ scrollTrigger: { trigger: escena, start: 'top bottom', end: 'top top', scrub: true } })
    .to(luzAmanecer, { opacity: 0, ease: 'none' }, 0)
    .to(luzTarde, { opacity: 1, ease: 'none' }, 0);
}

/**
 * Una curva cubic-bezier(x1, y1, x2, y2) como función de 0 a 1, para GSAP.
 * Newton sobre x(t) y, si no converge, bisección.
 */
function bezier(x1, y1, x2, y2) {
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
