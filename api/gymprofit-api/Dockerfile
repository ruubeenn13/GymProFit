# ============================================================
# Dockerfile de la API GymProFit (Spring Boot). Multi-stage:
#   1) build  -> compila y empaqueta el .jar con JDK 21 + Maven
#   2) runtime-> imagen ligera JRE 21 que solo ejecuta el .jar
# Pensado para PaaS (Render/Koyeb): el puerto se toma de $PORT y el
# perfil activo es 'prod' (config 100% por variables de entorno).
# ============================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache de dependencias: primero el pom, luego el resto
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Código fuente (incluye src/generated/jooq versionado -> compila sin BD)
COPY src ./src

# Empaqueta sin tests (los tests ya corren en CI) y sin regenerar jOOQ (skip=true)
RUN mvn -B -q -DskipTests package

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# LOG_DIR a un directorio escribible del contenedor (el logback por defecto
# apunta a /home/ubuntu/logs, que no existe aquí).
ENV LOG_DIR=/tmp/logs
ENV SPRING_PROFILES_ACTIVE=prod

# Copia el jar ejecutable generado en el stage de build
COPY --from=build /build/target/*.jar app.jar

# Puerto informativo (la PaaS enruta por $PORT; server.port=${PORT:8080})
EXPOSE 8080

# MaxRAMPercentage=60: en la instancia de 512MB de Render el default de la JVM (~25%,
# ~128MB de heap) se queda corto desde que firebase-admin (grpc/netty) engordó el
# arranque — el primer deploy con él llegó a agotar el timeout del health check.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-jar", "app.jar"]
