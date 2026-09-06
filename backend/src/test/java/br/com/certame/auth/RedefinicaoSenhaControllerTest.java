package br.com.certame.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.excecao.TokenInvalidoException;
import br.com.certame.config.PropriedadesSeguranca;
import br.com.certame.auth.dto.RequisicaoEsqueciSenha;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AutenticacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ManipuladorErrosGlobal.class)
class RedefinicaoSenhaControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AutenticacaoService autenticacao;
    @MockitoBean RedefinicaoSenhaService redefinicao;
    @MockitoBean PropriedadesSeguranca propriedades;

    @Test
    void TI06_pedidoValidoSempreRetorna204() throws Exception {
        doNothing().when(redefinicao).solicitar("pessoa@example.com");

        mvc.perform(post("/api/v1/auth/senha/esqueci").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pessoa@example.com\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void TI07_pedidoComEmailMalformadoRetorna422SemChamarServico() throws Exception {
        mvc.perform(post("/api/v1/auth/senha/esqueci").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalido\"}"))
                .andExpect(status().isUnprocessableContent());
        org.mockito.Mockito.verifyNoInteractions(redefinicao);
    }

    @Test
    void TI08_redefinicaoValidaRetorna204() throws Exception {
        doNothing().when(redefinicao).redefinir("token", "novaSenha");

        mvc.perform(post("/api/v1/auth/senha/redefinir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token\",\"senha\":\"novaSenha\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void TI09_tokenInvalidoRetorna400() throws Exception {
        doThrow(new TokenInvalidoException()).when(redefinicao).redefinir(any(), any());

        mvc.perform(post("/api/v1/auth/senha/redefinir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token\",\"senha\":\"novaSenha\"}"))
                .andExpect(status().isBadRequest());
    }
}
