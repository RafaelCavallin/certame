package br.com.certame.catalogo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import java.util.UUID;
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
        "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class DisciplinaEditalIntegracaoTest {
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

    private UUID novoEditalVinculado(UUID disciplinaId) {
        var id = UUID.randomUUID();
        jdbc.update("insert into edital (id, nome, status) values (?, ?, 'PREVISTO')", id, "Concurso " + UUID.randomUUID());
        jdbc.update("insert into edital_disciplina (edital_id, disciplina_id) values (?, ?)", id, disciplinaId);
        return id;
    }

    @Test
    void TI22_excluirDisciplinaComEditalVinculadoRecusaComConflito() throws Exception {
        var disciplinaId = novaDisciplina("Direito Constitucional " + UUID.randomUUID());
        novoEditalVinculado(disciplinaId);

        mvc.perform(delete("/api/v1/admin/disciplinas/" + disciplinaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DISCIPLINA_EM_USO"));

        var totalNaArvore = jdbc.queryForObject(
                "select count(*) from disciplina where id = ? and excluido_em is null", Integer.class, disciplinaId);
        var editalIntegro = jdbc.queryForObject(
                "select count(*) from edital_disciplina where disciplina_id = ?", Integer.class, disciplinaId);
        org.assertj.core.api.Assertions.assertThat(totalNaArvore).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(editalIntegro).isEqualTo(1);
    }

    @Test
    void TI23_renomearDisciplinaVinculadaContinuaPermitidoEReflete() throws Exception {
        var disciplinaId = novaDisciplina("Direito Administrativo " + UUID.randomUUID());
        var editalId = novoEditalVinculado(disciplinaId);
        var novoNome = "Direito Administrativo Renomeado " + UUID.randomUUID();

        mvc.perform(put("/api/v1/admin/disciplinas/" + disciplinaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + novoNome + "\",\"disciplinaId\":null}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/editais").with(user("estudante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previstos[?(@.id=='" + editalId + "')].disciplinas[0].nome").value(novoNome))
                .andExpect(jsonPath("$.previstos[?(@.id=='" + editalId + "')].disciplinas[0].id").value(disciplinaId.toString()));
    }

    @Test
    void TI24_leituraDaArvoreDeDisciplinasPermitidaParaQualquerPerfilAutenticado() throws Exception {
        var estudante = usuarios.save(new Usuario("Estudante " + UUID.randomUUID(), UUID.randomUUID() + "@teste.local",
                "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                estudante.getId().toString(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))));

        mvc.perform(get("/api/v1/admin/disciplinas")).andExpect(status().isOk());
    }
}
