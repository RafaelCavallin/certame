package br.com.certame.flashcard.revisao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Contrato HTTP de {@link RevisaoController} — TI-05…TI-18, TI-28…TI-30: abertura de sessão, registro
 * de revisão, finalização e resumo, sempre restritos ao dono do token (RN-07).
 */
@SpringBootTest(properties = "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois")
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers
class RevisaoControllerIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private TempoEstudoFlashcards tempoEstudo;

    @Test
    void TI05_abrirSessaoMontaFilaRespeitandoOsLimitesDoDeck() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Constitucional", (short) 2, (short) 60);
        for (var i = 0; i < 5; i++) {
            cartaoDevendo(usuario, deck, "NOVO", 0);
        }

        mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escopo\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void TI06_abrirSessaoDevolveAsQuatroPrevisoesPorCard() throws Exception {
        var usuario = usuario();
        cartaoDevendo(usuario, null, "NOVO", 0);

        mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escopo\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fila[0].previsoes.length()").value(4))
                .andExpect(jsonPath("$.fila[0].previsoes[0].nota").value("ERREI"))
                .andExpect(jsonPath("$.fila[0].previsoes[3].nota").value("FACIL"));
    }

    @Test
    void TI07_abrirSessaoDevolveOBlocoDeMemoriaDoCard() throws Exception {
        var usuario = usuario();
        cartaoDevendo(usuario, null, "NOVO", 0);

        mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escopo\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fila[0].memoria.rotulo").value("Primeiro contato com este card"));
    }

    @Test
    void TI08_abrirSessaoComFilaDoDiaTerminada() throws Exception {
        var usuario = usuario();

        mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escopo\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.fila").isEmpty());
    }

    @Test
    void TI09_abrirSessaoDeUmUsuarioNaoAlcancaCardsDeOutro() throws Exception {
        var usuario = usuario();
        var outro = usuario();
        cartaoDevendo(outro, null, "NOVO", 0);

        mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escopo\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void TI10_abrirSessaoComEscopoDeckDeDeckDeTerceiroResponde404() throws Exception {
        var usuario = usuario();
        var outro = usuario();
        var deckDeOutro = deck(outro, "Deck de outro", (short) 10, (short) 60);

        mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escopo\":\"DECK\",\"deckId\":\"" + deckDeOutro + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void TI11_registrarComBomRecalculaMemoriaEAgenda() throws Exception {
        var usuario = usuario();
        var card = cartaoDevendo(usuario, null, "NOVO", 0);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");

        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/revisoes").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashcardId\":\"" + card + "\",\"nota\":\"BOM\",\"tempoMs\":4000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APRENDENDO"))
                .andExpect(jsonPath("$.estabilidade").value(org.hamcrest.Matchers.greaterThan(0.0)));

        var estabilidade = jdbc.queryForObject("select estabilidade from flashcard where id = ?", Float.class, card);
        assertThat(estabilidade).isGreaterThan(0f);
    }

    @Test
    void TI12_registrarComErreiAplicaLapsoEReaprendizado() throws Exception {
        var usuario = usuario();
        var card = cartaoEmRevisao(usuario, null, 10f, 6f);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");

        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/revisoes").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashcardId\":\"" + card + "\",\"nota\":\"ERREI\",\"tempoMs\":3000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REAPRENDENDO"))
                .andExpect(jsonPath("$.reenfileirar").value(true));

        var lapsos = jdbc.queryForObject("select count(*) from flashcard_revisao where flashcard_id = ? and nota = 'ERREI'",
                Integer.class, card);
        assertThat(lapsos).isEqualTo(1);
    }

    @Test
    void TI13_registrarTruncaNoIntervaloMaximoDoDeck() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Intervalo curto", (short) 10, (short) 60);
        jdbc.update("update deck set intervalo_max_dias = 3 where id = ?", deck);
        var card = cartaoEmRevisao(usuario, deck, 500f, 3f);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");

        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/revisoes").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashcardId\":\"" + card + "\",\"nota\":\"FACIL\",\"tempoMs\":2000}"))
                .andExpect(status().isOk());

        var intervalo = jdbc.queryForObject("select intervalo_novo from flashcard_revisao where flashcard_id = ?",
                Integer.class, card);
        assertThat(intervalo).isEqualTo(3);
    }

    @Test
    void TI14_registrarGravaALinhaCompletaDoLog() throws Exception {
        var usuario = usuario();
        var card = cartaoDevendo(usuario, null, "NOVO", 0);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");

        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/revisoes").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashcardId\":\"" + card + "\",\"nota\":\"BOM\",\"tempoMs\":4200}"))
                .andExpect(status().isOk());

        var linha = jdbc.queryForMap("select * from flashcard_revisao where flashcard_id = ?", card);
        assertThat(linha.get("sessao_id")).isEqualTo(sessao);
        assertThat(linha.get("usuario_id")).isEqualTo(usuario);
        assertThat(linha.get("nota")).isEqualTo("BOM");
        assertThat(linha.get("tempo_ms")).isEqualTo(4200);
        assertThat(linha.get("segundos_decorridos")).isEqualTo(4);
    }

    @Test
    void TI15_registrarEmCardDeOutroUsuarioResponde404() throws Exception {
        var usuario = usuario();
        var outro = usuario();
        var cardDeOutro = cartaoDevendo(outro, null, "NOVO", 0);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");

        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/revisoes").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashcardId\":\"" + cardDeOutro + "\",\"nota\":\"BOM\",\"tempoMs\":1000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void TI16_registrarEmSessaoJaFinalizadaResponde409() throws Exception {
        var usuario = usuario();
        var card = cartaoDevendo(usuario, null, "NOVO", 0);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");
        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/finalizar").with(estudante(usuario)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/revisoes").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashcardId\":\"" + card + "\",\"nota\":\"BOM\",\"tempoMs\":1000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSAO_FINALIZADA"));
    }

    @Test
    void TI17_finalizarDevolveOResumoCompleto() throws Exception {
        var usuario = usuario();
        var cardA = cartaoDevendo(usuario, null, "NOVO", 0);
        var cardB = cartaoDevendo(usuario, null, "NOVO", 0);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");
        revisar(usuario, sessao, cardA, "BOM", 1000);
        revisar(usuario, sessao, cardB, "ERREI", 2000);

        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/finalizar").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisados").value(2))
                .andExpect(jsonPath("$.classificacoes").value(2))
                .andExpect(jsonPath("$.distribuicao.bom").value(1))
                .andExpect(jsonPath("$.distribuicao.errei").value(1))
                .andExpect(jsonPath("$.tempoMs").value(3000))
                .andExpect(jsonPath("$.proximosVencimentos.length()").value(3));
    }

    @Test
    void TI18_segundaSessaoNoMesmoDiaRespeitaOConsumoAnterior() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Limite justo", (short) 1, (short) 60);
        var cardA = cartaoDevendo(usuario, deck, "NOVO", 0);
        var cardB = cartaoDevendo(usuario, deck, "NOVO", 0);
        var primeiraSessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");
        revisar(usuario, primeiraSessao, cardA, "BOM", 1000);

        mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escopo\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fila[?(@.id=='" + cardB + "')]").doesNotExist());
    }

    @Test
    void TI28_resumoDevolveVencendoHojeEProximos7Dias() throws Exception {
        var usuario = usuario();
        cartaoDevendo(usuario, null, "REVISAO", 0);
        cartaoDevendo(usuario, null, "REVISAO", -3);
        cartaoDevendo(usuario, null, "NOVO", 0);

        mvc.perform(get("/api/v1/flashcards/resumo").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencendoHoje").value(2))
                .andExpect(jsonPath("$.proximos7Dias").value(1))
                .andExpect(jsonPath("$.novosDisponiveis").value(1));
    }

    @Test
    void TI29_tempoEstudoFlashcardsSomaAsSessoesFinalizadasDoDia() throws Exception {
        var usuario = usuario();
        var cardA = cartaoDevendo(usuario, null, "NOVO", 0);
        var sessao = abrirSessao(usuario, "{\"escopo\":\"TODOS\"}");
        revisar(usuario, sessao, cardA, "BOM", 4000);
        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessao + "/finalizar").with(estudante(usuario)))
                .andExpect(status().isOk());

        var segundos = tempoEstudo.segundosNoDia(usuario, LocalDate.now());

        assertThat(segundos).isEqualTo(4);
    }

    @Test
    void TI30_rotasSemTokenRespondem401() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(post("/api/v1/flashcards/sessoes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/flashcards/sessoes/" + id + "/revisoes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/flashcards/sessoes/" + id + "/finalizar")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/flashcards/resumo")).andExpect(status().isUnauthorized());
    }

    private UUID abrirSessao(UUID usuarioId, String corpo) throws Exception {
        var resultado = mvc.perform(post("/api/v1/flashcards/sessoes").with(estudante(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn();
        return extrairId(resultado.getResponse().getContentAsString());
    }

    private void revisar(UUID usuarioId, UUID sessaoId, UUID cardId, String nota, int tempoMs) throws Exception {
        mvc.perform(post("/api/v1/flashcards/sessoes/" + sessaoId + "/revisoes").with(estudante(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flashcardId\":\"" + cardId + "\",\"nota\":\"" + nota + "\",\"tempoMs\":" + tempoMs + "}"))
                .andExpect(status().isOk());
    }

    private UUID extrairId(String corpoJson) {
        var inicio = corpoJson.indexOf("\"id\":\"") + 6;
        return UUID.fromString(corpoJson.substring(inicio, corpoJson.indexOf('"', inicio)));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor estudante(UUID usuarioId) {
        return user(usuarioId.toString()).roles("ESTUDANTE");
    }

    private UUID usuario() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Estudante', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                id, id + "@revisao-controller.local");
        return id;
    }

    private UUID deck(UUID usuarioId, String nome, short novosDia, short revisoesDia) {
        var id = UUID.randomUUID();
        jdbc.update("insert into deck (id, usuario_id, nome, novos_dia, revisoes_dia) values (?, ?, ?, ?, ?)",
                id, usuarioId, nome, novosDia, revisoesDia);
        return id;
    }

    private UUID cartaoDevendo(UUID usuarioId, UUID deckId, String estado, int diasAtras) {
        var id = UUID.randomUUID();
        var data = LocalDate.now().minusDays(diasAtras);
        jdbc.update("insert into flashcard (id, usuario_id, deck_id, frente, verso, estado, proxima_revisao, proxima_revisao_em)"
                        + " values (?, ?, ?, ?, ?, ?, ?, ?::timestamp at time zone 'America/Sao_Paulo')",
                id, usuarioId, deckId, "Frente " + id, "Verso " + id, estado, data, data);
        return id;
    }

    private UUID cartaoEmRevisao(UUID usuarioId, UUID deckId, float estabilidade, float dificuldade) {
        var id = UUID.randomUUID();
        var data = LocalDate.now();
        jdbc.update("insert into flashcard (id, usuario_id, deck_id, frente, verso, estado, estabilidade, dificuldade,"
                        + " ultima_revisao, proxima_revisao, proxima_revisao_em)"
                        + " values (?, ?, ?, ?, ?, 'REVISAO', ?, ?, ?, ?, ?::timestamp at time zone 'America/Sao_Paulo')",
                id, usuarioId, deckId, "Frente " + id, "Verso " + id, estabilidade, dificuldade,
                java.sql.Timestamp.from(Instant.now().minusSeconds(864_000)), data, data);
        return id;
    }
}
