// ============================================================
// cargar.js — trae GSAP y sus plugins de uno en uno, cada uno en su tarea (PLAN-UN-DIA.md §4).
//
// Un import() de motor.js evalúa GSAP, ScrollTrigger, SplitText y DrawSVG de una vez: con la
// CPU a ×4 eran 94 ms seguidos. Aquí se evalúa cada uno por separado, cediendo el hilo entre
// medias; cuando motor.js llega, sus dependencias ya están evaluadas y cuesta casi nada.
// Lo pide inicio.js con import(): así el módulo inicial no nombra ninguno de estos trozos.
// ============================================================
const pausa = () => (globalThis.scheduler?.yield ? globalThis.scheduler.yield() : new Promise((r) => setTimeout(r, 0)));

/** Evalúa las piezas de GSAP una a una y devuelve las dos escenas, ya importadas. */
export async function cargar() {
  await import('gsap');
  await pausa();
  await import('gsap/ScrollTrigger');
  await pausa();
  await import('gsap/SplitText');
  await pausa();
  await import('gsap/DrawSVGPlugin');
  await pausa();
  await import('./motor.js');
  await pausa();
  const dia = await import('./escena-dia.js');
  await pausa();
  const entreno = await import('./escena-entreno.js');
  return { dia, entreno };
}
