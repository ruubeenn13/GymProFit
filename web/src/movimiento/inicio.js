// ============================================================
// inicio.js — el único script que pide la página al cargar (PLAN-UN-DIA.md §3).
//
// Pequeño a propósito (objetivo ≤ 6 KB gzip): decide si hay movimiento, mueve el reloj de
// la cabecera y pide con import() lo demás. La entrada ya corre en CSS desde el primer
// pintado; lo diferido espera al load y a que acabe esa entrada (para no competir con el
// LCP), o al primer gesto de scroll, lo que llegue antes:
// · por cargar.js, GSAP pieza a pieza y escena-dia.js (la entrada al bajar y la transición de
//   7:00 a 19:00), en cuanto se puede;
// · escena-entreno.js, justo después, en una tarea aparte cuando el navegador está ocioso;
// · con ratón, Lenis y escritorio.js (el cursor y el botón magnético).
// GSAP, ScrollTrigger, SplitText y DrawSVG van en el trozo de motor.js, que comparten.
//
// · ?debug=motion carga el panel de medida (depurar.js), también con movimiento reducido.
// · Con prefers-reduced-motion: reduce sale aquí: ni GSAP ni Lenis se descargan.
// ============================================================

const depurar = new URLSearchParams(location.search).get('debug') === 'motion';
const estado = { escena: 'entrada', hora: '07:00' };
if (depurar) import('./depurar.js').then((m) => m.iniciar(estado));

const reducido = matchMedia('(prefers-reduced-motion: reduce)').matches;
if (!reducido) arrancar();

function arrancar() {
  const cabecera = document.querySelector('.reloj-cabecera');

  // El reloj de la cabecera aparece con la sesión, cuando el gigante ya ha llegado a las 19:00:
  // la sesión cruza la línea de arriba de la pantalla.
  new IntersectionObserver(([e]) => {
    cabecera.classList.toggle('visible', e.isIntersecting);
  }, { rootMargin: '0px 0px -99% 0px' }).observe(document.querySelector('.entreno'));

  // La hora de la sección que ocupa el centro de la pantalla.
  const centro = new IntersectionObserver((entradas) => {
    for (const e of entradas) {
      if (!e.isIntersecting) continue;
      estado.escena = e.target.dataset.escena || 'entrada';
      ponerHora(e.target.dataset.hora);
    }
  }, { rootMargin: '-50% 0px -50% 0px' });
  document.querySelectorAll('section[data-hora]').forEach((s) => centro.observe(s));

  const api = { ponerHora, oro: (si) => cabecera.classList.toggle('oro', si), estado };

  // Cuándo se puede pedir lo diferido: tras el load y la entrada, o al primer scroll.
  let listo;
  const permiso = new Promise((r) => { listo = r; });
  addEventListener('scroll', listo, { once: true, passive: true });
  if (document.readyState === 'complete') setTimeout(listo, 900);
  else addEventListener('load', () => setTimeout(listo, 900), { once: true });

  const raton = matchMedia('(hover: hover) and (pointer: fine)').matches;
  const lenis = permiso.then(() => (raton ? import('./lenis.js').then((m) => new m.default({ autoRaf: false })) : null));

  if (raton) permiso.then(() => import('./escritorio.js')).then((m) => m.iniciar());
  // GSAP y las escenas llegan por cargar.js, una pieza por tarea. La transición se monta
  // primero (comparte el móvil con la sesión); Torso A, después, en trozos y en tiempo libre,
  // antes de que llegue al viewport: nada de montarla en pleno scroll.
  const escena = document.querySelector('[data-escena="entreno"]');
  const ocioso = () => new Promise((r) => ('requestIdleCallback' in window ? requestIdleCallback(r, { timeout: 1500 }) : setTimeout(r, 60)));
  permiso
    .then(() => Promise.all([import('./cargar.js').then((m) => m.cargar()), lenis]))
    .then(async ([{ dia, entreno }, l]) => {
      const d = await dia.iniciar({ ...api, lenis: l });
      await ocioso();
      await entreno.iniciar(escena, { ...api, lenis: l }, d);
    });
}

// El reloj de la cabecera rueda a la hora nueva: cada cifra que cambia sube un 100 % y
// entra la nueva desde abajo, 420 ms con --recorrido. Es un corte, no un cronómetro.
const RECORRIDO = 'cubic-bezier(0.77, 0, 0.175, 1)';
function ponerHora(hora) {
  if (!hora || hora === estado.hora) return;
  estado.hora = hora;
  const cifras = document.querySelectorAll('.reloj-cabecera .reloj-cifra');
  const nuevas = hora.replace(':', '');
  cifras.forEach((caja, i) => {
    const vieja = caja.lastElementChild;
    if (vieja.textContent === nuevas[i]) return;
    const nueva = document.createElement('span');
    nueva.textContent = nuevas[i];
    nueva.style.position = 'absolute';
    nueva.style.inset = '0';
    caja.append(nueva);
    const t = { duration: 420, easing: RECORRIDO, fill: 'forwards' };
    vieja.animate([{ transform: 'none' }, { transform: 'translateY(-100%)' }], t);
    nueva.animate([{ transform: 'translateY(100%)' }, { transform: 'none' }], t).onfinish = () => {
      nueva.style.position = '';
      caja.replaceChildren(nueva);
    };
  });
}
