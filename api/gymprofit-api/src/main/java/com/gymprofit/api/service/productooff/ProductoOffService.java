package com.gymprofit.api.service.productooff;

import com.gymprofit.api.dto.entity.productooff.ImportacionFinDTO;
import com.gymprofit.api.dto.entity.productooff.ImportacionLoteDTO;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ============================================================
// ProductoOffService — escritura de productos_off (GP-164)
// Recibe los lotes de la importación semanal y los guarda con un solo INSERT por
// lote. Reimportar actualiza la fila del producto, nunca el alimento ya materializado
// (`alimentos`), que puede estar en comidas de alguien.
// ============================================================
@Service
public class ProductoOffService {

    private static final Logger logger = LoggerFactory.getLogger(ProductoOffService.class);

    // Un lote mayor que esto se rechaza: con 13 parámetros por fila, 2000 filas son
    // 26 000, por debajo del máximo de 65 535 del protocolo de MySQL y MariaDB.
    public static final int MAX_LOTE = 2000;

    private static final String COLUMNAS = "codigo, nombre, marca, kcal, proteinas, carbohidratos, grasas, "
            + "fibra, racion_gramos, racion_texto, envase, escaneos, actualizado";
    private static final String FILA = "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // VALUES() funciona igual en MySQL 8 y en MariaDB. La sintaxis con alias de MySQL
    // 8.0.19 no existe en MariaDB.
    private static final String AL_REPETIR = " ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), "
            + "marca = VALUES(marca), kcal = VALUES(kcal), proteinas = VALUES(proteinas), "
            + "carbohidratos = VALUES(carbohidratos), grasas = VALUES(grasas), fibra = VALUES(fibra), "
            + "racion_gramos = VALUES(racion_gramos), racion_texto = VALUES(racion_texto), "
            + "envase = VALUES(envase), escaneos = VALUES(escaneos), actualizado = VALUES(actualizado)";

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventos;

    public ProductoOffService(JdbcTemplate jdbc, ApplicationEventPublisher eventos) {
        this.jdbc = jdbc;
        this.eventos = eventos;
    }

    /** Aviso de que productos_off ha cambiado: la búsqueda tiene que reconstruir su índice. */
    public record ProductosCambiados() {
    }

    /**
     * Guarda un lote: valida cada producto, se queda con el último de cada código y hace
     * un único INSERT … ON DUPLICATE KEY UPDATE.
     *
     * @param lote productos tal como los manda la importación.
     * @return cuántos se recibieron, cuántos se guardaron y cuántos se descartaron.
     */
    @Transactional
    public ImportacionLoteDTO importarLote(List<ProductoOffImportDTO> lote) {
        Map<String, ProductoOffImportDTO> validos = new LinkedHashMap<>();
        for (ProductoOffImportDTO producto : lote) {
            ProductoOffValidacion.limpiar(producto).ifPresent(p -> validos.put(p.getCodigo(), p));
        }
        int descartados = lote.size() - validos.size();
        if (!validos.isEmpty()) {
            guardar(new ArrayList<>(validos.values()));
            eventos.publishEvent(new ProductosCambiados());
        }
        logger.info("Importación de productos: {} recibidos, {} guardados, {} descartados",
                lote.size(), validos.size(), descartados);
        return new ImportacionLoteDTO(lote.size(), validos.size(), descartados);
    }

    /**
     * Guarda (o actualiza) un único producto ya validado, el que trae la lectura de un
     * código a Open Food Facts (GP-160).
     * <p>
     * No avisa de {@link ProductosCambiados}: quien lo llama lo materializa en el acto, y
     * así entra en el índice del catálogo, que se reconstruye entero en milisegundos.
     * Reconstruir el de los ~200 000 productos por cada código leído no añadiría nada a
     * la búsqueda y en Render, con 0,1 de CPU, se nota. Entra en el índice de productos
     * con la siguiente reconstrucción (arranque o importación).
     *
     * @param producto producto que ya pasó {@link ProductoOffValidacion#limpiar}.
     */
    @Transactional
    public void guardarUno(ProductoOffImportDTO producto) {
        guardar(List.of(producto));
    }

    /**
     * Cierra una importación: actualiza las estadísticas de la tabla y devuelve cuántos
     * productos hay y cuánto ocupan, que es lo que se mide contra el presupuesto.
     *
     * @return número de productos y bytes de datos e índices de la tabla.
     */
    public ImportacionFinDTO cerrar() {
        // ANALYZE para que information_schema no devuelva tamaños viejos.
        jdbc.execute("ANALYZE TABLE productos_off");
        long productos = Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM productos_off", Long.class))
                .orElse(0L);
        Map<String, Object> tamano = jdbc.queryForMap("""
                SELECT COALESCE(DATA_LENGTH, 0) AS datos, COALESCE(INDEX_LENGTH, 0) AS indices
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'productos_off'""");
        eventos.publishEvent(new ProductosCambiados());
        return new ImportacionFinDTO(productos, ((Number) tamano.get("datos")).longValue(),
                ((Number) tamano.get("indices")).longValue());
    }

    private void guardar(List<ProductoOffImportDTO> productos) {
        StringBuilder sql = new StringBuilder("INSERT INTO productos_off (").append(COLUMNAS).append(") VALUES ");
        List<Object> args = new ArrayList<>(productos.size() * 13);
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        for (int i = 0; i < productos.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(FILA);
            ProductoOffImportDTO p = productos.get(i);
            args.add(p.getCodigo());
            args.add(p.getNombre());
            args.add(p.getMarca());
            args.add(p.getKcal());
            args.add(p.getProteinas());
            args.add(p.getCarbohidratos());
            args.add(p.getGrasas());
            args.add(p.getFibra());
            args.add(p.getRacionGramos());
            args.add(p.getRacionTexto());
            args.add(p.getEnvase());
            args.add(p.getEscaneos());
            args.add(ahora);
        }
        sql.append(AL_REPETIR);
        jdbc.update(sql.toString(), args.toArray());
    }
}
