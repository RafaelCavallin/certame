package br.com.certame.estudo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.estudo.dto.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessaoEstudoController.class)
@AutoConfigureMockMvc
@Import(ManipuladorErrosGlobal.class)
class SessaoEstudoControllerTest {
    private static final UUID SESSAO = UUID.randomUUID();
    private static final UUID QUESTAO = UUID.randomUUID();
    @Autowired MockMvc mvc;
    @MockitoBean SessaoEstudoService sessoes;
    @MockitoBean RegistroRespostaService respostas;
    @MockitoBean MarcacaoService marcacoes;
    @MockitoBean EstudoObservabilidade observabilidade;

    @Test
    void TI07_aberturaRetorna201ComSerieSemGabarito() throws Exception {
        var retorno = new RespostaSessao(SESSAO, ModoSessao.FOCO, OrigemSessao.LIVRE, null, 10, 0,
                Instant.EPOCH, 0, List.of(), List.of());
        when(sessoes.abrir(any(), any())).thenReturn(retorno);
        mvc.perform(post("/api/v1/estudo/sessoes").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("00000000-0000-0000-0000-000000000001")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tamanho\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.modo").value("FOCO"))
                .andExpect(jsonPath("$.questoes").isArray()).andExpect(jsonPath("$.gabarito").doesNotExist());
    }

    @Test
    void TI10_aberturaRecusaTamanhoForaDoContrato() throws Exception {
        mvc.perform(post("/api/v1/estudo/sessoes").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("00000000-0000-0000-0000-000000000001")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tamanho\":7}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.fieldErrors.tamanho").exists());
    }

    @Test
    void TI13_trocaModoRetornaSessao() throws Exception {
        when(sessoes.trocarModo(any(), any(), any())).thenReturn(new RespostaSessao(SESSAO, ModoSessao.CONTEXTO,
                OrigemSessao.LIVRE, null, 10, 2, Instant.EPOCH, 12, List.of(), List.of()));
        mvc.perform(patch("/api/v1/estudo/sessoes/" + SESSAO + "/modo").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("00000000-0000-0000-0000-000000000001")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"CONTEXTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.modo").value("CONTEXTO"));
    }

    @Test
    void TI14_pausarRetorna204ESemCorpo() throws Exception {
        mvc.perform(post("/api/v1/estudo/sessoes/" + SESSAO + "/pausar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("00000000-0000-0000-0000-000000000001")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void TI15_retomarSessaoDevolveSerieDespausada() throws Exception {
        when(sessoes.retomarSessao(any(), any())).thenReturn(new RespostaSessao(SESSAO, ModoSessao.FOCO,
                OrigemSessao.LIVRE, null, 10, 3, Instant.EPOCH, 55, List.of(), List.of()));
        mvc.perform(post("/api/v1/estudo/sessoes/" + SESSAO + "/retomar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("00000000-0000-0000-0000-000000000001")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.decorridoSegundos").value(55));
    }

    @Test
    void TI16_pausarSemAutenticacaoRecusa() throws Exception {
        mvc.perform(post("/api/v1/estudo/sessoes/" + SESSAO + "/pausar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized());
    }
}
