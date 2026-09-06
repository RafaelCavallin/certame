package br.com.certame.estudo.caderno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certame.comum.excecao.CadernoAutomaticoImutavelException;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.QuestaoNaoPublicadaException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.estudo.caderno.dto.RequisicaoCaderno;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de {@link CadernoService} e {@link ConsultaQuestoesCadernoService} — TU-20…TU-24
 * (criação, imutabilidade dos automáticos, validação de publicação) sobre PostgreSQL real: os dois
 * serviços dependem de gerar UUID no banco (`@GeneratedValue`) e de SQL nativo, o que um mock puro
 * não reproduz.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CadernoServicoIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CadernoRepository cadernos;
    @Autowired private CadernoQuestaoRepository cadernoQuestoes;
    @Autowired private br.com.certame.questao.QuestaoRepository questoes;

    private CadernoService service;
    private ConsultaQuestoesCadernoService consulta;
    private UUID usuario;
    private UUID disciplina;

    @BeforeEach
    void preparar() {
        var relogio = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);
        var mutadorItens = new MutadorItensCaderno(cadernoQuestoes, questoes, relogio);
        var resumoService = new ResumoCadernoService(cadernos, cadernoQuestoes);
        service = new CadernoService(cadernos, mutadorItens, resumoService, relogio);
        consulta = new ConsultaQuestoesCadernoService(jdbc, service, new CadernoAutomaticoService(jdbc));
        usuario = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Pessoa', ?, 'hash', 'ESTUDANTE', 'ATIVO')", usuario, usuario + "@caderno-servico.local");
        disciplina = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, 'Disciplina do teste')", disciplina);
    }

    @Test
    void TU20_criarRecusaNomeDuplicadoDoMesmoUsuario() {
        service.criar(usuario, new RequisicaoCaderno("Revisão", null));

        assertThatThrownBy(() -> service.criar(usuario, new RequisicaoCaderno("revisão", null)))
                .isInstanceOf(ConflitoException.class)
                .satisfies(ex -> assertThat(((ConflitoException) ex).getCode()).isEqualTo("NOME_CADERNO_EM_USO"));
    }

    @Test
    void TU21_criarAceitaNomeJaUsadoPorOutroUsuario() {
        service.criar(usuario, new RequisicaoCaderno("Revisão", null));
        var outro = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Outra pessoa', ?, 'hash', 'ESTUDANTE', 'ATIVO')", outro, outro + "@caderno-servico.local");

        var resumo = service.criar(outro, new RequisicaoCaderno("Revisão", null));

        assertThat(resumo.nome()).isEqualTo("Revisão");
        assertThat(resumo.totalQuestoes()).isZero();
        assertThat(resumo.progressoPercentual()).isZero();
    }

    @Test
    void criarPersisteDescricaoETrimaONome() {
        var resumo = service.criar(usuario, new RequisicaoCaderno("  Revisão de véspera  ", "Só o essencial."));

        assertThat(resumo.nome()).isEqualTo("Revisão de véspera");
        assertThat(resumo.descricao()).isEqualTo("Só o essencial.");
    }

    @Test
    void TU22_operacoesEmCadernoAutomaticoLancamImutavelENaoPersistemNada() {
        assertThatThrownBy(() -> service.atualizar(usuario, "auto:FAVORITA", new RequisicaoCaderno("Novo nome", null)))
                .isInstanceOf(CadernoAutomaticoImutavelException.class);
        assertThatThrownBy(() -> service.excluir(usuario, "auto:ERRO_RECORRENTE"))
                .isInstanceOf(CadernoAutomaticoImutavelException.class);
        assertThatThrownBy(() -> service.adicionarQuestoes(usuario, "auto:FAVORITA", List.of(UUID.randomUUID())))
                .isInstanceOf(CadernoAutomaticoImutavelException.class);

        assertThat(cadernos.findByUsuarioIdAndExcluidoEmIsNullOrderByAtualizadoEmDesc(usuario)).isEmpty();
    }

    @Test
    void atualizarRenomeiaCadernoManualExistente() {
        var criado = service.criar(usuario, new RequisicaoCaderno("Nome original", null));

        var atualizado = service.atualizar(usuario, criado.id(), new RequisicaoCaderno("Nome novo", "Descrição nova"));

        assertThat(atualizado.nome()).isEqualTo("Nome novo");
        assertThat(atualizado.descricao()).isEqualTo("Descrição nova");
    }

    @Test
    void atualizarRecusaRenomearParaNomeJaUsadoPeloMesmoUsuario() {
        service.criar(usuario, new RequisicaoCaderno("Já existe", null));
        var criado = service.criar(usuario, new RequisicaoCaderno("A renomear", null));

        assertThatThrownBy(() -> service.atualizar(usuario, criado.id(), new RequisicaoCaderno("já existe", null)))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void excluirMarcaExcluidoEmENaoAparecMaisParaOUsuario() {
        var criado = service.criar(usuario, new RequisicaoCaderno("Descartável", null));

        service.excluir(usuario, criado.id());

        assertThatThrownBy(() -> service.obter(usuario, UUID.fromString(criado.id())))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void obterDeOutroUsuarioNaoEEncontrado() {
        var criado = service.criar(usuario, new RequisicaoCaderno("Privado", null));
        var outro = UUID.randomUUID();

        assertThatThrownBy(() -> service.obter(outro, UUID.fromString(criado.id())))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void TU23_adicionarQuestoesRecusaLoteComQuestaoNaoPublicada() {
        var criado = service.criar(usuario, new RequisicaoCaderno("Caderno", null));
        var rascunho = inserirQuestao("RASCUNHO");

        assertThatThrownBy(() -> service.adicionarQuestoes(usuario, criado.id(), List.of(rascunho)))
                .isInstanceOf(QuestaoNaoPublicadaException.class);
        assertThat(service.obter(usuario, UUID.fromString(criado.id())).getId()).isNotNull();
        assertThat(consulta.listar(usuario, criado.id(), 0, 20).totalElements()).isZero();
    }

    @Test
    void TU24_adicionarQuestoesIgnoraQuestaoJaPresente() {
        var criado = service.criar(usuario, new RequisicaoCaderno("Caderno", null));
        var publicada = inserirQuestao("PUBLICADA");

        service.adicionarQuestoes(usuario, criado.id(), List.of(publicada));
        var resumo = service.adicionarQuestoes(usuario, criado.id(), List.of(publicada, publicada));

        assertThat(resumo.totalQuestoes()).isEqualTo(1);
    }

    @Test
    void adicionarQuestoesAtualizaProgressoEListaAsQuestoesDoCaderno() {
        var criado = service.criar(usuario, new RequisicaoCaderno("Caderno", null));
        var publicada = inserirQuestao("PUBLICADA");

        var resumo = service.adicionarQuestoes(usuario, criado.id(), List.of(publicada));

        assertThat(resumo.totalQuestoes()).isEqualTo(1);
        assertThat(resumo.questoesRespondidas()).isZero();
        var pagina = consulta.listar(usuario, criado.id(), 0, 20);
        assertThat(pagina.totalElements()).isEqualTo(1);
        assertThat(pagina.content()).extracting("questaoId").containsExactly(publicada);
    }

    @Test
    void removerQuestaoRetiraDoCadernoESoDaqueleCaderno() {
        var criado = service.criar(usuario, new RequisicaoCaderno("Caderno", null));
        var publicada = inserirQuestao("PUBLICADA");
        service.adicionarQuestoes(usuario, criado.id(), List.of(publicada));

        service.removerQuestao(usuario, criado.id(), publicada);

        assertThat(consulta.listar(usuario, criado.id(), 0, 20).totalElements()).isZero();
    }

    @Test
    void consultaListarDeCadernoAutomaticoSemMarcacaoDevolveListaVazia() {
        var pagina = consulta.listar(usuario, "auto:FAVORITA", 0, 20);

        assertThat(pagina.content()).isEmpty();
        assertThat(pagina.totalElements()).isZero();
    }

    private UUID inserirQuestao(String status) {
        var id = UUID.randomUUID();
        if (status.equals("PUBLICADA")) {
            jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'CE', 'Enunciado de teste com tamanho suficiente.', ?, 'CERTO', 'Comentário do teste.', 'PUBLICADA', ?)",
                    id, disciplina, usuario);
        } else {
            jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, status, autor_id) values (?, 'ME', 'Enunciado de teste com tamanho suficiente.', ?, ?, ?)",
                    id, disciplina, status, usuario);
        }
        return id;
    }
}
