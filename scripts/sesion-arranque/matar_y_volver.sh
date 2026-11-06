#!/usr/bin/env bash
# Mata el proceso con la app en segundo plano, la vuelve a abrir desde el lanzador
# (Android restaura la pantalla de arriba) y cuenta cuántas veces se crea cada pantalla
# (wm_on_create_called). Con el tema de la app distinto del del sistema, cada pantalla
# tiene que crearse una vez (GP-234).
#
# Uso: con la pantalla que se quiera medir delante, sh scripts/sesion-arranque/matar_y_volver.sh
# Espera a la primera reanudación: en un emulador cargado una creación llegó a tardar 19 s.
# Android 9 o más cuenta wm_on_create_called. Android 7 y 8 no lo tienen: cuenta las
# reanudaciones (am_on_resume_called) con su motivo; una pantalla recreada suma otra con
# el motivo handleRelaunchActivity.
export MSYS_NO_PATHCONV=1
A=${ADB:-$HOME/AppData/Local/Android/Sdk/platform-tools/adb}
top() { $A shell dumpsys activity activities | grep -m1 -E "topResumedActivity|mResumedActivity" | sed 's/.*activities\.//; s/ .*//'; }
echo "antes: $(top)"
$A shell input keyevent 3
sleep 2
$A shell am kill com.gymprofit.app
sleep 1
P=$($A shell pidof com.gymprofit.app | tr -cd '0-9')
if [ -n "$P" ]; then $A shell run-as com.gymprofit.app kill -9 "$P"; sleep 1; fi
P=$($A shell pidof com.gymprofit.app | tr -cd '0-9')
[ -n "$P" ] && echo "SIGUE VIVO $P"
$A logcat -b events -c
$A shell monkey -p com.gymprofit.app -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
# Hasta la primera reanudación de una pantalla de la app, y 5 s más por si se recrea.
for i in $(seq 1 60); do
  $A logcat -b events -d | grep -qE "(wm|am)_on_resume_called: \[[0-9]*,es.pmdm" && break
  sleep 1
done
sleep 5
echo "después: $(top)"
EV=$($A logcat -b events -d)
if echo "$EV" | grep -q wm_on_create_called; then
  echo "$EV" | grep wm_on_create_called | grep es.pmdm | sed 's/.*wm_on_create_called: \[[0-9]*,//; s/,performCreate.*//' | sort | uniq -c
else
  echo "$EV" | grep am_on_resume_called | grep es.pmdm | sed 's/.*am_on_resume_called: \[[0-9]*,//; s/\]//; s/,/ (reanudación: /; s/$/)/' | sort | uniq -c
fi
