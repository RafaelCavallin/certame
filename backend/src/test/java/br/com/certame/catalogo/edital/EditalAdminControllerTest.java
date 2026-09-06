package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.com.certame.catalogo.edital.dto.EditalAdminResumo;
import br.com.certame.catalogo.edital.dto.EditalDetalhe;
import br.com.certame.catalogo.edital.dto.ItemCatalogoResumo;
import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EditalAdminController.class)
@AutoConfigureMockMvc
@Import({ManipuladorErrosGlobal.class, EditalAdminControllerTest.SegurancaMetodoTeste.class})
class EditalAdminControllerTest {
    private static final String CORPO_VALIDO = """
            {
              "nome": "SEFAZ-RS",
              "status": "ABERTO",
              "vagas": 90,
              "bancaId": null,
              "orgaoId": null,
              "prazoInscricao": "2026-10-15",
              "urlOficial": null,
              "disciplinaIds": []
            }
            """;

    @Autowired MockMvcTester mvc;
    @MockitoBean EditalService editalService;
    @MockitoBean ConsultaEditaisService consulta;

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
    void TI12_postComoEstudanteRecebe403() {
        assertThat(mvc.post().uri("/api/v1/admin/editais")
                        .with(user("estudante").roles("ESTUDANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI13_putComoProfessorRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.put().uri("/api/v1/admin/editais/" + id)
                        .with(user("professor").roles("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI14_deleteComoEstudanteRecebe403() {
        var id = UUID.randomUUID();
        assertThat(mvc.delete().uri("/api/v1/admin/editais/" + id).with(user("estudante").roles("ESTUDANTE")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void TI17_postComDisciplinaInexistenteRecebe422ComFieldErrors() {
        given(editalService.criar(any())).willThrow(
                new DadosInvalidosException(Map.of("disciplinaIds", "Uma das disciplinas selecionadas não existe mais na árvore de matérias.")));

        assertThat(mvc.post().uri("/api/v1/admin/editais")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.fieldErrors.disciplinaIds")
                .isNotNull();
    }

    @Test
    void TI18_postComNomeDuplicadoRecebe409() {
        given(editalService.criar(any())).willThrow(new ConflitoException("NOME_EM_USO", "Já existe um edital com este nome."));

        assertThat(mvc.post().uri("/api/v1/admin/editais")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("NOME_EM_USO");
    }

    @Test
    void TI19_listaAdministrativaComUmAbertoDePrazoVencido() {
        var banca = new ItemCatalogoResumo(UUID.randomUUID(), "Cebraspe", null);
        var vencido = new EditalAdminResumo(UUID.randomUUID(), "TRF1", StatusEdital.ABERTO, 90,
                LocalDate.of(2026, 8, 1), banca, null, 3, true, null);
        var emDia = new EditalAdminResumo(UUID.randomUUID(), "TCU", StatusEdital.ABERTO, 10,
                LocalDate.of(2026, 12, 1), banca, null, 1, false, null);
        given(consulta.listarParaAdministracao()).willReturn(List.of(vencido, emDia));

        assertThat(mvc.get().uri("/api/v1/admin/editais").with(user("admin").roles("ADMIN")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .convertTo(EditalAdminResumo[].class)
                .satisfies(lista -> assertThat(lista).extracting("nome", "prazoVencido")
                        .containsExactly(tuple("TRF1", true), tuple("TCU", false)));
    }

    @Test
    void TI20_buscarEditalExcluidoRecebe404() {
        var id = UUID.randomUUID();
        given(consulta.buscarParaEdicao(eq(id))).willThrow(new RecursoNaoEncontradoException("EDITAL_NAO_ENCONTRADO"));

        assertThat(mvc.get().uri("/api/v1/admin/editais/" + id).with(user("admin").roles("ADMIN")))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EDITAL_NAO_ENCONTRADO");
    }
}
