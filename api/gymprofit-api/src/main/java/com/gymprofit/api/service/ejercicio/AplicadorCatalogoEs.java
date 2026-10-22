package com.gymprofit.api.service.ejercicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// AplicadorCatalogoEs — lleva el CSV del español a los ejercicios que ya hay en la base
// (GP-217)
//
// Lo llama la migración V202610221810 y los tests. JDBC a pelo, sin JPA: corre dentro de
// Flyway, antes de que exista el contexto, como SembradorPlantillas.
//
// Solo escribe nombre y descripcion (el español). Nunca toca las columnas *_en, las
// imágenes, el equipamiento, el grupo muscular ni la dificultad. Es idempotente: una fila
// que ya dice lo que dice el CSV no se escribe. Lo que el CSV trae vacío (los ejercicios
// sin texto original que traducir) no borra la descripción que haya.
// ============================================================
public final class AplicadorCatalogoEs {

    private static final Logger log = LoggerFactory.getLogger(AplicadorCatalogoEs.class);

    private AplicadorCatalogoEs() { }

    /**
     * Lo que ha hecho la aplicación.
     *
     * @param enElCsv       ejercicios que trae el CSV.
     * @param actualizados  filas de la base que cambiaron.
     * @param sinCambios    filas que ya decían lo mismo.
     * @param sinFila       fed_id del CSV que no están en la base (no es un error: la CI y las bases
     *                      nuevas no traen el catálogo).
     */
    public record Resultado(int enElCsv, int actualizados, int sinCambios, List<String> sinFila) { }

    /**
     * Aplica el CSV a la conexión dada, sin hacer commit (lo hace quien llama).
     */
    public static Resultado aplicar(Connection c, CatalogoEs catalogo) throws SQLException {
        Map<String, String[]> enBase = new HashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT fed_id, nombre, descripcion FROM ejercicios WHERE fed_id IS NOT NULL")) {
            while (rs.next()) {
                enBase.put(rs.getString("fed_id"), new String[]{rs.getString("nombre"), rs.getString("descripcion")});
            }
        }
        int actualizados = 0, sinCambios = 0;
        List<String> sinFila = new ArrayList<>();
        try (PreparedStatement up = c.prepareStatement("UPDATE ejercicios SET nombre = ?, descripcion = ? WHERE fed_id = ?")) {
            for (CatalogoEs.Entrada e : catalogo.todas()) {
                String[] actual = enBase.get(e.fedId());
                if (actual == null) {
                    sinFila.add(e.fedId());
                    continue;
                }
                // Sin texto original no hay descripción nueva: se deja la que haya.
                String descripcion = e.descripcion().isEmpty() ? actual[1] : e.descripcion();
                if (e.nombre().equals(actual[0]) && java.util.Objects.equals(descripcion, actual[1])) {
                    sinCambios++;
                    continue;
                }
                up.setString(1, e.nombre());
                up.setString(2, descripcion);
                up.setString(3, e.fedId());
                up.addBatch();
                actualizados++;
            }
            up.executeBatch();
        }
        log.info("Catálogo en español: {} en el CSV, {} actualizados, {} ya estaban, {} sin fila en la base",
                catalogo.tamano(), actualizados, sinCambios, sinFila.size());
        return new Resultado(catalogo.tamano(), actualizados, sinCambios, sinFila);
    }
}
