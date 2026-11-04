#!/bin/sh
# N arranques en frío grabados y analizados con oro.py y caja.py, con carga baja antes de cada uno.
# Uso: sh serie.sh <prefijo> <n> <segundos> [extras de am start...]
H=$(cd "$(dirname "$0")" && pwd)  # los scripts
S=${GP_SALIDA:-$PWD}              # vídeos, fotogramas y trazas
P=$1; N=$2; T=$3; shift 3
i=1
while [ $i -le $N ]; do
  for k in $(seq 1 30); do
    L=$(adb shell cat /proc/loadavg | cut -d' ' -f1)
    awk -v l="$L" 'BEGIN{exit !(l<2.5)}' && break
    sleep 15
  done
  adb logcat -c
  sh $H/graba.sh $P$i 1 $T "$@" > /dev/null 2>&1
  D=$(python -I $H/ui.py top)
  RS=$(adb logcat -d -s Arranque:W | grep -c respaldo)
  echo "carga $L · respaldo $RS · $D · $(python -I $H/oro.py $S/fotos_$P$i)"
  echo "      $(python -I $H/caja.py $S/$P$i.mp4)"
  i=$((i+1))
done
