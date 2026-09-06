package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Privacidade por construção (RN-07) — TI-16…TI-20, TI-26: card de terceiro responde 404 idêntico ao
 * de id inexistente na leitura, edição e exclusão, inclusive para super-admin (nenhuma rota
 * administrativa existe para flashcard).
 */
@SpringBootTest(properties = "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois")
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers
class FlashcardPrivacidadeIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private ConsultaFilaFlashcards fila;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void TI16_leituraDeCardDeOutroUsuarioE404IdenticoAoInexistente() throws Exception {
        var dono = usuario();
        var estranho = usuario();
        var cardId = cartaoAtivo(dono);

        var corpoDeTerceiro = mvc.perform(get("/api/v1/flashcards/" + cardId).with(estudante(estranho)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        var corpoInexistente = mvc.perform(get("/api/v1/flashcards/" + UUID.randomUUID()).with(estudante(estranho)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        assertThat(corpoDeTerceiro).isEqualTo(corpoInexistente);
    }

    @Test
    void TI17_edicaoDeCardDeOutroUsuarioE404() throws Exception {
        var dono = usuario();
        var estranho = usuario();
        var cardId = cartaoAtivo(dono);

        mvc.perform(put("/api/v1/flashcards/" + cardId).with(estudante(estranho)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frente\":\"Nova frente\",\"verso\":\"Novo verso\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
        assertThat(jdbc.queryForObject("select frente from flashcard where id = ?", String.class, cardId)).isNotEqualTo("Nova frente");
    }

    @Test
    void TI18_exclusaoDeCardDeOutroUsuarioE404() throws Exception {
        var dono = usuario();
        var estranho = usuario();
        var cardId = cartaoAtivo(dono);

        mvc.perform(delete("/api/v1/flashcards/" + cardId).with(estudante(estranho)))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("select status from flashcard where id = ?", String.class, cardId)).isEqualTo("ATIVO");
    }

    @Test
    void TI19_arquivarRetiraCardDaFilaMasMantemNaListagemDeArquivados() throws Exception {
        var usuario = usuario();
        var cardId = cartaoAtivo(usuario);

        mvc.perform(post("/api/v1/flashcards/" + cardId + "/arquivar").with(estudante(usuario)))
                .andExpect(status().isNoContent());

        autenticar(usuario);
        assertThat(fila.elegiveis(usuario, LocalDate.now().plusDays(1))).extracting(Flashcard::getId).doesNotContain(cardId);
        mvc.perform(get("/api/v1/flashcards").param("arquivados", "true").with(estudante(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(cardId.toString()));
    }

    @Test
    void TI20_excluirCardComDuasRevisoesRegistradasPreservaHistorico() throws Exception {
        var usuario = usuario();
        var cardId = cartaoAtivo(usuario);
        revisao(cardId, usuario);
        revisao(cardId, usuario);

        mvc.perform(delete("/api/v1/flashcards/" + cardId).with(estudante(usuario)))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select status from flashcard where id = ?", String.class, cardId)).isEqualTo("EXCLUIDO");
        assertThat(jdbc.queryForObject("select count(*) from flashcard_revisao where flashcard_id = ?", Integer.class, cardId)).isEqualTo(2);
        autenticar(usuario);
        assertThat(fila.elegiveis(usuario, LocalDate.now().plusYears(1))).extracting(Flashcard::getId).doesNotContain(cardId);
    }

    @Test
    void TI26_superAdminRecebe404AoLerEditarEExcluirCardDeEstudante() throws Exception {
        var estudante = usuario();
        var superAdmin = superAdmin();
        var cardId = cartaoAtivo(estudante);

        mvc.perform(get("/api/v1/flashcards/" + cardId).with(user(superAdmin.toString()).roles("SUPER_ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/flashcards/" + cardId).with(user(superAdmin.toString()).roles("SUPER_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frente\":\"Tentativa\",\"verso\":\"De alteração\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/flashcards/" + cardId).with(user(superAdmin.toString()).roles("SUPER_ADMIN")))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("select status from flashcard where id = ?", String.class, cardId)).isEqualTo("ATIVO");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor estudante(UUID usuarioId) {
        return user(usuarioId.toString()).roles("ESTUDANTE");
    }

    private void autenticar(UUID usuarioId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuarioId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))));
    }

    private UUID usuario() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Estudante', ?, 'hash', 'ESTUDANTE', 'ATIVO')",
                id, id + "@flashcard-privacidade.local");
        return id;
    }

    private UUID superAdmin() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Super admin', ?, 'hash', 'SUPER_ADMIN', 'ATIVO')",
                id, id + "@flashcard-privacidade.local");
        return id;
    }

    private UUID cartaoAtivo(UUID usuarioId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into flashcard (id, usuario_id, frente, verso) values (?, ?, ?, ?)", id, usuarioId, "Frente " + id, "Verso " + id);
        return id;
    }

    private void revisao(UUID cardId, UUID usuarioId) {
        jdbc.update("insert into flashcard_revisao (flashcard_id, usuario_id, nota, dias_decorridos, estabilidade_ant,"
                        + " estabilidade_nova, dificuldade_ant, dificuldade_nova, intervalo_novo, tempo_ms)"
                        + " values (?, ?, 'BOM', 1, 0, 1, 5, 5, 1, 3000)",
                cardId, usuarioId);
    }
}
