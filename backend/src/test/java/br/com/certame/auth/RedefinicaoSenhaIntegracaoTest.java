package br.com.certame.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.email.EnviadorEmail;
import br.com.certame.usuario.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RedefinicaoSenhaIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("certame.seguranca.jwt-segredo", () -> "certame-integracao-segredo-com-32-bytes");
        registry.add("certame.seguranca.cookie-secure", () -> "false");
    }

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @MockitoBean EnviadorEmail enviador;

    @BeforeEach
    void limparMensagens() { reset(enviador); }

    @Test
    void TI06_pedidoExistenteEInexistenteRetorna204() throws Exception {
        String existente = cadastrar();
        pedir(existente);
        pedir("ausente-" + UUID.randomUUID() + "@example.com");
        verify(enviador).enviarRedefinicaoSenha(any(), anyString());
    }

    @Test
    void TI07_emailMalformadoRetorna422ESemEnvio() throws Exception {
        mvc.perform(post("/api/v1/auth/senha/esqueci").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"malformado\"}"))
                .andExpect(status().isUnprocessableContent());
        org.mockito.Mockito.verifyNoInteractions(enviador);
    }

    @Test
    void TI08_cicloCompletoTrocaSenha() throws Exception {
        String email = cadastrar();
        String token = pedirEObterToken(email);
        redefinir(token, "senhaNova123");
        autenticar(email, "senhaNova123").andExpect(status().isOk());
        autenticar(email, "senhaSegura").andExpect(status().isUnauthorized());
    }

    @Test
    void TI09_reusoDoTokenRetorna400() throws Exception {
        String token = pedirEObterToken(cadastrar());
        redefinir(token, "senhaNova123");
        mvc.perform(post("/api/v1/auth/senha/redefinir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"senha\":\"outraSenha\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
    }

    @Test
    void TI10_refreshAnteriorERevogadoAposRedefinicao() throws Exception {
        String email = "refresh-" + UUID.randomUUID() + "@example.com";
        var resposta = mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa Teste\",\"email\":\"" + email + "\",\"senha\":\"senhaSegura\"}"))
                .andExpect(status().isCreated()).andReturn();
        String refresh = resposta.getResponse().getCookie("certame_rt").getValue();
        String token = pedirEObterToken(email);
        redefinir(token, "senhaNova123");
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("certame_rt", refresh)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSAO_EXPIRADA"));
    }

    private String cadastrar() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa Teste\",\"email\":\"" + email + "\",\"senha\":\"senhaSegura\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private void pedir(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/senha/esqueci").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isNoContent());
    }

    private String pedirEObterToken(String email) throws Exception {
        pedir(email);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviarRedefinicaoSenha(any(), link.capture());
        return link.getValue().substring(link.getValue().lastIndexOf('/') + 1);
    }

    private void redefinir(String token, String senha) throws Exception {
        mvc.perform(post("/api/v1/auth/senha/redefinir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isNoContent());
    }

    private org.springframework.test.web.servlet.ResultActions autenticar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }
}
