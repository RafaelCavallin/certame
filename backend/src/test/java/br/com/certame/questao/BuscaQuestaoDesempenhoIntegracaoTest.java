package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.comum.Paginacao;
import br.com.certame.questao.dto.FiltroQuestao;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * TI-06 — CA-01: a primeira página da listagem sobre 100 mil questões precisa responder abaixo do
 * orçamento de 800 ms (p95), e o plano de consulta precisa usar {@code questao_filtro_idx}.
 * Carga em massa por {@code insert ... select generate_series}, gerada uma vez por classe.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("lento")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuscaQuestaoDesempenhoIntegracaoTest {
    private static final int TOTAL_QUESTOES = 100_000;
    private static final int AQUECIMENTO = 10;
    private static final int MEDICOES = 40;
    private static final Duration ORCAMENTO = Duration.ofMillis(800);

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    static { postgres.start(); }
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

    @BeforeAll
    void semearAcervo() {
        disciplina = UUID.randomUUID(); banca = UUID.randomUUID();
        var autor = UUID.randomUUID();
        jdbc.update("insert into usuario (id,nome,email,senha_hash,perfil,status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')", autor, autor + "@carga.local");
        jdbc.update("insert into disciplina (id,nome) values (?, 'Direito Administrativo')", disciplina);
        jdbc.update("insert into banca (id,nome) values (?, 'Banca da carga')", banca);
        jdbc.update("""
                insert into questao (tipo, enunciado, disciplina_id, banca_id, status, autor_id, gabarito, comentario, atualizado_em)
                select case when i %% 2 = 0 then 'ME' else 'CE' end,
                       'Questao ' || i || ' sobre o regime das licitacoes e contratos administrativos.',
                       ?, ?,
                       case when i %% 3 = 0 then 'PUBLICADA' else 'RASCUNHO' end,
                       ?,
                       case when i %% 3 = 0 then 'A' end,
                       case when i %% 3 = 0 then 'Comentario da questao ' || i end,
                       now() - (i || ' seconds')::interval
                from generate_series(1, %d) as i
                """.formatted(TOTAL_QUESTOES), disciplina, banca, autor);
        jdbc.execute("analyze questao");
    }

    @Test
    void TI06_primeiraPaginaSemFiltroRespondeDentroDoOrcamento() {
        var p95 = medirP95(new FiltroQuestao(null, null, null, null, null, null, null));

        System.out.printf("TI-06 · p95 da primeira página sem filtro com %d questões: %d ms%n", TOTAL_QUESTOES, p95.toMillis());
        assertThat(contarQuestoes()).isEqualTo(TOTAL_QUESTOES);
        assertThat(p95).isLessThan(ORCAMENTO);
    }

    @Test
    void TI06_primeiraPaginaComFiltrosCombinadosRespondeDentroDoOrcamento() {
        var p95 = medirP95(new FiltroQuestao(null, disciplina, null, banca, null, null, StatusQuestao.PUBLICADA));

        System.out.printf("TI-06 · p95 da primeira página com disciplina, banca e status: %d ms%n", p95.toMillis());
        assertThat(p95).isLessThan(ORCAMENTO);
    }

    @Test
    void TI06_primeiraPaginaComBuscaTextualRespondeDentroDoOrcamento() {
        var p95 = medirP95(new FiltroQuestao("licitacoes", null, null, null, null, null, null));

        System.out.printf("TI-06 · p95 da primeira página com busca textual: %d ms%n", p95.toMillis());
        assertThat(p95).isLessThan(ORCAMENTO);
    }

    @Test
    void TI06_planoDaListagemFiltradaUsaOIndiceDeFiltro() {
        var plano = String.join("\n", jdbc.queryForList("""
                explain select q.id, q.codigo, left(q.enunciado, 180), d.nome, b.nome, q.ano, q.tipo, q.status, q.atualizado_em
                from questao q left join disciplina d on d.id = q.disciplina_id left join banca b on b.id = q.banca_id
                where q.excluido_em is null and q.disciplina_id = ? and q.banca_id = ? and q.status = 'PUBLICADA'
                order by q.atualizado_em desc limit 20 offset 0
                """, String.class, disciplina, banca));

        assertThat(plano).contains("questao_filtro_idx");
    }

    private long contarQuestoes() {
        return jdbc.queryForObject("select count(*) from questao where excluido_em is null", Long.class);
    }

    private Duration medirP95(FiltroQuestao filtro) {
        service = new BuscaQuestaoService(repositorio, new SimpleMeterRegistry());
        for (int i = 0; i < AQUECIMENTO; i++) service.buscar(filtro, new Paginacao(0, 20));
        var amostras = new ArrayList<Long>(MEDICOES);
        for (int i = 0; i < MEDICOES; i++) {
            var inicio = System.nanoTime();
            var pagina = service.buscar(filtro, new Paginacao(0, 20));
            amostras.add(System.nanoTime() - inicio);
            assertThat(pagina.content()).isNotEmpty();
        }
        return percentil95(amostras);
    }

    private Duration percentil95(List<Long> amostrasEmNanos) {
        var ordenadas = amostrasEmNanos.stream().sorted().toList();
        var indice = (int) Math.ceil(0.95 * ordenadas.size()) - 1;
        return Duration.ofNanos(ordenadas.get(indice));
    }
}
