package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(properties = {
        "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class EditalIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void autenticarAdmin() {
        var usuario = usuarios.save(new Usuario("Admin " + UUID.randomUUID(), UUID.randomUUID() + "@teste.local",
                "hash", PerfilUsuario.ADMIN, StatusUsuario.ATIVO));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario.getId().toString(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private UUID novaDisciplina(String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, nome);
        return id;
    }

    private UUID novoEditalAberto(String nome, UUID... disciplinas) {
        var id = UUID.randomUUID();
        jdbc.update("insert into edital (id, nome, status, prazo_inscricao) values (?, ?, 'ABERTO', current_date + 30)", id, nome);
        for (var disciplina : disciplinas) {
            jdbc.update("insert into edital_disciplina (edital_id, disciplina_id) values (?, ?)", id, disciplina);
        }
        return id;
    }

    private String requisicaoValida(String nome, String status, String prazo) {
        return """
                {"nome":"%s","status":"%s","vagas":90,"bancaId":null,"orgaoId":null,
                "prazoInscricao":%s,"urlOficial":null,"disciplinaIds":[]}
                """.formatted(nome, status, prazo == null ? "null" : "\"" + prazo + "\"");
    }

    @Test
    void TI06_carregaDisciplinasBancaEOrgaoNumaConsultaSemNMaisUm() throws Exception {
        for (var i = 0; i < 3; i++) {
            novoEditalAberto("Concurso " + UUID.randomUUID(),
                    novaDisciplina("Disciplina A " + i), novaDisciplina("Disciplina B " + i));
        }
        var estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estatisticas.clear();

        mvc.perform(get("/api/v1/editais").with(user("estudante"))).andExpect(status().isOk());

        assertThat(estatisticas.getPrepareStatementCount()).isLessThanOrEqualTo(5);
    }

    @Test
    void TI11_postComoAdminAparecemNoGetEditaisSemEtapaDePublicacao() throws Exception {
        var nome = "Concurso " + UUID.randomUUID();

        mvc.perform(post("/api/v1/admin/editais").contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida(nome, "ABERTO", "2026-10-15")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        mvc.perform(get("/api/v1/editais").with(user("estudante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertos[?(@.nome=='" + nome + "')]").exists());
    }

    @Test
    void TI15_mudarStatusMigraEditalDeAgrupamento() throws Exception {
        var id = novoPrevisto("Concurso " + UUID.randomUUID());
        var nome = nomeDe(id);

        mvc.perform(put("/api/v1/admin/editais/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida(nome, "ABERTO", "2026-10-15")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/editais").with(user("estudante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertos[?(@.nome=='" + nome + "')]").exists())
                .andExpect(jsonPath("$.previstos[?(@.nome=='" + nome + "')]").doesNotExist());
    }

    @Test
    void TI16_excluirSomeDoGetEditais() throws Exception {
        var id = novoEditalAberto("Concurso " + UUID.randomUUID());
        var nome = nomeDe(id);

        mvc.perform(delete("/api/v1/admin/editais/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/editais").with(user("estudante")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(nome))));
    }

    @Test
    void TI21_registraAuditoriaDasTresAcoesAdministrativas() throws Exception {
        var nome = "Concurso " + UUID.randomUUID();

        var criacao = mvc.perform(post("/api/v1/admin/editais").contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida(nome, "PREVISTO", null)))
                .andExpect(status().isCreated()).andReturn();
        var id = extrairId(criacao.getResponse().getContentAsString());

        mvc.perform(put("/api/v1/admin/editais/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida(nome, "ABERTO", "2026-11-01")))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/editais/" + id)).andExpect(status().isNoContent());

        var acoes = jdbc.queryForList(
                "select acao from auditoria where entidade = 'edital' and entidade_id = ? order by criado_em",
                String.class, id);
        assertThat(acoes).containsExactly("CRIAR", "ATUALIZAR", "EXCLUIR");
    }

    private UUID novoPrevisto(String nome) {
        var id = UUID.randomUUID();
        jdbc.update("insert into edital (id, nome, status) values (?, ?, 'PREVISTO')", id, nome);
        return id;
    }

    private String nomeDe(UUID id) {
        return jdbc.queryForObject("select nome from edital where id = ?", String.class, id);
    }

    private UUID extrairId(String corpoJson) {
        var inicio = corpoJson.indexOf("\"id\":\"") + 6;
        return UUID.fromString(corpoJson.substring(inicio, corpoJson.indexOf('"', inicio)));
    }
}
