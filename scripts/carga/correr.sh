#!/bin/sh
# ============================================================
# correr.sh — levanta la API en Docker con los límites de Render free y le pasa
# recorrido.js con k6 a varios niveles de usuarios (GP-221). Ver README.md.
#
#   DB_PASSWORD=... sh scripts/carga/correr.sh 5 10 20 40
#
# Cada argumento es un nivel de usuarios simultáneos; cada nivel corre DURACION (3m).
# Deja en scripts/carga/resultados/<fecha>/ el resumen de k6 por nivel, docker stats
# cada 5 s y el log de la API.
#
# Contra la base LOCAL siempre (host.docker.internal). Nunca contra producción.
# ============================================================
set -eu

: "${DB_PASSWORD:?Falta DB_PASSWORD (la de la base local)}"
DB_URL="${DB_URL:-jdbc:mariadb://host.docker.internal:3308/gymprofit_db?useUnicode=true&characterEncoding=UTF-8}"
DB_USER="${DB_USER:-root}"
DURACION="${DURACION:-3m}"
CPUS="${CPUS:-0.1}"
IMAGEN="${IMAGEN:-gymprofit-api:carga}"
case "$DB_URL" in
  *host.docker.internal*|*localhost*|*127.0.0.1*) ;;
  *) echo "DB_URL no es local: esta prueba no se corre contra producción" >&2; exit 1 ;;
esac

RAIZ="$(cd "$(dirname "$0")" && pwd)"
SALIDA="$RAIZ/resultados/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$SALIDA"
# En Git Bash (Windows) los -v de Docker necesitan la ruta de Windows y que MSYS no la toque.
if command -v cygpath >/dev/null 2>&1; then
  export MSYS_NO_PATHCONV=1
  VOL_RAIZ="$(cygpath -w "$RAIZ")"; VOL_SALIDA="$(cygpath -w "$SALIDA")"
else
  VOL_RAIZ="$RAIZ"; VOL_SALIDA="$SALIDA"
fi

# Secretos de usar y tirar: solo valen para este contenedor.
JWT="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
IMPORT="$(head -c 48 /dev/urandom | base64 | tr -d '\n=+/')"

docker rm -f gymprofit-carga >/dev/null 2>&1 || true
# Render free: 512 MB para el contenedor entero y 0,1 de CPU. Los topes de la JVM
# (heap 160 MB, etc.) los pone el ENTRYPOINT del Dockerfile, igual que en producción.
docker run -d --name gymprofit-carga \
  --memory=512m --memory-swap=512m --cpus="$CPUS" \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="$DB_URL" \
  -e SPRING_DATASOURCE_USERNAME="$DB_USER" \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT" \
  -e BREVO_API_KEY=clave-de-carga-que-no-envia-nada \
  -e MAIL_FROM="Carga <carga@carga.local>" \
  -e IMPORTACION_CLAVE="$IMPORT" \
  "$IMAGEN" >/dev/null

echo "Esperando a la API..."
i=0
until curl -fs http://localhost:8080/api/actuator/health >/dev/null 2>&1; do
  # Con 0,1 de CPU el arranque pasa de 7 minutos: se espera hasta 15.
  i=$((i + 1)); [ "$i" -gt 180 ] && { docker logs gymprofit-carga | tail -40; exit 1; }
  sleep 5
done
echo "API arriba tras ~$((i * 5)) s"

docker stats gymprofit-carga --format '{{.CPUPerc}};{{.MemUsage}};{{.PIDs}}' \
  | while IFS= read -r l; do echo "$(date +%H:%M:%S);$l"; done > "$SALIDA/docker-stats.csv" &
STATS=$!

for VUS in "$@"; do
  echo "== $VUS usuarios =="
  docker run --rm -i --add-host=host.docker.internal:host-gateway \
    -v "$VOL_RAIZ:/carga" -v "$VOL_SALIDA:/resultados" \
    -e BASE_URL=http://host.docker.internal:8080/api -e VUS="$VUS" -e DURACION="$DURACION" \
    -e EJERCICIO_ID="${EJERCICIO_ID:-860}" \
    grafana/k6:1.4.2 run --quiet /carga/recorrido.js | tee -a "$SALIDA/resumen.txt" || true
  sleep 20
done

kill "$STATS" 2>/dev/null || true
docker logs gymprofit-carga > "$SALIDA/api.log" 2>&1
docker rm -f gymprofit-carga >/dev/null
echo "Resultados en $SALIDA"
