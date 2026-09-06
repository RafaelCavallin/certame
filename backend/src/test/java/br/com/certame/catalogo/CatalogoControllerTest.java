package br.com.certame.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
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

/**
 * Slice web com o {@link CatalogoService} real (não mockado) para que o proxy do
 * {@code @PreAuthorize} intercepte de fato o CRUD de bancas/órgãos — os testes de serviço
 * instanciam a classe diretamente e não passam pelo AOP de segurança do Spring.
 */
@WebMvcTest(CatalogoController.class)
@AutoConfigureMockMvc
@Import({ManipuladorErrosGlobal.class, CatalogoService.class, CatalogoControllerTest.SegurancaMetodoTeste.class})
class CatalogoControllerTest {
    private static final String CORPO_VALIDO = """
            {"nome": "Cebraspe", "sigla": "CBSP"}
            """;

    @Autowired MockMvcTester mvc;
    @MockitoBean TaxonomiaService taxonomiaService;
    @MockitoBean BancaRepository bancaRepository;
    @MockitoBean OrgaoRepository orgaoRepository;
    @MockitoBean RegistradorAuditoria auditoria;

    @EnableMethodSecurity
    @TestConfiguration
    static class SegurancaMetodoTeste {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    @Test
    void TI23_listarBancasComoEstudanteRecebe200() {
        assertThat(mvc.get().uri("/api/v1/admin/bancas").with(user("estudante").roles("ESTUDANTE")))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void TI23_criarBancaComoProfessorRecebe403() {
        assertThat(mvc.post().uri("/api/v1/admin/bancas").with(user("professor").roles("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI23_renomearBancaComoProfessorRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.put().uri("/api/v1/admin/bancas/" + id).with(user("professor").roles("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI23_excluirBancaComoProfessorRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.delete().uri("/api/v1/admin/bancas/" + id).with(user("professor").roles("PROFESSOR")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI24_listarOrgaosComoEstudanteRecebe403() {
        assertThat(mvc.get().uri("/api/v1/admin/orgaos").with(user("estudante").roles("ESTUDANTE")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI24_criarOrgaoComoEstudanteRecebe403() {
        assertThat(mvc.post().uri("/api/v1/admin/orgaos").with(user("estudante").roles("ESTUDANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI24_renomearOrgaoComoEstudanteRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.put().uri("/api/v1/admin/orgaos/" + id).with(user("estudante").roles("ESTUDANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI24_excluirOrgaoComoEstudanteRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.delete().uri("/api/v1/admin/orgaos/" + id).with(user("estudante").roles("ESTUDANTE")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }
}
