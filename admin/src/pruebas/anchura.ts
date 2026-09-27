// ============================================================
// Simula el ancho de la ventana para los tests: jsdom no tiene matchMedia, y la web
// decide con él qué pintar (tabla o tarjetas, lista y editor juntos o por separado).
// Solo entiende consultas de min-width y max-width en px, que son las que usa la web.
// ============================================================

/** Hace que window.matchMedia conteste como una ventana de `px` de ancho. */
export function simularAnchura(px: number) {
  window.matchMedia = ((consulta: string) => {
    const min = /min-width:\s*(\d+)px/.exec(consulta);
    const max = /max-width:\s*(\d+)px/.exec(consulta);
    const matches = (!min || px >= Number(min[1])) && (!max || px <= Number(max[1]));
    return {
      matches, media: consulta, onchange: null,
      addEventListener: () => {}, removeEventListener: () => {}, addListener: () => {}, removeListener: () => {},
      dispatchEvent: () => false,
    } as MediaQueryList;
  }) as typeof window.matchMedia;
}
