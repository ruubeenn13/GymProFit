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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    // free-exercise-db: ~870 ejercicios con demostración de 2 fotogramas
    // ("monigote") e instrucciones paso a paso. Se cruza por nombre EN.
    private static final String FED_JSON_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json";
    private static final String FED_IMG_BASE =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/";

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

    // ── free-exercise-db → catálogo propio (fuente PRECISA cuando hay match) ──
    // Músculo primario FED → nombre ES mostrado en el detalle
    private static final Map<String, String> MUSCULO_ES = new HashMap<>();
    // Músculo primario FED → nombre EN mostrado en el detalle
    private static final Map<String, String> MUSCULO_EN = new HashMap<>();
    // Músculo primario FED → grupo grueso (para el chip de filtro)
    private static final Map<String, GrupoMuscular> MUSCULO_GRUPO = new HashMap<>();
    static {
        reg("abdominals", "Abdominales", "Abs", GrupoMuscular.ABDOMEN);
        reg("abductors", "Abductores", "Abductors", GrupoMuscular.PIERNAS);
        reg("adductors", "Aductores", "Adductors", GrupoMuscular.PIERNAS);
        reg("biceps", "Bíceps", "Biceps", GrupoMuscular.BRAZOS);
        reg("calves", "Gemelos", "Calves", GrupoMuscular.PIERNAS);
        reg("chest", "Pecho", "Chest", GrupoMuscular.PECHO);
        reg("forearms", "Antebrazos", "Forearms", GrupoMuscular.BRAZOS);
        reg("glutes", "Glúteos", "Glutes", GrupoMuscular.PIERNAS);
        reg("hamstrings", "Isquiotibiales", "Hamstrings", GrupoMuscular.PIERNAS);
        reg("lats", "Dorsales", "Lats", GrupoMuscular.ESPALDA);
        reg("lower back", "Lumbares", "Lower back", GrupoMuscular.ESPALDA);
        reg("middle back", "Espalda media", "Middle back", GrupoMuscular.ESPALDA);
        reg("neck", "Cuello", "Neck", GrupoMuscular.HOMBROS);
        reg("quadriceps", "Cuádriceps", "Quadriceps", GrupoMuscular.PIERNAS);
        reg("shoulders", "Hombros", "Shoulders", GrupoMuscular.HOMBROS);
        reg("traps", "Trapecios", "Traps", GrupoMuscular.ESPALDA);
        reg("triceps", "Tríceps", "Triceps", GrupoMuscular.BRAZOS);
    }
    private static void reg(String fed, String es, String en, GrupoMuscular grupo) {
        MUSCULO_ES.put(fed, es);
        MUSCULO_EN.put(fed, en);
        MUSCULO_GRUPO.put(fed, grupo);
    }

    // Músculo de wger (por id) → clave FED, para reutilizar MUSCULO_ES/EN/GRUPO.
    // Es el fallback de precisión cuando el ejercicio no matchea free-exercise-db.
    private static final Map<Integer, String> WGER_MUSCULO = new HashMap<>();
    static {
        WGER_MUSCULO.put(2, "shoulders");   // Anterior deltoid
        WGER_MUSCULO.put(1, "biceps");      // Biceps brachii
        WGER_MUSCULO.put(11, "hamstrings"); // Biceps femoris
        WGER_MUSCULO.put(13, "biceps");     // Brachialis → brazo
        WGER_MUSCULO.put(7, "calves");      // Gastrocnemius
        WGER_MUSCULO.put(8, "glutes");      // Gluteus maximus
        WGER_MUSCULO.put(12, "lats");       // Latissimus dorsi
        WGER_MUSCULO.put(14, "abdominals"); // Obliquus externus
        WGER_MUSCULO.put(4, "chest");       // Pectoralis major
        WGER_MUSCULO.put(10, "quadriceps"); // Quadriceps femoris
        WGER_MUSCULO.put(6, "abdominals");  // Rectus abdominis
        WGER_MUSCULO.put(3, "chest");       // Serratus anterior → pecho aprox
        WGER_MUSCULO.put(15, "calves");     // Soleus
        WGER_MUSCULO.put(9, "traps");       // Trapezius
        WGER_MUSCULO.put(5, "triceps");     // Triceps brachii
    }

    // Equipo FED → texto ES / EN (más limpio que el de wger)
    private static final Map<String, String[]> EQUIPO_FED = new HashMap<>();
    static {
        EQUIPO_FED.put("body only", new String[]{"Sin equipo", "Bodyweight"});
        EQUIPO_FED.put("machine", new String[]{"Máquina", "Machine"});
        EQUIPO_FED.put("dumbbell", new String[]{"Mancuernas", "Dumbbell"});
        EQUIPO_FED.put("barbell", new String[]{"Barra", "Barbell"});
        EQUIPO_FED.put("cable", new String[]{"Polea", "Cable"});
        EQUIPO_FED.put("kettlebells", new String[]{"Kettlebell", "Kettlebell"});
        EQUIPO_FED.put("bands", new String[]{"Banda elástica", "Resistance band"});
        EQUIPO_FED.put("medicine ball", new String[]{"Balón medicinal", "Medicine ball"});
        EQUIPO_FED.put("exercise ball", new String[]{"Fitball", "Exercise ball"});
        EQUIPO_FED.put("e-z curl bar", new String[]{"Barra Z", "EZ-bar"});
        EQUIPO_FED.put("foam roll", new String[]{"Rodillo de espuma", "Foam roller"});
    }

    private final IEjercicioRepository ejercicioRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(WgerImportService.class);

    // Resumen del import devuelto al panel admin.
    public record ImportResumen(int nuevos, int actualizados, int omitidos, int desactivados) {}

    // Ejercicio de free-exercise-db relevante para el cruce: 2 fotogramas,
    // nivel/músculo/equipo REALES e instrucciones EN paso a paso.
    private record FedEjercicio(String imagen1, String imagen2, String nivel,
                                String instrucciones, String musculo, String equipo) {}

    // Entrada del índice difuso: tokens del nombre FED + su ejercicio.
    private record FedTokens(Set<String> tokens, FedEjercicio ejercicio) {}

    public WgerImportService(IEjercicioRepository ejercicioRepository, ObjectMapper objectMapper) {
        this.ejercicioRepository = ejercicioRepository;
        this.objectMapper = objectMapper;
        // Import batch (no interactivo) → timeouts más generosos
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // Recorre todo el catálogo de wger paginado y hace upsert por wger_id.
    // Criterios de entrada al catálogo: traducción ES+EN Y demostración
    // visual (fotogramas de free-exercise-db cruzados por nombre EN, o al
    // menos imagen de wger). Los importados que dejen de cumplirlos se
    // desactivan al final.
    @Transactional
    public ImportResumen importarCatalogo() {
        logger.info("Iniciando import del catálogo de ejercicios (wger + free-exercise-db)");

        Map<String, FedEjercicio> fedExacto = new HashMap<>();
        List<FedTokens> fedIndice = new ArrayList<>();
        cargarFreeExerciseDb(fedExacto, fedIndice);

        int nuevos = 0, actualizados = 0, omitidos = 0;
        Set<Integer> vistos = new HashSet<>();
        int offset = 0;
        boolean hayMas = true;

        while (hayMas) {
            JsonNode pagina = descargarPagina(offset);
            for (JsonNode ejercicioWger : pagina.path("results")) {
                Optional<Ejercicio> mapeado = mapear(ejercicioWger, fedExacto, fedIndice);
                if (mapeado.isEmpty()) {
                    omitidos++;
                    continue;
                }
                Ejercicio nuevo = mapeado.get();
                vistos.add(nuevo.getWgerId());
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

        // Importados en ejecuciones previas que ya no cumplen los criterios
        int desactivados = vistos.isEmpty() ? 0 : ejercicioRepository.desactivarWgerNoVistos(vistos);

        logger.info("Import completado: {} nuevos, {} actualizados, {} omitidos, {} desactivados",
                nuevos, actualizados, omitidos, desactivados);
        return new ImportResumen(nuevos, actualizados, omitidos, desactivados);
    }

    // Descarga free-exercise-db y construye el índice de cruce por nombre EN:
    // clave exacta normalizada + índice de tokens para el match difuso.
    private void cargarFreeExerciseDb(Map<String, FedEjercicio> exacto, List<FedTokens> indice) {
        try {
            String body = restClient.get().uri(java.net.URI.create(FED_JSON_URL))
                    .retrieve().body(String.class);
            JsonNode raiz = objectMapper.readTree(body);
            for (JsonNode e : raiz) {
                JsonNode imagenes = e.path("images");
                if (!imagenes.isArray() || imagenes.isEmpty()) continue;

                String img1 = FED_IMG_BASE + imagenes.get(0).asText();
                String img2 = imagenes.size() > 1 ? FED_IMG_BASE + imagenes.get(1).asText() : null;
                // Primer músculo primario (el más representativo)
                JsonNode musculos = e.path("primaryMuscles");
                String musculo = musculos.isArray() && !musculos.isEmpty() ? musculos.get(0).asText() : null;
                FedEjercicio fed = new FedEjercicio(img1, img2,
                        e.path("level").asText(""), instruccionesFed(e),
                        musculo, e.path("equipment").asText(null));

                String nombre = e.path("name").asText("");
                exacto.put(claveExacta(nombre), fed);
                Set<String> tokens = tokens(nombre);
                if (!tokens.isEmpty()) indice.add(new FedTokens(tokens, fed));
            }
            logger.info("free-exercise-db cargado: {} ejercicios con demostración", exacto.size());
        } catch (Exception ex) {
            throw new ExternalServiceException("free-exercise-db no disponible: " + ex.getMessage(), ex);
        }
    }

    // Instrucciones EN de FED como pasos numerados "1. ...\n2. ..."
    private String instruccionesFed(JsonNode e) {
        StringBuilder sb = new StringBuilder();
        int paso = 1;
        for (JsonNode inst : e.path("instructions")) {
            String texto = inst.asText("").trim();
            if (texto.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(paso++).append(". ").append(texto);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // Busca la demostración FED para un nombre EN de wger: primero clave
    // exacta normalizada, después match difuso por solapamiento de tokens
    // (Jaccard >= 0.75 con al menos 2 tokens comunes).
    private FedEjercicio buscarFed(String nombreEn, Map<String, FedEjercicio> exacto, List<FedTokens> indice) {
        FedEjercicio porClave = exacto.get(claveExacta(nombreEn));
        if (porClave != null) return porClave;

        Set<String> tokens = tokens(nombreEn);
        if (tokens.size() < 2) return null;
        for (FedTokens candidato : indice) {
            Set<String> interseccion = new HashSet<>(tokens);
            interseccion.retainAll(candidato.tokens());
            if (interseccion.size() < 2) continue;
            Set<String> union = new HashSet<>(tokens);
            union.addAll(candidato.tokens());
            if ((double) interseccion.size() / union.size() >= 0.75) return candidato.ejercicio();
        }
        return null;
    }

    // Clave de match exacto: minúsculas, sin paréntesis ni signos, sin plural final.
    private String claveExacta(String nombre) {
        String s = nombre.toLowerCase()
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("[^a-z0-9]", "");
        return s.endsWith("s") ? s.substring(0, s.length() - 1) : s;
    }

    // Tokens significativos del nombre para el match difuso (sin stopwords
    // ni posiciones genéricas, singularizados).
    private Set<String> tokens(String nombre) {
        Set<String> stop = Set.of("the", "a", "an", "with", "on", "in", "of", "and", "to",
                "bench", "standing", "seated", "lying");
        Set<String> resultado = new HashSet<>();
        String limpio = nombre.toLowerCase()
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("[^a-z0-9 ]", " ");
        for (String palabra : limpio.split("\\s+")) {
            if (palabra.length() < 2 || stop.contains(palabra)) continue;
            resultado.add(palabra.endsWith("s") ? palabra.substring(0, palabra.length() - 1) : palabra);
        }
        return resultado;
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
    // Devuelve vacío (omitido) si falta la traducción ES o EN, o si el
    // ejercicio se queda SIN demostración visual (ni FED ni imagen wger):
    // el catálogo solo contiene ejercicios con demostración.
    private Optional<Ejercicio> mapear(JsonNode ejercicioWger,
                                       Map<String, FedEjercicio> fedExacto,
                                       List<FedTokens> fedIndice) {
        JsonNode es = traduccion(ejercicioWger, LANG_ES);
        JsonNode en = traduccion(ejercicioWger, LANG_EN);
        if (es == null || en == null) return Optional.empty();

        String nombreEs = es.path("name").asText("").trim();
        String nombreEn = en.path("name").asText("").trim();
        if (nombreEs.isEmpty() || nombreEn.isEmpty()) return Optional.empty();

        // Demostración visual: FED (2 fotogramas animables) o imagen wger
        FedEjercicio fed = buscarFed(nombreEn, fedExacto, fedIndice);
        String imagenWger = imagenPrincipal(ejercicioWger);
        if (fed == null && imagenWger == null) return Optional.empty();

        // Músculo primario PRECISO por prioridad: (1) free-exercise-db si hay
        // match, (2) el primer músculo de wger (más fino que la categoría),
        // (3) null. El grupo grueso (chip) se deriva del músculo cuando existe.
        String musculoKey = fed != null ? fed.musculo() : null;
        if (musculoKey == null) musculoKey = musculoPrimarioWger(ejercicioWger);
        String musculoEs = musculoKey != null ? MUSCULO_ES.get(musculoKey) : null;
        String musculoEn = musculoKey != null ? MUSCULO_EN.get(musculoKey) : null;
        GrupoMuscular grupo = (musculoKey != null && MUSCULO_GRUPO.containsKey(musculoKey))
                ? MUSCULO_GRUPO.get(musculoKey)
                : CATEGORIA_GRUPO.getOrDefault(
                        ejercicioWger.path("category").path("name").asText(""), GrupoMuscular.FULLBODY);

        Ejercicio e = new Ejercicio();
        e.setWgerId(ejercicioWger.path("id").asInt());
        e.setNombre(truncar(nombreEs, 100));
        e.setNombreEn(truncar(nombreEn, 100));
        // Descripción = texto de wger (ES/EN). Instrucciones: SOLO si aportan
        // contenido distinto (pasos numerados de FED, en EN); nunca se duplica
        // la descripción como antes.
        String descEs = htmlATexto(es.path("description").asText(""));
        String descEn = htmlATexto(en.path("description").asText(""));
        e.setDescripcion(descEs.isBlank() ? null : descEs);
        e.setDescripcionEn(descEn.isBlank() ? null : descEn);
        e.setInstrucciones(null);
        e.setInstruccionesEn(fed != null ? fed.instrucciones() : null);
        e.setGrupoMuscular(grupo);
        e.setMusculoPrimario(musculoEs);
        e.setMusculoPrimarioEn(musculoEn);
        e.setDificultad(fed != null && !fed.nivel().isBlank()
                ? nivelFed(fed.nivel())
                : estimarDificultad(ejercicioWger, grupo));
        e.setCaloriasQuemadas(KCAL_GRUPO.getOrDefault(grupo, 6));
        // Equipo PRECISO de FED si el mapeo lo cubre; si no, el de wger.
        String[] equipoFed = fed != null && fed.equipo() != null ? EQUIPO_FED.get(fed.equipo()) : null;
        e.setEquipoNecesario(equipoFed != null ? equipoFed[0] : equipo(ejercicioWger, true));
        e.setEquipoNecesarioEn(equipoFed != null ? equipoFed[1] : equipo(ejercicioWger, false));
        // Preferencia: fotogramas FED (animables); si no, imagen estática wger
        e.setImagenUrl(fed != null ? fed.imagen1() : imagenWger);
        e.setImagenUrl2(fed != null ? fed.imagen2() : null);
        e.setActivo(true);
        return Optional.of(e);
    }

    // Primer músculo declarado por wger → clave FED (o null si wger no lo trae).
    private String musculoPrimarioWger(JsonNode ejercicioWger) {
        for (JsonNode m : ejercicioWger.path("muscles")) {
            String key = WGER_MUSCULO.get(m.path("id").asInt());
            if (key != null) return key;
        }
        return null;
    }

    // Nivel real de free-exercise-db → enum Dificultad propio.
    private Dificultad nivelFed(String nivel) {
        return switch (nivel) {
            case "beginner" -> Dificultad.PRINCIPIANTE;
            case "expert" -> Dificultad.AVANZADO;
            default -> Dificultad.INTERMEDIO;
        };
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
        destino.setMusculoPrimario(origen.getMusculoPrimario());
        destino.setMusculoPrimarioEn(origen.getMusculoPrimarioEn());
        destino.setDificultad(origen.getDificultad());
        destino.setCaloriasQuemadas(origen.getCaloriasQuemadas());
        destino.setEquipoNecesario(origen.getEquipoNecesario());
        destino.setEquipoNecesarioEn(origen.getEquipoNecesarioEn());
        destino.setImagenUrl(origen.getImagenUrl());
        destino.setImagenUrl2(origen.getImagenUrl2());
        destino.setActivo(true);
    }

    // Pasa el HTML de wger a texto plano legible. Clave: los saltos de línea
    // CRUDOS dentro de un párrafo (wraps del editor, ej. "Este\nes un...") se
    // colapsan a un espacio; solo se conserva salto de párrafo real en los
    // límites de bloque (</p>, <br>, <li>). Así el texto fluye y envuelve
    // natural en la pantalla en vez de cortarse a media frase.
    private String htmlATexto(String html) {
        if (html == null) return "";
        // Los limites de bloque reales se marcan con el centinela |BR| antes de
        // limpiar; el resto de espacios en blanco (incluidos los saltos crudos
        // del editor) se colapsan a un espacio, y luego |BR| vuelve a ser salto.
        String txt = html
                .replaceAll("(?i)</p>|<br ?/?>|</li>", "|BR|")
                .replaceAll("(?i)<li>", "|BR|- ")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        txt = txt.replaceAll("\\s+", " ");
        txt = txt.replaceAll("(?: ?\\|BR\\| ?)+", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return txt.replaceAll("^\n+|\n+$", "").trim();
    }

    // Recorta un texto a la longitud máxima de su columna.
    private String truncar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
