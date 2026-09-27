// ============================================================
// Formato de números y fechas, en español de España (GP-085)
// ============================================================

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const MESES_LARGOS = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto',
  'septiembre', 'octubre', 'noviembre', 'diciembre'];
const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

const numero = new Intl.NumberFormat('es-ES');
const decimal = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 });

/** 1240 → «1.240». */
export function entero(n: number): string {
  return numero.format(n);
}

/** 3.5 → «3,5»; null → «—». */
export function decimal1(n: number | null | undefined): string {
  return n === null || n === undefined ? '—' : decimal.format(n);
}

/** Fecha de calendario ISO (2026-09-21) sin pasar por zonas. */
export function deCalendario(iso: string): Date {
  const [a, m, d] = iso.split('-').map(Number);
  return new Date(a, m - 1, d);
}

/** «21 sep 2026». */
export function fechaCorta(fecha: Date): string {
  return `${fecha.getDate()} ${MESES[fecha.getMonth()]} ${fecha.getFullYear()}`;
}

/** «21 sep». */
export function diaMes(fecha: Date): string {
  return `${fecha.getDate()} ${MESES[fecha.getMonth()]}`;
}

/** «Domingo, 27 de septiembre». */
export function fechaLarga(fecha: Date): string {
  return `${DIAS[fecha.getDay()]}, ${fecha.getDate()} de ${MESES_LARGOS[fecha.getMonth()]}`;
}

function mismoDia(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function hora(fecha: Date): string {
  return `${String(fecha.getHours()).padStart(2, '0')}:${String(fecha.getMinutes()).padStart(2, '0')}`;
}

/**
 * Cuándo fue algo, como lo diría una persona: «Ahora», «Hoy, 18:40», «Ayer»,
 * «Hace 3 días», y a partir de un mes, la fecha.
 *
 * @param iso   instante ISO con zona, o null
 * @param ahora referencia (para los tests)
 */
export function haceCuanto(iso: string | null, ahora: Date = new Date()): string {
  if (!iso) return 'Sin registrar';
  const fecha = new Date(iso);
  const minutos = (ahora.getTime() - fecha.getTime()) / 60000;
  if (minutos < 2) return 'Ahora';
  if (mismoDia(fecha, ahora)) return `Hoy, ${hora(fecha)}`;
  const ayer = new Date(ahora);
  ayer.setDate(ahora.getDate() - 1);
  if (mismoDia(fecha, ayer)) return 'Ayer';
  const dias = Math.floor((new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate()).getTime()
    - new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate()).getTime()) / 86400000);
  if (dias <= 30) return `Hace ${dias} días`;
  return fechaCorta(fecha);
}

/** «hace 1 min», para la hora de la última comprobación. */
export function haceMinutos(desde: Date, ahora: Date = new Date()): string {
  const min = Math.floor((ahora.getTime() - desde.getTime()) / 60000);
  if (min < 1) return 'hace un momento';
  return min === 1 ? 'hace 1 min' : `hace ${min} min`;
}

/** Plural sencillo: (1, 'cuenta', 'cuentas') → «1 cuenta». */
export function cuenta(n: number, singular: string, plural: string): string {
  return `${entero(n)} ${n === 1 ? singular : plural}`;
}

/** Lee un número escrito con coma o punto. Vacío → null; texto que no es número → NaN. */
export function leerNumero(texto: string): number | null {
  const t = texto.trim().replace(',', '.');
  if (t === '') return null;
  return /^-?\d+(\.\d+)?$/.test(t) ? Number(t) : NaN;
}
