#!/bin/sh
# Arranques en frío con traza de Perfetto y grabación a la vez, hasta uno con hueco (oro.py).
# Uso: sh graba_traza.sh <prefijo> <max> [extras de am start...]
export MSYS_NO_PATHCONV=1
H=$(cd "$(dirname "$0")" && pwd)  # los scripts
S=${GP_SALIDA:-$PWD}              # vídeos, fotogramas y trazas
P=$1; M=$2; shift 2
adb push $H/traza.cfg /data/local/tmp/traza.cfg > /dev/null
i=1
while [ $i -le $M ]; do
  for k in $(seq 1 30); do
    L=$(adb shell cat /proc/loadavg | cut -d' ' -f1)
    awk -v l="$L" 'BEGIN{exit !(l<2.5)}' && break
    sleep 15
  done
  N=$P$i
  adb shell am force-stop com.gymprofit.app
  sleep 2
  adb shell rm -f /sdcard/$N.mp4 /data/misc/perfetto-traces/$N.pftrace
  (adb shell "cat /data/local/tmp/traza.cfg | perfetto --txt -c - -o /data/misc/perfetto-traces/$N.pftrace" > /dev/null 2>&1 &)
  sleep 2
  (adb shell screenrecord --bit-rate 8000000 --time-limit 13 /sdcard/$N.mp4 &)
  sleep 1.5
  adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
      -n com.gymprofit.app/es.pmdm.gymprofit.ui.activities.MainActivity "$@" > /dev/null
  sleep 13
  while [ -n "$(adb shell pidof screenrecord | tr -d '\r')" ]; do sleep 1; done
  while [ -n "$(adb shell pidof perfetto | tr -d '\r')" ]; do sleep 1; done
  adb pull /sdcard/$N.mp4 $S/$N.mp4 > /dev/null
  adb pull /data/misc/perfetto-traces/$N.pftrace $S/$N.pftrace > /dev/null
  python -I $H/fotogramas.py $S/$N.mp4 $S/fotos_$N > $S/fotos_$N.txt
  R=$(python -I $H/oro.py $S/fotos_$N)
  echo "carga $L · $R"
  case "$R" in *ninguno*) : ;; *) echo "HUECO en $N"; exit 0 ;; esac
  i=$((i+1))
done
