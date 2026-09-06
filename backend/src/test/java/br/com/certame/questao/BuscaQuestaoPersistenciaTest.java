package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.comum.Paginacao;
import br.com.certame.questao.dto.FiltroQuestao;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class BuscaQuestaoPersistenciaTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private QuestaoRepository repositorio;
    private BuscaQuestaoService service;
    private UUID disciplina;
    private UUID banca;

    @BeforeEach
    void preparar() {
        service = new BuscaQuestaoService(repositorio, new SimpleMeterRegistry());
        var autor = UUID.randomUUID(); disciplina = UUID.randomUUID(); banca = UUID.randomUUID();
        jdbc.update("insert into usuario (id,nome,email,senha_hash,perfil,status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')", autor, autor + "@busca.local");
        jdbc.update("insert into disciplina (id,nome) values (?, 'Direito Administrativo')", disciplina);
        jdbc.update("insert into banca (id,nome) values (?, 'Banca Busca')", banca);
        jdbc.update("insert into questao (id,codigo,tipo,enunciado,disciplina_id,banca_id,status,autor_id) values (?, 'Q-88409', 'ME', 'Regime das licitações e contratos administrativos', ?, ?, 'RASCUNHO', ?)", UUID.randomUUID(), disciplina, banca, autor);
        jdbc.update("insert into questao (id,codigo,tipo,enunciado,disciplina_id,status,autor_id) values (?, 'Q-88410', 'CE', 'Outro enunciado administrativo', ?, 'EM_REVISAO', ?)", UUID.randomUUID(), disciplina, autor);
    }

    @Test
    void filtraPorTermoEFiltrosCombinados() {
        var resultado = service.buscar(new FiltroQuestao("licitacao", disciplina, null, banca, null, TipoQuestao.ME, StatusQuestao.RASCUNHO), new Paginacao(0, 20));
        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content()).extracting("codigo").containsExactly("Q-88409");
    }

    @Test
    void reconheceBuscaTextualComAcentoEFlexao() {
        var resultado = service.buscar(new FiltroQuestao("licitações", null, null, null, null, null, null), new Paginacao(0, 20));
        assertThat(resultado.content()).extracting("codigo").contains("Q-88409");
    }

    @Test
    void buscaPorCodigoAceitaPrefixoSemHifen() {
        var resultado = service.buscar(new FiltroQuestao("88409", null, null, null, null, null, null), new Paginacao(0, 20));
        assertThat(resultado.content()).extracting("codigo").containsExactly("Q-88409");
    }
}
