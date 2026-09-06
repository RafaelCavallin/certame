package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.excecao.StatusIncompativelException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AcessoUsuarioAdminController.class)
@AutoConfigureMockMvc
@Import({ManipuladorErrosGlobal.class, AcessoUsuarioAdminControllerTest.SegurancaMetodoTeste.class})
class AcessoUsuarioAdminControllerTest {
    @Autowired MockMvcTester mvc;
    @MockitoBean AcessoUsuarioAdminService acesso;

    @EnableMethodSecurity
    @TestConfiguration
    static class SegurancaMetodoTeste {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                        res.setStatus(401);
                        res.setContentType("application/problem+json");
                    }))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    @Test
    void TI03_redefinirSenhaComoProfessorRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.post().uri("/api/v1/super/usuarios/" + id + "/redefinir-senha").with(user("prof").roles("PROFESSOR")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI03_reenviarConviteComoAdminRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.post().uri("/api/v1/super/usuarios/" + id + "/reenviar-convite").with(user("admin").roles("ADMIN")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI07_statusIncompativelRetorna409ComCodeEstavel() {
        var id = UUID.randomUUID();
        willThrow(new StatusIncompativelException("Só é possível redefinir a senha de um usuário ativo.")).given(acesso).redefinirSenha(eq(id));
        assertThat(mvc.post().uri("/api/v1/super/usuarios/" + id + "/redefinir-senha").with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("STATUS_INCOMPATIVEL");
        willThrow(new StatusIncompativelException("Só é possível reenviar convite a um usuário com convite pendente.")).given(acesso).reenviarConvite(eq(id));
        assertThat(mvc.post().uri("/api/v1/super/usuarios/" + id + "/reenviar-convite").with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("STATUS_INCOMPATIVEL");
    }
}
