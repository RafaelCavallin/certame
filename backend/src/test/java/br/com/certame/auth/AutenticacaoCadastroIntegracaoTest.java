package br.com.certame.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.usuario.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AutenticacaoCadastroIntegracaoTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
    }

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;

    @Test
    void TI04_cadastraUsuarioEAbreSessao() throws Exception {
        String email = "novo-" + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa Teste\",\"email\":\"" + email
                                + "\",\"senha\":\"senhaSegura\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(jsonPath("$.usuario.perfil").value("ESTUDANTE"));

        var usuario = usuarios.findByEmailAndExcluidoEmIsNull(email).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(usuario.getStatus().name()).isEqualTo("ATIVO");
    }

    @Test
    void TI05_rejeitaEmailDuplicadoSemCriarOutroUsuario() throws Exception {
        String email = "duplicado-" + UUID.randomUUID() + "@example.com";
        String corpo = "{\"nome\":\"Pessoa Teste\",\"email\":\"" + email
                + "\",\"senha\":\"senhaSegura\"}";
        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        long quantidadeAntes = usuarios.findAll().stream().filter(usuario -> email.equals(usuario.getEmail())).count();

        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EM_USO"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Este e-mail já está em uso."));

        org.assertj.core.api.Assertions.assertThat(usuarios.findAll().stream()
                .filter(usuario -> email.equals(usuario.getEmail())).count()).isEqualTo(quantidadeAntes);
    }
}
