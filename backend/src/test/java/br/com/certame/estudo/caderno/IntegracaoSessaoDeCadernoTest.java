package br.com.certame.estudo.caderno;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.estudo.OrigemSessao;
import br.com.certame.estudo.RegistroRespostaService;
import br.com.certame.estudo.SessaoEstudoService;
import br.com.certame.estudo.dto.RequisicaoFinalizacao;
import br.com.certame.estudo.dto.RequisicaoResposta;
import br.com.certame.estudo.dto.RequisicaoSessao;
import java.util.List;
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

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(username = "estudante@certame.local")
class IntegracaoSessaoDeCadernoTest {
    private static final int TOTAL = 40;
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
    @Autowired private ConsultaCadernosService consulta;

    @Test
    void TI16_abreSessaoRestritaAsQuarentaQuestoesDoCaderno() {
        var contexto = criarCadernoComQuestoes();

        var sessao = sessoes.abrir(contexto.usuario(), new RequisicaoSessao(null, null, OrigemSessao.CADERNO, contexto.caderno().toString(), null));

        assertThat(sessao.total()).isEqualTo(TOTAL);
        assertThat(sessao.modo().name()).isEqualTo("FOCO");
        assertThat(sessao.origem()).isEqualTo(OrigemSessao.CADERNO);
        assertThat(sessao.questoes()).extracting(q -> q.id()).containsExactlyInAnyOrderElementsOf(contexto.questoes());
    }

    @Test
    void TI17_progressoReflete18Das40RespondidasAposFinalizar() {
        var contexto = criarCadernoComQuestoes();
        var sessao = sessoes.abrir(contexto.usuario(), new RequisicaoSessao(null, null, OrigemSessao.CADERNO, contexto.caderno().toString(), null));
        contexto.questoes().subList(0, 18).forEach(questaoId ->
                respostas.registrar(contexto.usuario(), sessao.sessaoId(), new RequisicaoResposta(questaoId, "A", 1000, null)));
        sessoes.finalizar(contexto.usuario(), sessao.sessaoId(), new RequisicaoFinalizacao(List.of(), null));

        var resumo = consulta.listar(contexto.usuario()).stream()
                .filter(c -> c.id().equals(contexto.caderno().toString())).findFirst().orElseThrow();

        assertThat(resumo.questoesRespondidas()).isEqualTo(18);
        assertThat(resumo.progressoPercentual()).isEqualTo(45);
    }

    private Contexto criarCadernoComQuestoes() {
        var usuario = UUID.randomUUID();
        var disciplina = UUID.randomUUID();
        var caderno = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Pessoa', ?, 'hash', 'ESTUDANTE', 'ATIVO')", usuario, usuario + "@teste.local");
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", disciplina, "Direito " + disciplina);
        jdbc.update("insert into caderno (id, usuario_id, nome) values (?, ?, 'Revisão final')", caderno, usuario);
        var questoes = new java.util.ArrayList<UUID>();
        for (var i = 0; i < TOTAL; i++) {
            var questao = UUID.randomUUID();
            jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'ME', 'Enunciado', ?, 'A', 'Comentário', 'PUBLICADA', ?)", questao, disciplina, usuario);
            jdbc.update("insert into caderno_questao (caderno_id, questao_id) values (?, ?)", caderno, questao);
            questoes.add(questao);
        }
        return new Contexto(usuario, caderno, questoes);
    }

    private record Contexto(UUID usuario, UUID caderno, List<UUID> questoes) {}
}
