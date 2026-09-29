package com.gymprofit.api.service.programa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// ============================================================
// SembradorPlantillas — siembra el catálogo de programas y plantillas (GP-074)
//
// Lo llama la migración V202609292002 con una copia FIJA del catálogo
// (db/semillas/catalogo-plantillas-v1.json), no con el de documentacion/: si el
// catálogo cambia, va en una migración nueva. Está aparte de la migración para que los
// tests puedan volver a sembrar sobre una base con los ejercicios del catálogo, que la
// de CI no trae.
//
// JDBC a pelo, sin JPA: corre dentro de Flyway, antes de que exista el contexto.
//
// Cada ejercicio se busca por fed_id. Si falta o está inactivo NO se para: la rutina se
// siembra sin él y queda en el log y en el informe. Además deja revisados los nombres en
// español de los ejercicios del catálogo, el equipamiento que corrige el JSON y las
// descripciones que el JSON trae para cuando la base no tiene ninguna.
// ============================================================
public final class SembradorPlantillas {

    private static final Logger log = LoggerFactory.getLogger(SembradorPlantillas.class);

    private SembradorPlantillas() { }

    /**
     * Lo que ha hecho la siembra.
     *
     * @param programas         programas sembrados.
     * @param rutinas           plantillas sembradas.
     * @param ejercicios        filas de rutina_ejercicio sembradas.
     * @param omitidos          ejercicios que no se sembraron, «RUTINA · fedId (motivo)».
     * @param nombresCambiados  nombres en español que cambian, «fedId: antes → después».
     */
    public record Informe(int programas, int rutinas, int ejercicios,
                          List<String> omitidos, List<String> nombresCambiados) { }

    // Un ejercicio del catálogo de la base, tal como lo necesita la siembra.
    private record EjercicioBase(int id, boolean activo, String nombre) { }

    /**
     * Siembra el catálogo en la conexión dada, sin hacer commit (lo hace quien llama).
     *
     * @param conexion conexión abierta; Flyway o la transacción del test.
     * @param json     el catálogo, con el formato de catalogo-plantillas.json.
     * @return el informe de lo sembrado.
     */
    public static Informe sembrar(Connection conexion, InputStream json) throws SQLException, IOException {
        JsonNode catalogo = new ObjectMapper().readTree(json);

        Map<String, EjercicioBase> ejercicios = cargarEjercicios(conexion);
        List<String> nombresCambiados = revisarEjercicios(conexion, catalogo.get("ejercicios"), ejercicios);

        List<String> omitidos = new ArrayList<>();
        Map<String, Integer> rutinaPorCodigo = new HashMap<>();
        int filas = 0;
        for (JsonNode rutina : catalogo.get("rutinas")) {
            int id = insertarRutina(conexion, rutina);
            rutinaPorCodigo.put(rutina.get("codigo").asText(), id);
            filas += insertarEjercicios(conexion, id, rutina, ejercicios, omitidos);
        }

        int programas = 0;
        for (JsonNode programa : catalogo.get("programas")) {
            insertarPrograma(conexion, programa, rutinaPorCodigo);
            programas++;
        }

        for (String o : omitidos) {
            log.warn("Semilla de plantillas: ejercicio omitido, {}", o);
        }
        log.info("Semilla de plantillas: {} programas, {} rutinas, {} ejercicios, {} omitidos, {} nombres cambiados",
                programas, rutinaPorCodigo.size(), filas, omitidos.size(), nombresCambiados.size());
        return new Informe(programas, rutinaPorCodigo.size(), filas, omitidos, nombresCambiados);
    }

    private static Map<String, EjercicioBase> cargarEjercicios(Connection c) throws SQLException {
        Map<String, EjercicioBase> mapa = new HashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, fed_id, activo, nombre FROM ejercicios WHERE fed_id IS NOT NULL")) {
            while (rs.next()) {
                mapa.put(rs.getString("fed_id"),
                        new EjercicioBase(rs.getInt("id"), rs.getBoolean("activo"), rs.getString("nombre")));
            }
        }
        return mapa;
    }

    // Nombres en español revisados, equipamiento corregido y descripciones que faltan.
    private static List<String> revisarEjercicios(Connection c, JsonNode lista,
                                                  Map<String, EjercicioBase> ejercicios) throws SQLException {
        List<String> cambiados = new ArrayList<>();
        for (JsonNode e : lista) {
            String fedId = e.get("fedId").asText();
            EjercicioBase base = ejercicios.get(fedId);
            if (base == null) continue; // ya sale en el informe al sembrar su rutina

            String nombre = e.get("nombre").asText();
            if (!Objects.equals(base.nombre(), nombre)) {
                cambiados.add(fedId + ": " + base.nombre() + " → " + nombre);
            }
            actualizar(c, "UPDATE ejercicios SET nombre = ?, nombre_revisado = 1 WHERE id = ?", nombre, base.id());

            if (e.hasNonNull("equipamiento")) {
                actualizar(c, "UPDATE ejercicios SET equipamiento = ? WHERE id = ?",
                        e.get("equipamiento").asText(), base.id());
            }
            if (e.hasNonNull("descripcionSiVacia")) {
                actualizar(c, "UPDATE ejercicios SET descripcion = ? WHERE id = ? "
                        + "AND (descripcion IS NULL OR TRIM(descripcion) = '')",
                        e.get("descripcionSiVacia").asText(), base.id());
            }
            if (e.hasNonNull("descripcionEnSiVacia")) {
                actualizar(c, "UPDATE ejercicios SET descripcion_en = ? WHERE id = ? "
                        + "AND (descripcion_en IS NULL OR TRIM(descripcion_en) = '')",
                        e.get("descripcionEnSiVacia").asText(), base.id());
            }
        }
        return cambiados;
    }

    private static void actualizar(Connection c, String sql, String valor, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private static int insertarRutina(Connection c, JsonNode r) throws SQLException {
        String sql = """
                INSERT INTO rutinas (nombre, nombre_en, descripcion, descripcion_en, duracion_minutos, nivel,
                                     es_predefinida, categoria, categoria_en, fecha_creacion, activa,
                                     usuario_id, codigo, es_plantilla)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, NOW(), 1, NULL, ?, 1)""";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.get("nombre").asText());
            ps.setString(2, r.get("nombreEn").asText());
            ps.setString(3, r.get("descripcion").asText());
            ps.setString(4, r.get("descripcionEn").asText());
            ps.setInt(5, r.get("duracionMinutos").asInt());
            ps.setString(6, r.get("nivel").asText());
            ps.setString(7, r.get("categoria").asText());
            ps.setString(8, r.get("categoriaEn").asText());
            ps.setString(9, r.get("codigo").asText());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    private static int insertarEjercicios(Connection c, int rutinaId, JsonNode rutina,
                                          Map<String, EjercicioBase> ejercicios,
                                          List<String> omitidos) throws SQLException {
        String sql = """
                INSERT INTO rutina_ejercicio (series, repeticiones, tiempo_descanso, orden, notas, notas_en,
                                              rutina_id, ejercicio_id, repeticiones_min, repeticiones_max,
                                              medida, tipo, por_lado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        String codigo = rutina.get("codigo").asText();
        int filas = 0;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (JsonNode e : rutina.get("ejercicios")) {
                String fedId = e.get("fedId").asText();
                EjercicioBase base = ejercicios.get(fedId);
                if (base == null || !base.activo()) {
                    omitidos.add(codigo + " · " + fedId + (base == null ? " (no está en la base)" : " (inactivo)"));
                    continue;
                }
                ps.setInt(1, e.get("series").asInt());
                // repeticiones de siempre lleva el máximo, para las builds viejas.
                ps.setInt(2, e.get("max").asInt());
                ps.setInt(3, e.get("descansoSegundos").asInt());
                ps.setInt(4, e.get("orden").asInt());
                texto(ps, 5, e.get("nota"));
                texto(ps, 6, e.get("notaEn"));
                ps.setInt(7, rutinaId);
                ps.setInt(8, base.id());
                ps.setInt(9, e.get("min").asInt());
                ps.setInt(10, e.get("max").asInt());
                ps.setString(11, e.get("medida").asText());
                ps.setString(12, e.get("tipo").asText());
                texto(ps, 13, e.get("porLado"));
                ps.executeUpdate();
                filas++;
            }
        }
        return filas;
    }

    private static void texto(PreparedStatement ps, int i, JsonNode valor) throws SQLException {
        if (valor == null || valor.isNull()) ps.setNull(i, Types.VARCHAR);
        else ps.setString(i, valor.asText());
    }

    private static void insertarPrograma(Connection c, JsonNode p, Map<String, Integer> rutinas) throws SQLException {
        int programaId;
        String sql = """
                INSERT INTO programas (codigo, nombre, nombre_en, descripcion, descripcion_en, nivel,
                                       equipamiento, dias_min, dias_max, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)""";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.get("codigo").asText());
            ps.setString(2, p.get("nombre").asText());
            ps.setString(3, p.get("nombreEn").asText());
            ps.setString(4, p.get("descripcion").asText());
            ps.setString(5, p.get("descripcionEn").asText());
            ps.setString(6, p.get("nivel").asText());
            ps.setString(7, p.get("equipamiento").asText());
            ps.setInt(8, p.get("diasMin").asInt());
            ps.setInt(9, p.get("diasMax").asInt());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                programaId = k.getInt(1);
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO programa_rutina (programa_id, rutina_id, posicion) VALUES (?, ?, ?)")) {
            int posicion = 1;
            for (JsonNode codigo : p.get("semana")) {
                Integer rutinaId = rutinas.get(codigo.asText());
                if (rutinaId == null) {
                    // El JSON es coherente (lo comprueba un test); si no lo fuera, mejor
                    // que la migración falle a que el programa quede con un día de menos.
                    throw new IllegalStateException("El programa " + p.get("codigo").asText()
                            + " pide la rutina " + codigo.asText() + ", que no está en el catálogo");
                }
                ps.setInt(1, programaId);
                ps.setInt(2, rutinaId);
                ps.setInt(3, posicion++);
                ps.executeUpdate();
            }
        }
    }
}
