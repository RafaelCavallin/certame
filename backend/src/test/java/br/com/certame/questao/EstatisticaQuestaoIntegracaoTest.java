package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EstatisticaQuestaoIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EstatisticaQuestaoService service;

    @Test
    void recalculaComRespostasDoAutorEDeTerceiros() {
        var autor = UUID.randomUUID();
        var terceiro = UUID.randomUUID();
        var questao = UUID.randomUUID();
        var sessaoAutor = UUID.randomUUID();
        var sessaoTerceiro = UUID.randomUUID();
        var sessaoTerceiroDois = UUID.randomUUID();
        var sessaoTerceiroTres = UUID.randomUUID();
        inserirUsuario(autor, "autor");
        inserirUsuario(terceiro, "terceiro");
        var disciplina = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, 'Estatística')", disciplina);
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'ME', 'Questão de teste', ?, 'A', 'Comentário', 'PUBLICADA', ?)", questao, disciplina, autor);
        jdbc.update("insert into questao_estatistica (questao_id) values (?)", questao);
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo) values (?, ?, 'FOCO'), (?, ?, 'FOCO'), (?, ?, 'FOCO'), (?, ?, 'FOCO')", sessaoAutor, autor, sessaoTerceiro, terceiro, sessaoTerceiroDois, terceiro, sessaoTerceiroTres, terceiro);
        inserirResposta(sessaoAutor, autor, questao, true);
        inserirResposta(sessaoTerceiro, terceiro, questao, true);
        inserirResposta(sessaoTerceiroDois, terceiro, questao, false);
        inserirResposta(sessaoTerceiroTres, terceiro, questao, true);

        var resultado = service.recalcular(questao);

        assertThat(resultado.totalRespostas()).isEqualTo(3);
        assertThat(resultado.totalAcertos()).isEqualTo(2);
        assertThat(resultado.pctAcerto()).isEqualTo((short) 67);
        assertThat(jdbc.queryForObject("select total_respostas from questao_estatistica where questao_id = ?", Integer.class, questao)).isEqualTo(3);
    }

    private void inserirUsuario(UUID id, String nome) {
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, ?, ?, 'hash', 'ESTUDANTE', 'ATIVO')", id, nome, id + "@estatistica.local");
    }

    private void inserirResposta(UUID sessao, UUID usuario, UUID questao, boolean correta) {
        jdbc.update("insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms) values (?, ?, ?, 'A', ?, 1000)", sessao, usuario, questao, correta);
    }
}
