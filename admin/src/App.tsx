// ============================================================
// Rutas de la web de administración (GP-085)
// Todo salvo Entrada exige sesión; sin ella (al recargar, o si caduca) se vuelve
// a Entrada recordando a dónde se iba.
// ============================================================
import { lazy, Suspense, type ComponentType, type ReactNode } from 'react';
import { Navigate, useLocation, type RouteObject } from 'react-router-dom';
import { ProveedorEstadoApi } from './componentes/EstadoApi';
import { Entrada } from './paginas/Entrada';
import { useSesion } from './sesion/Sesion';

// Entrada va en el paquete inicial: es lo primero que se ve y lo único antes de entrar.
// El resto se descarga al abrirlo, así que Entrada no espera a todo el panel.
// Si la descarga falla (sin red, o una versión nueva ya desplegada que retiró los
// archivos viejos), se dice y se ofrece recargar, en vez de dejar la pantalla en blanco.
function FalloCarga() {
  return (
    <div className="estado-lista estado-lista--error" role="alert">
      <span className="estado-lista__titulo">No se ha podido cargar esta pantalla</span>
      <span>Puede que haya una versión nueva de la web o que no haya conexión. Al recargar hay que volver a entrar.</span>
      <button type="button" className="boton" onClick={() => window.location.reload()}>Recargar</button>
    </div>
  );
}

function pantalla<K extends string>(cargar: () => Promise<Record<K, ComponentType>>, nombre: K) {
  return lazy(() => cargar().then((m) => ({ default: m[nombre] }), () => ({ default: FalloCarga })));
}

const Resumen = pantalla(() => import('./paginas/Resumen'), 'Resumen');
const Usuarios = pantalla(() => import('./paginas/Usuarios'), 'Usuarios');
const Ejercicios = pantalla(() => import('./paginas/Ejercicios'), 'Ejercicios');
const Alimentos = pantalla(() => import('./paginas/Alimentos'), 'Alimentos');
const Avisos = pantalla(() => import('./paginas/Avisos'), 'Avisos');

function Cargando() {
  return (
    <div className="estado-lista" role="status" aria-live="polite">
      <div className="cargando-barra" />
      <span>Cargando…</span>
    </div>
  );
}

function ConSesion({ children }: { children: ReactNode }) {
  const { usuario } = useSesion();
  const ubicacion = useLocation();
  if (!usuario) return <Navigate to="/entrar" replace state={{ desde: ubicacion.pathname }} />;
  return (
    <ProveedorEstadoApi>
      <Suspense fallback={<Cargando />}>{children}</Suspense>
    </ProveedorEstadoApi>
  );
}

/** Las rutas de la web, para el router de datos de main.tsx. */
export const rutas: RouteObject[] = [
  { path: '/entrar', element: <Entrada /> },
  { path: '/', element: <ConSesion><Resumen /></ConSesion> },
  { path: '/usuarios', element: <ConSesion><Usuarios /></ConSesion> },
  { path: '/ejercicios', element: <ConSesion><Ejercicios /></ConSesion> },
  { path: '/alimentos', element: <ConSesion><Alimentos /></ConSesion> },
  { path: '/avisos', element: <ConSesion><Avisos /></ConSesion> },
  { path: '*', element: <Navigate to="/" replace /> },
];
