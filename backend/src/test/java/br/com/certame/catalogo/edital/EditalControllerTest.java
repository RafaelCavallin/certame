package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.com.certame.catalogo.edital.dto.DisciplinaExigida;
import br.com.certame.catalogo.edital.dto.EditaisAgrupados;
import br.com.certame.catalogo.edital.dto.EditalResumo;
import br.com.certame.catalogo.edital.dto.ItemCatalogoResumo;
import br.com.certame.comum.ManipuladorErrosGlobal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EditalController.class)
@AutoConfigureMockMvc
@Import(ManipuladorErrosGlobal.class)
class EditalControllerTest {
    @Autowired MockMvcTester mvc;
    @MockitoBean ConsultaEditaisService consulta;

    private EditalResumo editalCompleto() {
        var banca = new ItemCatalogoResumo(UUID.randomUUID(), "Cebraspe", null);
        var orgao = new ItemCatalogoResumo(UUID.randomUUID(), "TRF1", null);
        var disciplinas = List.of(new DisciplinaExigida(UUID.randomUUID(), "Direito Constitucional"));
        return new EditalResumo(UUID.randomUUID(), "TRF1", StatusEdital.ABERTO, 90,
                LocalDate.of(2026, 10, 1), "https://trf1.jus.br", banca, orgao, disciplinas);
    }

    @Test
    void TI05_editaisNosTresStatusRetorna200ComOsTresGrupos() {
        given(consulta.agrupados()).willReturn(
                new EditaisAgrupados(List.of(editalCompleto()), List.of(), List.of()));

        assertThat(mvc.get().uri("/api/v1/editais").with(user("estudante")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .convertTo(EditaisAgrupados.class)
                .satisfies(corpo -> {
                    assertThat(corpo.abertos()).hasSize(1);
                    assertThat(corpo.previstos()).isEmpty();
                    assertThat(corpo.encerrados()).isEmpty();
                });
    }

    @Test
    void TI06_editaisCarregaDisciplinasBancaEOrgaoNoPayload() {
        given(consulta.agrupados()).willReturn(
                new EditaisAgrupados(List.of(editalCompleto()), List.of(), List.of()));

        assertThat(mvc.get().uri("/api/v1/editais").with(user("estudante")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .convertTo(EditaisAgrupados.class)
                .satisfies(corpo -> {
                    var edital = corpo.abertos().getFirst();
                    assertThat(edital.disciplinas()).extracting("nome").containsExactly("Direito Constitucional");
                    assertThat(edital.banca().nome()).isEqualTo("Cebraspe");
                    assertThat(edital.orgao().nome()).isEqualTo("TRF1");
                });
    }

    @Test
    void TI07_editaisDevolveVagasPrazoEBancaNoPayload() {
        given(consulta.agrupados()).willReturn(
                new EditaisAgrupados(List.of(editalCompleto()), List.of(), List.of()));

        assertThat(mvc.get().uri("/api/v1/editais").with(user("estudante")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .convertTo(EditaisAgrupados.class)
                .satisfies(corpo -> {
                    var edital = corpo.abertos().getFirst();
                    assertThat(edital.vagas()).isEqualTo(90);
                    assertThat(edital.prazoInscricao()).isEqualTo(LocalDate.of(2026, 10, 1));
                    assertThat(edital.banca().nome()).isEqualTo("Cebraspe");
                });
    }

    @Test
    void TI08_baseSemEditaisRetorna200ComOsTresGruposVazios() {
        given(consulta.agrupados()).willReturn(new EditaisAgrupados(List.of(), List.of(), List.of()));

        assertThat(mvc.get().uri("/api/v1/editais").with(user("estudante")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .convertTo(EditaisAgrupados.class)
                .satisfies(corpo -> {
                    assertThat(corpo.abertos()).isEmpty();
                    assertThat(corpo.previstos()).isEmpty();
                    assertThat(corpo.encerrados()).isEmpty();
                });
    }

    @Test
    void TI09_semTokenRetorna401() {
        assertThat(mvc.get().uri("/api/v1/editais")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void TI10_respondeCacheControlNoStore() {
        given(consulta.agrupados()).willReturn(new EditaisAgrupados(List.of(), List.of(), List.of()));

        assertThat(mvc.get().uri("/api/v1/editais").with(user("estudante")))
                .hasStatus(HttpStatus.OK)
                .hasHeader("Cache-Control", "no-store");
    }
}
