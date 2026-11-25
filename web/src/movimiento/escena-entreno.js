// ============================================================
// escena-entreno.js — Torso A a las 19:00 y el récord de las 19:40 (PLAN-UN-DIA.md, «Política
// de impacto», 3 y 4).
//
// Llega con import() después de la transición, en una tarea ociosa (inicio.js). El HTML ya trae el
// estado final; aquí se pone cada pieza en su estado inicial y el scroll la lleva al final.
//
// Una sola línea de tiempo, de 0 a 100, atada al scroll de la escena (sticky en CSS, sin pin),
// con varias capas a la vez, cada una a su ritmo: el reloj gigante rueda de 19:00 a 19:40 por
// detrás; la interfaz se sale del móvil (la fila de la serie, el aro del descanso), crece a
// escala de página, hace su momento y vuelve a su sitio; las frases cambian con cada paso y la
// que se lee no se mueve. Lo que en la app es un toque (marcar, +15 s, el récord) se aplica al
// cruzar su punto, con los tiempos de Movimiento.java, y se deshace al volver hacia arriba.
// ============================================================
import {
  gsap, SplitText, conectarLenis, cuentakilometros, escalaMovil, hastaSesion,
  centroSesion, rectEnEscenario, rectFinal, encima, esEscritorio, ENFATIZADA, ESTANDAR, RECORRIDO, SALIDA,
} from './motor.js';

const s = (ms) => ms / 1000;
// Las chispas del récord: solo en escritorio y en móviles que den la talla.
const CHISPAS = 16;

/**
 * Prepara la escena y engancha sus pasos al scroll.
 * @param {HTMLElement} escena  la <section data-escena="entreno">
 * @param {{ponerHora: Function, oro: Function, estado: object, lenis: object|null}} api
 * @param {{enSesion: Function}} dia  lo que devuelve escena-dia.js
 */
export function iniciar(escena, api, dia) {
  conectarLenis(api.lenis);
  const $ = (sel, raiz = document) => raiz.querySelector(sel);
  const $$ = (sel, raiz = document) => [...raiz.querySelectorAll(sel)];

  // ── Piezas ──────────────────────────────────────────────────────────────
  const escenario = $('.escenario');
  const pos = $('.movil-pos', escenario);
  const escala = $('.movil-escala', escenario);
  const pantalla = $('.pantalla', escenario);
  const capaHoy = $('.capa-hoy', pantalla);
  const capaSesion = $('.capa-sesion', pantalla);
  const panel = $('[data-pieza="descanso"]', pantalla);
  const fin = $('[data-pieza="fin"]', pantalla);
  const ejercicio = $('[data-pieza="ejercicio"]', pantalla);
  const tuRecord = $('[data-pieza="tu-record"] span', pantalla);
  const aviso = $('[data-pieza="aviso-record"]', pantalla);
  const trofeo = $('[data-pieza="trofeo"]', pantalla);
  const aro = $('[data-pieza="aro"]', pantalla);
  const aroCaja = $('[data-pieza="aro-caja"]', pantalla);
  const cuenta = $('[data-cifra="cuenta"]', pantalla);
  const mas15 = $('[data-pieza="mas15"]', pantalla);
  const cifra = {
    tiempo: $('[data-cifra="tiempo"]', pantalla),
    volumen: $('[data-cifra="volumen"]', pantalla),
    series: $('[data-cifra="series"]', pantalla),
  };
  const barra = $('.cab-barra-hecho', pantalla);
  const total = Number($('.cab-de', pantalla).textContent.replace(/\D/g, ''));

  const fugaFila = $('.fuga-fila', escenario);
  const fugaCheck = $('.fuga-check', escenario);
  const fugaAro = $('.fuga-aro', escenario);
  const fugaAroLleno = $('.fuga-aro-lleno', escenario);
  const fugaCuenta = $('.fuga-cuenta', escenario);
  const fugaNum = $('.fuga-cuenta-num', escenario);
  const fugaMas = $('.fuga-mas', escenario);
  const chispas = $('.chispas', escenario);

  const frases = $$('.frases li', escena);
  const record = $('.record', escena);
  const recordTrofeo = $('.record-trofeo', escena);
  const recordCifra = $('.record-cifra', escena);
  const recordRotulo = $('.record-rotulo', escena);
  const recordAntes = $('.record-antes', escena);
  const tachon = $('.record-tachon path', escena);

  const reloj = $('.reloj-dia');
  const ponerReloj = cuentakilometros(reloj);
  const luzTarde = $('.luz-tarde');
  const luzOro = $('.luz-oro');

  // Lo que dice el HTML de cada serie (su estado final), para volver a él. La fila que se
  // escapa es una copia de la serie 1: se escribe a la vez que la del móvil.
  const leerFila = (f) => {
    const valores = $$('.s-valor', f);
    return { el: f, valores, final: valores.map((v) => v.textContent), pistas: $$('.s-pista', f),
      marca: $('.s-marca', f), record: f.classList.contains('serie-record') };
  };
  const fila = $$('[data-pieza="serie"]', pantalla).map(leerFila);
  const copia = leerFila($('[data-pieza="serie"]', fugaFila));
  const datosSeries = JSON.parse(escena.dataset.series || '[]');
  const segundos = Number(cuenta.dataset.segundos);
  const perimetro = parseFloat(getComputedStyle(aro).getPropertyValue('--perimetro'));
  const recordFinal = tuRecord.textContent;
  const extraBoton = Number(mas15.textContent.replace(/\D/g, ''));

  let cursorActivo = null, cursorLleno = -1; // lo último que se le dijo al cursor (avisarCursor)

  // ── Estado inicial ──────────────────────────────────────────────────────
  gsap.set(capaSesion, { y: 40 });
  gsap.set($('.capa-descanso', pantalla), { opacity: 1 });
  gsap.set(panel, { yPercent: 110 });
  gsap.set(fin, { autoAlpha: 0, y: -12 });
  for (const f of [...fila, copia]) {
    f.valores.forEach((v) => { v.textContent = ''; });
    f.pistas.forEach((p) => { p.style.visibility = 'visible'; });
    f.el.classList.remove('serie-record');
  }
  fila.forEach((f, i) => f.marca.classList.add(i === 0 ? 'toca' : 'pendiente'));
  copia.marca.classList.add('toca');
  tuRecord.textContent = tuRecord.dataset.antes;
  gsap.set(aviso, { height: 0, autoAlpha: 0, marginTop: 0, paddingTop: 0, paddingBottom: 0, borderWidth: 0 });
  gsap.set(aro, { strokeDashoffset: 0 });
  gsap.set(fugaAroLleno, { strokeDashoffset: 0 });
  gsap.set(fugaCheck.querySelector('path'), { drawSVG: '0%' });
  gsap.set(record, { opacity: 1 });
  gsap.set([recordTrofeo, recordRotulo, recordAntes], { opacity: 0 });
  gsap.set(tachon, { drawSVG: '0%' });
  ponerCabecera(-1);

  // Las frases, partidas en caracteres con máscara: entran desde abajo y salen por arriba.
  const partidas = frases.map((li) => SplitText.create(li.querySelector('.letras'),
    { type: 'chars,words', mask: 'chars', wordsClass: 'pal', aria: 'none' }));
  frases.forEach((li) => gsap.set(li, { opacity: 1 }));
  partidas.forEach((p) => gsap.set(p.chars, { yPercent: 115 }));
  const marcaPartida = SplitText.create(recordCifra.querySelector('.letras'), { type: 'chars', mask: 'chars', aria: 'none' });
  gsap.set(marcaPartida.chars, { yPercent: 115 });

  // ── Lo que es un toque en la app: se aplica al cruzar y se deshace al volver ──
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
  const pulsar = (el) => gsap.fromTo(el, { scale: 1 }, { scale: 0.94, duration: s(140), ease: ESTANDAR, yoyo: true, repeat: 1 });

  // CHECK: el botón se rellena (260 ms) y la cabecera suma.
  function marcar(n, si) {
    const filas = n === 0 ? [fila[0], copia] : [fila[n]];
    for (const f of filas) {
      f.marca.classList.remove('pendiente');
      f.marca.classList.toggle('toca', !si);
      if (si) gsap.fromTo(f.marca.firstElementChild, { scale: 0.4 }, { scale: 1, duration: s(260), ease: ENFATIZADA });
    }
    if (fila[n + 1]) {
      fila[n + 1].marca.classList.toggle('toca', si);
      fila[n + 1].marca.classList.toggle('pendiente', !si);
    }
    ponerCabecera(si ? n : n - 1);
  }

  // Como la lista de la app al marcar: se desplaza lo justo para que la fila n se vea entera.
  function verFila(n) {
    const fondo = fila[n].el.offsetTop + fila[n].el.offsetHeight;
    const limite = capaSesion.offsetHeight - 16;
    gsap.to(ejercicio, { y: Math.min(0, limite - fondo), duration: s(350), ease: ESTANDAR, overwrite: 'auto' });
  }

  // El récord (momento 25): la fila a oro, el aviso entra con el trofeo, «Tu récord» pasa a
  // «de hoy» y el reloj de la cabecera y el gigante se tiñen de oro.
  function marcarRecord(si) {
    const f = fila.find((x) => x.record);
    f.el.classList.toggle('serie-record', si);
    tuRecord.textContent = si ? recordFinal : tuRecord.dataset.antes;
    api.oro(si);
    reloj.classList.toggle('oro', si);
    if (si) {
      gsap.to(aviso, { height: 'auto', autoAlpha: 1, marginTop: 10, paddingTop: 12, paddingBottom: 12, borderWidth: 1,
        duration: s(350), ease: ENFATIZADA, overwrite: true });
      gsap.fromTo(trofeo, { scale: 0.5 }, { scale: 1, duration: s(450), ease: ENFATIZADA });
    } else {
      gsap.to(aviso, { height: 0, autoAlpha: 0, marginTop: 0, paddingTop: 0, paddingBottom: 0, borderWidth: 0,
        duration: s(250), ease: ESTANDAR, overwrite: true });
    }
  }

  // Chispas DOM: salen del trofeo y se apagan (CHISPAS, 700 ms). Una vez, al bajar.
  const potente = esEscritorio() || ((navigator.hardwareConcurrency || 0) >= 8 && (navigator.deviceMemory || 8) >= 6);
  function chispear() {
    if (!potente) return;
    const t = recordTrofeo.getBoundingClientRect(), e = escenario.getBoundingClientRect();
    chispas.style.transform = `translate(${t.left - e.left + t.width / 2}px, ${t.top - e.top + t.height / 2}px)`;
    for (let i = 0; i < CHISPAS; i++) {
      const c = document.createElement('span');
      c.className = 'chispa';
      chispas.append(c);
      const ang = (i / CHISPAS) * Math.PI * 2 + Math.random() * 0.4;
      const r = 70 + Math.random() * 90;
      c.animate([
        { transform: 'translate(0, 0) scale(.6)', opacity: 1 },
        { transform: `translate(${Math.cos(ang) * r}px, ${Math.sin(ang) * r}px) scale(1)`, opacity: 1, offset: 0.55 },
        { transform: `translate(${Math.cos(ang) * r * 1.15}px, ${Math.sin(ang) * r * 1.15 + 18}px) scale(.3)`, opacity: 0 },
      ], { duration: 700 + Math.random() * 200, easing: 'cubic-bezier(0.23, 1, 0.32, 1)' }).onfinish = () => c.remove();
    }
  }

  // Los pasos que son un toque, por el progreso de la escena (0 a 1).
  const PASOS = [
    { en: 0.06, hacer: () => pulsar($('[data-pieza="empezar"]', pantalla)), deshacer: () => {} },
    { en: 0.215, hacer: () => marcar(0, true), deshacer: () => marcar(0, false) },
    { en: 0.46, hacer: () => { pulsar(fugaMas); pulsar(mas15); }, deshacer: () => {} },
    { en: 0.61, hacer: () => verFila(1), deshacer: () => verFila(0) },
    { en: 0.665, hacer: () => marcar(1, true), deshacer: () => marcar(1, false) },
    { en: 0.705, hacer: () => api.ponerHora(frases[5].dataset.hora), deshacer: () => api.ponerHora(escena.dataset.hora) },
    { en: 0.71, hacer: () => verFila(2), deshacer: () => verFila(1) },
    { en: 0.765, hacer: () => { marcar(2, true); marcarRecord(true); gsap.delayedCall(s(380), () => verFila(2)); },
      deshacer: () => { marcarRecord(false); marcar(2, false); gsap.delayedCall(s(280), () => verFila(2)); } },
    { en: 0.785, hacer: chispear, deshacer: () => {} },
  ];
  let hechos = 0;
  function pasos(p) {
    while (hechos < PASOS.length && PASOS[hechos].en <= p) PASOS[hechos++].hacer();
    while (hechos > 0 && PASOS[hechos - 1].en > p) PASOS[--hechos].deshacer();
  }

  // ── Geometría: de dónde salen las piezas que se escapan ────────────────
  const pose = () => ({ ...centroSesion(), s: escalaMovil(escala) });
  const desdeFila = () => encima(rectFinal(fugaFila, escenario), rectEnEscenario(fila[0].el, pantalla, pose()));
  const desdeAro = () => encima(rectFinal(fugaAro, escenario), rectEnEscenario(aroCaja, pantalla, pose()));
  // Mientras la fila está fuera, el móvil se aparta un poco y se apaga: es un plano más.
  const apartado = () => ({ x: hastaSesion(pos).x - (esEscritorio() ? innerWidth * 0.05 : 0),
    y: hastaSesion(pos).y + (esEscritorio() ? 0 : innerHeight * 0.06) });

  // ── La línea de tiempo (0 a 100) ────────────────────────────────────────
  // La lleva el scroll dentro de la línea de tiempo maestra de escena-dia.js (enSesion).
  const tl = gsap.timeline({ defaults: { ease: 'none' } });
  const reloj19 = 19 * 60, reloj1940 = 19 * 60 + 40;
  const hora = { min: reloj19 };
  const escribir = (f, campo, prox) => { f.valores[campo].textContent = f.final[campo].slice(0, Math.round(prox.n));
    f.pistas[campo].style.visibility = prox.n > 0.5 ? 'hidden' : 'visible'; };
  const teclear = (n, campo, en, dura) => {
    const prox = { n: 0 };
    const filas = n === 0 ? [fila[0], copia] : [fila[n]];
    tl.to(prox, { n: fila[n].final[campo].length, duration: dura, onUpdate: () => filas.forEach((f) => escribir(f, campo, prox)) }, en);
  };
  // Cada frase entra en su punto (data-en del HTML, de 0 a 1) y la anterior sale justo antes.
  const frase = (i) => {
    const en = Math.max(0.5, Number(frases[i].dataset.en) * 100);
    if (i > 0) tl.to(partidas[i - 1].chars, { yPercent: -115, duration: 0.9, stagger: 0.015, ease: 'power2.in' }, en - 1);
    tl.to(partidas[i].chars, { yPercent: 0, duration: 1.6, stagger: 0.03, ease: SALIDA }, en);
  };

  // El reloj de fondo: de 19:00 a 19:40 mientras dura la sesión, con los minutos sueltos.
  tl.to(hora, { min: reloj1940, duration: 66, onUpdate: () => ponerReloj(hora.min, true) }, 10);

  // 0–10 · «Hoy toca» y Empezar: la sesión sube y entra (SESION_SUBE_DP, ENFATIZADA).
  frase(0);
  tl.fromTo(capaHoy, { opacity: 1 }, { opacity: 0, duration: 1.5, immediateRender: false }, 7)
    .to(capaSesion, { y: 0, duration: 2.5, ease: ENFATIZADA }, 7);

  // 10–27 · Serie 1: la fila sale del móvil y crece; la pista deja sitio a los números de hoy,
  // cifra a cifra; un check gigante se dibuja de un trazo; la fila vuelve.
  frase(1);
  tl.set(fila[0].el, { visibility: 'hidden' }, 10)
    .fromTo(fugaFila, { opacity: 1, x: () => desdeFila().x, y: () => desdeFila().y, scale: () => desdeFila().scale },
      { opacity: 1, x: 0, y: 0, scale: 1, duration: 4, ease: RECORRIDO, immediateRender: false }, 10)
    .to(pos, { x: () => apartado().x, y: () => apartado().y, opacity: 0.35, duration: 4, ease: RECORRIDO }, 10);
  teclear(0, 0, 14.5, 2.2);
  teclear(0, 1, 17, 2.2);
  tl.set(fugaCheck, { opacity: 1 }, 19.5)
    .to(fugaCheck.querySelector('path'), { drawSVG: '100%', duration: 3, ease: SALIDA }, 19.5)
    .to(fugaCheck, { opacity: 0, duration: 1.5 }, 23.5)
    .to(fugaFila, { x: () => desdeFila().x, y: () => desdeFila().y, scale: () => desdeFila().scale, duration: 4, ease: RECORRIDO }, 23.5)
    .to(pos, { x: () => hastaSesion(pos).x, y: () => hastaSesion(pos).y, opacity: 1, duration: 4, ease: RECORRIDO }, 23.5)
    .set(fugaFila, { opacity: 0 }, 27.5)
    .set(fila[0].el, { visibility: 'visible' }, 27.5);

  // 25–61 · El descanso: sube en el móvil (HOJA), el aro se sale y crece hasta enmarcar la
  // pantalla, la cuenta atrás va atada al scroll y a mitad «+15 s» le devuelve el trozo.
  frase(2);
  tl.to(panel, { yPercent: 0, duration: 2.5, ease: ENFATIZADA }, 27.5)
    .fromTo(fugaAro, { opacity: 1, x: () => desdeAro().x, y: () => desdeAro().y, scale: () => desdeAro().scale },
      { opacity: 1, x: 0, y: 0, scale: 1, duration: 5, ease: RECORRIDO, immediateRender: false }, 30.5)
    .set(aroCaja, { opacity: 0 }, 30.5)
    .to(pos, { opacity: 0.16, duration: 4 }, 31)
    .to(reloj, { opacity: 0.3, duration: 4 }, 31)
    .fromTo(fugaCuenta, { opacity: 0, scale: 0.86 }, { opacity: 1, scale: 1, duration: 3, ease: SALIDA, immediateRender: false }, 33);
  frase(3);
  const DESDE = 36, HASTA = 56, MAS15 = 46;
  const descanso = { t: 0 };
  let ultimo = '';
  tl.to(descanso, { t: 1, duration: HASTA - DESDE, onUpdate: () => {
    const p = DESDE + descanso.t * (HASTA - DESDE);
    const extra = p >= MAS15 ? extraBoton : 0;
    const quedan = Math.max(0, Math.round(segundos - descanso.t * (segundos + extraBoton) + extra));
    const texto = `${Math.floor(quedan / 60)}:${String(quedan % 60).padStart(2, '0')}`;
    if (texto !== ultimo) { cuenta.textContent = texto; fugaNum.textContent = texto; ultimo = texto; }
    const lleno = Math.min(1, quedan / segundos);
    aro.style.strokeDashoffset = String(perimetro * (1 - lleno));
    fugaAroLleno.style.strokeDashoffset = String(1 - lleno);
    avisarCursor(descanso.t > 0 && descanso.t < 1, lleno);
  } }, DESDE);
  tl.to(fugaCuenta, { opacity: 0, scale: 0.92, duration: 2 }, 56)
    .to(fugaAro, { x: () => desdeAro().x, y: () => desdeAro().y, scale: () => desdeAro().scale, duration: 4, ease: RECORRIDO }, 56)
    .to(pos, { opacity: 1, duration: 4 }, 56)
    .to(reloj, { opacity: 1, duration: 4 }, 56)
    .set(fugaAro, { opacity: 0 }, 60)
    .set(aroCaja, { opacity: 1 }, 60)
    // El aviso de fin («¡A por la serie 2!») baja y se va (ESTANDAR).
    .to(panel, { yPercent: 110, duration: 1.5, ease: ESTANDAR }, 60)
    .to(fin, { autoAlpha: 1, y: 0, duration: 1.5, ease: ESTANDAR }, 60)
    .to(fin, { autoAlpha: 0, y: 20, duration: 1.5, ease: ESTANDAR }, 63.5);

  // 61–70 · Serie 2, igual que la 1, sin salir del móvil.
  frase(4);
  teclear(1, 0, 62.5, 1.5);
  teclear(1, 1, 64.2, 1.5);

  // 70–100 · 19:40, el récord. La luz pasa al oro en toda la escena, la marca nueva sale
  // enorme, la de antes se tacha con un trazo, el trofeo entra y el móvil se aparta.
  frase(5);
  teclear(2, 0, 72.5, 2);
  teclear(2, 1, 74.7, 1.2);
  frase(6);
  tl.to(luzOro, { opacity: 1, duration: 3 }, 76.5)
    .to(luzTarde, { opacity: 0.25, duration: 3 }, 76.5)
    .to(pos, { opacity: esEscritorio() ? 0.6 : 0.3, duration: 3 }, 76.5)
    .fromTo(recordTrofeo, { opacity: 0, scale: 0.6, y: 30 }, { opacity: 1, scale: 1, y: 0, duration: 3, ease: ENFATIZADA, immediateRender: false }, 77)
    .to(marcaPartida.chars, { yPercent: 0, duration: 2.2, stagger: 0.12, ease: SALIDA }, 77.6)
    .to(recordRotulo, { opacity: 1, duration: 1.5 }, 78)
    .to(recordAntes, { opacity: 1, duration: 1.5 }, 79.5)
    .to(tachon, { drawSVG: '100%', duration: 3, ease: SALIDA }, 81)
    .to({}, { duration: 1 }, 99);

  tl.eventCallback('onUpdate', () => {
    const p = tl.progress();
    pasos(p);
    api.estado.escena = 'entreno';
    api.estado.progreso = Math.round(p * 100);
  });
  dia.enSesion(tl);

  // El cursor (escritorio.js) se vuelve el aro durante el descanso.
  function avisarCursor(activo, lleno) {
    const l = Math.round(lleno * 200) / 200;
    if (activo === cursorActivo && l === cursorLleno) return;
    cursorActivo = activo; cursorLleno = l;
    document.dispatchEvent(new CustomEvent('undia:aro', { detail: { activo, lleno: l } }));
  }
}
