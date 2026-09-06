package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EditalPersistenciaTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EditalRepository editais;

    private UUID novaDisciplina(String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, nome);
        return id;
    }

    @Test
    void TI01_aplicaV12EmBaseLimpa() {
        var tabelas = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public' and table_name in ('edital','edital_disciplina')",
                String.class);

        assertThat(tabelas).containsExactlyInAnyOrder("edital", "edital_disciplina");
    }

    @Test
    void TI02_recusaEditalAbertoSemPrazoDeInscricao() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into edital (nome, status) values ('Edital sem prazo', 'ABERTO')"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void TI02_aceitaEditalAbertoComPrazoDeInscricao() {
        var novo = new Edital("Edital com prazo", StatusEdital.ABERTO);
        novo.setPrazoInscricao(LocalDate.now().plusDays(30));

        var edital = editais.saveAndFlush(novo);

        assertThat(jdbc.queryForObject("select count(*) from edital where id = ?", Integer.class, edital.getId()))
                .isEqualTo(1);
    }

    @Test
    void TI03_recusaNomeDuplicadoEntreEditaisNaoExcluidos() {
        editais.saveAndFlush(new Edital("Concurso Duplicado", StatusEdital.PREVISTO));

        assertThatThrownBy(() -> editais.saveAndFlush(new Edital("concurso duplicado", StatusEdital.PREVISTO)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void TI03_permiteReusarNomeDeEditalExcluido() {
        var excluido = editais.saveAndFlush(new Edital("Concurso Reaproveitado", StatusEdital.PREVISTO));
        excluido.setExcluidoEm(java.time.Instant.now());
        editais.saveAndFlush(excluido);

        var reaproveitado = editais.saveAndFlush(new Edital("concurso reaproveitado", StatusEdital.PREVISTO));

        assertThat(reaproveitado.getId()).isNotNull();
    }

    @Test
    void TI04_recusaParDuplicadoEmEditalDisciplina() {
        var edital = editais.saveAndFlush(new Edital("Concurso com disciplina", StatusEdital.PREVISTO));
        var disciplina = novaDisciplina("Disciplina do vínculo");
        jdbc.update("insert into edital_disciplina (edital_id, disciplina_id) values (?, ?)", edital.getId(), disciplina);

        assertThatThrownBy(() -> jdbc.update(
                "insert into edital_disciplina (edital_id, disciplina_id) values (?, ?)", edital.getId(), disciplina))
                .isInstanceOf(Exception.class);
    }
}
