// ============================================================
// Rutas de la web de administración (GP-085)
// Todo salvo Entrada exige sesión; sin ella (al recargar, o si caduca) se vuelve
// a Entrada recordando a dónde se iba.
// ============================================================
import type { ReactNode } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { ProveedorEstadoApi } from './componentes/EstadoApi';
import { Alimentos } from './paginas/Alimentos';
import { Ejercicios } from './paginas/Ejercicios';
import { Entrada } from './paginas/Entrada';
import { Resumen } from './paginas/Resumen';
import { Usuarios } from './paginas/Usuarios';
import { useSesion } from './sesion/Sesion';

function ConSesion({ children }: { children: ReactNode }) {
  const { usuario } = useSesion();
  const ubicacion = useLocation();
  if (!usuario) return <Navigate to="/entrar" replace state={{ desde: ubicacion.pathname }} />;
  return <ProveedorEstadoApi>{children}</ProveedorEstadoApi>;
}

export function App() {
  return (
    <Routes>
      <Route path="/entrar" element={<Entrada />} />
      <Route path="/" element={<ConSesion><Resumen /></ConSesion>} />
      <Route path="/usuarios" element={<ConSesion><Usuarios /></ConSesion>} />
      <Route path="/ejercicios" element={<ConSesion><Ejercicios /></ConSesion>} />
      <Route path="/alimentos" element={<ConSesion><Alimentos /></ConSesion>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
