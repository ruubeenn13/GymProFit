package db.migration;

import com.gymprofit.api.service.ejercicio.AplicadorCatalogoEs;
import com.gymprofit.api.service.ejercicio.CatalogoEs;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// ============================================================
// El catálogo de ejercicios en español (GP-217, lote 1.7.1)
//
// Lleva a los ejercicios que ya hay en la base el español de catalogo/ejercicios-es.csv:
// nombre y descripcion, por fed_id. No toca el inglés (*_en), ni las imágenes, ni nada más.
// Es una migración de datos y no un arranque que sincroniza cada vez porque deja constancia
// en flyway_schema_history, se aplica una sola vez y se puede revertir con la copia previa
// de documentacion/estado/. La importación del catálogo lee el mismo CSV, así que reimportar
// no vuelve atrás lo que esto escribe.
//
// Es Java y no SQL para leer el CSV, que es la fuente aprobada, en vez de transcribir 873
// filas a mano (como la semilla de las plantillas). Si el CSV cambia después, va en otra
// migración: una migración publicada no se edita.
// ============================================================
public class V202610221810__Catalogo_ejercicios_es extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (InputStream in = V202610221810__Catalogo_ejercicios_es.class.getClassLoader()
                .getResourceAsStream(CatalogoEs.RECURSO)) {
            if (in == null) {
                throw new IllegalStateException("No está el catálogo " + CatalogoEs.RECURSO);
            }
            CatalogoEs catalogo = CatalogoEs.de(new InputStreamReader(in, StandardCharsets.UTF_8));
            AplicadorCatalogoEs.aplicar(context.getConnection(), catalogo);
        }
    }
}
