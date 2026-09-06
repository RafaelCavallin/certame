package br.com.certame.estudo.caderno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CadernosPersistenciaIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void TI01_aplicaMigracaoEDdlDasSeisTabelas() {
        var aplicadas = flyway.info().applied();
        assertThat(aplicadas).extracting(info -> info.getVersion().getVersion()).contains("6", "7");
        assertThat(jdbc.queryForList("select table_name from information_schema.tables where table_schema = 'public' and (table_name like 'caderno%' or table_name like 'desempenho_%')", String.class))
                .contains("caderno", "caderno_questao", "desempenho_semanal", "desempenho_disciplina", "desempenho_assunto", "desempenho_pendencia");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void TI02_nomeRepetidoNoUsuarioEReutilizadoAposExclusaoLogica() {
        var usuario = inserirUsuario();
        jdbc.update("insert into caderno (usuario_id, nome) values (?, 'Revisão')", usuario);
        assertThatThrownBy(() -> jdbc.update("insert into caderno (usuario_id, nome) values (?, 'revisão')", usuario)).isInstanceOf(Exception.class);
        jdbc.update("update caderno set excluido_em = now() where usuario_id = ?", usuario);
        jdbc.update("insert into caderno (usuario_id, nome) values (?, 'REVISÃO')", usuario);
        assertThat(jdbc.queryForObject("select count(*) from caderno where usuario_id = ?", Integer.class, usuario)).isEqualTo(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void TI03_pkCompostaImpedeDuplicidadeDoVinculo() {
        var ids = inserirCadernoEQuestao();
        jdbc.update("insert into caderno_questao (caderno_id, questao_id) values (?, ?) on conflict do nothing", ids[0], ids[1]);
        assertThat(jdbc.queryForObject("select count(*) from caderno_questao where caderno_id = ?", Integer.class, ids[0])).isEqualTo(1);
    }

    @Test
    void TI04_excluirCadernoPreservaRespostaEMarcacao() {
        var ids = inserirCadernoEQuestao();
        var sessao = UUID.randomUUID();
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo) values (?, ?, 'FOCO')", sessao, ids[2]);
        jdbc.update("insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms) values (?, ?, ?, 'A', true, 10)", sessao, ids[2], ids[1]);
        jdbc.update("insert into marcacao (usuario_id, questao_id, tipo) values (?, ?, 'FAVORITA')", ids[2], ids[1]);
        jdbc.update("delete from caderno where id = ?", ids[0]);
        assertThat(jdbc.queryForObject("select count(*) from caderno_questao", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from resposta", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from marcacao", Integer.class)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void TI05_recusaAcertosMaioresQueVolume() {
        var usuario = inserirUsuario();
        assertThatThrownBy(() -> jdbc.update("insert into desempenho_semanal (usuario_id, semana, volume, acertos) values (?, '2026-08-24', 1, 2)", usuario)).isInstanceOf(Exception.class);
        assertThat(jdbc.queryForObject("select count(*) from desempenho_semanal where usuario_id = ?", Integer.class, usuario)).isZero();
    }

    private UUID inserirUsuario() {
        var usuario = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Pessoa', ?, 'hash', 'ESTUDANTE', 'ATIVO')", usuario, usuario + "@teste.local");
        return usuario;
    }

    private UUID[] inserirCadernoEQuestao() {
        var usuario = inserirUsuario();
        var disciplina = UUID.randomUUID();
        var questao = UUID.randomUUID();
        var caderno = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, 'Disciplina')", disciplina);
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, status, autor_id) values (?, 'ME', 'Enunciado', ?, 'RASCUNHO', ?)", questao, disciplina, usuario);
        jdbc.update("insert into caderno (id, usuario_id, nome) values (?, ?, 'Caderno')", caderno, usuario);
        jdbc.update("insert into caderno_questao (caderno_id, questao_id) values (?, ?)", caderno, questao);
        return new UUID[] { caderno, questao, usuario };
    }
}
