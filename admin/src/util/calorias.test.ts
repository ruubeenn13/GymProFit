// Aviso de calorías: 4·P + 4·C + 9·G frente a las declaradas; avisa si se aleja
// más de un 15 % Y más de 10 kcal.
import { describe, expect, it } from 'vitest';
import { UMBRAL_ABSOLUTO_KCAL, UMBRAL_RELATIVO, comprobarCalorias } from './calorias';

describe('comprobarCalorias', () => {
  it('el yogur del diseño cuadra: 62,5 frente a 61', () => {
    const r = comprobarCalorias(61, 3.5, 4.7, 3.3)!;
    expect(r.calculadas).toBe(62.5);
    expect(r.diferencia).toBe(1.5);
    expect(r.seAleja).toBe(false);
  });

  it('avisa cuando se aleja más del 15 % y de 10 kcal', () => {
    // 4·20 + 4·10 + 9·10 = 210 frente a 120: 75 % y 90 kcal.
    expect(comprobarCalorias(120, 20, 10, 10)!.seAleja).toBe(true);
  });

  it('no avisa si pasa del 15 % pero no llega a 10 kcal', () => {
    // 4·5 = 20 frente a 12: 67 %, pero solo 8 kcal.
    expect(comprobarCalorias(12, 5, 0, 0)!.seAleja).toBe(false);
  });

  it('no avisa si pasa de 10 kcal pero no del 15 %', () => {
    // 4·100 + 9·10 = 490 frente a 450: 40 kcal, un 8,9 %.
    expect(comprobarCalorias(450, 100, 0, 10)!.seAleja).toBe(false);
  });

  it('en el borde exacto no avisa: tiene que ser más', () => {
    // 4·28,75 = 115 frente a 100: justo un 15 % y 15 kcal.
    expect(comprobarCalorias(100, 28.75, 0, 0)!.seAleja).toBe(false);
    expect(comprobarCalorias(100, 28.8, 0, 0)!.seAleja).toBe(true);
  });

  it('también avisa cuando los macros se quedan cortos', () => {
    // 9·10 = 90 frente a 200.
    expect(comprobarCalorias(200, 0, 0, 10)!.seAleja).toBe(true);
  });

  it('un macro vacío cuenta como 0', () => {
    expect(comprobarCalorias(884, null, null, 100)!.calculadas).toBe(900);
  });

  it('con 0 kcal declaradas y macros de verdad, avisa', () => {
    expect(comprobarCalorias(0, 10, 0, 0)!.seAleja).toBe(true);
    expect(comprobarCalorias(0, 0, 0, 0)!.seAleja).toBe(false);
  });

  it('sin calorías declaradas no hay nada que comparar', () => {
    expect(comprobarCalorias(null, 1, 1, 1)).toBeNull();
  });

  it('los umbrales son los de la especificación', () => {
    expect(UMBRAL_RELATIVO).toBe(0.15);
    expect(UMBRAL_ABSOLUTO_KCAL).toBe(10);
  });
});
