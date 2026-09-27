// ============================================================
// Entrada (GP-085)
// Solo pasan cuentas ADMIN; cualquier otra ve el mismo aviso que una contraseña
// equivocada, para no decir desde fuera qué cuentas existen ni cuáles no lo son.
// ============================================================
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/cliente';
import { Icono, type NombreIcono } from '../componentes/Icono';
import { useTituloDocumento } from '../componentes/Marco';
import { useSesion } from '../sesion/Sesion';
import { textoError } from '../util/useCarga';

const PUNTOS: { icono: NombreIcono; texto: string }[] = [
  { icono: 'monitoring', texto: 'Cómo va el producto, de un vistazo' },
  { icono: 'group', texto: 'Las cuentas, sin enseñar datos de salud' },
  { icono: 'fitness_center', texto: 'Ejercicios en español y en inglés' },
  { icono: 'nutrition', texto: 'Alimentos del catálogo' },
];

export function Entrada() {
  const { usuario, caducada, entrar, salir } = useSesion();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [nombre, setNombre] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [verContrasena, setVerContrasena] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useTituloDocumento('Entrar');
  const estado = ubicacion.state as { desde?: string; cerrarSesion?: boolean } | null;
  const destino = estado?.desde ?? '/';
  // «Cerrar sesión» llega aquí navegando, para que el aviso de cambios sin guardar
  // salte antes; la sesión se cierra ya en Entrada.
  // Después se limpia el estado de la navegación: si no, volver a entrar la cerraría otra vez.
  const cerrando = !!estado?.cerrarSesion && !!usuario;
  const yaCerrando = useRef(false);
  useEffect(() => {
    if (!cerrando || yaCerrando.current) return;
    yaCerrando.current = true;
    void salir().finally(() => {
      yaCerrando.current = false;
      navegar('/entrar', { replace: true });
    });
  }, [cerrando, salir, navegar]);
  if (usuario && !cerrando) return <Navigate to={destino} replace />;

  async function alEnviar(e: FormEvent) {
    e.preventDefault();
    if (!nombre.trim() || !contrasena) {
      setError('Escribe el usuario y la contraseña.');
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      await entrar(nombre.trim(), contrasena);
      navegar(destino, { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : textoError(err));
      setEnviando(false);
    }
  }

  return (
    <div className="entrada">
      <section className="entrada__lado">
        <div className="entrada__marca">
          <span className="marca marca--grande">GymProFit</span>
          <span className="insignia insignia--grande">Administración</span>
        </div>
        <div className="entrada__lema">
          <p className="entrada__titular">Todo el producto,<br />en una pestaña.</p>
          <ul className="entrada__puntos">
            {PUNTOS.map((p) => (
              <li key={p.texto}><Icono nombre={p.icono} />{p.texto}</li>
            ))}
          </ul>
        </div>
        <p className="entrada__cerraduras">
          <Icono nombre="verified_user" />
          Dos cerraduras: antes de esta página, tu cuenta de Google en Cloudflare Access; aquí, tu cuenta de
          administrador de GymProFit.
        </p>
      </section>
      <section className="entrada__formulario">
        <form onSubmit={alEnviar} noValidate aria-labelledby="titulo-entrar">
          {/* En el móvil, solo el formulario: la marca sube aquí. */}
          <div className="entrada__marca entrada__marca--movil">
            <span className="marca marca--grande">GymProFit</span>
            <span className="insignia insignia--grande">Administración</span>
          </div>
          <div className="entrada__cabeza">
            <h1 id="titulo-entrar">Entrar</h1>
            <span>Con tu cuenta de administrador de GymProFit.</span>
          </div>
          {caducada && !error && (
            <p className="nota" role="status"><Icono nombre="history" tamano={20} />La sesión ha caducado. Vuelve a entrar.</p>
          )}
          <div className="campo">
            <label htmlFor="usuario" className="campo__etiqueta entrada__etiqueta">
              Usuario <span className="campo__obligatorio">(obligatorio)</span>
            </label>
            <input id="usuario" className="campo__control entrada__control" type="text" autoComplete="username" aria-required="true"
                   value={nombre} onChange={(e) => setNombre(e.target.value)} aria-invalid={!!error} autoFocus />
          </div>
          <div className="campo">
            <label htmlFor="contrasena" className="campo__etiqueta entrada__etiqueta">
              Contraseña <span className="campo__obligatorio">(obligatorio)</span>
            </label>
            <div className="entrada__clave">
              <input id="contrasena" type={verContrasena ? 'text' : 'password'} autoComplete="current-password" aria-required="true"
                     value={contrasena} onChange={(e) => setContrasena(e.target.value)} aria-invalid={!!error}
                     aria-describedby={error ? 'error-entrada' : undefined} />
              <button type="button" className="boton-icono"
                      aria-label={verContrasena ? 'Ocultar la contraseña' : 'Enseñar la contraseña'}
                      aria-pressed={verContrasena} onClick={() => setVerContrasena((v) => !v)}>
                <Icono nombre={verContrasena ? 'visibility_off' : 'visibility'} />
              </button>
            </div>
          </div>
          {error && <p id="error-entrada" className="entrada__error" role="alert"><Icono nombre="error" tamano={20} />{error}</p>}
          <button type="submit" className="boton boton--principal entrada__boton" disabled={enviando}>
            {enviando ? 'Entrando…' : 'Entrar'}
          </button>
          <div className="entrada__notas">
            <p>Solo entran cuentas con rol de administrador. Cualquier otra recibe el mismo aviso que una contraseña equivocada.</p>
            <p>¿Has olvidado la contraseña? Se recupera desde la app, con el correo de la cuenta.</p>
          </div>
        </form>
      </section>
    </div>
  );
}
