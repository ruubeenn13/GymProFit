-- ============================================================
-- Lote 1.6.6 (A1): las líneas que ya existían, con los macros que hoy se leen de ellas
--
-- Cada línea queda con lo que devolvía AlimentoComidaMapper: el valor del alimento por
-- sus gramos entre 100, redondeado a dos decimales con la mitad hacia arriba (ROUND en
-- MariaDB y en MySQL, sobre DECIMAL, es exactamente eso). Un macro sin dato era 0.
--
-- Después, los totales de cada comida con líneas, iguales a la suma de sus líneas, como
-- los deja recalcularTotales. Una comida sin líneas no se toca: sus totales, si los
-- tiene, los escribió un cliente antiguo a mano y no hay líneas con que rehacerlos.
--
-- No cambia ningún id y se puede ejecutar dos veces (MacrosLineaMigracionTest).
-- ============================================================

UPDATE alimentos_comida ac
    JOIN alimentos a ON a.id = ac.alimento_id
SET ac.proteinas_totales     = ROUND(COALESCE(a.proteinas, 0) * ac.cantidad_gramos / 100, 2),
    ac.carbohidratos_totales = ROUND(COALESCE(a.carbohidratos, 0) * ac.cantidad_gramos / 100, 2),
    ac.grasas_totales        = ROUND(COALESCE(a.grasas, 0) * ac.cantidad_gramos / 100, 2);

UPDATE comidas c
    JOIN (SELECT comida_id,
                 SUM(COALESCE(calorias_totales, 0)) AS kcal,
                 SUM(proteinas_totales)             AS p,
                 SUM(carbohidratos_totales)         AS hc,
                 SUM(grasas_totales)                AS g
          FROM alimentos_comida
          GROUP BY comida_id) t ON t.comida_id = c.id
SET c.total_calorias      = t.kcal,
    c.total_proteinas     = t.p,
    c.total_carbohidratos = t.hc,
    c.total_grasas        = t.g;
