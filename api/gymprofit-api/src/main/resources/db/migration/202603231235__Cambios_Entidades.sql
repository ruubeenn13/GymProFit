ALTER TABLE mediciones_corporales
    MODIFY fecha datetime NOT NULL;

ALTER TABLE sesiones_entrenamiento
    MODIFY fecha_fin datetime NOT NULL;