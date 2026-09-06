package br.com.certame.estudo.inicio;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.estudo.dto.ResumoInicio;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InicioController.class)
@AutoConfigureMockMvc
@Import(ManipuladorErrosGlobal.class)
class InicioControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ResumoDiaService resumo;

    @Test
    void TI22_inicioRetornaResumoDoDia() throws Exception {
        when(resumo.doDia(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new ResumoInicio(LocalDate.of(2026, 8, 31), 42, 68, 5460, 4530, 930, null));
        mvc.perform(get("/api/v1/estudo/inicio").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("00000000-0000-0000-0000-000000000001")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questoesResolvidas").value(42))
                .andExpect(jsonPath("$.tempoFlashcardsSegundos").value(930));
    }
}
