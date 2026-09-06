package br.com.certame.flashcard.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
 * Contrato HTTP de {@link DeckController} — TI-22…TI-24: mover em lote respeitando a posse dos
 * cards e o índice único de nome de deck por usuário (CA-16).
 */
@SpringBootTest(properties = "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois")
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers
class DeckControllerIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;

    @Test
    void listaEcriaDeckDoUsuario() throws Exception {
        var usuario = usuario();

        mvc.perform(post("/api/v1/flashcards/decks").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Constitucional\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Constitucional"))
                .andExpect(jsonPath("$.totalCards").value(0));

        mvc.perform(get("/api/v1/flashcards/decks").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Constitucional"));
    }

    @Test
    void TI22_moverDozeCardsParaUmDeckDevolveAQuantidadeMovida() throws Exception {
        var usuario = usuario();
        var origem = deck(usuario, "Origem");
        var destino = deck(usuario, "Destino");
        var ids = IntStream.range(0, 12).mapToObj(i -> cartao(usuario, origem)).toList();

        mvc.perform(post("/api/v1/flashcards/decks/" + destino + "/mover").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON).content(listaJson(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movidos").value(12));

        var noDestino = jdbc.queryForObject("select count(*) from flashcard where deck_id = ?", Integer.class, destino);
        assertThat(noDestino).isEqualTo(12);
    }

    @Test
    void TI23_moverIgnoraIdDeCardDeTerceiroNaLista() throws Exception {
        var usuario = usuario();
        var outro = usuario();
        var destino = deck(usuario, "Destino");
        var proprio = cartao(usuario, null);
        var deTerceiro = cartao(outro, null);

        mvc.perform(post("/api/v1/flashcards/decks/" + destino + "/mover").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON).content(listaJson(List.of(proprio, deTerceiro))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movidos").value(1));

        assertThat(jdbc.queryForObject("select deck_id from flashcard where id = ?", UUID.class, proprio)).isEqualTo(destino);
        assertThat(jdbc.queryForObject("select deck_id from flashcard where id = ?", UUID.class, deTerceiro)).isNull();
    }

    @Test
    void TI24_criarDeckComNomeRepetidoRespondeConflito() throws Exception {
        var usuario = usuario();
        deck(usuario, "Direito Constitucional");

        mvc.perform(post("/api/v1/flashcards/decks").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"direito constitucional\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOME_EM_USO"))
                .andExpect(jsonPath("$.fieldErrors.nome").exists());
    }

    @Test
    void TI28_criarAplicaDescricaoELimitesInformadosNaMesmaChamada() throws Exception {
        var usuario = usuario();

        var resposta = mvc.perform(post("/api/v1/flashcards/decks").with(estudante(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Deck com limites\",\"descricao\":\"Descrição do deck\","
                                + "\"novosDia\":20,\"revisoesDia\":100,\"intervaloMaxDias\":30,\"retencaoAlvo\":0.85}"))
                .andExpect(status().isCreated())
                .andReturn();
        var location = resposta.getResponse().getHeader("Location");
        var deckId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        mvc.perform(get("/api/v1/flashcards/decks").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Descrição do deck"))
                .andExpect(jsonPath("$[0].limites.novosDia").value(20))
                .andExpect(jsonPath("$[0].limites.revisoesDia").value(100))
                .andExpect(jsonPath("$[0].limites.intervaloMaxDias").value(30));

        assertThat(jdbc.queryForObject("select revisoes_dia from deck where id = ?", Short.class, deckId)).isEqualTo((short) 100);
    }

    @Test
    void TI19_getDecksDevolveOPainelCompleto() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Direito Administrativo");
        cartao(usuario, deck);

        mvc.perform(get("/api/v1/flashcards/decks").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Direito Administrativo"))
                .andExpect(jsonPath("$[0].totalCards").value(1))
                .andExpect(jsonPath("$[0].limites.novosDia").exists())
                .andExpect(jsonPath("$[0].limitesRotulo").exists())
                .andExpect(jsonPath("$[0].retencaoRotulo").exists())
                .andExpect(jsonPath("$[0].editavel").value(true));
    }

    @Test
    void TI20_getDecksIncluiALinhaVirtualSemDeck() throws Exception {
        var usuario = usuario();
        cartao(usuario, null);

        mvc.perform(get("/api/v1/flashcards/decks").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].nome").value("Sem deck"))
                .andExpect(jsonPath("$[0].editavel").value(false));
    }

    @Test
    void TI21_getDecksEmiteOAvisoDeFilaAcumulada() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Fila acumulada");
        jdbc.update("update deck set revisoes_dia = 5 where id = ?", deck);
        for (var i = 0; i < 15; i++) {
            cartao(usuario, deck);
        }

        mvc.perform(get("/api/v1/flashcards/decks").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aviso.tipo").value("FILA_ACUMULADA"))
                .andExpect(jsonPath("$[0].aviso.vencidos").value(15));
    }

    @Test
    void TI22_putRecusaNomeCurtoENomeDuplicado() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Constitucional");
        deck(usuario, "Penal");

        mvc.perform(put("/api/v1/flashcards/decks/" + deck).with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ab\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DADOS_INVALIDOS"));

        mvc.perform(put("/api/v1/flashcards/decks/" + deck).with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"penal\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOME_EM_USO"))
                .andExpect(jsonPath("$.fieldErrors.nome").exists());
    }

    @Test
    void TI23_putRenomeiaMantendoCardsEAgendamento() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Constitucional");
        var card = cartao(usuario, deck);

        mvc.perform(put("/api/v1/flashcards/decks/" + deck).with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Constitucional avançado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Constitucional avançado"))
                .andExpect(jsonPath("$.totalCards").value(1));

        assertThat(jdbc.queryForObject("select deck_id from flashcard where id = ?", UUID.class, card)).isEqualTo(deck);
    }

    @Test
    void TI24_putAjustaValoresForaDoIntervalo() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Constitucional — controle");

        mvc.perform(put("/api/v1/flashcards/decks/" + deck).with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Constitucional — controle\",\"revisoesDia\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limites.revisoesDia").value(400));
    }

    @Test
    void TI25_putMudaARetencaoSemTocarOHistorico() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Constitucional");
        var card = cartao(usuario, deck);
        var estabilidadeAntes = jdbc.queryForObject("select estabilidade from flashcard where id = ?", Float.class, card);

        mvc.perform(put("/api/v1/flashcards/decks/" + deck).with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Constitucional\",\"retencaoAlvo\":0.95}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limites.retencaoAlvo").value(0.95));

        var estabilidadeDepois = jdbc.queryForObject("select estabilidade from flashcard where id = ?", Float.class, card);
        assertThat(estabilidadeDepois).isEqualTo(estabilidadeAntes);
    }

    @Test
    void TI26_deleteMoveOsCardsEPreservaTodos() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "A excluir");
        IntStream.range(0, 12).forEach(i -> cartao(usuario, deck));

        mvc.perform(delete("/api/v1/flashcards/decks/" + deck).with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsMovidos").value(12));

        var total = jdbc.queryForObject("select count(*) from flashcard where usuario_id = ?", Integer.class, usuario);
        assertThat(total).isEqualTo(12);
        var semDeck = jdbc.queryForObject("select count(*) from flashcard where usuario_id = ? and deck_id is null",
                Integer.class, usuario);
        assertThat(semDeck).isEqualTo(12);
        var deckRestante = jdbc.queryForObject("select count(*) from deck where id = ?", Integer.class, deck);
        assertThat(deckRestante).isZero();
    }

    @Test
    void TI27_deleteEAtomico() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Atômico");
        IntStream.range(0, 50).forEach(i -> cartao(usuario, deck));

        mvc.perform(delete("/api/v1/flashcards/decks/" + deck).with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsMovidos").value(50));

        var comDeckAntigo = jdbc.queryForObject("select count(*) from flashcard where deck_id = ?", Integer.class, deck);
        assertThat(comDeckAntigo).isZero();
        var total = jdbc.queryForObject("select count(*) from flashcard where usuario_id = ?", Integer.class, usuario);
        assertThat(total).isEqualTo(50);
    }

    @Test
    void TI30_rotasSemTokenRespondem401() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(put("/api/v1/flashcards/decks/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/flashcards/decks/" + id)).andExpect(status().isUnauthorized());
    }

    private String listaJson(List<UUID> ids) {
        return "{\"flashcardIds\":[" + ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",")) + "]}";
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor estudante(UUID usuarioId) {
        return user(usuarioId.toString()).roles("ESTUDANTE");
    }

    private UUID usuario() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Estudante', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                id, id + "@deck-controller.local");
        return id;
    }

    private UUID deck(UUID usuarioId, String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into deck (id, usuario_id, nome) values (?, ?, ?)", id, usuarioId, nome);
        return id;
    }

    private UUID cartao(UUID usuarioId, UUID deckId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, deck_id, frente, verso) values (?, ?, ?, ?, ?)",
                id, usuarioId, deckId, "Frente " + id, "Verso " + id);
        return id;
    }
}
