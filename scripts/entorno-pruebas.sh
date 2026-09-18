#!/usr/bin/env bash
# ============================================================================
# entorno-pruebas.sh — Arranca y comprueba el entorno de pruebas de GymProFit.
#
# Todo lo que se haga con este entorno se queda en la máquina: la app de
# desarrollo habla con la API local y la API local escribe en la base MariaDB
# local del puerto 3308. Producción (Render + Aiven) no se toca en ningún paso.
#
#   ./scripts/entorno-pruebas.sh            arranca lo que falte y comprueba
#   ./scripts/entorno-pruebas.sh estado     solo comprueba, no arranca nada
#   ./scripts/entorno-pruebas.sh parar      para la API local
#
# Requisitos: MariaDB local escuchando en 3308 y el JDK 21 en la ruta de abajo.
# ============================================================================
set -u

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_DIR="$RAIZ/api/gymprofit-api"
APP_DIR="$RAIZ/app/GymProFit"
JDK="/c/Users/ruben/.jdks/ms-21.0.11"
PUERTO_API=8080
PUERTO_BD=3308
SALUD="http://localhost:$PUERTO_API/api/actuator/health"

verde()  { printf '  \033[32m✓\033[0m %s\n' "$1"; }
rojo()   { printf '  \033[31m✗\033[0m %s\n' "$1"; }
aviso()  { printf '  \033[33m!\033[0m %s\n' "$1"; }

# --- Comprobación de aislamiento: lo más importante del script ---------------
# La app de desarrollo solo puede apuntar a la red local. Si local.properties
# tiene otra cosa, el build de Gradle ya falla, pero se avisa aquí antes de
# perder tiempo compilando.
comprobar_aislamiento() {
  local url
  url="$(grep -E '^BASE_URL=' "$APP_DIR/local.properties" 2>/dev/null | cut -d= -f2-)"
  url="${url:-http://10.0.2.2:8080/api/ (por defecto)}"

  if printf '%s' "$url" | grep -qiE 'onrender|aiven|https://'; then
    rojo "La app de desarrollo apunta FUERA de la red local: $url"
    rojo "Corrige BASE_URL en $APP_DIR/local.properties antes de seguir."
    return 1
  fi
  verde "App de desarrollo aislada: $url"
  return 0
}

comprobar_bd() {
  if (echo > "/dev/tcp/127.0.0.1/$PUERTO_BD") >/dev/null 2>&1; then
    verde "Base de datos local escuchando en el puerto $PUERTO_BD"
    return 0
  fi
  rojo "No hay base de datos en el puerto $PUERTO_BD. Arranca MariaDB local."
  return 1
}

api_viva() {
  curl -s -o /dev/null --max-time 3 "$SALUD" 2>/dev/null
}

arrancar_api() {
  if api_viva; then
    verde "API local ya estaba en marcha (puerto $PUERTO_API)"
    return 0
  fi

  aviso "Arrancando la API local en perfil dev…"
  ( cd "$API_DIR" && JAVA_HOME="$JDK" ./mvnw -q spring-boot:run > "$RAIZ/.api-dev.log" 2>&1 & )

  for _ in $(seq 1 60); do
    sleep 2
    if api_viva; then
      verde "API local lista en $SALUD"
      return 0
    fi
  done

  rojo "La API no respondió a tiempo. Mira $RAIZ/.api-dev.log"
  return 1
}

parar_api() {
  local pid
  pid="$(powershell -NoProfile -Command "(Get-NetTCPConnection -LocalPort $PUERTO_API -State Listen -ErrorAction SilentlyContinue).OwningProcess" 2>/dev/null | tr -d '\r\n ')"
  if [ -n "$pid" ]; then
    powershell -NoProfile -Command "Stop-Process -Id $pid -Force" >/dev/null 2>&1
    verde "API local detenida"
  else
    aviso "No había ninguna API escuchando en el puerto $PUERTO_API"
  fi
}

echo
echo "Entorno de pruebas de GymProFit"
echo "───────────────────────────────"

case "${1:-arrancar}" in
  parar)
    parar_api
    ;;
  estado)
    comprobar_aislamiento
    comprobar_bd
    if api_viva; then verde "API local en marcha"; else aviso "API local parada"; fi
    ;;
  *)
    comprobar_aislamiento || exit 1
    comprobar_bd || exit 1
    arrancar_api || exit 1
    echo
    echo "  Usuario de pruebas:  prueba / Prueba1234."
    echo "  Restaurar los datos: ./scripts/reset-datos-pruebas.sh"
    ;;
esac
echo
