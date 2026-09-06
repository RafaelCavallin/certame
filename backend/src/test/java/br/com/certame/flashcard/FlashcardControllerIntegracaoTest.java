package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.SessionFactory;
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
 * Contrato HTTP de {@link FlashcardController} — TI-06…TI-15, TI-21, TI-25, TI-27 (criação,
 * listagem com busca/filtros/ordenação, origem com código da questão, ausência de token e ausência
 * de N+1 sobre deck, disciplina e questão de origem).
 */
@SpringBootTest(properties = {
        "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers
class FlashcardControllerIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void TI06_criaCardDentroDeUmaSessao() throws Exception {
        var usuario = usuario();
        var disciplina = disciplina("Direito Constitucional");
        var questao = questaoPublicada(disciplina, usuario);
        var sessao = sessao(usuario, "FOCO");

        var resultado = mvc.perform(post("/api/v1/flashcards").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"frente":"Efeitos da decisão de mérito em ADI","verso":"Eficácia erga omnes.",
                                 "questaoOrigemId":"%s","sessaoOrigemId":"%s"}
                                """.formatted(questao, sessao)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flashcard.origem.questaoId").value(questao.toString()))
                .andExpect(jsonPath("$.criadosNaSessao").value(1))
                .andReturn();

        assertThat(resultado.getResponse().getHeader("Location")).startsWith("/api/v1/flashcards/");
    }

    @Test
    void TI07_recusaFrenteComDoisCaracteres() throws Exception {
        var usuario = usuario();

        mvc.perform(post("/api/v1/flashcards").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frente\":\"ab\",\"verso\":\"Verso válido\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.fieldErrors.frente").exists());
    }

    @Test
    void TI08_criaCardAvulsoComDisciplinaEAssunto() throws Exception {
        var usuario = usuario();
        var disciplina = disciplina("Direito Administrativo " + UUID.randomUUID());
        var assunto = assunto(disciplina, "Licitações");

        mvc.perform(post("/api/v1/flashcards").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"frente":"Modalidade de licitação","verso":"Pregão eletrônico.",
                                 "disciplinaId":"%s","assuntoId":"%s"}
                                """.formatted(disciplina, assunto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flashcard.origem").doesNotExist())
                .andExpect(jsonPath("$.flashcard.disciplina.id").value(disciplina.toString()))
                .andExpect(jsonPath("$.criadosNaSessao").doesNotExist());
    }

    @Test
    void TI09_recusaSessaoDeOutroUsuarioComMensagemIdenticaANaoEncontrada() throws Exception {
        var usuarioA = usuario();
        var usuarioB = usuario();
        var disciplina = disciplina("Direito Penal");
        var questao = questaoPublicada(disciplina, usuarioB);
        var sessaoDeB = sessao(usuarioB, "FOCO");

        mvc.perform(post("/api/v1/flashcards").with(estudante(usuarioA)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"frente":"Crime continuado","verso":"Art. 71 do Código Penal.",
                                 "questaoOrigemId":"%s","sessaoOrigemId":"%s"}
                                """.formatted(questao, sessaoDeB)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.sessaoOrigemId").value("Sessão de estudo não encontrada."));
    }

    @Test
    void TI10_criaCardComSessaoEmModoProvaAberta() throws Exception {
        var usuario = usuario();
        var disciplina = disciplina("Direito Tributário");
        var questao = questaoPublicada(disciplina, usuario);
        var sessao = sessao(usuario, "PROVA");

        mvc.perform(post("/api/v1/flashcards").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"frente":"Imunidade tributária recíproca","verso":"Art. 150, VI, a, da CF.",
                                 "questaoOrigemId":"%s","sessaoOrigemId":"%s"}
                                """.formatted(questao, sessao)))
                .andExpect(status().isCreated());
    }

    @Test
    void TI11_listaAcervoComIndicadores() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Constitucional");
        cartaoAtivo(usuario, new NovoCartao(deck, EstadoFlashcard.NOVO, LocalDate.now()));
        cartaoAtivo(usuario, new NovoCartao(null, EstadoFlashcard.REVISAO, LocalDate.now().minusDays(1)));
        cartaoAtivo(usuario, new NovoCartao(null, EstadoFlashcard.APRENDENDO, LocalDate.now().plusDays(10)));

        mvc.perform(get("/api/v1/flashcards").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.indicadores.total").value(3))
                .andExpect(jsonPath("$.indicadores.decks").value(1))
                .andExpect(jsonPath("$.indicadores.vencendoHoje").value(2))
                .andExpect(jsonPath("$.indicadores.emFixacao").value(2));
    }

    @Test
    void TI12_buscaTextualEmPortugues() throws Exception {
        var usuario = usuario();
        cartaoComTexto(usuario, "Direito Constitucional", "Controle concentrado e ADI.");
        cartaoComTexto(usuario, "Processo Civil", "Recurso extraordinário.");

        mvc.perform(get("/api/v1/flashcards").param("q", "constitucional").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].frente").value("Direito Constitucional"));
    }

    @Test
    void TI13_filtrosCombinadosEOrdenacao() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Deck alvo");
        cartaoAtivo(usuario, new NovoCartao(deck, EstadoFlashcard.NOVO, LocalDate.now().plusDays(5)));
        cartaoAtivo(usuario, new NovoCartao(deck, EstadoFlashcard.NOVO, LocalDate.now().plusDays(1)));
        cartaoAtivo(usuario, new NovoCartao(deck, EstadoFlashcard.REVISAO, LocalDate.now()));
        cartaoAtivo(usuario, new NovoCartao(null, EstadoFlashcard.NOVO, LocalDate.now()));

        mvc.perform(get("/api/v1/flashcards").param("deck", deck.toString()).param("estado", "NOVO")
                        .param("sort", "proximaRevisao,asc").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].proximaRevisao").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.content[1].proximaRevisao").value(LocalDate.now().plusDays(5).toString()));
    }

    @Test
    void TI14_listaArquivadosSeparadamenteDosAtivos() throws Exception {
        var usuario = usuario();
        cartaoAtivo(usuario, new NovoCartao(null, EstadoFlashcard.NOVO, LocalDate.now()));
        var arquivado = cartaoArquivado(usuario);

        mvc.perform(get("/api/v1/flashcards").param("arquivados", "true").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(arquivado.toString()))
                .andExpect(jsonPath("$.content[0].status").value("ARQUIVADO"));
    }

    @Test
    void TI15_recusaSortForaDaListaBranca() throws Exception {
        var usuario = usuario();

        mvc.perform(get("/api/v1/flashcards").param("sort", "frente,asc").with(estudante(usuario)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.sort").exists());
    }

    @Test
    void recusaSizeAcimaDeCem() throws Exception {
        var usuario = usuario();

        mvc.perform(get("/api/v1/flashcards").param("size", "101").with(estudante(usuario)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.size").exists());
    }

    @Test
    void recusaDeckComFormatoInvalido() throws Exception {
        var usuario = usuario();

        mvc.perform(get("/api/v1/flashcards").param("deck", "não-é-um-uuid").with(estudante(usuario)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.deck").exists());
    }

    @Test
    void recusaEstadoDesconhecido() throws Exception {
        var usuario = usuario();

        mvc.perform(get("/api/v1/flashcards").param("estado", "INEXISTENTE").with(estudante(usuario)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.estado").exists());
    }

    @Test
    void filtraPorDeckLiteralSemDeck() throws Exception {
        var usuario = usuario();
        var deck = deck(usuario, "Com deck");
        cartaoAtivo(usuario, new NovoCartao(deck, EstadoFlashcard.NOVO, LocalDate.now()));
        cartaoAtivo(usuario, new NovoCartao(null, EstadoFlashcard.NOVO, LocalDate.now()));

        mvc.perform(get("/api/v1/flashcards").param("deck", FiltroFlashcards.SEM_DECK).with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].deck").doesNotExist());
    }

    @Test
    void TI21_listagemTrazAOrigemComOCodigoDaQuestao() throws Exception {
        var usuario = usuario();
        var disciplina = disciplina("Direito Civil");
        var questao = questaoPublicada(disciplina, usuario);
        var sessao = sessao(usuario, "FOCO");
        mvc.perform(post("/api/v1/flashcards").with(estudante(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"frente":"Prescrição e decadência","verso":"Diferença de institutos.",
                                 "questaoOrigemId":"%s","sessaoOrigemId":"%s"}
                                """.formatted(questao, sessao)))
                .andExpect(status().isCreated());
        var codigo = jdbc.queryForObject("select codigo from questao where id = ?", String.class, questao);

        mvc.perform(get("/api/v1/flashcards").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].origem.codigo").value(codigo))
                .andExpect(jsonPath("$.content[0].origem.questaoId").value(questao.toString()));
    }

    @Test
    void TI25_todasAsRotasSemTokenRespondem401() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(post("/api/v1/flashcards")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/flashcards")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/flashcards/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/flashcards/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/flashcards/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/flashcards/" + id + "/arquivar")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/flashcards/" + id + "/restaurar")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/flashcards/decks")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/flashcards/decks")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/flashcards/decks/" + id + "/mover")).andExpect(status().isUnauthorized());
    }

    @Test
    void TI27_listagemDe500CardsNaoGeraNMaisUmSobreDeckDisciplinaEQuestao() throws Exception {
        var usuario = usuario();
        var disciplina = disciplina("Direito Administrativo");
        var deckA = deck(usuario, "Deck A");
        var deckB = deck(usuario, "Deck B");
        var questao = questaoPublicada(disciplina, usuario);
        var sessao = sessao(usuario, "FOCO");
        jdbc.update("""
                insert into flashcard (usuario_id, deck_id, frente, verso, disciplina_id, questao_origem_id, sessao_origem_id)
                select ?, case when i % 2 = 0 then ? else ? end, 'Frente ' || i, 'Verso ' || i, ?,
                       case when i % 5 = 0 then ? end, case when i % 5 = 0 then ? end
                  from generate_series(1, 500) as i
                """, usuario, deckA, deckB, disciplina, questao, sessao);
        var estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estatisticas.clear();

        mvc.perform(get("/api/v1/flashcards").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(500));

        assertThat(estatisticas.getPrepareStatementCount()).isLessThanOrEqualTo(20);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor estudante(UUID usuarioId) {
        return user(usuarioId.toString()).roles("ESTUDANTE");
    }

    private UUID usuario() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Estudante', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                id, id + "@flashcard-controller.local");
        return id;
    }

    private UUID disciplina(String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, nome);
        return id;
    }

    private UUID assunto(UUID disciplinaId, String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into assunto (id, disciplina_id, nome) values (?, ?, ?)", id, disciplinaId, nome);
        return id;
    }

    private UUID questaoPublicada(UUID disciplinaId, UUID autorId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id)"
                        + " values (?, 'ME', 'Enunciado de teste com tamanho suficiente.', ?, 'A', 'Comentário.', 'PUBLICADA', ?)",
                id, disciplinaId, autorId);
        return id;
    }

    private UUID sessao(UUID usuarioId, String modo) {
        var id = UUID.randomUUID();
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo) values (?, ?, ?)", id, usuarioId, modo);
        return id;
    }

    private UUID deck(UUID usuarioId, String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into deck (id, usuario_id, nome) values (?, ?, ?)", id, usuarioId, nome);
        return id;
    }

    private UUID cartaoAtivo(UUID usuarioId, NovoCartao dados) {
        var id = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, deck_id, frente, verso, estado, proxima_revisao, proxima_revisao_em)"
                        + " values (?, ?, ?, ?, ?, ?, ?, ?::timestamp at time zone 'America/Sao_Paulo')",
                id, usuarioId, dados.deckId(), "Frente " + id, "Verso " + id, dados.estado().name(),
                dados.proximaRevisao(), dados.proximaRevisao());
        return id;
    }

    private UUID cartaoArquivado(UUID usuarioId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso, status, arquivado_em) values (?, ?, ?, ?, 'ARQUIVADO', now())",
                id, usuarioId, "Frente " + id, "Verso " + id);
        return id;
    }

    private UUID cartaoComTexto(UUID usuarioId, String frente, String verso) {
        var id = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso) values (?, ?, ?, ?)", id, usuarioId, frente, verso);
        return id;
    }

    private record NovoCartao(UUID deckId, EstadoFlashcard estado, LocalDate proximaRevisao) {
    }
}
