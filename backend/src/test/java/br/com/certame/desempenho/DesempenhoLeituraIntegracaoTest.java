package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * TI-19, TI-20, TI-21, TI-22, TI-23 e TI-26 — os quatro paineis de leitura, exercitados sobre
 * agregados construidos a partir de resposta/sessao_estudo reais com o mesmo AtualizadorAgregados
 * que o OuvinteSessaoFinalizada chama em producao. Faltavam da suite (bloqueados por F2 no momento
 * da tarefa 6.0) e nunca foram escritos apos a entrega de prd-sessao-de-estudo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(username = "estudante-leitura", roles = "ESTUDANTE")
class DesempenhoLeituraIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private AtualizadorAgregados atualizador;
    @Autowired private EvolucaoSemanalService evolucao;
    @Autowired private DisciplinaDesempenhoService disciplinasService;
    @Autowired private PontosFracosService pontosFracosService;

    private UUID usuarioId;
    private UUID disciplinaForte;
    private UUID disciplinaFraca;
    private UUID assuntoForte;
    private UUID assuntoFraco;

    @BeforeEach
    void preparar() {
        usuarioId = criarUsuario();
        disciplinaForte = criarDisciplina("Direito Constitucional");
        disciplinaFraca = criarDisciplina("Português");
        assuntoForte = criarAssunto(disciplinaForte, "Controle de constitucionalidade");
        assuntoFraco = criarAssunto(disciplinaFraca, "Crase");
    }

    @Test
    void TI19_evolucaoComAtividadeEmVariasSemanas() {
        var questao = criarQuestao(disciplinaForte, assuntoForte);
        inserirResposta(usuarioId, questao, true, Instant.now().minus(7, ChronoUnit.DAYS));
        inserirResposta(usuarioId, questao, true, Instant.now());
        atualizador.atualizar(usuarioId);

        var resultado = evolucao.consultar(usuarioId);

        assertThat(resultado.semanas()).hasSize(8);
        assertThat(resultado.volumeTotal()).isEqualTo(2);
        assertThat(resultado.semanas()).filteredOn(semana -> semana.volume() > 0).hasSize(2);
    }

    @Test
    void TI20_evolucaoComDuasSemanasSemAtividadeApareceComoLacuna() {
        var questao = criarQuestao(disciplinaForte, assuntoForte);
        inserirResposta(usuarioId, questao, true, Instant.now());
        atualizador.atualizar(usuarioId);

        var resultado = evolucao.consultar(usuarioId);

        assertThat(resultado.semanas()).filteredOn(semana -> semana.pctAcerto() == null).hasSize(7)
                .allSatisfy(semana -> assertThat(semana.volume()).isZero());
    }

    @Test
    void TI21_disciplinasOrdenadasPorVolumeDesc() {
        var forte = criarQuestao(disciplinaForte, assuntoForte);
        var fraca = criarQuestao(disciplinaFraca, assuntoFraco);
        inserirResposta(usuarioId, forte, true, Instant.now());
        for (int i = 0; i < 3; i++) inserirResposta(usuarioId, fraca, false, Instant.now());
        atualizador.atualizar(usuarioId);

        var resultado = disciplinasService.consultar(usuarioId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).disciplinaId()).isEqualTo(disciplinaFraca);
        assertThat(resultado.get(0).volume()).isEqualTo(3);
    }

    @Test
    void TI22_pontosFracosAplicaVolumeMinimoEMediaDoUsuario() {
        var forte = criarQuestao(disciplinaForte, assuntoForte);
        var fraco = criarQuestao(disciplinaFraca, assuntoFraco);
        for (int i = 0; i < 20; i++) inserirResposta(usuarioId, forte, true, Instant.now());
        for (int i = 0; i < 20; i++) inserirResposta(usuarioId, fraco, i < 4, Instant.now());
        atualizador.atualizar(usuarioId);

        var resultado = pontosFracosService.consultar(usuarioId);

        assertThat(resultado.itens()).extracting("assuntoId").containsExactly(assuntoFraco);
    }

    @Test
    void TI23_respostaDeSessaoDeModoProvaAbandonadaNaoEntraEmMetrica() {
        var questao = criarQuestao(disciplinaForte, assuntoForte);
        var sessaoAbandonada = criarSessaoProvaAbandonada(usuarioId);
        jdbc.update("insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms, respondida_em) "
                + "values (?, ?, ?, 'A', true, 1000, now())", sessaoAbandonada, usuarioId, questao);
        atualizador.atualizar(usuarioId);

        var resultado = evolucao.consultar(usuarioId);

        assertThat(resultado.volumeTotal()).isZero();
    }

    @Test
    void TI26_desempenhoDeUmUsuarioNuncaIncluiRespostaDeOutro() {
        var outroUsuario = criarUsuario();
        var questao = criarQuestao(disciplinaForte, assuntoForte);
        inserirResposta(usuarioId, questao, true, Instant.now());
        for (int i = 0; i < 5; i++) inserirResposta(outroUsuario, questao, false, Instant.now());
        atualizador.atualizar(usuarioId);
        atualizador.atualizar(outroUsuario);

        var resultado = disciplinasService.consultar(usuarioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).volume()).isEqualTo(1);
        assertThat(resultado.get(0).pctAcerto()).isEqualTo(100);
    }

    private void inserirResposta(UUID usuario, UUID questaoId, boolean correta, Instant quando) {
        var sessaoId = criarSessao(usuario, quando);
        jdbc.update("insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms, respondida_em) "
                + "values (?, ?, ?, 'A', ?, 1000, ?)", sessaoId, usuario, questaoId, correta, Timestamp.from(quando));
    }

    private UUID criarSessao(UUID usuario, Instant finalizadaEm) {
        var id = UUID.randomUUID();
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo, origem, tamanho, finalizada_em) values (?, ?, 'FOCO', 'LIVRE', 10, ?)",
                id, usuario, Timestamp.from(finalizadaEm));
        return id;
    }

    private UUID criarSessaoProvaAbandonada(UUID usuario) {
        var id = UUID.randomUUID();
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo, origem, tamanho, finalizada_em) values (?, ?, 'PROVA', 'LIVRE', 10, null)", id, usuario);
        return id;
    }

    private UUID criarUsuario() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Pessoa', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                id, id + "@leitura.local");
        return id;
    }

    private UUID criarDisciplina(String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, nome + " " + id);
        return id;
    }

    private UUID criarAssunto(UUID disciplinaId, String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into assunto (id, disciplina_id, nome) values (?, ?, ?)", id, disciplinaId, nome + " " + id);
        return id;
    }

    private UUID criarQuestao(UUID disciplinaId, UUID assuntoId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, assunto_id, gabarito, comentario, status, autor_id) "
                + "values (?, 'ME', 'Enunciado de teste com tamanho suficiente.', ?, ?, 'A', 'Comentário', 'PUBLICADA', ?)",
                id, disciplinaId, assuntoId, usuarioId);
        return id;
    }
}
