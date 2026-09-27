import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { rutas } from './App';
import { ProveedorSesion } from './sesion/Sesion';
import './estilos/tokens.css';
import './estilos/base.css';
import './estilos/pantallas.css';

// Router de datos: es el que deja bloquear la navegación con cambios sin guardar
// (useBlocker), también cuando se sale por la barra lateral o por un enlace.
const router = createBrowserRouter(rutas);

createRoot(document.getElementById('raiz')!).render(
  <StrictMode>
    <ProveedorSesion>
      <RouterProvider router={router} />
    </ProveedorSesion>
  </StrictMode>,
);
