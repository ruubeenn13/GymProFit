#!/bin/sh
# Graba un arranque en frío y analiza sus fotogramas.
# Uso: sh graba.sh <nombre> <escala_animaciones> <segundos> [extras de am start...]
export MSYS_NO_PATHCONV=1
H=$(cd "$(dirname "$0")" && pwd)  # los scripts
S=${GP_SALIDA:-$PWD}              # vídeos, fotogramas y trazas
N=$1; E=$2; T=$3; shift 3
adb shell settings put global animator_duration_scale "$E"
adb shell am force-stop com.gymprofit.app
sleep 2
adb shell rm -f /sdcard/$N.mp4
(adb shell screenrecord --bit-rate 8000000 --time-limit "$T" /sdcard/$N.mp4 &)
sleep 1.5
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
    -n "${COMP:-com.gymprofit.app/es.pmdm.gymprofit.ui.activities.MainActivity}" "$@" > /dev/null
sleep "$T"
while [ -n "$(adb shell pidof screenrecord | tr -d "
")" ]; do sleep 1; done
adb pull /sdcard/$N.mp4 $S/$N.mp4 > /dev/null
adb shell settings put global animator_duration_scale 1
python -I $H/fotogramas.py $S/$N.mp4 $S/fotos_$N > $S/fotos_$N.txt
head -3 $S/fotos_$N.txt
awk 'NR>3' $S/fotos_$N.txt | awk '{printf "%s:%s/%s  ", $1, $3, $5} END {print ""}' | fold -w 180
