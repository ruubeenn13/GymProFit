#!/bin/sh
# ============================================================
# comprobar-limite-ip.sh — comprueba que el límite de peticiones del login no se salta
# cambiando X-Forwarded-For (S28, DEC-062). Repetirlo tras cualquier cambio de
# infraestructura o de plan en Render, o de dominio, CDN o proxy delante de la API.
#
#   sh scripts/comprobar-limite-ip.sh                         # api.gymprofit.app
#   BASE_URL=https://gymprofit-api.onrender.com/api sh scripts/comprobar-limite-ip.sh
#   USUARIO=prueba-algo sh scripts/comprobar-limite-ip.sh     # con una cuenta de prueba
#
# Manda MAX+1 logins con una clave mala y un X-Forwarded-For distinto e inventado en
# cada uno. Las MAX primeras tienen que dar 401 y la última 429: si la última no es 429,
# la API está contando la IP que manda el cliente y el límite no sirve.
#
# No lleva credenciales: la clave es mala a propósito. Gasta el cupo de tu IP durante la
# ventana (60 s en producción): espera ese rato antes de entrar de verdad.
# ============================================================
set -eu

BASE_URL="${BASE_URL:-https://api.gymprofit.app/api}"
USUARIO="${USUARIO:-__comprobar_limite__}"
MAX="${MAX:-15}"
CLAVE_MALA="Incorrecta-$(date +%s)"

echo "Contra $BASE_URL, $((MAX + 1)) logins con X-Forwarded-For distinto"
i=1
codigos=""
while [ "$i" -le $((MAX + 1)) ]; do
  falsa="198.51.$((i / 250)).$((i % 250 + 1))"
  c=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" -H "X-Forwarded-For: $falsa" \
    -d "{\"username\":\"$USUARIO\",\"password\":\"$CLAVE_MALA\"}")
  codigos="$codigos $c"
  ultimo="$c"
  if [ "$i" -le "$MAX" ] && [ "$c" = "429" ]; then
    echo "Códigos:$codigos"
    echo "429 antes de tiempo, en la petición $i: el cupo ya estaba gastado. Espera la ventana y repite." >&2
    exit 2
  fi
  i=$((i + 1))
done
echo "Códigos:$codigos"

if [ "$ultimo" = "429" ]; then
  echo "BIEN: la petición $((MAX + 1)) da 429 aunque cada una traía otro X-Forwarded-For."
else
  echo "MAL: la petición $((MAX + 1)) da $ultimo y no 429: el límite se salta cambiando X-Forwarded-For." >&2
  exit 1
fi
