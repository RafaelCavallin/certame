package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certame.catalogo.TaxonomiaService;
import br.com.certame.comum.Paginacao;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.PublicacaoInvalidaException;
import br.com.certame.questao.dto.FiltroQuestao;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
 * Subtarefa 10.10 — confere as seis métricas de *Monitoramento e observabilidade* da TechSpec e a
 * política de log: nenhum registro pode conter o enunciado ou o comentário completos.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(username = "admin@certame.local", roles = "ADMIN")
class ObservabilidadeIntegracaoTest {
    private static final String ENUNCIADO = "Enunciado sigiloso que jamais deve aparecer em log algum do sistema.";
    private static final String COMENTARIO = "Comentario sigiloso do professor que jamais deve aparecer em log algum.";
    private static final List<String> METRICAS_ESPERADAS = List.of(
            "certame.questao.publicada", "certame.questao.publicacao.recusada", "certame.questao.despublicada",
            "certame.questao.busca", "certame.taxonomia.exclusao.recusada", "certame.acervo.publicadas");

    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MeterRegistry metricas;
    @Autowired private PublicacaoService publicacao;
    @Autowired private BuscaQuestaoService busca;
    @Autowired private TaxonomiaService taxonomia;

    private ListAppender<ILoggingEvent> registros;
    private ch.qos.logback.classic.Logger aplicacao;
    private Level nivelOriginal;

    /**
     * Captura em TRACE tudo que a aplicação escreve — a política de log incide sobre o que o código
     * do projeto registra, não sobre o dump de protocolo de bibliotecas de infraestrutura.
     */
    @BeforeEach
    void capturarLogs() {
        aplicacao = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("br.com.certame");
        nivelOriginal = aplicacao.getLevel();
        aplicacao.setLevel(Level.TRACE);
        registros = new ListAppender<>();
        registros.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        registros.start();
        aplicacao.addAppender(registros);
    }

    @AfterEach
    void soltarLogs() {
        aplicacao.detachAppender(registros);
        aplicacao.setLevel(nivelOriginal);
        registros.stop();
    }

    @Test
    void expoeAsSeisMetricasDeMonitoramento() {
        exercitarFluxos();

        var nomes = metricas.getMeters().stream().map(m -> m.getId().getName()).distinct().toList();
        assertThat(nomes).containsAll(METRICAS_ESPERADAS);
    }

    @Test
    void nenhumLogRegistraOEnunciadoOuOComentarioCompletos() {
        exercitarFluxos();

        var textos = registros.list.stream().flatMap(evento -> Stream.concat(
                Stream.of(evento.getFormattedMessage(), String.valueOf(evento.getMessage())),
                evento.getArgumentArray() == null ? Stream.of() : Stream.of(evento.getArgumentArray()).map(String::valueOf))).toList();

        assertThat(textos).isNotEmpty();
        assertThat(textos).noneMatch(texto -> texto.contains(ENUNCIADO) || texto.contains(COMENTARIO));
    }

    /** Percorre publicar, recusar, despublicar, buscar e recusar exclusão de assunto vinculado. */
    private void exercitarFluxos() {
        var autor = UUID.randomUUID();
        var disciplina = UUID.randomUUID();
        var assunto = UUID.randomUUID();
        var completa = UUID.randomUUID();
        var incompleta = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')", autor, autor + "@observabilidade.local");
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", disciplina, "Disciplina " + disciplina);
        jdbc.update("insert into assunto (id, disciplina_id, nome) values (?, ?, 'Assunto vinculado')", assunto, disciplina);
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, assunto_id, gabarito, comentario, status, autor_id) values (?, 'CE', ?, ?, ?, 'CERTO', ?, 'EM_REVISAO', ?)",
                completa, ENUNCIADO, disciplina, assunto, COMENTARIO, autor);
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, status, autor_id) values (?, 'CE', ?, ?, 'EM_REVISAO', ?)",
                incompleta, ENUNCIADO, disciplina, autor);

        assertThatThrownBy(() -> publicacao.publicar(incompleta)).isInstanceOf(PublicacaoInvalidaException.class);
        publicacao.publicar(completa);
        publicacao.despublicar(completa);
        busca.buscar(new FiltroQuestao("sigiloso", disciplina, null, null, null, null, null), new Paginacao(0, 20));
        assertThatThrownBy(() -> taxonomia.excluirAssunto(assunto)).isInstanceOf(ConflitoException.class);
    }
}
