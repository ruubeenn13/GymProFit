/* GymProFit · el movimiento de la portada (GP-205, DEC-053)
 *
 * Es una mejora: sin este script la página está entera y quieta. Si el visitante
 * no ha pedido menos movimiento, carga GSAP, ScrollTrigger y Lenis desde nuestro
 * dominio (las rutas, versionadas, van en data-libs) y ata al scroll lo que CSS
 * no puede atar. Las pide cuando acaba la entrada del hero (CSS, no las necesita)
 * o en cuanto el visitante desplaza la página, lo que llegue antes: pedirlas al
 * cargar competía con la entrada y retrasaba el LCP medio segundo en un 4G lento. Con prefers-reduced-motion: reduce no descarga nada. La
 * coreografía está en web/DISENO.md.
 */
(function () {
  "use strict";

  var menos = window.matchMedia("(prefers-reduced-motion: reduce)");
  if (menos.matches) return;

  var ENTRADA_MS = 1300;
  var yo = document.currentScript;
  var libs = ((yo && yo.getAttribute("data-libs")) || "").split(/\s+/).filter(Boolean);
  if (!libs.length) return;

  cuandoHagaFalta(function () { cargar(libs, iniciar); });

  /** Llama a listo una sola vez: tras el load y la entrada del hero, o al primer gesto de desplazamiento. */
  function cuandoHagaFalta(listo) {
    var hecho = false;
    var gestos = ["scroll", "wheel", "touchstart", "keydown"];
    function ya() {
      if (hecho) return;
      hecho = true;
      gestos.forEach(function (g) { window.removeEventListener(g, ya); });
      listo();
    }
    gestos.forEach(function (g) { window.addEventListener(g, ya, { passive: true }); });
    // La entrada del hero acaba hacia 1,3 s (portada.css): después, nada compite con ella
    function trasLaEntrada() { setTimeout(ya, ENTRADA_MS); }
    if (document.readyState === "complete") trasLaEntrada();
    else window.addEventListener("load", trasLaEntrada, { once: true });
  }

  /** Carga los scripts en orden y llama a listo cuando ha llegado el último. */
  function cargar(rutas, listo) {
    rutas.forEach(function (ruta, i) {
      var s = document.createElement("script");
      s.src = ruta;
      s.async = false; // async=false en scripts creados: se ejecutan en orden
      // Si una librería no llega, la página se queda como está, quieta y completa
      if (i === rutas.length - 1) s.onload = listo;
      document.head.appendChild(s);
    });
  }

  function iniciar() {
    var gsap = window.gsap;
    var ScrollTrigger = window.ScrollTrigger;
    if (!gsap || !ScrollTrigger) return;
    gsap.registerPlugin(ScrollTrigger);
    // En el móvil, la barra de direcciones cambia el alto de la ventana al bajar:
    // recalcular todo en cada cambio haría saltar lo que va atado al scroll.
    ScrollTrigger.config({ ignoreMobileResize: true });
    // Las fuentes pueden llegar después que los scripts y cambiar alturas
    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(function () { ScrollTrigger.refresh(); });
    }

    var raiz = document.documentElement;
    var mm = gsap.matchMedia();
    var conMovimiento = "(prefers-reduced-motion: no-preference)";

    // Todo vive dentro de matchMedia: si el visitante pide menos movimiento a
    // mitad de visita, GSAP lo deshace y la página vuelve a su estado quieto.
    mm.add(conMovimiento, function () {
      raiz.classList.add("movimiento");

      // Scroll suave. Lenis mueve el scroll nativo, así que sticky y el teclado siguen igual.
      var lenis = window.Lenis ? new window.Lenis({ lerp: 0.12 }) : null;
      var latido = function (t) { lenis.raf(t * 1000); };
      if (lenis) {
        lenis.on("scroll", ScrollTrigger.update);
        gsap.ticker.add(latido);
        gsap.ticker.lagSmoothing(0);
      }

      soltarElDorsal(gsap);
      revelarElProblema(gsap);

      return function () {
        raiz.classList.remove("movimiento");
        if (lenis) {
          gsap.ticker.remove(latido);
          lenis.destroy();
        }
      };
    });

    mm.add("(min-width: 900px) and " + conMovimiento, function () {
      return moverElMovil(gsap, ScrollTrigger);
    });

    mm.add("(max-width: 899px) and " + conMovimiento, function () {
      inclinarLosMoviles(gsap);
    });
  }

  /* Del dorsal al problema: el dorsal se suelta del pecho (de -2° a 0°) y sube más
     despacio que la página; el móvil se inclina hacia atrás al irse. */
  function soltarElDorsal(gsap) {
    var hero = { trigger: ".hero", start: "top top", end: "bottom top", scrub: 0.5 };
    gsap.to(".soltar", { rotate: 2, yPercent: -16, ease: "none", scrollTrigger: hero });
    gsap.to(".hero-movil", {
      rotateX: 10, yPercent: -8, scale: 0.96, transformPerspective: 1200, ease: "none",
      scrollTrigger: hero
    });
  }

  /* «Entrenas en serio. Comes a ojo.» entra por líneas al llegar, una vez. */
  function revelarElProblema(gsap) {
    var problema = document.querySelector(".problema");
    if (!problema) return;
    var lineas = problema.querySelectorAll("h2 > span > span");
    var texto = problema.querySelector("p");
    var tl = gsap.timeline({
      defaults: { ease: "expo.out" },
      scrollTrigger: { trigger: problema, start: "top 70%", once: true }
    });
    tl.from(lineas, { yPercent: 104, duration: 0.8, stagger: 0.09 })
      .from(texto, { y: 24, duration: 0.7 }, 0.25);
  }

  /* Escritorio: las capturas de los pilares pasan a un solo móvil que se queda
     quieto (sticky) y cambia de pantalla con una cortina cuando cada pilar llega
     al centro. Las figuras de cada pilar se quedan en su sitio, invisibles: la
     página mide lo mismo con este script que sin él (ver portada.css). Devuelve
     cómo deshacerlo. */
  function moverElMovil(gsap, ScrollTrigger) {
    var pantallas = document.querySelector(".pantallas");
    if (!pantallas) return;
    var destino = pantallas.querySelector(".pantalla");
    var pilares = Array.prototype.slice.call(document.querySelectorAll(".pilar"));
    var figuras = pilares.map(function (p) { return p.querySelector(".movil[data-pantalla]"); });
    var imagenes = figuras.map(function (f) { return f.querySelector("img"); });

    imagenes.forEach(function (img) {
      img.loading = "eager";
      destino.appendChild(img);
    });
    pantallas.hidden = false;

    var actual = -1;
    var cortinas = [];
    gsap.set(imagenes, { clipPath: "inset(100% 0% 0% 0%)" });
    mostrar(0, false);

    var disparadores = pilares.map(function (pilar, i) {
      return ScrollTrigger.create({
        trigger: pilar,
        start: "top 55%",
        end: "bottom 55%",
        onToggle: function (self) { if (self.isActive) mostrar(i, true); }
      });
    });

    function mostrar(i, animar) {
      if (i === actual) return;
      var baja = i > actual;
      pilares.forEach(function (p, j) { p.classList.toggle("activo", j === i); });
      pantallas.setAttribute("data-activa", figuras[i].getAttribute("data-pantalla"));
      imagenes.forEach(function (img, j) { img.style.zIndex = j === i ? 2 : (j === actual ? 1 : 0); });
      cortinas.forEach(function (c) { c.kill(); });
      cortinas = [];
      var desde = baja ? "inset(100% 0% 0% 0%)" : "inset(0% 0% 100% 0%)";
      if (animar) {
        cortinas.push(gsap.fromTo(imagenes[i], { clipPath: desde }, {
          clipPath: "inset(0% 0% 0% 0%)", duration: 0.42, ease: "power3.inOut"
        }));
      } else {
        gsap.set(imagenes[i], { clipPath: "inset(0% 0% 0% 0%)" });
      }
      actual = i;
    }

    return function () {
      disparadores.forEach(function (d) { d.kill(); });
      imagenes.forEach(function (img, j) {
        img.removeAttribute("style");
        figuras[j].appendChild(img);
      });
      pilares.forEach(function (p) { p.classList.remove("activo"); });
      pantallas.removeAttribute("data-activa");
      pantallas.hidden = true;
    };
  }

  /* Móvil: cada captura llega inclinada hacia atrás y se endereza al subir. */
  function inclinarLosMoviles(gsap) {
    gsap.utils.toArray(".pilar .movil").forEach(function (movil) {
      gsap.fromTo(movil, { rotateX: 16, transformPerspective: 1000 }, {
        rotateX: 0, ease: "none",
        scrollTrigger: { trigger: movil, start: "top bottom", end: "center 60%", scrub: 0.5 }
      });
    });
  }
})();
