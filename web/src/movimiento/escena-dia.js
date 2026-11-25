// ============================================================
// escena-dia.js — de las 7:00 a las 19:00 (PLAN-UN-DIA.md, «Política de impacto», 1 y 2).
//
// La entrada ya ha corrido en CSS (la luz barre, el reloj rueda hasta las 07:00, el titular
// sube letra a letra, el móvil sube y gira). Aquí, todo atado al scroll y en capas que se
// mueven a la vez, cada una a su ritmo:
// · Al bajar de la entrada: el móvil sube más despacio que el texto (parallax) y el reloj se
//   asienta en el centro.
// · La transición (120 svh): las horas ruedan de 07 a 19 como un cuentakilómetros, las lamas
//   giran y se alargan, la luz pasa del dorado al naranja, el reloj pasa a contorno, el móvil
//   cruza al centro y se enciende en «Hoy toca». La frase de la transición entra por
//   caracteres y se queda quieta mientras se lee.
// ============================================================
import { gsap, SplitText, ceder, conectarLenis, cuentakilometros, hastaSesion, esEscritorio, RECORRIDO, SALIDA } from './motor.js';

/**
 * @param {{lenis: object|null}} api
 */
export async function iniciar(api) {
  conectarLenis(api.lenis);
  const $ = (s) => document.querySelector(s);
  const reloj = $('.reloj-dia');
  const ponerReloj = cuentakilometros(reloj);
  const llena = reloj.querySelector('.cara-llena');
  const contorno = reloj.querySelector('.cara-contorno');
  const pos = $('.escenario .movil-pos');
  const arranque = $('.capa-arranque');
  const hoy = $('.capa-hoy');
  const amanecer = $('.luz-amanecer');
  const tarde = $('.luz-tarde');
  const lamasAmanecer = amanecer.querySelector('.lamas');
  const lamasTarde = tarde.querySelector('.lamas');
  const frase = $('.frase-transicion');

  // Si se ha bajado antes de que llegara este módulo, la entrada en CSS se da por acabada.
  if (scrollY > 0) document.getAnimations().forEach((a) => { if (a.playState === 'running') a.finish(); });

  // ── Una sola línea de tiempo para todo el viaje ────────────────────────
  // La entrada al bajar, la transición y la sesión tocan las mismas piezas (el móvil, su
  // pantalla, el reloj): con una línea de tiempo y un ScrollTrigger cada una, la inercia de
  // cada scrub decidía cuál pintaba la última. Aquí van en una, medida en svh de scroll: cada
  // tramo es una línea de tiempo propia (de duración 1) colocada en su sitio. La de la sesión
  // la añade escena-entreno.js cuando llega (enSesion).
  const vh = innerHeight;
  const arriba = (el) => el.getBoundingClientRect().top + scrollY;
  const pistaT = $('.transicion-pista'), pistaE = $('.entreno-pista');
  const tramo = {
    entrada: [0, arriba(pistaT) / vh],
    transicion: [arriba(pistaT) / vh, (pistaT.offsetHeight - vh) / vh],
    sesion: [arriba(pistaE) / vh, (pistaE.offsetHeight - vh) / vh],
  };
  const fin = tramo.sesion[0] + tramo.sesion[1];
  const maestra = gsap.timeline({
    defaults: { ease: 'none' },
    scrollTrigger: { trigger: document.body, start: 0, end: () => arriba(pistaE) + pistaE.offsetHeight - innerHeight,
      scrub: 0.6, invalidateOnRefresh: true },
  });
  maestra.to({}, { duration: 0.001 }, fin - 0.001);
  const colocar = (sub, [desde, largo]) => { maestra.add(sub, desde); sub.timeScale(sub.duration() / largo); };

  await ceder();

  // ── Al bajar de la entrada ──────────────────────────────────────────────
  const entrada = gsap.timeline({ defaults: { ease: 'none' } })
    .to(pos, { y: () => (esEscritorio() ? -0.12 : -0.22) * innerHeight, rotation: 1, duration: 1 }, 0)
    .to(reloj, { y: () => (esEscritorio() ? 0 : innerHeight * 0.19), scale: 1.04, duration: 1 }, 0)
    .to(lamasAmanecer, { xPercent: -12, duration: 1 }, 0);
  colocar(entrada, tramo.entrada);

  await ceder();

  // ── La transición: el día pasa bajo el pulgar ───────────────────────────
  const reloj07 = 7 * 60, reloj19 = 19 * 60;
  const hora = { min: reloj07 };
  const partida = SplitText.create(frase.querySelector('.letras'), { type: 'chars,words', mask: 'chars', wordsClass: 'pal', aria: 'none' });
  gsap.set(frase, { opacity: 1 });
  gsap.set(partida.chars, { yPercent: 110 });

  await ceder();
  const tl = gsap.timeline({ defaults: { ease: 'none' } });
  tl.to(hora, { min: reloj19, duration: 1, ease: 'power1.inOut', onUpdate: () => ponerReloj(hora.min) }, 0)
    // Las lamas giran, se alargan y cambian de color con la hora (las dos capas, a la vez).
    .fromTo([lamasAmanecer, lamasTarde], { rotation: -11, skewX: -32, scaleX: 1.25, scaleY: 0.8, xPercent: -12, yPercent: 22 },
      { rotation: 24, skewX: 18, scaleX: 0.9, scaleY: 1.1, xPercent: 20, yPercent: 4, duration: 1 }, 0)
    .to(amanecer, { opacity: 0, duration: 0.7 }, 0.15)
    .to(tarde, { opacity: 1, duration: 0.7 }, 0.15)
    // El reloj, al centro y de lleno a contorno.
    .to(reloj, { y: () => (esEscritorio() ? 0 : innerHeight * 0.24), scale: 1, duration: 0.6, ease: RECORRIDO }, 0)
    .to(llena, { opacity: 0, duration: 0.25 }, 0.72)
    .to(contorno, { opacity: 1, duration: 0.25 }, 0.72)
    // El móvil cruza al centro y se endereza; a mitad de camino, la pantalla se enciende.
    .to(pos, { x: () => hastaSesion(pos).x, y: () => hastaSesion(pos).y, rotation: 2, duration: 0.85, ease: RECORRIDO }, 0.05)
    .to(arranque, { opacity: 0, duration: 0.15 }, 0.55)
    .to(hoy, { opacity: 1, duration: 0.15 }, 0.55)
    // La frase entra por caracteres, se queda quieta y se va al final.
    .to(partida.chars, { yPercent: 0, duration: 0.18, stagger: 0.004, ease: SALIDA }, 0.08)
    .to(partida.chars, { yPercent: -110, duration: 0.12, stagger: 0.002, ease: 'power2.in' }, 0.86);
  // Rotation 2 sobre el -2 del CSS: el móvil acaba recto en la sesión.
  colocar(tl, tramo.transicion);

  return {
    /** Coloca la línea de tiempo de la sesión (duración cualquiera) en su tramo. */
    // Sin refresh: la duración de la maestra ya cuenta con el tramo de la sesión, y un refresh
    // la pintaba entera de una vez (84–127 ms con la CPU a ×4). Así cada tween se prepara
    // cuando el scroll llega a él, repartido entre fotogramas.
    enSesion: (sub) => colocar(sub, tramo.sesion),
  };
}
