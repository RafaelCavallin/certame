package br.com.certame.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.certame.auth.dto.RequisicaoCadastro;
import br.com.certame.auth.dto.RespostaAutenticacao;
import br.com.certame.auth.dto.UsuarioResumo;
import br.com.certame.comum.ManipuladorErrosGlobal;
import br.com.certame.comum.excecao.EmailEmUsoException;
import br.com.certame.config.PropriedadesSeguranca;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AutenticacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ManipuladorErrosGlobal.class)
class AutenticacaoCadastroControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AutenticacaoService service;
    @MockitoBean RedefinicaoSenhaService redefinicaoSenha;
    @MockitoBean PropriedadesSeguranca propriedades;

    @Test
    void TI04_cadastroRetorna201ECookie() throws Exception {
        when(propriedades.getCookieNome()).thenReturn("certame_rt");
        when(propriedades.isCookieSecure()).thenReturn(true);
        when(propriedades.getCookieSameSite()).thenReturn("Strict");
        when(propriedades.getCookiePath()).thenReturn("/api/v1/auth");
        when(propriedades.getRefreshTokenTtl()).thenReturn(java.time.Duration.ofDays(14));
        when(service.cadastrarEstudante(any(RequisicaoCadastro.class), any(ContextoRequisicao.class)))
                .thenReturn(new AutenticacaoService.ResultadoAutenticacao(
                        new RespostaAutenticacao("jwt", Instant.EPOCH,
                                new UsuarioResumo(UUID.randomUUID(), "Pessoa Teste", "pessoa@example.com", "ESTUDANTE")), "refresh"));

        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa Teste\",\"email\":\"pessoa@example.com\",\"senha\":\"senhaSegura\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("certame_rt=refresh")))
                .andExpect(jsonPath("$.usuario.perfil").value("ESTUDANTE"));
    }

    @Test
    void TI05_cadastroDuplicadoRetorna409ComErroDoCampo() throws Exception {
        when(service.cadastrarEstudante(any(RequisicaoCadastro.class), any(ContextoRequisicao.class)))
                .thenThrow(new EmailEmUsoException());

        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa Teste\",\"email\":\"pessoa@example.com\",\"senha\":\"senhaSegura\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("EMAIL_EM_USO"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Este e-mail já está em uso."));
    }
}
