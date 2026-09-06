package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
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

/**
 * TI-27 — carga de 50 mil respostas em 8 semanas, montada uma vez por classe com {@code insert
 * … select generate_series}. Mede o p95 dos quatro painéis (servidos pelos agregados) e confirma,
 * pelo plano de consulta, que nenhum deles varre {@code resposta} — só quem escreve o agregado
 * ({@link AtualizadorAgregados}) faz essa varredura, e é executado uma única vez na carga.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(username = "estudante-latencia", roles = "ESTUDANTE")
class DesempenhoLatenciaIntegracaoTest {
    private static final int SESSOES = 1_250;
    private static final int RESPOSTAS_POR_SESSAO = 40;
    private static final int TOTAL_RESPOSTAS = SESSOES * RESPOSTAS_POR_SESSAO;
    private static final int QUESTOES = 50;
    private static final int DISCIPLINAS = 5;
    private static final int ASSUNTOS = 10;
    private static final long LIMITE_P95_MS = 400;
    private static final int AMOSTRAS = 30;

    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResumoDesempenhoService resumo;
    @Autowired private EvolucaoSemanalService evolucao;
    @Autowired private DisciplinaDesempenhoService disciplinas;
    @Autowired private PontosFracosService pontosFracos;

    private static UUID usuarioId;

    @BeforeAll
    static void carregarBaseDe50MilRespostasEAtualizarOsAgregados(@Autowired JdbcTemplate jdbc, @Autowired AtualizadorAgregados atualizador) {
        usuarioId = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Estudante Latência', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                usuarioId, usuarioId + "@latencia.local");

        var disciplinaIds = new ArrayList<UUID>();
        for (int i = 0; i < DISCIPLINAS; i++) {
            var id = UUID.randomUUID();
            jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, "Disciplina latência " + i);
            disciplinaIds.add(id);
        }
        var assuntoIds = new ArrayList<UUID>();
        for (int i = 0; i < ASSUNTOS; i++) {
            var id = UUID.randomUUID();
            jdbc.update("insert into assunto (id, disciplina_id, nome) values (?, ?, ?)", id, disciplinaIds.get(i % DISCIPLINAS), "Assunto latência " + i);
            assuntoIds.add(id);
        }
        for (int i = 0; i < QUESTOES; i++) {
            jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, assunto_id, gabarito, comentario, status, autor_id) values (?, 'ME', ?, ?, ?, 'A', 'Comentário', 'PUBLICADA', ?)",
                    UUID.randomUUID(), "Questão de latência " + i, disciplinaIds.get(i % DISCIPLINAS), assuntoIds.get(i % ASSUNTOS), usuarioId);
        }

        // 1.250 sessões finalizadas, uma por semana (quarta-feira ao meio-dia, hora de São Paulo — a
        // mesma referência de fuso que CalendarioSemanas e FonteRespostas usam para agrupar) para cair
        // exatamente nas 8 semanas de CalendarioSemanas, sem depender do dia em que o teste roda.
        jdbc.update("""
                insert into sessao_estudo (id, usuario_id, modo, origem, finalizada_em, iniciada_em, ultima_atividade_em)
                select gen_random_uuid(), ?, 'FOCO', 'LIVRE', quarta, quarta - interval '10 minutes', quarta
                from generate_series(0, ?) s
                cross join lateral (
                  select (date_trunc('week', now() at time zone 'America/Sao_Paulo')
                          - ((s % 8) || ' days')::interval * 7 + interval '2 days 12 hours') at time zone 'America/Sao_Paulo' as quarta
                ) semana
                """, usuarioId, SESSOES - 1);

        // 40 respostas por sessão, cada uma para uma questão distinta do pool de 50 (deslocamento por sessão).
        jdbc.update("""
                insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms, respondida_em)
                select se.id, se.usuario_id, q.id, 'A', (random() < 0.65), (200 + (random() * 800))::int, se.finalizada_em
                from (select id, usuario_id, finalizada_em, row_number() over (order by id) - 1 as si
                      from sessao_estudo where usuario_id = ?) se
                cross join generate_series(0, ?) as deslocamento(off)
                join (select id, row_number() over (order by id) - 1 as qi from questao where autor_id = ?) q
                  on q.qi = (se.si + deslocamento.off) % ?
                """, usuarioId, RESPOSTAS_POR_SESSAO - 1, usuarioId, QUESTOES);

        assertThat(jdbc.queryForObject("select count(*) from resposta where usuario_id = ?", Integer.class, usuarioId)).isEqualTo(TOTAL_RESPOSTAS);

        atualizador.atualizar(usuarioId);

        assertThat(jdbc.queryForObject("select count(*) from desempenho_semanal where usuario_id = ?", Integer.class, usuarioId)).isEqualTo(8);
    }

    @Test
    void osQuatroPaineisRespondemAbaixoDe400MsNoP95() {
        assertThat(p95(() -> resumo.consultar(usuarioId))).as("painel resumo").isLessThan(LIMITE_P95_MS);
        assertThat(p95(() -> evolucao.consultar(usuarioId))).as("painel evolução").isLessThan(LIMITE_P95_MS);
        assertThat(p95(() -> disciplinas.consultar(usuarioId))).as("painel disciplinas").isLessThan(LIMITE_P95_MS);
        assertThat(p95(() -> pontosFracos.consultar(usuarioId))).as("painel pontos fracos").isLessThan(LIMITE_P95_MS);
    }

    /**
     * Mesmas consultas (por forma) dos repositórios dos quatro painéis: nenhum plano cita
     * {@code resposta} — a leitura é inteiramente sobre os agregados, mesmo com 50 mil linhas na
     * tabela de origem.
     */
    @Test
    void oPlanoDosQuatroPaineisNaoVarreResposta() {
        assertThat(plano("select * from desempenho_disciplina where usuario_id = '" + usuarioId + "' order by volume desc")).doesNotContain("resposta");
        assertThat(plano("select * from desempenho_semanal where usuario_id = '" + usuarioId + "' and semana between current_date - 60 and current_date order by semana asc")).doesNotContain("resposta");
        assertThat(plano("select * from desempenho_assunto where usuario_id = '" + usuarioId + "' and volume >= 20 order by volume desc")).doesNotContain("resposta");
    }

    private long p95(Runnable painel) {
        for (int i = 0; i < 3; i++) painel.run();
        var amostras = new ArrayList<Long>();
        for (int i = 0; i < AMOSTRAS; i++) {
            var inicio = System.nanoTime();
            painel.run();
            amostras.add((System.nanoTime() - inicio) / 1_000_000);
        }
        amostras.sort(Long::compareTo);
        return amostras.get((int) Math.ceil(amostras.size() * 0.95) - 1);
    }

    private String plano(String sql) {
        List<String> linhas = jdbc.queryForList("explain " + sql, String.class);
        return String.join("\n", linhas).toLowerCase();
    }
}
