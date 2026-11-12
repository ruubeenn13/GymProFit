// ============================================================
// recorrido.js — prueba de carga de la API de GymProFit con k6 (GP-221).
//
// Cada usuario virtual hace, en bucle, el recorrido de un usuario típico, con las mismas
// llamadas que hace la app (mapeadas desde app/GymProFit, ver README.md):
//   1. Entra: POST /auth/login y GET /usuarios/username/{u} para su id.
//   2. Abre Inicio: las diez llamadas de InicioFragment.cargarTodo(), más una
//      GET /ejercicios/{id} por récord, como InicioProgreso.
//   3. Guarda una sesión: POST /sesiones/completa y el resumen que abre la app después.
//   4. Abre el diario de hoy: comidas del día y /diario.
//   5. Busca un alimento: GET /alimentos/buscar.
// Entre paso y paso, 1 a 3 s de pausa: una persona no encadena pantallas en milisegundos.
//
// NUNCA contra producción: setup() se niega si BASE_URL no es local.
// Las cuentas se crean en setup() y se borran en teardown() con DELETE /usuarios/me.
//
// Variables: BASE_URL (http://host.docker.internal:8080/api), VUS (10), DURACION (3m),
// EJERCICIO_ID (860), RUN (sufijo de las cuentas; por defecto la hora).
// ============================================================
import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080/api';
const VUS = parseInt(__ENV.VUS || '10', 10);
const DURACION = __ENV.DURACION || '3m';
const EJERCICIO_ID = parseInt(__ENV.EJERCICIO_ID || '860', 10);
// El sufijo lo fija setup() y llega a cada usuario virtual en sus datos: calcularlo aquí
// daría uno distinto en cada VU, que se inicializa por separado.
const RUN_FIJO = __ENV.RUN || null;
const CLAVE = 'Carga1234.';
const BUSQUEDAS = ['pollo', 'arroz', 'leche', 'manzana', 'yogur', 'pan', 'huevo', 'atun'];

const fallos = new Counter('fallos_recorrido');
const PASOS = ['login', 'usuario', 'inicio', 'sesion', 'resumen', 'diario', 'buscar'];

export const options = {
  setupTimeout: '20m',
  teardownTimeout: '20m',
  scenarios: {
    recorrido: { executor: 'constant-vus', vus: VUS, duration: DURACION, gracefulStop: '30s' },
  },
  thresholds: {
    'http_req_duration{fase:recorrido}': ['p(95)<800'],
    'http_req_failed{fase:recorrido}': ['rate==0'],
    // Sin umbral real: solo para que el resumen traiga el p95 de cada paso.
    ...Object.fromEntries(PASOS.map((p) => [`http_req_duration{paso:${p}}`, ['max>=0']])),
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// Cada cuenta, una IP propia: el límite de peticiones va por IP, y cien usuarios reales no
// salen todos de la misma. La API toma la última entrada de X-Forwarded-For (IpCliente):
// en local no hay proxy delante, así que es la que pone k6; en Render, el proxy añade la
// IP real detrás de lo que mande el cliente, y esta cabecera ya no cuenta.
function ip(i) {
  return `10.77.${Math.floor(i / 250)}.${(i % 250) + 1}`;
}

function cabeceras(i, token, extra) {
  const h = { 'Content-Type': 'application/json', 'Accept-Language': 'es', 'X-Forwarded-For': ip(i) };
  if (token) h.Authorization = `Bearer ${token}`;
  return Object.assign(h, extra || {});
}

function nombre(run, i) {
  return `carga_${run}_${i}`;
}

function esLocal(url) {
  return /^https?:\/\/(localhost|127\.0\.0\.1|host\.docker\.internal)(:\d+)?\//.test(url);
}

function hoy() {
  return new Date().toISOString().slice(0, 10);
}

function lunesIso() {
  const d = new Date();
  const dia = (d.getDay() + 6) % 7;
  d.setDate(d.getDate() - dia);
  return `${d.toISOString().slice(0, 10)}T00:00:00`;
}

function hace(semanas) {
  const d = new Date();
  d.setDate(d.getDate() - semanas * 7);
  return `${d.toISOString().slice(0, 10)}T00:00:00`;
}

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export function setup() {
  if (!esLocal(BASE)) fail(`BASE_URL no es local (${BASE}): esta prueba no se corre contra producción`);
  const ej = http.get(`${BASE}/ejercicios/${EJERCICIO_ID}`, { headers: cabeceras(0) });
  // Sin token responde 401 o 403 si existe la ruta; 404/5xx es que algo no cuadra.
  if (ej.status >= 404) fail(`no responde ${BASE}: ${ej.status}`);
  const run = RUN_FIJO || String(Date.now()).slice(-7);
  for (let i = 0; i < VUS; i++) {
    const r = http.post(`${BASE}/auth/register`,
      JSON.stringify({ username: nombre(run, i), email: `${nombre(run, i)}@carga.local`, password: CLAVE, edad: 30 }),
      { headers: cabeceras(i), tags: { fase: 'setup' } });
    if (r.status !== 201) fail(`alta de ${nombre(run, i)}: ${r.status} ${r.body}`);
  }
  return { run };
}

function pedir(metodo, ruta, i, token, cuerpo, paso) {
  const r = http.request(metodo, `${BASE}${ruta}`, cuerpo ? JSON.stringify(cuerpo) : null,
    { headers: cabeceras(i, token), tags: { fase: 'recorrido', paso } });
  const ok = check(r, { [`${paso} 2xx`]: (x) => x.status >= 200 && x.status < 300 });
  if (!ok) fallos.add(1, { paso, estado: String(r.status) });
  return r;
}

export default function (datos) {
  const i = __VU - 1;
  const u = nombre(datos.run, i);

  // 1. Entrar
  const login = pedir('POST', '/auth/login', i, null, { username: u, password: CLAVE }, 'login');
  if (login.status !== 200) { sleep(2); return; }
  const token = login.json('token');
  const yo = pedir('GET', `/usuarios/username/${u}`, i, token, null, 'usuario');
  const uid = yo.status === 200 ? yo.json('id') : null;
  if (!uid) { sleep(2); return; }
  sleep(1 + Math.random() * 2);

  // 2. Inicio
  pedir('GET', `/rutinas/usuario/${uid}/activas`, i, token, null, 'inicio');
  pedir('GET', `/sesiones/usuario/${uid}`, i, token, null, 'inicio');
  pedir('GET', '/programas/seguido', i, token, null, 'inicio');
  pedir('GET', `/sesiones/usuario/${uid}/volumen-muscular?desde=${lunesIso()}`, i, token, null, 'inicio');
  pedir('GET', '/sesiones/historial', i, token, null, 'inicio');
  pedir('GET', `/sesiones/usuario/${uid}/volumen-semanal?desde=${hace(6)}&semanas=6`, i, token, null, 'inicio');
  const records = pedir('GET', '/records', i, token, null, 'inicio');
  let ids = [];
  try {
    const cuerpo = records.json();
    const lista = (cuerpo && cuerpo.records) || [];
    ids = [...new Set(lista.map((x) => x.ejercicioId).filter(Boolean))];
  } catch (e) { /* sin récords todavía: la primera vuelta de cada cuenta */ }
  ids.forEach((id) => pedir('GET', `/ejercicios/${id}`, i, token, null, 'inicio'));
  pedir('GET', `/comidas/usuario/${uid}/fecha/${hoy()}`, i, token, null, 'inicio');
  pedir('GET', `/diario?desde=${hoy()}&hasta=${hoy()}`, i, token, null, 'inicio');
  sleep(1 + Math.random() * 2);

  // 3. Guardar una sesión de tres series
  const inicio = new Date(Date.now() - 45 * 60000).toISOString().slice(0, 19);
  const sesion = pedir('POST', '/sesiones/completa', i, token, {
    claveIdempotencia: uuid(),
    fechaInicio: inicio,
    duracionMinutos: 45,
    valoracion: 4,
    completada: true,
    ejercicios: [{
      ejercicioId: EJERCICIO_ID,
      series: [1, 2, 3].map((n) => ({ numero: n, repeticiones: 10, peso: 40 + n * 2.5, completada: true })),
    }],
  }, 'sesion');
  const sid = sesion.status < 300 ? sesion.json('id') : null;
  if (sid) {
    pedir('GET', `/sesiones/${sid}`, i, token, null, 'resumen');
    pedir('GET', `/sesiones/${sid}/resumen`, i, token, null, 'resumen');
  }
  sleep(1 + Math.random() * 2);

  // 4. Diario de hoy
  pedir('GET', `/comidas/usuario/${uid}/fecha/${hoy()}`, i, token, null, 'diario');
  pedir('GET', `/diario?desde=${hoy()}&hasta=${hoy()}`, i, token, null, 'diario');
  sleep(1 + Math.random() * 2);

  // 5. Buscar un alimento
  const q = BUSQUEDAS[Math.floor(Math.random() * BUSQUEDAS.length)];
  pedir('GET', `/alimentos/buscar?q=${q}&page=0&size=20`, i, token, null, 'buscar');
  sleep(1 + Math.random() * 2);
}

// Borra las cuentas de esta pasada con el borrado de la propia API, que se lleva todo lo suyo.
export function teardown(datos) {
  let quedan = 0;
  for (let i = 0; i < VUS; i++) {
    const u = nombre(datos.run, i);
    const l = http.post(`${BASE}/auth/login`, JSON.stringify({ username: u, password: CLAVE }),
      { headers: cabeceras(i), tags: { fase: 'teardown' } });
    if (l.status !== 200) { quedan++; continue; }
    const b = http.request('DELETE', `${BASE}/usuarios/me`, JSON.stringify({ password: CLAVE }),
      { headers: cabeceras(i, l.json('token')), tags: { fase: 'teardown' } });
    if (b.status !== 200) quedan++;
  }
  if (quedan > 0) console.error(`teardown: ${quedan} cuentas carga_${datos.run}_* sin borrar`);
}

export function handleSummary(datos) {
  const salida = __ENV.SALIDA || `/resultados/vus-${VUS}.json`;
  return { [salida]: JSON.stringify(datos, null, 2), stdout: resumen(datos) };
}

function resumen(d) {
  const m = d.metrics;
  const dur = m['http_req_duration{fase:recorrido}'] || m.http_req_duration;
  const err = m['http_req_failed{fase:recorrido}'] || m.http_req_failed;
  const reqs = m.http_reqs;
  const v = (x, k) => (x && x.values && x.values[k] !== undefined ? x.values[k].toFixed(1) : '-');
  return `VUS=${VUS} p95=${v(dur, 'p(95)')}ms p99=${v(dur, 'p(99)')}ms med=${v(dur, 'med')}ms ` +
    `max=${v(dur, 'max')}ms errores=${err ? (err.values.rate * 100).toFixed(2) : '-'}% ` +
    `req/s=${v(reqs, 'rate')} fallos=${m.fallos_recorrido ? m.fallos_recorrido.values.count : 0}\n` +
    `  p95 por paso: ${PASOS.map((p) => `${p}=${v(m[`http_req_duration{paso:${p}}`], 'p(95)')}`).join(' ')}\n`;
}
