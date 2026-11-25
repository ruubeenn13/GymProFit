// ============================================================
// escritorio.js — lo que solo tiene sentido con ratón (PLAN-UN-DIA.md, «Política de impacto»).
//
// · El cursor propio: un anillo pequeño que sigue al ratón con algo de inercia y que, durante
//   el descanso, se vuelve el aro (se vacía con la cuenta atrás que le pasa escena-entreno.js
//   con el evento «undia:aro»). En enlaces, botones y campos se aparta y vuelve el del sistema.
// · El botón de la beta, magnético: se acerca al puntero cuando pasa cerca, sin rebote.
//
// Sin GSAP: un requestAnimationFrame que solo corre mientras algo se mueve. Lo pide inicio.js
// solo con (hover: hover) y (pointer: fine), y nunca con movimiento reducido.
// ============================================================

const SIGUE = 0.22;      // lo que recorre el anillo hacia el puntero en cada fotograma
const IMAN = 0.28;       // lo que se acerca el botón al puntero
const IMAN_MAX = 12;     // y como mucho, en px
const IMAN_RADIO = 72;   // a qué distancia del botón empieza a notarse

/** Pone el cursor propio y los botones magnéticos. */
export function iniciar() {
  const cursor = document.createElement('div');
  cursor.className = 'cursor';
  cursor.setAttribute('aria-hidden', 'true');
  cursor.innerHTML = '<svg viewBox="0 0 36 36"><circle class="cursor-pista" cx="18" cy="18" r="16"/>'
    + '<circle class="cursor-lleno" cx="18" cy="18" r="16" pathLength="1"/></svg>';
  document.body.append(cursor);
  const lleno = cursor.querySelector('.cursor-lleno');
  document.documentElement.classList.add('cursor-propio');

  const destino = { x: innerWidth / 2, y: innerHeight / 2 };
  const actual = { ...destino };
  let corriendo = false;
  let visto = false;

  const botones = [...document.querySelectorAll('.accion')].map((el) => ({ el, x: 0, y: 0, ax: 0, ay: 0 }));

  addEventListener('pointermove', (e) => {
    if (e.pointerType !== 'mouse') return;
    destino.x = e.clientX; destino.y = e.clientY;
    if (!visto) { visto = true; actual.x = destino.x; actual.y = destino.y; cursor.classList.add('visible'); }
    cursor.classList.toggle('enlace', !!e.target.closest('a, button, input, textarea, select, [role="button"]'));
    for (const b of botones) {
      const r = b.el.getBoundingClientRect();
      const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
      const dentro = e.clientX > r.left - IMAN_RADIO && e.clientX < r.right + IMAN_RADIO
        && e.clientY > r.top - IMAN_RADIO && e.clientY < r.bottom + IMAN_RADIO;
      b.ax = dentro ? limitar((e.clientX - cx) * IMAN) : 0;
      b.ay = dentro ? limitar((e.clientY - cy) * IMAN) : 0;
    }
    arrancar();
  }, { passive: true });
  document.addEventListener('pointerleave', () => cursor.classList.remove('visible'));
  document.addEventListener('pointerenter', () => { if (visto) cursor.classList.add('visible'); });

  // El descanso: el anillo crece y se vacía con la cuenta atrás.
  document.addEventListener('undia:aro', (e) => {
    cursor.classList.toggle('aro', e.detail.activo);
    lleno.style.strokeDashoffset = e.detail.activo ? String(1 - e.detail.lleno) : '1';
  });

  function arrancar() {
    if (corriendo) return;
    corriendo = true;
    requestAnimationFrame(paso);
  }

  function paso() {
    actual.x += (destino.x - actual.x) * SIGUE;
    actual.y += (destino.y - actual.y) * SIGUE;
    cursor.style.transform = `translate3d(${actual.x}px, ${actual.y}px, 0)`;
    let quieto = Math.abs(destino.x - actual.x) < 0.1 && Math.abs(destino.y - actual.y) < 0.1;
    for (const b of botones) {
      b.x += (b.ax - b.x) * 0.18;
      b.y += (b.ay - b.y) * 0.18;
      b.el.style.translate = `${b.x.toFixed(2)}px ${b.y.toFixed(2)}px`;
      // El texto se mueve un poco más que el botón: da profundidad sin otra capa.
      b.el.firstElementChild.style.translate = `${(b.x * 0.4).toFixed(2)}px ${(b.y * 0.4).toFixed(2)}px`;
      if (Math.abs(b.ax - b.x) > 0.05 || Math.abs(b.ay - b.y) > 0.05) quieto = false;
    }
    if (quieto) { corriendo = false; return; }
    requestAnimationFrame(paso);
  }
}

const limitar = (v) => Math.max(-IMAN_MAX, Math.min(IMAN_MAX, v));
