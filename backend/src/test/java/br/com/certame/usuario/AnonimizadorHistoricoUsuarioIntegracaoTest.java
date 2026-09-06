package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.desempenho.AtualizadorAgregados;
import br.com.certame.desempenho.ResumoDesempenhoService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
@WithMockUser(username = "anonimizacao", roles = "ESTUDANTE")
class AnonimizadorHistoricoUsuarioIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @Autowired JdbcTemplate jdbc;
    @Autowired AnonimizadorHistoricoUsuario anonimizador;
    @Autowired AtualizadorAgregados atualizador;
    @Autowired ResumoDesempenhoService resumo;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
    }

    @Test
    void TI08_anonimizaHistoricoAposSeteDiasPreservandoAgregados() {
        var dados = prepararDados(Instant.now().minus(8, ChronoUnit.DAYS));
        atualizador.atualizar(dados.usuarioId());
        var antes = resumo.consultar(dados.usuarioId());

        anonimizador.executar();
        atualizador.atualizar(dados.usuarioId());
        var depois = resumo.consultar(dados.usuarioId());

        assertThat(depois).usingRecursiveComparison().isEqualTo(antes);
        assertThat(jdbc.queryForObject("select nome from usuario where id = ?", String.class, dados.usuarioId()))
                .isEqualTo("Usuario anonimizado");
        assertThat(jdbc.queryForObject("select email from usuario where id = ?", String.class, dados.usuarioId()))
                .endsWith("@anonimizado.invalid");
        assertThat(jdbc.queryForObject("select anonimizado_em is not null from usuario where id = ?", Boolean.class, dados.usuarioId()))
                .isTrue();
        assertThat(jdbc.queryForObject("select count(*) from resposta where usuario_id = ?", Integer.class, dados.usuarioId()))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("select frente from flashcard where usuario_id = ?", String.class, dados.usuarioId()))
                .isEqualTo("Flashcard anonimizado");
        assertThat(jdbc.queryForObject("select payload::text from auditoria where entidade_id = ?", String.class, dados.usuarioId()))
                .isEqualTo("{\"anonimizado\": true}");
    }

    @Test
    void TI09_naoAnonimizaHistoricoAntesDoPrazo() {
        var dados = prepararDados(Instant.now().minus(6, ChronoUnit.DAYS));

        anonimizador.executar();

        assertThat(jdbc.queryForObject("select nome from usuario where id = ?", String.class, dados.usuarioId()))
                .isEqualTo("Pessoa Excluida");
        assertThat(jdbc.queryForObject("select anonimizado_em is null from usuario where id = ?", Boolean.class, dados.usuarioId()))
                .isTrue();
    }

    private Dados prepararDados(Instant excluidoEm) {
        var usuarioId = UUID.randomUUID();
        var disciplinaId = UUID.randomUUID();
        var questaoId = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status, excluido_em) values (?, 'Pessoa Excluida', ?, 'hash', 'ESTUDANTE', 'ATIVO', ?)", usuarioId, usuarioId + "@original.local", Timestamp.from(excluidoEm));
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", disciplinaId, "Disciplina " + disciplinaId);
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, autor_id) values (?, 'ME', 'Enunciado', ?, ?)", questaoId, disciplinaId, usuarioId);
        inserirTextosPessoais(usuarioId);
        inserirRespostas(usuarioId, questaoId);
        return new Dados(usuarioId, disciplinaId, questaoId);
    }

    private void inserirTextosPessoais(UUID usuarioId) {
        var deckId = UUID.randomUUID();
        jdbc.update("insert into deck (id, usuario_id, nome, descricao) values (?, ?, 'Meu deck', 'Descricao pessoal')", deckId, usuarioId);
        jdbc.update("insert into caderno (usuario_id, nome, descricao) values (?, 'Meu caderno', 'Descricao pessoal')", usuarioId);
        jdbc.update("insert into flashcard (usuario_id, deck_id, frente, verso, tags) values (?, ?, 'Frente pessoal', 'Verso pessoal', ?)", usuarioId, deckId, new String[] {"pessoal"});
        jdbc.update("insert into auditoria (ator_id, acao, entidade, entidade_id, payload) values (?, 'EDITAR', 'usuario', ?, cast(? as jsonb))", usuarioId, usuarioId, "{\"nome\":\"Pessoa Excluida\"}");
    }

    private void inserirRespostas(UUID usuarioId, UUID questaoId) {
        for (var correta : new boolean[] {true, false}) {
            var sessaoId = UUID.randomUUID();
            var quando = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS));
            jdbc.update("insert into sessao_estudo (id, usuario_id, modo, origem, tamanho, finalizada_em) values (?, ?, 'FOCO', 'LIVRE', 10, ?)", sessaoId, usuarioId, quando);
            jdbc.update("insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms) values (?, ?, ?, 'A', ?, 1000)", sessaoId, usuarioId, questaoId, correta);
        }
    }

    private record Dados(UUID usuarioId, UUID disciplinaId, UUID questaoId) {
    }
}
