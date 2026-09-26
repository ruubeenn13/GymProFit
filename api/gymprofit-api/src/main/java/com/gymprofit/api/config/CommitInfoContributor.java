package com.gymprofit.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

// ============================================================
// CommitInfoContributor — GP-100
//
// Aporta a /actuator/info el commit que corre, y es lo ÚNICO que ese endpoint enseña:
// la ruta es pública, así que nada de entorno ni de configuración.
//
// Render deja el hash en RENDER_GIT_COMMIT también en tiempo de ejecución con Docker.
// Fuera de Render la variable no existe y sale «local», que dice a las claras que eso
// no es producción. Sirve para comprobar un despliegue sin cuenta: el health check
// sigue en verde aunque el despliegue nuevo falle y responda la instancia vieja.
// ============================================================
@Component
public class CommitInfoContributor implements InfoContributor {

    private final String commit;

    public CommitInfoContributor(@Value("${RENDER_GIT_COMMIT:local}") String commit) {
        this.commit = commit;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("commit", commit);
    }
}
