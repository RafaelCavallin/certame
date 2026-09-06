package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certame.flashcard.deck.DeckRepository;
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
class FlashcardEsquemaIntegracaoTest {

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
    private DeckRepository decks;

    @Test
    @DisplayName("TI-01 aplica V13 em base limpa com as tres tabelas, checks e indices")
    void TI01_migracaoAplicaTresTabelasComIndicesEChecks() {
        var tabelas = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public' and table_name in ('deck','flashcard','flashcard_revisao')",
                String.class);
        assertThat(tabelas).containsExactlyInAnyOrder("deck", "flashcard", "flashcard_revisao");

        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'deck_nome_uk'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'flashcard_lista_idx'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'flashcard_fila_idx'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'flashcard_busca_idx'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'flashcard_deck_idx'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'flashcard_origem_idx'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'flashcard_revisao_idx'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname = 'flashcard_revisao_card_idx'", Integer.class)).isEqualTo(1);

        var checks = jdbc.queryForList(
                "select conname from pg_constraint where conname in ('flashcard_sessao_exige_questao','flashcard_excluido_coerente','flashcard_arquivado_coerente')",
                String.class);
        assertThat(checks).containsExactlyInAnyOrder("flashcard_sessao_exige_questao", "flashcard_excluido_coerente", "flashcard_arquivado_coerente");

        var colunaBusca = jdbc.queryForObject(
                "select is_generated from information_schema.columns where table_name = 'flashcard' and column_name = 'busca'",
                String.class);
        assertThat(colunaBusca).isEqualTo("ALWAYS");

        var onDelete = jdbc.queryForObject(
                "select delete_rule from information_schema.referential_constraints where constraint_name = (select conname from pg_constraint where conname like 'flashcard_revisao_flashcard_id_fkey' limit 1)",
                String.class);
        // flashcard_revisao nao tem ON DELETE CASCADE
        if (onDelete != null) {
            assertThat(onDelete).isNotEqualTo("CASCADE");
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("TI-02 check flashcard_excluido_coerente e flashcard_arquivado_coerente")
    void TI02_checksDeStatusCoerenteRecusamDivergencia() {
        var usuario = criarUsuario();

        // EXCLUIDO sem excluido_em deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard (usuario_id, frente, verso, status) values (?, 'Frente valida', 'Verso valido', 'EXCLUIDO')",
                usuario)).isInstanceOf(Exception.class);

        // ATIVO com excluido_em preenchido deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard (usuario_id, frente, verso, status, excluido_em) values (?, 'Frente valida', 'Verso valido', 'ATIVO', now())",
                usuario)).isInstanceOf(Exception.class);

        // ARQUIVADO sem arquivado_em deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard (usuario_id, frente, verso, status) values (?, 'Frente valida', 'Verso valido', 'ARQUIVADO')",
                usuario)).isInstanceOf(Exception.class);

        // ATIVO com arquivado_em preenchido deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard (usuario_id, frente, verso, status, arquivado_em) values (?, 'Frente valida', 'Verso valido', 'ATIVO', now())",
                usuario)).isInstanceOf(Exception.class);

        // casos validos devem passar
        var idAtivo = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso, status) values (?, ?, 'Frente valida', 'Verso valido', 'ATIVO')",
                idAtivo, usuario);
        assertThat(jdbc.queryForObject("select count(*) from flashcard where id = ?", Integer.class, idAtivo)).isEqualTo(1);

        var idExcluido = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso, status, excluido_em) values (?, ?, 'Frente valida', 'Verso valido', 'EXCLUIDO', now())",
                idExcluido, usuario);
        assertThat(jdbc.queryForObject("select count(*) from flashcard where id = ?", Integer.class, idExcluido)).isEqualTo(1);

        var idArquivado = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso, status, arquivado_em) values (?, ?, 'Frente valida', 'Verso valido', 'ARQUIVADO', now())",
                idArquivado, usuario);
        assertThat(jdbc.queryForObject("select count(*) from flashcard where id = ?", Integer.class, idArquivado)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("TI-03 check flashcard_sessao_exige_questao")
    void TI03_sessaoSemQuestaoEhRecusada() {
        var usuario = criarUsuario();
        var sessao = criarSessao(usuario);
        var questao = criarQuestao(usuario);

        // sessao sem questao deve falhar
        assertThatThrownBy(() -> jdbc.update(
                "insert into flashcard (usuario_id, frente, verso, sessao_origem_id) values (?, 'Frente ok', 'Verso ok', ?)",
                usuario, sessao)).isInstanceOf(Exception.class);

        // sessao com questao deve passar
        var idOk = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso, questao_origem_id, sessao_origem_id) values (?, ?, 'Frente ok', 'Verso ok', ?, ?)",
                idOk, usuario, questao, sessao);
        assertThat(jdbc.queryForObject("select count(*) from flashcard where id = ?", Integer.class, idOk)).isEqualTo(1);

        // sem ambos deve passar (avulso)
        var idAvulso = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso) values (?, ?, 'Frente avulsa', 'Verso avulso')",
                idAvulso, usuario);
        assertThat(jdbc.queryForObject("select count(*) from flashcard where id = ?", Integer.class, idAvulso)).isEqualTo(1);

        // so questao sem sessao deve passar
        var idSoQuestao = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso, questao_origem_id) values (?, ?, 'Frente q', 'Verso q', ?)",
                idSoQuestao, usuario, questao);
        assertThat(jdbc.queryForObject("select count(*) from flashcard where id = ?", Integer.class, idSoQuestao)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("TI-04 indice unico de deck por usuario ignorando maiusculas")
    void TI04_deckNomeDuplicadoIgnorandoCaixaEhRecusado() {
        var usuarioA = criarUsuario();
        var usuarioB = criarUsuario();

        jdbc.update("insert into deck (usuario_id, nome) values (?, 'Constitucional')", usuarioA);

        // mesmo usuario, caixa diferente deve falhar
        assertThatThrownBy(() -> jdbc.update("insert into deck (usuario_id, nome) values (?, 'constitucional')", usuarioA))
                .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> jdbc.update("insert into deck (usuario_id, nome) values (?, 'CONSTITUCIONAL')", usuarioA))
                .isInstanceOf(Exception.class);

        // usuario diferente deve permitir mesmo nome
        jdbc.update("insert into deck (usuario_id, nome) values (?, 'Constitucional')", usuarioB);
        assertThat(jdbc.queryForObject("select count(*) from deck where usuario_id = ?", Integer.class, usuarioB)).isEqualTo(1);

        // repositorio tambem deve detectar
        assertThat(decks.existsByUsuarioIdAndNomeIgnoreCase(usuarioA, "constitucional")).isTrue();
        assertThat(decks.existsByUsuarioIdAndNomeIgnoreCase(usuarioA, "CONSTITUCIONAL")).isTrue();
        assertThat(decks.existsByUsuarioIdAndNomeIgnoreCase(usuarioB, "constitucional")).isTrue();
        assertThat(decks.existsByUsuarioIdAndNomeIgnoreCase(usuarioA, "Administrativo")).isFalse();
    }

    @Test
    @DisplayName("TI-05 coluna gerada busca e indice GIN com websearch_to_tsquery")
    void TI05_buscaGeradaEGinFunciona() {
        var usuario = criarUsuario();

        var id1 = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso) values (?, ?, 'Direito Constitucional', 'Controle concentrado e ADI')",
                id1, usuario);
        var id2 = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso) values (?, ?, 'Processo Civil', 'Recurso extraordinario')",
                id2, usuario);

        var busca = jdbc.queryForObject("select busca from flashcard where id = ?", String.class, id1);
        assertThat(busca).isNotNull();

        var viaJdbc = jdbc.queryForList(
                "select id from flashcard where usuario_id = ? and busca @@ websearch_to_tsquery('portuguese', 'constitucional')",
                UUID.class, usuario);
        assertThat(viaJdbc).contains(id1);
        assertThat(viaJdbc).doesNotContain(id2);

        // via repositorio nativo
        var viaRepo = flashcards.buscarPorTermo(usuario, "constitucional");
        assertThat(viaRepo).extracting(Flashcard::getId).contains(id1);

        var vazio = flashcards.buscarPorTermo(usuario, "xyzinexistente");
        assertThat(vazio).isEmpty();

        // indice GIN existe
        assertThat(jdbc.queryForObject(
                "select indexdef from pg_indexes where indexname = 'flashcard_busca_idx'", String.class))
                .contains("gin");

        // deck_id on delete set null tambem exercitado aqui via TI indireta
        var deckId = UUID.randomUUID();
        jdbc.update("insert into deck (id, usuario_id, nome) values (?, ?, 'Deck GIN')", deckId, usuario);
        var cardComDeck = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, deck_id, frente, verso) values (?, ?, ?, 'Frente deck', 'Verso deck')",
                cardComDeck, usuario, deckId);
        jdbc.update("delete from deck where id = ?", deckId);
        assertThat(jdbc.queryForObject("select deck_id from flashcard where id = ?", UUID.class, cardComDeck)).isNull();
    }

    private UUID criarUsuario() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Teste', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                id, id + "@teste.local");
        return id;
    }

    private UUID criarSessao(UUID usuarioId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo, tamanho) values (?, ?, 'FOCO', 10)", id, usuarioId);
        return id;
    }

    private UUID criarQuestao(UUID usuarioId) {
        var disciplina = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", disciplina, "Disc-" + disciplina.toString().substring(0, 8));
        var questao = UUID.randomUUID();
        jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'ME', 'Enunciado teste', ?, 'A', 'Coment', 'PUBLICADA', ?)",
                questao, disciplina, usuarioId);
        return questao;
    }
}
