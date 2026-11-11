-- GP-204: el IMC se guardaba como 0,00 porque se dividía el peso entre la altura en
-- centímetros al cuadrado. Se recalcula con la regla de CalculoImc, que es la que usa
-- ya el servicio: altura en cm → metros, a un decimal con la mitad hacia arriba, y
-- nulo si falta el peso o la altura, si alguno no es positivo o si el resultado no es
-- un IMC posible (100 o más, que además no cabe en DECIMAL(4,2): sale de una altura
-- en metros). Se aplica a todas las filas, también a las que estaban bien, para que
-- no quede ningún IMC escrito con la cuenta de antes. Ejecutarla dos veces deja lo mismo.
UPDATE mediciones_corporales
SET imc = CASE
    WHEN peso > 0 AND altura > 0
         AND ROUND(peso / ((altura / 100) * (altura / 100)), 1) < 100
        THEN ROUND(peso / ((altura / 100) * (altura / 100)), 1)
    ELSE NULL
END;
