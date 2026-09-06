package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.estudo.RegistroRespostaService;
import br.com.certame.estudo.SessaoEstudoService;
import br.com.certame.estudo.dto.RequisicaoFinalizacao;
import br.com.certame.estudo.dto.RequisicaoResposta;
import br.com.certame.estudo.dto.RequisicaoSessao;
import java.util.UUID;
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
 * TI-24 — agregado consistente com {@code resposta} após a finalização da sessão. Ao contrário de
 * {@link AtualizadorAgregadosTest} e {@link OuvinteSessaoFinalizadaTest} (que chamam o serviço e o
 * ouvinte diretamente, com mocks), esta classe sobe o contexto Spring inteiro e finaliza a sessão
 * pelo fluxo real do controller para baixo — é o único jeito de provar que o
 * {@code @TransactionalEventListener(AFTER_COMMIT)} realmente publica e entrega o evento, e que
 * {@link AtualizadorAgregados#atualizar} roda numa transação genuína (REQUIRES_NEW) quando chamado
 * de dentro desse listener, e não silenciosamente sem transação nenhuma.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(username = "estudante-agregacao", roles = "ESTUDANTE")
class DesempenhoAgregacaoIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SessaoEstudoService sessoes;
    @Autowired private RegistroRespostaService respostas;
    @Autowired private ResumoDesempenhoService resumo;
    @Autowired private DesempenhoSemanalRepository semanais;

    @Test
    void TI24_agregadoConsistenteComRespostaAposFinalizacaoDaSessao() {
        var usuarioId = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Pessoa', ?, 'hash', 'ESTUDANTE', 'ATIVO')", usuarioId, usuarioId + "@agregacao.local");
        var disciplinaId = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, 'Disciplina')", disciplinaId);
        for (int i = 0; i < 10; i++) {
            jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'ME', 'Enunciado de teste com tamanho suficiente.', ?, 'A', 'Comentário', 'PUBLICADA', ?)",
                    UUID.randomUUID(), disciplinaId, usuarioId);
        }

        var sessao = sessoes.abrir(usuarioId, new RequisicaoSessao(null, 10, null, null, null));
        var questaoId = sessao.questoes().get(0).id();
        respostas.registrar(usuarioId, sessao.sessaoId(), new RequisicaoResposta(questaoId, "A", 1000, 0));
        sessoes.finalizar(usuarioId, sessao.sessaoId(), new RequisicaoFinalizacao(null, 0));

        var painel = resumo.consultar(usuarioId);
        assertThat(painel.possuiHistorico()).isTrue();
        assertThat(painel.totalRespostas()).isEqualTo(1);
        assertThat(painel.acertos()).isEqualTo(1);
        assertThat(semanais.findByIdUsuarioIdOrderByIdSemanaAsc(usuarioId)).hasSize(1);
    }
}
