package br.com.certame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import br.com.certame.questao.QuestaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class BancoQuestoesPersistenciaTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired private JdbcTemplate jdbc;
    @Autowired private QuestaoRepository questoes;
    @Autowired private br.com.certame.usuario.UsuarioRepository usuarios;

    @Test
    void TI01_aplicaV2EV3EmBaseLimpa() {
        var tabelas = jdbc.queryForList("select table_name from information_schema.tables where table_schema = 'public' and table_name in ('disciplina','assunto','banca','orgao','questao','alternativa','questao_estatistica','auditoria')", String.class);
        assertThat(tabelas).containsExactlyInAnyOrder("disciplina", "assunto", "banca", "orgao", "questao", "alternativa", "questao_estatistica", "auditoria");
        assertThat(jdbc.queryForObject("select count(*) from banca", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("select count(*) from orgao", Integer.class)).isEqualTo(10);
    }

    @Test
    void TI19_recusaQuestaoPublicadaIncompleta() {
        var autor = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')", autor, autor + "@teste.local");
        assertThatThrownBy(() -> jdbc.update("insert into questao (tipo, enunciado, status, autor_id) values ('ME', 'Enunciado incompleto', 'PUBLICADA', ?)", autor))
                .isInstanceOf(Exception.class);
    }

    @Test
    void TI15_consultaDeSerieIgnoraQuestoesNaoPublicadasEExcluidas() {
        var autor = UUID.randomUUID();
        var disciplina = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')", autor, autor + "@serie.local");
        jdbc.update("insert into disciplina (id, nome) values (?, 'Disciplina da série')", disciplina);
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'ME', 'Publicada', ?, 'A', 'Comentário', 'PUBLICADA', ?)", UUID.randomUUID(), disciplina, autor);
        jdbc.update("insert into questao (tipo, enunciado, disciplina_id, status, autor_id) values ('ME', 'Rascunho', ?, 'RASCUNHO', ?), ('ME', 'Revisão', ?, 'EM_REVISAO', ?)", disciplina, autor, disciplina, autor);

        assertThat(questoes.buscarIdsPublicadas()).hasSize(1);
    }

    @Test
    void codigoDaQuestaoContinuaUnicoDepoisDeNoventaMilQuestoes() {
        var autor = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')", autor, autor + "@codigo.local");
        jdbc.execute("select setval('questao_codigo_seq', 99999, false)");

        jdbc.update("insert into questao (tipo, enunciado, status, autor_id) select 'ME', 'Enunciado ' || i, 'RASCUNHO', ? from generate_series(1, 3) as i", autor);

        assertThat(jdbc.queryForList("select codigo from questao order by codigo", String.class))
                .containsExactly("Q-100000", "Q-100001", "Q-99999");
    }

    @Test
    void codigoAtribuidoPeloGeradorEOMesmoQueFicaGravado() {
        var autor = usuarios.save(new br.com.certame.usuario.Usuario("Autor", UUID.randomUUID() + "@codigo-api.local",
                "hash", br.com.certame.usuario.PerfilUsuario.PROFESSOR, br.com.certame.usuario.StatusUsuario.ATIVO));
        var questao = new br.com.certame.questao.Questao(br.com.certame.questao.TipoQuestao.ME, "Enunciado do código", autor);
        questao.setCodigo("Q-77777");

        var salva = questoes.saveAndFlush(questao);

        assertThat(jdbc.queryForObject("select codigo from questao where id = ?", String.class, salva.getId()))
                .isEqualTo("Q-77777");
    }
}
