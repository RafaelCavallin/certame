package br.com.certame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FlywayMigracaoTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("TI-15 aplica V1 em uma base PostgreSQL limpa")
    void deveAplicarMigracaoBase() {
        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion().getVersion())
                .contains("1");

        assertThat(tabelasAcessoCriadas()).containsExactlyInAnyOrder("usuario", "refresh_token", "token_senha");
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'usuario_email_uk'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void TI02_atualizacaoDeRespostaPermaneceImutavel() {
        var dados = prepararResposta();
        jdbc.update("update resposta set marcada = 'B' where id = ?", dados.resposta());
        assertThat(jdbc.queryForObject("select marcada from resposta where id = ?", String.class, dados.resposta())).isEqualTo("A");
    }

    @Test
    void TI03_exclusaoDeRespostaPermaneceImutavel() {
        var dados = prepararResposta();
        jdbc.update("delete from resposta where id = ?", dados.resposta());
        assertThat(jdbc.queryForObject("select count(*) from resposta where id = ?", Integer.class, dados.resposta())).isEqualTo(1);
    }

    @Test
    void TI04_respostaDuplicadaEhRecusada() {
        var dados = prepararResposta();
        assertThatThrownBy(() -> jdbc.update("insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms) values (?, ?, ?, 'B', false, 2)", dados.sessao(), dados.usuario(), dados.questao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI05_marcacaoDuplicadaEhRecusada() {
        var dados = prepararResposta();
        jdbc.update("insert into marcacao (usuario_id, questao_id, tipo) values (?, ?, 'FAVORITA')", dados.usuario(), dados.questao());
        assertThatThrownBy(() -> jdbc.update("insert into marcacao (usuario_id, questao_id, tipo) values (?, ?, 'FAVORITA')", dados.usuario(), dados.questao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI06_tamanhoZeroParaLivreEhRecusado() {
        var dados = prepararResposta("LIVRE", 10, null);
        assertThatThrownBy(() -> jdbc.update("update sessao_estudo set tamanho = 0 where id = ?", dados.sessao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI07_tamanhoAcimaDoLimiteParaLivreEhRecusado() {
        var dados = prepararResposta("LIVRE", 10, null);
        assertThatThrownBy(() -> jdbc.update("update sessao_estudo set tamanho = 51 where id = ?", dados.sessao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI08_tamanhoZeroParaCadernoEhRecusado() {
        var dados = prepararResposta("CADERNO", 10, "ref-caderno");
        assertThatThrownBy(() -> jdbc.update("update sessao_estudo set tamanho = 0 where id = ?", dados.sessao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI09_tamanhoAcimaDoLimiteParaCadernoEhRecusado() {
        var dados = prepararResposta("CADERNO", 10, "ref-caderno");
        assertThatThrownBy(() -> jdbc.update("update sessao_estudo set tamanho = 51 where id = ?", dados.sessao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI10_tamanhoAbaixoDoLimiteParaSimuladoEhRecusado() {
        var dados = prepararResposta("SIMULADO", 10, "ref-simulado");
        assertThatThrownBy(() -> jdbc.update("update sessao_estudo set tamanho = 5 where id = ?", dados.sessao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI11_tamanhoAcimaDoLimiteParaSimuladoEhRecusado() {
        var dados = prepararResposta("SIMULADO", 10, "ref-simulado");
        assertThatThrownBy(() -> jdbc.update("update sessao_estudo set tamanho = 201 where id = ?", dados.sessao())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void TI12_tamanhoQuarentaParaLivreEhAceito() {
        var dados = prepararResposta("LIVRE", 10, null);

        jdbc.update("update sessao_estudo set tamanho = 40 where id = ?", dados.sessao());

        assertThat(jdbc.queryForObject("select tamanho from sessao_estudo where id = ?", Integer.class, dados.sessao())).isEqualTo(40);
    }

    private DadosResposta prepararResposta() {
        return prepararResposta("LIVRE", 10, null);
    }

    private DadosResposta prepararResposta(String origem, int tamanho, String origemRef) {
        var usuario = UUID.randomUUID();
        var disciplina = UUID.randomUUID();
        var questao = UUID.randomUUID();
        var sessao = UUID.randomUUID();
        var resposta = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Teste', ?, 'hash', 'ESTUDANTE', 'ATIVO')", usuario, usuario + "@teste.local");
        jdbc.update("insert into disciplina (id, nome) values (?, 'Teste')", disciplina);
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'ME', 'Teste', ?, 'A', 'Comentario', 'PUBLICADA', ?)", questao, disciplina, usuario);
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo, origem, origem_ref, tamanho) values (?, ?, 'FOCO', ?, ?, ?)", sessao, usuario, origem, origemRef, tamanho);
        jdbc.update("insert into resposta (id, sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms) values (?, ?, ?, ?, 'A', true, 1)", resposta, sessao, usuario, questao);
        return new DadosResposta(usuario, questao, sessao, resposta);
    }

    private record DadosResposta(UUID usuario, UUID questao, UUID sessao, UUID resposta) {}

    private java.util.List<String> tabelasAcessoCriadas() {
        return jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'public' and table_name in ('usuario', 'refresh_token', 'token_senha')
                """, String.class);
    }
}
