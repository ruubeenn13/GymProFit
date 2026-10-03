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

# Presupuesto de memoria (GP-187, DEC-046). Render mata el contenedor al pasar de 512 MB, sin
# OutOfMemoryError ni nada en el log; el 60 % del heap de antes miraba solo el heap, y fuera
# de él ya había ~160 MB al arrancar: el contenedor vivía al 93 %. Cada parte con su tope,
# medido con los 198 000 productos, Docker a 512 MB y NMT (informe del lote 1.6.5):
#   · heap 160 MB, Serial (el que elige la JVM con 1 CPU). Lo vivo llega a ~108 MB al
#     reconstruir el índice; con menos, un OOM tras el health sería un bucle de reinicios.
#   · metaspace 150 MB (la app usa ~125 tras recorrerla entera), code cache 24 MB (solo C1:
#     ahorra memoria y arranque, y la búsqueda no va más lenta), memoria directa 16 MB,
#     pilas de 512 KB (los hilos no pasan de ~35).
#   · Sin tope y medido: symbol ~55 MB, GC y resto ~10, y ~30 fuera de lo que cuenta la JVM.
#   Total con todo al máximo: unos 445; quedan más de 40 libres.
# ExitOnOutOfMemoryError: si falta memoria de Java, sale con su mensaje y Render lo reinicia,
# en vez de quedarse a medias. MALLOC_ARENA_MAX=2: menos arenas de malloc, menos memoria suelta.
ENV MALLOC_ARENA_MAX=2
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms96m", "-Xmx160m", "-XX:MaxMetaspaceSize=150m", \
    "-XX:TieredStopAtLevel=1", "-XX:ReservedCodeCacheSize=24m", "-XX:MaxDirectMemorySize=16m", "-Xss512k", \
    "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
