// ============================================================
// Pinta una pantalla del panel como en la web: con sesión, estado de la API y un
// router de datos (el que permite bloquear la navegación con cambios sin guardar).
// '/' es un marcador para comprobar a dónde se ha ido.
// ============================================================
import { configure, render } from '@testing-library/react';
import type { ReactNode } from 'react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { ProveedorEstadoApi } from '../componentes/EstadoApi';
import { ProveedorSesion } from '../sesion/Sesion';

// Estas pantallas encadenan varias cargas simuladas; con la máquina cargada, el
// segundo por defecto de findBy se queda corto y el test falla sin motivo.
configure({ asyncUtilTimeout: 3000 });

/**
 * @param ruta     ruta de la pantalla, p. ej. '/alimentos'
 * @param pantalla la pantalla
 */
export function pintarPantalla(ruta: string, pantalla: ReactNode) {
  const router = createMemoryRouter([
    { path: ruta, element: <ProveedorEstadoApi>{pantalla}</ProveedorEstadoApi> },
    { path: '/', element: <p>Pantalla de inicio</p> },
    { path: '/entrar', element: <p>Pantalla de entrada</p> },
  ], { initialEntries: [ruta] });
  render(<ProveedorSesion><RouterProvider router={router} /></ProveedorSesion>);
  return router;
}
