// ============================================================
// Marco — navegación y cabecera comunes a todas las pantallas (GP-085, GP-120)
// Desde 1280 px, barra lateral con nombre e icono; de 768 a 1279, barra de iconos
// con el nombre al pasar el ratón o al llegar con el teclado; por debajo, el título
// arriba, cuatro pestañas abajo como en la app y la cuenta en un menú. El reparto lo
// hace pantallas.css: aquí está todo, y cada ancho enseña lo suyo.
// La API y el commit viven en el pie de la barra lateral (o en el menú del móvil);
// la cabecera queda para el título, el total y la acción principal.
// ============================================================
import { useEffect, useState, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useSesion } from '../sesion/Sesion';
import { Dialogo } from './Dialogo';
import { LineaApi } from './EstadoApi';
import { Icono, type NombreIcono } from './Icono';

// En el móvil, cuatro pestañas como en la app (DESIGN.md): Avisos se abre desde Alimentos.
const SECCIONES: { ruta: string; texto: string; icono: NombreIcono; iconoActivo: NombreIcono; soloLateral?: boolean }[] = [
  { ruta: '/', texto: 'Resumen', icono: 'monitoring', iconoActivo: 'monitoring_relleno' },
  { ruta: '/usuarios', texto: 'Usuarios', icono: 'group', iconoActivo: 'group_relleno' },
  { ruta: '/ejercicios', texto: 'Ejercicios', icono: 'fitness_center', iconoActivo: 'fitness_center_relleno' },
  { ruta: '/alimentos', texto: 'Alimentos', icono: 'nutrition', iconoActivo: 'nutrition_relleno' },
  { ruta: '/avisos', texto: 'Avisos', icono: 'warning', iconoActivo: 'warning', soloLateral: true },
];

/**
 * Cerrar sesión pasa por una navegación a Entrada, que es la que cierra la sesión:
 * así, con cambios sin guardar, el aviso de salir salta antes de perder nada.
 */
function useSalir() {
  const navegar = useNavigate();
  return () => navegar('/entrar', { state: { cerrarSesion: true } });
}

/**
 * @param props.bloque 'lateral' o 'pestanas'. En la barra lateral el enlace lleva
 *                     aria-label: en la barra de iconos el texto solo se ve al pasar.
 */
function Enlaces({ bloque }: { bloque: 'lateral' | 'pestanas' }) {
  return (
    <>
      {SECCIONES.filter((s) => bloque === 'lateral' || !s.soloLateral).map((s) => (
        <NavLink key={s.ruta} to={s.ruta} end={s.ruta === '/'} className={`${bloque}__enlace`}
                 aria-label={bloque === 'lateral' ? s.texto : undefined}>
          {({ isActive }) => (
            <>
              <Icono nombre={isActive ? s.iconoActivo : s.icono} />
              <span className={`${bloque}__texto`}>{s.texto}</span>
            </>
          )}
        </NavLink>
      ))}
    </>
  );
}

function BarraLateral() {
  const { usuario } = useSesion();
  const salir = useSalir();

  return (
    <aside className="lateral" aria-label="Barra lateral">
      <div className="lateral__marca">
        <img className="lateral__logo" src="/favicon.png" alt="" width={32} height={32} />
        <span className="marca">GymProFit</span>
        <span className="insignia">Admin</span>
      </div>
      <nav aria-label="Secciones" className="lateral__nav">
        <Enlaces bloque="lateral" />
      </nav>
      <div className="lateral__pie">
        <LineaApi />
        <div className="lateral__usuario">
          <span className="avatar" aria-hidden="true">{(usuario ?? '?').charAt(0).toUpperCase()}</span>
          <div className="lateral__quien">
            <span className="lateral__nombre">{usuario}</span>
            <span className="lateral__rol">Administrador</span>
          </div>
          <button type="button" className="boton-icono" aria-label="Cerrar sesión" title="Cerrar sesión" onClick={salir}>
            <Icono nombre="logout" />
          </button>
        </div>
      </div>
    </aside>
  );
}

/** Menú de cuenta del móvil: quién ha entrado, la línea de la API y cerrar sesión. */
function MenuCuenta({ abierto, alCerrar }: { abierto: boolean; alCerrar: () => void }) {
  const { usuario } = useSesion();
  const salir = useSalir();
  return (
    <Dialogo abierto={abierto} titulo="Cuenta" alCerrar={alCerrar}>
      <div className="menu-cuenta">
        <div className="lateral__usuario">
          <span className="avatar" aria-hidden="true">{(usuario ?? '?').charAt(0).toUpperCase()}</span>
          <div className="lateral__quien">
            <span className="lateral__nombre">{usuario}</span>
            <span className="lateral__rol">Administrador</span>
          </div>
        </div>
        <LineaApi />
        <div className="dialogo__acciones">
          <button type="button" className="boton" onClick={alCerrar}>Cerrar</button>
          <button type="button" className="boton" onClick={() => { alCerrar(); salir(); }}>
            <Icono nombre="logout" tamano={20} />Cerrar sesión
          </button>
        </div>
      </div>
    </Dialogo>
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
 * @param props.subtitulo  el total, al lado del título
 * @param props.accion     la acción principal de la pantalla, a la derecha
 */
export function Marco({ titulo, subtitulo, accion, children }: {
  titulo: string; subtitulo?: ReactNode; accion?: ReactNode; children: ReactNode;
}) {
  // Cada pantalla con su título: es lo primero que lee un lector de pantalla al
  // cambiar de sección, y lo que distingue las pestañas (WCAG 2.4.2).
  useTituloDocumento(titulo);
  const { usuario } = useSesion();
  const [menu, setMenu] = useState(false);
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
          {accion && <div className="cabecera__accion">{accion}</div>}
          <button type="button" className="boton-icono cabecera__menu" aria-label="Cuenta y estado de la API"
                  onClick={() => setMenu(true)}>
            <span className="avatar" aria-hidden="true">{(usuario ?? '?').charAt(0).toUpperCase()}</span>
          </button>
        </header>
        <div id="contenido" className="principal__contenido" tabIndex={-1}>
          {children}
        </div>
      </main>
      <nav aria-label="Pestañas" className="pestanas">
        <Enlaces bloque="pestanas" />
      </nav>
      <MenuCuenta abierto={menu} alCerrar={() => setMenu(false)} />
    </div>
  );
}
