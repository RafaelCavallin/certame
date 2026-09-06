package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.comum.excecao.SessaoEncerradaException;
import br.com.certame.estudo.dto.*;
import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class SessaoEstudoIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", postgres::getJdbcUrl);
        registro.add("spring.datasource.username", postgres::getUsername);
        registro.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SessaoEstudoService sessoes;
    @Autowired private RegistroRespostaService respostasService;
    @Autowired private MarcacaoService marcacoes;
    @Autowired private ExpiradorSessoes expirador;
    @Autowired private SessaoEstudoRepository sessaoRepo;
    @Autowired private RespostaRepository respostaRepo;
    @Autowired private br.com.certame.estudo.TituloSessaoService titulos;
    @Autowired private MockMvc mvc;

    private UUID professor;

    @BeforeEach
    void preparar() {
        professor = novoUsuario(PerfilUsuario.PROFESSOR).getId();
    }

    @Test
    void TI08_aberturaDeSessaoNuncaIncluiRascunhoNemEmRevisao() {
        var disciplina = disciplina();
        var publicada = questaoPublicada(disciplina, "ME", "A");
        questaoNaoPublicada(disciplina, "RASCUNHO");
        questaoNaoPublicada(disciplina, "EM_REVISAO");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");

        var resposta = sessoes.abrir(estudante, new RequisicaoSessao(ModoSessao.FOCO, 10, OrigemSessao.LIVRE, null,
                new FiltroSerie(disciplina, null, null, null, null, false, false)));

        assertThat(resposta.total()).isEqualTo(1);
        assertThat(resposta.questoes()).extracting(QuestaoDaSerie::id).containsExactly(publicada);
    }

    @Test
    void TI09_aberturaComFiltroSemCorrespondenciaResponde422SerieVazia() throws Exception {
        var disciplina = disciplina();
        questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();

        mvc.perform(post("/api/v1/estudo/sessoes").with(user(estudante.toString()).roles("ESTUDANTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tamanho\":10,\"filtro\":{\"disciplinaId\":\"" + disciplina + "\",\"ano\":1900}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SERIE_VAZIA"));
    }

    @Test
    void TI11_respostaForaDoModoProvaDevolveGabaritoComentarioEEstatistica() {
        var disciplina = disciplina();
        var questaoId = questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var sessao = abrirSerieDeUma(estudante, disciplina);

        var registrada = respostasService.registrar(estudante, sessao.sessaoId(),
                new RequisicaoResposta(questaoId, "A", 1000, null));

        assertThat(registrada.gabarito()).isEqualTo("A");
        assertThat(registrada.comentario()).isNotBlank();
        assertThat(registrada.correta()).isTrue();
        assertThat(respostaRepo.findBySessaoIdAndQuestaoId(sessao.sessaoId(), questaoId)).isPresent();
    }

    @Test
    void TI12_estatisticaDevolvidaExcluiAsRespostasDoAutorDaQuestao() {
        var disciplina = disciplina();
        var questaoId = questaoPublicada(disciplina, "ME", "A");
        autenticar(professor, "PROFESSOR");
        for (var i = 0; i < 3; i++) {
            var sessaoAutor = abrirSerieDeUma(professor, disciplina);
            respostasService.registrar(professor, sessaoAutor.sessaoId(), new RequisicaoResposta(questaoId, "A", 1000, null));
        }
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var sessaoEstudante = abrirSerieDeUma(estudante, disciplina);

        var registrada = respostasService.registrar(estudante, sessaoEstudante.sessaoId(),
                new RequisicaoResposta(questaoId, "A", 1000, null));

        assertThat(registrada.estatistica()).isNotNull();
        assertThat(registrada.estatistica().totalRespostas()).isEqualTo(1);
    }

    @Test
    void TI14_retomadaDevolveAMesmaSerieNaPrimeiraQuestaoSemResposta() {
        var disciplina = disciplina();
        var questoesIds = List.of(questaoPublicada(disciplina, "ME", "A"), questaoPublicada(disciplina, "ME", "B"),
                questaoPublicada(disciplina, "ME", "A"));
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var aberta = sessoes.abrir(estudante, new RequisicaoSessao(ModoSessao.FOCO, 10, OrigemSessao.LIVRE, null,
                new FiltroSerie(disciplina, null, null, null, null, false, false)));
        assertThat(aberta.total()).isEqualTo(questoesIds.size());
        respostasService.registrar(estudante, aberta.sessaoId(),
                new RequisicaoResposta(aberta.questoes().get(0).id(), "A", 500, null));

        var retomada = sessoes.retomar(estudante, aberta.sessaoId());

        assertThat(retomada.indiceAtual()).isEqualTo(1);
        assertThat(retomada.questoes()).extracting(QuestaoDaSerie::id)
                .containsExactlyElementsOf(aberta.questoes().stream().map(QuestaoDaSerie::id).toList());
    }

    @Test
    void TI15_retomadaDeSessaoDeOutroUsuarioResponde404() {
        var disciplina = disciplina();
        questaoPublicada(disciplina, "ME", "A");
        var dono = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(dono, "ESTUDANTE");
        var sessao = abrirSerieDeUma(dono, disciplina);
        var outro = novoUsuario(PerfilUsuario.ESTUDANTE).getId();

        assertThatThrownBy(() -> sessoes.retomar(outro, sessao.sessaoId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void TI16_modoProvaNaoDevolveGabaritoEmNenhumaEtapaAntesDeFinalizarERecusaRespostaAvulsa() {
        var disciplina = disciplina();
        var questaoId = questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");

        var aberta = sessoes.abrir(estudante, new RequisicaoSessao(ModoSessao.PROVA, 10, OrigemSessao.LIVRE, null,
                new FiltroSerie(disciplina, null, null, null, null, false, false)));

        assertThat(aberta.questoes()).extracting(QuestaoDaSerie::id).contains(questaoId);
        assertThatThrownBy(() -> respostasService.registrar(estudante, aberta.sessaoId(),
                new RequisicaoResposta(questaoId, "A", 1000, null)))
                .isInstanceOf(SessaoEncerradaException.class);
    }

    @Test
    void TI17_finalizacaoDoModoProvaGravaOLoteEDevolveACorrecao() {
        var disciplina = disciplina();
        var certa = questaoPublicada(disciplina, "ME", "A");
        var errada = questaoPublicada(disciplina, "ME", "B");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var aberta = sessoes.abrir(estudante, new RequisicaoSessao(ModoSessao.PROVA, 10, OrigemSessao.LIVRE, null,
                new FiltroSerie(disciplina, null, null, null, null, false, false)));
        var lote = List.of(new RespostaProva(certa, "A", 2000), new RespostaProva(errada, "A", 2000));

        var correcao = sessoes.finalizar(estudante, aberta.sessaoId(), new RequisicaoFinalizacao(lote, 0));

        assertThat(correcao.acertos()).isEqualTo(1);
        assertThat(correcao.erros()).isEqualTo(1);
        assertThat(correcao.total()).isEqualTo(2);
        assertThat(respostaRepo.findBySessaoIdOrderByRespondidaEm(aberta.sessaoId())).hasSize(2);
    }

    @Test
    void TI18_provaAbandonadaNaoDeixaRespostaAlguma() {
        var disciplina = disciplina();
        questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");

        var aberta = sessoes.abrir(estudante, new RequisicaoSessao(ModoSessao.PROVA, 10, OrigemSessao.LIVRE, null,
                new FiltroSerie(disciplina, null, null, null, null, false, false)));

        var total = jdbc.queryForObject("select count(*) from resposta where sessao_id = ?", Integer.class, aberta.sessaoId());
        assertThat(total).isZero();
        var sessaoNoBanco = sessaoRepo.findById(aberta.sessaoId()).orElseThrow();
        assertThat(sessaoNoBanco.getFinalizadaEm()).isNull();
    }

    @Test
    void TI19_sessaoExpiradaSomeDaRetomadaENaoContaComoFinalizada() {
        var disciplina = disciplina();
        questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var aberta = abrirSerieDeUma(estudante, disciplina);
        jdbc.update("update sessao_estudo set ultima_atividade_em = now() - interval '8 days' where id = ?", aberta.sessaoId());

        expirador.expirar();

        assertThatThrownBy(() -> sessoes.retomar(estudante, aberta.sessaoId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        var sessaoNoBanco = sessaoRepo.findById(aberta.sessaoId()).orElseThrow();
        assertThat(sessaoNoBanco.getFinalizadaEm()).isNull();
        assertThat(sessaoNoBanco.getExpiradaEm()).isNotNull();
    }

    @Test
    void TI20_finalizacaoPublicaSessaoFinalizadaDepoisDoCommit() {
        var disciplina = disciplina();
        questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var aberta = abrirSerieDeUma(estudante, disciplina);

        var correcao = sessoes.finalizar(estudante, aberta.sessaoId(), new RequisicaoFinalizacao(List.of(), 0));

        var visivel = jdbc.queryForObject("select finalizada_em is not null from sessao_estudo where id = ?",
                Boolean.class, aberta.sessaoId());
        assertThat(visivel).isTrue();
        assertThat(correcao.finalizadaEm()).isNotNull();
    }

    @Test
    void TI21_marcacaoPersisteEntreSessoes() {
        var disciplina = disciplina();
        var questaoId = questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        marcacoes.aplicar(estudante, questaoId, new RequisicaoMarcacoes(false, true));

        var novaSessao = abrirSerieDeUma(estudante, disciplina);

        assertThat(novaSessao.questoes()).extracting(QuestaoDaSerie::erroRecorrente).containsExactly(true);
    }

    @Test
    void TI23_inicioSomaTempoDeQuestoesEDeFlashcardsQuandoAPortaEstaDisponivel() {
        var disciplina = disciplina();
        questaoPublicada(disciplina, "ME", "A");
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var aberta = abrirSerieDeUma(estudante, disciplina);
        sessoes.finalizar(estudante, aberta.sessaoId(), new RequisicaoFinalizacao(List.of(), 0));
        var tempoExterno = new TempoEstudoExternoDeTeste(930);
        var clock = java.time.Clock.systemUTC();
        var resumoComExterno = new br.com.certame.estudo.inicio.ResumoDiaService(sessaoRepo, respostaRepo,
                new TempoSessao(clock), tempoExterno, clock, titulos);

        var resumo = resumoComExterno.doDia(estudante, java.time.LocalDate.now(br.com.certame.comum.ZonaAplicacao.AMERICA_SAO_PAULO));

        assertThat(resumo.tempoFlashcardsSegundos()).isEqualTo(930);
        assertThat(resumo.tempoEstudoSegundos()).isEqualTo(resumo.tempoQuestoesSegundos() + 930);
    }

    private record TempoEstudoExternoDeTeste(int segundos) implements br.com.certame.estudo.inicio.TempoEstudoExterno {
        @Override
        public int segundosNoDia(UUID usuarioId, java.time.LocalDate dia) {
            return segundos;
        }
    }

    @Test
    void TI24_todosOsSeteEndpointsExigemAutenticacao() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(get("/api/v1/estudo/inicio")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/estudo/sessoes").contentType(MediaType.APPLICATION_JSON).content("{\"tamanho\":10}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/estudo/sessoes/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/estudo/sessoes/" + id + "/modo").contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"CONTEXTO\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/estudo/sessoes/" + id + "/respostas").contentType(MediaType.APPLICATION_JSON).content("{\"questaoId\":\"" + id + "\",\"marcada\":\"A\",\"tempoMs\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/estudo/sessoes/" + id + "/finalizar").contentType(MediaType.APPLICATION_JSON).content("{\"respostas\":[]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/estudo/questoes/" + id + "/marcacoes").contentType(MediaType.APPLICATION_JSON).content("{\"favorita\":true,\"erroRecorrente\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void TI25_aberturaDeSerieDe50QuestoesNaoDependeDeNMaisUmConsultas() {
        var disciplina = disciplina();
        for (var i = 0; i < 50; i++) {
            questaoPublicada(disciplina, "ME", "A");
        }
        var estudante = novoUsuario(PerfilUsuario.ESTUDANTE).getId();
        autenticar(estudante, "ESTUDANTE");
        var inicio = Instant.now();

        var aberta = sessoes.abrir(estudante, new RequisicaoSessao(ModoSessao.FOCO, 50, OrigemSessao.LIVRE, null,
                new FiltroSerie(disciplina, null, null, null, null, false, false)));

        var duracaoMs = Instant.now().toEpochMilli() - inicio.toEpochMilli();
        assertThat(aberta.total()).isEqualTo(50);
        assertThat(aberta.questoes()).allSatisfy(questao -> assertThat(questao.alternativas()).hasSize(5));
        assertThat(duracaoMs).isLessThan(5000);
    }

    private RespostaSessao abrirSerieDeUma(UUID usuarioId, UUID disciplinaId) {
        return sessoes.abrir(usuarioId, new RequisicaoSessao(ModoSessao.FOCO, 10, OrigemSessao.LIVRE, null,
                new FiltroSerie(disciplinaId, null, null, null, null, false, false)));
    }

    private UUID disciplina() {
        var id = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, "Disciplina " + id);
        return id;
    }

    private UUID questaoPublicada(UUID disciplinaId, String tipo, String gabarito) {
        var id = UUID.randomUUID();
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id, publicado_em) "
                        + "values (?, ?, 'Enunciado de teste com tamanho suficiente para publicar.', ?, ?, 'Comentário de teste.', 'PUBLICADA', ?, now())",
                id, tipo, disciplinaId, gabarito, professor);
        for (var letra : List.of("A", "B", "C", "D", "E")) {
            jdbc.update("insert into alternativa (questao_id, letra, texto, ordem) values (?, ?, ?, ?)",
                    id, letra, "Texto da alternativa " + letra, "ABCDE".indexOf(letra) + 1);
        }
        return id;
    }

    private void questaoNaoPublicada(UUID disciplinaId, String status) {
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, status, autor_id) values (?, 'ME', "
                        + "'Enunciado de teste com tamanho suficiente.', ?, ?, ?)",
                UUID.randomUUID(), disciplinaId, status, professor);
    }

    private Usuario novoUsuario(PerfilUsuario perfil) {
        return usuarios.save(new Usuario(perfil + " " + UUID.randomUUID(), UUID.randomUUID() + "@sessao-estudo.local",
                "hash", perfil, StatusUsuario.ATIVO));
    }

    private void autenticar(UUID id, String perfil) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }
}
