package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.pruebas.ContadorSentencias;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// PlatosTest — los platos propios (lote 1.6.6, A2 y A3; DEC-047)
// Con el contexto levantado y JWT de verdad (DEC-014). El id de un plato tiene dueño:
// el de otra cuenta y el que no existe dan el mismo 403. Los ingredientes llegan en el
// cuerpo y también tienen dueño. Las reglas 2 a 6 del lote, con el ejemplo del tablero
// 10: seis ingredientes que suman 1.896 kcal y 100 g de proteína.
// ============================================================
@Import(ContadorSentencias.class)
@DisplayName("Lote 1.6.6 — platos propios")
class PlatosTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private final LocalDate hoy = LocalDate.now();
    private final List<Alimento> tablero = new ArrayList<>();
    private final int[] gramosTablero = {300, 80, 300, 150, 100, 20};

    @BeforeEach
    void sembrar() {
        tablero.clear();
        tablero.add(catalogo("Lentejas pardinas kzplato", 338, "24", "51", "1.5"));
        tablero.add(catalogo("Chorizo kzplato", 461, "24", "2", "38"));
        tablero.add(catalogo("Patata kzplato", 77, "2", "17", "0.1"));
        tablero.add(catalogo("Zanahoria kzplato", 41, "1", "7", "0.2"));
        tablero.add(catalogo("Cebolla kzplato", 40, "1.3", "6.9", "0.1"));
        tablero.add(catalogo("Aceite de oliva kzplato", 900, "0", "0", "100"));
    }

    // --- A2 · Crear y la cuenta -------------------------------------------

    @Test
    @DisplayName("regla 4 sin peso: 4 raciones de 474 kcal y 25 g, al crearlo, en la lista, en la ficha y en el diario")
    void tablero_sin_peso() throws Exception {
        JsonNode plato = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size())));
        assertThat(plato.get("total").get("calorias").asInt()).isEqualTo(1896);
        assertThat(plato.get("total").get("proteinas").decimalValue()).isEqualByComparingTo("100");
        assertThat(plato.get("porRacion").get("calorias").asInt()).isEqualTo(474);
        assertThat(plato.get("porRacion").get("proteinas").decimalValue()).isEqualByComparingTo("25");
        assertThat(plato.get("porRacion").get("gramos").isNull()).isTrue();
        assertThat(plato.get("ingredientes")).hasSize(6);

        int id = plato.get("id").asInt();
        JsonNode ficha = json(pedir(owner, "GET /alimentos/" + id));
        assertThat(ficha.get("calorias").asInt()).isEqualTo(474);
        assertThat(ficha.get("platoRaciones").asInt()).isEqualTo(4);
        assertThat(ficha.get("platoPesoGramos").isNull()).isTrue();
        assertThat(ficha.get("raciones")).hasSize(1);

        JsonNode linea = anadir(owner, id, "CENA", racion(ficha), "1").get("linea");
        assertThat(linea.get("caloriasTotales").asInt()).isEqualTo(474);
        assertThat(linea.get("proteinasTotales").decimalValue()).isEqualByComparingTo("25");
        assertThat(linea.get("raciones").decimalValue()).isEqualByComparingTo("1");
        assertThat(linea.get("racionSinPeso").asBoolean()).isTrue();
        assertThat(linea.get("alimentoEsPlato").asBoolean()).isTrue();

        JsonNode enLista = loMio(owner).get("platos").get(0);
        assertThat(enLista.get("id").asInt()).isEqualTo(id);
        assertThat(enLista.get("calorias").asInt()).isEqualTo(474);
    }

    @Test
    @DisplayName("regla 4 con peso: 1.800 g dan 105 kcal por 100 g y 473 por ración; en gramos, como cualquier alimento")
    void tablero_con_peso() throws Exception {
        JsonNode plato = crear(owner, platoJson("Lentejas de casa", 4, "1800", tableroJson(tablero.size())));
        assertThat(plato.get("porRacion").get("calorias").asInt()).isEqualTo(473);
        assertThat(plato.get("porRacion").get("gramos").decimalValue()).isEqualByComparingTo("450");
        JsonNode alimento = plato.get("alimento");
        assertThat(alimento.get("calorias").asInt()).isEqualTo(105);
        assertThat(alimento.get("raciones").get(0).get("gramos").decimalValue()).isEqualByComparingTo("450");

        int id = plato.get("id").asInt();
        assertThat(anadir(owner, id, "CENA", racion(alimento), "1").get("linea").get("caloriasTotales").asInt())
                .isEqualTo(473);
        JsonNode enGramos = anadirGramos(owner, id, "COMIDA", "200").get("linea");
        assertThat(enGramos.get("caloriasTotales").asInt()).isEqualTo(210);
        assertThat(enGramos.get("racionId").isNull()).isTrue();
    }

    @Test
    @DisplayName("regla 3: sin peso no hay gramos; los de una build anterior son raciones de 100 g")
    void sin_peso_gramos_son_raciones() throws Exception {
        JsonNode plato = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size())));
        JsonNode linea = anadirGramos(owner, plato.get("id").asInt(), "CENA", "150").get("linea");
        assertThat(linea.get("raciones").decimalValue()).isEqualByComparingTo("1.5");
        assertThat(linea.get("racionSinPeso").asBoolean()).isTrue();
        assertThat(linea.get("caloriasTotales").asInt()).isEqualTo(711);
    }

    @Test
    @DisplayName("regla 2: los valores son la suma de los ingredientes, cada uno como se apuntaría, y por raciones o gramos")
    void suma_de_ingredientes() throws Exception {
        Alimento pan = catalogo("Pan kzplato", 250, "9", "48", "3");
        Integer rebanada = racionDe(pan, "1 rebanada", "1 slice", "28");
        String ingredientes = "{\"alimentoId\":" + pan.getId() + ",\"racionId\":" + rebanada + ",\"raciones\":2},"
                + "{\"alimentoId\":" + tablero.get(5).getId() + ",\"cantidadGramos\":10}";
        JsonNode plato = crear(owner, platoJson("Tostadas", 1, null, ingredientes));
        // 56 g de pan: 140 kcal; 10 g de aceite: 90 kcal.
        assertThat(plato.get("total").get("calorias").asInt()).isEqualTo(230);
        assertThat(plato.get("ingredientes").get(0).get("raciones").decimalValue()).isEqualByComparingTo("2");
        assertThat(plato.get("ingredientes").get(0).get("cantidadGramos").decimalValue()).isEqualByComparingTo("56");
        assertThat(plato.get("porRacion").get("calorias").asInt()).isEqualTo(230);
    }

    @Test
    @DisplayName("regla 2: si después cambia un ingrediente, el plato no cambia hasta que se vuelve a guardar")
    void ingrediente_cambiado() throws Exception {
        Alimento salsa = propio(owner, "Salsa kzplato", 200, "10");
        JsonNode plato = crear(owner, platoJson("Pasta", 2, null,
                "{\"alimentoId\":" + salsa.getId() + ",\"cantidadGramos\":100}"));
        int id = plato.get("id").asInt();
        pedir(owner, "PATCH /alimentos/" + salsa.getId(), "{\"calorias\":400,\"proteinas\":20}").andExpect(status().isOk());
        assertThat(json(pedir(owner, "GET /alimentos/" + id)).get("calorias").asInt()).isEqualTo(100);
        cambiar(owner, id, platoJson("Pasta", 2, null, "{\"alimentoId\":" + salsa.getId() + ",\"cantidadGramos\":100}"));
        assertThat(json(pedir(owner, "GET /alimentos/" + id)).get("calorias").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("añadir dos veces 1 ración suma raciones en la misma línea")
    void sumar_raciones() throws Exception {
        JsonNode plato = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size())));
        int id = plato.get("id").asInt();
        anadir(owner, id, "CENA", racion(plato.get("alimento")), "1");
        JsonNode linea = anadir(owner, id, "CENA", racion(plato.get("alimento")), "1").get("linea");
        assertThat(linea.get("raciones").decimalValue()).isEqualByComparingTo("2");
        assertThat(linea.get("caloriasTotales").asInt()).isEqualTo(948);
    }

    @Test
    @DisplayName("400: más de 900 kcal por 100 g, un peso menor que 1 g por ración, raciones o ingredientes fuera de rango")
    void validaciones() throws Exception {
        String aceite = "{\"alimentoId\":" + tablero.get(5).getId() + ",\"cantidadGramos\":100}";
        crearMal(owner, platoJson("Aceite", 1, "50", aceite), 400);
        crearMal(owner, platoJson("Aceite", 4, "3", aceite), 400);
        crearMal(owner, platoJson("Aceite", 0, null, aceite), 400);
        crearMal(owner, platoJson("Aceite", 100, null, aceite), 400);
        crearMal(owner, platoJson("Aceite", 1, null, ""), 400);
        crearMal(owner, platoJson("", 1, null, aceite), 400);
        crearMal(owner, platoJson("x".repeat(101), 1, null, aceite), 400);
        StringBuilder muchos = new StringBuilder(aceite);
        for (int i = 0; i < 50; i++) muchos.append(',').append(aceite);
        crearMal(owner, platoJson("Aceite", 1, null, muchos.toString()), 400);
        crearMal(owner, platoJson("Aceite", 1, null, "{\"alimentoId\":" + tablero.get(5).getId() + "}"), 400);
        // Exactamente 900 por 100 g, sí.
        crear(owner, platoJson("Aceite", 1, "100", aceite));
        // Menos de 1 g por ración, aunque no salgan kcal de más (el agua no tiene).
        Alimento agua = catalogo("Agua kzplato", 0, "0", "0", "0");
        String conAgua = "{\"alimentoId\":" + agua.getId() + ",\"cantidadGramos\":500}";
        crearMal(owner, platoJson("Caldo", 4, "3", conAgua), 400);
        crear(owner, platoJson("Caldo", 4, "4", conAgua));
    }

    // --- A2 · De quién es -------------------------------------------------

    @Test
    @DisplayName("DEC-014: el plato de otra cuenta y el que no existe dan el mismo 403, al leer, cambiar y borrar")
    void ajeno_e_inexistente() throws Exception {
        int id = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(2))).get("id").asInt();
        int noExiste = id + 100000;
        String cuerpo = platoJson("Mío", 1, null, tableroJson(1));
        for (String verbo : new String[]{"GET", "PUT", "DELETE"}) {
            ResultActions ajeno = pedir(attacker, verbo + " /platos/" + id, verbo.equals("PUT") ? cuerpo : null);
            ResultActions nada = pedir(attacker, verbo + " /platos/" + noExiste, verbo.equals("PUT") ? cuerpo : null);
            assertThat(ajeno.andReturn().getResponse().getStatus()).as(verbo).isEqualTo(403);
            assertThat(nada.andReturn().getResponse().getStatus()).as(verbo).isEqualTo(403);
            assertThat(ajeno.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .isEqualTo(nada.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        }
        // Un alimento que no es un plato tampoco es un plato.
        assertThat(estado(owner, "GET /platos/" + tablero.get(0).getId())).isEqualTo(403);
        // Y el ADMIN no ve los alimentos de nadie (DEC-027).
        Usuario admin = crearUsuario("__plato_admin__", RoleType.ADMIN);
        assertThat(estado(admin, "GET /platos/" + id)).isEqualTo(403);
        // Sigue entero.
        assertThat(json(pedir(owner, "GET /platos/" + id)).get("activo").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("un ingrediente de otra cuenta o que no existe, el mismo 403; un plato como ingrediente, 400")
    void ingredientes_ajenos_y_platos() throws Exception {
        Alimento suyo = propio(attacker, "Batido secreto kzplato", 100, "5");
        ResultActions ajeno = pedir(owner, "POST /platos",
                platoJson("Robo", 1, null, "{\"alimentoId\":" + suyo.getId() + ",\"cantidadGramos\":100}"));
        ResultActions nada = pedir(owner, "POST /platos",
                platoJson("Robo", 1, null, "{\"alimentoId\":" + (suyo.getId() + 100000) + ",\"cantidadGramos\":100}"));
        assertThat(ajeno.andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(nada.andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(ajeno.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo(nada.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(ajeno.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("Batido");

        int miPlato = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(2))).get("id").asInt();
        crearMal(owner, platoJson("Doble", 1, null, "{\"alimentoId\":" + miPlato + ",\"cantidadGramos\":100}"), 400);
        crearMal(owner, platoJson("Doble", 1, null, "{\"alimentoId\":" + miPlato + ",\"racionIndice\":0,\"raciones\":1}"), 400);
        // El plato de otra cuenta como ingrediente: el 403 de lo ajeno, no el 400 de un plato.
        int suyoPlato = crear(attacker, platoJson("Suyo", 1, null, tableroJson(1))).get("id").asInt();
        crearMal(owner, platoJson("Doble", 1, null, "{\"alimentoId\":" + suyoPlato + ",\"cantidadGramos\":100}"), 403);
    }

    @Test
    @DisplayName("el invitado no crea, no lee y no tiene «Mis platos»: 403")
    void invitado() throws Exception {
        int id = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(2))).get("id").asInt();
        assertThat(pedir(guest, "POST /platos", platoJson("X", 1, null, tableroJson(1))).andReturn().getResponse()
                .getStatus()).isEqualTo(403);
        assertThat(estado(guest, "GET /platos/" + id)).isEqualTo(403);
        assertThat(estado(guest, "DELETE /platos/" + id)).isEqualTo(403);
        assertThat(estado(guest, "GET /alimentos/mios")).isEqualTo(403);
    }

    @Test
    @DisplayName("PATCH y PUT /alimentos sobre un plato, 400: sus valores no se escriben a mano")
    void patch_alimento_plato() throws Exception {
        int id = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(2))).get("id").asInt();
        pedir(owner, "PATCH /alimentos/" + id, "{\"calorias\":10}").andExpect(status().isBadRequest());
        pedir(owner, "PUT /alimentos", "{\"id\":" + id + ",\"nombre\":\"X\",\"calorias\":10,\"activo\":true}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("borrar del todo un alimento que está en un plato es un 409, y el plato sigue entero")
    void admin_no_rompe_un_plato() throws Exception {
        int id = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size()))).get("id").asInt();
        Usuario admin = crearUsuario("__plato_admin2__", RoleType.ADMIN);
        assertThat(estado(admin, "DELETE /alimentos/" + tablero.get(0).getId() + "/permanente")).isEqualTo(409);
        assertThat(json(pedir(owner, "GET /platos/" + id)).get("ingredientes")).hasSize(6);
        // El ingrediente propio, tampoco.
        Alimento salsa = propio(owner, "Salsa kzplato", 200, "10");
        crear(owner, platoJson("Pasta", 1, null, "{\"alimentoId\":" + salsa.getId() + ",\"cantidadGramos\":50}"));
        assertThat(estado(owner, "DELETE /alimentos/" + salsa.getId() + "/permanente")).isEqualTo(409);
    }

    // --- A3 · Lo mío ------------------------------------------------------

    @Test
    @DisplayName("«Mis platos»: solo lo propio, platos aparte, del usado más recientemente al menos y los nunca usados, los nuevos primero")
    void lo_mio_orden_y_aislamiento() throws Exception {
        int viejo = crear(owner, platoJson("Viejo", 1, null, tableroJson(1))).get("id").asInt();
        int usadoAyer = crear(owner, platoJson("Usado ayer", 1, null, tableroJson(1))).get("id").asInt();
        int usadoHoy = crear(owner, platoJson("Usado hoy", 1, null, tableroJson(1))).get("id").asInt();
        int nuevo = crear(owner, platoJson("Nuevo", 1, null, tableroJson(1))).get("id").asInt();
        int masNuevo = crear(owner, platoJson("Más nuevo", 1, null, tableroJson(1))).get("id").asInt();
        Alimento creado = propio(owner, "Kéfir kzplato", 60, "3");
        Alimento creadoDespues = propio(owner, "Hummus kzplato", 170, "8");
        anadirEn(owner, usadoAyer, hoy.minusDays(1), "CENA");
        anadirEn(owner, usadoHoy, hoy, "CENA");
        crear(attacker, platoJson("Suyo", 1, null, tableroJson(1)));
        pedir(owner, "DELETE /platos/" + viejo).andExpect(status().isNoContent());

        JsonNode mio = loMio(owner);
        // Los nunca usados, los más nuevos primero.
        assertThat(ids(mio.get("platos"))).containsExactly(usadoHoy, usadoAyer, masNuevo, nuevo);
        assertThat(ids(mio.get("alimentos"))).containsExactly(creadoDespues.getId(), creado.getId());
        assertThat(mio.get("platos").get(0).get("ultima").get("raciones").decimalValue()).isEqualByComparingTo("1");
        assertThat(mio.get("platos").get(0).get("favorito").asBoolean()).isFalse();
        JsonNode suyo = loMio(attacker);
        assertThat(suyo.get("platos")).hasSize(1);
        assertThat(suyo.get("alimentos")).isEmpty();
    }

    @Test
    @DisplayName("«Mis platos» no hace un viaje a la base por fila")
    void lo_mio_consultas() throws Exception {
        crear(owner, platoJson("Uno", 1, null, tableroJson(1)));
        int uno = contarLoMio(owner);
        for (int i = 0; i < 4; i++) {
            int id = crear(owner, platoJson("Más " + i, 1, null, tableroJson(2))).get("id").asInt();
            anadirEn(owner, id, hoy, "CENA");
            propio(owner, "Creado kzplato " + i, 100, "1");
        }
        assertThat(contarLoMio(owner)).isEqualTo(uno);
    }

    @Test
    @DisplayName("en la búsqueda y en los favoritos, un plato dice que lo es")
    void plato_en_las_listas() throws Exception {
        int id = crear(owner, platoJson("Lentejas kzbuscaplato", 4, "1800", tableroJson(2))).get("id").asInt();
        em.flush();
        em.clear();
        JsonNode buscado = null;
        for (JsonNode n : json(pedir(owner, "GET /alimentos/buscar?q=kzbuscaplato")).get("content")) {
            if (n.get("id") != null && n.get("id").asInt() == id) buscado = n;
        }
        assertThat(buscado).isNotNull();
        assertThat(buscado.get("platoRaciones").asInt()).isEqualTo(4);
        assertThat(buscado.get("platoPesoGramos").decimalValue()).isEqualByComparingTo("1800");
        pedir(owner, "PUT /favoritos/" + id).andExpect(status().isOk());
        JsonNode fav = json(pedir(owner, "GET /favoritos")).get("favoritos").get(0);
        assertThat(fav.get("platoRaciones").asInt()).isEqualTo(4);
    }

    // --- Regla 5 · Editar o borrar un plato no cambia lo apuntado ---------

    @Test
    @DisplayName("regla 5 · cambiar las raciones: la línea sigue en «1 ración» con sus 474 kcal; el «+» añade la ración nueva")
    void regla5_raciones() throws Exception {
        Apuntado a = apuntarTablero(null);
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 6, null, tableroJson(tablero.size())));
        a.sigueIgual();
        assertThat(mas(a.plato()).get("caloriasTotales").asInt()).isEqualTo(316);
    }

    @Test
    @DisplayName("regla 5 · cambiar los ingredientes: la línea no cambia; el «+» añade el plato nuevo")
    void regla5_ingredientes() throws Exception {
        Apuntado a = apuntarTablero(null);
        // Sin el aceite: 1.716 kcal entre 4.
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 4, null, tableroJson(5)));
        a.sigueIgual();
        assertThat(mas(a.plato()).get("caloriasTotales").asInt()).isEqualTo(429);
    }

    @Test
    @DisplayName("regla 5 · ponerle peso después: la línea sigue en «1 ración» sin gramos; el «+» añade 1 ración de 450 g")
    void regla5_poner_peso() throws Exception {
        Apuntado a = apuntarTablero(null);
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 4, "1800", tableroJson(tablero.size())));
        a.sigueIgual();
        JsonNode nueva = mas(a.plato());
        assertThat(nueva.get("caloriasTotales").asInt()).isEqualTo(473);
        assertThat(nueva.get("cantidadGramos").decimalValue()).isEqualByComparingTo("450");
        assertThat(nueva.get("racionSinPeso").asBoolean()).isFalse();
        // Copiar la comida de antes apunta el plato de hoy, en raciones.
        JsonNode copia = json(pedir(owner, "POST /comidas/copiar", "{\"comidaId\":" + a.comida() + ",\"fecha\":\""
                + hoy.minusDays(2) + "\",\"tipoComida\":\"CENA\"}"));
        JsonNode copiada = copia.get("lineas").get(0).get("linea");
        assertThat(copiada.get("raciones").decimalValue()).isEqualByComparingTo("1");
        assertThat(copiada.get("caloriasTotales").asInt()).isEqualTo(473);
    }

    @Test
    @DisplayName("la última cantidad de un plato apuntado por raciones se repite en raciones, de las de hoy")
    void ultima_en_raciones_de_hoy() throws Exception {
        Apuntado a = apuntarTablero(null);
        JsonNode antes = json(pedir(owner, "GET /alimentos/" + a.plato()));
        int sinPeso = racion(antes);
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 4, "1800", tableroJson(tablero.size())));
        int conPeso = racion(json(pedir(owner, "GET /alimentos/" + a.plato())));
        assertThat(conPeso).isNotEqualTo(sinPeso);
        JsonNode u = loMio(owner).get("platos").get(0).get("ultima");
        assertThat(u.get("racionId").asInt()).isEqualTo(conPeso);
        assertThat(u.get("raciones").decimalValue()).isEqualByComparingTo("1");
        assertThat(u.get("cantidadGramos").decimalValue()).isEqualByComparingTo("450");
        // Quitado el peso, vuelve a la ración sin peso, sin gramos.
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size())));
        u = loMio(owner).get("platos").get(0).get("ultima");
        assertThat(u.get("racionId").asInt()).isEqualTo(sinPeso);
        assertThat(u.get("racionSinPeso").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("una build con la ración de antes de cambiar el plato apunta la de hoy")
    void racion_vieja_es_la_de_hoy() throws Exception {
        JsonNode plato = crear(owner, platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size())));
        int sinPeso = racion(plato.get("alimento"));
        cambiar(owner, plato.get("id").asInt(), platoJson("Lentejas de casa", 4, "1800", tableroJson(tablero.size())));
        JsonNode linea = anadir(owner, plato.get("id").asInt(), "CENA", sinPeso, "1").get("linea");
        assertThat(linea.get("caloriasTotales").asInt()).isEqualTo(473);
        assertThat(linea.get("cantidadGramos").decimalValue()).isEqualByComparingTo("450");
        assertThat(linea.get("racionSinPeso").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("copiar una línea en gramos de cuando tenía peso, ya sin él, apunta las raciones que eran")
    void copiar_gramos_sin_peso() throws Exception {
        JsonNode plato = crear(owner, platoJson("Lentejas de casa", 4, "1800", tableroJson(tablero.size())));
        int id = plato.get("id").asInt();
        JsonNode r = anadirCon(owner, id, hoy.minusDays(3), "CENA", "\"cantidadGramos\":225");
        cambiar(owner, id, platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size())));
        JsonNode copia = json(pedir(owner, "POST /comidas/copiar", "{\"comidaId\":" + r.get("comida").get("id").asInt()
                + ",\"fecha\":\"" + hoy + "\",\"tipoComida\":\"CENA\"}"));
        JsonNode copiada = copia.get("lineas").get(0).get("linea");
        // 225 g de un plato de raciones de 450: media ración, de 474 kcal.
        assertThat(copiada.get("raciones").decimalValue()).isEqualByComparingTo("0.5");
        assertThat(copiada.get("caloriasTotales").asInt()).isEqualTo(237);
        assertThat(copiada.get("racionSinPeso").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("añadir en la misma comida tras ponerle peso suma raciones de las de hoy, no los 100 g de antes")
    void sumar_tras_poner_peso() throws Exception {
        Apuntado a = apuntarTablero(null);
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 4, "1800", tableroJson(tablero.size())));
        JsonNode plato = json(pedir(owner, "GET /alimentos/" + a.plato()));
        JsonNode linea = anadirCon(owner, a.plato(), hoy.minusDays(3), "CENA",
                "\"racionId\":" + racion(plato) + ",\"raciones\":1").get("linea");
        assertThat(linea.get("id").asInt()).isEqualTo(a.linea());
        assertThat(linea.get("raciones").decimalValue()).isEqualByComparingTo("2");
        assertThat(linea.get("cantidadGramos").decimalValue()).isEqualByComparingTo("900");
        assertThat(linea.get("caloriasTotales").asInt()).isEqualTo(945);
    }

    @Test
    @DisplayName("regla 5 · quitarle el peso: la línea sigue en «1 ración (450 g)» con 473 kcal; el «+» añade 1 ración de 474")
    void regla5_quitar_peso() throws Exception {
        Apuntado a = apuntarTablero("1800");
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 4, null, tableroJson(tablero.size())));
        a.sigueIgual();
        JsonNode linea = json(pedir(owner, "GET /alimentos-comida/" + a.linea()));
        assertThat(linea.get("racionGramos").decimalValue()).isEqualByComparingTo("450");
        JsonNode nueva = mas(a.plato());
        assertThat(nueva.get("caloriasTotales").asInt()).isEqualTo(474);
        assertThat(nueva.get("racionSinPeso").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("regla 5 · cambiar el peso: la línea de antes se lee en gramos (GP-177) y conserva sus kcal")
    void regla5_cambiar_peso() throws Exception {
        Apuntado a = apuntarTablero("1800");
        cambiar(owner, a.plato(), platoJson("Lentejas de casa", 4, "2000", tableroJson(tablero.size())));
        JsonNode linea = json(pedir(owner, "GET /alimentos-comida/" + a.linea()));
        assertThat(linea.get("caloriasTotales").asInt()).isEqualTo(a.kcal());
        assertThat(linea.get("proteinasTotales").decimalValue()).isEqualByComparingTo(a.proteinas());
        assertThat(linea.get("racionId").isNull()).isTrue();
        assertThat(linea.get("cantidadGramos").decimalValue()).isEqualByComparingTo("450");
    }

    @Test
    @DisplayName("regla 5 · borrar el plato: la línea sigue igual y ya no sale en «Mis platos» ni en la búsqueda")
    void regla5_borrar() throws Exception {
        Apuntado a = apuntarTablero(null);
        pedir(owner, "DELETE /platos/" + a.plato()).andExpect(status().isNoContent());
        pedir(owner, "DELETE /platos/" + a.plato()).andExpect(status().isNoContent());
        a.sigueIgual();
        assertThat(loMio(owner).get("platos")).isEmpty();
        em.flush();
        em.clear();
        for (JsonNode n : json(pedir(owner, "GET /alimentos/buscar?q=Lentejas")).get("content")) {
            assertThat(n.get("id") == null || n.get("id").isNull() || n.get("id").asInt() != a.plato()).isTrue();
        }
        assertThat(pedir(owner, "PUT /platos/" + a.plato(), platoJson("X", 1, null, tableroJson(1)))
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    // --- Ayudas -------------------------------------------------------------

    /** Una línea apuntada antes de cambiar el plato, con lo que valía. */
    private record Apuntado(PlatosTest t, int plato, int comida, int linea, int kcal, BigDecimal proteinas,
                            BigDecimal raciones, boolean sinPeso) {
        void sigueIgual() throws Exception {
            t.em.flush();
            t.em.clear();
            JsonNode l = t.json(t.pedir(t.owner, "GET /alimentos-comida/" + linea));
            assertThat(l.get("caloriasTotales").asInt()).as("kcal").isEqualTo(kcal);
            assertThat(l.get("proteinasTotales").decimalValue()).as("proteínas").isEqualByComparingTo(proteinas);
            assertThat(l.get("raciones").decimalValue()).as("raciones").isEqualByComparingTo(raciones);
            assertThat(l.get("racionSinPeso").asBoolean()).as("sin gramos").isEqualTo(sinPeso);
            JsonNode c = t.json(t.pedir(t.owner, "GET /comidas/" + comida));
            assertThat(c.get("totalCalorias").asInt()).as("total de la comida").isEqualTo(kcal);
            assertThat(c.get("totalProteinas").decimalValue()).isEqualByComparingTo(proteinas);
        }
    }

    // El plato del tablero apuntado hace tres días en la cena: 1 ración.
    private Apuntado apuntarTablero(String peso) throws Exception {
        JsonNode plato = crear(owner, platoJson("Lentejas de casa", 4, peso, tableroJson(tablero.size())));
        int id = plato.get("id").asInt();
        JsonNode r = anadirCon(owner, id, hoy.minusDays(3), "CENA",
                "\"racionId\":" + racion(plato.get("alimento")) + ",\"raciones\":1");
        JsonNode l = r.get("linea");
        return new Apuntado(this, id, r.get("comida").get("id").asInt(), l.get("id").asInt(),
                l.get("caloriasTotales").asInt(), l.get("proteinasTotales").decimalValue(), l.get("raciones").decimalValue(),
                l.get("racionSinPeso").asBoolean());
    }

    // Lo que añade el «+» de la fila en «Mis platos»: su última cantidad, en otra comida.
    private JsonNode mas(int plato) throws Exception {
        JsonNode fila = null;
        for (JsonNode n : loMio(owner).get("platos")) if (n.get("id").asInt() == plato) fila = n;
        assertThat(fila).isNotNull();
        JsonNode u = fila.get("ultima");
        String cantidad = u.get("racionId").isNull()
                ? "\"cantidadGramos\":" + u.get("cantidadGramos").decimalValue()
                : "\"racionId\":" + u.get("racionId").asInt() + ",\"raciones\":" + u.get("raciones").decimalValue();
        return anadirCon(owner, plato, hoy, "MERIENDA", cantidad).get("linea");
    }

    private Alimento catalogo(String nombre, int kcal, String p, String c, String g) {
        Alimento a = new Alimento();
        a.setNombre(nombre);
        a.setCalorias(kcal);
        a.setProteinas(new BigDecimal(p));
        a.setCarbohidratos(new BigDecimal(c));
        a.setGrasas(new BigDecimal(g));
        a.setActivo(true);
        return alimentoRepository.save(a);
    }

    private Alimento propio(Usuario dueno, String nombre, int kcal, String p) {
        Alimento a = catalogo(nombre, kcal, p, "0", "0");
        a.setUsuario(dueno);
        return alimentoRepository.save(a);
    }

    private Integer racionDe(Alimento a, String nombre, String nombreEn, String gramos) {
        em.createNativeQuery("INSERT INTO alimento_raciones (alimento_id, nombre, nombre_en, gramos, fuente, orden) "
                        + "VALUES (?, ?, ?, ?, 'prueba', 1)")
                .setParameter(1, a.getId()).setParameter(2, nombre).setParameter(3, nombreEn)
                .setParameter(4, new BigDecimal(gramos)).executeUpdate();
        em.flush();
        em.clear();
        return ((Number) em.createNativeQuery("SELECT MAX(id) FROM alimento_raciones WHERE alimento_id = ?")
                .setParameter(1, a.getId()).getSingleResult()).intValue();
    }

    private String tableroJson(int cuantos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cuantos; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"alimentoId\":").append(tablero.get(i).getId())
                    .append(",\"cantidadGramos\":").append(gramosTablero[i]).append('}');
        }
        return sb.toString();
    }

    private static String platoJson(String nombre, int raciones, String peso, String ingredientes) {
        return "{\"nombre\":\"" + nombre + "\",\"raciones\":" + raciones
                + (peso == null ? "" : ",\"pesoGramos\":" + peso) + ",\"ingredientes\":[" + ingredientes + "]}";
    }

    private JsonNode crear(Usuario quien, String cuerpo) throws Exception {
        String r = pedir(quien, "POST /platos", cuerpo).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        em.flush();
        em.clear();
        return objectMapper.readTree(r);
    }

    private void crearMal(Usuario quien, String cuerpo, int estado) throws Exception {
        assertThat(pedir(quien, "POST /platos", cuerpo).andReturn().getResponse().getStatus())
                .as(cuerpo).isEqualTo(estado);
    }

    private JsonNode cambiar(Usuario quien, int id, String cuerpo) throws Exception {
        JsonNode r = json(pedir(quien, "PUT /platos/" + id, cuerpo));
        em.flush();
        em.clear();
        return r;
    }

    private static int racion(JsonNode alimento) {
        return alimento.get("raciones").get(0).get("id").asInt();
    }

    private JsonNode anadir(Usuario quien, int alimento, String tipo, int racion, String raciones) throws Exception {
        return anadirCon(quien, alimento, hoy, tipo, "\"racionId\":" + racion + ",\"raciones\":" + raciones);
    }

    private JsonNode anadirGramos(Usuario quien, int alimento, String tipo, String gramos) throws Exception {
        return anadirCon(quien, alimento, hoy, tipo, "\"cantidadGramos\":" + gramos);
    }

    private void anadirEn(Usuario quien, int alimento, LocalDate dia, String tipo) throws Exception {
        JsonNode a = json(pedir(quien, "GET /alimentos/" + alimento));
        anadirCon(quien, alimento, dia, tipo, "\"racionId\":" + racion(a) + ",\"raciones\":1");
    }

    private JsonNode anadirCon(Usuario quien, int alimento, LocalDate dia, String tipo, String cantidad) throws Exception {
        JsonNode r = json(pedir(quien, "POST /comidas/anadir", "{\"fecha\":\"" + dia + "\",\"tipoComida\":\"" + tipo
                + "\",\"alimentoId\":" + alimento + "," + cantidad + "}"));
        em.flush();
        em.clear();
        return r;
    }

    private JsonNode loMio(Usuario quien) throws Exception {
        em.flush();
        em.clear();
        return json(pedir(quien, "GET /alimentos/mios"));
    }

    private int contarLoMio(Usuario quien) throws Exception {
        em.flush();
        em.clear();
        ContadorSentencias.empezar();
        pedir(quien, "GET /alimentos/mios").andExpect(status().isOk());
        return ContadorSentencias.sentencias().size();
    }

    private static List<Integer> ids(JsonNode lista) {
        List<Integer> ids = new ArrayList<>();
        lista.forEach(n -> ids.add(n.get("id").asInt()));
        return ids;
    }

    JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }
}
