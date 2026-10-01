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
import java.util.Optional;

// ============================================================
// OpenFoodFactsClient — lectura de UN producto de Open Food Facts por código
// Ya no busca por texto (GP-162): la búsqueda va contra nuestra base y los
// productos de España llegan por la importación semanal (GP-164). Solo queda
// la lectura de un código que no tenemos (GP-160).
// Normaliza el producto a AlimentoDTO con macros POR 100g y descarta los que
// no traen calorías. Sin API key; OFF pide identificarse vía User-Agent.
// ============================================================
@Component
public class OpenFoodFactsClient {

    private static final String PRODUCT_URL =
            "https://world.openfoodfacts.org/api/v2/product/%s?fields=code,product_name,product_name_es,brands,nutriments";
    // OFF exige un User-Agent identificativo en su política de uso
    private static final String USER_AGENT = "GymProFit/1.0 (Android; contacto: gymprofit.app)";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(OpenFoodFactsClient.class);

    public OpenFoodFactsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Timeouts cortos: quien escanea está esperando la respuesta
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
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
            throw new ExternalServiceException(ex, "error.externo.noDisponible", "Open Food Facts");
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
