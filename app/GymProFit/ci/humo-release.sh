#!/bin/sh
# ============================================================
# Prueba de humo de la release con R8 (GP-247), para el job app-release-r8 del CI.
#
# Instala la APK, arranca la app y recorre lo que se puede sin sesión (Bienvenida, el alta,
# Entrar, recuperar la contraseña); después entra con la cuenta de usar y tirar que el job
# acaba de crear en la API local y recorre las pestañas, el Perfil y «Tus datos». Falla si la
# app se cierra, si en el logcat de fallos aparece un FATAL EXCEPTION de la app, o si sin
# sesión guardada sale «Tu sesión ha terminado» (GP-258).
#
# Por qué existe: R8 quita lo que solo se nombra por reflexión, y la debug no lo enseña. En el
# lote 7 quitó BurbujaProfi (un <drawable class=…>) y la release se cerraba al abrir Inicio.
#
# Uso: sh ci/humo-release.sh <apk> <usuario>; la clave llega en HUMO_CLAVE, nunca como
# argumento (no sale en ninguna traza). Las capturas y el logcat quedan en HUMO_SALIDA.
# Se lanza con sh y sin -x: input text lleva la clave.
# ============================================================
set -eu

APK=$1
USUARIO=$2
CLAVE=${HUMO_CLAVE:?falta HUMO_CLAVE}
SALIDA=${HUMO_SALIDA:-humo}
PAQ=com.gymprofit.app
ID="resource-id=\"$PAQ:id"
MAIN=es.pmdm.gymprofit.ui.activities.MainActivity
mkdir -p "$SALIDA"

paso() { echo "== $*"; }

captura() { adb exec-out screencap -p > "$SALIDA/$1.png" 2>/dev/null || true; }

guardar_logcat() {
    adb logcat -d -b crash > "$SALIDA/logcat-crash.txt" 2>/dev/null || true
    adb logcat -d > "$SALIDA/logcat.txt" 2>/dev/null || true
}

fallar() {
    vivo || set -- "$*; la app ya no está viva"
    echo "::error::Humo de la release: $*"
    captura fallo
    guardar_logcat
    adb logcat -d -b crash || true
    exit 1
}

# Un FATAL EXCEPTION de nuestro proceso (no de otras apps del emulador): la línea siguiente
# del buffer de fallos dice de qué proceso es.
#
# Y lo que R8 rompe sin cerrar la app: un ObjectAnimator que busca su setter por el nombre de
# la propiedad y no lo encuentra («Method setPhaseX() … not found on target class»). Android
# solo lo avisa en el log y la animación no corre; en la debug no pasa.
sin_fatal() {
    if adb logcat -d -b crash | grep -A1 "FATAL EXCEPTION" | grep -q "Process: $PAQ"; then
        fallar "FATAL EXCEPTION en $PAQ ($1)"
    fi
    pid=$(adb shell pidof "$PAQ" 2> /dev/null | tr -d '
')
    if [ -n "$pid" ] && adb logcat -d --pid="$pid" | grep "PropertyValuesHolder" | grep -q "not found on target class"; then
        adb logcat -d --pid="$pid" | grep "PropertyValuesHolder"
        fallar "R8 ha quitado un setter que se usa por reflexión ($1)"
    fi
}

vivo() { adb shell pidof "$PAQ" > /dev/null 2>&1; }

# Centro del primer nodo de la entrada (un nodo por línea) que contiene el trozo dado.
centro_en() {
    grep -F "$1" | head -1 \
        | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p' \
        | awk '{ print int(($1 + $3) / 2), int(($2 + $4) / 2) }'
}

# La jerarquía de vistas en XML, un nodo por línea. uiautomator a veces no la saca si la
# pantalla aún se mueve: se reintenta.
#
# Si encima hay un «X isn't responding» del sistema (en el emulador recién arrancado le pasa
# al lanzador), uiautomator solo ve ese diálogo: se pulsa «Esperar» y se vuelve a mirar. No es
# de la app: un ANR suyo lo verían sin_fatal o vivo.
volcar() {
    for _ in 1 2 3 4 5; do
        if adb shell uiautomator dump /sdcard/humo.xml > /dev/null 2>&1; then
            arbol=$(adb shell cat /sdcard/humo.xml | tr '>' '\n')
            anr=$(printf '%s\n' "$arbol" | centro_en 'resource-id="android:id/aerr_wait"')
            if [ -z "$anr" ]; then
                printf '%s\n' "$arbol"
                return 0
            fi
            echo "(diálogo de ANR del sistema: Esperar)" >&2
            # shellcheck disable=SC2086
            adb shell input tap $anr
        fi
        sleep 1
    done
}

# Centro del primer nodo en pantalla cuya línea contiene el trozo dado, p. ej. un resource-id.
centro() { volcar | centro_en "$1"; }

# Espera a que aparezca un nodo y escribe su centro. Corre dentro de $(…), así que no puede
# llamar a fallar (saldría solo de la subshell): devuelve 1 y quien llama falla. Si la app
# se cierra mientras, deja de esperar.
esperar() {
    t=0
    while [ "$t" -lt "${2:-40}" ]; do
        c=$(centro "$1")
        if [ -n "$c" ]; then echo "$c"; return 0; fi
        vivo || return 1
        sleep 1
        t=$((t + 1))
    done
    return 1
}

ver() { esperar "$ID/$1\"" "${2:-40}" > /dev/null || fallar "no aparece $1"; sin_fatal "$1"; }

tocar() {
    c=$(esperar "$ID/$1\"") || fallar "no aparece $1 para tocarlo"
    # shellcheck disable=SC2086
    adb shell input tap $c
}

atras() { adb shell input keyevent KEYCODE_BACK; sleep 1; }

# Atrás hasta ver la vista dada: el primer atrás puede solo cerrar el teclado.
volver_a() {
    for _ in 1 2 3; do
        atras
        if [ -n "$(centro "$ID/$1\"")" ]; then sin_fatal "$1"; return 0; fi
    done
    fallar "atrás no vuelve a $1"
}

# Como el icono: sin ACTION_MAIN y CATEGORY_LAUNCHER, MainActivity no pasa por el arranque
# (PantallaArranque.esArranque) y monta las pestañas aunque no haya sesión.
abrir_como_el_icono() {
    adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER         -n "$PAQ/$MAIN" > /dev/null
}

# Sin sesión guardada, «Tu sesión ha terminado» (avisoSesionCerrada en Entrar) no sale nunca
# (GP-258): lo vería quien acaba de instalar la app. Espera a la vista dada y falla si el aviso
# aparece antes o con ella.
esperar_sin_aviso() {
    t=0
    while :; do
        arbol=$(volcar)
        if printf '%s\n' "$arbol" | grep -qF "$ID/avisoSesionCerrada\""; then
            captura aviso-sin-sesion
            fallar "«Tu sesión ha terminado» sin sesión guardada ($2)"
        fi
        if printf '%s\n' "$arbol" | grep -qF "$ID/$1\""; then break; fi
        vivo || fallar "la app se ha cerrado ($2)"
        t=$((t + 1))
        [ "$t" -lt 60 ] || fallar "no aparece $1 ($2)"
        sleep 1
    done
    sin_fatal "$2"
}

pestana() {
    adb shell am start -W -f 0x24000000 -n "$PAQ/$MAIN" --ei nav_tab "$1" > /dev/null
    sleep 3
    vivo || fallar "la app se ha cerrado en la pestaña $1"
    sin_fatal "pestaña $1"
    captura "pestana-$1"
}

paso "Instalar"
# Desde cero: nada de una instalación anterior (ni su sesión) en el AVD.
adb uninstall "$PAQ" > /dev/null 2>&1 || true
adb install "$APK" > /dev/null
# Android 13 o más pide el permiso de notificaciones con un diálogo del sistema: se concede
# antes para que no tape la pantalla.
adb shell pm grant "$PAQ" android.permission.POST_NOTIFICATIONS 2> /dev/null || true
# Los diálogos de error y de ANR del sistema tapan la pantalla: fuera. Los fallos de la app se
# ven igual, por el logcat.
adb shell settings put global hide_error_dialogs 1 2> /dev/null || true
adb logcat -b all -c

paso "Arranque sin sesión: Bienvenida, sin el aviso de sesión terminada"
abrir_como_el_icono
esperar_sin_aviso btnYaTengoCuenta "arranque desde el icono"
captura bienvenida

# Sin la categoría LAUNCHER (una notificación, un acceso directo) MainActivity no pasa por
# PantallaArranque.esArranque: antes montaba Inicio sin sesión, Inicio pedía sus datos sin
# token y el 401 abría Entrar con el aviso (los runs 43105641695 y 43113670636). Es el mismo
# camino que una MainActivity recreada mientras el arranque decide.
paso "Abierta sin la categoría LAUNCHER y sin sesión: Bienvenida, sin el aviso"
adb shell am force-stop "$PAQ"
adb shell am start -W -n "$PAQ/$MAIN" > /dev/null
esperar_sin_aviso btnYaTengoCuenta "abierta sin LAUNCHER"

paso "El alta"
tocar btnEmpezar
ver contenedorAlta
captura alta
volver_a btnYaTengoCuenta

paso "Entrar y recuperar la contraseña"
tocar btnYaTengoCuenta
ver btnEntrar
captura entrar
tocar tvOlvidastePassword
ver etIdentificador
captura recuperar
volver_a btnEntrar

# El 401 de una contraseña mal escrita no es una sesión que se acaba (GP-258): antes, el
# primero tras instalar reabría Entrar con «Tu sesión ha terminado».
paso "Entrar con la contraseña mal: sin el aviso de sesión terminada"
tocar etUsuario
adb shell input text "$USUARIO"
tocar etPassword
adb shell input text "NoEsLaClave-258"
atras
tocar btnEntrar
sleep 4
esperar_sin_aviso btnEntrar "contraseña mal escrita"
captura entrar-clave-mal

paso "Entrar con la cuenta de usar y tirar"
tocar etPassword
# Fuera la contraseña mala: el cursor queda al final y se borra hacia atrás.
adb shell input keyevent KEYCODE_MOVE_END
# shellcheck disable=SC2046
adb shell input keyevent $(printf 'KEYCODE_DEL %.0s' $(seq 1 30))
adb shell input text "$CLAVE"
# Fuera el teclado: puede tapar el botón.
atras
tocar btnEntrar

paso "Inicio"
# La primera vez tras entrar, encima de Inicio se abre la pregunta de los avisos
# (AvisosActivity): también se recorre, y se cierra con «Ahora no».
t=0
while :; do
    arbol=$(volcar)
    if printf '%s
' "$arbol" | grep -qF "$ID/avatarInicio\""; then break; fi
    if printf '%s
' "$arbol" | grep -qF "$ID/btnAhoraNo\""; then
        sin_fatal "avisos"
        captura avisos
        tocar btnAhoraNo
    fi
    vivo || fallar "la app se ha cerrado al entrar"
    t=$((t + 1))
    [ "$t" -lt 60 ] || fallar "no aparece avatarInicio"
    sleep 1
done
sin_fatal "Inicio"
captura inicio

paso "Pestañas"
for n in 1 2 3 0; do pestana "$n"; done

paso "Perfil y Tus datos"
tocar avatarInicio
ver btnEditarDatos
captura perfil
tocar btnEditarDatos
sleep 3
vivo || fallar "la app se ha cerrado al abrir Tus datos"
sin_fatal "Tus datos"
captura tus-datos

sleep 2
vivo || fallar "la app se ha cerrado al final"
sin_fatal "final"
guardar_logcat
paso "Humo superado: la release con R8 ha recorrido todas las pantallas sin cerrarse"
