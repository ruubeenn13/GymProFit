package com.gymprofit.api.service.externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.exceptions.ExternalServiceException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

// ============================================================
// OpenFoodFactsClient — lectura de UN producto de Open Food Facts por código
// Ya no busca por texto (GP-162): la búsqueda va contra nuestra base y los
// productos de España llegan por la importación semanal (GP-164). Solo queda
// la lectura de un código que no tenemos (GP-160), y quien la llama respeta el
// cupo de LimiteOpenFoodFacts.
//
// Devuelve el producto tal cual, con los mismos campos que la importación; si es
// aceptable lo decide ProductoOffValidacion, igual que con la importación.
// Sin API key; OFF pide identificarse vía User-Agent.
// ============================================================
@Component
public class OpenFoodFactsClient {

    private static final String PRODUCT_URL = "https://world.openfoodfacts.org/api/v2/product/%s"
            + "?fields=code,product_name,product_name_es,brands,nutriments,serving_quantity,serving_size,"
            + "quantity,unique_scans_n";
    // OFF exige un User-Agent identificativo en su política de uso
    private static final String USER_AGENT = "GymProFit/1.0 (Android; contacto: gymprofit.app)";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

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

    /**
     * Lee un producto por código de barras.
     *
     * @param barcode código, solo cifras.
     * @return el producto sin validar, o vacío si Open Food Facts no lo tiene.
     * @throws ExternalServiceException (502) si Open Food Facts no responde.
     */
    public Optional<ProductoOffImportDTO> porBarcode(String barcode) {
        String url = String.format(PRODUCT_URL, UriUtils.encodeQueryParam(barcode, StandardCharsets.UTF_8));
        JsonNode root;
        try {
            // URI.create evita que RestClient re-encodee la URL ya codificada
            String body = restClient.get().uri(java.net.URI.create(url)).retrieve().body(String.class);
            root = objectMapper.readTree(body);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound noExiste) {
            // La API v2 responde 404 cuando el código no existe.
            return Optional.empty();
        } catch (Exception ex) {
            throw new ExternalServiceException(ex, "error.externo.noDisponible", "Open Food Facts");
        }
        if (root.path("status").asInt(0) != 1) return Optional.empty();
        return Optional.of(mapear(root.path("product"), barcode));
    }

    // Mismos nombres de campo que datos/productos/filtrar_off.py.
    private ProductoOffImportDTO mapear(JsonNode producto, String barcode) {
        String nombre = producto.path("product_name_es").asText("");
        if (nombre.isBlank()) nombre = producto.path("product_name").asText("");
        JsonNode n = producto.path("nutriments");
        String codigo = producto.path("code").asText("");
        return new ProductoOffImportDTO(
                codigo.isBlank() ? barcode : codigo,
                nombre,
                primeraMarca(producto.path("brands")),
                numero(n, "energy-kcal_100g"),
                numero(n, "proteins_100g"),
                numero(n, "carbohydrates_100g"),
                numero(n, "fat_100g"),
                numero(n, "fiber_100g"),
                numero(producto, "serving_quantity"),
                texto(producto, "serving_size"),
                texto(producto, "quantity"),
                producto.path("unique_scans_n").isNumber() ? producto.path("unique_scans_n").asInt() : 0,
                numero(n, "alcohol_100g"),
                numero(n, "polyols_100g"));
    }

    // brands: texto «a, b» en la API v2; a veces lista. Se queda la primera, como el script.
    private static String primeraMarca(JsonNode brands) {
        String todas = brands.isArray() && !brands.isEmpty() ? brands.get(0).asText() : brands.asText("");
        String primera = todas.split(",")[0].strip();
        return primera.isEmpty() ? null : primera;
    }

    // Open Food Facts manda los números unas veces como número y otras como texto.
    private static Double numero(JsonNode nodo, String campo) {
        JsonNode v = nodo.path(campo);
        if (v.isNumber()) return v.asDouble();
        if (v.isTextual()) {
            try {
                return Double.parseDouble(v.asText().replace(',', '.'));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String texto(JsonNode nodo, String campo) {
        String t = nodo.path(campo).asText("");
        return t.isBlank() ? null : t;
    }
}
