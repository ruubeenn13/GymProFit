package com.gymprofit.api.service.externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.exceptions.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ============================================================
// OpenFoodFactsClient — cliente de la API pública de Open Food Facts
// Fuente del catálogo de alimentos (roadmap fase 1.2): búsqueda por texto
// (productos con marcas reales, priorizando el mercado español) y consulta
// por código de barras. Normaliza los productos a AlimentoDTO con macros
// POR 100g y descarta los que no traen calorías (inservibles para tracking).
// Sin API key; OFF pide identificarse vía User-Agent.
// ============================================================
@Component
public class OpenFoodFactsClient {

    // Search-a-licious: la API de búsqueda dedicada de OFF (el clásico
    // /cgi/search.pl devuelve 503 a clientes anónimos bajo carga)
    private static final String SEARCH_URL =
            "https://search.openfoodfacts.org/search?q=%s&page=%d&page_size=%d&langs=es"
                    + "&fields=code,product_name,product_name_es,brands,nutriments";
    private static final String PRODUCT_URL =
            "https://world.openfoodfacts.org/api/v2/product/%s?fields=code,product_name,product_name_es,brands,nutriments";
    // OFF exige un User-Agent identificativo en su política de uso
    private static final String USER_AGENT = "GymProFit/1.0 (Android; contacto: gymprofit.app)";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(OpenFoodFactsClient.class);

    // Resultado de una búsqueda externa: página de alimentos + total del proveedor.
    public record BusquedaExterna(List<AlimentoDTO> alimentos, long totalElements) {}

    public OpenFoodFactsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Timeouts cortos: la búsqueda es interactiva (el usuario está tecleando)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    // Catálogo inicial sin texto: productos más escaneados de España (browse
    // por popularidad). Search-a-licious exige sort_by cuando no hay texto.
    public BusquedaExterna browsePopulares(int page, int size) {
        String url = String.format(SEARCH_URL,
                UriUtils.encodeQueryParam("countries_tags:\"en:spain\"", StandardCharsets.UTF_8),
                page + 1, size) + "&sort_by=-unique_scans_n";
        return ejecutarBusqueda(url, "(populares)");
    }

    // Busca productos por texto en OFF (página de OFF empieza en 1; la nuestra en 0).
    public BusquedaExterna buscar(String q, int page, int size) {
        String url = String.format(SEARCH_URL,
                UriUtils.encodeQueryParam(q, StandardCharsets.UTF_8), page + 1, size);
        return ejecutarBusqueda(url, q);
    }

    // Ejecuta una búsqueda Search-a-licious y normaliza los hits.
    // Fallo del proveedor → lista vacía (la app sigue funcionando sin catálogo).
    private BusquedaExterna ejecutarBusqueda(String url, String contexto) {
        try {
            // URI.create evita que RestClient re-encodee la URL ya codificada
            String body = restClient.get().uri(java.net.URI.create(url)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);

            List<AlimentoDTO> alimentos = new ArrayList<>();
            for (JsonNode producto : root.path("hits")) {
                mapearProducto(producto).ifPresent(alimentos::add);
            }
            return new BusquedaExterna(alimentos, root.path("count").asLong(0));
        } catch (Exception ex) {
            logger.warn("Open Food Facts no disponible en la búsqueda '{}': {}", contexto, ex.getMessage());
            return new BusquedaExterna(List.of(), 0);
        }
    }

    // Consulta un producto por código de barras. Aquí el fallo del proveedor SÍ
    // es error (502): el usuario ha seleccionado un producto y espera importarlo.
    public Optional<AlimentoDTO> porBarcode(String barcode) {
        String url = String.format(PRODUCT_URL, UriUtils.encodeQueryParam(barcode, StandardCharsets.UTF_8));
        try {
            // URI.create evita que RestClient re-encodee la URL ya codificada
            String body = restClient.get().uri(java.net.URI.create(url)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);

            if (root.path("status") .asInt(0) != 1) return Optional.empty();
            return mapearProducto(root.path("product"));
        } catch (Exception ex) {
            throw new ExternalServiceException("Open Food Facts no disponible: " + ex.getMessage(), ex);
        }
    }

    // Normaliza un producto OFF a AlimentoDTO (macros por 100g, id null =
    // aún no importado). Devuelve vacío si no tiene nombre, barcode o kcal.
    private Optional<AlimentoDTO> mapearProducto(JsonNode producto) {
        String barcode = producto.path("code").asText("");
        String nombre = producto.path("product_name_es").asText("");
        if (nombre.isBlank()) nombre = producto.path("product_name").asText("");
        JsonNode nutriments = producto.path("nutriments");
        JsonNode kcal = nutriments.path("energy-kcal_100g");

        // Sin nombre, sin barcode o sin calorías → producto inservible para tracking
        if (nombre.isBlank() || barcode.isBlank() || !kcal.isNumber()) return Optional.empty();

        AlimentoDTO dto = new AlimentoDTO();
        dto.setNombre(truncar(nombre, 100));
        dto.setBarcode(truncar(barcode, 32));
        dto.setMarca(truncar(marcas(producto), 100));
        dto.setCalorias((int) Math.round(kcal.asDouble()));
        dto.setProteinas(decimal(nutriments, "proteins_100g"));
        dto.setCarbohidratos(decimal(nutriments, "carbohydrates_100g"));
        dto.setGrasas(decimal(nutriments, "fat_100g"));
        dto.setFibra(decimal(nutriments, "fiber_100g"));
        dto.setPorcionGramos(100);
        dto.setActivo(true);
        return Optional.of(dto);
    }

    // Campo brands: string en la API clásica (v2/product) y array en
    // Search-a-licious → se normaliza a "a, b" tolerando ambos formatos.
    private String marcas(JsonNode producto) {
        JsonNode brands = producto.path("brands");
        if (brands.isTextual()) return brands.asText();
        if (brands.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode b : brands) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(b.asText());
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        return null;
    }

    // Extrae un nutriente como BigDecimal escala 2, acotado al rango de la
    // columna DECIMAL(5,2); null si OFF no lo informa.
    private BigDecimal decimal(JsonNode nutriments, String campo) {
        JsonNode nodo = nutriments.path(campo);
        if (!nodo.isNumber()) return null;
        BigDecimal valor = BigDecimal.valueOf(nodo.asDouble()).setScale(2, RoundingMode.HALF_UP);
        if (valor.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        BigDecimal max = new BigDecimal("999.99");
        return valor.min(max);
    }

    // Recorta un texto a la longitud máxima de su columna (null-safe).
    private String truncar(String texto, int max) {
        if (texto == null || texto.isBlank()) return null;
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
