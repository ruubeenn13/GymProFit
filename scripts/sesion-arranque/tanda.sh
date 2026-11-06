#!/usr/bin/env bash
# Una tanda de la renovación de sesión a mitad de uso (GP-237, GP-235, DEC-059), con la
# app de depuración en el emulador y proxy-refresh.py delante de la API local.
#
# Con la app en Inicio y la sesión abierta: abre Perfil, deja caducar el access (la API
# local va con --jwt.expiration=60000), pone /auth/refresh en el modo pedido y vuelve a
# Inicio, que lanza 8 peticiones a la vez. Cuenta las llamadas a /auth/refresh que llegan
# al proxy (tiene que ser 1). Después, con /auth/refresh de vuelta («pasar»), espera lo
# que pida la app (5 s, o el Retry-After de un 429), hace la siguiente acción (abrir
# Perfil) y vuelve a contar: tiene que renovar con una llamada y seguir.
#
# Uso: sh scripts/renovacion/tanda.sh <modo> [segundos_tras_volver] [segundos_de_espera]
#   modo: 503, 500, 502, 429, colgar, cortar o pasar (los de proxy-refresh.py)
#   segundos_tras_volver: cuánto mirar tras volver a Inicio (12; con colgar, 75)
#   segundos_de_espera: la espera antes de la siguiente acción (6; con 429, 31)
export MSYS_NO_PATHCONV=1 PYTHONIOENCODING=utf-8
A=${ADB:-$HOME/AppData/Local/Android/Sdk/platform-tools/adb}
DIR=$(python -c "import os, tempfile; print(os.path.join(tempfile.gettempdir(), 'gymprofit-proxy-refresh'))")
mkdir -p "$DIR"
MODO=$1
[ -z "$MODO" ] && { echo "Uso: tanda.sh <modo> [segundos_tras_volver] [segundos_de_espera]"; exit 1; }
TRAS=${2:-$([ "$MODO" = colgar ] && echo 75 || echo 12)}
ESPERA=${3:-$([ "$MODO" = 429 ] && echo 31 || echo 6)}
LOG="$DIR/refresh.log"
touch "$LOG"

top() { $A shell dumpsys activity activities | grep -m1 topResumedActivity | sed 's/.*activities\.//; s/ .*//'; }
llamadas() { wc -l < "$LOG" | tr -d ' '; }
modo() { echo "$1" > "$DIR/modo.txt"; }
resumen_logcat() {
  $A logcat -d | grep -E "GymProFit|OkHttp" \
    | grep -E "Refresh|renovado|Sesión|HTTP FAILED|<-- 401|<-- 429" \
    | sed 's/^.*\(GymProFit\|OkHttp\)/\1/; s/[0-9]\+ms//' | sort | uniq -c
}
# Pulsa el avatar de Inicio (en español o en inglés) con uiautomator.
abrir_perfil() {
  # Python en Windows no entiende las rutas /c/...: se le pasa la de Windows.
  python - "$(cygpath -w "$A" 2>/dev/null || echo "$A")" <<'EOF'
import re, subprocess, sys
adb = sys.argv[1]
subprocess.run([adb, "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
xml = subprocess.run([adb, "exec-out", "cat", "/sdcard/ui.xml"], capture_output=True).stdout.decode("utf-8", "replace")
for nodo in re.findall(r"<node [^>]*>", xml):
    if "Tu perfil y ajustes" in nodo or "Your profile and settings" in nodo:
        x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', nodo).groups())
        subprocess.run([adb, "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
        sys.exit(0)
sys.exit("No encuentro el avatar de Inicio: ¿está la app en Inicio con la sesión abierta?")
EOF
}

modo pasar
abrir_perfil || exit 1
echo "--- modo $MODO: en Perfil, esperando 65 s a que caduque el access"
sleep 65

modo "$MODO"
antes=$(llamadas)
$A logcat -c
$A shell input keyevent 4
sleep "$TRAS"
despues=$(llamadas)
echo "VOLVER A INICIO: llamadas a /auth/refresh = $((despues - antes))"
[ "$despues" -gt "$antes" ] && tail -n $((despues - antes)) "$LOG"
resumen_logcat
echo "delante: $(top)"

modo pasar
echo "--- espera de la app: ${ESPERA} s, con /auth/refresh de vuelta"
sleep "$ESPERA"
antes=$(llamadas)
$A logcat -c
abrir_perfil
sleep 8
despues=$(llamadas)
echo "SIGUIENTE ACCIÓN: llamadas a /auth/refresh = $((despues - antes))"
[ "$despues" -gt "$antes" ] && tail -n $((despues - antes)) "$LOG"
resumen_logcat
echo "delante: $(top)"
$A shell input keyevent 4
