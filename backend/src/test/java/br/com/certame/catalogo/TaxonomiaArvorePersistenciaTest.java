package br.com.certame.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.comum.auditoria.RegistradorAuditoria;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A árvore de matérias só é provada contra o PostgreSQL real: os testes de unidade do
 * TaxonomiaService mockam o repositório e não enxergam a SQL. Foi assim que a versão com
 * `grouping()` dentro de `filter` — recusada pelo PostgreSQL — chegou até a suíte E2E.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TaxonomiaArvorePersistenciaTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DisciplinaRepository disciplinas;
    @Autowired private AssuntoRepository assuntos;
    private TaxonomiaService service;
    private UUID disciplina;
    private UUID assuntoComQuestoes;
    private UUID assuntoArquivado;

    @BeforeEach
    void semear() {
        var consulta = new ConsultaArvoreTaxonomia(disciplinas);
        var auditoria = Mockito.mock(RegistradorAuditoria.class);
        var disciplinaService = new DisciplinaTaxonomiaService(disciplinas, auditoria,
                Mockito.mock(br.com.certame.catalogo.edital.EditalConsultaVinculos.class), consulta);
        var assuntoService = new AssuntoTaxonomiaService(disciplinas, assuntos, auditoria, consulta);
        service = new TaxonomiaService(consulta, disciplinaService, assuntoService);
        var autor = UUID.randomUUID();
        disciplina = UUID.randomUUID(); assuntoComQuestoes = UUID.randomUUID(); assuntoArquivado = UUID.randomUUID();
        jdbc.update("insert into usuario (id,nome,email,senha_hash,perfil,status) values (?, 'Autor', ?, 'hash', 'PROFESSOR', 'ATIVO')", autor, autor + "@arvore.local");
        jdbc.update("insert into disciplina (id,nome) values (?, 'Disciplina da árvore')", disciplina);
        jdbc.update("insert into assunto (id,disciplina_id,nome) values (?, ?, 'Assunto com questões')", assuntoComQuestoes, disciplina);
        jdbc.update("insert into assunto (id,disciplina_id,nome,arquivado_em) values (?, ?, 'Assunto arquivado', now())", assuntoArquivado, disciplina);
        inserir(autor, assuntoComQuestoes, "PUBLICADA");
        inserir(autor, assuntoComQuestoes, "RASCUNHO");
        inserir(autor, null, "RASCUNHO");
    }

    @Test
    void TI20_arvoreContaQuestoesDaDisciplinaEDeCadaAssunto() {
        var arvore = service.arvore();

        var no = arvore.stream().filter(d -> d.id().equals(disciplina)).findFirst().orElseThrow();
        assertThat(no.totalQuestoes()).isEqualTo(3);
        assertThat(no.totalPublicadas()).isEqualTo(1);
        assertThat(no.assuntos()).singleElement().satisfies(a -> {
            assertThat(a.id()).isEqualTo(assuntoComQuestoes);
            assertThat(a.totalQuestoes()).isEqualTo(2);
            assertThat(a.totalPublicadas()).isEqualTo(1);
        });
    }

    @Test
    void TI21_arvorePadraoOmiteArquivadosEIncluiQuandoPedido() {
        assertThat(service.arvore()).allSatisfy(d ->
                assertThat(d.assuntos()).extracting(a -> a.id()).doesNotContain(assuntoArquivado));

        var comArquivados = service.arvore(true).stream().filter(d -> d.id().equals(disciplina)).findFirst().orElseThrow();
        assertThat(comArquivados.assuntos()).extracting(a -> a.id())
                .containsExactlyInAnyOrder(assuntoComQuestoes, assuntoArquivado);
    }

    @Test
    void TI22_assuntoSemQuestoesApareceComContagemZero() {
        var vazio = UUID.randomUUID();
        jdbc.update("insert into assunto (id,disciplina_id,nome) values (?, ?, 'Assunto vazio')", vazio, disciplina);

        var no = service.arvore().stream().filter(d -> d.id().equals(disciplina)).findFirst().orElseThrow();

        assertThat(no.assuntos()).filteredOn(a -> a.id().equals(vazio)).singleElement()
                .satisfies(a -> assertThat(a.totalQuestoes()).isZero());
    }

    private void inserir(UUID autor, UUID assunto, String status) {
        jdbc.update("""
                insert into questao (tipo, enunciado, disciplina_id, assunto_id, gabarito, comentario, status, autor_id)
                values ('ME', 'Enunciado da questão da árvore', ?, ?, 'A', 'Comentário.', ?, ?)
                """, disciplina, assunto, status, autor);
    }
}
