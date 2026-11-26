package br.com.certame.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class MigracaoImportacaoIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired private JdbcTemplate jdbc;

    @Test
    void TI01_aplicaMigracaoV19PreservandoQuestoesExistentesComoInternas() {
        var questaoId = inserirQuestao(UUID.randomUUID().toString(), null, null);

        var fonte = jdbc.queryForObject("select fonte from questao where id = ?", String.class, questaoId);
        var identificadorExterno = jdbc.queryForObject("select identificador_externo from questao where id = ?", String.class, questaoId);

        assertThat(fonte).isEqualTo("INTERNA");
        assertThat(identificadorExterno).isNull();
    }

    @Test
    void TI02_geraHashDeEnunciadoParaTodoOAcervoPreExistente() {
        var enunciado = "Enunciado com conteúdo suficiente para o hash " + UUID.randomUUID();
        var questaoId = inserirQuestao(enunciado, null, null);

        var hash = jdbc.queryForObject("select hash_enunciado from questao where id = ?", String.class, questaoId);

        assertThat(hash).isNotBlank();
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void TI03_recusaSegundaGravacaoDoMesmoParFonteEIdentificador() {
        var identificador = "ext-" + UUID.randomUUID();
        inserirQuestao(UUID.randomUUID().toString(), "QAPI", identificador);

        assertThatThrownBy(() -> inserirQuestao(UUID.randomUUID().toString(), "QAPI", identificador))
                .isInstanceOf(RuntimeException.class);
    }

    private UUID inserirQuestao(String enunciado, String fonte, String identificadorExterno) {
        var autor = UUID.randomUUID();
        var disciplina = UUID.randomUUID();
        var questao = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')",
                autor, autor + "@importacao.local");
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", disciplina, "Disciplina " + disciplina);
        if (fonte == null) {
            jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) "
                    + "values (?, 'ME', ?, ?, 'A', 'Comentário', 'PUBLICADA', ?)", questao, enunciado, disciplina, autor);
        } else {
            jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id, fonte, identificador_externo) "
                    + "values (?, 'ME', ?, ?, 'A', 'Comentário', 'PUBLICADA', ?, ?, ?)",
                    questao, enunciado, disciplina, autor, fonte, identificadorExterno);
        }
        return questao;
    }
}
