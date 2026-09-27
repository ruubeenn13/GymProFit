// ============================================================
// Comprobación de calorías de un alimento (GP-085)
//
// Las calorías declaradas deberían salir, aproximadamente, de los macros:
// 4 kcal por gramo de proteína y de carbohidrato y 9 por gramo de grasa (factores
// de Atwater). Si se alejan mucho, hay una errata en alguno de los números. El
// aviso no bloquea: la fibra, el alcohol o los polialcoholes explican diferencias
// legítimas, y quien edita decide.
// ============================================================

/** Diferencia relativa a partir de la cual se avisa: 15 %. */
export const UMBRAL_RELATIVO = 0.15;

/** Diferencia absoluta mínima para avisar: por debajo de 10 kcal no merece la pena. */
export const UMBRAL_ABSOLUTO_KCAL = 10;

export interface ComprobacionCalorias {
  /** kcal que salen de los macros. */
  calculadas: number;
  /** Diferencia con las declaradas (calculadas − declaradas). */
  diferencia: number;
  /** true si se aleja más del 15 % y de 10 kcal. */
  seAleja: boolean;
}

/**
 * Compara las calorías declaradas con las que salen de los macros. Un macro vacío
 * cuenta como 0. Sin calorías declaradas no hay nada que comparar: devuelve null.
 *
 * @param declaradas  kcal por 100 g
 * @param proteinas   g por 100 g
 * @param carbohidratos g por 100 g
 * @param grasas      g por 100 g
 */
export function comprobarCalorias(
  declaradas: number | null,
  proteinas: number | null,
  carbohidratos: number | null,
  grasas: number | null,
): ComprobacionCalorias | null {
  if (declaradas === null || !Number.isFinite(declaradas)) return null;
  const calculadas = 4 * (proteinas ?? 0) + 4 * (carbohidratos ?? 0) + 9 * (grasas ?? 0);
  const diferencia = calculadas - declaradas;
  const absoluta = Math.abs(diferencia);
  // Relativa sobre las declaradas; con 0 declaradas cualquier macro es infinitamente lejos.
  const relativa = declaradas === 0 ? (absoluta === 0 ? 0 : Infinity) : absoluta / declaradas;
  return {
    calculadas: Math.round(calculadas * 10) / 10,
    diferencia: Math.round(diferencia * 10) / 10,
    seAleja: relativa > UMBRAL_RELATIVO && absoluta > UMBRAL_ABSOLUTO_KCAL,
  };
}
