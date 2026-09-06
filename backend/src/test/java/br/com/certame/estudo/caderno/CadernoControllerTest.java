package br.com.certame.estudo.caderno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.excecao.NomeCadernoEmUsoException;
import br.com.certame.comum.excecao.QuestaoNaoPublicadaException;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CadernoController.class)
@AutoConfigureMockMvc
@Import({ManipuladorErrosGlobal.class, CadernoControllerTest.SegurancaMetodoTeste.class})
class CadernoControllerTest {
    @Autowired MockMvcTester mvc;
    @MockitoBean CadernoService service;
    @MockitoBean ConsultaCadernosService consulta;
    @MockitoBean ConsultaQuestoesCadernoService consultaQuestoes;

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
    void TI25_listarSemTokenRecebe401() {
        assertThat(mvc.get().uri("/api/v1/cadernos")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void TI25_criarSemTokenRecebe401() {
        assertThat(mvc.post().uri("/api/v1/cadernos")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"Direito Constitucional","descricao":null}
                                """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void TI26_criarComNomeDuplicadoRecebe409ComEnvelopePadrao() {
        given(service.criar(any(), any())).willThrow(new NomeCadernoEmUsoException());

        assertThat(mvc.post().uri("/api/v1/cadernos")
                        .with(user(UUID.randomUUID().toString()).roles("ESTUDANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"Direito Constitucional","descricao":null}
                                """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("NOME_CADERNO_EM_USO");
    }

    @Test
    void TI27_adicionarQuestaoNaoPublicadaRecebe422ComEnvelopePadrao() {
        var cadernoId = UUID.randomUUID();
        var questaoId = UUID.randomUUID();
        given(service.adicionarQuestoes(any(UUID.class), any(String.class), any()))
                .willThrow(new QuestaoNaoPublicadaException(1));

        assertThat(mvc.post().uri("/api/v1/cadernos/" + cadernoId + "/questoes")
                        .with(user(UUID.randomUUID().toString()).roles("ESTUDANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questaoIds\":[\"" + questaoId + "\"]}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("QUESTAO_NAO_PUBLICADA");
    }
}
