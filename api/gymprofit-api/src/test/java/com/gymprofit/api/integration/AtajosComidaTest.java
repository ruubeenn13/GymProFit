package com.gymprofit.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.pruebas.ContadorSentencias;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// AtajosComidaTest — comidas recientes, copiar una comida y «Lo que sueles» (lote 1.6.4)
// Con el contexto levantado y JWT de verdad (DEC-014). Copiar recibe el id de la comida
// de origen en el cuerpo, y esa comida tiene dueño: la de otra cuenta es un 403 que no
// dice si existe (DEC-027). Las dos listas no reciben ids: se comprueba el aislamiento.
// ============================================================
@Import(ContadorSentencias.class)
class AtajosComidaTest extends AbstractOwnershipTest {

    @Autowired
    private IAlimentoRepository alimentoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private final LocalDate hoy = LocalDate.now();
    private Alimento pan;
    private Alimento pavo;
    private Alimento yogur;
    private Alimento nueces;

    @BeforeEach
    void sembrar() {
        pan = catalogo("Pan integral kzatajo", 250);
        pavo = catalogo("Pechuga de pavo kzatajo", 105);
        yogur = catalogo("Yogur kzatajo", 60);
        nueces = catalogo("Nueces kzatajo", 654);
    }

    // --- A1 · Comidas recientes ---------------------------------------------

    @Test
    @DisplayName("A1: primero la del mismo tipo de antes de ese día; después, las más recientes, la más tardía primero")
    void recientes_orden() throws Exception {
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", linea(pan, "56", 140), linea(pavo, "60", 63));
        int almuerzo = comida(owner, hoy, "ALMUERZO", linea(yogur, "125", 75));
        int comidaHoy = comida(owner, hoy, "COMIDA", linea(nueces, "20", 131));
        comida(owner, hoy, "DESAYUNO", linea(pan, "40", 100));

        assertThat(ids(recientes(owner, hoy, "MERIENDA"))).containsExactly(ayer, comidaHoy, almuerzo);
    }

    @Test
    @DisplayName("A1: la del mismo tipo va primera aunque sea de hace días y haya otras más nuevas")
    void recientes_mismo_tipo_primero() throws Exception {
        int hace5 = comida(owner, hoy.minusDays(5), "MERIENDA", linea(pan, "56", 140));
        comida(owner, hoy.minusDays(1), "CENA", linea(pavo, "60", 63));
        int comidaHoy = comida(owner, hoy, "COMIDA", linea(nueces, "20", 131));
        int desayunoHoy = comida(owner, hoy, "DESAYUNO", linea(yogur, "125", 75));

        // La cena de ayer se queda fuera: hoy va antes que ayer.
        assertThat(ids(recientes(owner, hoy, "MERIENDA"))).containsExactly(hace5, comidaHoy, desayunoHoy);
    }

    @Test
    @DisplayName("A1: ni la de destino, ni las vacías, ni lo desactivado, ni lo de hace 14 días o después de ese día")
    void recientes_lo_que_no_sale() throws Exception {
        comida(owner, hoy, "MERIENDA", linea(pan, "56", 140));                  // la de destino
        comida(owner, hoy.minusDays(1), "CENA");                                 // vacía
        Alimento retirado = catalogo("Retirado kzatajo", 100);
        comida(owner, hoy.minusDays(2), "COMIDA", linea(retirado, "100", 100));  // solo lo desactivado
        retirado.setActivo(false);
        alimentoRepository.saveAndFlush(retirado);
        comida(owner, hoy.minusDays(14), "MERIENDA", linea(pan, "56", 140));     // fuera de la ventana
        comida(owner, hoy.plusDays(1), "DESAYUNO", linea(pan, "56", 140));       // después del día
        int valida = comida(owner, hoy.minusDays(13), "ALMUERZO", linea(yogur, "125", 75));

        assertThat(ids(recientes(owner, hoy, "MERIENDA"))).containsExactly(valida);
    }

    @Test
    @DisplayName("A1: cada una con su tipo, su día, sus líneas (nombre y ración) y las kcal de lo que se copiaría")
    void recientes_contenido() throws Exception {
        Integer rebanada = racion(pan, "1 rebanada", "1 slice", "28.0");
        Alimento retirado = catalogo("Retirado kzatajo", 100);
        comida(owner, hoy.minusDays(1), "MERIENDA", lineaRacion(pan, "56", 140, rebanada, "2"),
                linea(pavo, "60", 63), linea(retirado, "100", 100));
        retirado.setActivo(false);
        alimentoRepository.saveAndFlush(retirado);

        JsonNode r = recientes(owner, hoy, "MERIENDA").get(0);
        assertThat(r.get("tipoComida").asText()).isEqualTo("MERIENDA");
        assertThat(r.get("fecha").asText()).isEqualTo(hoy.minusDays(1).toString());
        assertThat(r.get("kcal").asInt()).isEqualTo(203);
        assertThat(r.get("lineas")).hasSize(2);
        JsonNode primera = r.get("lineas").get(0);
        assertThat(primera.get("nombreAlimento").asText()).isEqualTo("Pan integral kzatajo");
        assertThat(primera.get("racionId").asInt()).isEqualTo(rebanada);
        assertThat(primera.get("raciones").decimalValue()).isEqualByComparingTo("2");
        assertThat(primera.get("racionUnidadPlural").asText()).isEqualTo("rebanadas");
    }

    @Test
    @DisplayName("A1 · DEC-027: solo las propias; lo de otra cuenta no sale nunca")
    void recientes_aislamiento() throws Exception {
        comida(attacker, hoy.minusDays(1), "MERIENDA", linea(pan, "56", 140));
        assertThat(recientes(owner, hoy, "MERIENDA")).isEmpty();
        int mia = comida(owner, hoy.minusDays(1), "MERIENDA", linea(pavo, "60", 63));
        assertThat(ids(recientes(owner, hoy, "MERIENDA"))).containsExactly(mia);
        assertThat(ids(recientes(attacker, hoy, "MERIENDA"))).doesNotContain(mia);
    }

    @Test
    @DisplayName("A1: las sentencias no crecen con las comidas: tres cuestan lo mismo que una")
    void recientes_sin_viaje_por_comida() throws Exception {
        comida(owner, hoy.minusDays(1), "MERIENDA", linea(pan, "56", 140), linea(pavo, "60", 63));
        recientes(owner, hoy, "MERIENDA"); // la primera petición del contexto carga cosas que no son de esto
        int una = contar(owner, hoy, "MERIENDA");

        comida(owner, hoy, "COMIDA", linea(nueces, "20", 131), linea(yogur, "125", 75));
        comida(owner, hoy, "ALMUERZO", linea(yogur, "125", 75), linea(pan, "40", 100), linea(pavo, "30", 32));
        assertThat(recientes(owner, hoy, "MERIENDA")).hasSize(3);
        assertThat(contar(owner, hoy, "MERIENDA")).isEqualTo(una);
    }

    @Test
    @DisplayName("A1: un tipo que no existe es un 400")
    void recientes_tipo_400() throws Exception {
        pedir(owner, "GET /comidas/recientes?fecha=" + hoy + "&tipoComida=BRUNCH").andExpect(status().isBadRequest());
    }

    // --- A2 · Copiar --------------------------------------------------------

    @Test
    @DisplayName("A2: a una comida que no existía: se crea, con cada línea y su ración, y anterior null")
    void copiar_a_una_nueva() throws Exception {
        Integer rebanada = racion(pan, "1 rebanada", "1 slice", "28.0");
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", lineaRacion(pan, "56", 140, rebanada, "2"),
                linea(pavo, "60", 63));

        JsonNode r = json(copiar(owner, ayer, hoy, "MERIENDA"));
        assertThat(r.get("comida").get("tipoComida").asText()).isEqualTo("MERIENDA");
        assertThat(r.get("comida").get("totalCalorias").asInt()).isEqualTo(140 + 63);
        assertThat(r.get("lineas")).hasSize(2);
        JsonNode pan0 = r.get("lineas").get(0);
        assertThat(pan0.get("linea").get("alimentoId").asInt()).isEqualTo(pan.getId());
        assertThat(pan0.get("linea").get("racionId").asInt()).isEqualTo(rebanada);
        assertThat(pan0.get("linea").get("raciones").decimalValue()).isEqualByComparingTo("2");
        assertThat(pan0.get("anterior").isNull()).isTrue();
        assertThat(r.get("lineas").get(1).get("linea").get("cantidadGramos").decimalValue()).isEqualByComparingTo("60");
        assertThat(comidas(owner, hoy, "MERIENDA")).isEqualTo(1);
    }

    @Test
    @DisplayName("A2: si el alimento ya estaba, se suma, y anterior dice lo que tenía")
    void copiar_suma() throws Exception {
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", linea(pan, "56", 140), linea(pavo, "60", 63));
        comida(owner, hoy, "MERIENDA", linea(pan, "40", 100));

        JsonNode r = json(copiar(owner, ayer, hoy, "MERIENDA"));
        JsonNode panCopiado = r.get("lineas").get(0);
        assertThat(panCopiado.get("linea").get("cantidadGramos").decimalValue()).isEqualByComparingTo("96");
        assertThat(panCopiado.get("anterior").get("cantidadGramos").decimalValue()).isEqualByComparingTo("40");
        assertThat(r.get("lineas").get(1).get("anterior").isNull()).isTrue();
        assertThat(lineas(owner, hoy, "MERIENDA")).isEqualTo(2);
    }

    @Test
    @DisplayName("A2: una línea cuya ración ya no pesa lo mismo se copia en gramos (GP-177)")
    void copiar_racion_que_no_cuadra() throws Exception {
        Integer rebanada = racion(pan, "1 rebanada", "1 slice", "28.0");
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", lineaRacion(pan, "56", 140, rebanada, "2"));
        jdbc.update("UPDATE alimento_raciones SET gramos = 35 WHERE id = ?", rebanada);

        JsonNode linea = json(copiar(owner, ayer, hoy, "MERIENDA")).get("lineas").get(0).get("linea");
        assertThat(linea.get("cantidadGramos").decimalValue()).isEqualByComparingTo("56");
        assertThat(linea.get("racionId").isNull()).isTrue();
        // En la base también: si la ración vuelve a pesar 28, la copia sigue siendo de 56 g.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alimentos_comida WHERE id = ? AND racion_id IS NULL "
                + "AND raciones IS NULL", Integer.class, linea.get("id").asInt())).isEqualTo(1);
    }

    @Test
    @DisplayName("A2: un alimento desactivado se salta y el resto se copia")
    void copiar_salta_desactivados() throws Exception {
        Alimento retirado = catalogo("Retirado kzatajo", 100);
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", linea(retirado, "100", 100), linea(pavo, "60", 63));
        retirado.setActivo(false);
        alimentoRepository.saveAndFlush(retirado);

        JsonNode r = json(copiar(owner, ayer, hoy, "MERIENDA"));
        assertThat(r.get("lineas")).hasSize(1);
        assertThat(r.get("lineas").get(0).get("linea").get("alimentoId").asInt()).isEqualTo(pavo.getId());
        assertThat(lineas(owner, hoy, "MERIENDA")).isEqualTo(1);
    }

    @Test
    @DisplayName("A2: solo lo desactivado, 400, y no deja una comida vacía")
    void copiar_nada() throws Exception {
        Alimento retirado = catalogo("Retirado kzatajo", 100);
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", linea(retirado, "100", 100));
        retirado.setActivo(false);
        alimentoRepository.saveAndFlush(retirado);

        copiar(owner, ayer, hoy, "MERIENDA").andExpect(status().isBadRequest());
        assertThat(comidas(owner, hoy, "MERIENDA")).isZero();
    }

    @Test
    @DisplayName("A2: origen y destino iguales, 400, sin tocar nada")
    void copiar_sobre_si_misma() throws Exception {
        int hoyMerienda = comida(owner, hoy, "MERIENDA", linea(pan, "56", 140));
        copiar(owner, hoyMerienda, hoy, "MERIENDA").andExpect(status().isBadRequest());
        assertThat(lineas(owner, hoy, "MERIENDA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cantidad_gramos FROM alimentos_comida WHERE comida_id = ?",
                BigDecimal.class, hoyMerienda)).isEqualByComparingTo("56");
    }

    @Test
    @DisplayName("A2 · DEC-014: la comida de otra cuenta es un 403, igual que una que no existe, y no crea nada")
    void copiar_ajena_403() throws Exception {
        Alimento secreto = propio(owner, "Dieta secreta kzatajo");
        int suya = comida(owner, hoy.minusDays(1), "MERIENDA", linea(secreto, "100", 100));

        String deOtro = copiar(attacker, suya, hoy, "MERIENDA").andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String queNoExiste = copiar(attacker, 2147483000, hoy, "MERIENDA").andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(deOtro).isEqualTo(queNoExiste).doesNotContain("Dieta secreta");
        assertThat(comidas(attacker, hoy, "MERIENDA")).isZero();
    }

    @Test
    @DisplayName("A2: un 400 por datos que faltan o un tipo que no existe")
    void copiar_400() throws Exception {
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", linea(pan, "56", 140));
        pedir(owner, "POST /comidas/copiar", "{\"comidaId\":" + ayer + ",\"tipoComida\":\"MERIENDA\"}")
                .andExpect(status().isBadRequest());
        copiar(owner, ayer, hoy, "BRUNCH").andExpect(status().isBadRequest());
    }

    // --- A3 · Lo que sueles -------------------------------------------------

    @Test
    @DisplayName("A3: dos días distintos en ese tipo cuentan; un solo día, aunque sean dos comidas, no")
    void habituales_dos_dias() throws Exception {
        comida(owner, hoy.minusDays(1), "MERIENDA", linea(yogur, "125", 75));
        comida(owner, hoy.minusDays(1), "MERIENDA", linea(yogur, "125", 75));
        assertThat(habituales(owner, "MERIENDA")).isEmpty();

        comida(owner, hoy.minusDays(3), "MERIENDA", linea(yogur, "125", 75));
        assertThat(idsAlimentos(habituales(owner, "MERIENDA"))).containsExactly(yogur.getId());
    }

    @Test
    @DisplayName("A3: del más repetido al menos; a igualdad, el más reciente; como mucho tres; otro tipo no cuenta")
    void habituales_orden() throws Exception {
        // Las nueces, tres días, pero no las más recientes; yogur, pan y pavo, dos.
        for (int d : new int[]{3, 4, 5}) comida(owner, hoy.minusDays(d), "MERIENDA", linea(nueces, "20", 131));
        for (int d : new int[]{1, 6}) comida(owner, hoy.minusDays(d), "MERIENDA", linea(yogur, "125", 75));
        for (int d : new int[]{2, 7}) comida(owner, hoy.minusDays(d), "MERIENDA", linea(pan, "56", 140));
        for (int d : new int[]{8, 9}) comida(owner, hoy.minusDays(d), "MERIENDA", linea(pavo, "60", 63));
        // En la cena, mucho pavo: no es merienda.
        for (int d : new int[]{1, 2, 3, 4}) comida(owner, hoy.minusDays(d), "CENA", linea(pavo, "60", 63));

        assertThat(idsAlimentos(habituales(owner, "MERIENDA"))).containsExactly(nueces.getId(), yogur.getId(), pan.getId());
    }

    @Test
    @DisplayName("A3: fuera de los 60 días no cuenta")
    void habituales_60_dias() throws Exception {
        comida(owner, hoy.minusDays(1), "MERIENDA", linea(yogur, "125", 75));
        comida(owner, hoy.minusDays(60), "MERIENDA", linea(yogur, "125", 75));
        assertThat(habituales(owner, "MERIENDA")).isEmpty();
        comida(owner, hoy.minusDays(59), "MERIENDA", linea(yogur, "125", 75));
        assertThat(habituales(owner, "MERIENDA")).hasSize(1);
    }

    @Test
    @DisplayName("A3: cada uno con sus raciones, favorito y la última cantidad en ese tipo de comida")
    void habituales_contenido() throws Exception {
        Integer envase = racion(yogur, "1 envase", "1 pot", "125.0");
        comida(owner, hoy.minusDays(3), "MERIENDA", lineaRacion(yogur, "250", 150, envase, "2"));
        comida(owner, hoy.minusDays(2), "MERIENDA", lineaRacion(yogur, "125", 75, envase, "1"));
        // Más reciente, pero en el desayuno: no es la última en la merienda.
        comida(owner, hoy.minusDays(1), "DESAYUNO", linea(yogur, "200", 120));
        pedir(owner, "PUT /favoritos/" + yogur.getId()).andExpect(status().isOk());

        JsonNode y = habituales(owner, "MERIENDA").get(0);
        assertThat(y.get("favorito").asBoolean()).isTrue();
        assertThat(y.get("raciones").get(0).get("id").asInt()).isEqualTo(envase);
        assertThat(y.get("ultima").get("cantidadGramos").decimalValue()).isEqualByComparingTo("125");
        assertThat(y.get("ultima").get("racionId").asInt()).isEqualTo(envase);
        assertThat(y.get("ultima").get("raciones").decimalValue()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("A3 · DEC-027: lo de otra cuenta no sale; los desactivados, tampoco")
    void habituales_aislamiento_y_desactivados() throws Exception {
        Alimento suyo = propio(attacker, "Batido secreto kzatajo");
        for (int d : new int[]{1, 2}) comida(attacker, hoy.minusDays(d), "MERIENDA", linea(suyo, "300", 200), linea(pan, "56", 140));
        assertThat(habituales(owner, "MERIENDA")).isEmpty();

        for (int d : new int[]{1, 2}) comida(owner, hoy.minusDays(d), "MERIENDA", linea(yogur, "125", 75));
        yogur.setActivo(false);
        alimentoRepository.saveAndFlush(yogur);
        assertThat(habituales(owner, "MERIENDA")).isEmpty();
        assertThat(idsAlimentos(habituales(attacker, "MERIENDA"))).containsExactlyInAnyOrder(suyo.getId(), pan.getId());
    }

    // --- El invitado ----------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /comidas/recientes?fecha={hoy}&tipoComida=MERIENDA", "GET /comidas/habituales?tipoComida=MERIENDA"})
    @DisplayName("el invitado no añade: 403")
    void invitado_403(String ruta) throws Exception {
        pedir(guest, ruta.replace("{hoy}", hoy.toString())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el invitado no copia: 403, y no crea nada")
    void invitado_no_copia() throws Exception {
        int ayer = comida(owner, hoy.minusDays(1), "MERIENDA", linea(pan, "56", 140));
        copiar(guest, ayer, hoy, "MERIENDA").andExpect(status().isForbidden());
        assertThat(comidas(guest, hoy, "MERIENDA")).isZero();
        assertThat(comidas(owner, hoy, "MERIENDA")).isZero();
    }

    // --- Andamiaje ----------------------------------------------------------

    private record Linea(Alimento alimento, String gramos, int kcal, Integer racionId, String raciones) {
    }

    private static Linea linea(Alimento a, String gramos, int kcal) {
        return new Linea(a, gramos, kcal, null, null);
    }

    private static Linea lineaRacion(Alimento a, String gramos, int kcal, Integer racionId, String raciones) {
        return new Linea(a, gramos, kcal, racionId, raciones);
    }

    // Una comida nueva ese día y de ese tipo, con sus líneas y sus totales.
    private int comida(Usuario quien, LocalDate dia, String tipo, Linea... lineas) {
        int total = 0;
        for (Linea l : lineas) total += l.kcal();
        jdbc.update("INSERT INTO comidas (usuario_id, fecha, tipo_comida, total_calorias) VALUES (?, ?, ?, ?)",
                quien.getId(), Timestamp.valueOf(dia.atStartOfDay()), tipo, total);
        Integer comida = jdbc.queryForObject("SELECT MAX(id) FROM comidas WHERE usuario_id = ?", Integer.class, quien.getId());
        for (Linea l : lineas) {
            jdbc.update("INSERT INTO alimentos_comida (comida_id, alimento_id, cantidad_gramos, calorias_totales, racion_id, raciones) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", comida, l.alimento().getId(), new BigDecimal(l.gramos()), l.kcal(),
                    l.racionId(), l.raciones() == null ? null : new BigDecimal(l.raciones()));
        }
        return comida;
    }

    private Alimento catalogo(String nombre, int kcal) {
        Alimento a = crearAlimentoCatalogo();
        a.setNombre(nombre);
        a.setCalorias(kcal);
        return alimentoRepository.saveAndFlush(a);
    }

    private Alimento propio(Usuario dueno, String nombre) {
        Alimento a = crearAlimentoCatalogo();
        a.setNombre(nombre);
        a.setUsuario(dueno);
        return alimentoRepository.saveAndFlush(a);
    }

    private Integer racion(Alimento a, String nombre, String nombreEn, String gramos) {
        jdbc.update("INSERT INTO alimento_raciones (alimento_id, nombre, nombre_en, gramos, fuente, orden) "
                + "VALUES (?, ?, ?, ?, 'prueba', 1)", a.getId(), nombre, nombreEn, new BigDecimal(gramos));
        em.flush();
        em.clear();
        return jdbc.queryForObject("SELECT MAX(id) FROM alimento_raciones WHERE alimento_id = ?", Integer.class, a.getId());
    }

    private ResultActions copiar(Usuario quien, int origen, LocalDate dia, String tipo) throws Exception {
        em.flush();
        em.clear();
        return pedir(quien, "POST /comidas/copiar",
                "{\"comidaId\":" + origen + ",\"fecha\":\"" + dia + "\",\"tipoComida\":\"" + tipo + "\"}");
    }

    private JsonNode recientes(Usuario quien, LocalDate dia, String tipo) throws Exception {
        em.flush();
        em.clear();
        return json(pedir(quien, "GET /comidas/recientes?fecha=" + dia + "&tipoComida=" + tipo));
    }

    private JsonNode habituales(Usuario quien, String tipo) throws Exception {
        em.flush();
        em.clear();
        return json(pedir(quien, "GET /comidas/habituales?tipoComida=" + tipo));
    }

    private int contar(Usuario quien, LocalDate dia, String tipo) throws Exception {
        em.flush();
        em.clear();
        ContadorSentencias.empezar();
        pedir(quien, "GET /comidas/recientes?fecha=" + dia + "&tipoComida=" + tipo).andExpect(status().isOk());
        return ContadorSentencias.sentencias().size();
    }

    private int comidas(Usuario quien, LocalDate dia, String tipo) {
        em.flush();
        return jdbc.queryForObject("SELECT COUNT(*) FROM comidas WHERE usuario_id = ? AND tipo_comida = ? "
                + "AND DATE(fecha) = ?", Integer.class, quien.getId(), tipo, dia.toString());
    }

    private int lineas(Usuario quien, LocalDate dia, String tipo) {
        em.flush();
        return jdbc.queryForObject("SELECT COUNT(*) FROM alimentos_comida ac JOIN comidas c ON c.id = ac.comida_id "
                + "WHERE c.usuario_id = ? AND c.tipo_comida = ? AND DATE(c.fecha) = ?", Integer.class,
                quien.getId(), tipo, dia.toString());
    }

    private static List<Integer> ids(JsonNode lista) {
        List<Integer> ids = new ArrayList<>();
        lista.forEach(n -> ids.add(n.get("id").asInt()));
        return ids;
    }

    private static List<Integer> idsAlimentos(JsonNode lista) {
        return ids(lista);
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }
}
