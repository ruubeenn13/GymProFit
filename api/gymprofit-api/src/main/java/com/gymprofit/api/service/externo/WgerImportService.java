package com.gymprofit.api.service.externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.exceptions.ExternalServiceException;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// ============================================================
// WgerImportService — import del catálogo de ejercicios desde wger
// wger (wger.de) es una API pública de fitness con ~800 ejercicios y
// traducciones comunitarias. Se importan a la BD local SOLO los que tienen
// nombre en ES y EN (~640), con upsert idempotente por wger_id: relanzar
// el import actualiza los existentes y añade los nuevos, sin duplicar.
// El catálogo local resultante alimenta rutinas/sesiones por FK como
// siempre (sin dependencia de red en el uso diario de la app).
// ============================================================
@Service
public class WgerImportService {

    private static final String EXERCISEINFO_URL =
            "https://wger.de/api/v2/exerciseinfo/?format=json&limit=%d&offset=%d";
    private static final int PAGE_SIZE = 100;
    // Ids de idioma en wger (endpoint /api/v2/language/): 2 = en, 4 = es
    private static final int LANG_EN = 2;
    private static final int LANG_ES = 4;

    // Categoría wger → enum GrupoMuscular propio
    private static final Map<String, GrupoMuscular> CATEGORIA_GRUPO = new HashMap<>();
    static {
        CATEGORIA_GRUPO.put("Abs", GrupoMuscular.ABDOMEN);
        CATEGORIA_GRUPO.put("Arms", GrupoMuscular.BRAZOS);
        CATEGORIA_GRUPO.put("Back", GrupoMuscular.ESPALDA);
        CATEGORIA_GRUPO.put("Calves", GrupoMuscular.PIERNAS);
        CATEGORIA_GRUPO.put("Cardio", GrupoMuscular.CARDIO);
        CATEGORIA_GRUPO.put("Chest", GrupoMuscular.PECHO);
        CATEGORIA_GRUPO.put("Legs", GrupoMuscular.PIERNAS);
        CATEGORIA_GRUPO.put("Shoulders", GrupoMuscular.HOMBROS);
    }

    // Equipo wger (EN) → texto ES mostrado en la app
    private static final Map<String, String> EQUIPO_ES = new HashMap<>();
    static {
        EQUIPO_ES.put("Barbell", "Barra");
        EQUIPO_ES.put("SZ-Bar", "Barra Z");
        EQUIPO_ES.put("Dumbbell", "Mancuernas");
        EQUIPO_ES.put("Gym mat", "Esterilla");
        EQUIPO_ES.put("Swiss Ball", "Fitball");
        EQUIPO_ES.put("Pull-up bar", "Barra de dominadas");
        EQUIPO_ES.put("Bench", "Banco");
        EQUIPO_ES.put("Incline bench", "Banco inclinado");
        EQUIPO_ES.put("Kettlebell", "Kettlebell");
        EQUIPO_ES.put("Resistance band", "Banda elástica");
        EQUIPO_ES.put("none (bodyweight exercise)", "Sin equipo");
    }

    // Estimación de kcal por serie según grupo muscular (heurística honesta:
    // wger no aporta gasto calórico; rangos coherentes con el catálogo previo)
    private static final Map<GrupoMuscular, Integer> KCAL_GRUPO = Map.of(
            GrupoMuscular.CARDIO, 10, GrupoMuscular.PIERNAS, 8, GrupoMuscular.ESPALDA, 8,
            GrupoMuscular.FULLBODY, 9, GrupoMuscular.PECHO, 6, GrupoMuscular.HOMBROS, 6,
            GrupoMuscular.BRAZOS, 5, GrupoMuscular.ABDOMEN, 5);

    private final IEjercicioRepository ejercicioRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(WgerImportService.class);

    // Resumen del import devuelto al panel admin.
    public record ImportResumen(int nuevos, int actualizados, int omitidos) {}

    public WgerImportService(IEjercicioRepository ejercicioRepository, ObjectMapper objectMapper) {
        this.ejercicioRepository = ejercicioRepository;
        this.objectMapper = objectMapper;
        // Import batch (no interactivo) → timeouts más generosos
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // Recorre todo el catálogo de wger paginado y hace upsert por wger_id de
    // los ejercicios con traducción ES+EN. Omite los que no son bilingües.
    @Transactional
    public ImportResumen importarCatalogo() {
        logger.info("Iniciando import del catálogo de ejercicios desde wger");

        int nuevos = 0, actualizados = 0, omitidos = 0;
        int offset = 0;
        boolean hayMas = true;

        while (hayMas) {
            JsonNode pagina = descargarPagina(offset);
            for (JsonNode ejercicioWger : pagina.path("results")) {
                Optional<Ejercicio> mapeado = mapear(ejercicioWger);
                if (mapeado.isEmpty()) {
                    omitidos++;
                    continue;
                }
                Ejercicio nuevo = mapeado.get();
                Optional<Ejercicio> existente = ejercicioRepository.findByWgerId(nuevo.getWgerId());
                if (existente.isPresent()) {
                    copiarCampos(nuevo, existente.get());
                    ejercicioRepository.save(existente.get());
                    actualizados++;
                } else {
                    ejercicioRepository.save(nuevo);
                    nuevos++;
                }
            }
            hayMas = !pagina.path("next").isNull() && pagina.path("next").isTextual();
            offset += PAGE_SIZE;
        }

        logger.info("Import wger completado: {} nuevos, {} actualizados, {} omitidos",
                nuevos, actualizados, omitidos);
        return new ImportResumen(nuevos, actualizados, omitidos);
    }

    // Descarga una página del endpoint exerciseinfo; fallo → 502.
    private JsonNode descargarPagina(int offset) {
        try {
            String body = restClient.get()
                    .uri(String.format(EXERCISEINFO_URL, PAGE_SIZE, offset))
                    .retrieve().body(String.class);
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new ExternalServiceException("wger no disponible (offset " + offset + "): " + ex.getMessage(), ex);
        }
    }

    // Convierte un exerciseinfo de wger en una entidad Ejercicio local.
    // Devuelve vacío si falta la traducción ES o EN (no bilingüe → se omite).
    private Optional<Ejercicio> mapear(JsonNode ejercicioWger) {
        JsonNode es = traduccion(ejercicioWger, LANG_ES);
        JsonNode en = traduccion(ejercicioWger, LANG_EN);
        if (es == null || en == null) return Optional.empty();

        String nombreEs = es.path("name").asText("").trim();
        String nombreEn = en.path("name").asText("").trim();
        if (nombreEs.isEmpty() || nombreEn.isEmpty()) return Optional.empty();

        GrupoMuscular grupo = CATEGORIA_GRUPO.getOrDefault(
                ejercicioWger.path("category").path("name").asText(""), GrupoMuscular.FULLBODY);

        Ejercicio e = new Ejercicio();
        e.setWgerId(ejercicioWger.path("id").asInt());
        e.setNombre(truncar(nombreEs, 100));
        e.setNombreEn(truncar(nombreEn, 100));
        // wger trae una única descripción HTML por idioma → texto plano; se usa
        // como descripción e instrucciones (no distingue ambos conceptos)
        String descEs = htmlATexto(es.path("description").asText(""));
        String descEn = htmlATexto(en.path("description").asText(""));
        e.setDescripcion(descEs.isBlank() ? null : descEs);
        e.setDescripcionEn(descEn.isBlank() ? null : descEn);
        e.setInstrucciones(descEs.isBlank() ? null : descEs);
        e.setInstruccionesEn(descEn.isBlank() ? null : descEn);
        e.setGrupoMuscular(grupo);
        e.setDificultad(estimarDificultad(ejercicioWger, grupo));
        e.setCaloriasQuemadas(KCAL_GRUPO.getOrDefault(grupo, 6));
        e.setEquipoNecesario(equipo(ejercicioWger, true));
        e.setEquipoNecesarioEn(equipo(ejercicioWger, false));
        e.setImagenUrl(imagenPrincipal(ejercicioWger));
        e.setActivo(true);
        return Optional.of(e);
    }

    // Traducción de un idioma concreto dentro del array translations de wger.
    private JsonNode traduccion(JsonNode ejercicioWger, int idioma) {
        for (JsonNode t : ejercicioWger.path("translations")) {
            if (t.path("language").asInt() == idioma) return t;
        }
        return null;
    }

    // Heurística de dificultad (wger no la aporta): cardio y ejercicios sin
    // equipo → PRINCIPIANTE; con barra → INTERMEDIO; resto → INTERMEDIO.
    private Dificultad estimarDificultad(JsonNode ejercicioWger, GrupoMuscular grupo) {
        if (grupo == GrupoMuscular.CARDIO) return Dificultad.PRINCIPIANTE;
        JsonNode equipos = ejercicioWger.path("equipment");
        if (equipos.isEmpty()) return Dificultad.PRINCIPIANTE;
        for (JsonNode eq : equipos) {
            String nombre = eq.path("name").asText("");
            if (nombre.equals("Gym mat") || nombre.equals("none (bodyweight exercise)")) continue;
            return Dificultad.INTERMEDIO;
        }
        return Dificultad.PRINCIPIANTE;
    }

    // Lista de equipo separada por comas, en ES (mapa) o EN (nombre wger).
    private String equipo(JsonNode ejercicioWger, boolean enEspanol) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode eq : ejercicioWger.path("equipment")) {
            String nombre = eq.path("name").asText("");
            if (nombre.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(enEspanol ? EQUIPO_ES.getOrDefault(nombre, nombre) : nombre);
        }
        if (sb.length() == 0) return enEspanol ? "Sin equipo" : "No equipment";
        return truncar(sb.toString(), 255);
    }

    // URL de la imagen principal del ejercicio (is_main) o la primera; null si no hay.
    private String imagenPrincipal(JsonNode ejercicioWger) {
        String primera = null;
        for (JsonNode img : ejercicioWger.path("images")) {
            String url = img.path("image").asText("");
            if (url.isEmpty()) continue;
            if (primera == null) primera = url;
            if (img.path("is_main").asBoolean(false)) return url;
        }
        return primera;
    }

    // Copia los campos importables sobre la entidad ya existente (upsert).
    private void copiarCampos(Ejercicio origen, Ejercicio destino) {
        destino.setNombre(origen.getNombre());
        destino.setNombreEn(origen.getNombreEn());
        destino.setDescripcion(origen.getDescripcion());
        destino.setDescripcionEn(origen.getDescripcionEn());
        destino.setInstrucciones(origen.getInstrucciones());
        destino.setInstruccionesEn(origen.getInstruccionesEn());
        destino.setGrupoMuscular(origen.getGrupoMuscular());
        destino.setDificultad(origen.getDificultad());
        destino.setCaloriasQuemadas(origen.getCaloriasQuemadas());
        destino.setEquipoNecesario(origen.getEquipoNecesario());
        destino.setEquipoNecesarioEn(origen.getEquipoNecesarioEn());
        destino.setImagenUrl(origen.getImagenUrl());
        destino.setActivo(true);
    }

    // Pasa el HTML de wger (<p>, <li>, &nbsp;...) a texto plano legible.
    private String htmlATexto(String html) {
        if (html == null) return "";
        return html
                .replaceAll("(?i)</p>|<br ?/?>", "\n")
                .replaceAll("(?i)<li>", "\n- ")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    // Recorta un texto a la longitud máxima de su columna.
    private String truncar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
