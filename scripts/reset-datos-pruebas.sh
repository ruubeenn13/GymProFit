#!/usr/bin/env bash
# ============================================================================
# reset-datos-pruebas.sh — Devuelve la base de datos LOCAL a su estado base.
#
# Deshace todo lo creado probando: usuarios, rutinas, sesiones, comidas y
# mediciones vuelven al contenido del volcado de referencia. El catálogo (873
# ejercicios y los alimentos) forma parte de ese volcado, así que se conserva.
#
#   ./scripts/reset-datos-pruebas.sh              restaura el estado base
#   ./scripts/reset-datos-pruebas.sh --guardar    toma un nuevo estado base
#
# Solo actúa sobre localhost:3308. No existe ninguna ruta desde aquí a la base
# de datos de producción.
# ============================================================================
set -eu

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SNAPSHOT="$RAIZ/api/gymprofit-api/sql/snapshots/dev_baseline.sql"
BIN="/c/Program Files/MariaDB 11.8/bin"
HOST=127.0.0.1
PUERTO=3308
BD=gymprofit_db
USUARIO=root
CLAVE=12345

cliente="$BIN/mariadb.exe"
volcador="$BIN/mariadb-dump.exe"
[ -f "$volcador" ] || volcador="$BIN/mysqldump.exe"

if [ "${1:-}" = "--guardar" ]; then
  mkdir -p "$(dirname "$SNAPSHOT")"
  "$volcador" -h "$HOST" -P "$PUERTO" -u "$USUARIO" "-p$CLAVE" \
      --single-transaction --routines --add-drop-table "$BD" > "$SNAPSHOT"
  echo "Nuevo estado base guardado ($(du -h "$SNAPSHOT" | cut -f1))."
  exit 0
fi

if [ ! -f "$SNAPSHOT" ]; then
  echo "No hay estado base guardado en:"
  echo "  $SNAPSHOT"
  echo "Créalo con: ./scripts/reset-datos-pruebas.sh --guardar"
  exit 1
fi

# La API mantiene un pool de conexiones abierto; restaurar con ella en marcha
# deja datos a medias en memoria. Se avisa en vez de restaurar a ciegas.
if curl -s -o /dev/null --max-time 3 "http://localhost:8080/api/actuator/health" 2>/dev/null; then
  echo "La API local está en marcha. Párala antes de restaurar:"
  echo "  ./scripts/entorno-pruebas.sh parar"
  exit 1
fi

echo "Restaurando $BD desde el estado base…"
"$cliente" -h "$HOST" -P "$PUERTO" -u "$USUARIO" "-p$CLAVE" "$BD" < "$SNAPSHOT"
echo "Hecho. Los datos locales vuelven al punto de partida."
echo "Usuario de pruebas: prueba / Prueba1234."
