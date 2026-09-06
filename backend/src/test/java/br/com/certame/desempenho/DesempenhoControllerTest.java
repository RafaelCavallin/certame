package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.comum.ManipuladorErrosGlobal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(DesempenhoController.class)
@AutoConfigureMockMvc
@Import({ManipuladorErrosGlobal.class, DesempenhoControllerTest.SegurancaMetodoTeste.class})
class DesempenhoControllerTest {
    @Autowired MockMvcTester mvc;
    @MockitoBean ResumoDesempenhoService resumo;
    @MockitoBean EvolucaoSemanalService evolucao;
    @MockitoBean DisciplinaDesempenhoService disciplinas;
    @MockitoBean PontosFracosService pontosFracos;

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
    void TI28_resumoSemTokenRecebe401() {
        assertThat(mvc.get().uri("/api/v1/desempenho/resumo")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void TI28_evolucaoSemTokenRecebe401() {
        assertThat(mvc.get().uri("/api/v1/desempenho/evolucao")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void TI28_disciplinasSemTokenRecebe401() {
        assertThat(mvc.get().uri("/api/v1/desempenho/disciplinas")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void TI28_pontosFracosSemTokenRecebe401() {
        assertThat(mvc.get().uri("/api/v1/desempenho/pontos-fracos")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
