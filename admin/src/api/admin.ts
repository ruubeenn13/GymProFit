// ============================================================
// Rutas de la API que usa la web, con sus tipos (GP-085)
// Las fechas llegan en ISO: con zona las del reloj del servidor (alta, último
// acceso) y sin zona las fechas de calendario de Madrid (hoy, lunes, fecha).
// ============================================================
import { api, type Cliente } from './cliente';

/** Página de una lista paginada (PageDTO de la API). */
export interface Pagina<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface Resumen {
  hoy: string;
  cuentas: { total: number; altasSemana: number; activas: number };
  sesiones: { hoy: number; semana: number; total: number };
  entrenaronSemana: number;
  comidaHoy: number;
  altasPorSemana: { lunes: string; altas: number }[];
  sesionesPorDia: { fecha: string; sesiones: number }[];
  catalogo: { ejerciciosActivos: number; ejerciciosSinRevisar: number; alimentosCatalogo: number; alimentosSinIngles: number };
}

export type Rol = 'ADMIN' | 'USER' | 'GUEST';

export interface Cuenta {
  id: number;
  username: string;
  email: string;
  fechaRegistro: string | null;
  ultimoAcceso: string | null;
  rol: Rol | null;
  activo: boolean;
}

export interface FichaCuenta {
  cuenta: Cuenta;
  sesiones: number;
  comidas: number;
}

export interface Ejercicio {
  id: number;
  nombre: string;
  nombreEn: string | null;
  grupoMuscular: string;
  equipamiento: string;
  dificultad: string;
  activo: boolean;
  nombreRevisado: boolean;
}

export interface EjercicioDetalle extends Ejercicio {
  descripcion: string | null;
  descripcionEn: string | null;
  instrucciones: string | null;
  instruccionesEn: string | null;
  musculoPrimario: string | null;
  musculoPrimarioEn: string | null;
  imagenUrl: string | null;
  imagenUrl2: string | null;
  origen: 'WGER' | 'FREE_EXERCISE_DB' | 'MANUAL';
  equipoNecesario: string | null;
  rutinas: number;
}

export interface EjercicioGuardar {
  nombre: string;
  nombreEn: string | null;
  descripcion: string | null;
  descripcionEn: string | null;
  instrucciones: string | null;
  instruccionesEn: string | null;
  grupoMuscular: string;
  musculoPrimario: string | null;
  musculoPrimarioEn: string | null;
  equipamiento: string;
  dificultad: string;
  activo: boolean;
  nombreRevisado: boolean;
}

export interface ResumenEjercicios {
  activos: number;
  sinRevisar: number;
  equipamientos: { valor: string; etiqueta: string; etiquetaEn: string }[];
}

export interface Alimento {
  id: number;
  nombre: string;
  nombreEn: string | null;
  marca: string | null;
  categoria: string | null;
  barcode: string | null;
  calorias: number;
  proteinas: number | null;
  carbohidratos: number | null;
  grasas: number | null;
  fibra: number | null;
  porcionGramos: number | null;
  activo: boolean;
  origen: 'OPEN_FOOD_FACTS' | 'MANUAL';
  /** De dónde salen los datos (GP-127): básicos de Ciqual o USDA, Open Food Facts; null, a mano. */
  fuente?: 'CIQUAL' | 'USDA' | 'OFF' | null;
  /** true en los básicos, curados uno a uno. */
  revisado?: boolean;
}

export interface ResumenAlimentos {
  catalogo: number;
  sinIngles: number;
  categorias: string[];
  /** Cuántos del catálogo hay de cada fuente (GP-127). */
  porFuente?: Partial<Record<'CIQUAL' | 'USDA' | 'OFF' | 'MANUAL', number>>;
  /** Productos de España para elegir, materializados o no (GP-164). */
  productos?: number;
}

/** Cambios de un alimento para PATCH /alimentos/{id}; un texto vacío borra el campo. */
export type AlimentoCambios = Partial<{
  nombre: string;
  nombreEn: string;
  marca: string;
  barcode: string;
  categoria: string;
  calorias: number;
  proteinas: number;
  carbohidratos: number;
  grasas: number;
  fibra: number;
  porcionGramos: number;
  activo: boolean;
}>;

function consulta(parametros: Record<string, string | number | boolean | null | undefined>): string {
  const q = new URLSearchParams();
  for (const [clave, valor] of Object.entries(parametros)) {
    if (valor !== null && valor !== undefined && valor !== '') q.set(clave, String(valor));
  }
  const texto = q.toString();
  return texto ? `?${texto}` : '';
}

/**
 * Las rutas de la web, sobre un cliente. Por defecto, el de la web.
 *
 * @param cliente cliente autenticado
 */
export function rutasAdmin(cliente: Cliente = api) {
  return {
    resumen: (senal?: AbortSignal) => cliente.pedir<Resumen>('/admin/resumen', { senal }),

    cuentas: (f: { q?: string; rol?: string; activo?: string; page: number; size: number }, senal?: AbortSignal) =>
      cliente.pedir<Pagina<Cuenta>>('/admin/cuentas' + consulta(f), { senal }),
    cuenta: (id: number, senal?: AbortSignal) => cliente.pedir<FichaCuenta>(`/admin/cuentas/${id}`, { senal }),
    cambiarActivo: (id: number) => cliente.pedir<unknown>(`/admin/usuarios/${id}/toggle-activo`, { metodo: 'PATCH' }),
    cambiarRol: (id: number, rol: Rol) =>
      cliente.pedir<unknown>(`/admin/usuarios/${id}/rol${consulta({ nuevoRol: rol })}`, { metodo: 'PATCH' }),
    borrarCuenta: (id: number, confirmacion: string, motivo: string) =>
      cliente.pedir<void>(`/admin/cuentas/${id}`, { metodo: 'DELETE', cuerpo: { confirmacion, motivo } }),

    ejercicios: (f: { q?: string; grupo?: string; equipamiento?: string; sinRevisar?: boolean; page: number; size: number },
                 senal?: AbortSignal) =>
      cliente.pedir<Pagina<Ejercicio>>('/admin/ejercicios' + consulta(f), { senal }),
    resumenEjercicios: (senal?: AbortSignal) => cliente.pedir<ResumenEjercicios>('/admin/ejercicios/resumen', { senal }),
    ejercicio: (id: number, senal?: AbortSignal) => cliente.pedir<EjercicioDetalle>(`/admin/ejercicios/${id}`, { senal }),
    guardarEjercicio: (id: number, datos: EjercicioGuardar) =>
      cliente.pedir<EjercicioDetalle>(`/admin/ejercicios/${id}`, { metodo: 'PUT', cuerpo: datos }),

    alimentos: (f: { q?: string; categoria?: string; sinIngles?: boolean; fuente?: string; page: number; size: number },
                senal?: AbortSignal) =>
      cliente.pedir<Pagina<Alimento>>('/admin/alimentos' + consulta(f), { senal }),
    resumenAlimentos: (senal?: AbortSignal) => cliente.pedir<ResumenAlimentos>('/admin/alimentos/resumen', { senal }),
    guardarAlimento: (id: number, cambios: AlimentoCambios) =>
      cliente.pedir<unknown>(`/alimentos/${id}`, { metodo: 'PATCH', cuerpo: cambios }),

    salud: (senal?: AbortSignal) => cliente.pedirPublico<{ status: string }>('/actuator/health', senal),
    info: (senal?: AbortSignal) => cliente.pedirPublico<{ commit?: string }>('/actuator/info', senal),
  };
}

export const admin = rutasAdmin();
