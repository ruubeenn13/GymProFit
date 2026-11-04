"""Ayudante de adb para el emulador (GP-223). Uso: python ui.py <orden> [args]

Órdenes: ids · tap <id|texto> · esperar <id|texto> [s] · escribir <texto> · top ·
entrar <correo> <fichero_clave>
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


def adb(*a):
    r = subprocess.run(['adb', *a], capture_output=True, text=True, encoding='utf-8', errors='replace')
    return r.stdout


def dump():
    for _ in range(3):
        adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
        x = adb('shell', 'cat', '/sdcard/ui.xml')
        if '<hierarchy' in x:
            return ET.fromstring(x[x.index('<?xml') if '<?xml' in x else x.index('<hierarchy'):])
        time.sleep(0.5)
    raise RuntimeError('sin volcado')


def nodos():
    yield from dump().iter('node')


def buscar(clave):
    for n in nodos():
        rid = n.get('resource-id', '').split('/')[-1]
        if rid == clave or n.get('text') == clave or n.get('content-desc') == clave:
            return n
    return None


def centro(n):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap(clave):
    n = buscar(clave)
    if n is None:
        raise SystemExit('no está: ' + clave)
    x, y = centro(n)
    adb('shell', 'input', 'tap', str(x), str(y))


def esperar(clave, s=20):
    fin = time.time() + s
    while time.time() < fin:
        if buscar(clave) is not None:
            return True
        time.sleep(0.7)
    raise SystemExit('no apareció: ' + clave)


def top():
    x = adb('shell', 'dumpsys', 'activity', 'activities')
    m = re.search(r'topResumedActivity=ActivityRecord\{\S+ \S+ (\S+)', x)
    return m.group(1).split('.')[-1] if m else '?'


def main():
    o = sys.argv[1]
    if o == 'ids':
        for n in nodos():
            rid = n.get('resource-id', '').split('/')[-1]
            t = n.get('text') or n.get('content-desc')
            if rid or t:
                print(rid, '|', t, '|', n.get('bounds'))
    elif o == 'tap':
        tap(sys.argv[2])
    elif o == 'esperar':
        esperar(sys.argv[2], float(sys.argv[3]) if len(sys.argv) > 3 else 20)
    elif o == 'escribir':
        adb('shell', 'input', 'text', sys.argv[2])
    elif o == 'top':
        print(top())
    elif o == 'entrar':
        correo, clave = sys.argv[2], open(sys.argv[3]).read().strip()
        if buscar('btnYaTengoCuenta') is not None:
            tap('btnYaTengoCuenta')
        esperar('etUsuario')
        tap('etUsuario')
        adb('shell', 'input', 'keycombination', '113', '29')
        adb('shell', 'input', 'keyevent', '67')
        adb('shell', 'input', 'text', correo)
        tap('etPassword')
        adb('shell', 'input', 'text', clave)
        adb('shell', 'input', 'keyevent', '4')
        time.sleep(0.5)
        tap('btnEntrar')
    else:
        print(__doc__)


if __name__ == '__main__':
    main()
