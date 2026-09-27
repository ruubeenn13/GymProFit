// ============================================================
// Marco — barra lateral y cabecera comunes a todas las pantallas (GP-085)
// Plantillas es de la fase 2: no sale en la barra lateral.
// ============================================================
import { useEffect, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useSesion } from '../sesion/Sesion';
import { Icono, type NombreIcono } from './Icono';
import { PildorasEstado } from './EstadoApi';

const SECCIONES: { ruta: string; texto: string; icono: NombreIcono; iconoActivo: NombreIcono }[] = [
  { ruta: '/', texto: 'Resumen', icono: 'monitoring', iconoActivo: 'monitoring_relleno' },
  { ruta: '/usuarios', texto: 'Usuarios', icono: 'group', iconoActivo: 'group_relleno' },
  { ruta: '/ejercicios', texto: 'Ejercicios', icono: 'fitness_center', iconoActivo: 'fitness_center_relleno' },
  { ruta: '/alimentos', texto: 'Alimentos', icono: 'nutrition', iconoActivo: 'nutrition_relleno' },
];

const MAS_ADELANTE: { texto: string; icono: NombreIcono }[] = [
  { texto: 'Registro de acciones', icono: 'history' },
  { texto: 'Exportar datos de una cuenta', icono: 'download' },
  { texto: 'Importaciones', icono: 'cloud_sync' },
];

function BarraLateral() {
  const { usuario, salir } = useSesion();
  const navegar = useNavigate();

  async function alSalir() {
    await salir();
    navegar('/entrar', { replace: true });
  }

  return (
    <aside className="lateral">
      <div className="lateral__marca">
        <span className="marca">GymProFit</span>
        <span className="insignia">Admin</span>
      </div>
      <nav aria-label="Secciones" className="lateral__nav">
        {SECCIONES.map((s) => (
          <NavLink key={s.ruta} to={s.ruta} end={s.ruta === '/'} className="lateral__enlace">
            {({ isActive }) => (
              <>
                <Icono nombre={isActive ? s.iconoActivo : s.icono} />
                {s.texto}
              </>
            )}
          </NavLink>
        ))}
      </nav>
      <div className="lateral__adelante" aria-labelledby="mas-adelante">
        <span id="mas-adelante" className="lateral__subtitulo">Más adelante</span>
        <ul>
          {MAS_ADELANTE.map((m) => (
            <li key={m.texto}>
              <Icono nombre={m.icono} tamano={20} />
              {m.texto}
            </li>
          ))}
        </ul>
      </div>
      <div className="lateral__usuario">
        <span className="avatar" aria-hidden="true">{(usuario ?? '?').charAt(0).toUpperCase()}</span>
        <div className="lateral__quien">
          <span className="lateral__nombre">{usuario}</span>
          <span className="lateral__rol">Administrador</span>
        </div>
        <button type="button" className="boton-icono" aria-label="Cerrar sesión" onClick={alSalir}>
          <Icono nombre="logout" />
        </button>
      </div>
    </aside>
  );
}

/** Pone «titulo · GymProFit Admin» como título del documento. */
export function useTituloDocumento(titulo: string) {
  useEffect(() => {
    document.title = `${titulo} · GymProFit Admin`;
  }, [titulo]);
}

/**
 * @param props.titulo     título de la pantalla (h1)
 * @param props.subtitulo  texto al lado del título
 */
export function Marco({ titulo, subtitulo, children }: { titulo: string; subtitulo?: ReactNode; children: ReactNode }) {
  // Cada pantalla con su título: es lo primero que lee un lector de pantalla al
  // cambiar de sección, y lo que distingue las pestañas (WCAG 2.4.2).
  useTituloDocumento(titulo);
  return (
    <div className="marco">
      <a className="saltar" href="#contenido">Saltar al contenido</a>
      <BarraLateral />
      <main className="principal">
        <header className="cabecera">
          <div className="cabecera__titulo">
            <h1>{titulo}</h1>
            {subtitulo !== undefined && <span className="cabecera__subtitulo">{subtitulo}</span>}
          </div>
          <PildorasEstado />
        </header>
        <div id="contenido" className="principal__contenido" tabIndex={-1}>
          {children}
        </div>
      </main>
    </div>
  );
}
