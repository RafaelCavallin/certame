package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

/**
 * Slice web com o {@link PublicacaoService} real (não mockado) para que o proxy do
 * {@code @PreAuthorize} de fato intercepte a chamada — os testes de serviço existentes
 * (`PublicacaoServiceTest`) instanciam a classe diretamente e nunca passam pelo AOP de segurança.
 */
@WebMvcTest(QuestaoAdminController.class)
@AutoConfigureMockMvc
@Import({ManipuladorErrosGlobal.class, PublicacaoService.class, ValidadorPublicacao.class, MetricasPublicacao.class,
        QuestaoAdminControllerTest.SegurancaMetodoTeste.class, QuestaoAdminControllerTest.ClockTeste.class})
class QuestaoAdminControllerTest {
    @Autowired MockMvcTester mvc;
    @MockitoBean QuestaoService questaoService;
    @MockitoBean BuscaQuestaoService buscaService;
    @MockitoBean QuestaoRepository questaoRepository;
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

    @TestConfiguration
    static class ClockTeste {
        @Bean
        Clock clock() { return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC); }
    }

    @Test
    void TI21_publicarComoProfessorRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.post().uri("/api/v1/admin/questoes/" + id + "/publicar")
                        .with(user("professor").roles("PROFESSOR")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI21_publicarComoEstudanteRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.post().uri("/api/v1/admin/questoes/" + id + "/publicar")
                        .with(user("estudante").roles("ESTUDANTE")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI22_despublicarComoProfessorRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.post().uri("/api/v1/admin/questoes/" + id + "/despublicar")
                        .with(user("professor").roles("PROFESSOR")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI22_despublicarComoEstudanteRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.post().uri("/api/v1/admin/questoes/" + id + "/despublicar")
                        .with(user("estudante").roles("ESTUDANTE")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }
}
