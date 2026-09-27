# Product

<!-- impeccable:product-schema 1 -->

<!-- Escrito desde DEC-035 (documentacion/PRODUCT-DECISIONS.md), el README del diseño aprobado
     (documentacion/diseno/2026-09-27-administracion/README.md) y el encargo de GP-120.
     Solo hechos confirmados por el propietario. -->

## Platform

web

## Users

Una sola persona: el propietario de GymProFit. Es una herramienta interna, no un producto para
terceros. La usa sobre todo en el ordenador, para trabajar el catálogo; y también desde el móvil,
para tres cosas rápidas: mirar el resumen, buscar una cuenta y activarla (o desactivarla).

## Product Purpose

Administrar GymProFit sin tocar la base de datos: ver cómo va el producto (Resumen), gestionar
cuentas sin ver datos de salud (Usuarios) y poner al día el catálogo en español e inglés
(Ejercicios y Alimentos). Existe porque el panel de la app Android no servía para el trabajo real:
752 de 873 ejercicios activos se llamaban igual en español que en inglés, y traducir un catálogo
así en la pantalla de un móvil no se hace.

Éxito: el propietario resuelve cada tarea en el mínimo de pasos, sin dudar de qué ha hecho cada
botón, y recorre el catálogo de uno en uno sin perder el hilo.

## Positioning

No compite con nada: es la mesa de trabajo del propietario. Lo que la distingue de un panel
genérico es lo que no hace: nunca toca la base, nunca enseña datos de salud y no guarda nada en el
navegador.

## Operating Context

- Escritorio (1280 px o más) para las sesiones largas de revisión del catálogo, con «Guardar y
  siguiente» para ir de un ejercicio o alimento al siguiente pendiente.
- Móvil para consultas puntuales: el resumen, buscar una cuenta, activarla.
- Detrás de Cloudflare Access (solo el correo del propietario) y de una cuenta ADMIN de GymProFit.
  Los tokens viven solo en memoria: al recargar, se vuelve a entrar.
- Toda lectura y escritura pasa por la API (`/admin/...`), que exige ADMIN en cada ruta.

## Capabilities and Constraints

- Pantallas: Entrada, Resumen, Usuarios, Ejercicios y Alimentos. Plantillas llega en la fase 2 y
  no se anuncia antes de existir.
- Solo en español. Solo en tema oscuro.
- React + Vite + TypeScript con React Router. Sin librerías de componentes ni de gráficas, y
  ninguna dependencia nueva.
- Sin datos de salud: de una cuenta se ve cuántas sesiones y comidas tiene, no cuáles.
- Sin secretos: el repositorio es público; lo único compilado dentro es la URL de la API.
- CSP que solo deja conectar con `https://api.gymprofit.app` y prohíbe los marcos.

## Brand Commitments

- Marca GymProFit, en oscuro, con la paleta, las fuentes (Barlow y Barlow Condensed) y los iconos
  (Material Symbols Rounded) de la app. Aprobados por el propietario el 27-09-2026.
- Tono: directo y en español de España; frases cortas que dicen qué pasa y qué se puede hacer.
- Nada de marketing dentro de la herramienta.

## Evidence on Hand

- Diseño aprobado: `documentacion/diseno/2026-09-27-administracion/` (capturas a 1440 px y el
  lienzo HTML con las medidas).
- Tokens: `admin/src/estilos/tokens.css`.
- Decisión de arquitectura: DEC-035.

## Product Principles

1. Densidad, rapidez y claridad antes que expresión.
2. Lo que no existe no se anuncia.
3. Cada acción dice qué ha hecho, y lo irreversible pide confirmación explícita.
4. La web no decide nada de seguridad: como mucho, evita pedir lo que la API va a rechazar.

## Accessibility & Inclusion

WCAG 2.2 AA como en la app (DEC-019): todo alcanzable con teclado y foco visible, zonas de toque
de 44 px en pantallas táctiles, contraste de texto 4,5:1 y de bordes de control y barras de
gráfica 3:1, `prefers-reduced-motion` respetado.
