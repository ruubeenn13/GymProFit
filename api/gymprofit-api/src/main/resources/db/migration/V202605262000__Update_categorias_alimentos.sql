-- Actualiza las categorías de los alimentos del seed a los nuevos valores específicos
UPDATE alimentos SET categoria = 'Carnes y aves'   WHERE id IN (1, 7);
UPDATE alimentos SET categoria = 'Huevos'           WHERE id = 3;
UPDATE alimentos SET categoria = 'Cereales y pan'  WHERE id IN (2, 4);
UPDATE alimentos SET categoria = 'Aceites y grasas' WHERE id = 10;
