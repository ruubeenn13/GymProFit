package db.migration;

import com.gymprofit.api.service.programa.SembradorPlantillas;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.InputStream;

// ============================================================
// Semilla del catálogo de plantillas v1 (GP-074, lote 1.2.0)
//
// Siembra los 13 programas y las 31 rutinas plantilla desde una copia FIJA del catálogo,
// db/semillas/catalogo-plantillas-v1.json. Esa copia no se edita: un catálogo nuevo es
// otro fichero y otra migración. La lógica está en SembradorPlantillas.
//
// Es Java y no SQL porque lee el JSON, que es la fuente aprobada, en vez de transcribir
// 200 filas a mano. Flyway la encuentra por estar en el paquete db.migration.
// ============================================================
public class V202609292002__Semilla_plantillas_v1 extends BaseJavaMigration {

    static final String CATALOGO = "db/semillas/catalogo-plantillas-v1.json";

    @Override
    public void migrate(Context context) throws Exception {
        try (InputStream json = V202609292002__Semilla_plantillas_v1.class.getClassLoader()
                .getResourceAsStream(CATALOGO)) {
            if (json == null) {
                throw new IllegalStateException("No está el catálogo " + CATALOGO);
            }
            SembradorPlantillas.sembrar(context.getConnection(), json);
        }
    }
}
