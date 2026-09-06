package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.PaginaResposta;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.EmailEmUsoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.usuario.dto.UsuarioGestaoResumo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(UsuarioAdminController.class)
@AutoConfigureMockMvc
@Import({ManipuladorErrosGlobal.class, UsuarioAdminControllerTest.SegurancaMetodoTeste.class})
class UsuarioAdminControllerTest {
    @Autowired MockMvcTester mvc;
    @MockitoBean GestaoUsuariosService gestao;

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
    void TI03_getComoEstudanteRecebe403() {
        assertThat(mvc.get().uri("/api/v1/super/usuarios").with(user("estudante").roles("ESTUDANTE")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI03_postComoProfessorRecebe403() {
        var corpo = """
                {"nome":"Marina Duarte","email":"marina@example.com","perfil":"PROFESSOR","status":"ATIVO","enviarConvite":true}
                """;
        assertThat(mvc.post().uri("/api/v1/super/usuarios").with(user("prof").roles("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI03_deleteComoAdminRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.delete().uri("/api/v1/super/usuarios/" + id).with(user("admin").roles("ADMIN")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI03_patchComoEstudanteRecebe403() {
        var id = UUID.randomUUID();
        var corpo = """
                {"nome":"Marina","email":"marina@example.com","perfil":"ADMIN","status":"ATIVO"}
                """;
        assertThat(mvc.patch().uri("/api/v1/super/usuarios/" + id).with(user("estudante").roles("ESTUDANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI03_semTokenRecebe401() {
        assertThat(mvc.get().uri("/api/v1/super/usuarios"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void TI03_getComoSuperAdminRetorna200() {
        var resumo = new UsuarioGestaoResumo(UUID.randomUUID(), "Super", "super@example.com",
                PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO, Instant.now());
        given(gestao.listar(any(), any())).willReturn(new PaginaResposta<>(List.of(resumo), 0, 20, 1));
        assertThat(mvc.get().uri("/api/v1/super/usuarios?page=0&size=20&sort=criadoEm,desc")
                        .with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN")))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void TI07_conflitoAutoModificacaoRetorna409ComCodeEstavel() {
        var id = UUID.randomUUID();
        given(gestao.editar(eq(id), any(), any())).willThrow(new ConflitoException("AUTO_MODIFICACAO", "Você não pode rebaixar ou desativar o próprio acesso."));
        var corpo = """
                {"nome":"Marina","email":"marina@example.com","perfil":"ADMIN","status":"ATIVO"}
                """;
        assertThat(mvc.patch().uri("/api/v1/super/usuarios/" + id).with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("AUTO_MODIFICACAO");
    }

    @Test
    void TI07_conflitoUltimoSuperAdminRetorna409ComCodeEstavel() {
        var id = UUID.randomUUID();
        willThrow(new ConflitoException("ULTIMO_SUPER_ADMIN", "O último super-admin ativo não pode ser rebaixado ou desativado.")).given(gestao).excluir(eq(id), any());
        assertThat(mvc.delete().uri("/api/v1/super/usuarios/" + id).with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("ULTIMO_SUPER_ADMIN");
    }

    @Test
    void TI07_emailEmUsoRetorna409ComFieldErrors() {
        var corpoCriacao = """
                {"nome":"Marina Duarte","email":"duplicado@example.com","perfil":"PROFESSOR","status":"ATIVO","enviarConvite":true}
                """;
        given(gestao.criar(any())).willThrow(new EmailEmUsoException());
        assertThat(mvc.post().uri("/api/v1/super/usuarios").with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("EMAIL_EM_USO");
        assertThat(mvc.post().uri("/api/v1/super/usuarios").with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao))
                .bodyJson().extractingPath("$.fieldErrors.email").isEqualTo("Este e-mail já está em uso.");
    }

    @Test
    void TI07_recursoNaoEncontradoRetorna404ComCodeEstavel() {
        var id = UUID.randomUUID();
        given(gestao.editar(eq(id), any(), any())).willThrow(new RecursoNaoEncontradoException());
        var corpo = """
                {"nome":"Marina","email":"marina@example.com","perfil":"ADMIN","status":"ATIVO"}
                """;
        assertThat(mvc.patch().uri("/api/v1/super/usuarios/" + id).with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void TI07_sortInvalidoRetorna422ComFieldErrors() {
        given(gestao.listar(any(), any())).willThrow(new DadosInvalidosException(Map.of("sort", "A ordenação informada não é válida.")));
        assertThat(mvc.get().uri("/api/v1/super/usuarios?sort=invalido,desc").with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN")))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("DADOS_INVALIDOS");
    }

    @Test
    void TI07_validacaoBeanRetorna422ComFieldErrors() {
        var corpoInvalido = """
                {"nome":"","email":"nao-email","perfil":null,"status":null}
                """;
        assertThat(mvc.post().uri("/api/v1/super/usuarios").with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoInvalido))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("DADOS_INVALIDOS");
    }

    @Test
    void TI07_envelopeProblemJsonEmErroDeNegocio() {
        var id = UUID.randomUUID();
        given(gestao.editar(eq(id), any(), any())).willThrow(new ConflitoException("AUTO_MODIFICACAO", "Você não pode rebaixar ou desativar o próprio acesso."));
        var corpo = """
                {"nome":"Marina","email":"marina@example.com","perfil":"ADMIN","status":"ATIVO"}
                """;
        assertThat(mvc.patch().uri("/api/v1/super/usuarios/" + id).with(user(UUID.randomUUID().toString()).roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(HttpStatus.CONFLICT)
                .headers().satisfies(h -> assertThat(h.getFirst("content-type")).contains("application/problem+json"));
    }
}
