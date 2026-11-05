"""Mide el arranque en frío hasta Inicio (GP-223). Uso: python mide.py <componente> <veces> <etiqueta>

Por cada vez: force-stop, logcat -c, am start -W del componente lanzador y, del logcat
(con hora de época), la hora del «START u0» y la del «Displayed …MainActivity». La
diferencia es del toque (el intent del lanzador) al primer fotograma de MainActivity.
"""
import re
import statistics
import subprocess
import sys
import time

comp, veces, etiqueta = sys.argv[1], int(sys.argv[2]), sys.argv[3]


def adb(*a):
    return subprocess.run(['adb', *a], capture_output=True, text=True, encoding='utf-8', errors='replace').stdout


res = []
for i in range(veces):
    adb('shell', 'am', 'force-stop', 'com.gymprofit.app')
    time.sleep(2)
    adb('logcat', '-c')
    w = adb('shell', 'am', 'start', '-W', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER', '-n', comp)
    time.sleep(5)
    log = adb('logcat', '-d', '-v', 'epoch', '-s', 'ActivityTaskManager:I')
    ini = re.search(r'^\s*(\d+\.\d+).*START u0 .*com\.gymprofit\.app', log, re.M)
    fin = re.search(r'^\s*(\d+\.\d+).*Displayed com\.gymprofit\.app/es\.pmdm\.gymprofit\.ui\.activities\.MainActivity.*?\+(\S+)', log, re.M)
    tot = re.search(r'TotalTime: (\d+)', w)
    if ini and fin:
        ms = (float(fin.group(1)) - float(ini.group(1))) * 1000
        res.append(ms)
        print('%s #%d: toque->MainActivity %.0f ms (Displayed +%s; am start TotalTime %s)' % (
            etiqueta, i + 1, ms, fin.group(2), tot.group(1) if tot else '?'), flush=True)
    else:
        print('%s #%d: sin datos' % (etiqueta, i + 1), log[-600:], flush=True)
if res:
    print('%s: mediana %.0f ms, min %.0f, max %.0f (n=%d)' % (etiqueta, statistics.median(res), min(res), max(res), len(res)))
