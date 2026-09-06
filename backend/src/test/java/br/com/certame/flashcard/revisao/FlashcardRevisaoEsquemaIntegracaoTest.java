package br.com.certame.flashcard.revisao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.StatusFlashcard;
import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class FlashcardRevisaoEsquemaIntegracaoTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FlashcardRepository flashcards;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private SessaoRevisaoRepository sessoes;

    @Autowired
    private FlashcardRevisaoRepository revisoes;

    @Test
    @DisplayName("TI-01 migracao V14 preenche proxima_revisao_em coerente com proxima_revisao")
    void TI01_migracaoPreencheProximaRevisaoEmCoerenteComProximaRevisao() {
        var tabelas = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public' and table_name = 'flashcard_sessao'",
                String.class);
        assertThat(tabelas).containsExactly("flashcard_sessao");

        var colunas = jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'flashcard' and column_name in ('proxima_revisao_em','passo_aprendizado')",
                String.class);
        assertThat(colunas).containsExactlyInAnyOrder("proxima_revisao_em", "passo_aprendizado");

        var colunasRevisao = jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'flashcard_revisao' and column_name in ('sessao_id','intervalo_minutos','segundos_decorridos')",
                String.class);
        assertThat(colunasRevisao).containsExactlyInAnyOrder("sessao_id", "intervalo_minutos", "segundos_decorridos");

        for (var indice : new String[] {"flashcard_sessao_idx", "flashcard_sessao_dia_idx", "flashcard_fila_hora_idx",
                "flashcard_revisao_sessao_idx", "flashcard_revisao_dia_idx"}) {
            assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = ?", Integer.class, indice))
                    .as("indice %s", indice)
                    .isEqualTo(1);
        }

        var usuario = criarUsuario();
        var idCard = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso) values (?, ?, 'Frente valida', 'Verso valido')", idCard, usuario);

        var coerente = jdbc.queryForObject(
                "select proxima_revisao = (proxima_revisao_em at time zone 'America/Sao_Paulo')::date from flashcard where id = ?",
                Boolean.class, idCard);
        assertThat(coerente).isTrue();

        // insercao legada (como a semente ja aplicada V908) so informa proxima_revisao: o gatilho
        // flashcard_preenche_agenda preenche proxima_revisao_em coerente, sem exigir edicao daquela migracao.
        var idLegado = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso, proxima_revisao) values (?, ?, 'Frente legada', 'Verso legado', current_date - 2)",
                idLegado, usuario);

        var coerenteLegado = jdbc.queryForObject(
                "select proxima_revisao = (proxima_revisao_em at time zone 'America/Sao_Paulo')::date from flashcard where id = ?",
                Boolean.class, idLegado);
        assertThat(coerenteLegado).isTrue();
        assertThat(jdbc.queryForObject("select proxima_revisao_em is not null from flashcard where id = ?", Boolean.class, idLegado)).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("TI-02 check flashcard_agenda_coerente recusa divergencia entre proxima_revisao e proxima_revisao_em")
    void TI02_checkAgendaCoerenteRecusaDivergencia() {
        var usuario = criarUsuario();

        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard (usuario_id, frente, verso, proxima_revisao, proxima_revisao_em) values (?, 'Frente ok', 'Verso ok', current_date + 5, now())",
                usuario)).isInstanceOf(Exception.class);

        var idOk = UUID.randomUUID();
        jdbc.update(
                "insert into flashcard (id, usuario_id, frente, verso, proxima_revisao, proxima_revisao_em)"
                        + " values (?, ?, 'Frente ok', 'Verso ok',"
                        + " (now() at time zone 'America/Sao_Paulo')::date, now())",
                idOk, usuario);
        assertThat(jdbc.queryForObject("select count(*) from flashcard where id = ?", Integer.class, idOk)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("TI-03 checks de escopo e de contagem de flashcard_sessao")
    void TI03_checksDeEscopoEContagemDeFlashcardSessao() {
        var usuario = criarUsuario();
        var deck = criarDeck(usuario);

        // escopo DECK sem deck_id deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard_sessao (usuario_id, escopo) values (?, 'DECK')", usuario))
                .isInstanceOf(Exception.class);

        // escopo TODOS com deck_id preenchido deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard_sessao (usuario_id, escopo, deck_id) values (?, 'TODOS', ?)", usuario, deck))
                .isInstanceOf(Exception.class);

        // revisados maior que a soma das notas deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard_sessao (usuario_id, escopo, revisados, errei, dificil, bom, facil) values (?, 'TODOS', 5, 1, 0, 1, 0)",
                usuario)).isInstanceOf(Exception.class);

        // casos validos devem passar
        var idTodos = UUID.randomUUID();
        jdbc.update("insert into flashcard_sessao (id, usuario_id, escopo) values (?, ?, 'TODOS')", idTodos, usuario);
        assertThat(jdbc.queryForObject("select count(*) from flashcard_sessao where id = ?", Integer.class, idTodos)).isEqualTo(1);

        var idDeck = UUID.randomUUID();
        jdbc.update("insert into flashcard_sessao (id, usuario_id, escopo, deck_id) values (?, ?, 'DECK', ?)", idDeck, usuario, deck);
        assertThat(jdbc.queryForObject("select count(*) from flashcard_sessao where id = ?", Integer.class, idDeck)).isEqualTo(1);

        var idContagemOk = UUID.randomUUID();
        jdbc.update(
                "insert into flashcard_sessao (id, usuario_id, escopo, revisados, errei, dificil, bom, facil) values (?, ?, 'TODOS', 2, 1, 0, 1, 0)",
                idContagemOk, usuario);
        assertThat(jdbc.queryForObject("select count(*) from flashcard_sessao where id = ?", Integer.class, idContagemOk)).isEqualTo(1);
    }

    @Test
    @DisplayName("TI-04 exclusao logica de card preserva o log de revisoes")
    void TI04_exclusaoLogicaDeCardPreservaLogDeRevisoes() {
        var usuario = usuarios.save(new Usuario("Estudante Teste", UUID.randomUUID() + "@teste.local", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO));
        var card = flashcards.save(new Flashcard(usuario, "Frente do card", "Verso do card"));

        var sessao = sessoes.save(new SessaoRevisao(usuario, "TODOS", null, null));
        revisoes.save(new FlashcardRevisao(card, usuario, sessao, "BOM",
                (short) 3, 1.2f, 2.1f, 5.0f, 4.8f, (short) 5, 0, 12000, 720));
        revisoes.save(new FlashcardRevisao(card, usuario, sessao, "FACIL",
                (short) 5, 2.1f, 3.5f, 4.8f, 4.5f, (short) 12, 0, 8000, 480));

        card.setStatus(StatusFlashcard.EXCLUIDO);
        card.getRegistroTemporal().setExcluidoEm(Instant.now());
        flashcards.saveAndFlush(card);

        var cardExcluido = flashcards.findById(card.getId()).orElseThrow();
        assertThat(cardExcluido.getStatus()).isEqualTo(StatusFlashcard.EXCLUIDO);

        var log = revisoes.findByFlashcardIdOrderByRevisadoEmDesc(card.getId());
        assertThat(log).hasSize(2);
        assertThat(log).extracting(FlashcardRevisao::getNota).containsExactlyInAnyOrder("BOM", "FACIL");
    }

    private UUID criarUsuario() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Teste', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                id, id + "@teste.local");
        return id;
    }

    private UUID criarDeck(UUID usuarioId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into deck (id, usuario_id, nome) values (?, ?, 'Deck de teste')", id, usuarioId);
        return id;
    }
}
