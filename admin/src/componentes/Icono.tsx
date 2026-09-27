// ============================================================
// Icono — Material Symbols Rounded, importados como SVG (GP-085)
// Solo los que se usan, desde src/iconos, sin fuente de iconos ni CDN. Toman el
// color del texto (fill: currentColor en base.css) y son decorativos: el nombre
// accesible lo pone siempre el control que los lleva.
// ============================================================
import arrowForward from '../iconos/arrow_forward.svg?raw';
import block from '../iconos/block.svg?raw';
import check from '../iconos/check.svg?raw';
import checkCircle from '../iconos/check_circle.svg?raw';
import chevronLeft from '../iconos/chevron_left.svg?raw';
import chevronRight from '../iconos/chevron_right.svg?raw';
import circleRelleno from '../iconos/circle_relleno.svg?raw';
import close from '../iconos/close.svg?raw';
import cloudSync from '../iconos/cloud_sync.svg?raw';
import deleteForever from '../iconos/delete_forever.svg?raw';
import download from '../iconos/download.svg?raw';
import error from '../iconos/error.svg?raw';
import expandMore from '../iconos/expand_more.svg?raw';
import fitnessCenter from '../iconos/fitness_center.svg?raw';
import fitnessCenterRelleno from '../iconos/fitness_center_relleno.svg?raw';
import formatListNumbered from '../iconos/format_list_numbered.svg?raw';
import group from '../iconos/group.svg?raw';
import groupRelleno from '../iconos/group_relleno.svg?raw';
import history from '../iconos/history.svg?raw';
import image from '../iconos/image.svg?raw';
import logout from '../iconos/logout.svg?raw';
import monitoring from '../iconos/monitoring.svg?raw';
import monitoringRelleno from '../iconos/monitoring_relleno.svg?raw';
import nutrition from '../iconos/nutrition.svg?raw';
import nutritionRelleno from '../iconos/nutrition_relleno.svg?raw';
import openInNew from '../iconos/open_in_new.svg?raw';
import refresh from '../iconos/refresh.svg?raw';
import removeModerator from '../iconos/remove_moderator.svg?raw';
import search from '../iconos/search.svg?raw';
import shieldPerson from '../iconos/shield_person.svg?raw';
import translate from '../iconos/translate.svg?raw';
import verifiedUser from '../iconos/verified_user.svg?raw';
import visibility from '../iconos/visibility.svg?raw';
import visibilityOff from '../iconos/visibility_off.svg?raw';
import warning from '../iconos/warning.svg?raw';

const ICONOS = {
  arrow_forward: arrowForward,
  block,
  check,
  check_circle: checkCircle,
  chevron_left: chevronLeft,
  chevron_right: chevronRight,
  circle_relleno: circleRelleno,
  close,
  cloud_sync: cloudSync,
  delete_forever: deleteForever,
  download,
  error,
  expand_more: expandMore,
  fitness_center: fitnessCenter,
  fitness_center_relleno: fitnessCenterRelleno,
  format_list_numbered: formatListNumbered,
  group,
  group_relleno: groupRelleno,
  history,
  image,
  logout,
  monitoring,
  monitoring_relleno: monitoringRelleno,
  nutrition,
  nutrition_relleno: nutritionRelleno,
  open_in_new: openInNew,
  refresh,
  remove_moderator: removeModerator,
  search,
  shield_person: shieldPerson,
  translate,
  verified_user: verifiedUser,
  visibility,
  visibility_off: visibilityOff,
  warning,
} as const;

export type NombreIcono = keyof typeof ICONOS;

/**
 * @param props.nombre  icono de src/iconos
 * @param props.tamano  lado en px: 16, 18, 20 o 22 (por defecto)
 */
export function Icono({ nombre, tamano, className }: { nombre: NombreIcono; tamano?: 16 | 18 | 20; className?: string }) {
  const clases = ['icono', tamano ? `icono--${tamano}` : '', className ?? ''].filter(Boolean).join(' ');
  // SVG propios del repositorio, sin datos de nadie: no hay nada que escapar.
  return <span className={clases} aria-hidden="true" dangerouslySetInnerHTML={{ __html: ICONOS[nombre] }} />;
}
